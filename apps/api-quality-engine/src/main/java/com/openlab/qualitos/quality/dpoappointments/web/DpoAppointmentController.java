package com.openlab.qualitos.quality.dpoappointments.web;

import com.openlab.qualitos.quality.dpoappointments.application.DpoAppointmentDto;
import com.openlab.qualitos.quality.dpoappointments.application.DpoAppointmentService;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointmentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Endpoints — registre des désignations DPO (RGPD Art. 37-39).
 */
@RestController
@RequestMapping("/api/v1/gdpr/dpo-appointments")
@Validated
public class DpoAppointmentController {

    private final DpoAppointmentService service;

    public DpoAppointmentController(DpoAppointmentService service) {
        this.service = service;
    }

    @GetMapping
    public List<DpoAppointmentDto.View> list(
            @RequestParam(required = false) DpoAppointmentStatus status) {
        return service.list(status);
    }

    @GetMapping("/{id}")
    public DpoAppointmentDto.View get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/by-reference")
    public DpoAppointmentDto.View getByReference(
            @RequestParam @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$") String reference) {
        return service.getByReference(reference);
    }

    @GetMapping("/active")
    public ResponseEntity<DpoAppointmentDto.View> findActive(
            @RequestParam @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{0,63}$") String scope) {
        Optional<DpoAppointmentDto.View> found = service.findActiveByScope(scope);
        return found.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DpoAppointmentDto.View propose(
            @Valid @RequestBody DpoAppointmentWebDto.ProposeRequest req) {
        return service.propose(new DpoAppointmentDto.ProposeRequest(
                req.reference(), req.dpoFullName(), req.dpoEmail(), req.dpoPhone(),
                req.dpoType(), req.externalCompanyName(), req.qualifications(),
                req.scope(), req.linkedProcessingActivityIds(), req.createdByUserId()));
    }

    @PutMapping("/{id}")
    public DpoAppointmentDto.View edit(@PathVariable UUID id,
                                       @Valid @RequestBody DpoAppointmentWebDto.EditRequest req) {
        return service.edit(id, new DpoAppointmentDto.EditRequest(
                req.dpoFullName(), req.dpoEmail(), req.dpoPhone(),
                req.dpoType(), req.externalCompanyName(), req.qualifications(),
                req.linkedProcessingActivityIds()));
    }

    @PostMapping("/{id}/activate")
    public DpoAppointmentDto.View activate(@PathVariable UUID id,
                                           @Valid @RequestBody DpoAppointmentWebDto.ActivateRequest req) {
        return service.activate(id, new DpoAppointmentDto.ActivateRequest(
                req.effectiveFrom(), req.regulatorNotifiedAt(),
                req.regulatorNotificationReference()));
    }

    @PostMapping("/{id}/end")
    public DpoAppointmentDto.View end(@PathVariable UUID id,
                                      @Valid @RequestBody DpoAppointmentWebDto.EndRequest req) {
        return service.end(id, new DpoAppointmentDto.EndRequest(
                req.reason(), req.effectiveTo()));
    }

    @PostMapping("/{id}/cancel")
    public DpoAppointmentDto.View cancel(@PathVariable UUID id,
                                         @Valid @RequestBody DpoAppointmentWebDto.CancelRequest req) {
        return service.cancel(id, new DpoAppointmentDto.CancelRequest(req.reason()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }
}
