package com.openlab.qualitos.quality.visiongateway;

/**
 * Image fournie invalide (partie multipart manquante/vide, content-type non whitelisté,
 * signature binaire incohérente). Mappé en {@code 400 Bad Request}
 * (type {@code vision-image-invalid}) par le {@code GlobalExceptionHandler}.
 */
public class VisionImageValidationException extends RuntimeException {
    public VisionImageValidationException(String message) { super(message); }
}
