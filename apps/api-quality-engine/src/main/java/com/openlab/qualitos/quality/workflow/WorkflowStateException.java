package com.openlab.qualitos.quality.workflow;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Transition de cycle de vie invalide (ex. publier une définition déjà archivée) → 409.
 * Mappée via {@link ResponseStatus} pour ne PAS toucher au GlobalExceptionHandler partagé.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class WorkflowStateException extends RuntimeException {
    public WorkflowStateException(String message) {
        super(message);
    }
}
