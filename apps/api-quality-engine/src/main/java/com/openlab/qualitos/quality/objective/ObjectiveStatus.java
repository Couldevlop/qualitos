package com.openlab.qualitos.quality.objective;

/**
 * Où en est un objectif face à son KPI réel.
 *
 * <p>Même lecture que {@code KpiHealth} — atteint, en alerte, critique, pas
 * mesuré — et c'est voulu : l'écran de revue montre les deux côte à côte, et
 * deux échelles divergentes y auraient fait dire au même chiffre deux choses
 * différentes.
 *
 * <p>Ce que l'objectif ajoute, c'est la MÉMOIRE : un objectif qui a déclenché
 * une CAPA ne repasse pas {@code ON_TARGET} au premier mois redevenu bon, mais
 * seulement quand le KPI est resté sous la cible pendant la durée que l'objectif
 * s'est donnée ({@code recoveryPeriods}). Un retour au vert sur un seul relevé
 * n'est pas une preuve d'efficacité, c'est une fluctuation.
 */
public enum ObjectiveStatus {

    /** Aucune mesure sur le KPI : il n'y a rien à comparer. */
    NOT_EVALUATED,

    /** Le KPI est du bon côté de la cible, et le reste depuis assez longtemps. */
    ON_TARGET,

    /** Le KPI a quitté la cible, sans franchir le seuil critique. */
    WARNING,

    /** Le KPI a franchi le seuil critique. */
    CRITICAL
}
