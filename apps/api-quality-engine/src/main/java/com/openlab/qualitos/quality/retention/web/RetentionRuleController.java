package com.openlab.qualitos.quality.retention.web;

import com.openlab.qualitos.quality.retention.application.RetentionRuleDto;
import com.openlab.qualitos.quality.retention.application.RetentionRuleService;
import com.openlab.qualitos.quality.retention.domain.RetentionRuleStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Endpoints règles de rétention RGPD (Art. 5.1.e).
 */
@RestController
@RequestMapping("/api/v1/gdpr/retention-rules")
@Validated
public class RetentionRuleController {

    private final RetentionRuleService service;

    public RetentionRuleController(RetentionRuleService service) { this.service = service; }

    @GetMapping
    public List<RetentionRuleDto.View> list(
            @RequestParam(required = false) RetentionRuleStatus status) {
        return service.list(status);
    }

    @GetMapping("/{id}")
    public RetentionRuleDto.View get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RetentionRuleDto.View create(
            @Valid @RequestBody RetentionRuleWebDto.CreateRequest req) {
        return service.create(new RetentionRuleDto.CreateRequest(
                req.dataCategoryCode(), req.dataCategoryLabel(),
                req.retentionPeriod(), req.legalBasis(), req.lawfulBasisReference(),
                req.createdByUserId()));
    }

    @PutMapping("/{id}")
    public RetentionRuleDto.View edit(@PathVariable UUID id,
                                      @Valid @RequestBody RetentionRuleWebDto.EditRequest req) {
        return service.edit(id, new RetentionRuleDto.EditRequest(
                req.dataCategoryLabel(), req.retentionPeriod(),
                req.legalBasis(), req.lawfulBasisReference()));
    }

    @PostMapping("/{id}/activate")
    public RetentionRuleDto.View activate(@PathVariable UUID id) {
        return service.activate(id);
    }

    @PostMapping("/{id}/archive")
    public RetentionRuleDto.View archive(@PathVariable UUID id) {
        return service.archive(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }

    /** Évaluation : pour cette catégorie et cette date de création, donne la
     *  date d'effacement et si elle est dépassée. */
    @GetMapping("/erasure-evaluation")
    public ResponseEntity<RetentionRuleDto.ErasureEvaluation> evaluate(
            @RequestParam @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[a-z][a-z0-9._-]{1,63}$") String dataCategoryCode,
            @RequestParam @NotNull Instant recordCreatedAt) {
        return service.evaluateErasure(dataCategoryCode, recordCreatedAt)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
