package com.openlab.qualitos.quality.standards;

import com.openlab.qualitos.quality.aigateway.AiCompletionResult;
import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Génère un « storyboard » IA (CLAUDE.md §7.4) : un court récit narratif de l'état
 * d'avancement d'une certification, synthétisé à partir des chiffres déjà calculés
 * (préparation, alignement, roadmap, écarts) par {@link StandardsService}. Texte libre
 * destiné à une revue de direction — l'IA raconte, l'humain reste décisionnaire.
 *
 * Réutilise la passerelle {@link AiGatewayClient} (→ ai-service, redaction PII +
 * bouclier injection, ADR 0014). Aucune sortie structurée à parser : on renvoie le récit.
 */
@Service
public class StoryboardService {

    private static final int MAX_TOKENS = 300;

    private final StandardsService standards;
    private final AiGatewayClient ai;

    public StoryboardService(StandardsService standards, AiGatewayClient ai) {
        this.standards = standards;
        this.ai = ai;
    }

    @Transactional(readOnly = true)
    public StandardsDto.StoryboardResponse generate(UUID adoptionId) {
        StandardsDto.AdoptionResponse adoption = standards.getAdoption(adoptionId);
        StandardsDto.AlignmentReport alignment = standards.computeAlignment(adoptionId);
        StandardsDto.AuditBlancReport audit = standards.computeAuditBlanc(adoptionId);
        StandardsDto.RoadmapSummary roadmap = standards.getRoadmap(adoptionId);

        String topGaps = audit.findings().stream().limit(3)
                .map(f -> f.requirementCode() + " (" + f.findingSeverity() + ")")
                .collect(Collectors.joining(", "));

        String system = "Tu es analyste qualité. Rédige en français un COURT récit narratif "
                + "(3 à 5 phrases, un seul paragraphe fluide, ton professionnel de revue de "
                + "direction) qui raconte l'état d'avancement de la certification à partir des "
                + "chiffres fournis : où en est-on, ce qui progresse bien, les points d'attention, "
                + "et la prochaine action recommandée. Pas de liste, pas de titres, pas de chiffres inventés.";
        String user = "Norme : " + adoption.standardName() + " (" + adoption.standardCode() + ").\n"
                + "Périmètre : " + nz(adoption.scopeDescription()) + ".\n"
                + "Préparation audit (MUST couvertes) : " + pct(audit.readinessScore())
                + " (" + audit.mustCovered() + "/" + audit.mustTotal() + ").\n"
                + "Alignement global : " + pct(alignment.overallScore()) + ".\n"
                + "Avancement roadmap : " + pct(roadmap.completionPercent())
                + " (" + roadmap.doneStages() + "/" + roadmap.totalStages() + " étapes).\n"
                + "Verdict audit blanc : " + audit.verdict() + ".\n"
                + "Écarts : " + audit.criticalGaps() + " critiques, " + audit.majorGaps()
                + " majeurs, " + audit.minorGaps() + " mineurs"
                + (topGaps.isBlank() ? "" : " ; principaux : " + topGaps) + ".\n"
                + "Rédige le récit.";

        AiCompletionResult r = ai.complete(system, user, MAX_TOKENS);
        return new StandardsDto.StoryboardResponse(
                adoptionId, adoption.standardCode(), r.text().strip(), r.provider(), Instant.now());
    }

    private String pct(double v) {
        return Math.round(v) + " %";
    }

    private String nz(String s) {
        return s == null || s.isBlank() ? "non précisé" : s;
    }
}
