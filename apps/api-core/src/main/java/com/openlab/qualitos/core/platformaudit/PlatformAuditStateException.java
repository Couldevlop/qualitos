package com.openlab.qualitos.core.platformaudit;

/**
 * La piste d'audit ne peut pas faire ce qu'on lui demande : fenêtre de
 * vérification absurde, compteur de séquence absent.
 *
 * <p>Traduite en 400 par {@code GlobalExceptionHandler} quand elle vient d'une
 * demande mal bornée. Elle n'est pas une {@code IllegalArgumentException}
 * laissée remonter telle quelle : celle-là tombe dans le fourre-tout du
 * gestionnaire et rend un 500, c'est-à-dire « la plateforme est en panne »
 * là où le problème est « votre fenêtre commence à 0 ».
 */
public class PlatformAuditStateException extends RuntimeException {

    public PlatformAuditStateException(String message) {
        super(message);
    }
}
