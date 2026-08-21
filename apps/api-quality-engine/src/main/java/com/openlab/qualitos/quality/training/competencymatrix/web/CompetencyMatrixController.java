package com.openlab.qualitos.quality.training.competencymatrix.web;

import com.openlab.qualitos.quality.training.competencymatrix.application.CompetencyMatrixService;
import com.openlab.qualitos.quality.training.competencymatrix.domain.CompetencyGrid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * La matrice de competences du tenant.
 *
 * <p>Lecture ouverte a tout utilisateur authentifie : savoir qui sait faire
 * quoi est une information d'equipe, pas un secret. Le tenant vient du jeton,
 * jamais d'un parametre.
 */
@RestController
@RequestMapping("/api/v1/training/competencies/matrix")
public class CompetencyMatrixController {

    private final CompetencyMatrixService service;

    public CompetencyMatrixController(CompetencyMatrixService service) {
        this.service = service;
    }

    @GetMapping
    public CompetencyGrid grid() {
        return service.grid();
    }
}
