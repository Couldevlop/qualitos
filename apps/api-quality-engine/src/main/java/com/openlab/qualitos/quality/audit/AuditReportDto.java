package com.openlab.qualitos.quality.audit;

import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class AuditReportDto {

    private AuditReportDto() {}

    /**
     * Brouillon du rapport. Remplacement complet, pas un correctif : l'écran
     * envoie le formulaire tel qu'il l'affiche, et un champ vidé doit se vider.
     */
    public record DraftRequest(
            @Size(max = 20000) String summary,
            @Size(max = 10000) String strengths,
            @Size(max = 10000) String improvements,
            AuditConclusion conclusion
    ) {}

    /**
     * Chiffres clés : nombre de constats par type et score de conformité.
     * Calculés en direct sur un brouillon, figés sur un rapport émis.
     */
    public record KeyFigures(
            int majorNc,
            int minorNc,
            int observation,
            int opportunity,
            int conformity,
            int total,
            Double conformityScore
    ) {}

    public record View(
            /** {@code null} tant qu'aucun brouillon n'a été enregistré. */
            UUID id,
            UUID planId,
            AuditReportStatus status,
            String summary,
            String strengths,
            String improvements,
            AuditConclusion conclusion,
            KeyFigures keyFigures,
            Instant issuedAt,
            UUID issuedBy,
            String issuedByName,
            String contentSha256,
            Instant updatedAt
    ) {}
}
