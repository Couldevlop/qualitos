package com.openlab.qualitos.quality.objective;

import java.math.BigDecimal;

/**
 * Ce que dit la confrontation d'un objectif à ses mesures, à un instant donné.
 *
 * <p>Rien n'est persisté : l'appréciation se recalcule à chaque lecture depuis
 * les relevés du KPI. C'est voulu. Figer un statut d'objectif en base le ferait
 * mentir dès qu'un relevé est corrigé — et un relevé se corrige, c'est même
 * l'objet d'une bonne part du module KPI.
 *
 * @param status               où en est l'objectif
 * @param observedValue        la valeur de la dernière période, {@code null} si aucune mesure
 * @param deviationAbsolute    observée − cible, dans l'unité du KPI ; {@code null} si aucune mesure
 * @param deviationPercent     le même écart en pourcentage de la cible ; {@code null} si aucune
 *                             mesure ou si la cible est nulle — diviser par zéro ne dirait rien
 * @param warningBreaches      périodes consécutives, en finissant par la dernière, au-delà du
 *                             seuil d'alerte
 * @param criticalBreaches     périodes consécutives, en finissant par la dernière, au-delà du
 *                             seuil critique
 * @param onTargetPeriods      périodes consécutives, en finissant par la dernière, du bon côté
 *                             de la cible
 * @param triggeredLevel       le niveau que la règle de déclenchement atteint, ou {@code null}
 *                             si aucun seuil n'est franchi assez longtemps pour déclencher
 * @param recoveryAchieved     {@code true} si le KPI est resté sous la cible pendant la durée
 *                             que l'objectif s'est donnée — le critère d'efficacité de sa CAPA
 * @param heldByOpenCapa       {@code true} si le statut est MAINTENU au niveau de l'alerte parce
 *                             qu'une CAPA acceptée est encore ouverte et que le retour durable
 *                             n'est pas constaté
 * @param periodsConsidered    nombre de périodes réellement comparées
 */
public record ObjectiveAssessment(
        ObjectiveStatus status,
        BigDecimal observedValue,
        BigDecimal deviationAbsolute,
        BigDecimal deviationPercent,
        int warningBreaches,
        int criticalBreaches,
        int onTargetPeriods,
        ObjectiveLevel triggeredLevel,
        boolean recoveryAchieved,
        boolean heldByOpenCapa,
        int periodsConsidered
) {
    /** L'appréciation d'un objectif dont le KPI n'a aucun relevé. */
    public static ObjectiveAssessment notEvaluated() {
        return new ObjectiveAssessment(ObjectiveStatus.NOT_EVALUATED, null, null, null,
                0, 0, 0, null, false, false, 0);
    }
}
