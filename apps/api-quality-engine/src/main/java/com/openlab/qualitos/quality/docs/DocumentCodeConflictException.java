package com.openlab.qualitos.quality.docs;

public class DocumentCodeConflictException extends RuntimeException {
    public DocumentCodeConflictException(String code) {
        super("Document code already exists in this tenant: " + code);
    }
}
