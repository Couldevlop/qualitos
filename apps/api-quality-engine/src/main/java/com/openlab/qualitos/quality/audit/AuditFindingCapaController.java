package com.openlab.qualitos.quality.audit;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
import com.openlab.qualitos.quality.capa.CapaDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * « Créer une CAPA » sur un constat d'audit non conforme (ADR 0088).
 *
 * <p>Le corps est celui de la fenêtre de création commune ; la source est le
 * constat, fixée ici. Il faut le droit de créer une CAPA : relever un constat
 * ne suffit pas à engager un plan d'action.
 */
@RestController
@RequestMapping("/api/v1/audits/plans/{planId}/findings/{findingId}/capa")
public class AuditFindingCapaController {

    private final AuditFindingCapaService service;

    public AuditFindingCapaController(AuditFindingCapaService service) {
        this.service = service;
    }

    @PostMapping
    @RequiresPermission(Permission.CAPA_CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    public AuditFindingCapaService.FindingCapa open(@PathVariable UUID planId,
                                                    @PathVariable UUID findingId,
                                                    @Valid @RequestBody CapaDto.OpenCaseRequest request) {
        return service.open(planId, findingId, request);
    }
}
