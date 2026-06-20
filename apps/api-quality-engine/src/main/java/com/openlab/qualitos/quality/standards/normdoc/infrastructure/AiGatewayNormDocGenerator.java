package com.openlab.qualitos.quality.standards.normdoc.infrastructure;

import com.openlab.qualitos.quality.aigateway.AiCompletionResult;
import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.standards.normdoc.domain.GeneratedNormDoc;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocGenerationCommand;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocGenerator;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocSection;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adapter du port {@link NormDocGenerator} : rédige le document section par
 * section via la passerelle IA réelle ({@link AiGatewayClient} → ai-service,
 * §18.2 #4 : redaction PII + bouclier injection appliqués par la passerelle).
 * Aucun document en dur ; le tenant provient du JWT côté passerelle (jamais du
 * body, §18.2 #2).
 */
@Component
public class AiGatewayNormDocGenerator implements NormDocGenerator {

    /** Borne de tokens par section (latence CPU raisonnable, cf. ADR 0014). */
    private static final int MAX_TOKENS_PER_SECTION = 320;

    private static final Map<NormDocKind, String> KIND_LABEL = Map.of(
            NormDocKind.MANUAL, "Manuel Qualité",
            NormDocKind.POLICY, "Politique Qualité",
            NormDocKind.PROCEDURE, "Procédure documentée");

    private final AiGatewayClient ai;

    public AiGatewayNormDocGenerator(AiGatewayClient ai) {
        this.ai = ai;
    }

    @Override
    public GeneratedNormDoc generate(NormDocGenerationCommand command) {
        String kindLabel = KIND_LABEL.get(command.kind());
        String system = "Tu es un expert qualité QualitOS. Rédige en " + command.language()
                + " le contenu d'UNE section d'un " + kindLabel + " conforme à la norme "
                + command.standardName() + " (" + command.standardCode() + "). Style : Markdown "
                + "structuré (sous-titres, listes, tableaux si utile), concis et directement "
                + "exploitable. Marque tout élément à personnaliser par [[à compléter]]. "
                + "N'invente AUCUNE exigence hors de la norme citée. Ne répète pas le titre de la "
                + "section ; réponds uniquement avec le corps de la section.";

        String context = buildContext(command);

        List<NormDocSection> sections = new ArrayList<>();
        String provider = "";
        for (NormDocGenerationCommand.SectionRequest s : command.sections()) {
            String user = buildSectionPrompt(context, kindLabel, s);
            AiCompletionResult r = ai.complete(system, user, MAX_TOKENS_PER_SECTION);
            provider = r.provider();
            String body = r.text() == null ? "" : r.text().strip();
            sections.add(new NormDocSection(s.key(), s.title(), s.clauses(), body));
        }

        String title = kindLabel + " — " + command.organizationName()
                + " (" + command.standardCode() + ")";
        return new GeneratedNormDoc(title, sections, provider);
    }

    private static String buildContext(NormDocGenerationCommand c) {
        StringBuilder sb = new StringBuilder()
                .append("Organisation : ").append(c.organizationName()).append("\n")
                .append("Secteur : ").append(c.industry()).append("\n")
                .append("Taille : ").append(c.size());
        if (!c.knownProcesses().isEmpty()) {
            sb.append("\nProcessus connus : ").append(String.join(", ", c.knownProcesses()));
        }
        return sb.toString();
    }

    private static String buildSectionPrompt(String context, String kindLabel,
                                             NormDocGenerationCommand.SectionRequest s) {
        StringBuilder sb = new StringBuilder()
                .append("Contexte de l'organisation :\n").append(context).append("\n")
                .append("Document : ").append(kindLabel).append(".\n")
                .append("Section à rédiger : ").append(s.title()).append(".");
        if (!s.clauses().isEmpty()) {
            sb.append("\nClauses couvertes : ").append(String.join(", ", s.clauses())).append(".");
        }
        if (!s.guidance().isBlank()) {
            sb.append("\nConsigne : ").append(s.guidance());
        }
        sb.append("\nRédige le corps de cette section.");
        return sb.toString();
    }
}
