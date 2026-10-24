package com.openlab.qualitos.quality.dmaic.resolution.domain;

/** Le projet, ou la photo, n'existe pas chez CE client (404). */
public class ResolutionNotFoundException extends RuntimeException {

    public ResolutionNotFoundException(String message) {
        super(message);
    }
}
