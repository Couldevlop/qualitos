package com.openlab.qualitos.core.platformaudit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * La piste d'audit de la plateforme, en lecture.
 *
 * <p><b>Aucune écriture n'est exposée</b>, et c'est la différence la plus
 * importante avec le journal du moteur de qualité, qui porte un {@code POST}
 * d'enregistrement. Une surface d'écriture, même réservée à l'éditeur, permet
 * de FABRIQUER un acte qui n'a pas eu lieu — exactement ce qu'un auditeur
 * cherche à exclure. Les actes n'entrent ici que depuis le code qui les
 * exécute.
 *
 * <p>Réservée au super-administrateur. Un client ne lit pas cette piste : il
 * lit la sienne, dans le moteur de qualité. Ce qu'il peut obtenir de celle-ci,
 * c'est l'extrait qui le concerne, remis par l'éditeur
 * ({@code ?targetTenantId=…} puis export).
 */
@RestController
@RequestMapping("/api/v1/platform-audit")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Platform audit trail",
        description = "Actes de l'editeur sur les dossiers clients — super-administrateur uniquement")
public class PlatformAuditController {

    private final PlatformAuditService service;

    public PlatformAuditController(PlatformAuditService service) {
        this.service = service;
    }

    /**
     * Les actes consignés, du plus récent au plus ancien.
     *
     * <p>Les filtres sont exclusifs et évalués dans l'ordre du service : un
     * client, sinon un acte, sinon un auteur, sinon une période. Le croisement
     * n'est pas offert tant qu'un besoin réel ne l'exige pas — une
     * combinatoire de filtres non demandée est de la surface d'attaque et du
     * code non couvert.
     */
    @GetMapping
    @Operation(summary = "Lister les actes de plateforme consignes")
    public Page<PlatformAuditDto.Event> list(
            @RequestParam(required = false) PlatformAuditAction action,
            @RequestParam(required = false) UUID targetTenantId,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 50) Pageable pageable) {
        return service.list(action, targetTenantId, actorUserId, from, to, pageable);
    }

    /**
     * Vérifie que la chaîne n'a pas été retouchée sur une fenêtre.
     *
     * <p>Les bornes par défaut couvrent TOUT le journal : c'est la
     * vérification qu'on veut en pratique, et une valeur par défaut étroite
     * donnerait un « intact » rassurant sur trois lignes.
     */
    @GetMapping("/verify")
    @Operation(summary = "Verifier l'integrite de la chaine sur une fenetre")
    public PlatformAuditDto.Verification verify(
            @RequestParam(required = false) Long fromSeq,
            @RequestParam(required = false) Long toSeq) {
        long debut = fromSeq != null ? fromSeq : 1L;
        long fin = toSeq != null ? toSeq : Math.max(service.derniereSequence(), debut);
        return service.verifyChain(debut, fin);
    }

    /**
     * Le dossier d'audit à remettre à une autorité : la fenêtre d'actes, son
     * verdict d'intégrité et son sceau.
     *
     * <p>Servi en pièce jointe avec le sceau dans le nom du fichier : deux
     * dossiers du même intervalle, extraits à deux dates, ne se confondent
     * pas sur un bureau.
     */
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Exporter un dossier d'audit scelle")
    public ResponseEntity<PlatformAuditDto.Export> export(
            @RequestParam(required = false) Long fromSeq,
            @RequestParam(required = false) Long toSeq) {
        long debut = fromSeq != null ? fromSeq : 1L;
        long fin = toSeq != null ? toSeq : Math.max(service.derniereSequence(), debut);
        PlatformAuditDto.Export dossier = service.export(debut, fin);
        String nom = "qualitos-piste-audit-" + debut + "-" + fin
                + "-" + dossier.seal().substring(0, 12) + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nom + "\"")
                // Un dossier d'audit ne se met pas en cache : il consigne son
                // propre export, donc deux appels ne rendent pas le même
                // journal, et un intermédiaire qui rejouerait une réponse
                // masquerait les actes survenus entre-temps.
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(dossier);
    }
}
