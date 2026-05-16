package com.openlab.qualitos.quality.breach.domain;

/**
 * Cycle de vie d'un incident de violation de données personnelles.
 *
 * <ul>
 *   <li>DETECTED — découverte (compteur 72h démarré, Art. 33§1).</li>
 *   <li>ASSESSING — analyse en cours.</li>
 *   <li>CONTAINED — endiguement effectif. Les notifications DPA/sujets peuvent
 *       être enregistrées dans cet état ou avant le passage à CLOSED.</li>
 *   <li>CLOSED — terminal — incident clôturé.</li>
 *   <li>REJECTED — terminal — pas une violation (false positive).</li>
 * </ul>
 */
public enum BreachStatus {
    DETECTED,
    ASSESSING,
    CONTAINED,
    CLOSED,
    REJECTED
}
