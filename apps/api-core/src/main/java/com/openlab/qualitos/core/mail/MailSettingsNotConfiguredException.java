package com.openlab.qualitos.core.mail;

/** On a demande un essai sur une messagerie qui n'est pas encore reglee. */
public class MailSettingsNotConfiguredException extends RuntimeException {

    public MailSettingsNotConfiguredException() {
        super("Aucune messagerie n'est reglee pour cette organisation.");
    }
}
