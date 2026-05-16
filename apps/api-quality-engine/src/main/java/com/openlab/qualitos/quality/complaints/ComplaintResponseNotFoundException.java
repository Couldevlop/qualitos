package com.openlab.qualitos.quality.complaints;

import java.util.UUID;

public class ComplaintResponseNotFoundException extends RuntimeException {
    public ComplaintResponseNotFoundException(UUID id) {
        super("Complaint response not found: " + id);
    }
}
