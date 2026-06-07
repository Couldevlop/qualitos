package com.openlab.qualitos.quality.ehrconnector;

/**
 * Mode d'authentification vers le serveur FHIR.
 *
 * <ul>
 *   <li>{@link #BASIC} : en-tête {@code Authorization: Basic base64(user:secret)}.</li>
 *   <li>{@link #BEARER} : en-tête {@code Authorization: Bearer <secret>} (jeton OAuth2 / SMART-on-FHIR).</li>
 * </ul>
 *
 * Dans les deux cas le secret est chiffré au repos (AES-GCM) et n'est déchiffré
 * qu'en mémoire au moment du sync.
 */
public enum EhrAuthMode {
    BASIC,
    BEARER
}
