package com.openlab.qualitos.quality.capa;

import com.openlab.qualitos.quality.aigateway.AiCompletionResult;
import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.nonconformity.NcStatus;
import com.openlab.qualitos.quality.nonconformity.NonConformityRepository;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class CapaService {

    /** Borne de génération (latence CPU raisonnable ; cf. ADR 0014). */
    private static final int SUGGEST_MAX_TOKENS = 320;
    private static final int SUGGEST_MAX = 8;

    private final CapaCaseRepository caseRepository;
    private final CapaActionRepository actionRepository;
    private final AiGatewayClient ai;
    /**
     * Un seul collaborateur pour tout ce qui se consigne : le service décrit ce
     * qui arrive au dossier, sans connaître ni le journal d'audit ni les abonnés.
     */
    private final CapaLifecycleJournal journal;
    /**
     * Lecture seule des non-conformités, pour le verrou de clôture : un dossier
     * ne se referme pas au-dessus d'un écart encore ouvert.
     */
    private final NonConformityRepository ncRepository;
    /**
     * Lecture seule des preuves : uniquement pour savoir si une action en porte
     * une avant de la supprimer. Le dépôt lui-même reste l'affaire de
     * {@link CapaEvidenceService} — ce service-ci ne connaît pas le stockage
     * objet et n'a pas à le connaître.
     */
    private final CapaEvidenceRepository evidenceRepository;

    public CapaService(CapaCaseRepository caseRepository, CapaActionRepository actionRepository,
                       AiGatewayClient ai, CapaLifecycleJournal journal,
                       NonConformityRepository ncRepository,
                       CapaEvidenceRepository evidenceRepository) {
        this.caseRepository = caseRepository;
        this.actionRepository = actionRepository;
        this.ai = ai;
        this.journal = journal;
        this.ncRepository = ncRepository;
        this.evidenceRepository = evidenceRepository;
    }

    /**
     * Statuts où une non-conformité est refermée. CANCELLED en fait partie : un
     * écart annulé n'attend plus rien, contrairement à un écart en cours.
     */
    private static final List<NcStatus> NC_SETTLED_STATUSES =
            List.of(NcStatus.RESOLVED, NcStatus.CLOSED, NcStatus.CANCELLED);

    @Transactional(readOnly = true)
    public Page<CapaDto.CaseResponse> findAll(CapaStatus status, Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<CapaCase> page = status != null
                ? caseRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                : caseRepository.findByTenantId(tenantId, pageable);
        return page.map(c -> toResponse(c, false));
    }

    @Transactional(readOnly = true)
    public CapaDto.CaseResponse findById(UUID id) {
        return toResponse(loadCase(id));
    }

    public CapaDto.CaseResponse createCase(CapaDto.CreateCaseRequest request) {
        UUID tenantId = requireTenantId();
        CapaCase c = new CapaCase();
        c.setTenantId(tenantId);
        c.setTitle(request.title());
        c.setDescription(request.description());
        c.setType(request.type());
        c.setCriticity(request.criticity());
        c.setSourceType(request.sourceType());
        c.setSourceRef(request.sourceRef());
        c.setOwnerId(request.ownerId());
        c.setRootCauseId(request.rootCauseId());
        c.setDueDate(request.dueDate());
        c.setStatus(CapaStatus.OPEN);
        CapaCase saved = caseRepository.save(c);
        journal.record(saved, CapaTransition.OPENED);
        return toResponse(saved);
    }

    public CapaDto.CaseResponse updateCase(UUID id, CapaDto.UpdateCaseRequest request) {
        CapaCase c = loadCase(id);
        if (c.getStatus() == CapaStatus.CLOSED || c.getStatus() == CapaStatus.REJECTED) {
            throw new CapaStateException("Cannot modify a " + c.getStatus() + " CAPA");
        }
        if (request.title() != null) c.setTitle(request.title());
        if (request.description() != null) c.setDescription(request.description());
        if (request.criticity() != null) c.setCriticity(request.criticity());
        if (request.sourceRef() != null) c.setSourceRef(request.sourceRef());
        if (request.rootCauseId() != null) c.setRootCauseId(request.rootCauseId());
        if (request.dueDate() != null) c.setDueDate(request.dueDate());
        CapaCase saved = caseRepository.save(c);
        journal.record(saved, CapaTransition.UPDATED);
        return toResponse(saved);
    }

    public CapaDto.CaseResponse startCase(UUID id) {
        CapaCase c = loadCase(id);
        if (c.getStatus() != CapaStatus.OPEN) {
            throw new CapaStateException("Only OPEN CAPA can be started");
        }
        c.setStatus(CapaStatus.IN_PROGRESS);
        CapaCase saved = caseRepository.save(c);
        journal.record(saved, CapaTransition.STARTED);
        return toResponse(saved);
    }

    public CapaDto.CaseResponse resolveCase(UUID id) {
        CapaCase c = loadCase(id);
        if (c.getStatus() != CapaStatus.IN_PROGRESS) {
            throw new CapaStateException("Only IN_PROGRESS CAPA can be resolved");
        }
        boolean allDone = !c.getActions().isEmpty()
                && c.getActions().stream().allMatch(a -> a.getStatus() == CapaActionStatus.DONE);
        if (!allDone) {
            throw new CapaStateException("All actions must be DONE before resolution");
        }
        c.setStatus(CapaStatus.RESOLVED);
        c.setResolvedAt(Instant.now());
        CapaCase saved = caseRepository.save(c);
        journal.record(saved, CapaTransition.RESOLVED);
        return toResponse(saved);
    }

    public CapaDto.CaseResponse verifyEffectiveness(UUID id, CapaDto.EffectivenessRequest request) {
        CapaCase c = loadCase(id);
        if (c.getStatus() != CapaStatus.RESOLVED) {
            throw new CapaStateException("Effectiveness check requires status RESOLVED");
        }
        boolean effective = Boolean.TRUE.equals(request.effective());
        if (effective) {
            // Toutes les actions doivent être faites — y compris celles ajoutées
            // APRÈS la résolution. `resolveCase` l'a vérifié à son moment, mais
            // rien n'interdit d'ajouter une action à un dossier RESOLVED, et le
            // dossier se serait alors clos au-dessus d'une action jamais menée.
            // C'est aussi ce que la fiche annonce (ACTIONS_NOT_DONE) : une liste
            // d'obstacles qui n'engage pas le serveur ne vaut rien, et l'écran
            // interdirait un geste que l'API accorderait.
            long notDone = c.getActions().stream()
                    .filter(a -> a.getStatus() != CapaActionStatus.DONE).count();
            if (notDone > 0) {
                throw new CapaStateException(notDone + " action(s) are not DONE: "
                        + "complete them before closing this CAPA");
            }
            // Un dossier qui n'a fait qu'endiguer n'a rien réglé : les mesures
            // d'endiguement arrêtent l'effet et laissent la cause intacte
            // (ISO 9001 §10.2). Le clore reviendrait à inscrire au registre que
            // le problème est traité, alors qu'il reviendra dès la mesure levée.
            // Le refus ne coûte rien aux dossiers antérieurs : ils portent tous
            // des actions correctives, ce type n'ayant jamais pu être saisi avant.
            if (isContainmentOnly(c.getActions())) {
                throw new CapaStateException(
                        "This CAPA carries containment actions only: they stop the effect "
                        + "without removing the cause. Add a corrective or preventive action "
                        + "before closing it");
            }
            // Clore une CAPA au-dessus d'un écart encore ouvert reviendrait à
            // déclarer le problème réglé pendant que le constat, lui, dit le
            // contraire. Le refus est explicite et chiffré : l'utilisateur sait
            // combien de non-conformités il lui reste à traiter.
            long stillOpen = ncRepository.countByTenantIdAndCapaCaseIdAndStatusNotIn(
                    c.getTenantId(), c.getId(), NC_SETTLED_STATUSES);
            if (stillOpen > 0) {
                throw new CapaStateException(stillOpen + " linked non-conformity(ies) are still open: "
                        + "close them before closing this CAPA");
            }
        }
        c.setEffectivenessVerified(request.effective());
        c.setEffectivenessVerifiedAt(Instant.now());
        if (effective) {
            c.setStatus(CapaStatus.CLOSED);
            c.setClosedAt(Instant.now());
        } else {
            c.setStatus(CapaStatus.IN_PROGRESS);
            c.setResolvedAt(null);
        }
        CapaCase saved = caseRepository.save(c);
        // Une efficacité NON démontrée est un fait au moins aussi important que
        // la clôture : elle dit que l'action corrective n'a pas produit son effet.
        journal.record(saved, effective ? CapaTransition.CLOSED : CapaTransition.EFFECTIVENESS_REJECTED);
        return toResponse(saved);
    }

    public CapaDto.CaseResponse rejectCase(UUID id) {
        CapaCase c = loadCase(id);
        if (c.getStatus() != CapaStatus.OPEN && c.getStatus() != CapaStatus.IN_PROGRESS) {
            throw new CapaStateException("Only OPEN or IN_PROGRESS CAPA can be rejected");
        }
        c.setStatus(CapaStatus.REJECTED);
        CapaCase saved = caseRepository.save(c);
        journal.record(saved, CapaTransition.REJECTED);
        return toResponse(saved);
    }

    public void deleteCase(UUID id) {
        CapaCase c = loadCase(id);
        if (c.getStatus() == CapaStatus.CLOSED) {
            throw new CapaStateException("Closed CAPA cannot be deleted");
        }
        // Consigné AVANT l'effacement : après, il ne reste rien à décrire.
        journal.record(c, CapaTransition.DELETED);
        caseRepository.delete(c);
    }

    // --- actions ---

    public CapaDto.ActionResponse addAction(UUID capaId, CapaDto.ActionRequest request) {
        CapaCase c = loadCase(capaId);
        if (c.getStatus() == CapaStatus.CLOSED || c.getStatus() == CapaStatus.REJECTED) {
            throw new CapaStateException("Cannot add actions to a " + c.getStatus() + " CAPA");
        }
        CapaAction a = new CapaAction();
        a.setCapa(c);
        a.setTitle(requireUsableTitle(request.title()));
        a.setDescription(request.description());
        a.setStatus(request.status() != null ? request.status() : CapaActionStatus.PENDING);
        // Corrective par défaut : c'est le cas de loin le plus fréquent, et
        // supposer l'endiguement obligerait à qualifier chaque action pour
        // écrire ce que tout le monde entend déjà par « action d'une CAPA ».
        a.setActionType(request.actionType() != null ? request.actionType() : CapaActionType.CORRECTIVE);
        a.setAssigneeId(request.assigneeId());
        a.setAssigneeName(normalizeName(request.assigneeName()));
        // La date de décision est SAISIE si l'appelant la donne, sinon DÉDUITE
        // du jour de l'enregistrement — et cette déduction est faite ici,
        // explicitement, pas en recyclant createdAt à l'affichage. Une action
        // décidée en comité et saisie plus tard se corrige par une mise à jour ;
        // une colonne qui afficherait created_at sous le nom « décidé le »
        // ne se corrigerait jamais, parce qu'elle mentirait sans le dire.
        a.setDecidedOn(request.decidedOn() != null ? request.decidedOn() : LocalDate.now());
        a.setDueDate(request.dueDate());
        return toActionResponse(actionRepository.save(a));
    }

    /**
     * Suggère par l'IA (passerelle → ai-service) des actions correctives/préventives
     * pour la CAPA (§4.2). L'IA suggère, l'humain valide : rien n'est persisté tant que
     * l'utilisateur ne clique pas « Ajouter ».
     */
    @Transactional(readOnly = true)
    public List<CapaDto.SuggestedAction> suggestActions(UUID capaId) {
        CapaCase c = loadCase(capaId);
        String kind = c.getType() == CapaType.PREVENTIVE
                ? "préventives (empêcher l'apparition du problème)"
                : "correctives (corriger et éliminer la cause racine)";

        String system = "Tu es un expert qualité (démarche CAPA). Pour le problème donné, propose "
                + "des actions " + kind + " concrètes et activables. Donne UNE action par ligne, "
                + "commençant par un verbe d'action. Exemple : - Réviser le plan de contrôle réception. "
                + "Donne 5 à 8 actions concrètes, sans numéro ni autre texte.";
        StringBuilder user = new StringBuilder("Problème : ").append(c.getTitle()).append("\n");
        if (c.getDescription() != null && !c.getDescription().isBlank()) {
            user.append("Détail : ").append(c.getDescription()).append("\n");
        }
        user.append("Type : ").append(c.getType())
            .append(", criticité : ").append(c.getCriticity()).append(".\n")
            .append("Liste les actions :");

        AiCompletionResult r = ai.complete(system, user.toString(), SUGGEST_MAX_TOKENS);
        return parseActions(r.text());
    }

    /** Une action par ligne ; ignore préambules/titres ; déduplique. */
    private List<CapaDto.SuggestedAction> parseActions(String text) {
        List<CapaDto.SuggestedAction> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        if (text == null) return out;
        for (String raw : text.split("\\r?\\n")) {
            String line = raw.strip().replaceFirst("^[-*•·\\d.)\\s]+", "").strip();
            if (line.length() < 5 || line.endsWith(":")) continue;
            String title = line;
            String description = null;
            if (title.length() > 255) { description = title; title = title.substring(0, 255); }
            if (!seen.add(title.toLowerCase(Locale.ROOT))) continue;
            out.add(new CapaDto.SuggestedAction(title, description));
            if (out.size() >= SUGGEST_MAX) break;
        }
        return out;
    }

    public CapaDto.ActionResponse updateAction(UUID capaId, UUID actionId, CapaDto.ActionRequest request) {
        CapaCase c = loadCase(capaId);
        if (c.getStatus() == CapaStatus.CLOSED || c.getStatus() == CapaStatus.REJECTED) {
            throw new CapaStateException("Cannot modify actions on a " + c.getStatus() + " CAPA");
        }
        CapaAction a = actionRepository.findByIdAndCapaId(actionId, capaId)
                .orElseThrow(() -> new CapaActionNotFoundException(actionId));
        // PATCH : un champ absent ne se touche pas. Un champ PRÉSENT, en
        // revanche, est validé ici — le contrôleur ne peut pas déléguer à
        // Jakarta sans rendre le titre obligatoire à chaque appel partiel.
        // Sans ce garde-fou, l'édition en ligne du tableau viderait le libellé
        // d'une action sur un simple espace.
        if (request.title() != null) a.setTitle(requireUsableTitle(request.title()));
        if (request.description() != null) a.setDescription(request.description());
        if (request.assigneeId() != null) a.setAssigneeId(request.assigneeId());
        if (request.assigneeName() != null) a.setAssigneeName(normalizeName(request.assigneeName()));
        if (request.decidedOn() != null) a.setDecidedOn(request.decidedOn());
        if (request.dueDate() != null) a.setDueDate(request.dueDate());
        if (request.actionType() != null) a.setActionType(request.actionType());
        if (request.status() != null) {
            a.setStatus(request.status());
            if (request.status() == CapaActionStatus.DONE && a.getCompletedAt() == null) {
                a.setCompletedAt(Instant.now());
            } else if (request.status() != CapaActionStatus.DONE) {
                a.setCompletedAt(null);
            }
        }
        return toActionResponse(actionRepository.save(a));
    }

    public void deleteAction(UUID capaId, UUID actionId) {
        CapaCase c = loadCase(capaId);
        if (c.getStatus() == CapaStatus.CLOSED || c.getStatus() == CapaStatus.REJECTED) {
            throw new CapaStateException("Cannot delete actions on a " + c.getStatus() + " CAPA");
        }
        CapaAction a = actionRepository.findByIdAndCapaId(actionId, capaId)
                .orElseThrow(() -> new CapaActionNotFoundException(actionId));
        // La contrainte de base efface en cascade la pièce rattachée à l'action,
        // mais pas le binaire dans le stockage objet — il resterait orphelin,
        // invisible et facturé. Surtout, une preuve d'audit disparaîtrait sans
        // que rien ne le consigne. Refus explicite : on retire la preuve
        // d'abord, ce qui laisse une trace (§11.5), puis l'action.
        if (evidenceRepository.countByTenantIdAndActionId(c.getTenantId(), actionId) > 0) {
            throw new CapaStateException(
                    "This action carries an evidence file: remove it before deleting the action");
        }
        actionRepository.delete(a);
    }

    // --- helpers ---

    /** Borne du libellé, alignée sur la colonne (255) — refusée, jamais tronquée en silence. */
    private static final int ACTION_TITLE_MAX = 255;

    /**
     * Un libellé d'action doit rester lisible : ni vide, ni fait d'espaces, ni
     * plus long que sa colonne. Tronquer serait pire que refuser — l'utilisateur
     * croirait avoir enregistré ce qu'il a tapé.
     */
    private static String requireUsableTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new CapaValidationException("Action title must not be blank");
        }
        String trimmed = title.strip();
        if (trimmed.length() > ACTION_TITLE_MAX) {
            throw new CapaValidationException(
                    "Action title exceeds " + ACTION_TITLE_MAX + " characters");
        }
        return trimmed;
    }

    /**
     * Nom du porteur : mis à blanc plutôt que conservé vide, pour que l'écran
     * ait une seule chose à tester. Une chaîne d'espaces afficherait une colonne
     * « Responsable » vide sans le repère « — » qui dit qu'elle l'est.
     */
    private static String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.strip();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > ACTION_TITLE_MAX) {
            throw new CapaValidationException(
                    "Assignee name exceeds " + ACTION_TITLE_MAX + " characters");
        }
        return trimmed;
    }

    /**
     * Écart d'origine du dossier, s'il y en a un.
     *
     * <p>Deux chemins, dans cet ordre : le lien réel porté par la NC
     * ({@code capa_case_id}, posé à l'escalade), puis à défaut la référence
     * saisie à la main dans le dossier. Le second n'est qu'un repli — une
     * référence tapée peut désigner un écart inexistant, auquel cas on ne
     * montre rien plutôt qu'un nom inventé.
     */
    private CapaDto.LinkedNonConformity resolveSourceNc(CapaCase c) {
        var linked = ncRepository.findFirstByTenantIdAndCapaCaseIdOrderByDetectedAtAsc(
                c.getTenantId(), c.getId());
        if (linked.isEmpty() && c.getSourceType() == CapaSourceType.NON_CONFORMITY
                && c.getSourceRef() != null && !c.getSourceRef().isBlank()) {
            linked = ncRepository.findByTenantIdAndReference(c.getTenantId(), c.getSourceRef().strip());
        }
        return linked
                .map(nc -> new CapaDto.LinkedNonConformity(nc.getId(), nc.getReference(), nc.getTitle()))
                .orElse(null);
    }

    private CapaCase loadCase(UUID id) {
        UUID tenantId = requireTenantId();
        return caseRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new CapaNotFoundException(id));
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    private CapaDto.CaseResponse toResponse(CapaCase c) {
        return toResponse(c, true);
    }

    /**
     * @param detailed résout l'écart d'origine ET les motifs de blocage — soit
     *        une requête de plus par dossier. La liste paginée s'en passe :
     *        vingt dossiers vaudraient vingt requêtes pour deux informations
     *        que la liste n'affiche pas.
     */
    private CapaDto.CaseResponse toResponse(CapaCase c, boolean detailed) {
        return new CapaDto.CaseResponse(
                c.getId(), c.getTenantId(), c.getTitle(), c.getDescription(),
                c.getType(), c.getCriticity(), c.getStatus(),
                c.getSourceType(), c.getSourceRef(), c.getOwnerId(),
                c.getRootCauseId(), c.getDueDate(),
                c.getResolvedAt(), c.getClosedAt(),
                c.getEffectivenessVerified(), c.getEffectivenessVerifiedAt(),
                c.getCreatedAt(), c.getUpdatedAt(),
                c.getActions().stream().map(this::toActionResponse).toList(),
                detailed ? resolveSourceNc(c) : null,
                detailed ? closureBlockers(c) : null);
    }

    private CapaDto.ActionResponse toActionResponse(CapaAction a) {
        return new CapaDto.ActionResponse(
                a.getId(), a.getCapa().getId(), a.getTitle(), a.getDescription(),
                a.getStatus(), a.getActionType(), a.getAssigneeId(), a.getAssigneeName(),
                a.getDecidedOn(), a.getDueDate(), a.getCompletedAt(),
                a.getCreatedAt(), a.getUpdatedAt());
    }

    /**
     * Énumère ce qui s'oppose encore à la clôture — avant le clic, pas après.
     *
     * <p>La liste couvre TOUT le chemin restant, y compris ce qui bloque la
     * résolution qui précède la clôture : l'utilisateur veut savoir ce qui le
     * sépare de la fin, pas ce qui bloque l'étape suivante prise isolément.
     *
     * <p>Un dossier déjà clos ou rejeté ne renvoie rien : plus rien ne s'y
     * oppose, et afficher des obstacles sur un dossier terminé donnerait à
     * croire qu'il faudrait encore agir.
     */
    private List<CapaDto.ClosureBlocker> closureBlockers(CapaCase c) {
        if (c.getStatus() == CapaStatus.CLOSED || c.getStatus() == CapaStatus.REJECTED) {
            return List.of();
        }
        List<CapaDto.ClosureBlocker> blockers = new ArrayList<>();
        List<CapaAction> actions = c.getActions();

        if (actions.isEmpty()) {
            blockers.add(new CapaDto.ClosureBlocker(ClosureBlockerCode.NO_ACTION, 0));
        } else {
            long notDone = actions.stream().filter(a -> a.getStatus() != CapaActionStatus.DONE).count();
            if (notDone > 0) {
                blockers.add(new CapaDto.ClosureBlocker(ClosureBlockerCode.ACTIONS_NOT_DONE, notDone));
            }
            if (isContainmentOnly(actions)) {
                blockers.add(new CapaDto.ClosureBlocker(
                        ClosureBlockerCode.CONTAINMENT_ONLY, actions.size()));
            }
        }

        long stillOpen = ncRepository.countByTenantIdAndCapaCaseIdAndStatusNotIn(
                c.getTenantId(), c.getId(), NC_SETTLED_STATUSES);
        if (stillOpen > 0) {
            blockers.add(new CapaDto.ClosureBlocker(
                    ClosureBlockerCode.OPEN_NON_CONFORMITIES, stillOpen));
        }
        return List.copyOf(blockers);
    }

    /**
     * Vrai quand le dossier ne porte QUE des mesures d'endiguement.
     *
     * <p>Un dossier sans action ne relève pas de ce cas : il a son propre motif
     * ({@link ClosureBlockerCode#NO_ACTION}), et un {@code allMatch} sur une
     * liste vide répondrait « oui » à une question qui ne se pose pas.
     */
    static boolean isContainmentOnly(List<CapaAction> actions) {
        return !actions.isEmpty()
                && actions.stream().allMatch(a -> a.getActionType() == CapaActionType.CONTAINMENT);
    }
}
