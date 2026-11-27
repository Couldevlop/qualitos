package com.openlab.qualitos.quality.capa;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/capa")
public class CapaController {

    private final CapaService service;

    public CapaController(CapaService service) {
        this.service = service;
    }

    /**
     * Le registre, filtré par statut, criticité, pilote et échéance limite.
     *
     * <p>Le pilote arrive par son compte ({@code sub}), jamais par son nom : deux
     * personnes peuvent porter le même, et un nom se réécrit.
     *
     * <p>{@code dueBefore} rend ce qui échoit à cette date ou avant : c'est la
     * question qu'on pose à un registre d'actions — « qu'est-ce qui tombe
     * cette semaine » — et elle se tranchait jusqu'ici à l'œil, colonne par
     * colonne, sur une liste paginée.
     */
    @GetMapping("/cases")
    public Page<CapaDto.CaseResponse> list(
            @RequestParam(required = false) CapaStatus status,
            @RequestParam(required = false) CapaCriticity criticity,
            @RequestParam(required = false) UUID ownerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dueBefore,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.findAll(status, criticity, ownerId, dueBefore, pageable);
    }

    @PostMapping("/cases")
    @RequiresPermission(Permission.CAPA_CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    public CapaDto.CaseResponse create(@Valid @RequestBody CapaDto.CreateCaseRequest request) {
        return service.createCase(request);
    }

    /**
     * Ouvre un dossier avec son action unique, depuis la liste CAPA (ADR 0088).
     *
     * <p>La source est choisie librement, sauf {@code RISK} : un dossier de
     * risque se rattache à sa fiche par la référence R-xxx, et seul le
     * registre peut la poser — sans quoi n'importe qui ferait apparaître un
     * dossier dans le traitement d'un risque qu'il n'a pas ouvert. Depuis une
     * NC, un constat ou un risque, c'est l'écran d'origine qui ouvre le
     * dossier, par sa propre route, et fixe la source.
     */
    @PostMapping("/cases/open")
    @RequiresPermission(Permission.CAPA_CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    public CapaDto.CaseResponse open(@Valid @RequestBody CapaDto.OpenCaseRequest request) {
        CapaSourceType source = request.sourceType() != null ? request.sourceType() : CapaSourceType.INTERNAL;
        if (source == CapaSourceType.RISK) {
            throw new CapaValidationException("A CAPA on a risk is opened from the risk register");
        }
        return service.openCase(request, source, request.sourceRef());
    }

    @GetMapping("/cases/{id}")
    public CapaDto.CaseResponse get(@PathVariable UUID id) { return service.findById(id); }

    @PatchMapping("/cases/{id}")
    @RequiresPermission(Permission.CAPA_EDIT)
    public CapaDto.CaseResponse update(@PathVariable UUID id,
                                       @Valid @RequestBody CapaDto.UpdateCaseRequest request) {
        return service.updateCase(id, request);
    }

    // Plus de /start ni de /resolve : le statut du dossier se déduit de ses
    // actions (ADR 0117). Les deux verbes ne décidaient rien — /resolve
    // refusait déjà d'aboutir tant qu'une action restait à faire, et /start ne
    // faisait qu'annoncer un travail dont la première action témoigne mieux.

    /**
     * Le verdict d'efficacite du dossier (ADR 0108).
     *
     * <p>Meme droit qu'avant — {@code capa.verify} —, mais la garde est portee
     * par le SERVICE (ADR 0143) : la PERSONNE DESIGNEE sur le dossier, ou sur
     * une de ses actions, porte son verdict sans detenir ce droit, et nul autre
     * ne le peut. Un refus pose ici, avant de connaitre le dossier visé, ne
     * savait pas qui en etait le verificateur et refusait donc a celui-la meme
     * a qui la verification avait ete confiee.
     */
    @PatchMapping("/cases/{id}/effectiveness")
    public CapaDto.CaseResponse verifyEffectiveness(
            @PathVariable UUID id,
            @Valid @RequestBody CapaDto.EffectivenessRequest request) {
        return service.verifyEffectiveness(id, request);
    }

    /**
     * Le jugement porte sur UNE action du dossier (ADR 0132).
     *
     * <p>Meme droit que le jugement du dossier — {@code capa.verify} —, c'est le
     * meme geste a l'echelle ou la promesse de verification a ete faite. Mais la
     * garde est portee par le SERVICE et non par l'intercepteur (ADR 0143) : la
     * PERSONNE DESIGNEE sur l'action peut porter son verdict sur cette action-la
     * sans detenir ce droit, et nul autre ne le peut. Un refus pose ici, avant
     * de connaitre l'action visee, ne savait pas qui en etait le verificateur et
     * refusait donc a celui-la meme a qui la verification avait ete confiee.
     *
     * <p>Le catalogue des droits n'en est pas elargi d'un pouce : {@code tous()}
     * aurait ouvert le jugement a chacun, et c'est la designation, non le role,
     * qui ouvre ce chemin-ci.
     */
    @PatchMapping("/cases/{id}/actions/{actionId}/effectiveness")
    public CapaDto.ActionResponse verifyActionEffectiveness(
            @PathVariable UUID id,
            @PathVariable UUID actionId,
            @Valid @RequestBody CapaDto.EffectivenessRequest request) {
        return service.verifyActionEffectiveness(id, actionId, request);
    }

    /**
     * Clôt un dossier qui n'a promis aucune vérification, sans écrire de verdict.
     *
     * <p>Même permission que le jugement d'efficacité : c'est la même décision
     * — fermer le dossier — prise là où il n'y a rien à juger. Un droit distinct
     * laisserait fermer sans vérifier à qui on refuse de vérifier.
     */
    @PostMapping("/cases/{id}/close")
    @RequiresPermission(Permission.CAPA_VERIFY)
    public CapaDto.CaseResponse close(@PathVariable UUID id) {
        return service.closeWithoutVerification(id);
    }

    @PatchMapping("/cases/{id}/reject")
    @RequiresPermission(Permission.CAPA_REJECT)
    public CapaDto.CaseResponse reject(@PathVariable UUID id) { return service.rejectCase(id); }

    @DeleteMapping("/cases/{id}")
    @RequiresPermission(Permission.CAPA_DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.deleteCase(id); }

    @PostMapping("/cases/{id}/actions")
    @RequiresPermission(Permission.CAPA_ACTION_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public CapaDto.ActionResponse addAction(@PathVariable UUID id,
                                            @Valid @RequestBody CapaDto.ActionRequest request) {
        return service.addAction(id, request);
    }

    @PatchMapping("/cases/{id}/actions/{actionId}")
    @RequiresPermission(Permission.CAPA_ACTION_UPDATE)
    public CapaDto.ActionResponse updateAction(
            @PathVariable UUID id,
            @PathVariable UUID actionId,
            @RequestBody CapaDto.ActionRequest request) {
        return service.updateAction(id, actionId, request);
    }

    @DeleteMapping("/cases/{id}/actions/{actionId}")
    @RequiresPermission(Permission.CAPA_ACTION_MANAGE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAction(@PathVariable UUID id, @PathVariable UUID actionId) {
        service.deleteAction(id, actionId);
    }
}
