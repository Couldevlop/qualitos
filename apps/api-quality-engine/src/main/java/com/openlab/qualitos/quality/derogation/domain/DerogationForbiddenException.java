package com.openlab.qualitos.quality.derogation.domain;

/**
 * Ce compte a le droit en général, mais pas sur CE dossier.
 *
 * <p>La permission dit qui peut signer des dérogations ; le dossier dit qui est
 * son manager désigné. Les deux sont nécessaires, et c'est le domaine qui porte
 * la seconde : sans elle, n'importe quel manager qualité signerait le dossier
 * confié à un autre.
 */
public class DerogationForbiddenException extends RuntimeException {

    public DerogationForbiddenException(String message) {
        super(message);
    }
}
