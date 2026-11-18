package com.openlab.qualitos.core.mail;

/**
 * Le serveur sortant designe n'est pas joignable depuis la plateforme.
 *
 * <p>Elle etend {@link IllegalArgumentException} a dessein : le gestionnaire
 * global rend deja 400 avec le message pour cette famille. Un serveur interne
 * designe par un administrateur est une SAISIE a corriger, pas une panne -- et
 * un 500 ferait croire a un incident de la plateforme.
 */
public class MailHostRefusedException extends IllegalArgumentException {

    public MailHostRefusedException(String motif) {
        super(motif);
    }
}
