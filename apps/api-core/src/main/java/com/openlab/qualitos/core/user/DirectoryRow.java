package com.openlab.qualitos.core.user;

import java.util.UUID;

/**
 * Ce que l'annuaire lit d'un membre, et rien d'autre (ADR 0091).
 *
 * <p>Projection et non entité : charger un {@link AppUser} chargerait ses rôles
 * (collection lue d'emblée), une requête de plus par ligne pour des données que
 * l'annuaire ne doit de toute façon pas rendre.
 */
public record DirectoryRow(UUID id, String displayName, String email, boolean active) {}
