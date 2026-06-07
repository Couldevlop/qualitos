package com.openlab.qualitos.quality.ehrconnector;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.itsm.ConnectionStatus;
import com.openlab.qualitos.quality.itsm.SecretCipher;
import com.openlab.qualitos.quality.nonconformity.NcCategory;
import com.openlab.qualitos.quality.nonconformity.NcDto;
import com.openlab.qualitos.quality.nonconformity.NcService;
import com.openlab.qualitos.quality.nonconformity.NcSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orchestre les connexions EHR / HL7 FHIR (§13.3) et la synchronisation des
 * ressources « patient-safety » vers des Non-Conformités (§5.2 : événements
 * indésirables → NC).
 *
 * <h2>Sécurité</h2>
 * <ul>
 *   <li>Tenant TOUJOURS issu du {@link TenantContext} (JWT), jamais du body (§18.2-2).</li>
 *   <li>Le secret est chiffré au repos ({@link SecretCipher}) et déchiffré en mémoire
 *       uniquement au moment du sync ; jamais ré-exposé ni loggé.</li>
 * </ul>
 *
 * <h2>PRIVACY / RGPD (§11.3)</h2>
 * <p>Aucune donnée personnelle de santé n'est stockée en clair côté QualitOS. La NC
 * créée référence la ressource FHIR par son <b>identifiant technique</b>
 * (ex. {@code Observation/abc-123}) et sa <b>classification</b> (code, interprétation),
 * jamais le patient (subject), ni une valeur clinique, ni une note libre. La table
 * d'idempotence ne conserve elle aussi que l'id technique de la ressource.</p>
 *
 * <h2>Idempotence</h2>
 * <p>Avant de créer une NC, on vérifie {@link EhrImportedResourceRepository
 * #existsByConnectionIdAndFhirResourceId}. Une 2e synchronisation ne recrée donc
 * pas de NC pour une ressource déjà importée.</p>
 */
@Service
public class EhrConnectorService {

    private static final Logger log = LoggerFactory.getLogger(EhrConnectorService.class);
    static final int MAX_CONSECUTIVE_FAILURES_BEFORE_DISABLE = 10;

    private final EhrConnectionRepository connectionRepo;
    private final EhrImportedResourceRepository importedRepo;
    private final SecretCipher cipher;
    private final FhirClient fhirClient;
    private final NcService ncService;

    public EhrConnectorService(EhrConnectionRepository connectionRepo,
                               EhrImportedResourceRepository importedRepo,
                               SecretCipher cipher,
                               FhirClient fhirClient,
                               NcService ncService) {
        this.connectionRepo = connectionRepo;
        this.importedRepo = importedRepo;
        this.cipher = cipher;
        this.fhirClient = fhirClient;
        this.ncService = ncService;
    }

    // ---------- Connections (CRUD) ----------

    @Transactional
    public EhrDto.ConnectionResponse createConnection(EhrDto.CreateConnectionRequest req) {
        UUID tenantId = requireTenantId();
        EhrConnection c = new EhrConnection();
        c.setTenantId(tenantId);
        c.setName(req.name());
        c.setProvider(req.provider());
        c.setFhirBaseUrl(req.fhirBaseUrl());
        c.setAuthMode(req.authMode());
        c.setUsername(req.username());
        c.setCredentialCipher(cipher.encrypt(req.secret()));
        c.setResourceCategory(req.resourceCategory());
        c.setStatus(ConnectionStatus.ACTIVE);
        c.setCreatedBy(req.createdBy());
        return toResponse(connectionRepo.save(c));
    }

    @Transactional(readOnly = true)
    public Page<EhrDto.ConnectionResponse> listConnections(Pageable pageable) {
        UUID tenantId = requireTenantId();
        return connectionRepo.findByTenantId(tenantId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EhrDto.ConnectionResponse getConnection(UUID id) {
        return toResponse(load(id));
    }

    @Transactional
    public EhrDto.ConnectionResponse updateConnection(UUID id, EhrDto.UpdateConnectionRequest req) {
        EhrConnection c = load(id);
        if (req.name() != null) c.setName(req.name());
        if (req.fhirBaseUrl() != null) c.setFhirBaseUrl(req.fhirBaseUrl());
        if (req.authMode() != null) c.setAuthMode(req.authMode());
        if (req.username() != null) c.setUsername(req.username());
        if (req.secret() != null) c.setCredentialCipher(cipher.encrypt(req.secret()));
        if (req.resourceCategory() != null) c.setResourceCategory(req.resourceCategory());
        if (req.status() != null) {
            c.setStatus(req.status());
            if (req.status() == ConnectionStatus.ACTIVE) c.setConsecutiveFailures(0);
        }
        return toResponse(connectionRepo.save(c));
    }

    @Transactional
    public void deleteConnection(UUID id) {
        connectionRepo.delete(load(id));
    }

    // ---------- Sync ----------

    /**
     * Synchronise une connexion : récupère les ressources FHIR anormales modifiées
     * depuis {@code lastSuccessAt}, crée une NC par ressource non encore importée,
     * et trace l'idempotence. Renvoie un {@link EhrDto.SyncReport}.
     */
    @Transactional
    public EhrDto.SyncReport sync(UUID id) {
        UUID tenantId = requireTenantId();
        EhrConnection c = load(id);
        Instant ranAt = Instant.now();
        c.setLastSyncAt(ranAt);

        if (c.getStatus() != ConnectionStatus.ACTIVE) {
            connectionRepo.save(c);
            return new EhrDto.SyncReport(c.getId(), 0, 0, 0, 0, ranAt,
                    "Connection is not ACTIVE (" + c.getStatus() + ")");
        }

        String secret;
        try {
            secret = cipher.decrypt(c.getCredentialCipher());
        } catch (RuntimeException ex) {
            log.warn("EHR secret decryption failed for connection {}", c.getId());
            failOnce(c);
            connectionRepo.save(c);
            return new EhrDto.SyncReport(c.getId(), 0, 0, 0, 0, ranAt, "Secret decryption failed");
        }

        List<FhirResource> fetched;
        try {
            fetched = fhirClient.fetchAdverseResources(c, secret, c.getLastSuccessAt());
        } catch (EhrSyncException ex) {
            failOnce(c);
            connectionRepo.save(c);
            return new EhrDto.SyncReport(c.getId(), 0, 0, 0, 1, ranAt, ex.getMessage());
        }

        int created = 0;
        int skipped = 0;
        int errors = 0;
        for (FhirResource res : fetched) {
            if (res.id() == null || res.id().isBlank()) {
                continue;
            }
            if (importedRepo.existsByConnectionIdAndFhirResourceId(c.getId(), res.id())) {
                skipped++;
                continue;
            }
            try {
                UUID ncId = createNonConformity(res);
                EhrImportedResource imported = new EhrImportedResource();
                imported.setTenantId(tenantId);
                imported.setConnectionId(c.getId());
                imported.setFhirResourceType(res.resourceType());
                imported.setFhirResourceId(res.id());
                imported.setNcId(ncId);
                importedRepo.save(imported);
                created++;
            } catch (RuntimeException ex) {
                // Une ressource en échec ne doit pas faire échouer tout le batch.
                log.warn("EHR import failed for resource {} on connection {}",
                        res.reference(), c.getId());
                errors++;
            }
        }

        c.setLastSuccessAt(ranAt);
        c.setConsecutiveFailures(0);
        connectionRepo.save(c);
        return new EhrDto.SyncReport(c.getId(), fetched.size(), created, skipped, errors, ranAt, null);
    }

    /**
     * Crée la NC à partir d'une ressource FHIR. Description STRICTEMENT non-PII :
     * référence technique + classification uniquement.
     */
    private UUID createNonConformity(FhirResource res) {
        NcSeverity severity = mapSeverity(res.interpretation());
        Instant detectedAt = res.effective() != null ? res.effective() : Instant.now();
        String label = res.codeDisplay() != null ? res.codeDisplay()
                : (res.code() != null ? res.code() : res.resourceType());

        // Titre & description SANS donnée patient : on ne cite que l'identité
        // TECHNIQUE de la ressource et sa classification (§11.3 RGPD).
        String title = "EHR patient-safety signal: " + truncate(label, 200);
        String description = "Imported from FHIR resource " + res.reference()
                + " | code=" + nullSafe(res.code())
                + " | interpretation=" + nullSafe(res.interpretation())
                + " | status=" + nullSafe(res.status())
                + ". No patient-identifiable data stored (RGPD §11.3).";

        NcDto.CreateRequest req = new NcDto.CreateRequest(
                truncate(title, 255),
                description,
                NcCategory.SAFETY,
                severity,
                detectedAt,
                null,   // zone
                null,   // geoLat
                null,   // geoLng
                null,   // photoUrls
                null    // reporterId : import système, pas un acteur humain nominatif
        );
        return ncService.create(req).id();
    }

    /**
     * Mappe l'interprétation FHIR vers une sévérité NC :
     * <ul>
     *   <li>AA / HH / LL → CRITICAL (critiquement anormal) ;</li>
     *   <li>autres codes anormaux (A, H, L, POS…) → MAJOR ;</li>
     *   <li>inconnu/absent → MINOR (signal faible).</li>
     * </ul>
     */
    static NcSeverity mapSeverity(String interpretation) {
        if (interpretation == null) {
            return NcSeverity.MINOR;
        }
        String code = interpretation.toUpperCase();
        return switch (code) {
            case "AA", "HH", "LL" -> NcSeverity.CRITICAL;
            case "A", "H", "L", "HU", "LU", "POS", "U", "D", "W" -> NcSeverity.MAJOR;
            default -> NcSeverity.MINOR;
        };
    }

    // ---------- helpers ----------

    private void failOnce(EhrConnection c) {
        c.setConsecutiveFailures(c.getConsecutiveFailures() + 1);
        if (c.getConsecutiveFailures() >= MAX_CONSECUTIVE_FAILURES_BEFORE_DISABLE
                && c.getStatus() == ConnectionStatus.ACTIVE) {
            c.setStatus(ConnectionStatus.DISABLED_ON_ERRORS);
            log.warn("EHR connection {} auto-disabled after {} consecutive failures",
                    c.getId(), c.getConsecutiveFailures());
        }
    }

    private EhrConnection load(UUID id) {
        UUID tenantId = requireTenantId();
        EhrConnection c = connectionRepo.findById(id).orElseThrow(() -> new EhrConnectionNotFoundException(id));
        if (!c.getTenantId().equals(tenantId)) {
            // Ne pas leaker l'existence cross-tenant.
            throw new EhrConnectionNotFoundException(id);
        }
        return c;
    }

    private static String nullSafe(String s) {
        return s == null ? "n/a" : s;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private EhrDto.ConnectionResponse toResponse(EhrConnection c) {
        return new EhrDto.ConnectionResponse(
                c.getId(), c.getTenantId(), c.getName(), c.getProvider(),
                c.getFhirBaseUrl(), c.getAuthMode(), c.getUsername(), c.getResourceCategory(),
                c.getStatus(), c.getConsecutiveFailures(),
                c.getLastSyncAt(), c.getLastSuccessAt(),
                c.getCreatedBy(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
