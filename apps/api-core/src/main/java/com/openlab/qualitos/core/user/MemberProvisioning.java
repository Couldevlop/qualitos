package com.openlab.qualitos.core.user;

import com.openlab.qualitos.core.edition.EditionExceptions;
import com.openlab.qualitos.core.edition.MemberLimit;
import com.openlab.qualitos.core.identity.PlatformRoles;
import com.openlab.qualitos.core.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * L'annuaire se remplit à la première connexion (ADR 0093).
 *
 * <p>Un utilisateur authentifié dont le jeton porte un client ({@code tenant_id})
 * et qui n'a pas encore de membre ({@code app_users}) en reçoit un, construit
 * depuis le jeton : compte ({@code sub}), adresse, nom affiché, rôles de
 * plateforme. S'il en a un, ce qui a changé dans Keycloak — la source de vérité —
 * y est reporté. Rien d'autre : ni courriel, ni invitation, ni activation —
 * l'utilisateur est déjà entré.
 *
 * <p><b>Jamais</b> pour un jeton de service (client_credentials), ni pour un
 * jeton sans client (le super administrateur de la plateforme). Le client vient
 * du jeton et de lui seul. Un membre rattaché à un AUTRE client n'est jamais
 * déplacé : le client d'un membre ne change pas après sa création.
 *
 * <p><b>Client inconnu.</b> Si la ligne {@code tenants} du client n'existe pas,
 * rien n'est écrit : elle porte la facturation, et seule l'API de création des
 * clients la crée. Un avertissement structuré le dit (identifiant du client,
 * jamais l'adresse ni le nom), et l'annuaire de ce client reste vide.
 *
 * <p><b>Coût.</b> Un cache court par compte retient l'empreinte du dernier
 * jeton traité : tant qu'elle ne change pas, aucune lecture ni écriture. Sous
 * concurrence, deux premières requêtes simultanées se disputent l'insertion ;
 * la contrainte unique {@code keycloak_id} tranche, la perdante relit et ne
 * répond jamais 500.
 */
@Component
public class MemberProvisioning {

    private static final Logger log = LoggerFactory.getLogger(MemberProvisioning.class);

    static final Duration TTL = Duration.ofMinutes(5);
    static final int MAX_ENTRIES = 10_000;
    private static final String SERVICE_ACCOUNT_PREFIX = "service-account-";
    /** La forme que la base exige (chk_app_users_email, V3). */
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

    /** Ce que la provision a fait d'un jeton : lisible par les bancs et le journal. */
    public enum Outcome {
        SERVICE_TOKEN, NO_TENANT, UNUSABLE, CACHED, TENANT_MISSING, CREATED, UPDATED, UNCHANGED, CONFLICT,
        LIMIT_REACHED
    }

    /** Ce que le jeton dit du membre. */
    record Member(UUID tenantId, String accountId, String email, String displayName, Set<String> roles) {
        String fingerprint() {
            return tenantId + "|" + email + "|" + displayName + "|" + String.join(",", roles);
        }
    }

    private record Seen(String fingerprint, Instant until) {}

    private final UserRepository users;
    private final TenantRepository tenants;
    private final MemberLimit memberLimit;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final Map<String, Seen> seen = new ConcurrentHashMap<>();

    @Autowired
    public MemberProvisioning(UserRepository users, TenantRepository tenants, MemberLimit memberLimit,
                              PlatformTransactionManager transactions, Clock clock) {
        this.users = users;
        this.tenants = tenants;
        this.memberLimit = memberLimit;
        this.tx = new TransactionTemplate(transactions);
        // Sa propre transaction : la provision ne se mêle jamais à celle de la requête.
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
    }

    /** Provisionne (ou met à jour) le membre du jeton ; ne lève jamais pour un jeton inexploitable. */
    public Outcome provision(Jwt jwt) {
        if (isServiceToken(jwt)) {
            return Outcome.SERVICE_TOKEN;
        }
        Optional<UUID> tenant = uuid(jwt.getClaimAsString("tenant_id"));
        if (tenant.isEmpty()) {
            return Outcome.NO_TENANT;
        }
        Optional<Member> lu = member(jwt, tenant.get());
        if (lu.isEmpty()) {
            log.warn("directory.provisioning.unusable-token tenant_id={}", tenant.get());
            return Outcome.UNUSABLE;
        }
        Member m = lu.get();
        Instant now = clock.instant();
        Seen deja = seen.get(m.accountId());
        if (deja != null && deja.fingerprint().equals(m.fingerprint()) && now.isBefore(deja.until())) {
            return Outcome.CACHED;
        }

        Outcome resultat;
        try {
            resultat = tx.execute(status -> apply(m));
        } catch (DataIntegrityViolationException premiere) {
            // Une requête concurrente a inséré le même compte entre la lecture et
            // l'écriture : on relit, la ligne est là.
            try {
                resultat = tx.execute(status -> apply(m));
            } catch (DataIntegrityViolationException seconde) {
                log.warn("directory.provisioning.conflict tenant_id={} account_id={}", m.tenantId(), m.accountId());
                resultat = Outcome.CONFLICT;
            }
        }
        if (resultat != Outcome.CONFLICT && resultat != Outcome.LIMIT_REACHED) {
            remember(m, now);
        }
        return resultat;
    }

    private Outcome apply(Member m) {
        if (!tenants.existsById(m.tenantId())) {
            log.warn("directory.provisioning.tenant-missing tenant_id={}", m.tenantId());
            return Outcome.TENANT_MISSING;
        }
        Optional<AppUser> existant = users.findByKeycloakId(m.accountId());
        if (existant.isPresent()) {
            AppUser u = existant.get();
            if (!m.tenantId().equals(u.getTenantId())) {
                log.warn("directory.provisioning.tenant-mismatch tenant_id={} account_id={}",
                        m.tenantId(), m.accountId());
                return Outcome.CONFLICT;
            }
            return update(u, m);
        }
        try {
            memberLimit.ensureRoomForOneMore(users.countByTenantIdAndActiveTrue(m.tenantId()));
        } catch (EditionExceptions.MemberLimitReached e) {
            log.warn("directory.provisioning.member-limit tenant_id={}", m.tenantId());
            return Outcome.LIMIT_REACHED;
        }
        AppUser saved = users.saveAndFlush(AppUser.builder()
                .tenantId(m.tenantId())
                .keycloakId(m.accountId())
                .email(m.email())
                .displayName(m.displayName())
                .roles(new HashSet<>(m.roles()))
                .active(true)
                .build());
        log.info("directory.provisioned user_id={} tenant_id={}", saved.getId(), m.tenantId());
        return Outcome.CREATED;
    }

    private Outcome update(AppUser u, Member m) {
        boolean change = false;
        if (!m.email().equals(u.getEmail())) {
            u.setEmail(m.email());
            change = true;
        }
        if (!Objects.equals(m.displayName(), u.getDisplayName())) {
            u.setDisplayName(m.displayName());
            change = true;
        }
        if (!m.roles().equals(u.getRoles())) {
            u.getRoles().clear();
            u.getRoles().addAll(m.roles());
            change = true;
        }
        if (!change) {
            return Outcome.UNCHANGED;
        }
        users.saveAndFlush(u);
        log.info("directory.provisioning.updated user_id={} tenant_id={}", u.getId(), m.tenantId());
        return Outcome.UPDATED;
    }

    private void remember(Member m, Instant now) {
        if (seen.size() >= MAX_ENTRIES) {
            // Borne grossière mais sûre : le cache n'est qu'un raccourci.
            seen.clear();
        }
        seen.put(m.accountId(), new Seen(m.fingerprint(), now.plus(TTL)));
    }

    /** Un jeton de client technique (client_credentials) n'est pas une personne. */
    static boolean isServiceToken(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return jwt.hasClaim("client_id") || jwt.hasClaim("clientId")
                || (username != null && username.startsWith(SERVICE_ACCOUNT_PREFIX));
    }

    static Optional<Member> member(Jwt jwt, UUID tenant) {
        Optional<UUID> compte = uuid(jwt.getSubject());
        String email = jwt.getClaimAsString("email");
        email = email == null ? null : email.strip();
        if (compte.isEmpty() || email == null || email.length() > 255 || !EMAIL.matcher(email).matches()) {
            return Optional.empty();
        }
        return Optional.of(new Member(tenant, compte.get().toString(), email, displayName(jwt),
                PlatformRoles.fromToken(realmRoles(jwt))));
    }

    /** {@code name}, sinon prénom et nom, sinon l'identifiant de connexion. */
    static String displayName(Jwt jwt) {
        String nom = jwt.getClaimAsString("name");
        if (nom == null || nom.isBlank()) {
            nom = AppUser.displayNameOf(jwt.getClaimAsString("given_name"), jwt.getClaimAsString("family_name"));
        }
        if (nom == null || nom.isBlank()) {
            nom = jwt.getClaimAsString("preferred_username");
        }
        if (nom == null || nom.isBlank()) {
            return null;
        }
        nom = nom.strip();
        return nom.length() > 200 ? nom.substring(0, 200).strip() : nom;
    }

    @SuppressWarnings("unchecked")
    private static Collection<String> realmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        Object roles = realmAccess == null ? null : realmAccess.get("roles");
        return roles instanceof Collection<?> c ? (Collection<String>) c : Set.of();
    }

    private static Optional<UUID> uuid(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value.strip()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
