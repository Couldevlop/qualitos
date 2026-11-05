package com.openlab.qualitos.core.tenant;

import com.openlab.qualitos.core.edition.SaasOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

/**
 * REST controller pour la gestion des tenants.
 * Accessible uniquement aux SUPER_ADMIN (protégé via SecurityConfig + @PreAuthorize).
 * Le tenant_id n'est jamais lu depuis le body — géré au niveau sécurité.
 */
@RestController
@RequestMapping("/api/v1/tenants")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Tenants", description = "Tenant management — Super Admin only")
@SaasOnly
public class TenantController {

    private static final Logger log = LoggerFactory.getLogger(TenantController.class);

    private final TenantService tenantService;
    private final TenantOnboardingService onboarding;
    private final TenantIdentityService identityService;

    public TenantController(TenantService tenantService, TenantOnboardingService onboarding,
                            TenantIdentityService identityService) {
        this.tenantService = tenantService;
        this.onboarding = onboarding;
        this.identityService = identityService;
    }

    /**
     * Crée un client de bout en bout : l'entreprise, le compte de son premier
     * administrateur, ses modules. Le mot de passe provisoire n'est rendu qu'ici.
     */
    @PostMapping("/onboard")
    @Operation(summary = "Onboard a tenant: company, first administrator account, modules")
    public ResponseEntity<OnboardingDto.Response> onboardTenant(@Valid @RequestBody OnboardingDto.Request request) {
        OnboardingDto.Response created = onboarding.onboard(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/tenants/{id}").buildAndExpand(created.tenant().id()).toUri();
        return ResponseEntity.created(location).header("Cache-Control", "no-store").body(created);
    }

    @GetMapping
    @Operation(summary = "List all tenants (paginated)")
    public ResponseEntity<Page<TenantDto.Response>> listTenants(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(tenantService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tenant by ID")
    public ResponseEntity<TenantDto.Response> getTenant(@PathVariable UUID id) {
        return ResponseEntity.ok(tenantService.findById(id));
    }

    @GetMapping("/by-slug/{slug}")
    @Operation(summary = "Get tenant by slug")
    public ResponseEntity<TenantDto.Response> getTenantBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(tenantService.findBySlug(slug));
    }

    @PostMapping
    @Operation(summary = "Create a new tenant")
    public ResponseEntity<TenantDto.Response> createTenant(
            @Valid @RequestBody TenantDto.CreateRequest request) {
        TenantDto.Response created = tenantService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update tenant name, plan, or active status")
    public ResponseEntity<TenantDto.Response> updateTenant(
            @PathVariable UUID id,
            @Valid @RequestBody TenantDto.UpdateRequest request) {
        return ResponseEntity.ok(tenantService.update(id, request));
    }

    /**
     * L'identité du client : ce qui habille son espace et ce qui figure sur ses
     * documents. Lecture ouverte au super administrateur comme le reste de ce
     * contrôleur ; l'application d'un client lit la sienne par la route de
     * marque, qui n'expose pas les mentions légales d'un autre.
     */
    @GetMapping("/{id}/identity")
    @Operation(summary = "Read a tenant's visual and legal identity")
    public ResponseEntity<TenantIdentityDto.Response> getIdentity(@PathVariable UUID id) {
        return ResponseEntity.ok(identityService.get(id));
    }

    @PutMapping("/{id}/identity")
    @Operation(summary = "Set a tenant's visual and legal identity")
    public ResponseEntity<TenantIdentityDto.Response> putIdentity(
            @PathVariable UUID id,
            @Valid @RequestBody TenantIdentityDto.Request request) {
        TenantIdentityDto.Response saved = identityService.save(id, request);
        // Journalisation structuree, comme le reste d'api-core. La piste d'audit
        // scellee inter-clients est l'objet du lot 3 de la console : tant qu'elle
        // n'existe pas, ne pas faire croire qu'une ecriture d'editeur y figure.
        log.info("tenant.identity.updated tenant_id={} theme={}", id, saved.themeCode());
        return ResponseEntity.ok(saved);
    }

    /**
     * Le logo d'un client, posé par l'éditeur au moment du provisionnement.
     *
     * <p><b>Pourquoi cette route existe en plus de {@code /api/v1/branding/logo}.</b>
     * La règle de la spécification (§7) dit qu'une capacité dont un client a
     * besoin pour s'administrer vit dans la famille « client », et qu'on ne la
     * double pas. Le logo fait exception pour une raison précise : à la
     * <b>création</b>, le client n'existe pas encore comme utilisateur — aucun
     * de ses membres n'a de jeton — et la demande est que tout, logo compris,
     * soit paramétrable à ce moment-là. Sans cette route, l'éditeur devrait
     * créer le client, attendre que son administrateur se connecte, et lui
     * demander de téléverser le fichier que l'éditeur a déjà en main.
     *
     * <p>Le doublement reste sans coût de sécurité : le contrôle d'accès de
     * {@code /api/v1/tenants/**} est posé une seule fois, dans
     * {@code SecurityConfig}, et la politique de téléversement
     * ({@link LogoPolicy}) est appliquée par le service — donc par les deux
     * routes, sans possibilité qu'une seule soit resserrée.
     */
    @PostMapping(value = "/{id}/identity/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a tenant's logo")
    public ResponseEntity<Void> postIdentityLogo(@PathVariable UUID id,
                                                @RequestParam("file") MultipartFile file)
            throws IOException {
        String sha256 = identityService.saveLogo(id, file.getContentType(), file.getBytes());
        log.info("tenant.logo.updated tenant_id={} bytes={} sha256={}", id, file.getSize(), sha256);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/identity/logo")
    @Operation(summary = "Remove a tenant's logo")
    public ResponseEntity<Void> deleteIdentityLogo(@PathVariable UUID id) {
        identityService.deleteLogo(id);
        log.info("tenant.logo.removed tenant_id={}", id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a tenant (soft delete)")
    public ResponseEntity<Void> deactivateTenant(@PathVariable UUID id) {
        tenantService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
