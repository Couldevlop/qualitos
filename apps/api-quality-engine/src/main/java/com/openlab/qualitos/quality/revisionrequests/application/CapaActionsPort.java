package com.openlab.qualitos.quality.revisionrequests.application;

import java.util.List;
import java.util.UUID;

/**
 * Les actions d'un dossier CAPA, reduites a ce que le moteur en fait.
 *
 * <p>Le drapeau {@code containment} porte la seule distinction qui compte ici :
 * une mesure d'endiguement est temporaire par definition, et la graver dans un
 * control plan serait un contresens.
 */
public interface CapaActionsPort {

    List<CapaActionSummary> actionsOf(UUID tenantId, UUID capaId);

    record CapaActionSummary(UUID id, String title, String description, boolean containment) {}
}
