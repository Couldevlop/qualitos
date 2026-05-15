package com.openlab.qualitos.quality.industry;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Gère le catalogue d'Industry Packs et leurs activations par tenant.
 *
 * Activation : si une activation ACTIVE existe déjà pour (tenant, pack), elle est
 * renvoyée (idempotent). Désactivation : flip status + horodatage, ligne préservée
 * pour audit. Réactivation : nouvelle ligne ACTIVE — l'historique reste.
 */
@Service
public class IndustryPackService {

    private final IndustryPackRepository packRepo;
    private final TenantIndustryPackActivationRepository activationRepo;

    public IndustryPackService(IndustryPackRepository packRepo,
                               TenantIndustryPackActivationRepository activationRepo) {
        this.packRepo = packRepo;
        this.activationRepo = activationRepo;
    }

    // ---------- Catalog ----------

    @Transactional(readOnly = true)
    public Page<IndustryPackDto.PackResponse> listAll(Pageable pageable) {
        return packRepo.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public IndustryPackDto.PackResponse getByCode(String code) {
        IndustryPack p = packRepo.findByCode(code).orElseThrow(() -> new IndustryPackNotFoundException(code));
        return toResponse(p);
    }

    // ---------- Activations ----------

    @Transactional
    public IndustryPackDto.ActivationResponse activate(String code, IndustryPackDto.ActivateRequest req) {
        UUID tenantId = requireTenantId();
        // Pack must exist in catalog.
        packRepo.findByCode(code).orElseThrow(() -> new IndustryPackNotFoundException(code));

        Optional<TenantIndustryPackActivation> existing =
                activationRepo.findByTenantIdAndPackCodeAndStatus(tenantId, code, ActivationStatus.ACTIVE);
        if (existing.isPresent()) return toResponse(existing.get()); // idempotent

        TenantIndustryPackActivation a = new TenantIndustryPackActivation();
        a.setTenantId(tenantId);
        a.setPackCode(code);
        a.setStatus(ActivationStatus.ACTIVE);
        a.setActivatedBy(req.activatedBy());
        a.setActivatedAt(Instant.now());
        a.setConfigOverridesJson(req.configOverridesJson());
        return toResponse(activationRepo.save(a));
    }

    @Transactional
    public IndustryPackDto.ActivationResponse deactivate(String code, UUID deactivatedBy) {
        UUID tenantId = requireTenantId();
        TenantIndustryPackActivation a = activationRepo
                .findByTenantIdAndPackCodeAndStatus(tenantId, code, ActivationStatus.ACTIVE)
                .orElseThrow(() -> new IndustryPackNotFoundException(code + " (no active activation)"));
        a.setStatus(ActivationStatus.DEACTIVATED);
        a.setDeactivatedAt(Instant.now());
        a.setDeactivatedBy(deactivatedBy);
        return toResponse(activationRepo.save(a));
    }

    @Transactional(readOnly = true)
    public List<IndustryPackDto.ActivationResponse> myActiveActivations() {
        UUID tenantId = requireTenantId();
        return activationRepo.findByTenantIdAndStatus(tenantId, ActivationStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<IndustryPackDto.ActivationResponse> myActivationHistory() {
        UUID tenantId = requireTenantId();
        return activationRepo.findByTenantIdOrderByActivatedAtDesc(tenantId)
                .stream().map(this::toResponse).toList();
    }

    // ---------- helpers ----------

    private IndustryPackDto.PackResponse toResponse(IndustryPack p) {
        List<String> tags = (p.getTagsCsv() == null || p.getTagsCsv().isBlank())
                ? List.of()
                : Arrays.stream(p.getTagsCsv().split(",")).map(String::trim).toList();
        return new IndustryPackDto.PackResponse(
                p.getId(), p.getCode(), p.getName(), p.getDescription(),
                p.getVersion(), p.getLocale(), tags, p.getManifestJson(),
                p.getCreatedAt(), p.getUpdatedAt());
    }

    private IndustryPackDto.ActivationResponse toResponse(TenantIndustryPackActivation a) {
        return new IndustryPackDto.ActivationResponse(
                a.getId(), a.getTenantId(), a.getPackCode(), a.getStatus(),
                a.getActivatedBy(), a.getActivatedAt(),
                a.getDeactivatedAt(), a.getDeactivatedBy());
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
