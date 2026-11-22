package com.openlab.qualitos.quality.derogation.application;

import java.util.Optional;
import java.util.UUID;

/**
 * Qui agit, et sous quel nom.
 *
 * <p>Le compte vient du jeton validé — jamais du corps de la requête (§18.2 #2).
 * Le nom d'affichage, lui, peut manquer : un jeton n'en porte pas toujours.
 */
public interface ActorProvider {

    UUID requireAccountId();

    Optional<String> displayName();
}
