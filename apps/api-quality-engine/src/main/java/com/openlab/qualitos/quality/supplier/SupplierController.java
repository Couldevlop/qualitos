package com.openlab.qualitos.quality.supplier;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierController {

    private final SupplierService service;

    public SupplierController(SupplierService service) { this.service = service; }

    // ---- Suppliers ----

    @GetMapping
    public Page<SupplierDto.SupplierResponse> list(
            @RequestParam(required = false) SupplierStatus status,
            @RequestParam(required = false) SupplierType type,
            @PageableDefault(size = 50) Pageable pageable) {
        return service.list(status, type, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierDto.SupplierResponse create(@Valid @RequestBody SupplierDto.CreateSupplierRequest req) {
        return service.create(req);
    }

    @GetMapping("/{id}")
    public SupplierDto.SupplierResponse get(@PathVariable UUID id) { return service.get(id); }

    @PatchMapping("/{id}")
    public SupplierDto.SupplierResponse update(@PathVariable UUID id,
                                               @Valid @RequestBody SupplierDto.UpdateSupplierRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }

    @PostMapping("/{id}/status/{target}")
    public SupplierDto.SupplierResponse changeStatus(@PathVariable UUID id,
                                                     @PathVariable SupplierStatus target,
                                                     @Valid @RequestBody SupplierDto.StatusChangeRequest req) {
        return service.changeStatus(id, target, req);
    }

    @GetMapping("/{id}/statistics")
    public SupplierDto.SupplierStatistics statistics(@PathVariable UUID id) {
        return service.statistics(id);
    }

    // ---- Audits ----

    @GetMapping("/{id}/audits")
    public Page<SupplierDto.AuditResponse> listAudits(@PathVariable UUID id,
                                                      @PageableDefault(size = 50) Pageable pageable) {
        return service.listAudits(id, pageable);
    }

    @PostMapping("/{id}/audits")
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierDto.AuditResponse addAudit(@PathVariable UUID id,
                                              @Valid @RequestBody SupplierDto.CreateAuditRequest req) {
        return service.addAudit(id, req);
    }

    // ---- Non-conformities ----

    @GetMapping("/{id}/non-conformities")
    public Page<SupplierDto.NonConformityResponse> listNc(@PathVariable UUID id,
                                                          @PageableDefault(size = 50) Pageable pageable) {
        return service.listNonConformities(id, pageable);
    }

    @PostMapping("/{id}/non-conformities")
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierDto.NonConformityResponse addNc(
            @PathVariable UUID id,
            @Valid @RequestBody SupplierDto.CreateNonConformityRequest req) {
        return service.addNonConformity(id, req);
    }

    @PatchMapping("/{id}/non-conformities/{ncId}")
    public SupplierDto.NonConformityResponse updateNc(
            @PathVariable UUID id, @PathVariable UUID ncId,
            @Valid @RequestBody SupplierDto.UpdateNonConformityRequest req) {
        return service.updateNonConformity(id, ncId, req);
    }

    // ---- Certificates ----

    @GetMapping("/{id}/certificates")
    public Page<SupplierDto.CertificateResponse> listCerts(@PathVariable UUID id,
                                                           @PageableDefault(size = 50) Pageable pageable) {
        return service.listCertificates(id, pageable);
    }

    @PostMapping("/{id}/certificates")
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierDto.CertificateResponse addCert(
            @PathVariable UUID id,
            @Valid @RequestBody SupplierDto.CreateCertificateRequest req) {
        return service.addCertificate(id, req);
    }

    @DeleteMapping("/{id}/certificates/{certId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCert(@PathVariable UUID id, @PathVariable UUID certId) {
        service.deleteCertificate(id, certId);
    }
}
