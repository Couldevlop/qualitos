package com.openlab.qualitos.quality.workflow;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Contenu de workflow invalide non couvert par la validation Jakarta du DTO
 * (ex. XML BPMN dépassant la taille maximale autorisée) → 400.
 * Mappée via {@link ResponseStatus} pour ne PAS toucher au GlobalExceptionHandler partagé.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class WorkflowValidationException extends RuntimeException {
    public WorkflowValidationException(String message) {
        super(message);
    }
}
