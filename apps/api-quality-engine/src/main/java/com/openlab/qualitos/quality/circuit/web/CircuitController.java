package com.openlab.qualitos.quality.circuit.web;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
import com.openlab.qualitos.quality.circuit.application.CircuitDto;
import com.openlab.qualitos.quality.circuit.application.CircuitService;
import com.openlab.qualitos.quality.circuit.domain.CircuitSubject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Les circuits de validation d'un client (ADR 0080).
 *
 * <p>Régler un circuit exige {@code authz.manage} ; voir où en est un objet est
 * ouvert à tout membre du client. Les décisions passent par le module métier
 * (approuver ou refuser une version de document), qui garde ses propres droits.
 */
@RestController
@Validated
@RequestMapping("/api/v1/circuits")
@PreAuthorize("isAuthenticated()")
public class CircuitController {

    private static final String SUBJECT = "[a-z][a-z0-9-]{1,39}";

    private final CircuitService service;

    public CircuitController(CircuitService service) {
        this.service = service;
    }

    @GetMapping("/{subject}")
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    @Transactional(readOnly = true)
    public CircuitDto.CircuitView circuit(@PathVariable @Pattern(regexp = SUBJECT) String subject) {
        return service.circuit(CircuitSubject.fromCode(subject));
    }

    @PutMapping("/{subject}")
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    @Transactional
    public CircuitDto.CircuitView replace(@PathVariable @Pattern(regexp = SUBJECT) String subject,
                                          @Valid @RequestBody CircuitRequest r) {
        return service.replace(CircuitSubject.fromCode(subject),
                r.steps().stream().map(s -> new CircuitDto.StepView(s.name(), s.roleCode(), s.minApprovals()))
                        .toList());
    }

    /** Le dernier passage de l'objet dans son circuit ; 204 s'il n'en a jamais eu. */
    @GetMapping("/{subject}/runs/{subjectId}")
    @Transactional(readOnly = true)
    public ResponseEntity<CircuitDto.RunView> run(@PathVariable @Pattern(regexp = SUBJECT) String subject,
                                                  @PathVariable UUID subjectId) {
        return service.run(CircuitSubject.fromCode(subject), subjectId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // ---------- corps de requête ----------

    public record CircuitRequest(@NotNull @Size(max = 10) List<@NotNull @Valid StepRequest> steps) {}

    public record StepRequest(
            @NotNull @Size(min = 1, max = 120) String name,
            @NotNull @Pattern(regexp = "[A-Z][A-Z0-9_]{1,63}") String roleCode,
            @Min(1) @Max(10) int minApprovals) {}
}
