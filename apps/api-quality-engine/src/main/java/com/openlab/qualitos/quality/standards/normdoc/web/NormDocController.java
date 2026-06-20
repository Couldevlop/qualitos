package com.openlab.qualitos.quality.standards.normdoc.web;

import com.openlab.qualitos.quality.standards.normdoc.application.NormDocDto;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocService;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints — génération assistée IA de documents normatifs + workflow de
 * validation humaine (Standards Hub §8.8, onglet 3).
 *
 * <p>Sécurité : tenant + acteur issus du JWT (jamais du body, §18.2 #2/#5).
 * Génération/édition/soumission/rejet/suppression = pilotage qualité
 * (Manager Qualité+). L'approbation (signature humaine) est réservée au
 * Directeur Qualité / Admin (validation stratégique, §16).
 */
@RestController
@RequestMapping("/api/v1/standards/norm-documents")
@Validated
public class NormDocController {

    private final NormDocService service;

    public NormDocController(NormDocService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<NormDocDto.View> list(@RequestParam(required = false) NormDocStatus status) {
        return service.list(status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public NormDocDto.View get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('QUALITY_MANAGER','ADMIN','ADMIN_TENANT','SUPER_ADMIN')")
    public NormDocDto.View generate(@Valid @RequestBody NormDocWebDto.GenerateBody body) {
        NormDocWebDto.TenantProfileBody p = body.tenantProfile();
        NormDocDto.TenantProfile profile = new NormDocDto.TenantProfile(
                p.organizationName(), p.industry(), p.size(), p.language(),
                p.knownProcesses() == null ? List.of() : p.knownProcesses());
        List<NormDocDto.SectionSpec> specs = body.sections().stream()
                .map(s -> new NormDocDto.SectionSpec(
                        s.key(), s.title(),
                        s.clauses() == null ? List.of() : s.clauses(),
                        s.guidance()))
                .toList();
        return service.generate(new NormDocDto.GenerateRequest(
                body.standardId(), body.kind(), profile, specs));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('QUALITY_MANAGER','ADMIN','ADMIN_TENANT','SUPER_ADMIN')")
    public NormDocDto.View edit(@PathVariable UUID id,
                               @Valid @RequestBody NormDocWebDto.EditBody body) {
        List<NormDocDto.SectionView> sections = body.sections().stream()
                .map(s -> new NormDocDto.SectionView(
                        s.key(), s.title(),
                        s.clauses() == null ? List.of() : s.clauses(),
                        s.bodyMarkdown()))
                .toList();
        return service.edit(id, new NormDocDto.EditRequest(body.title(), sections));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('QUALITY_MANAGER','ADMIN','ADMIN_TENANT','SUPER_ADMIN')")
    public NormDocDto.View submit(@PathVariable UUID id) {
        return service.submitForReview(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('DIRECTOR_QUALITY','ADMIN','ADMIN_TENANT','SUPER_ADMIN')")
    public NormDocDto.View approve(@PathVariable UUID id,
                                  @Valid @RequestBody NormDocWebDto.ApproveBody body) {
        return service.approve(id, new NormDocDto.ApproveRequest(body.signature(), body.notes()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('DIRECTOR_QUALITY','QUALITY_MANAGER','ADMIN','ADMIN_TENANT','SUPER_ADMIN')")
    public NormDocDto.View reject(@PathVariable UUID id,
                                 @Valid @RequestBody NormDocWebDto.RejectBody body) {
        return service.reject(id, new NormDocDto.RejectRequest(body.reason()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('QUALITY_MANAGER','ADMIN','ADMIN_TENANT','SUPER_ADMIN')")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
