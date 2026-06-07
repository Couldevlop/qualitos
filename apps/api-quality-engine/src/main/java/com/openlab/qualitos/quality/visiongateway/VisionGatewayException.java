package com.openlab.qualitos.quality.visiongateway;

/**
 * Échec de relais vers {@code ai-vision-5s} qui n'est pas une simple indisponibilité :
 * le service a répondu par une erreur (5xx en aval) ou une réponse invalide/vide.
 * Mappé en {@code 502 Bad Gateway} (type {@code vision-gateway-error}) par le
 * {@code GlobalExceptionHandler}, dans le même esprit que {@code AiGatewayException}.
 */
public class VisionGatewayException extends RuntimeException {
    public VisionGatewayException(String message) { super(message); }
    public VisionGatewayException(String message, Throwable cause) { super(message, cause); }
}
