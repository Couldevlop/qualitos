package com.openlab.qualitos.quality.erpconnector;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.itsm.SecretCipher;
import com.openlab.qualitos.quality.kpi.KpiDefinition;
import com.openlab.qualitos.quality.kpi.KpiDefinitionRepository;
import com.openlab.qualitos.quality.kpi.KpiMeasurement;
import com.openlab.qualitos.quality.kpi.KpiMeasurementRepository;
import com.openlab.qualitos.quality.kpi.MeasurementSource;
import com.openlab.qualitos.quality.supplier.Supplier;
import com.openlab.qualitos.quality.supplier.SupplierRepository;
import com.openlab.qualitos.quality.supplier.SupplierType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestre la gestion des connexions ERP et la synchronisation (CLAUDE.md §13.3 :
 * « ERP → indicateurs production, achats, fournisseurs »). Calqué sur
 * {@code itsm.ItsmConnectorService}.
 *
 * <p><b>Sécurité</b> : le secret n'est lu qu'à la création/mise à jour, jamais ré-exposé
 * (cf. {@link ErpDto.ConnectionResponse}). Le ciphertext est stocké en DB ; le déchiffrement
 * n'a lieu qu'au sync, en mémoire, jamais loggé. Le {@code tenantId} vient du JWT
 * ({@link TenantContext}), jamais du body (règle 18.2 #2).
 *
 * <p><b>Idempotence</b> :
 * <ul>
 *   <li>Fournisseurs : upsert par (tenant, code externe) — un même fournisseur ré-importé
 *       met à jour nom/catégorie/contact sans dupliquer.</li>
 *   <li>KPIs : upsert par (kpi, période) via {@code KpiMeasurementRepository}. Si le code
 *       KPI ne correspond à AUCUNE {@code KpiDefinition} du tenant, la mesure est IGNORÉE en
 *       WARN — on NE crée JAMAIS de KPI sauvage (règle 18.2 #12).</li>
 * </ul>
 *
 * <p><b>Politique d'échec</b> : sur erreur de sync, {@code consecutiveFailures} est incrémenté ;
 * au-delà de {@link #MAX_CONSECUTIVE_FAILURES_BEFORE_DISABLE}, la connexion passe en
 * DISABLED_ON_ERRORS.
 */
@Service
public class ErpConnectorService {

    private static final Logger log = LoggerFactory.getLogger(ErpConnectorService.class);
    static final int MAX_CONSECUTIVE_FAILURES_BEFORE_DISABLE = 10;

    private final ErpConnectionRepository connectionRepo;
    private final SupplierRepository supplierRepo;
    private final KpiDefinitionRepository kpiDefinitionRepo;
    private final KpiMeasurementRepository kpiMeasurementRepo;
    private final SecretCipher cipher;
    private final Map<ErpProvider, ErpProviderClient> clients;

    public ErpConnectorService(ErpConnectionRepository connectionRepo,
                               SupplierRepository supplierRepo,
                               KpiDefinitionRepository kpiDefinitionRepo,
                               KpiMeasurementRepository kpiMeasurementRepo,
                               SecretCipher cipher,
                               List<ErpProviderClient> providerBeans) {
        this.connectionRepo = connectionRepo;
        this.supplierRepo = supplierRepo;
        this.kpiDefinitionRepo = kpiDefinitionRepo;
        this.kpiMeasurementRepo = kpiMeasurementRepo;
        this.cipher = cipher;
        Map<ErpProvider, ErpProviderClient> map = new EnumMap<>(ErpProvider.class);
        for (ErpProviderClient c : providerBeans) map.put(c.provider(), c);
        this.clients = map;
    }

    // ---------- Connections (CRUD) ----------

    @Transactional
    public ErpDto.ConnectionResponse createConnection(ErpDto.CreateConnectionRequest req) {
        UUID tenantId = requireTenantId();
        ErpConnection c = new ErpConnection();
        c.setTenantId(tenantId);
        c.setName(req.name());
        c.setProvider(req.provider());
        c.setBaseUrl(req.baseUrl());
        c.setUsername(req.username());
        c.setCredentialCipher(cipher.encrypt(req.secret()));
        c.setExternalScope(req.externalScope());
        c.setStatus(ErpConnectionStatus.ACTIVE);
        c.setCreatedBy(req.createdBy());
        return toResponse(connectionRepo.save(c));
    }

    @Transactional(readOnly = true)
    public Page<ErpDto.ConnectionResponse> listConnections(Pageable pageable) {
        UUID tenantId = requireTenantId();
        return connectionRepo.findByTenantId(tenantId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ErpDto.ConnectionResponse getConnection(UUID id) {
        UUID tenantId = requireTenantId();
        ErpConnection c = connectionRepo.findById(id).orElseThrow(() -> new ErpConnectionNotFoundException(id));
        ensureSameTenant(c, tenantId);
        return toResponse(c);
    }

    @Transactional
    public ErpDto.ConnectionResponse updateConnection(UUID id, ErpDto.UpdateConnectionRequest req) {
        UUID tenantId = requireTenantId();
        ErpConnection c = connectionRepo.findById(id).orElseThrow(() -> new ErpConnectionNotFoundException(id));
        ensureSameTenant(c, tenantId);
        if (req.name() != null) c.setName(req.name());
        if (req.baseUrl() != null) c.setBaseUrl(req.baseUrl());
        if (req.username() != null) c.setUsername(req.username());
        if (req.secret() != null) c.setCredentialCipher(cipher.encrypt(req.secret()));
        if (req.externalScope() != null) c.setExternalScope(req.externalScope());
        if (req.status() != null) {
            c.setStatus(req.status());
            if (req.status() == ErpConnectionStatus.ACTIVE) c.setConsecutiveFailures(0);
        }
        return toResponse(connectionRepo.save(c));
    }

    @Transactional
    public void deleteConnection(UUID id) {
        UUID tenantId = requireTenantId();
        ErpConnection c = connectionRepo.findById(id).orElseThrow(() -> new ErpConnectionNotFoundException(id));
        ensureSameTenant(c, tenantId);
        connectionRepo.delete(c);
    }

    // ---------- Sync ----------

    /**
     * Lance une sync manuelle : importe fournisseurs (upsert) + indicateurs de production
     * (mappés vers KpiDefinition existantes). Si la connexion n'est pas ACTIVE, retour
     * immédiat avec un rapport vide + message.
     */
    @Transactional
    public ErpDto.SyncReport sync(UUID id) {
        UUID tenantId = requireTenantId();
        ErpConnection c = connectionRepo.findById(id).orElseThrow(() -> new ErpConnectionNotFoundException(id));
        ensureSameTenant(c, tenantId);
        Instant ranAt = Instant.now();
        c.setLastSyncAt(ranAt);

        if (c.getStatus() != ErpConnectionStatus.ACTIVE) {
            connectionRepo.save(c);
            return emptyReport(c.getId(), ranAt, "Connection is not ACTIVE (" + c.getStatus() + ")");
        }

        ErpProviderClient client = clients.get(c.getProvider());
        if (client == null) {
            connectionRepo.save(c);
            return emptyReport(c.getId(), ranAt, "No client registered for provider " + c.getProvider());
        }

        String secret;
        try {
            secret = cipher.decrypt(c.getCredentialCipher());
        } catch (RuntimeException ex) {
            log.warn("ERP secret decryption failed for connection {}", c.getId());
            failOnce(c);
            connectionRepo.save(c);
            return emptyReport(c.getId(), ranAt, "Secret decryption failed");
        }

        List<ExternalSupplier> suppliers;
        List<ExternalProductionKpi> kpis;
        try {
            suppliers = client.fetchSuppliers(c, secret);
            kpis = client.fetchProductionKpis(c, secret);
        } catch (ErpSyncException ex) {
            failOnce(c);
            connectionRepo.save(c);
            return emptyReport(c.getId(), ranAt, ex.getMessage());
        }

        int[] supCounts = importSuppliers(tenantId, suppliers, c.getCreatedBy());
        int[] kpiCounts = importKpis(tenantId, kpis);

        c.setLastSuccessAt(ranAt);
        c.setConsecutiveFailures(0);
        connectionRepo.save(c);

        return new ErpDto.SyncReport(c.getId(),
                supCounts[0], supCounts[1], kpiCounts[0], kpiCounts[1], ranAt, null);
    }

    // ---------- Import: suppliers (quality/supplier) ----------

    /** @return [imported, ignored] */
    private int[] importSuppliers(UUID tenantId, List<ExternalSupplier> suppliers, UUID createdBy) {
        int imported = 0, ignored = 0;
        for (ExternalSupplier s : suppliers) {
            if (s.externalCode() == null || s.externalCode().isBlank()) { ignored++; continue; }
            Supplier entity = supplierRepo.findByTenantIdAndCode(tenantId, s.externalCode())
                    .orElseGet(() -> {
                        Supplier ns = new Supplier();
                        ns.setTenantId(tenantId);
                        ns.setCode(s.externalCode());
                        ns.setCreatedBy(createdBy);
                        return ns;
                    });
            // Champs synchronisés depuis l'ERP (source de vérité achats). Le SCORE n'est
            // JAMAIS écrit ici : il reste calculé par SupplierScoringService.
            if (s.name() != null && !s.name().isBlank()) entity.setName(s.name());
            else if (entity.getName() == null) entity.setName(s.externalCode());
            if (s.countryCode() != null) entity.setCountryCode(normalizeCountry(s.countryCode()));
            if (s.email() != null) entity.setContactEmail(s.email());
            if (entity.getSupplierType() == null || s.category() != null) {
                entity.setSupplierType(mapSupplierType(s.category()));
            }
            supplierRepo.save(entity);
            imported++;
        }
        return new int[]{imported, ignored};
    }

    /** Catégorie ERP brute → SupplierType (défaut OTHER). Aucune logique sectorielle dure. */
    static SupplierType mapSupplierType(String category) {
        if (category == null || category.isBlank()) return SupplierType.OTHER;
        String c = category.toUpperCase(Locale.ROOT);
        if (c.contains("RAW") || c.contains("MATERIAL") || c.contains("MATIERE")) return SupplierType.RAW_MATERIAL;
        if (c.contains("COMPONENT") || c.contains("COMPOSANT") || c.contains("PART")) return SupplierType.COMPONENT;
        if (c.contains("SERVICE") || c.contains("PRESTA")) return SupplierType.SERVICE;
        if (c.contains("CONTRACT") || c.contains("MANUF") || c.contains("CMO") || c.contains("OEM")) return SupplierType.CONTRACT_MANUFACTURER;
        if (c.contains("SOFT") || c.contains("SAAS") || c.contains("LICENSE")) return SupplierType.SOFTWARE;
        if (c.contains("LOGIST") || c.contains("TRANSPORT") || c.contains("FREIGHT")) return SupplierType.LOGISTICS;
        return SupplierType.OTHER;
    }

    private static String normalizeCountry(String cc) {
        if (cc == null) return null;
        String n = cc.trim().toUpperCase(Locale.ROOT);
        return n.length() > 2 ? n.substring(0, 2) : (n.isEmpty() ? null : n);
    }

    // ---------- Import: production KPIs (quality/kpi) ----------

    /** @return [imported, ignored] */
    private int[] importKpis(UUID tenantId, List<ExternalProductionKpi> kpis) {
        int imported = 0, ignored = 0;
        for (ExternalProductionKpi k : kpis) {
            if (k.kpiCode() == null || k.kpiCode().isBlank() || k.value() == null || k.periodStart() == null) {
                ignored++;
                continue;
            }
            Optional<KpiDefinition> def = kpiDefinitionRepo.findByTenantIdAndCode(tenantId, k.kpiCode());
            if (def.isEmpty()) {
                // Pas de KpiDefinition → on IGNORE en WARN. Pas de création sauvage.
                log.warn("ERP sync: unknown KPI code '{}' for tenant {}, measurement ignored",
                        k.kpiCode(), tenantId);
                ignored++;
                continue;
            }
            KpiDefinition d = def.get();
            KpiMeasurement m = kpiMeasurementRepo.findByKpiIdAndPeriodStart(d.getId(), k.periodStart())
                    .orElseGet(() -> {
                        KpiMeasurement nm = new KpiMeasurement();
                        nm.setTenantId(tenantId);
                        nm.setKpiId(d.getId());
                        nm.setPeriodStart(k.periodStart());
                        return nm;
                    });
            m.setPeriodEnd(k.periodEnd() != null ? k.periodEnd()
                    : k.periodStart().plus(1, ChronoUnit.DAYS));
            m.setValue(k.value());
            m.setUnit(k.unit() != null ? k.unit() : d.getUnit());
            m.setSource(MeasurementSource.IMPORT);
            m.setNotes("Imported from ERP connector");
            kpiMeasurementRepo.save(m);
            imported++;
        }
        return new int[]{imported, ignored};
    }

    // ---------- helpers ----------

    private ErpDto.SyncReport emptyReport(UUID connId, Instant ranAt, String msg) {
        return new ErpDto.SyncReport(connId, 0, 0, 0, 0, ranAt, msg);
    }

    private void failOnce(ErpConnection c) {
        c.setConsecutiveFailures(c.getConsecutiveFailures() + 1);
        if (c.getConsecutiveFailures() >= MAX_CONSECUTIVE_FAILURES_BEFORE_DISABLE
                && c.getStatus() == ErpConnectionStatus.ACTIVE) {
            c.setStatus(ErpConnectionStatus.DISABLED_ON_ERRORS);
            log.warn("ERP connection {} auto-disabled after {} consecutive failures",
                    c.getId(), c.getConsecutiveFailures());
        }
    }

    private void ensureSameTenant(ErpConnection c, UUID tenantId) {
        if (!c.getTenantId().equals(tenantId)) {
            throw new ErpConnectionNotFoundException(c.getId()); // ne pas leaker cross-tenant
        }
    }

    private ErpDto.ConnectionResponse toResponse(ErpConnection c) {
        return new ErpDto.ConnectionResponse(
                c.getId(), c.getTenantId(), c.getName(), c.getProvider(),
                c.getBaseUrl(), c.getUsername(), c.getExternalScope(),
                c.getStatus(), c.getConsecutiveFailures(),
                c.getLastSyncAt(), c.getLastSuccessAt(),
                c.getCreatedBy(), c.getCreatedAt(), c.getUpdatedAt());
    }

    // exposé pour les tests
    Map<ErpProvider, ErpProviderClient> clientsView() { return new HashMap<>(clients); }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
