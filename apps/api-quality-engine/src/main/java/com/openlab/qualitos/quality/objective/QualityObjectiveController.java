package com.openlab.qualitos.quality.objective;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Les objectifs qualité : les définir, les revoir face aux KPI réels, et
 * trancher la CAPA qu'un écart propose (ISO 9001 §6.2).
 *
 * <p>La LECTURE est ouverte à tout authentifié — la revue des objectifs est ce
 * qu'une organisation montre à ses équipes. L'ÉCRITURE porte
 * {@code objective.manage} : fixer une cible est une décision, et déplacer la
 * cible d'un objectif qu'on n'atteint pas le ferait passer au vert sans que rien
 * n'ait changé sur le terrain.
 *
 * <p>Ouvrir la CAPA d'un écart ou rejeter une proposition en relève aussi :
 * c'est l'arbitrage du pilote, pas une consultation.
 */
@RestController
@RequestMapping("/api/v1/quality-objectives")
public class QualityObjectiveController {

    private final QualityObjectiveService service;

    public QualityObjectiveController(QualityObjectiveService service) {
        this.service = service;
    }

    /**
     * L'écran « Objectifs vs KPI réels ».
     *
     * @param processId filtre sur un processus ; absent, tous les processus
     * @param asOf      fin de la période observée ({@code AAAA-MM-JJ}) ; absente, l'état
     *                  courant. Une revue mensuelle se tient après la fin du mois et doit
     *                  pouvoir se rejouer telle qu'elle a été lue.
     */
    @GetMapping
    public ObjectiveDto.ReviewResponse review(
            @RequestParam(required = false) UUID processId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return service.review(processId, asOf);
    }

    @GetMapping("/{id}")
    public ObjectiveDto.ObjectiveResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    /** L'historique des propositions d'un objectif : acceptées, rejetées, en attente. */
    @GetMapping("/{id}/proposals")
    public List<ObjectiveDto.ProposalResponse> history(@PathVariable UUID id) {
        return service.history(id);
    }

    @RequiresPermission(Permission.OBJECTIVE_MANAGE)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ObjectiveDto.ObjectiveResponse create(
            @Valid @RequestBody ObjectiveDto.CreateRequest req) {
        return service.create(req);
    }

    @RequiresPermission(Permission.OBJECTIVE_MANAGE)
    @PatchMapping("/{id}")
    public ObjectiveDto.ObjectiveResponse update(
            @PathVariable UUID id, @Valid @RequestBody ObjectiveDto.UpdateRequest req) {
        return service.update(id, req);
    }

    /**
     * Retire l'objectif du suivi. Il n'est PAS effacé : effacer l'objectif d'un
     * trimestre passé effacerait la raison des CAPA qu'il a fait ouvrir.
     */
    @RequiresPermission(Permission.OBJECTIVE_MANAGE)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id) {
        service.archive(id);
    }

    /**
     * Confronte l'objectif à ses mesures et, si la règle déclenche, rédige une
     * CAPA à soumettre au pilote.
     *
     * <p>{@code 200} avec la proposition si la règle déclenche, {@code 204} si
     * elle ne déclenche pas : l'écran doit pouvoir distinguer « rien à proposer »
     * d'une réponse vide par erreur.
     */
    @RequiresPermission(Permission.OBJECTIVE_MANAGE)
    @PostMapping("/{id}/evaluate")
    public ResponseEntity<ObjectiveDto.ProposalResponse> evaluate(@PathVariable UUID id) {
        return service.evaluate(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Confronte tous les objectifs suivis, et rend les propositions nées de ce passage. */
    @RequiresPermission(Permission.OBJECTIVE_MANAGE)
    @PostMapping("/evaluate")
    public List<ObjectiveDto.ProposalResponse> evaluateAll() {
        return service.evaluateAll();
    }

    /**
     * « Ajouter une action » : la fenêtre CAPA s'ouvre sur la ligne d'un objectif
     * en alerte ou critique, et le dossier naît de ce qui y est saisi.
     *
     * <p>Le corps ne porte ni source, ni criticité, ni vérification : le serveur
     * les impose (ADR 0143). Une proposition encore en attente est soldée par ce
     * dossier.
     */
    @RequiresPermission(Permission.OBJECTIVE_MANAGE)
    @PostMapping("/{id}/capa")
    @ResponseStatus(HttpStatus.CREATED)
    public ObjectiveDto.CapaOpenedResponse openCapa(
            @PathVariable UUID id, @Valid @RequestBody ObjectiveDto.OpenCapaRequest req) {
        return service.openCapa(id, req);
    }

    /** « Rejeter avec justification » : le motif est obligatoire. */
    @RequiresPermission(Permission.OBJECTIVE_MANAGE)
    @PostMapping("/proposals/{proposalId}/reject")
    public ObjectiveDto.ProposalResponse reject(
            @PathVariable UUID proposalId,
            @Valid @RequestBody ObjectiveDto.RejectRequest req) {
        return service.reject(proposalId, req.reason());
    }
}
