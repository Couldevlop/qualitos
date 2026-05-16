package com.openlab.qualitos.quality.kpi;

import java.util.UUID;

public class KpiNotFoundException extends RuntimeException {
    public KpiNotFoundException(UUID id) { super("KPI definition not found: " + id); }
}
