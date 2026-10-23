package com.openlab.qualitos.quality.training.competencymatrix.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Une personne suivie par la matrice : un compte de l'annuaire (le {@code sub}
 * du jeton), son nom recopié au moment du choix, et son atelier.
 *
 * <p>Le nom est recopié pour que la matrice reste lisible même quand la
 * personne quitte l'annuaire ; l'identité de référence reste le compte.
 *
 * @param id       l'inscription dans la matrice (pas la personne)
 * @param userId   le compte de connexion de la personne
 * @param name     le nom affiché
 * @param workshop l'atelier de rattachement
 * @param addedAt  l'inscription dans la matrice
 */
public record MatrixOperator(UUID id, UUID userId, String name, String workshop, Instant addedAt) {
}
