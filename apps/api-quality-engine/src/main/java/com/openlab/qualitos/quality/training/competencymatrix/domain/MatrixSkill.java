package com.openlab.qualitos.quality.training.competencymatrix.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Une compétence suivie par la matrice : une compétence du catalogue
 * ({@code training_skills}), sa catégorie, et la personne désignée pour la
 * transmettre.
 *
 * <p>Le formateur est désigné à l'ajout : une compétence à risque sans personne
 * pour la former laisse le constat sans suite. C'est un compte de l'annuaire,
 * dont le nom est recopié comme celui d'un opérateur.
 *
 * @param skillId        la compétence du catalogue
 * @param name           son intitulé
 * @param category       sa catégorie (famille), {@code null} si le catalogue n'en donne pas
 * @param trainerUserId  le compte du formateur désigné
 * @param trainerName    son nom
 * @param addedAt        l'entrée de la compétence dans la matrice
 */
public record MatrixSkill(UUID skillId, String name, String category, UUID trainerUserId, String trainerName,
                          Instant addedAt) {
}
