package com.openlab.qualitos.quality.audit;

import com.openlab.qualitos.quality.aigateway.AiCompletionResult;
import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.standards.ObligationLevel;
import com.openlab.qualitos.quality.standards.Standard;
import com.openlab.qualitos.quality.standards.StandardClause;
import com.openlab.qualitos.quality.standards.StandardNotFoundException;
import com.openlab.qualitos.quality.standards.StandardRepository;
import com.openlab.qualitos.quality.standards.StandardRequirement;
import com.openlab.qualitos.quality.standards.StandardSection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AuditService {

    /** Borne de génération du rapport d'audit (texte plus long qu'une suggestion). */
    private static final int REPORT_MAX_TOKENS = 700;

    /** Horizon par défaut du planning, en jours. Un trimestre : ce qui se prépare. */
    static final int PLANNING_DEFAULT_HORIZON_DAYS = 90;

    /** Horizon maximal accepté — au-delà, l'appelant demande un export, pas un planning. */
    static final int PLANNING_MAX_HORIZON_DAYS = 730;

    /**
     * Plafond de lignes renvoyées. Le planning n'est pas paginé (il se lit d'un
     * coup d'œil) ; sans plafond, un tenant chargé ferait tomber un millier de
     * lignes dans le navigateur pour un écran qui en montre trente.
     */
    static final int PLANNING_MAX_RESULTS = 200;

    private final AuditPlanRepository planRepository;
    private final AuditChecklistItemRepository checklistRepository;
    private final AuditFindingRepository findingRepository;
    private final StandardRepository standardRepository;
    private final AiGatewayClient ai;
    private final Clock clock;

    public AuditService(AuditPlanRepository planRepository,
                        AuditChecklistItemRepository checklistRepository,
                        AuditFindingRepository findingRepository,
                        StandardRepository standardRepository,
                        AiGatewayClient ai,
                        Clock clock) {
        this.planRepository = planRepository;
        this.checklistRepository = checklistRepository;
        this.findingRepository = findingRepository;
        this.standardRepository = standardRepository;
        this.ai = ai;
        this.clock = clock;
    }

    /**
     * Génère le rapport d'audit final par LLM (CLAUDE.md §1.4/§4.4) à partir du
     * périmètre, de la norme, des réponses de checklist et des constats. Tenant-scopé
     * (loadPlan). Le résumé généré est persisté dans {@code reportSummary}. Un service
     * IA indisponible remonte une {@code AiGatewayException} (mappée 502/503).
     */
    public AuditDto.PlanResponse generateReport(UUID id) {
        AuditPlan p = loadPlan(id);

        String systemPrompt = """
                Tu es un auditeur qualité senior (QualitOS §1.4/§4.4). À partir des
                éléments d'un audit (périmètre, norme, réponses de checklist, constats),
                rédige un RAPPORT D'AUDIT synthétique et professionnel en français :
                contexte, points conformes, non-conformités (majeures/mineures) avec la
                clause concernée, puis une conclusion avec recommandations. Texte clair
                de 5 à 12 phrases, sans markdown ni listes à puces.
                """;

        StringBuilder ctx = new StringBuilder();
        ctx.append("Titre : ").append(p.getTitle()).append('\n');
        if (p.getStandard() != null) {
            ctx.append("Norme visée : ").append(p.getStandard()).append('\n');
        }
        if (p.getScope() != null) {
            ctx.append("Périmètre : ").append(p.getScope()).append('\n');
        }
        ctx.append("\nRéponses de checklist :\n");
        if (p.getChecklist().isEmpty()) {
            ctx.append("(aucune)\n");
        }
        for (AuditChecklistItem i : p.getChecklist()) {
            String verdict = i.getConformant() == null ? "non évalué"
                    : (Boolean.TRUE.equals(i.getConformant()) ? "Conforme" : "NON CONFORME");
            ctx.append("- [").append(verdict).append("] ").append(i.getQuestion());
            if (i.getClauseRef() != null) {
                ctx.append(" (clause ").append(i.getClauseRef()).append(')');
            }
            if (i.getResponse() != null && !i.getResponse().isBlank()) {
                ctx.append(" — ").append(i.getResponse());
            }
            ctx.append('\n');
        }
        ctx.append("\nConstats :\n");
        if (p.getFindings().isEmpty()) {
            ctx.append("(aucun)\n");
        }
        for (AuditFinding f : p.getFindings()) {
            ctx.append("- [").append(f.getType()).append("] ").append(f.getDescription());
            if (f.getClauseRef() != null) {
                ctx.append(" (clause ").append(f.getClauseRef()).append(')');
            }
            ctx.append('\n');
        }

        AiCompletionResult result = ai.complete(systemPrompt, ctx.toString(), REPORT_MAX_TOKENS);
        p.setReportSummary(result.text().trim());
        return toResponse(planRepository.save(p));
    }

    // --- plans ---

    @Transactional(readOnly = true)
    public Page<AuditDto.PlanResponse> findAll(AuditStatus status, AuditType type, Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<AuditPlan> page;
        if (status != null) {
            page = planRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        } else if (type != null) {
            page = planRepository.findByTenantIdAndType(tenantId, type, pageable);
        } else {
            page = planRepository.findByTenantId(tenantId, pageable);
        }
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AuditDto.PlanResponse findById(UUID id) {
        return toResponse(loadPlan(id));
    }

    /**
     * Planning des audits à venir du tenant courant (§4.4), du plus proche au plus
     * lointain, retards compris.
     *
     * <p>{@code horizonDays} est borné des deux côtés plutôt que refusé : un
     * planning n'a pas à répondre 400 parce qu'on lui a demandé « 5 ans ». Un
     * horizon nul ou négatif retombe sur le défaut — la seule lecture sensée d'une
     * demande absurde, et elle ne renvoie jamais un écran vide inexplicable.
     */
    @Transactional(readOnly = true)
    public List<AuditDto.PlanningEntry> planning(AuditType type, Integer horizonDays) {
        UUID tenantId = requireTenantId();
        LocalDate today = LocalDate.now(clock);
        LocalDate horizon = today.plusDays(clampHorizon(horizonDays));
        Pageable cap = PageRequest.of(0, PLANNING_MAX_RESULTS);

        List<AuditPlan> plans = (type == null)
                ? planRepository.findUpcoming(tenantId, AuditStatus.PLANNED, horizon, cap)
                : planRepository.findUpcomingByType(tenantId, AuditStatus.PLANNED, type, horizon, cap);

        return plans.stream().map(p -> toPlanningEntry(p, today)).toList();
    }

    static int clampHorizon(Integer horizonDays) {
        if (horizonDays == null || horizonDays <= 0) {
            return PLANNING_DEFAULT_HORIZON_DAYS;
        }
        return Math.min(horizonDays, PLANNING_MAX_HORIZON_DAYS);
    }

    static AuditDto.PlanningEntry toPlanningEntry(AuditPlan p, LocalDate today) {
        long daysUntil = ChronoUnit.DAYS.between(today, p.getScheduledDate());
        return new AuditDto.PlanningEntry(
                p.getId(), p.getReference(), p.getTitle(), p.getType(), p.getStatus(), p.getStandard(),
                p.getLeadAuditorId(), p.getScheduledDate(), daysUntil, daysUntil < 0,
                p.getReminderSentAt() != null);
    }

    public AuditDto.PlanResponse createPlan(AuditDto.CreatePlanRequest req) {
        UUID tenantId = requireTenantId();
        AuditPlan p = new AuditPlan();
        p.setReference(generateReference(tenantId));
        p.setTenantId(tenantId);
        p.setTitle(req.title());
        p.setScope(req.scope());
        p.setType(req.type());
        p.setStatus(AuditStatus.PLANNED);
        p.setStandard(req.standard());
        p.setLeadAuditorId(req.leadAuditorId());
        p.setAuditeeId(req.auditeeId());
        p.setScheduledDate(req.scheduledDate());
        p.setReminderEmail(normalizeEmail(req.reminderEmail()));
        return toResponse(planRepository.save(p));
    }

    public AuditDto.PlanResponse updatePlan(UUID id, AuditDto.UpdatePlanRequest req) {
        AuditPlan p = loadPlan(id);
        if (p.getStatus() == AuditStatus.COMPLETED || p.getStatus() == AuditStatus.CANCELLED) {
            throw new AuditStateException("Cannot modify a " + p.getStatus() + " audit plan");
        }
        if (req.title() != null) p.setTitle(req.title());
        if (req.scope() != null) p.setScope(req.scope());
        if (req.type() != null) p.setType(req.type());
        if (req.standard() != null) p.setStandard(req.standard());
        if (req.leadAuditorId() != null) p.setLeadAuditorId(req.leadAuditorId());
        if (req.auditeeId() != null) p.setAuditeeId(req.auditeeId());
        if (req.scheduledDate() != null) p.setScheduledDate(req.scheduledDate());
        // Chaîne vide = « retire le destinataire ». Sans ce cas, une adresse posée
        // par erreur serait indéboulonnable : la convention « null = ne touche pas »
        // du reste du PATCH ne laisserait aucun moyen de l'effacer.
        if (req.reminderEmail() != null) p.setReminderEmail(normalizeEmail(req.reminderEmail()));
        return toResponse(planRepository.save(p));
    }

    public AuditDto.PlanResponse startPlan(UUID id) {
        AuditPlan p = loadPlan(id);
        if (p.getStatus() != AuditStatus.PLANNED) {
            throw new AuditStateException("Only PLANNED audits can be started");
        }
        if (p.getChecklist().isEmpty()) {
            throw new AuditStateException("Cannot start an audit without checklist items");
        }
        p.setStatus(AuditStatus.IN_PROGRESS);
        p.setStartedAt(Instant.now());
        return toResponse(planRepository.save(p));
    }

    public AuditDto.PlanResponse completePlan(UUID id, AuditDto.CompleteRequest req) {
        AuditPlan p = loadPlan(id);
        if (p.getStatus() != AuditStatus.IN_PROGRESS) {
            throw new AuditStateException("Only IN_PROGRESS audits can be completed");
        }
        boolean allAnswered = p.getChecklist().stream()
                .allMatch(i -> i.getConformant() != null);
        if (!allAnswered) {
            throw new AuditStateException("All checklist items must be answered before completion");
        }
        p.setStatus(AuditStatus.COMPLETED);
        p.setCompletedAt(Instant.now());
        if (req != null && req.reportSummary() != null) {
            p.setReportSummary(req.reportSummary());
        }
        return toResponse(planRepository.save(p));
    }

    public AuditDto.PlanResponse cancelPlan(UUID id) {
        AuditPlan p = loadPlan(id);
        if (p.getStatus() == AuditStatus.COMPLETED) {
            throw new AuditStateException("Completed audit cannot be cancelled");
        }
        if (p.getStatus() == AuditStatus.CANCELLED) {
            throw new AuditStateException("Audit is already cancelled");
        }
        p.setStatus(AuditStatus.CANCELLED);
        return toResponse(planRepository.save(p));
    }

    public void deletePlan(UUID id) {
        AuditPlan p = loadPlan(id);
        if (p.getStatus() == AuditStatus.COMPLETED) {
            throw new AuditStateException("Completed audit cannot be deleted");
        }
        planRepository.delete(p);
    }

    // --- checklist ---

    /**
     * Tire la checklist d'un audit des exigences d'un référentiel du catalogue (§8).
     *
     * <p>Les items produits sont des lignes AUTONOMES, pas des renvois vers les
     * exigences : une clause corrigée en mars ne doit pas réécrire le rapport de
     * janvier. C'est une photo du référentiel au moment où l'audit a été préparé,
     * et c'est cette photo qu'un auditeur externe relira.
     *
     * <p>Refusé si la checklist n'est pas vide : deux jeux de questions mêlés, et
     * plus personne ne sait lequel fait foi. Refusé aussi dès que l'audit a
     * quitté l'état PLANNED — y verser des dizaines de questions alors qu'il est
     * en cours reviendrait à changer le sujet de l'examen pendant l'épreuve.
     */
    public List<AuditDto.ChecklistItemResponse> generateChecklistFromStandard(UUID planId, UUID standardId) {
        AuditPlan p = loadPlan(planId);
        if (p.getStatus() != AuditStatus.PLANNED) {
            throw new AuditStateException(
                    "Checklist can only be generated while audit is PLANNED, not " + p.getStatus());
        }
        if (!p.getChecklist().isEmpty()) {
            throw new AuditStateException(
                    "Checklist is not empty: clear it before generating from a standard");
        }
        Standard standard = standardRepository.findVisibleById(standardId, requireTenantId())
                .orElseThrow(() -> new StandardNotFoundException(standardId));

        List<AuditDto.ChecklistItemResponse> generated = new ArrayList<>();
        int order = 0;
        for (StandardSection section : standard.getSections()) {
            for (StandardClause clause : section.getClauses()) {
                for (StandardRequirement requirement : clause.getRequirements()) {
                    AuditChecklistItem item = new AuditChecklistItem();
                    item.setPlan(p);
                    item.setQuestion(requirement.getText());
                    item.setClauseRef(clauseRef(section, clause, requirement));
                    item.setExpectedEvidence(requirement.getEvidenceTypes());
                    item.setWeight(weightOf(requirement.getObligation()));
                    item.setOrderIndex(order++);
                    generated.add(toChecklistResponse(checklistRepository.save(item)));
                }
            }
        }
        p.setStandardId(standardId);
        planRepository.save(p);
        return generated;
    }

    /**
     * Référence citable de l'exigence dans le rapport d'audit.
     *
     * <p>PAS une simple concaténation des trois codes : la plupart des
     * référentiels numérotent en cascade (section « 4 », clause « 4.1 », exigence
     * « 4.1.1 »), et concaténer produirait « 4.4.1.4.1.1 » — une référence que
     * personne ne peut rapprocher du texte qu'il a sous les yeux. On n'ajoute donc
     * un code que s'il ne porte pas déjà celui de son parent, ce qui rend aussi
     * bien les numérotations en cascade que les numérotations locales
     * (section « 1 », clause « 1.1 », exigence « 2 » → « 1.1.2 »).
     */
    private String clauseRef(StandardSection section, StandardClause clause,
                             StandardRequirement requirement) {
        return appendCode(appendCode(section.getCode(), clause.getCode()), requirement.getCode());
    }

    private String appendCode(String parent, String child) {
        if (child == null || child.isBlank()) return parent;
        if (parent == null || parent.isBlank()) return child;
        return child.startsWith(parent + ".") ? child : parent + "." + child;
    }

    /**
     * Une exigence obligatoire pèse plus qu'une recommandation dans le score de
     * conformité : MUST → 1, SHOULD → 2, MAY → 3, le poids servant de diviseur.
     */
    private int weightOf(ObligationLevel obligation) {
        if (obligation == ObligationLevel.SHOULD) return 2;
        if (obligation == ObligationLevel.MAY) return 3;
        return 1;
    }

    public AuditDto.ChecklistItemResponse addChecklistItem(UUID planId, AuditDto.ChecklistItemRequest req) {
        AuditPlan p = loadPlan(planId);
        if (p.getStatus() == AuditStatus.COMPLETED || p.getStatus() == AuditStatus.CANCELLED) {
            throw new AuditStateException("Cannot edit checklist of a " + p.getStatus() + " audit");
        }
        AuditChecklistItem item = new AuditChecklistItem();
        item.setPlan(p);
        item.setQuestion(req.question());
        item.setClauseRef(req.clauseRef());
        item.setExpectedEvidence(req.expectedEvidence());
        item.setWeight(req.weight() != null ? req.weight() : 1);
        item.setOrderIndex(req.orderIndex() != null ? req.orderIndex() : p.getChecklist().size());
        return toChecklistResponse(checklistRepository.save(item));
    }

    public AuditDto.ChecklistItemResponse updateChecklistItem(UUID planId, UUID itemId,
                                                              AuditDto.ChecklistItemRequest req) {
        AuditPlan p = loadPlan(planId);
        if (p.getStatus() == AuditStatus.COMPLETED || p.getStatus() == AuditStatus.CANCELLED) {
            throw new AuditStateException("Cannot edit checklist of a " + p.getStatus() + " audit");
        }
        AuditChecklistItem item = checklistRepository.findByIdAndPlanId(itemId, planId)
                .orElseThrow(() -> new AuditChecklistItemNotFoundException(itemId));
        if (req.question() != null) item.setQuestion(req.question());
        if (req.clauseRef() != null) item.setClauseRef(req.clauseRef());
        if (req.expectedEvidence() != null) item.setExpectedEvidence(req.expectedEvidence());
        if (req.weight() != null) item.setWeight(req.weight());
        if (req.orderIndex() != null) item.setOrderIndex(req.orderIndex());
        return toChecklistResponse(checklistRepository.save(item));
    }

    public AuditDto.ChecklistItemResponse respondToChecklistItem(UUID planId, UUID itemId,
                                                                 AuditDto.ChecklistResponseRequest req) {
        AuditPlan p = loadPlan(planId);
        if (p.getStatus() != AuditStatus.IN_PROGRESS) {
            throw new AuditStateException("Responses can only be recorded while audit is IN_PROGRESS");
        }
        AuditChecklistItem item = checklistRepository.findByIdAndPlanId(itemId, planId)
                .orElseThrow(() -> new AuditChecklistItemNotFoundException(itemId));
        if (req.response() != null) item.setResponse(req.response());
        if (req.conformant() != null) item.setConformant(req.conformant());
        return toChecklistResponse(checklistRepository.save(item));
    }

    public void deleteChecklistItem(UUID planId, UUID itemId) {
        AuditPlan p = loadPlan(planId);
        if (p.getStatus() != AuditStatus.PLANNED) {
            throw new AuditStateException("Checklist items can only be deleted while PLANNED");
        }
        AuditChecklistItem item = checklistRepository.findByIdAndPlanId(itemId, planId)
                .orElseThrow(() -> new AuditChecklistItemNotFoundException(itemId));
        checklistRepository.delete(item);
    }

    // --- findings ---

    public AuditDto.FindingResponse addFinding(UUID planId, AuditDto.FindingRequest req) {
        AuditPlan p = loadPlan(planId);
        if (p.getStatus() != AuditStatus.IN_PROGRESS) {
            throw new AuditStateException("Findings can only be raised while audit is IN_PROGRESS");
        }
        AuditFinding f = new AuditFinding();
        f.setPlan(p);
        f.setType(req.type());
        f.setDescription(req.description());
        f.setClauseRef(req.clauseRef());
        f.setPhotoUrl(req.photoUrl());
        f.setCapaId(req.capaId());
        f.setRaisedBy(req.raisedBy());
        f.setRaisedAt(Instant.now());
        if (req.checklistItemId() != null) {
            AuditChecklistItem item = checklistRepository
                    .findByIdAndPlanId(req.checklistItemId(), planId)
                    .orElseThrow(() -> new AuditChecklistItemNotFoundException(req.checklistItemId()));
            f.setChecklistItem(item);
        }
        return toFindingResponse(findingRepository.save(f));
    }

    public AuditDto.FindingResponse updateFinding(UUID planId, UUID findingId,
                                                  AuditDto.UpdateFindingRequest req) {
        AuditPlan p = loadPlan(planId);
        if (p.getStatus() == AuditStatus.CANCELLED) {
            throw new AuditStateException("Cannot modify findings on a cancelled audit");
        }
        AuditFinding f = findingRepository.findByIdAndPlanId(findingId, planId)
                .orElseThrow(() -> new AuditFindingNotFoundException(findingId));
        if (req.type() != null) f.setType(req.type());
        if (req.description() != null) f.setDescription(req.description());
        if (req.clauseRef() != null) f.setClauseRef(req.clauseRef());
        if (req.photoUrl() != null) f.setPhotoUrl(req.photoUrl());
        if (req.capaId() != null) f.setCapaId(req.capaId());
        return toFindingResponse(findingRepository.save(f));
    }

    public void deleteFinding(UUID planId, UUID findingId) {
        AuditPlan p = loadPlan(planId);
        if (p.getStatus() == AuditStatus.COMPLETED) {
            throw new AuditStateException("Cannot delete findings on a completed audit");
        }
        AuditFinding f = findingRepository.findByIdAndPlanId(findingId, planId)
                .orElseThrow(() -> new AuditFindingNotFoundException(findingId));
        findingRepository.delete(f);
    }

    // --- helpers ---

    private AuditPlan loadPlan(UUID id) {
        UUID tenantId = requireTenantId();
        return planRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new AuditPlanNotFoundException(id));
    }

    /**
     * {@code AUD-{année}-{séquence par tenant}}, avec garde anti-collision — même
     * fabrique que les non-conformités, et volontairement : deux registres qui
     * numérotent différemment obligeraient chacun à réapprendre à lire l'autre.
     *
     * <p>L'année vient de l'horloge INJECTÉE, comme le planning : une référence
     * frappée le 1er janvier à 00 h 30 doit porter la même année pour tout le
     * monde, et un test doit pouvoir la fixer.
     *
     * <p>La boucle n'est pas une garantie d'unicité — c'est la contrainte
     * {@code uq_audit_plans_reference} qui l'est. Elle évite seulement de
     * remonter une erreur de base pour un numéro déjà pris, cas ordinaire quand
     * deux audits sont créés dans la même seconde.
     */
    private String generateReference(UUID tenantId) {
        String prefix = "AUD-" + LocalDate.now(clock).getYear() + "-";
        long next = planRepository.countByTenantIdAndReferenceStartingWith(tenantId, prefix) + 1;
        String reference;
        do {
            reference = prefix + String.format("%04d", next);
            next++;
        } while (planRepository.existsByTenantIdAndReference(tenantId, reference));
        return reference;
    }

    /** Une adresse vide n'est pas une adresse : on la range en {@code null}, pas en "". */
    private static String normalizeEmail(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    /** Score pondéré 0..100 (conformes/total pondéré). */
    static Double computeConformityScore(AuditPlan p) {
        if (p.getChecklist().isEmpty()) return null;
        int totalWeight = 0;
        int conformantWeight = 0;
        for (AuditChecklistItem i : p.getChecklist()) {
            if (i.getConformant() == null) continue;
            int w = i.getWeight() != null ? i.getWeight() : 1;
            totalWeight += w;
            if (Boolean.TRUE.equals(i.getConformant())) {
                conformantWeight += w;
            }
        }
        if (totalWeight == 0) return null;
        return (conformantWeight * 100d) / totalWeight;
    }

    private AuditDto.PlanResponse toResponse(AuditPlan p) {
        return new AuditDto.PlanResponse(
                p.getId(), p.getTenantId(), p.getReference(), p.getTitle(), p.getScope(),
                p.getType(), p.getStatus(), p.getStandard(), p.getStandardId(),
                p.getLeadAuditorId(), p.getAuditeeId(), p.getScheduledDate(),
                p.getStartedAt(), p.getCompletedAt(), p.getReportSummary(),
                p.getReminderEmail(), p.getReminderSentAt(),
                p.getCreatedAt(), p.getUpdatedAt(),
                p.getChecklist().stream().map(this::toChecklistResponse).toList(),
                p.getFindings().stream().map(this::toFindingResponse).toList(),
                computeConformityScore(p));
    }

    private AuditDto.ChecklistItemResponse toChecklistResponse(AuditChecklistItem i) {
        return new AuditDto.ChecklistItemResponse(
                i.getId(), i.getPlan().getId(), i.getQuestion(), i.getClauseRef(),
                i.getExpectedEvidence(), i.getWeight(), i.getOrderIndex(),
                i.getResponse(), i.getConformant(), i.getCreatedAt(), i.getUpdatedAt());
    }

    private AuditDto.FindingResponse toFindingResponse(AuditFinding f) {
        return new AuditDto.FindingResponse(
                f.getId(), f.getPlan().getId(),
                f.getChecklistItem() != null ? f.getChecklistItem().getId() : null,
                f.getType(), f.getDescription(), f.getClauseRef(), f.getPhotoUrl(),
                f.getCapaId(), f.getRaisedBy(), f.getRaisedAt(),
                f.getCreatedAt(), f.getUpdatedAt());
    }
}
