package com.openlab.qualitos.quality.fives;

import java.util.UUID;

/** Photo introuvable, ou prise sur un autre audit — 404 dans les deux cas. */
public class FiveSItemPhotoNotFoundException extends RuntimeException {

    public FiveSItemPhotoNotFoundException(UUID id) {
        super("5S item photo " + id + " not found");
    }
}
