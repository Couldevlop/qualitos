package com.openlab.qualitos.quality.supplier;

import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Prédiction de risque fournisseur (CLAUDE.md §4.6 et §6.5).
 *
 * <p>Le modèle vit dans {@code ai-service} ({@code POST /v1/ai/predict/supplier-risk}),
 * qui expose un scoring pondéré explicable. Il était livré mais n'avait AUCUN appelant :
 * la promesse « scoring de risque fournisseur, prédiction de défaillance, alertes
 * proactives » restait lettre morte.
 *
 * <p>Le rôle de ce service est de traduire les données réelles du fournisseur en
 * caractéristiques attendues par le modèle. Aucun calcul de risque n'est fait ici : le
 * moteur reste unique et l'explicabilité (§12.3) provient de ses propres pondérations.
 *
 * <p>À distinguer de {@link SupplierScoringService}, qui calcule le score de QUALITÉ
 * observé (rétrospectif, déterministe, stocké sur le fournisseur). Celui-ci prédit un
 * risque à venir et n'est pas persisté.
 */
@Service
public class SupplierRiskPredictionService {

    /** Fenêtre d'observation des non-conformités récentes, en jours. */
    static final int RECENT_WINDOW_DAYS = 90;

    /** Au-delà, l'ancienneté du dernier audit cesse d'aggraver le risque. */
    static final int AUDIT_AGE_CAP_DAYS = 730;

    private final SupplierRepository supplierRepo;
    private final SupplierNonConformityRepository ncRepo;
    private final SupplierAuditRecordRepository auditRepo;
    private final AiGatewayClient ai;
    private final Clock clock;

    @Autowired
    public SupplierRiskPredictionService(SupplierRepository supplierRepo,
                                         SupplierNonConformityRepository ncRepo,
                                         SupplierAuditRecordRepository auditRepo,
                                         AiGatewayClient ai) {
        this(supplierRepo, ncRepo, auditRepo, ai, Clock.systemUTC());
    }

    /** Variante testable : horloge injectée (ancienneté du dernier audit). */
    SupplierRiskPredictionService(SupplierRepository supplierRepo,
                                  SupplierNonConformityRepository ncRepo,
                                  SupplierAuditRecordRepository auditRepo,
                                  AiGatewayClient ai,
                                  Clock clock) {
        this.supplierRepo = supplierRepo;
        this.ncRepo = ncRepo;
        this.auditRepo = auditRepo;
        this.ai = ai;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SupplierDto.RiskPrediction predict(UUID supplierId) {
        Supplier supplier = supplierRepo.findById(supplierId)
                .orElseThrow(() -> new SupplierNotFoundException(supplierId));

        Map<String, Double> features = buildFeatures(supplier);
        Map<String, Object> response = ai.predictSupplierRisk(features);

        return SupplierDto.RiskPrediction.from(supplier.getId(), features, response);
    }

    /**
     * Traduit l'historique du fournisseur en caractéristiques du modèle.
     *
     * <p>Seules les caractéristiques réellement mesurables sont transmises : le modèle
     * les accepte partiellement, et envoyer une valeur par défaut pour une donnée que
     * la plateforme ne possède pas (taux de livraison en retard, par exemple)
     * fabriquerait un signal. Mieux vaut un modèle qui ignore une dimension qu'un
     * modèle nourri d'une invention.
     */
    Map<String, Double> buildFeatures(Supplier supplier) {
        LocalDate today = LocalDate.now(clock);
        UUID id = supplier.getId();

        long open = ncRepo.countBySupplierIdAndStatus(id, NonConformityStatus.OPEN)
                + ncRepo.countBySupplierIdAndStatus(id, NonConformityStatus.IN_REVIEW);
        long resolved = ncRepo.countBySupplierIdAndStatus(id, NonConformityStatus.RESOLVED);
        long total = open + resolved;

        long recent = ncRepo.countBySupplierIdAndStatusAndDetectedOnAfter(
                id, NonConformityStatus.OPEN, today.minusDays(RECENT_WINDOW_DAYS));

        Map<String, Double> features = new LinkedHashMap<>();

        // Part des non-conformités encore ouvertes : le modèle attend un ratio 0..1.
        if (total > 0) {
            features.put("nc_rate", (double) open / total);
        }

        // Tendance : proportion des NC ouvertes qui sont récentes. Une valeur élevée
        // signale une dégradation en cours plutôt qu'un passif ancien.
        if (open > 0) {
            features.put("nc_trend", (double) recent / open);
        }

        // Score qualité observé, ramené en 0..1. Le modèle attend un axe où la valeur
        // haute est BONNE : il applique lui-même l'inversion via sa pondération.
        features.put("audit_score", supplier.getScore() / 100.0);

        // Ancienneté du dernier audit. Un fournisseur jamais audité est traité comme
        // au plafond : c'est le cas le plus défavorable, et il est vrai.
        LocalDate lastAudit = auditRepo.findLatestAuditDate(id).orElse(supplier.getLastAuditAt());
        double ageRatio = lastAudit == null
                ? 1.0
                : Math.min(1.0, (double) java.time.temporal.ChronoUnit.DAYS.between(lastAudit, today)
                        / AUDIT_AGE_CAP_DAYS);
        features.put("days_since_last_audit", Math.max(0.0, ageRatio));

        return features;
    }
}
