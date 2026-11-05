package com.openlab.qualitos.core.tenant;

import com.openlab.qualitos.core.common.MissingTenantContextException;
import com.openlab.qualitos.core.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * La marque du client, vue par le client lui-même : logo, slogan, mentions
 * légales, couleurs, thème.
 *
 * <p><b>Pourquoi cette route existe à côté de celle de l'éditeur.</b>
 * {@code /api/v1/tenants/{id}/identity} sert le super administrateur qui
 * provisionne un client : il est réservé à son rôle et porte {@code @SaasOnly},
 * donc il répond 404 dans une installation on-premise (ADR 0082). S'en tenir là
 * aurait produit deux régressions silencieuses :
 *
 * <ul>
 *   <li>un client <b>on-premise</b> — celui qui a le plus besoin de poser SA
 *       marque sur SON installation — n'aurait eu aucune route pour le faire ;</li>
 *   <li>un <b>administrateur de client</b> en SaaS aurait dû demander à l'éditeur
 *       de changer son slogan, alors que tout paramétrage lui revient.</li>
 * </ul>
 *
 * <p>Le client n'est jamais lu du corps ni du chemin : il vient du jeton
 * ({@code CLAUDE.md} §18.2.2). Un administrateur ne peut donc pas écrire la
 * marque d'un autre client, même en forgeant la requête.
 */
@RestController
@RequestMapping("/api/v1/branding")
@Tag(name = "Branding", description = "Identité visuelle et légale du client courant")
public class BrandingController {

    private static final Logger log = LoggerFactory.getLogger(BrandingController.class);

    private final TenantIdentityService identityService;

    public BrandingController(TenantIdentityService identityService) {
        this.identityService = identityService;
    }

    /**
     * Lecture ouverte à tout membre authentifié : l'application a besoin du
     * thème et des couleurs pour se rendre. La réponse n'est pas publique pour
     * autant — elle nomme la raison sociale, l'adresse et le numéro de TVA.
     */
    @GetMapping
    @Operation(summary = "Read the current tenant's branding")
    public ResponseEntity<TenantIdentityDto.Response> get() {
        return ResponseEntity.ok()
                // Courte : un changement de marque doit se voir au rechargement
                // suivant, pas le lendemain.
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePrivate())
                .body(identityService.get(currentTenant()));
    }

    /** Écriture réservée à l'administrateur du client (et à l'éditeur, qui dépanne). */
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Set the current tenant's branding")
    public ResponseEntity<TenantIdentityDto.Response> put(
            @Valid @RequestBody TenantIdentityDto.Request request) {
        UUID tenantId = currentTenant();
        TenantIdentityDto.Response saved = identityService.save(tenantId, request);
        log.info("tenant.branding.updated tenant_id={} theme={}", tenantId, saved.themeCode());
        return ResponseEntity.ok(saved);
    }

    private UUID currentTenant() {
        String raw = TenantContext.getTenantId();
        if (raw == null || raw.isBlank()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(raw);
    }
}
