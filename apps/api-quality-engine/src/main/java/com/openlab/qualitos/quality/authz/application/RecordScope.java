package com.openlab.qualitos.quality.authz.application;

import com.openlab.qualitos.quality.authz.domain.Permission;

import java.util.Optional;
import java.util.UUID;

/**
 * Ce que l'utilisateur courant voit du registre d'un module (ADR 0081).
 *
 * <p>Un module demande, avec l'action « voir tout » qui le concerne, s'il doit
 * restreindre ce qu'il rend. Vide : l'utilisateur voit tout le client. Sinon :
 * seulement ce qui le concerne, lui — à chaque module de dire ce que
 * « concerner » veut dire (déclarer une NC, piloter un dossier CAPA…).
 *
 * <p>Ce qu'on ne voit pas n'existe pas : une fiche hors de portée répond 404,
 * en lecture comme en écriture.
 */
public interface RecordScope {

    Optional<UUID> restrictTo(Permission viewAll);
}
