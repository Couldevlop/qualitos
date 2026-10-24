package com.openlab.qualitos.quality.dmaic.resolution.application;

import com.openlab.qualitos.quality.dmaic.resolution.domain.FailureType;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionContent;
import com.openlab.qualitos.quality.dmaic.resolution.domain.SeverityLevel;
import com.openlab.qualitos.quality.dmaic.resolution.domain.SheetQuestion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Les vues que rend le service de la fiche de résolution. */
public final class ResolutionDto {

    private ResolutionDto() {}

    /**
     * Le paramétrage en vigueur chez le client.
     *
     * @param defaults vrai quand ce sont les valeurs de la plateforme, faute de réglage
     */
    public record SettingsView(List<SheetQuestion> questions, List<FailureType> failureTypes,
                               List<SeverityLevel> severities, boolean defaults) {
    }

    /**
     * La fiche d'un projet, avec le paramétrage qui la met en forme.
     *
     * @param saved     faux tant que personne n'a enregistré la fiche : l'écran
     *                  montre alors une fiche vierge, rien n'existe en base
     * @param answers   réponses aux questions que le catalogue pose aujourd'hui ;
     *                  celles des questions retirées restent en base sans être rendues
     */
    public record SheetView(UUID projectId, String projectTitle, String problemStatement, boolean saved,
                            ResolutionContent content, SettingsView settings, List<PhotoView> photos,
                            Instant updatedAt) {
    }

    /** Une photo de la fiche ; {@code url} est une adresse de lecture à durée de vie courte. */
    public record PhotoView(int slot, String contentType, long sizeBytes, String remark, String url) {
    }
}
