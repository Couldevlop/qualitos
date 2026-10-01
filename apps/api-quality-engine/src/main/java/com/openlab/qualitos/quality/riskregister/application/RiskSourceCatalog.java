package com.openlab.qualitos.quality.riskregister.application;

import com.openlab.qualitos.quality.riskregister.domain.RegisterOrigin;

import java.util.Optional;
import java.util.UUID;

/**
 * Port : les objets de la plateforme dont un risque peut naître.
 *
 * <p>Quatre portes d'entrée en plus de la saisie directe : une ligne d'AMDEC
 * dont la criticité dépasse le seuil, une non-conformité, un constat d'audit,
 * l'analyse d'impact d'un changement (MOC). Le risque ne recopie pas une
 * référence tapée à la main : le serveur relit l'objet, DANS LE CLIENT DU
 * JETON, et c'est de lui que vient la référence affichée.
 */
public interface RiskSourceCatalog {

    /** L'objet, s'il existe dans ce client ; vide sinon — inconnu et étranger se confondent. */
    Optional<SourceDraft> find(RegisterOrigin origin, UUID tenantId, UUID sourceId);

    /**
     * Ce que l'objet source propose de pré-remplir.
     *
     * <p>{@code eligible} est faux quand l'objet ne justifie pas un risque : une
     * ligne d'AMDEC sous le seuil, un constat de conformité. {@code reason} dit
     * pourquoi, en code (ex. {@code BELOW_THRESHOLD}) que l'écran traduit.
     */
    record SourceDraft(
            String originRef,
            String title,
            String cause,
            String effect,
            Integer grossSeverity,
            Integer grossProbability,
            String process,
            boolean eligible,
            String reason) {}
}
