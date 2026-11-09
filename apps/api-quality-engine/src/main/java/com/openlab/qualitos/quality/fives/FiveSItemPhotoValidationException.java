package com.openlab.qualitos.quality.fives;

/** Photo refusée : type hors liste blanche, contenu vide, ou octets incohérents. */
public class FiveSItemPhotoValidationException extends RuntimeException {

    public FiveSItemPhotoValidationException(String message) {
        super(message);
    }
}
