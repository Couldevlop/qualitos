package com.openlab.qualitos.quality.aiact.domain;

/**
 * Classification de risque AI Act (UE 2024/1689).
 * <ul>
 *   <li>UNACCEPTABLE — pratiques interdites (Art. 5) : scoring social,
 *       manipulation cognitive, reconnaissance biométrique en temps réel
 *       dans l'espace public à des fins répressives, etc.</li>
 *   <li>HIGH — systèmes à haut risque (Annexe III) : éducation, emploi,
 *       services essentiels, application de la loi, justice, démocratie.
 *       Obligations renforcées : conformity assessment, supervision humaine,
 *       gouvernance des données, transparence, robustesse.</li>
 *   <li>LIMITED — risque limité (chatbots, deepfakes…) : obligations de
 *       transparence (Art. 50) — l'utilisateur doit savoir qu'il interagit
 *       avec une IA.</li>
 *   <li>MINIMAL_OR_NO — usage libre.</li>
 * </ul>
 */
public enum AiRiskClassification {
    UNACCEPTABLE,
    HIGH,
    LIMITED,
    MINIMAL_OR_NO;

    public boolean isProhibited()             { return this == UNACCEPTABLE; }
    public boolean requiresConformityAssessment() { return this == HIGH; }
    public boolean requiresTransparency()     { return this == HIGH || this == LIMITED; }
}
