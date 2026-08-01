package com.openlab.qualitos.quality.dashboards.executive;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard exécutif (CLAUDE.md §7.1).
 *
 * <p>Une seule route, en lecture : la vue de direction est une page, donc un aller-retour.
 * Le tenant est dérivé du JWT côté service (règle §18.2 #2) ; la route est accessible à
 * tout utilisateur authentifié du tenant — c'est une vue agrégée en lecture seule, dont
 * chaque brique est déjà filtrée par tenant en base.
 */
@RestController
@RequestMapping("/api/v1/dashboards/executive")
public class ExecutiveDashboardController {

    private final ExecutiveDashboardService service;

    public ExecutiveDashboardController(ExecutiveDashboardService service) {
        this.service = service;
    }

    @GetMapping
    public ExecutiveDashboardDto.ExecutiveDashboard overview() {
        return service.overview();
    }
}
