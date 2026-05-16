package com.openlab.qualitos.quality.nis2measures.domain;

import java.util.UUID;

public class Nis2MeasureNotFoundException extends RuntimeException {
    public Nis2MeasureNotFoundException(UUID id) {
        super("NIS2 measure not found: " + id);
    }
}
