package com.openlab.qualitos.quality.capa.effectiveness.web;

import com.openlab.qualitos.quality.capa.effectiveness.application.CapaEffectivenessDto;
import com.openlab.qualitos.quality.capa.effectiveness.application.CapaEffectivenessService;
import com.openlab.qualitos.quality.capa.effectiveness.application.EffectivenessBoardDto;
import com.openlab.qualitos.quality.capa.effectiveness.application.EffectivenessBoardService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

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
    private final EffectivenessBoardService board;

    public CapaEffectivenessController(CapaEffectivenessService service,
                                       EffectivenessBoardService board) {
        this.service = service;
        this.board = board;
    }

    @GetMapping
    public CapaEffectivenessDto.Summary measure(
            @RequestParam(defaultValue = "6") @Min(1) @Max(24) int months) {
        return service.measure(months);
    }

    /**
     * Le plan de travail des évaluations (ADR 0108) : ce qui est à juger, ce qui
     * a été jugé, et les quatre chiffres de l'en-tête.
     *
     * <p>Aucun paramètre, et c'est voulu. Un filtre de statut existait au
     * brouillon ; il a été retiré parce que le tri par urgence fait le travail
     * mieux qu'un filtre — « ce qui est à juger » arrive en tête de la liste, et
     * personne n'a à savoir quel statut demander pour l'obtenir.
     *
     * <p>Lecture ouverte à tout utilisateur authentifié du client, mais pas sur
     * tous les dossiers : la portée du registre CAPA s'applique telle quelle
     * (ADR 0081). Sans « voir tous les dossiers », on ne voit ici que ce qu'on
     * voit sur la liste — ce qu'on pilote, ce qu'on vérifie, ce dont une action
     * nous est confiée. Un écran de synthèse qui en montrerait davantage que la
     * liste dont il est la synthèse serait le chemin détourné qui vide la règle.
     * Le client, lui, vient du jeton, jamais d'un paramètre (CLAUDE.md
     * §18.2 #2).
     */
    @GetMapping("/board")
    public EffectivenessBoardDto.Summary board() {
        return board.board();
    }

    /**
     * L'historique complet des évaluations d'un dossier, du plus récent au plus
     * ancien.
     *
     * <p>Séparé du plan de travail, qui n'en porte que la dernière : afficher
     * tous les historiques de tous les dossiers chargerait, pour un tableau,
     * l'intégralité d'une table qui ne cesse de grossir. Celui-ci se demande
     * dossier par dossier, quand on ouvre la fiche.
     *
     * <p>Un dossier hors de portée répond 404, comme sa propre fiche
     * (ADR 0081) : l'identifiant d'un dossier ne doit pas suffire à lire qui a
     * jugé, contre quel critère et avec quels relevés.
     */
    @GetMapping("/cases/{capaId}/reviews")
    public List<EffectivenessBoardDto.Review> history(@PathVariable UUID capaId) {
        return board.history(capaId);
    }
}
