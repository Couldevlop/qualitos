package com.openlab.qualitos.quality.capa.effectiveness.application;

import com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessReview;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Port — l'historique des évaluations d'efficacité (ADR 0108).
 *
 * <p>Append-only, et sans méthode de modification : une évaluation
 * s'ajoute, jamais ne se corrige. Un jugement qu'on peut réécrire ne prouve
 * rien, et c'est exactement ce qu'un auditeur vient contrôler. Un verdict
 * qu'on regrette se reprend en évaluant de nouveau — la suite des deux se lit,
 * et c'est l'intérêt.
 */
public interface EffectivenessReviewPort {

    void save(EffectivenessReview review);

    /** L'historique d'un dossier, du plus récent au plus ancien. */
    List<EffectivenessReview> findByCase(UUID tenantId, UUID capaCaseId);

    /** La dernière évaluation d'un dossier, s'il y en a une. */
    Optional<EffectivenessReview> findLastByCase(UUID tenantId, UUID capaCaseId);

    /**
     * La dernière évaluation de chaque dossier d'un tenant, indexée par dossier.
     *
     * <p>Une seule requête plutôt qu'une par ligne du tableau : le plan de
     * travail affiche des dizaines de dossiers, et poser une question par ligne
     * transformerait l'affichage d'un écran en dizaines d'allers-retours en base.
     */
    Map<UUID, EffectivenessReview> findLastPerCase(UUID tenantId);

    /**
     * Le nombre d'évaluations de chaque dossier d'un tenant, indexé par dossier.
     *
     * <p>Les dossiers jamais évalués sont absents de la carte plutôt que
     * présents à zéro : l'appelant lit un défaut, et une entrée par dossier du
     * registre coûterait de la mémoire pour dire « rien ».
     *
     * <p>Séparé de {@link #findLastPerCase} bien que la donnée vienne de la même
     * table : un compte et une dernière ligne ne se ramènent pas par la même
     * requête, et les mêler obligerait à rapatrier tout l'historique pour en
     * compter les éléments.
     */
    Map<UUID, Integer> countPerCase(UUID tenantId);
}
