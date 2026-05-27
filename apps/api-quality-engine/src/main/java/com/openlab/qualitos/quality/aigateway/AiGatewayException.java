package com.openlab.qualitos.quality.aigateway;

/** Échec d'appel à la passerelle IA (ai-service indisponible, timeout, réponse invalide). */
public class AiGatewayException extends RuntimeException {
    public AiGatewayException(String message) { super(message); }
    public AiGatewayException(String message, Throwable cause) { super(message, cause); }
}
