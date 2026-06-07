package com.openlab.qualitos.keycloak.ldap;

/**
 * Signale que l'annuaire LDAP est injoignable (timeout, connexion refusée, bind
 * de service échoué pour cause d'infra). Distincte d'une "absence d'utilisateur" :
 * elle déclenche le <strong>fallback transparent</strong> (le provider renvoie
 * {@code null}/false sans propager d'exception, laissant Keycloak déléguer aux
 * autres providers / au stockage local).
 */
public class LdapUnavailableException extends RuntimeException {

    public LdapUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
