package com.openlab.qualitos.quality.spc.capability.web;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
import com.openlab.qualitos.quality.config.RequiresModule;
import com.openlab.qualitos.quality.spc.capability.application.SpcStudyDto;
import com.openlab.qualitos.quality.spc.capability.application.SpcStudyService;
import com.openlab.qualitos.quality.spc.capability.domain.SpcValidationException;
import com.openlab.qualitos.quality.spc.capability.workbook.SpcMeasurementsWorkbook;
import com.openlab.qualitos.quality.nonconformity.storage.UploadedBinaryGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

/**
 * Le registre des études de capabilité SPC (Cp / Cpk, cartes X̄ – R et X̄ – S).
 *
 * <p><b>Lire est ouvert à tout authentifié ; écrire demande l'action
 * {@code spc.manage}</b>, accordée par défaut aux profils de pilotage et
 * réglable dans la matrice des droits du client (ADR 0078).
 *
 * <p><b>Module « dmaic ».</b> La capabilité procédé relève du pilotage Six
 * Sigma (§3.4) : sans abonnement, les écritures sont refusées ; les lectures
 * restent permises, pour qu'une résiliation ne rende pas une preuve illisible.
 *
 * <p>Les écritures sont transactionnelles ICI : l'étude et sa trace d'audit
 * sont validées ensemble ou pas du tout.
 */
@RestController
@RequestMapping("/api/v1/spc/studies")
@PreAuthorize("isAuthenticated()")
@RequiresModule("dmaic")
@Tag(name = "SPC studies", description = "Process capability studies (Cp / Cpk) on X-bar / R and X-bar / S charts")
public class SpcStudyController {

    private final SpcStudyService service;

    public SpcStudyController(SpcStudyService service) {
        this.service = service;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "The study register, most recent first, without the measurement grids")
    public List<SpcStudyDto.StudySummary> list() {
        return service.list();
    }

    /**
     * Le classeur modèle de la grille de mesures.
     *
     * <p>Sans modèle, chacun invente sa disposition et l'import devrait devenir
     * devin : le fichier porte une ligne par pièce, une colonne par sous-groupe,
     * et un onglet qui dit quoi en faire. Télécharger est ouvert à tout
     * authentifié — c'est un formulaire vierge, il ne contient aucune donnée du
     * client.
     */
    @GetMapping(value = "/measurements-template.xlsx", produces = SpcMeasurementsWorkbook.XLSX)
    @Operation(summary = "The blank measurement grid as an Excel workbook, ready to fill in")
    public ResponseEntity<byte[]> measurementsTemplate(
            @RequestParam(name = "sampleSize", defaultValue = "5") int sampleSize,
            @RequestParam(name = "subgroupCount", defaultValue = "20") int subgroupCount) {
        byte[] contenu = SpcMeasurementsWorkbook.template(sampleSize, subgroupCount);
        String nom = "mesures-capabilite-" + sampleSize + "x" + subgroupCount + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nom).build().toString())
                .contentType(MediaType.parseMediaType(SpcMeasurementsWorkbook.XLSX))
                .body(contenu);
    }

    /**
     * Lit un classeur rempli et rend la grille, sans rien enregistrer.
     *
     * <p>L'écran la pose dans sa grille de saisie : l'utilisateur la relit, la
     * corrige, et c'est lui qui enregistre l'étude. Un import qui créerait
     * l'étude lui-même empêcherait de voir ce qui a été lu avant de le signer.
     *
     * <p><b>Gardes (OWASP).</b> Le type déclaré doit être celui d'un .xlsx, les
     * premiers octets doivent lui correspondre — un exécutable renommé ne passe
     * pas — et la taille est bornée avant que POI n'ouvre l'archive. Écrire
     * demande {@code spc.manage} : l'import remplace la grille de l'écran.
     */
    @PostMapping(value = "/measurements-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiresPermission(Permission.SPC_MANAGE)
    @Operation(summary = "Read a filled-in Excel workbook and return the measurement grid")
    public SpcMeasurementsWorkbook.ImportedGrid importMeasurements(
            @RequestParam("file") MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new SpcValidationException("file", "Aucun fichier reçu.");
        }
        if (fichier.getSize() > SpcMeasurementsWorkbook.MAX_UPLOAD_BYTES) {
            throw new SpcValidationException("file", "Le fichier dépasse "
                    + (SpcMeasurementsWorkbook.MAX_UPLOAD_BYTES / (1024 * 1024)) + " Mo.");
        }
        byte[] contenu;
        try {
            contenu = fichier.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture du classeur de mesures versé", e);
        }
        String type = UploadedBinaryGuard.normalizeType(fichier.getContentType());
        if (!SpcMeasurementsWorkbook.XLSX.equals(type)
                || !UploadedBinaryGuard.magicBytesMatch(type, contenu)) {
            throw new SpcValidationException("file",
                    "Attendu un classeur Excel .xlsx (ni .xls, ni .csv, ni autre chose).");
        }
        return SpcMeasurementsWorkbook.read(contenu);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "A study: identification, measurement grid and the full calculation")
    public SpcStudyDto.StudySheet get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission(Permission.SPC_MANAGE)
    @Transactional
    @Operation(summary = "Record a study; the server computes Cp and Cpk and freezes the tenant thresholds")
    public SpcStudyDto.StudySheet create(@Valid @RequestBody SpcStudyWebDto.StudyRequest requete) {
        return service.create(commande(requete));
    }

    @PutMapping("/{id}")
    @RequiresPermission(Permission.SPC_MANAGE)
    @Transactional
    @Operation(summary = "Correct a study; Cp and Cpk are recomputed, its thresholds are kept")
    public SpcStudyDto.StudySheet revise(@PathVariable UUID id,
                                         @Valid @RequestBody SpcStudyWebDto.StudyRequest requete) {
        return service.revise(id, commande(requete));
    }

    @PatchMapping("/{id}/action-plan")
    @RequiresPermission(Permission.SPC_MANAGE)
    @Transactional
    @Operation(summary = "Tick or untick the action plan from the register")
    public SpcStudyDto.StudySummary actionPlan(@PathVariable UUID id,
                                               @Valid @RequestBody SpcStudyWebDto.ActionPlanRequest requete) {
        return service.setActionPlan(id, requete.actionPlan());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission(Permission.SPC_MANAGE)
    @Transactional
    @Operation(summary = "Delete a study")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    private static SpcStudyDto.StudyCommand commande(SpcStudyWebDto.StudyRequest r) {
        return new SpcStudyDto.StudyCommand(r.partReference(), r.characteristic(), r.specification(),
                r.machine(), r.leaderUserId(), r.leaderName(), r.studyDate(), r.actionPlan(),
                r.lsl(), r.usl(), r.subgroupSize(), r.measurements());
    }
}
