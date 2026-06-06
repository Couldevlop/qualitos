package com.openlab.qualitos.quality.industry;

import com.openlab.qualitos.quality.common.CurrentUser;
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
    private final IndustryPackProvisioningService provisioningService;

    public IndustryPackService(IndustryPackRepository packRepo,
                               TenantIndustryPackActivationRepository activationRepo,
                               IndustryPackProvisioningService provisioningService) {
        this.packRepo = packRepo;
        this.activationRepo = activationRepo;
        this.provisioningService = provisioningService;
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
        // H2 (OWASP A01) : l'acteur est dérivé du JWT (sub), jamais d'un champ de body.
        UUID actor = CurrentUser.requireUserId();
        // Pack must exist in catalog.
        IndustryPack pack = packRepo.findByCode(code).orElseThrow(() -> new IndustryPackNotFoundException(code));

        Optional<TenantIndustryPackActivation> existing =
                activationRepo.findByTenantIdAndPackCodeAndStatus(tenantId, code, ActivationStatus.ACTIVE);
        if (existing.isPresent()) {
            // Idempotent : déjà actif. Aucun nouveau provisionnement (les KPIs sont déjà chez
            // le tenant — provision() est lui-même idempotent, mais on évite le travail inutile).
            return toResponse(existing.get(), null);
        }

        TenantIndustryPackActivation a = new TenantIndustryPackActivation();
        a.setTenantId(tenantId);
        a.setPackCode(code);
        a.setStatus(ActivationStatus.ACTIVE);
        a.setActivatedBy(actor);
        a.setActivatedAt(Instant.now());
        a.setConfigOverridesJson(req.configOverridesJson());
        // BUG #3 — Double activation concurrente : deux requêtes passent le check
        // d'idempotence puis insèrent en parallèle, violant la contrainte d'unicité
        // (tenant_id, pack_code, status=ACTIVE). On laisse remonter la
        // DataIntegrityViolationException → 409 via le GlobalExceptionHandler, plutôt
        // qu'un 500 opaque.
        TenantIndustryPackActivation saved = activationRepo.save(a);

        // Phase 2 (ADR 0019) : provisionnement du contenu APRÈS l'enregistrement de
        // l'activation, dans la même transaction. Résilience contenu : un échec de KPI
        // produit un warning, jamais un rollback de l'activation.
        IndustryPackProvisioningService.ProvisioningResult result =
                provisioningService.provision(tenantId, actor, pack.getManifestJson());
        return toResponse(saved, result);
    }

    @Transactional
    public IndustryPackDto.ActivationResponse deactivate(String code) {
        UUID tenantId = requireTenantId();
        // H2 (OWASP A01) : l'acteur de la désactivation est dérivé du JWT (sub).
        UUID actor = CurrentUser.requireUserId();
        TenantIndustryPackActivation a = activationRepo
                .findByTenantIdAndPackCodeAndStatus(tenantId, code, ActivationStatus.ACTIVE)
                .orElseThrow(() -> new IndustryPackNotFoundException(code + " (no active activation)"));
        a.setStatus(ActivationStatus.DEACTIVATED);
        a.setDeactivatedAt(Instant.now());
        a.setDeactivatedBy(actor);
        // Phase 2 (ADR 0019) : la désactivation NE SUPPRIME RIEN. Les KPIs provisionnés
        // appartiennent désormais au tenant (catalogue éditable indépendamment du pack) ;
        // les supprimer effacerait des mesures et des CAPA attachées. Aucun provisionnement.
        return toResponse(activationRepo.save(a), null);
    }

    @Transactional(readOnly = true)
    public List<IndustryPackDto.ActivationResponse> myActiveActivations() {
        UUID tenantId = requireTenantId();
        return activationRepo.findByTenantIdAndStatus(tenantId, ActivationStatus.ACTIVE)
                .stream().map(a -> toResponse(a, null)).toList();
    }

    @Transactional(readOnly = true)
    public List<IndustryPackDto.ActivationResponse> myActivationHistory() {
        UUID tenantId = requireTenantId();
        return activationRepo.findByTenantIdOrderByActivatedAtDesc(tenantId)
                .stream().map(a -> toResponse(a, null)).toList();
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

    private IndustryPackDto.ActivationResponse toResponse(
            TenantIndustryPackActivation a,
            IndustryPackProvisioningService.ProvisioningResult result) {
        IndustryPackDto.Provisioning provisioning = result == null ? null
                : new IndustryPackDto.Provisioning(
                        result.kpisCreated(), result.kpisSkipped(), result.warnings());
        return new IndustryPackDto.ActivationResponse(
                a.getId(), a.getTenantId(), a.getPackCode(), a.getStatus(),
                a.getActivatedBy(), a.getActivatedAt(),
                a.getDeactivatedAt(), a.getDeactivatedBy(),
                provisioning);
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
