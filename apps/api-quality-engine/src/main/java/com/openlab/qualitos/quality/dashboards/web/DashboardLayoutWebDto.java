package com.openlab.qualitos.quality.dashboards.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class DashboardLayoutWebDto {
    private DashboardLayoutWebDto() {}

    public record SaveRequest(
            @NotBlank @Size(min = 2, max = 120) String name,
            @Size(max = 2000) String description,
            @NotBlank String layoutJson,
            boolean shared
    ) {}
}
