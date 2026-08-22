package com.openlab.qualitos.quality.capa.effectiveness.application;

import com.openlab.qualitos.quality.capa.effectiveness.domain.RecurrenceSignature;

import java.time.Instant;
import java.util.UUID;

/**
 * Port — le comptage des non-conformités correspondant à une signature, sur un
 * intervalle.
 *
 * <p>Les bornes sont celles de la DÉTECTION du défaut, pas de sa saisie : une
 * non-conformité constatée en mars et enregistrée en mai appartient à mars, et
 * la compter en mai déplacerait la récidive hors de la fenêtre qui la concerne.
 */
public interface NcOccurrencePort {

    int countBetween(UUID tenantId, RecurrenceSignature signature, Instant from, Instant to);
}
