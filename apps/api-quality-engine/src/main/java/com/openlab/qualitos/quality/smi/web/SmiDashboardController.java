package com.openlab.qualitos.quality.smi.web;

import com.openlab.qualitos.quality.smi.application.SmiDashboardService;
import com.openlab.qualitos.quality.smi.application.SmiDto;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Le tableau de bord du système de management intégré.
 *
 * <p>Lecture seule, ouverte à tout utilisateur authentifié comme le tableau de
 * bord exécutif : chacun voit l'état du système de management de son client.
 * Le client vient du jeton ; rien dans la requête ne le désigne.
 */
@RestController
@Validated
@RequestMapping("/api/v1/smi")
public class SmiDashboardController {

    private final SmiDashboardService service;

    public SmiDashboardController(SmiDashboardService service) {
        this.service = service;
    }

    /** @param standard le code d'une norme adoptée ({@code iso-9001}) ; absent : toutes. */
    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public SmiDto.Dashboard dashboard(
            @RequestParam(required = false) @Size(max = 64) @Pattern(regexp = "[a-zA-Z0-9-]*") String standard) {
        return service.dashboard(standard);
    }

    @GetMapping("/requirements-matrix")
    @Transactional(readOnly = true)
    public SmiDto.RequirementsMatrix requirementsMatrix() {
        return service.requirementsMatrix();
    }
}
