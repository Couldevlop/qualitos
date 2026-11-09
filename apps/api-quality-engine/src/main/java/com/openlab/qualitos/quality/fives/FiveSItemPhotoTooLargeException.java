package com.openlab.qualitos.quality.fives;

/**
 * Plafond atteint : par photo, par pilier, ou pour l'audit entier.
 *
 * <p>413 et non 400 : la requête est valide, c'est sa TAILLE qui est refusée, et
 * le message dit lequel des trois plafonds a parlé — sans quoi l'auditeur
 * recommence avec une photo plus petite alors que c'est la sixième du pilier.
 */
public class FiveSItemPhotoTooLargeException extends RuntimeException {

    public FiveSItemPhotoTooLargeException(String message) {
        super(message);
    }
}
