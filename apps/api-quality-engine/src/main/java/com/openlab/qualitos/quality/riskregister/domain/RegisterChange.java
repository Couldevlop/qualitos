package com.openlab.qualitos.quality.riskregister.domain;

/**
 * Un changement que la révision d'une fiche signale au journal de suivi.
 *
 * <p>Seuls les changements qui racontent l'histoire de la fiche — cotation,
 * statut, décision — en produisent un. Corriger une faute dans la cause n'en
 * produit pas : le suivi deviendrait illisible.
 */
public record RegisterChange(RegisterEventType type, String from, String to) {
}
