package com.openlab.qualitos.quality.aiact.application;

import com.openlab.qualitos.quality.aiact.domain.AiRiskClassification;
import com.openlab.qualitos.quality.aiact.domain.AiSystem;
import com.openlab.qualitos.quality.aiact.domain.AiSystemNotFoundException;
import com.openlab.qualitos.quality.aiact.domain.AiSystemRepository;
import com.openlab.qualitos.quality.aiact.domain.AiSystemStateException;
import com.openlab.qualitos.quality.aiact.domain.AiSystemStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases — registre des systèmes d'IA AI Act.
 * Cross-tenant 404 (OWASP A01). Audit trail (OWASP A09).
 */
public class AiSystemService {

    private final AiSystemRepository repo;
    private final TenantProvider tenantProvider;
    private final AiSystemEventPublisher events;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public AiSystemService(AiSystemRepository repo,
                           TenantProvider tenantProvider, Clock clock) {
        this(repo, tenantProvider, new AiSystemEventPublisher.NoOp(), clock);
    }

    public AiSystemService(AiSystemRepository repo,
                           TenantProvider tenantProvider,
                           AiSystemEventPublisher events, Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.events = events;
        this.clock = clock;
    }

    public AiSystemDto.View draft(AiSystemDto.DraftRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (req.createdByUserId() == null) {
            throw new AiSystemStateException("createdByUserId required");
        }
        if (req.riskClassification() == null) {
            throw new AiSystemStateException("riskClassification required");
        }
        if (req.role() == null) {
            throw new AiSystemStateException("role required");
        }
        if (repo.existsByTenantAndReference(tenantId, req.reference())) {
            throw new AiSystemStateException("Reference already used: " + req.reference());
        }
        Instant now = Instant.now(clock);
        AiSystem s = AiSystem.draft(tenantId, req.reference(),
                req.name(), req.description(),
                req.providerName(), req.intendedPurpose(),
                req.riskClassification(), req.role(), req.generalPurpose(),
                req.conformityAssessmentEvidenceUrl(), req.ceMarkingNumber(),
                req.humanOversightDescription(), req.transparencyMeasures(),
                req.dataGovernanceNotes(),
                req.linkedDpiaId(),
                req.linkedProcessingActivityIds(),
                req.linkedAutomatedDecisionIds(),
                req.createdByUserId(), now);
        AiSystem saved = repo.save(s);
        events.publish(saved, AiSystemEventPublisher.Action.DRAFTED);
        return AiSystemDto.View.of(saved);
    }

    public AiSystemDto.View edit(UUID id, AiSystemDto.EditRequest req) {
        AiSystem s = loadForTenant(id);
        if (req.riskClassification() == null) {
            throw new AiSystemStateException("riskClassification required");
        }
        if (req.role() == null) {
            throw new AiSystemStateException("role required");
        }
        Instant now = Instant.now(clock);
        s.editDraft(req.name(), req.description(),
                req.providerName(), req.intendedPurpose(),
                req.riskClassification(), req.role(), req.generalPurpose(),
                req.conformityAssessmentEvidenceUrl(), req.ceMarkingNumber(),
                req.humanOversightDescription(), req.transparencyMeasures(),
                req.dataGovernanceNotes(),
                req.linkedDpiaId(),
                req.linkedProcessingActivityIds(),
                req.linkedAutomatedDecisionIds(), now);
        AiSystem saved = repo.save(s);
        events.publish(saved, AiSystemEventPublisher.Action.EDITED);
        return AiSystemDto.View.of(saved);
    }

    public AiSystemDto.View register(UUID id) {
        AiSystem s = loadForTenant(id);
        Instant now = Instant.now(clock);
        s.register(now);
        AiSystem saved = repo.save(s);
        events.publish(saved, AiSystemEventPublisher.Action.REGISTERED);
        return AiSystemDto.View.of(saved);
    }

    public AiSystemDto.View putInUse(UUID id) {
        AiSystem s = loadForTenant(id);
        Instant now = Instant.now(clock);
        s.putInUse(now);
        AiSystem saved = repo.save(s);
        events.publish(saved, AiSystemEventPublisher.Action.PUT_IN_USE);
        return AiSystemDto.View.of(saved);
    }

    public AiSystemDto.View decommission(UUID id) {
        AiSystem s = loadForTenant(id);
        Instant now = Instant.now(clock);
        s.decommission(now);
        AiSystem saved = repo.save(s);
        events.publish(saved, AiSystemEventPublisher.Action.DECOMMISSIONED);
        return AiSystemDto.View.of(saved);
    }

    public AiSystemDto.View withdraw(UUID id, AiSystemDto.WithdrawRequest req) {
        AiSystem s = loadForTenant(id);
        Instant now = Instant.now(clock);
        s.withdraw(req.reason(), now);
        AiSystem saved = repo.save(s);
        events.publish(saved, AiSystemEventPublisher.Action.WITHDRAWN);
        return AiSystemDto.View.of(saved);
    }

    public void delete(UUID id) {
        AiSystem s = loadForTenant(id);
        if (!s.isDraft()) {
            throw new AiSystemStateException(
                    "Only DRAFT systems can be deleted (other states preserved for audit)");
        }
        repo.delete(id);
        events.publish(s, AiSystemEventPublisher.Action.DELETED);
    }

    public AiSystemDto.View get(UUID id) { return AiSystemDto.View.of(loadForTenant(id)); }

    public List<AiSystemDto.View> list(AiSystemStatus status) {
        UUID tenantId = tenantProvider.requireTenantId();
        List<AiSystem> all = status == null
                ? repo.findByTenant(tenantId)
                : repo.findByTenantAndStatus(tenantId, status);
        return all.stream().map(AiSystemDto.View::of).toList();
    }

    public List<AiSystemDto.View> listByRiskClassification(AiRiskClassification risk) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (risk == null) throw new AiSystemStateException("riskClassification required");
        return repo.findByTenantAndRiskClassification(tenantId, risk).stream()
                .map(AiSystemDto.View::of).toList();
    }

    public AiSystemDto.View getByReference(String reference) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (reference == null || reference.isBlank()) {
            throw new AiSystemStateException("reference required");
        }
        return repo.findByTenantAndReference(tenantId, reference)
                .map(AiSystemDto.View::of)
                .orElseThrow(() -> new AiSystemNotFoundException(
                        UUID.fromString("00000000-0000-0000-0000-000000000000")));
    }

    private AiSystem loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        AiSystem s = repo.findById(id)
                .orElseThrow(() -> new AiSystemNotFoundException(id));
        if (!s.getTenantId().equals(tenantId)) {
            throw new AiSystemNotFoundException(id);
        }
        return s;
    }
}
