package com.openlab.qualitos.core.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Le compte de service qui crée et règle les comptes (ADR 0079).
 *
 * <pre>
 * qualitos.identity:
 *   base-url: http://keycloak:8080/auth     # interne au cluster, jamais l'URL publique (WAF)
 *   realm: qualitos
 *   client-id: qualitos-provisioner
 *   client-secret: ${KEYCLOAK_PROVISIONER_SECRET}
 *   invitation-mode: temporary-password    # ou email, quand le realm a un SMTP
 * </pre>
 */
@ConfigurationProperties(prefix = "qualitos.identity")
public record IdentityProperties(String baseUrl, String realm, String clientId, String clientSecret,
                                 String invitationMode) {

    public IdentityProperties {
        baseUrl = baseUrl == null ? "" : baseUrl.strip().replaceAll("/+$", "");
        realm = realm == null || realm.isBlank() ? "qualitos" : realm.strip();
        clientId = clientId == null || clientId.isBlank() ? "qualitos-provisioner" : clientId.strip();
        invitationMode = invitationMode == null || invitationMode.isBlank() ? "temporary-password" : invitationMode.strip();
    }

    public boolean configured() {
        return !baseUrl.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }

    public boolean emailInvitations() {
        return "email".equalsIgnoreCase(invitationMode);
    }
}
