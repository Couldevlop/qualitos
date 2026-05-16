package com.openlab.qualitos.quality.automateddecisions.domain;

/**
 * Bases autorisées pour la décision automatisée à effet juridique (Art. 22.2) :
 * <ul>
 *   <li>EXPLICIT_CONSENT — consentement explicite de la personne concernée.</li>
 *   <li>CONTRACTUAL_NECESSITY — nécessaire à la conclusion ou à l'exécution
 *       d'un contrat entre la personne et le responsable.</li>
 *   <li>AUTHORIZED_BY_LAW — autorisée par le droit de l'Union ou de l'État
 *       membre auquel le responsable est soumis.</li>
 * </ul>
 */
public enum Art22LawfulBasis {
    EXPLICIT_CONSENT,
    CONTRACTUAL_NECESSITY,
    AUTHORIZED_BY_LAW
}
