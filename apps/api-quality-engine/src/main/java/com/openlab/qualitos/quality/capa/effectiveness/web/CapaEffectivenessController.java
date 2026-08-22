package com.openlab.qualitos.quality.capa.effectiveness.web;

import com.openlab.qualitos.quality.capa.effectiveness.application.CapaEffectivenessDto;
import com.openlab.qualitos.quality.capa.effectiveness.application.CapaEffectivenessService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * L'efficacité mesurée des CAPA closes.
 *
 * <p>Lecture seule, et ouverte à tout utilisateur authentifié du tenant : elle ne
 * révèle rien de plus que la liste des CAPA, qui l'est déjà. Le tenant vient du
 * jeton, jamais d'un paramètre (CLAUDE.md §18.2 #2).
 *
 * <p>La fenêtre est un paramètre et non une constante : six mois est l'usage,
 * mais un procédé lent — un traitement thermique, un cycle agricole — demande
 * plus long, et un poste cadencé se juge en trois mois.
 */
@RestController
@RequestMapping("/api/v1/capa/effectiveness")
@Validated
public class CapaEffectivenessController {

    private final CapaEffectivenessService service;

    public CapaEffectivenessController(CapaEffectivenessService service) {
        this.service = service;
    }

    @GetMapping
    public CapaEffectivenessDto.Summary measure(
            @RequestParam(defaultValue = "6") @Min(1) @Max(24) int months) {
        return service.measure(months);
    }
}
