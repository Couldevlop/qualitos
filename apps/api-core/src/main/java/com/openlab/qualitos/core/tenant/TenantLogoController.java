package com.openlab.qualitos.core.tenant;

import com.openlab.qualitos.core.common.MissingTenantContextException;
import com.openlab.qualitos.core.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Le logo du client courant : le poser, le servir, le retirer.
 *
 * <p>Comme {@link BrandingController}, cette route vit hors de
 * {@code /api/v1/tenants/**} : celui-ci porte {@code @SaasOnly} et répondrait
 * 404 en on-premise, là où le besoin de poser SA marque sur SON installation
 * est le plus fort. Le client vient du jeton, jamais du chemin
 * ({@code CLAUDE.md} §18.2.2).
 *
 * <p><b>Servir un fichier téléversé depuis le domaine applicatif demande trois
 * précautions</b>, et elles sont toutes ici plutôt que dans la configuration
 * globale, parce que c'est ici qu'on sait que le contenu vient d'un client :
 *
 * <ul>
 *   <li>{@code Content-Type} celui qu'a VALIDÉ {@link LogoPolicy}, jamais celui
 *       qu'annonçait le client, et {@code X-Content-Type-Options: nosniff} pour
 *       que le navigateur ne le redevine pas ;</li>
 *   <li>{@code Content-Disposition: inline} avec un nom de fichier fixe : le nom
 *       d'origine est une chaîne venue du client, elle ne sert à rien ici ;</li>
 *   <li>{@code Content-Security-Policy: default-src 'none'} — ceinture : même si
 *       un contenu actif passait les contrôles, il n'aurait aucune permission.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/branding/logo")
@Tag(name = "Branding", description = "Identité visuelle et légale du client courant")
public class TenantLogoController {

    private static final Logger log = LoggerFactory.getLogger(TenantLogoController.class);

    /** Le nom servi. Jamais celui du fichier téléversé. */
    private static final String FILENAME = "logo";

    private final TenantIdentityService identityService;

    public TenantLogoController(TenantIdentityService identityService) {
        this.identityService = identityService;
    }

    /**
     * Sert le logo. Ouvert à tout membre authentifié : il s'affiche sur chaque
     * écran. Validé par ETag — le navigateur ne retélécharge pas un logo
     * inchangé, et le serveur ne relit pas ses octets pour rien.
     */
    @GetMapping
    @Operation(summary = "Serve the current tenant's logo")
    public ResponseEntity<byte[]> get(
            @RequestParam(value = "v", required = false) String version,
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        Optional<TenantLogo> found = identityService.logo(currentTenant());
        if (found.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        TenantLogo logo = found.get();
        String etag = "\"" + logo.getSha256() + "\"";
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(304).eTag(etag).build();
        }
        return ResponseEntity.ok()
                .eTag(etag)
                .contentType(MediaType.parseMediaType(logo.getContentType()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(FILENAME).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .body(logo.getContent());
    }

    /** Pose ou remplace le logo. Réservé à l'administrateur du client. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Upload the current tenant's logo")
    public ResponseEntity<Void> post(@RequestParam("file") MultipartFile file) throws IOException {
        UUID tenantId = currentTenant();
        // Le type est celui du formulaire : une déclaration. LogoPolicy, appelé
        // par le service, le confronte aux premiers octets avant d'enregistrer.
        String sha256 = identityService.saveLogo(tenantId, file.getContentType(), file.getBytes());
        log.info("tenant.logo.updated tenant_id={} bytes={} sha256={}",
                tenantId, file.getSize(), sha256);
        return ResponseEntity.noContent().build();
    }

    /** Retire le logo. L'identité — slogan, couleurs, mentions — reste intacte. */
    @DeleteMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Remove the current tenant's logo")
    public ResponseEntity<Void> delete() {
        UUID tenantId = currentTenant();
        identityService.deleteLogo(tenantId);
        log.info("tenant.logo.removed tenant_id={}", tenantId);
        return ResponseEntity.noContent().build();
    }

    private UUID currentTenant() {
        String raw = TenantContext.getTenantId();
        if (raw == null || raw.isBlank()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(raw);
    }
}
