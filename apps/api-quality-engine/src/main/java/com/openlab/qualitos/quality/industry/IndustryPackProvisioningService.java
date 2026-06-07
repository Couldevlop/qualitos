package com.openlab.qualitos.quality.industry;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.kpi.KpiDefinition;
import com.openlab.qualitos.quality.kpi.KpiDefinitionRepository;
import com.openlab.qualitos.quality.kpi.KpiDirection;
import com.openlab.qualitos.quality.kpi.KpiFrequency;
import com.openlab.qualitos.quality.kpi.KpiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provisionnement du contenu d'un Industry Pack chez le tenant à l'activation (ADR 0019,
 * Phase 2). Appelé par {@link IndustryPackService#activate} <b>après</b> l'enregistrement
 * de l'activation, dans la même transaction.
 *
 * <p><b>Périmètre Phase 2</b> : seuls les <b>KPIs riches</b> (§6.6) du manifeste sont
 * matérialisés en {@link KpiDefinition} du tenant. Le glossaire, les templates Ishikawa et
 * la bibliothèque Poka-Yoke restent exposés via le {@code manifest_json} du pack sans
 * matérialisation (assumé — cf. ADR §5, section « Phase 2 — provisionnement »).
 *
 * <p><b>Idempotence</b> : un KPI est créé uniquement si <i>aucune</i> {@link KpiDefinition}
 * du tenant ne porte déjà ce {@code code} (= {@code kpi_id} du pack). Une ré-activation, ou
 * l'activation d'un second pack partageant un {@code kpi_id}, ne crée rien et n'écrase rien
 * (skip + warning sur collision pré-existante).
 *
 * <p><b>Résilience contenu</b> : l'échec de provisionnement d'<i>un</i> KPI (mapping,
 * persistance) est consigné comme {@code warning} dans le {@link ProvisioningResult} et
 * n'interrompt ni les autres KPIs, ni l'activation (pas de rollback).
 *
 * <p><b>Désactivation</b> : <i>aucune</i> suppression. Les KPIs provisionnés appartiennent
 * désormais au tenant (catalogue éditable indépendamment du pack) — cf.
 * {@link IndustryPackService#deactivate}.
 */
@Service
public class IndustryPackProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(IndustryPackProvisioningService.class);

    /** Taille de la colonne {@code kpi_definitions.description}. La description composée est tronquée. */
    private static final int DESCRIPTION_MAX = 2000;

    /**
     * Premier nombre décimal d'une chaîne libre. Gère le signe, le séparateur décimal point
     * OU virgule (FR), et les pourcentages ('>= 95 %', '< 2.5', '0,5', '80% budget').
     */
    private static final Pattern FIRST_NUMBER = Pattern.compile("-?\\d+(?:[.,]\\d+)?");

    private final KpiDefinitionRepository kpiRepo;
    private final ObjectMapper jsonMapper;

    public IndustryPackProvisioningService(KpiDefinitionRepository kpiRepo) {
        this.kpiRepo = kpiRepo;
        this.jsonMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /** Résultat additif renvoyé dans la réponse d'activation (rétro-compatible). */
    public record ProvisioningResult(int kpisCreated, int kpisSkipped, List<String> warnings) {
        public static ProvisioningResult empty() {
            return new ProvisioningResult(0, 0, List.of());
        }
    }

    /**
     * Matérialise les KPIs riches du pack {@code manifestJson} chez {@code tenantId}.
     *
     * @param tenantId  tenant cible (déjà résolu depuis le JWT par l'appelant)
     * @param createdBy utilisateur qui active (devient {@code createdBy}/{@code ownerUserId} des KPIs)
     * @param manifestJson manifeste sérialisé du pack ({@code industry_packs.manifest_json})
     * @return compteurs créés/sautés + warnings ; jamais {@code null}
     */
    public ProvisioningResult provision(UUID tenantId, UUID createdBy, String manifestJson) {
        IndustryPackManifest manifest;
        try {
            manifest = jsonMapper.readValue(manifestJson, IndustryPackManifest.class);
        } catch (Exception e) {
            log.warn("Provisioning skipped: cannot parse manifest_json: {}", e.getMessage());
            return new ProvisioningResult(0, 0,
                    List.of("manifest illisible, aucun KPI provisionné: " + e.getMessage()));
        }

        List<IndustryPackManifest.Kpi> richKpis = manifest.getRichKpis();
        // Pack PLAT (sans richKpis) : aucun provisionnement, comportement inchangé.
        if (richKpis == null || richKpis.isEmpty()) {
            return ProvisioningResult.empty();
        }

        int created = 0;
        int skipped = 0;
        List<String> warnings = new ArrayList<>();

        for (IndustryPackManifest.Kpi k : richKpis) {
            String code = k == null ? null : k.getKpiId();
            if (code == null || code.isBlank()) {
                skipped++;
                warnings.add("KPI sans kpi_id ignoré");
                continue;
            }
            // Idempotence + non-écrasement : si un KPI de ce code existe déjà chez le tenant, skip.
            if (kpiRepo.findByTenantIdAndCode(tenantId, code).isPresent()) {
                skipped++;
                warnings.add("KPI '" + code + "' déjà présent chez le tenant, conservé en l'état (skip)");
                continue;
            }
            try {
                kpiRepo.save(map(tenantId, createdBy, k));
                created++;
            } catch (RuntimeException ex) {
                // Résilience contenu : un échec n'interrompt ni les autres KPIs ni l'activation.
                skipped++;
                warnings.add("KPI '" + code + "' non provisionné: " + ex.getMessage());
                log.warn("Provisioning of KPI '{}' for tenant {} failed: {}", code, tenantId, ex.getMessage());
            }
        }
        return new ProvisioningResult(created, skipped, List.copyOf(warnings));
    }

    // ---------- mapping ----------

    private KpiDefinition map(UUID tenantId, UUID createdBy, IndustryPackManifest.Kpi k) {
        KpiDefinition d = new KpiDefinition();
        d.setTenantId(tenantId);
        d.setCode(k.getKpiId());
        d.setName(blankToNull(k.getName()) != null ? k.getName() : k.getKpiId());
        d.setCategory(k.getCategory());
        d.setUnit(k.getUnit());
        d.setDescription(buildDescription(k.getFormula(), k.getExplainability()));
        d.setTargetValue(parseFirstNumber(k.getTarget()));
        d.setThresholdWarning(parseFirstNumber(k.getThresholdWarning()));
        d.setThresholdCritical(parseFirstNumber(k.getThresholdCritical()));
        d.setDirection(deduceDirection(k.getTarget()));
        d.setFrequency(mapFrequency(k.getRefreshFrequency()));
        d.setApplicableIndustriesCsv(csv(k.getApplicableIndustries()));
        d.setOwnerUserId(createdBy); // owner par défaut = l'utilisateur qui active
        d.setCreatedBy(createdBy);
        d.setStatus(KpiStatus.DRAFT); // catalogue : le tenant active ensuite
        return d;
    }

    /** description = formula + ' — ' + explainability, tronquée à la taille colonne. */
    static String buildDescription(String formula, String explainability) {
        String f = blankToNull(formula);
        String e = blankToNull(explainability);
        String composed;
        if (f != null && e != null) composed = f + " — " + e;
        else if (f != null) composed = f;
        else composed = e; // peut être null
        if (composed == null) return null;
        composed = composed.strip();
        return composed.length() > DESCRIPTION_MAX ? composed.substring(0, DESCRIPTION_MAX) : composed;
    }

    /**
     * Premier nombre décimal de la chaîne → {@link BigDecimal}. Virgule FR normalisée en point.
     * {@code null} si la chaîne est nulle/vide ou ne contient aucun nombre (PAS d'échec).
     */
    static BigDecimal parseFirstNumber(String raw) {
        if (raw == null || raw.isBlank()) return null;
        Matcher m = FIRST_NUMBER.matcher(raw);
        if (!m.find()) return null;
        try {
            return new BigDecimal(m.group().replace(',', '.'));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Sens d'optimisation déduit de la cible : '>='/'>' ⇒ HIGHER_IS_BETTER,
     * '<='/'<' ⇒ LOWER_IS_BETTER, défaut HIGHER_IS_BETTER (cf. enum, @PrePersist KpiDefinition).
     */
    static KpiDirection deduceDirection(String target) {
        if (target == null) return KpiDirection.HIGHER_IS_BETTER;
        if (target.contains("<")) return KpiDirection.LOWER_IS_BETTER;
        if (target.contains(">")) return KpiDirection.HIGHER_IS_BETTER;
        return KpiDirection.HIGHER_IS_BETTER;
    }

    /**
     * {@code refresh_frequency} (texte libre du pack) → {@link KpiFrequency}. Reconnaît les
     * valeurs proches (realtime/real-time/per-event ⇒ REALTIME, annual/yearly ⇒ YEARLY…).
     * Défaut MONTHLY si aucune correspondance (jamais d'échec).
     */
    static KpiFrequency mapFrequency(String raw) {
        if (raw == null || raw.isBlank()) return KpiFrequency.MONTHLY;
        String s = raw.strip().toLowerCase().replace('-', ' ').replace('_', ' ');
        if (s.contains("real") || s.contains("per event") || s.contains("live") || s.contains("stream")) {
            return KpiFrequency.REALTIME;
        }
        if (s.contains("daily") || s.contains("day")) return KpiFrequency.DAILY;
        if (s.contains("week")) return KpiFrequency.WEEKLY;
        if (s.contains("month")) return KpiFrequency.MONTHLY;
        if (s.contains("quarter")) return KpiFrequency.QUARTERLY;
        if (s.contains("annual") || s.contains("year")) return KpiFrequency.YEARLY;
        if (s.contains("demand") || s.contains("manual") || s.contains("event")) return KpiFrequency.ON_DEMAND;
        return KpiFrequency.MONTHLY;
    }

    private static String csv(List<String> values) {
        if (values == null || values.isEmpty()) return null;
        String joined = String.join(",", values);
        return joined.length() > 1000 ? joined.substring(0, 1000) : joined;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
