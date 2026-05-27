package com.openlab.qualitos.quality.standards;

import java.util.UUID;

public class DocumentTemplateNotFoundException extends RuntimeException {
    public DocumentTemplateNotFoundException(UUID id) {
        super("Standard document template not found: " + id);
    }
}
