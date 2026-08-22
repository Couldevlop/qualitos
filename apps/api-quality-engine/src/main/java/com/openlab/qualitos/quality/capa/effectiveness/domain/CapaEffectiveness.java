package com.openlab.qualitos.quality.capa.effectiveness.domain;

import java.time.Instant;

/**
 * Le verdict du terrain sur une CAPA close.
 *
 * <p>Objet de domaine, sans Spring ni JPA. Il porte le taux quand il vaut, et
 * les nombres qui l'ont produit dans tous les cas : un pourcentage sans ses
 * décomptes n'est pas contestable, et un indicateur qu'on ne peut pas contester
 * finit par n'être plus regardé.
 *
 * @param occurrencesBefore occurrences du même problème sur la fenêtre précédant
 *                          l'ouverture du dossier
 * @param occurrencesAfter  occurrences depuis la clôture, dans la limite de la
 *                          fenêtre
 * @param ratePercent       réduction en pourcentage, ou {@code null} quand le
 *                          statut interdit de conclure
 * @param aggravated        vrai quand il y a eu PLUS de récidives qu'avant : le
 *                          taux est alors à zéro, et c'est ce drapeau qui
 *                          distingue « sans effet » de « pire qu'avant »
 */
public record CapaEffectiveness(
        Instant closedAt,
        MeasurementStatus status,
        int occurrencesBefore,
        int occurrencesAfter,
        Integer ratePercent,
        boolean aggravated,
        int daysObserved,
        int daysInWindow) {
}
