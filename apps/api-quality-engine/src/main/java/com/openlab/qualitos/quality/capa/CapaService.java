package com.openlab.qualitos.quality.capa;

import com.openlab.qualitos.quality.aigateway.AiCompletionResult;
import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    public CapaService(CapaCaseRepository caseRepository, CapaActionRepository actionRepository,
                       AiGatewayClient ai) {
        this.caseRepository = caseRepository;
        this.actionRepository = actionRepository;
        this.ai = ai;
    }

    @Transactional(readOnly = true)
    public Page<CapaDto.CaseResponse> findAll(CapaStatus status, Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<CapaCase> page = status != null
                ? caseRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                : caseRepository.findByTenantId(tenantId, pageable);
        return page.map(this::toResponse);
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
        return toResponse(caseRepository.save(c));
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
        return toResponse(caseRepository.save(c));
    }

    public CapaDto.CaseResponse startCase(UUID id) {
        CapaCase c = loadCase(id);
        if (c.getStatus() != CapaStatus.OPEN) {
            throw new CapaStateException("Only OPEN CAPA can be started");
        }
        c.setStatus(CapaStatus.IN_PROGRESS);
        return toResponse(caseRepository.save(c));
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
        return toResponse(caseRepository.save(c));
    }

    public CapaDto.CaseResponse verifyEffectiveness(UUID id, CapaDto.EffectivenessRequest request) {
        CapaCase c = loadCase(id);
        if (c.getStatus() != CapaStatus.RESOLVED) {
            throw new CapaStateException("Effectiveness check requires status RESOLVED");
        }
        c.setEffectivenessVerified(request.effective());
        c.setEffectivenessVerifiedAt(Instant.now());
        if (Boolean.TRUE.equals(request.effective())) {
            c.setStatus(CapaStatus.CLOSED);
            c.setClosedAt(Instant.now());
        } else {
            c.setStatus(CapaStatus.IN_PROGRESS);
            c.setResolvedAt(null);
        }
        return toResponse(caseRepository.save(c));
    }

    public CapaDto.CaseResponse rejectCase(UUID id) {
        CapaCase c = loadCase(id);
        if (c.getStatus() != CapaStatus.OPEN && c.getStatus() != CapaStatus.IN_PROGRESS) {
            throw new CapaStateException("Only OPEN or IN_PROGRESS CAPA can be rejected");
        }
        c.setStatus(CapaStatus.REJECTED);
        return toResponse(caseRepository.save(c));
    }

    public void deleteCase(UUID id) {
        CapaCase c = loadCase(id);
        if (c.getStatus() == CapaStatus.CLOSED) {
            throw new CapaStateException("Closed CAPA cannot be deleted");
        }
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
        a.setTitle(request.title());
        a.setDescription(request.description());
        a.setStatus(request.status() != null ? request.status() : CapaActionStatus.PENDING);
        a.setAssigneeId(request.assigneeId());
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
        if (request.title() != null) a.setTitle(request.title());
        if (request.description() != null) a.setDescription(request.description());
        if (request.assigneeId() != null) a.setAssigneeId(request.assigneeId());
        if (request.dueDate() != null) a.setDueDate(request.dueDate());
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
        actionRepository.delete(a);
    }

    // --- helpers ---

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
        return new CapaDto.CaseResponse(
                c.getId(), c.getTenantId(), c.getTitle(), c.getDescription(),
                c.getType(), c.getCriticity(), c.getStatus(),
                c.getSourceType(), c.getSourceRef(), c.getOwnerId(),
                c.getRootCauseId(), c.getDueDate(),
                c.getResolvedAt(), c.getClosedAt(),
                c.getEffectivenessVerified(), c.getEffectivenessVerifiedAt(),
                c.getCreatedAt(), c.getUpdatedAt(),
                c.getActions().stream().map(this::toActionResponse).toList());
    }

    private CapaDto.ActionResponse toActionResponse(CapaAction a) {
        return new CapaDto.ActionResponse(
                a.getId(), a.getCapa().getId(), a.getTitle(), a.getDescription(),
                a.getStatus(), a.getAssigneeId(), a.getDueDate(), a.getCompletedAt(),
                a.getCreatedAt(), a.getUpdatedAt());
    }
}
