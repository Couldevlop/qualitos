package com.openlab.qualitos.quality.kpi;

import java.util.UUID;

public class KpiMeasurementNotFoundException extends RuntimeException {
    public KpiMeasurementNotFoundException(UUID id) {
        super("KPI measurement not found: " + id);
    }
}
