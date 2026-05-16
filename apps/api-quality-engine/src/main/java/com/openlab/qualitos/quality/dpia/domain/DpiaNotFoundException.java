package com.openlab.qualitos.quality.dpia.domain;

import java.util.UUID;

public class DpiaNotFoundException extends RuntimeException {
    public DpiaNotFoundException(UUID id) {
        super("DPIA not found: " + id);
    }
}
