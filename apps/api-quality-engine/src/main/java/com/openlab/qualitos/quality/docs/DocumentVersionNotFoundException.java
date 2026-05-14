package com.openlab.qualitos.quality.docs;

import java.util.UUID;

public class DocumentVersionNotFoundException extends RuntimeException {
    public DocumentVersionNotFoundException(UUID id) { super("Document version not found: " + id); }
}
