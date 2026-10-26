package com.openlab.qualitos.quality.aigateway;

/**
 * L'ai-service a refusé la requête SQL produite pour une question NLQ (table hors
 * liste, instruction interdite…). Ce n'est pas une panne : la passerelle a répondu,
 * c'est la question qui n'a pas pu être traduite en requête sûre.
 */
public class NlqUntranslatableException extends RuntimeException {
    public NlqUntranslatableException(String message, Throwable cause) { super(message, cause); }
}
