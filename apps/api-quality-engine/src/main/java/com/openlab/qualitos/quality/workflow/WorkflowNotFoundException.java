package com.openlab.qualitos.quality.workflow;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Définition de workflow introuvable (ou hors du tenant courant) → 404.
 * Mappée via {@link ResponseStatus} pour ne PAS toucher au GlobalExceptionHandler partagé.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class WorkflowNotFoundException extends RuntimeException {
    public WorkflowNotFoundException(UUID id) {
        super("Workflow definition not found: " + id);
    }
}
