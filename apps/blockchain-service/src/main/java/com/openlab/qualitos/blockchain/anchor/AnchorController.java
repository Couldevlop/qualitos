package com.openlab.qualitos.blockchain.anchor;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * API interne appelée par l'engine ({@code FabricGatewayClient}). Non exposée
 * publiquement : en prod, mTLS + réseau interne. RGPD §11.3 : hashes uniquement.
 */
@RestController
@RequestMapping("/internal/v1")
public class AnchorController {

    private final FabricAnchorService fabric;

    public AnchorController(FabricAnchorService fabric) {
        this.fabric = fabric;
    }

    @PostMapping("/anchor")
    public AnchorDto.AnchorResponse anchor(@Valid @RequestBody AnchorDto.AnchorRequest req) {
        String timestamp = (req.timestamp() == null || req.timestamp().isBlank())
                ? Instant.now().toString()
                : req.timestamp();
        int eventCount = req.eventCount() == null ? 0 : req.eventCount();
        FabricAnchorService.AnchorResult r =
                fabric.anchor(req.tenantId(), req.merkleRoot(), timestamp, eventCount);
        return new AnchorDto.AnchorResponse(r.txId(), r.recordJson());
    }

    @GetMapping("/verify")
    public AnchorDto.VerifyResponse verify(@RequestParam String tenantId, @RequestParam String root) {
        return new AnchorDto.VerifyResponse(fabric.verify(tenantId, root));
    }

    /** Indisponibilité / rejet Fabric → 502 (l'engine retombe alors sur le repli signé). */
    @ExceptionHandler(FabricInvocationException.class)
    ProblemDetail handleFabric(FabricInvocationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
        pd.setTitle("Fabric Invocation Failed");
        return pd;
    }
}
