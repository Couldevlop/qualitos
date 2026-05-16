package com.openlab.qualitos.quality.itsm;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestre la gestion des connexions ITSM et la synchronisation des incidents.
 *
 * Sécurité :
 * - Le secret n'est lu que depuis la requête de création/mise à jour, jamais ré-exposé
 *   dans une réponse (cf. {@link ItsmDto.ConnectionResponse} qui n'expose AUCUN champ
 *   sensible).
 * - Le ciphertext est stocké en DB. Le déchiffrement n'a lieu qu'au moment du sync,
 *   en mémoire, et n'est jamais loggé.
 *
 * Idempotence :
 * - L'import s'appuie sur (connection_id, external_id) UNIQUE. Un même incident
 *   réimporté met juste à jour {@code lastSeenAt} sans dupliquer d'entité locale.
 *
 * Politique d'échec :
 * - Sur erreur de sync, {@code consecutiveFailures} est incrémenté ; au-dessus de
 *   {@link #MAX_CONSECUTIVE_FAILURES_BEFORE_DISABLE}, la connexion bascule en
 *   DISABLED_ON_ERRORS pour éviter un effet boule de neige (cf. webhooks).
 */
@Service
public class ItsmConnectorService {

    private static final Logger log = LoggerFactory.getLogger(ItsmConnectorService.class);
    static final int MAX_CONSECUTIVE_FAILURES_BEFORE_DISABLE = 10;

    private final ItsmConnectionRepository connectionRepo;
    private final ItsmIncidentMappingRepository mappingRepo;
    private final SecretCipher cipher;
    private final Map<ItsmProvider, ItsmProviderClient> clients;

    public ItsmConnectorService(ItsmConnectionRepository connectionRepo,
                                ItsmIncidentMappingRepository mappingRepo,
                                SecretCipher cipher,
                                List<ItsmProviderClient> providerBeans) {
        this.connectionRepo = connectionRepo;
        this.mappingRepo = mappingRepo;
        this.cipher = cipher;
        Map<ItsmProvider, ItsmProviderClient> map = new EnumMap<>(ItsmProvider.class);
        for (ItsmProviderClient c : providerBeans) map.put(c.provider(), c);
        this.clients = map;
    }

    // ---------- Connections ----------

    @Transactional
    public ItsmDto.ConnectionResponse createConnection(ItsmDto.CreateConnectionRequest req) {
        UUID tenantId = requireTenantId();
        ItsmConnection c = new ItsmConnection();
        c.setTenantId(tenantId);
        c.setName(req.name());
        c.setProvider(req.provider());
        c.setBaseUrl(req.baseUrl());
        c.setUsername(req.username());
        c.setCredentialCipher(cipher.encrypt(req.secret()));
        c.setExternalScope(req.externalScope());
        c.setStatus(ConnectionStatus.ACTIVE);
        c.setCreatedBy(req.createdBy());
        c = connectionRepo.save(c);
        return toResponse(c);
    }

    @Transactional(readOnly = true)
    public Page<ItsmDto.ConnectionResponse> listConnections(Pageable pageable) {
        UUID tenantId = requireTenantId();
        return connectionRepo.findByTenantId(tenantId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ItsmDto.ConnectionResponse getConnection(UUID id) {
        UUID tenantId = requireTenantId();
        ItsmConnection c = connectionRepo.findById(id).orElseThrow(() -> new ItsmConnectionNotFoundException(id));
        ensureSameTenant(c, tenantId);
        return toResponse(c);
    }

    @Transactional
    public ItsmDto.ConnectionResponse updateConnection(UUID id, ItsmDto.UpdateConnectionRequest req) {
        UUID tenantId = requireTenantId();
        ItsmConnection c = connectionRepo.findById(id).orElseThrow(() -> new ItsmConnectionNotFoundException(id));
        ensureSameTenant(c, tenantId);
        if (req.name() != null) c.setName(req.name());
        if (req.baseUrl() != null) c.setBaseUrl(req.baseUrl());
        if (req.username() != null) c.setUsername(req.username());
        if (req.secret() != null) c.setCredentialCipher(cipher.encrypt(req.secret()));
        if (req.externalScope() != null) c.setExternalScope(req.externalScope());
        if (req.status() != null) {
            c.setStatus(req.status());
            // Re-activation manuelle remet à zéro le compteur d'échecs.
            if (req.status() == ConnectionStatus.ACTIVE) c.setConsecutiveFailures(0);
        }
        return toResponse(connectionRepo.save(c));
    }

    @Transactional
    public void deleteConnection(UUID id) {
        UUID tenantId = requireTenantId();
        ItsmConnection c = connectionRepo.findById(id).orElseThrow(() -> new ItsmConnectionNotFoundException(id));
        ensureSameTenant(c, tenantId);
        connectionRepo.delete(c);
    }

    // ---------- Sync ----------

    /**
     * Lance une sync manuelle pour une connexion donnée. Si la connexion n'est pas
     * ACTIVE, l'opération échoue silencieusement avec un rapport vide + message.
     */
    @Transactional
    public ItsmDto.SyncReport syncConnection(UUID id) {
        UUID tenantId = requireTenantId();
        ItsmConnection c = connectionRepo.findById(id).orElseThrow(() -> new ItsmConnectionNotFoundException(id));
        ensureSameTenant(c, tenantId);
        Instant ranAt = Instant.now();
        c.setLastSyncAt(ranAt);

        if (c.getStatus() != ConnectionStatus.ACTIVE) {
            connectionRepo.save(c);
            return new ItsmDto.SyncReport(c.getId(), 0, 0, 0, ranAt,
                    "Connection is not ACTIVE (" + c.getStatus() + ")");
        }

        ItsmProviderClient client = clients.get(c.getProvider());
        if (client == null) {
            connectionRepo.save(c);
            return new ItsmDto.SyncReport(c.getId(), 0, 0, 0, ranAt,
                    "No client registered for provider " + c.getProvider());
        }

        String secret;
        try {
            secret = cipher.decrypt(c.getCredentialCipher());
        } catch (RuntimeException ex) {
            log.warn("ITSM secret decryption failed for connection {}", c.getId());
            failOnce(c);
            connectionRepo.save(c);
            return new ItsmDto.SyncReport(c.getId(), 0, 0, 0, ranAt, "Secret decryption failed");
        }

        List<ExternalIncident> fetched;
        try {
            fetched = client.fetchIncidents(c, secret, c.getLastSuccessAt());
        } catch (ItsmSyncException ex) {
            failOnce(c);
            connectionRepo.save(c);
            return new ItsmDto.SyncReport(c.getId(), 0, 0, 0, ranAt, ex.getMessage());
        }

        int newImports = 0;
        int alreadyKnown = 0;
        for (ExternalIncident inc : fetched) {
            if (inc.externalId() == null || inc.externalId().isBlank()) continue;
            Optional<ItsmIncidentMapping> existing =
                    mappingRepo.findByConnectionIdAndExternalId(c.getId(), inc.externalId());
            if (existing.isPresent()) {
                ItsmIncidentMapping m = existing.get();
                m.setExternalStatus(inc.status());
                m.setExternalPriority(inc.priority());
                m.setExternalTitle(inc.title());
                m.setLastSeenAt(ranAt);
                mappingRepo.save(m);
                alreadyKnown++;
            } else {
                ItsmIncidentMapping m = new ItsmIncidentMapping();
                m.setTenantId(tenantId);
                m.setConnectionId(c.getId());
                m.setExternalId(inc.externalId());
                m.setExternalUrl(inc.url());
                m.setExternalStatus(inc.status());
                m.setExternalPriority(inc.priority());
                m.setExternalTitle(inc.title());
                m.setInternalEntityType("NON_CONFORMITY");
                m.setInternalEntityId(null); // créera l'entité NC plus tard via un dispatcher
                m.setFirstImportedAt(ranAt);
                m.setLastSeenAt(ranAt);
                mappingRepo.save(m);
                newImports++;
            }
        }

        c.setLastSuccessAt(ranAt);
        c.setConsecutiveFailures(0);
        connectionRepo.save(c);

        return new ItsmDto.SyncReport(c.getId(), fetched.size(), newImports, alreadyKnown, ranAt, null);
    }

    @Transactional(readOnly = true)
    public Page<ItsmDto.MappingResponse> listMappings(UUID connectionId, Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<ItsmIncidentMapping> page = connectionId == null
                ? mappingRepo.findByTenantId(tenantId, pageable)
                : mappingRepo.findByTenantIdAndConnectionId(tenantId, connectionId, pageable);
        return page.map(this::toResponse);
    }

    // ---------- helpers ----------

    private void failOnce(ItsmConnection c) {
        c.setConsecutiveFailures(c.getConsecutiveFailures() + 1);
        if (c.getConsecutiveFailures() >= MAX_CONSECUTIVE_FAILURES_BEFORE_DISABLE
                && c.getStatus() == ConnectionStatus.ACTIVE) {
            c.setStatus(ConnectionStatus.DISABLED_ON_ERRORS);
            log.warn("ITSM connection {} auto-disabled after {} consecutive failures",
                    c.getId(), c.getConsecutiveFailures());
        }
    }

    private void ensureSameTenant(ItsmConnection c, UUID tenantId) {
        if (!c.getTenantId().equals(tenantId)) {
            throw new ItsmConnectionNotFoundException(c.getId()); // ne pas leaker l'existence cross-tenant
        }
    }

    private ItsmDto.ConnectionResponse toResponse(ItsmConnection c) {
        return new ItsmDto.ConnectionResponse(
                c.getId(), c.getTenantId(), c.getName(), c.getProvider(),
                c.getBaseUrl(), c.getUsername(), c.getExternalScope(),
                c.getStatus(), c.getConsecutiveFailures(),
                c.getLastSyncAt(), c.getLastSuccessAt(),
                c.getCreatedBy(), c.getCreatedAt(), c.getUpdatedAt()
        );
    }

    private ItsmDto.MappingResponse toResponse(ItsmIncidentMapping m) {
        return new ItsmDto.MappingResponse(
                m.getId(), m.getTenantId(), m.getConnectionId(),
                m.getExternalId(), m.getExternalUrl(), m.getExternalStatus(),
                m.getExternalPriority(), m.getExternalTitle(),
                m.getInternalEntityType(), m.getInternalEntityId(),
                m.getFirstImportedAt(), m.getLastSeenAt()
        );
    }

    // exposé pour les tests
    Map<ItsmProvider, ItsmProviderClient> clientsView() { return new HashMap<>(clients); }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
