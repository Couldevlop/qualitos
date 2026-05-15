package com.openlab.qualitos.quality.complaints;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestration des réclamations + thread de réponses (§4.9).
 *
 * Transitions (ComplaintStateException → 409 sinon) :
 *   RECEIVED            → UNDER_INVESTIGATION | REJECTED
 *   UNDER_INVESTIGATION → RESPONDED | RESOLVED | REJECTED
 *   RESPONDED           → RESOLVED | UNDER_INVESTIGATION (besoin d'analyse complémentaire)
 *   RESOLVED            → CLOSED | REOPENED
 *   CLOSED              → REOPENED
 *   REOPENED            → UNDER_INVESTIGATION (auto)
 *   REJECTED            → ∅ (terminal)
 *
 * Effets de bord :
 *   - addResponse(non-interne) ⇒ premier passage RESPONDED si pas encore.
 *   - resolve() exige que firstResponseAt soit renseigné (pas de "résolution silencieuse").
 *   - reopen() crée la transition vers REOPENED puis bascule auto en UNDER_INVESTIGATION.
 */
@Service
public class ComplaintService {

    private static final Map<ComplaintStatus, Set<ComplaintStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(ComplaintStatus.class);
        ALLOWED.put(ComplaintStatus.RECEIVED, EnumSet.of(
                ComplaintStatus.UNDER_INVESTIGATION, ComplaintStatus.REJECTED));
        ALLOWED.put(ComplaintStatus.UNDER_INVESTIGATION, EnumSet.of(
                ComplaintStatus.RESPONDED, ComplaintStatus.RESOLVED, ComplaintStatus.REJECTED));
        ALLOWED.put(ComplaintStatus.RESPONDED, EnumSet.of(
                ComplaintStatus.RESOLVED, ComplaintStatus.UNDER_INVESTIGATION));
        ALLOWED.put(ComplaintStatus.RESOLVED, EnumSet.of(
                ComplaintStatus.CLOSED, ComplaintStatus.REOPENED));
        ALLOWED.put(ComplaintStatus.CLOSED, EnumSet.of(ComplaintStatus.REOPENED));
        ALLOWED.put(ComplaintStatus.REOPENED, EnumSet.of(ComplaintStatus.UNDER_INVESTIGATION));
        ALLOWED.put(ComplaintStatus.REJECTED, EnumSet.noneOf(ComplaintStatus.class));
    }

    private final ComplaintRepository complaintRepo;
    private final ComplaintResponseRepository responseRepo;
    private final Clock clock;

    public ComplaintService(ComplaintRepository complaintRepo,
                            ComplaintResponseRepository responseRepo) {
        this(complaintRepo, responseRepo, Clock.systemUTC());
    }

    ComplaintService(ComplaintRepository complaintRepo,
                     ComplaintResponseRepository responseRepo,
                     Clock clock) {
        this.complaintRepo = complaintRepo;
        this.responseRepo = responseRepo;
        this.clock = clock;
    }

    // ---------- CRUD ----------

    @Transactional
    public ComplaintDto.ComplaintResponse create(ComplaintDto.CreateComplaintRequest req) {
        UUID tenantId = requireTenantId();
        complaintRepo.findByTenantIdAndCode(tenantId, req.code()).ifPresent(c -> {
            throw new ComplaintStateException("Complaint code already exists: " + req.code());
        });
        Complaint c = new Complaint();
        c.setTenantId(tenantId);
        c.setCode(req.code());
        c.setChannel(req.channel());
        c.setCustomerName(req.customerName());
        c.setCustomerEmail(req.customerEmail());
        c.setCustomerExternalId(req.customerExternalId());
        c.setSubject(req.subject());
        c.setDescription(req.description());
        c.setSeverity(req.severity() == null ? ComplaintSeverity.MEDIUM : req.severity());
        c.setCategory(req.category() == null ? ComplaintCategory.OTHER : req.category());
        c.setStatus(ComplaintStatus.RECEIVED);
        c.setSupplierId(req.supplierId());
        c.setAssignedToUserId(req.assignedToUserId());
        c.setCreatedBy(req.createdBy());
        c.setReceivedAt(req.receivedAt() != null ? req.receivedAt() : Instant.now(clock));
        return toResponse(complaintRepo.save(c));
    }

    @Transactional(readOnly = true)
    public Page<ComplaintDto.ComplaintResponse> list(ComplaintStatus status,
                                                     ComplaintCategory category,
                                                     UUID supplierId,
                                                     Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<Complaint> page;
        if (status != null) page = complaintRepo.findByTenantIdAndStatus(tenantId, status, pageable);
        else if (category != null) page = complaintRepo.findByTenantIdAndCategory(tenantId, category, pageable);
        else if (supplierId != null) page = complaintRepo.findByTenantIdAndSupplierId(tenantId, supplierId, pageable);
        else page = complaintRepo.findByTenantId(tenantId, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ComplaintDto.ComplaintResponse get(UUID id) { return toResponse(loadForTenant(id)); }

    @Transactional
    public ComplaintDto.ComplaintResponse update(UUID id, ComplaintDto.UpdateComplaintRequest req) {
        Complaint c = loadForTenant(id);
        if (isTerminal(c.getStatus())) {
            throw new ComplaintStateException(
                    "Cannot edit a complaint in terminal status " + c.getStatus());
        }
        if (req.customerName() != null) c.setCustomerName(req.customerName());
        if (req.customerEmail() != null) c.setCustomerEmail(req.customerEmail());
        if (req.customerExternalId() != null) c.setCustomerExternalId(req.customerExternalId());
        if (req.subject() != null) c.setSubject(req.subject());
        if (req.description() != null) c.setDescription(req.description());
        if (req.severity() != null) c.setSeverity(req.severity());
        if (req.category() != null) c.setCategory(req.category());
        if (req.supplierId() != null) c.setSupplierId(req.supplierId());
        if (req.assignedToUserId() != null) c.setAssignedToUserId(req.assignedToUserId());
        return toResponse(complaintRepo.save(c));
    }

    @Transactional
    public void delete(UUID id) {
        Complaint c = loadForTenant(id);
        if (c.getStatus() != ComplaintStatus.RECEIVED
                && c.getStatus() != ComplaintStatus.REJECTED) {
            throw new ComplaintStateException(
                    "Only RECEIVED or REJECTED complaints can be deleted (current: "
                            + c.getStatus() + ")");
        }
        responseRepo.deleteByComplaintId(id);
        complaintRepo.delete(c);
    }

    // ---------- Workflow ----------

    @Transactional
    public ComplaintDto.ComplaintResponse assign(UUID id, ComplaintDto.AssignRequest req) {
        Complaint c = loadForTenant(id);
        if (isTerminal(c.getStatus())) {
            throw new ComplaintStateException(
                    "Cannot assign a complaint in terminal status " + c.getStatus());
        }
        c.setAssignedToUserId(req.assigneeUserId());
        // Le premier assignement déclenche l'investigation si on est encore en RECEIVED.
        if (c.getStatus() == ComplaintStatus.RECEIVED) {
            c.setStatus(ComplaintStatus.UNDER_INVESTIGATION);
        }
        return toResponse(complaintRepo.save(c));
    }

    @Transactional
    public ComplaintDto.ComplaintResponse reject(UUID id, ComplaintDto.RejectRequest req) {
        Complaint c = loadForTenant(id);
        if (!ALLOWED.getOrDefault(c.getStatus(), Set.of()).contains(ComplaintStatus.REJECTED)) {
            throw new ComplaintStateException(
                    "Cannot reject from status " + c.getStatus());
        }
        c.setStatus(ComplaintStatus.REJECTED);
        c.setRejectionReason(req.reason());
        c.setClosedAt(Instant.now(clock));
        return toResponse(complaintRepo.save(c));
    }

    @Transactional
    public ComplaintDto.ComplaintResponse resolve(UUID id, ComplaintDto.ResolveRequest req) {
        Complaint c = loadForTenant(id);
        if (!ALLOWED.getOrDefault(c.getStatus(), Set.of()).contains(ComplaintStatus.RESOLVED)) {
            throw new ComplaintStateException(
                    "Cannot resolve from status " + c.getStatus());
        }
        if (c.getFirstResponseAt() == null) {
            throw new ComplaintStateException(
                    "A response must be sent to the customer before resolving");
        }
        c.setStatus(ComplaintStatus.RESOLVED);
        c.setResolvedAt(Instant.now(clock));
        if (req != null && req.capaCaseId() != null) c.setCapaCaseId(req.capaCaseId());
        return toResponse(complaintRepo.save(c));
    }

    @Transactional
    public ComplaintDto.ComplaintResponse close(UUID id) {
        Complaint c = loadForTenant(id);
        if (!ALLOWED.getOrDefault(c.getStatus(), Set.of()).contains(ComplaintStatus.CLOSED)) {
            throw new ComplaintStateException(
                    "Cannot close from status " + c.getStatus());
        }
        c.setStatus(ComplaintStatus.CLOSED);
        c.setClosedAt(Instant.now(clock));
        return toResponse(complaintRepo.save(c));
    }

    @Transactional
    public ComplaintDto.ComplaintResponse reopen(UUID id) {
        Complaint c = loadForTenant(id);
        if (c.getStatus() != ComplaintStatus.RESOLVED && c.getStatus() != ComplaintStatus.CLOSED) {
            throw new ComplaintStateException(
                    "Only RESOLVED or CLOSED complaints can be reopened (current: "
                            + c.getStatus() + ")");
        }
        c.setStatus(ComplaintStatus.UNDER_INVESTIGATION);
        c.setClosedAt(null);
        c.setResolvedAt(null);
        return toResponse(complaintRepo.save(c));
    }

    @Transactional
    public ComplaintDto.ComplaintResponse setSatisfaction(UUID id, ComplaintDto.SatisfactionRequest req) {
        Complaint c = loadForTenant(id);
        if (c.getStatus() != ComplaintStatus.RESOLVED && c.getStatus() != ComplaintStatus.CLOSED) {
            throw new ComplaintStateException(
                    "Satisfaction score can only be recorded on a RESOLVED or CLOSED complaint");
        }
        c.setSatisfactionScore(req.score());
        return toResponse(complaintRepo.save(c));
    }

    // ---------- Responses ----------

    @Transactional
    public ComplaintDto.ResponseEntryResponse addResponse(
            UUID complaintId, ComplaintDto.AddResponseRequest req) {
        Complaint c = loadForTenant(complaintId);
        if (isTerminal(c.getStatus())) {
            throw new ComplaintStateException(
                    "Cannot add a response to a complaint in terminal status " + c.getStatus());
        }
        ComplaintResponse r = new ComplaintResponse();
        r.setTenantId(c.getTenantId());
        r.setComplaintId(complaintId);
        r.setAuthorUserId(req.authorUserId());
        r.setChannel(req.channel() == null ? c.getChannel() : req.channel());
        r.setBody(req.body());
        r.setInternalNote(req.internalNote());
        r.setSentAt(Instant.now(clock));
        ComplaintResponse saved = responseRepo.save(r);
        // Premier message externe → on stampe firstResponseAt et on passe à RESPONDED
        // si on était en UNDER_INVESTIGATION ou RECEIVED.
        if (!req.internalNote()) {
            if (c.getFirstResponseAt() == null) c.setFirstResponseAt(saved.getSentAt());
            if (c.getStatus() == ComplaintStatus.RECEIVED
                    || c.getStatus() == ComplaintStatus.UNDER_INVESTIGATION) {
                c.setStatus(ComplaintStatus.RESPONDED);
            }
            complaintRepo.save(c);
        }
        return toResponse(saved);
    }

    @Transactional
    public void deleteResponse(UUID complaintId, UUID responseId) {
        Complaint c = loadForTenant(complaintId);
        ComplaintResponse r = responseRepo.findById(responseId)
                .orElseThrow(() -> new ComplaintResponseNotFoundException(responseId));
        if (!r.getComplaintId().equals(complaintId) || !r.getTenantId().equals(c.getTenantId())) {
            throw new ComplaintResponseNotFoundException(responseId);
        }
        if (!r.isInternalNote()) {
            throw new ComplaintStateException(
                    "Only internal notes can be deleted; customer-facing responses are immutable");
        }
        responseRepo.delete(r);
    }

    @Transactional(readOnly = true)
    public Page<ComplaintDto.ResponseEntryResponse> listResponses(UUID complaintId, Pageable pageable) {
        loadForTenant(complaintId);
        return responseRepo.findByComplaintIdOrderBySentAtAsc(complaintId, pageable)
                .map(this::toResponse);
    }

    // ---------- Statistics ----------

    @Transactional(readOnly = true)
    public ComplaintDto.ComplaintStatistics statistics() {
        UUID tenantId = requireTenantId();
        return new ComplaintDto.ComplaintStatistics(
                tenantId,
                complaintRepo.findByTenantId(tenantId, Pageable.unpaged()).getTotalElements(),
                complaintRepo.countByTenantIdAndStatus(tenantId, ComplaintStatus.RECEIVED),
                complaintRepo.countByTenantIdAndStatus(tenantId, ComplaintStatus.UNDER_INVESTIGATION),
                complaintRepo.countByTenantIdAndStatus(tenantId, ComplaintStatus.RESPONDED),
                complaintRepo.countByTenantIdAndStatus(tenantId, ComplaintStatus.RESOLVED),
                complaintRepo.countByTenantIdAndStatus(tenantId, ComplaintStatus.CLOSED),
                complaintRepo.countByTenantIdAndStatus(tenantId, ComplaintStatus.REJECTED),
                complaintRepo.countByTenantIdAndCategory(tenantId, ComplaintCategory.PRODUCT),
                complaintRepo.countByTenantIdAndCategory(tenantId, ComplaintCategory.SERVICE),
                complaintRepo.countByTenantIdAndCategory(tenantId, ComplaintCategory.DELIVERY),
                complaintRepo.countByTenantIdAndCategory(tenantId, ComplaintCategory.BILLING),
                complaintRepo.countByTenantIdAndCategory(tenantId, ComplaintCategory.QUALITY),
                complaintRepo.countByTenantIdAndCategory(tenantId, ComplaintCategory.SAFETY),
                complaintRepo.countByTenantIdAndCategory(tenantId, ComplaintCategory.OTHER));
    }

    // ---------- helpers ----------

    Complaint loadForTenant(UUID id) {
        UUID tenantId = requireTenantId();
        Complaint c = complaintRepo.findById(id)
                .orElseThrow(() -> new ComplaintNotFoundException(id));
        if (!c.getTenantId().equals(tenantId)) throw new ComplaintNotFoundException(id);
        return c;
    }

    private static boolean isTerminal(ComplaintStatus s) {
        return s == ComplaintStatus.CLOSED || s == ComplaintStatus.REJECTED;
    }

    private ComplaintDto.ComplaintResponse toResponse(Complaint c) {
        return new ComplaintDto.ComplaintResponse(
                c.getId(), c.getTenantId(), c.getCode(), c.getChannel(),
                c.getCustomerName(), c.getCustomerEmail(), c.getCustomerExternalId(),
                c.getSubject(), c.getDescription(),
                c.getSeverity(), c.getCategory(), c.getStatus(),
                c.getSupplierId(), c.getCapaCaseId(), c.getAssignedToUserId(),
                c.getSatisfactionScore(),
                c.getReceivedAt(), c.getFirstResponseAt(),
                c.getResolvedAt(), c.getClosedAt(),
                c.getRejectionReason(),
                c.getCreatedBy(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private ComplaintDto.ResponseEntryResponse toResponse(ComplaintResponse r) {
        return new ComplaintDto.ResponseEntryResponse(
                r.getId(), r.getTenantId(), r.getComplaintId(),
                r.getAuthorUserId(), r.getChannel(),
                r.getBody(), r.isInternalNote(),
                r.getSentAt(), r.getCreatedAt());
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
