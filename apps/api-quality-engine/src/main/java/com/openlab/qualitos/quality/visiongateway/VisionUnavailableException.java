package com.openlab.qualitos.quality.visiongateway;

/**
 * Le service de vision {@code ai-vision-5s} est indisponible : flag
 * {@code qualitos.vision.enabled} à OFF (désactivé), connexion refusée, timeout ou
 * service injoignable. Mappé en {@code 503 Service Unavailable} (type
 * {@code vision-unavailable}) par le {@code GlobalExceptionHandler}.
 */
public class VisionUnavailableException extends RuntimeException {
    public VisionUnavailableException(String message) { super(message); }
    public VisionUnavailableException(String message, Throwable cause) { super(message, cause); }
}
