package com.openlab.qualitos.quality.capa;

import java.util.Optional;
import java.util.UUID;

/**
 * Ce que le module CAPA a besoin de savoir d'un objectif qualité, et rien de plus.
 *
 * <p><b>Pourquoi une interface ici plutôt qu'un appel direct.</b> Un dossier né
 * d'un écart d'objectif ne se clôt pas sur une déclaration : son efficacité EST
 * le retour du KPI sous la cible sur une durée définie. Le module CAPA doit donc
 * interroger l'objectif avant d'accepter un verdict qui clôt — mais le module des
 * objectifs, lui, appelle {@code CapaService} pour créer le dossier. Les faire se
 * connaître tous les deux fermerait le cycle.
 *
 * <p>La dépendance est donc inversée : le module CAPA déclare ce qu'il lui faut,
 * le module des objectifs le fournit. Aucun des deux n'importe l'autre.
 *
 * <p>Voir l'ADR 0133.
 */
public interface ObjectiveRecoveryPort {

    /**
     * L'état du retour sous la cible pour le dossier né d'un objectif.
     *
     * @param achieved        {@code true} si le KPI est resté du bon côté de la cible
     *                        pendant la durée exigée
     * @param onTargetPeriods périodes consécutives du bon côté, constatées
     * @param requiredPeriods périodes exigées par l'objectif
     * @param criterion       le critère, en mots, tel qu'il a été verrouillé à l'ouverture
     */
    record Recovery(boolean achieved, int onTargetPeriods, int requiredPeriods, String criterion) {}

    /**
     * L'état du retour sous la cible pour ce dossier, ou vide si le dossier ne
     * vient pas d'un objectif qualité.
     *
     * <p>Vide ne veut PAS dire « pas encore revenu » : cela veut dire que la
     * question ne se pose pas pour ce dossier. Les deux cas ne se confondent pas,
     * sans quoi tout dossier ordinaire deviendrait impossible à clore.
     */
    Optional<Recovery> forCase(UUID tenantId, UUID capaCaseId);
}
