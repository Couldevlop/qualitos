package com.openlab.qualitos.quality.audit;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
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
 * « Déclarer la non-conformité » d'un constat d'audit non conforme.
 *
 * <p>Il faut le droit de déclarer un écart, et non celui de conduire l'audit :
 * relever un constat et inscrire un écart au registre sont deux gestes, et le
 * second compte dans les indicateurs de la revue de direction.
 */
@RestController
@RequestMapping("/api/v1/audits/plans/{planId}/findings/{findingId}/nc")
public class AuditFindingNcController {

    private final AuditFindingNcService service;

    public AuditFindingNcController(AuditFindingNcService service) {
        this.service = service;
    }

    @PostMapping
    @RequiresPermission(Permission.NC_CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    public AuditFindingNcService.FindingNc declare(@PathVariable UUID planId,
                                                   @PathVariable UUID findingId,
                                                   @Valid @RequestBody AuditDto.DeclareNcRequest request) {
        return service.open(planId, findingId, request);
    }
}
