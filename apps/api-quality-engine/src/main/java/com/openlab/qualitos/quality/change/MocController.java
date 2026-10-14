package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
import com.openlab.qualitos.quality.change.domain.ImpactDomain;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * La gestion des changements (MOC, ADR 0087).
 *
 * <p>Lire est ouvert à tout membre du client. Demander exige {@code change.request},
 * analyser et soumettre {@code change.analyze}. Statuer ne dépend pas d'un droit
 * de la plateforme mais d'une fonction dans l'organisation : être titulaire du
 * rôle de validation, vérifié par le service sur l'utilisateur du jeton.
 */
@RestController
@RequestMapping("/api/v1/changes")
@PreAuthorize("isAuthenticated()")
public class MocController {

    static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final MocService service;

    public MocController(MocService service) {
        this.service = service;
    }

    @GetMapping
    public List<MocDto.Summary> list() {
        return service.list();
    }

    /** Toutes les demandes du client en un classeur Excel. */
    @GetMapping(value = "/export.xlsx", produces = XLSX)
    public ResponseEntity<byte[]> export() {
        byte[] contenu = MocWorkbook.build(service.exportRows());
        String nom = "demandes-de-changement-" + LocalDate.now(ZoneId.of("Europe/Paris")) + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nom).build().toString())
                .contentType(MediaType.parseMediaType(XLSX))
                .body(contenu);
    }

    @GetMapping("/{id}")
    public MocDto.Detail get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission(Permission.CHANGE_REQUEST)
    public MocDto.Detail create(@Valid @RequestBody MocDto.RequestForm form) {
        return service.create(form);
    }

    @PutMapping("/{id}")
    @RequiresPermission(Permission.CHANGE_REQUEST)
    public MocDto.Detail update(@PathVariable UUID id, @Valid @RequestBody MocDto.RequestForm form) {
        return service.update(id, form);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission(Permission.CHANGE_REQUEST)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @PostMapping("/{id}/analysis")
    @RequiresPermission(Permission.CHANGE_REQUEST)
    public MocDto.Detail sendToAnalysis(@PathVariable UUID id) {
        return service.sendToAnalysis(id);
    }

    @PutMapping("/{id}/impacts/{domain}")
    @RequiresPermission(Permission.CHANGE_ANALYZE)
    public MocDto.Detail assess(@PathVariable UUID id, @PathVariable ImpactDomain domain,
                                @Valid @RequestBody MocDto.ImpactForm form) {
        return service.assess(id, domain, form);
    }

    @PostMapping("/{id}/submit")
    @RequiresPermission(Permission.CHANGE_ANALYZE)
    public MocDto.Detail submit(@PathVariable UUID id) {
        return service.submit(id);
    }

    @PostMapping("/{id}/reopen")
    @RequiresPermission(Permission.CHANGE_ANALYZE)
    public MocDto.Detail reopen(@PathVariable UUID id) {
        return service.reopen(id);
    }

    /** Statuer sur la carte d'un rôle : l'auteur est l'utilisateur du jeton, titulaire du rôle. */
    @PostMapping("/{id}/validations/{validationId}/decision")
    public MocDto.Detail decide(@PathVariable UUID id, @PathVariable UUID validationId,
                                @Valid @RequestBody MocDto.DecisionForm form) {
        return service.decide(id, validationId, form);
    }
}
