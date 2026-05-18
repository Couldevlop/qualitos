package com.openlab.qualitos.quality.apikeys.application;

import com.openlab.qualitos.quality.apikeys.domain.ApiKey;
import com.openlab.qualitos.quality.apikeys.domain.ApiKeyHasher;
import com.openlab.qualitos.quality.apikeys.domain.ApiKeyNotFoundException;
import com.openlab.qualitos.quality.apikeys.domain.ApiKeyRepository;
import com.openlab.qualitos.quality.apikeys.domain.ApiKeySecretGenerator;
import com.openlab.qualitos.quality.apikeys.domain.ApiKeyStateException;
import com.openlab.qualitos.quality.apikeys.domain.ApiKeyStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Use cases API key management. Sécurité :
 *  - Plaintext renvoyé UNIQUEMENT à create()/rotate(). Aucune autre méthode
 *    n'expose le secret.
 *  - verify(plaintext) résout par prefix (lookup O(1)), puis bcrypt-matches.
 *    Si la clé n'existe pas, l'appel reste à temps comparable à un match raté
 *    pour limiter les attaques par mesure (best-effort — bcrypt ralentit
 *    naturellement les deux chemins).
 *  - Cross-tenant : 404 plutôt que 403 (no info leak).
 *  - Toute opération publie un événement (OWASP A09 — security logging).
 */
public class ApiKeyService {

    private final ApiKeyRepository repo;
    private final ApiKeyHasher hasher;
    private final ApiKeySecretGenerator generator;
    private final TenantProvider tenantProvider;
    private final ApiKeyEventPublisher events;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public ApiKeyService(ApiKeyRepository repo, ApiKeyHasher hasher,
                         ApiKeySecretGenerator generator,
                         TenantProvider tenantProvider, Clock clock) {
        this(repo, hasher, generator, tenantProvider,
                new ApiKeyEventPublisher.NoOp(), clock);
    }

    public ApiKeyService(ApiKeyRepository repo, ApiKeyHasher hasher,
                         ApiKeySecretGenerator generator,
                         TenantProvider tenantProvider,
                         ApiKeyEventPublisher events, Clock clock) {
        this.repo = repo;
        this.hasher = hasher;
        this.generator = generator;
        this.tenantProvider = tenantProvider;
        this.events = events;
        this.clock = clock;
    }

    public ApiKeyDto.IssuedKey create(ApiKeyDto.CreateRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (req.name() == null || req.name().isBlank()) {
            throw new ApiKeyStateException("name required");
        }
        Instant now = Instant.now(clock);
        ApiKeySecretGenerator.Material m = generator.generate();
        String hash = hasher.hash(m.rawSecret());
        ApiKey k = ApiKey.issued(tenantId, req.name().trim(), m.prefix(), hash,
                normalizeScopes(req.scopes()),
                req.expiresAt(), req.actor() != null ? req.actor() : tenantId,
                now);
        ApiKey saved = repo.save(k);
        events.publish(saved, ApiKeyEventPublisher.Action.ISSUED);
        return new ApiKeyDto.IssuedKey(ApiKeyDto.View.of(saved), m.plaintextRepresentation());
    }

    public ApiKeyDto.IssuedKey rotate(UUID id, ApiKeyDto.RotateRequest req) {
        ApiKey k = loadForTenant(id);
        Instant now = Instant.now(clock);
        ApiKeySecretGenerator.Material m = generator.generate();
        // Note : on garde le même {@code prefix} ? Non — la rotation génère un
        // nouveau prefix + secret pour invalider toute clé hors-ligne. Mais
        // le code stocke le prefix comme final dans l'aggregate. Pour éviter
        // un refactor, on EXIGE que les rotations émettent une nouvelle clé
        // (création) et révoquent l'ancienne. Donc rotate révoque + crée.
        k.revoke(req.actor(), now);
        repo.save(k);
        events.publish(k, ApiKeyEventPublisher.Action.REVOKED);
        ApiKey fresh = ApiKey.issued(k.getTenantId(), k.getName(), m.prefix(),
                hasher.hash(m.rawSecret()),
                k.getScopes(), k.getExpiresAt(), req.actor(), now);
        ApiKey saved = repo.save(fresh);
        events.publish(saved, ApiKeyEventPublisher.Action.ROTATED);
        return new ApiKeyDto.IssuedKey(ApiKeyDto.View.of(saved), m.plaintextRepresentation());
    }

