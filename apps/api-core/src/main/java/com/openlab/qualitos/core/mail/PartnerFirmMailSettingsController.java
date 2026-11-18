package com.openlab.qualitos.core.mail;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * La messagerie d'un cabinet partenaire, dans la console de plateforme.
 *
 * <p>Réservé au <b>super administrateur</b>, comme le reste de la gestion des
 * cabinets. Ce n'est pas un oubli : un cabinet n'a pas d'administrateur dans
 * QualitOS — ses consultants travaillent chez les clients qui les ont acceptés,
 * et leur rôle y est celui que le client leur a donné.
 *
 * <p><b>Ce qu'aucune route ne permet</b> : qu'un cabinet règle la messagerie de
 * ses clients. Il peut la leur demander ; il ne l'écrit pas à leur place, parce
 * que l'adresse d'où partent les courriels engage l'organisation, pas son
 * prestataire. C'est pour cela que ce contrôleur et celui du client sont deux
 * surfaces distinctes, et non une route paramétrée par le type d'organisation —
 * une seule route aurait fini par accepter les deux.
 */
@RestController
@RequestMapping("/api/v1/partner-firms/{firmId}/mail-settings")
@Validated
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PartnerFirmMailSettingsController {

    private static final Logger log =
            LoggerFactory.getLogger(PartnerFirmMailSettingsController.class);

    private final PartnerFirmMailSettingsService service;

    public PartnerFirmMailSettingsController(PartnerFirmMailSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<MailSettingsDto.Response> lire(@PathVariable UUID firmId) {
        return ResponseEntity.ok(service.lire(firmId));
    }

    @PutMapping
    public ResponseEntity<MailSettingsDto.Response> enregistrer(
            @PathVariable UUID firmId,
            @Valid @RequestBody MailSettingsDto.Request request) {
        MailSettingsDto.Response saved = service.enregistrer(firmId, request);
        log.info("firm.mail.settings-updated firm_id={} enabled={}", firmId, saved.enabled());
        return ResponseEntity.ok(saved);
    }

    /**
     * Répond <b>200 avec un échec décrit</b> quand le serveur refuse : un mot de
     * passe rejeté n'est pas une faute de la requête, c'est son résultat.
     */
    @PostMapping("/test")
    public ResponseEntity<MailSettingsDto.TestResult> eprouver(
            @PathVariable UUID firmId,
            @Valid @RequestBody TenantMailSettingsController.TestRequest request) {
        return ResponseEntity.ok(service.eprouver(firmId, request.recipient()));
    }

    @DeleteMapping
    public ResponseEntity<Void> retirer(@PathVariable UUID firmId) {
        service.retirer(firmId);
        return ResponseEntity.noContent().build();
    }
}
