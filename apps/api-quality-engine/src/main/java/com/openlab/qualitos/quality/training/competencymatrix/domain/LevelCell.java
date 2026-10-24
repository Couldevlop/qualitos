package com.openlab.qualitos.quality.training.competencymatrix.domain;

import java.util.UUID;

/** Une case de la matrice : le niveau d'une personne sur une compétence (0 à 4). */
public record LevelCell(UUID userId, UUID skillId, int level) {
}
