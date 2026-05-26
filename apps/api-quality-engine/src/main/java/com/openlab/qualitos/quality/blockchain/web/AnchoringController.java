package com.openlab.qualitos.quality.blockchain.web;

import com.openlab.qualitos.quality.blockchain.application.AnchorVerificationService;
import com.openlab.qualitos.quality.blockchain.application.AnchoringDto;
import com.openlab.qualitos.quality.blockchain.application.AnchoringService;
import com.openlab.qualitos.quality.blockchain.domain.AnchorVerificationResult;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/blockchain")
@Validated
public class AnchoringController {

    private final AnchoringService service;
    private final AnchorVerificationService verification;

    public AnchoringController(AnchoringService service, AnchorVerificationService verification) {
        this.service = service;
        this.verification = verification;
    }

    /** Déclenche un batch d'ancrage pour le tenant courant. */
    @PostMapping("/anchor/run")
    public AnchoringDto.AnchorBatchResult run(
            @RequestParam(defaultValue = "100") @Min(1) @Max(1000) int batchSize) {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        UUID tenantId = UUID.fromString(TenantContext.getTenantId());
        return service.anchorBatch(new AnchoringDto.AnchorBatchRequest(tenantId, batchSize));
    }

    /**
     * Vérifie l'intégrité d'un événement d'audit ancré, par son hash d'intégrité
     * (ADR 0012) : VERIFIED / TAMPERED / NOT_ANCHORED.
     */
    @GetMapping("/verify")
    public AnchorVerificationResult verify(@RequestParam @NotBlank String hash) {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        UUID tenantId = UUID.fromString(TenantContext.getTenantId());
        return verification.verify(tenantId, hash);
    }
}
