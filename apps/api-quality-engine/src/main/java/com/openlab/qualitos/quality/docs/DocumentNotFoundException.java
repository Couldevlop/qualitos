package com.openlab.qualitos.quality.docs;

import java.util.UUID;

public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(UUID id) { super("Document not found: " + id); }
}
