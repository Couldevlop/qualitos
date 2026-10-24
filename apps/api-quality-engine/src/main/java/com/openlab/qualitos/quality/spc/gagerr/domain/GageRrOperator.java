package com.openlab.qualitos.quality.spc.gagerr.domain;

import java.util.UUID;

/**
 * Un opérateur de l'étude : une personne de l'annuaire (ADR 0092), son compte
 * et son nom recopié pour l'affichage. Jamais un texte libre.
 */
public record GageRrOperator(UUID accountId, String name) {}
