package com.openlab.qualitos.core.mail;

import com.openlab.qualitos.core.common.MissingTenantContextException;
import com.openlab.qualitos.core.security.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * La messagerie du client connecté.
 *
 * <p><b>Aucune route ne prend un identifiant de client.</b> Le client est
 * toujours celui du jeton ({@code CLAUDE.md} §18.2.2). Une route
 * {@code /tenants/{id}/mail} aurait suffi à laisser un administrateur régler la
 * messagerie d'une autre organisation — c'est-à-dire détourner son courrier et
 * émettre en son nom.
 *
 * <p>Réservé à l'<b>administrateur du client</b> : le paramétrage lui revient.
 * Un cabinet partenaire peut demander à son client de régler sa messagerie ; il
 * ne l'écrit pas à sa place, parce que l'adresse d'où partent les courriels
 * engage l'organisation, pas son prestataire.
 */
@RestController
@RequestMapping("/api/v1/mail-settings")
@Validated
public class TenantMailSettingsController {

    private static final Logger log = LoggerFactory.getLogger(TenantMailSettingsController.class);

    private final TenantMailSettingsService service;

    public TenantMailSettingsController(TenantMailSettingsService service) {
        this.service = service;
    }

    /** Ce qu'il faut pour un essai : à qui l'envoyer. */
    public record TestRequest(@NotBlank @Email String recipient) {
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    public ResponseEntity<MailSettingsDto.Response> lire() {
        return ResponseEntity.ok(service.lire(currentTenant()));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    public ResponseEntity<MailSettingsDto.Response> enregistrer(
            @Valid @RequestBody MailSettingsDto.Request request) {
        UUID tenant = currentTenant();
        MailSettingsDto.Response saved = service.enregistrer(tenant, request);
        log.info("tenant.mail.settings-updated tenant_id={} enabled={}", tenant, saved.enabled());
        return ResponseEntity.ok(saved);
    }

    /**
     * Envoie un courriel d'essai.
     *
     * <p>Répond <b>200 avec un échec décrit</b> plutôt qu'une erreur HTTP quand
     * le serveur refuse : un mot de passe rejeté n'est pas une faute de la
     * requête, c'est son résultat. Un 4xx ferait afficher à l'écran un message
     * d'erreur générique, là où l'administrateur a besoin de lire « 535
     * authentication failed » pour corriger.
     */
    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    public ResponseEntity<MailSettingsDto.TestResult> eprouver(
            @Valid @RequestBody TestRequest request) {
        return ResponseEntity.ok(service.eprouver(currentTenant(), request.recipient()));
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    public ResponseEntity<Void> retirer() {
        service.retirer(currentTenant());
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