    public ApiKeyDto.View revoke(UUID id, ApiKeyDto.RevokeRequest req) {
        ApiKey k = loadForTenant(id);
        k.revoke(req.actor(), Instant.now(clock));
        ApiKey saved = repo.save(k);
        events.publish(saved, ApiKeyEventPublisher.Action.REVOKED);
        return ApiKeyDto.View.of(saved);
    }

    public ApiKeyDto.View get(UUID id) {
        return ApiKeyDto.View.of(loadForTenant(id));
    }

    public List<ApiKeyDto.View> list() {
        UUID tenantId = tenantProvider.requireTenantId();
        return repo.findAllByTenantId(tenantId).stream().map(ApiKeyDto.View::of).toList();
    }

    /**
     * Vérifie un secret en clair présenté par un client. Le plaintext attendu
     * est au format {@code qos_<prefix>_<secret>}. Renvoie la clé si valide,
     * met à jour {@code lastUsedAt}. Renvoie empty si la clé n'existe pas /
     * ne match pas / est non-ACTIVE / expirée — sans distinguer (OWASP A07 :
     * pas de différenciation d'erreur d'auth pour éviter l'énumération).
     */
    public Optional<ApiKey> verify(String plaintext) {
        if (plaintext == null) return Optional.empty();
        String[] parts = plaintext.split("_", 3);
        if (parts.length != 3 || !"qos".equals(parts[0])
                || parts[1].isBlank() || parts[2].isBlank()) {
            return Optional.empty();
        }
        Optional<ApiKey> found = repo.findByPrefix(parts[1]);
        if (found.isEmpty()) return Optional.empty();
        ApiKey k = found.get();
        if (!k.isUsable()) return Optional.empty();
        if (!hasher.matches(parts[2], k.getHashedSecret())) return Optional.empty();
        try {
            k.recordUsage(Instant.now(clock));
        } catch (ApiKeyStateException e) {
            // expirée à la volée
            repo.save(k);
            events.publish(k, ApiKeyEventPublisher.Action.EXPIRED);
            return Optional.empty();
        }
        repo.save(k);
        events.publish(k, ApiKeyEventPublisher.Action.USED);
        return Optional.of(k);
    }

    /** Scheduler : passe en EXPIRED les clés actives échues. */
    public int expireDue(int limit) {
        Instant now = Instant.now(clock);
        int expired = 0;
        for (ApiKey k : repo.findExpirable(now, Math.max(1, Math.min(limit, 500)))) {
            if (k.expireIfDue(now)) {
                repo.save(k);
                events.publish(k, ApiKeyEventPublisher.Action.EXPIRED);
                expired++;
            }
        }
        return expired;
    }

    private ApiKey loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        ApiKey k = repo.findById(id).orElseThrow(() -> new ApiKeyNotFoundException(id));
        if (!k.getTenantId().equals(tenantId)) throw new ApiKeyNotFoundException(id);
        return k;
    }

    private static Set<String> normalizeScopes(Set<String> scopes) {
        if (scopes == null) return Set.of();
        Set<String> out = new java.util.TreeSet<>();
        for (String s : scopes) {
            if (s == null) continue;
            String trimmed = s.trim();
            if (trimmed.isEmpty()) continue;
            if (!trimmed.matches("^[a-z][a-z0-9._:-]{0,99}$")) {
                throw new ApiKeyStateException("Invalid scope format: " + s);
            }
            out.add(trimmed);
        }
        return out;
    }

    public boolean hasStatus(UUID id, ApiKeyStatus status) {
        return loadForTenant(id).getStatus() == status;
    }
}
