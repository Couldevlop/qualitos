package com.openlab.qualitos.quality.standards;

import com.openlab.qualitos.quality.aigateway.AiCompletionResult;
import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Génère par LLM (via la passerelle {@link AiGatewayClient} → ai-service) un brouillon
 * de document normatif à partir d'un modèle du catalogue (Standards Hub §8.4 onglet 3,
 * §8.8 « génération IA assistée des documents normatifs »). L'humain valide ensuite
 * (aucun document publié sans revue + signature).
 */
@Service
public class AiDraftService {

    /** Borne le nombre de tokens (latence CPU raisonnable ; cf. ADR 0014). */
    private static final int MAX_TOKENS = 320;

    private final StandardDocumentTemplateRepository templates;
    private final AiGatewayClient ai;

    public AiDraftService(StandardDocumentTemplateRepository templates, AiGatewayClient ai) {
        this.templates = templates;
        this.ai = ai;
    }

    @Transactional(readOnly = true)
    public StandardsDto.AiDraftResponse generate(UUID standardId, UUID templateId) {
        StandardDocumentTemplate t = templates.findByIdAndStandardId(templateId, standardId)
                .orElseThrow(() -> new DocumentTemplateNotFoundException(templateId));
        Standard s = t.getStandard();

        String system = "Tu es un expert qualité QualitOS. Rédige en français un BROUILLON de "
                + "document normatif, en Markdown structuré (titres, listes, tableaux si utile), "
                + "concis et directement exploitable. Marque les éléments à personnaliser par "
                + "[[à compléter]]. N'invente pas d'exigences hors de la norme citée.";

        StringBuilder user = new StringBuilder()
                .append("Norme : ").append(s.getFullName()).append(" (").append(s.getCode()).append(").\n")
                .append("Document à rédiger : ").append(t.getName());
        if (t.getCategory() != null) user.append(" [").append(t.getCategory()).append("]");
        if (t.getMapsToClauses() != null) user.append("\nClauses couvertes : ").append(t.getMapsToClauses());
        if (t.getDescription() != null) user.append("\nObjet : ").append(t.getDescription());
        user.append("\nProduis le brouillon de ce document.");

        AiCompletionResult r = ai.complete(system, user.toString(), MAX_TOKENS);
        return new StandardsDto.AiDraftResponse(
                t.getId(), t.getCode(), t.getName(), r.text(), r.provider(), r.latencyMs());
    }
}
