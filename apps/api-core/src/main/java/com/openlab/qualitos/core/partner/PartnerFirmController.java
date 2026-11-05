package com.openlab.qualitos.core.partner;

import com.openlab.qualitos.core.common.CurrentUser;
import com.openlab.qualitos.core.edition.SaasOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Les cabinets, vus par l'éditeur.
 *
 * <p>Réservé au super administrateur, et {@link SaasOnly} : une installation
 * on-premise n'a qu'un client, et pas de portefeuille de cabinets à tenir
 * (ADR 0082). Ces routes répondent donc 404 chez un client qui héberge
 * lui-même.
 *
 * <p>Rien ici n'ouvre un accès : l'éditeur constitue le portefeuille, le client
 * décide. La décision est ailleurs, dans {@link PartnerAccessController}.
 */
@RestController
@RequestMapping("/api/v1/partner-firms")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Partner firms", description = "Cabinets partenaires — éditeur seulement")
@SaasOnly
public class PartnerFirmController {

    private final PartnerFirmService service;

    public PartnerFirmController(PartnerFirmService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List partner firms")
    public ResponseEntity<Page<PartnerDto.FirmResponse>> list(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(service.list(pageable));
    }

    @PostMapping
    @Operation(summary = "Create a partner firm")
    public ResponseEntity<PartnerDto.FirmResponse> create(
            @Valid @RequestBody PartnerDto.FirmRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping("/{firmId}")
    @Operation(summary = "Read one partner firm")
    public ResponseEntity<PartnerDto.FirmResponse> get(@PathVariable UUID firmId) {
        return ResponseEntity.ok(service.get(firmId));
    }

    @PutMapping("/{firmId}")
    @Operation(summary = "Update a partner firm")
    public ResponseEntity<PartnerDto.FirmResponse> update(
            @PathVariable UUID firmId, @Valid @RequestBody PartnerDto.FirmRequest request) {
        return ResponseEntity.ok(service.update(firmId, request));
    }

    @PutMapping("/{firmId}/active")
    @Operation(summary = "Suspend or reactivate a partner firm")
    public ResponseEntity<PartnerDto.FirmResponse> setActive(
            @PathVariable UUID firmId, @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(service.setActive(firmId, request.active()));
    }

    // ---------- consultants ----------

    @GetMapping("/{firmId}/consultants")
    @Operation(summary = "List the consultants of a firm")
    public ResponseEntity<List<PartnerDto.ConsultantResponse>> consultants(@PathVariable UUID firmId) {
        return ResponseEntity.ok(service.listConsultants(firmId));
    }

    @PostMapping("/{firmId}/consultants")
    @Operation(summary = "Add a consultant to a firm")
    public ResponseEntity<PartnerDto.ConsultantResponse> addConsultant(
            @PathVariable UUID firmId, @Valid @RequestBody PartnerDto.ConsultantRequest request) {
        return ResponseEntity.ok(service.addConsultant(firmId, request));
    }

    @PutMapping("/{firmId}/consultants/{consultantId}/active")
    @Operation(summary = "Deactivate or reactivate a consultant")
    public ResponseEntity<PartnerDto.ConsultantResponse> setConsultantActive(
            @PathVariable UUID firmId, @PathVariable UUID consultantId,
            @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(service.setConsultantActive(firmId, consultantId, request.active()));
    }

    // ---------- portefeuille ----------

    @GetMapping("/{firmId}/portfolio")
    @Operation(summary = "The firm's portfolio: proposed, granted, declined, revoked")
    public ResponseEntity<List<PartnerDto.EngagementResponse>> portfolio(@PathVariable UUID firmId) {
        return ResponseEntity.ok(service.portfolio(firmId));
    }

    @PostMapping("/{firmId}/portfolio")
    @Operation(summary = "Propose a client to the firm's portfolio (grants nothing)")
    public ResponseEntity<PartnerDto.EngagementResponse> propose(
            @PathVariable UUID firmId, @Valid @RequestBody PartnerDto.ProposeRequest request) {
        return ResponseEntity.ok(service.propose(firmId, request, CurrentUser.userId().orElse(null)));
    }

    /** Un seul drapeau : suspendre et réactiver sont le même geste, inversé. */
    public record ActiveRequest(boolean active) {}
}
