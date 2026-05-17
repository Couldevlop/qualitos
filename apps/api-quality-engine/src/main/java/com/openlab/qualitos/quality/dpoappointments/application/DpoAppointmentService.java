package com.openlab.qualitos.quality.dpoappointments.application;

import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointment;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointmentNotFoundException;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointmentRepository;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointmentStateException;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointmentStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Use cases — désignation DPO (RGPD Art. 37-39).
 *
 * Garantie clé : à l'activation, toute désignation ACTIVE existante pour la
 * même (tenant, scope) est automatiquement clôturée (ENDED) — invariant
 * "au plus une ACTIVE par scope" garanti applicativement et par DB (index).
 *
 * Cross-tenant lookups yieldent 404 (OWASP A01).
 * Audit trail via {@link DpoAppointmentEventPublisher} (OWASP A09).
 */
public class DpoAppointmentService {

    private static final String AUTO_END_REASON =
            "Auto-ended: superseded by new active appointment";

    private final DpoAppointmentRepository repo;
    private final TenantProvider tenantProvider;
    private final DpoAppointmentEventPublisher events;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public DpoAppointmentService(DpoAppointmentRepository repo,
                                 TenantProvider tenantProvider, Clock clock) {
        this(repo, tenantProvider, new DpoAppointmentEventPublisher.NoOp(), clock);
    }

    public DpoAppointmentService(DpoAppointmentRepository repo,
                                 TenantProvider tenantProvider,
                                 DpoAppointmentEventPublisher events, Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.events = events;
        this.clock = clock;
    }

    public DpoAppointmentDto.View propose(DpoAppointmentDto.ProposeRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (req.createdByUserId() == null) {
            throw new DpoAppointmentStateException("createdByUserId required");
        }
        if (req.dpoType() == null) {
            throw new DpoAppointmentStateException("dpoType required");
        }
        if (repo.existsByTenantAndReference(tenantId, req.reference())) {
            throw new DpoAppointmentStateException(
                    "Reference already used: " + req.reference());
        }
        Instant now = Instant.now(clock);
        DpoAppointment a = DpoAppointment.propose(tenantId, req.reference(),
                req.dpoFullName(), req.dpoEmail(), req.dpoPhone(),
                req.dpoType(), req.externalCompanyName(), req.qualifications(),
                req.scope(), req.linkedProcessingActivityIds(),
                req.createdByUserId(), now);
        DpoAppointment saved = repo.save(a);
        events.publish(saved, DpoAppointmentEventPublisher.Action.PROPOSED);
        return DpoAppointmentDto.View.of(saved);
    }

    public DpoAppointmentDto.View edit(UUID id, DpoAppointmentDto.EditRequest req) {
        DpoAppointment a = loadForTenant(id);
        if (req.dpoType() == null) {
            throw new DpoAppointmentStateException("dpoType required");
        }
        Instant now = Instant.now(clock);
        a.editProposed(req.dpoFullName(), req.dpoEmail(), req.dpoPhone(),
                req.dpoType(), req.externalCompanyName(), req.qualifications(),
                req.linkedProcessingActivityIds(), now);
        DpoAppointment saved = repo.save(a);
        events.publish(saved, DpoAppointmentEventPublisher.Action.EDITED);
        return DpoAppointmentDto.View.of(saved);
    }

    public DpoAppointmentDto.View activate(UUID id, DpoAppointmentDto.ActivateRequest req) {
        DpoAppointment a = loadForTenant(id);
        if (!a.isProposed()) {
            throw new DpoAppointmentStateException(
                    "Only PROPOSED appointments can be activated");
        }
        Instant now = Instant.now(clock);
        // Auto-end de la désignation ACTIVE existante pour le même scope.
        Optional<DpoAppointment> previous = repo.findActiveByScope(
                a.getTenantId(), a.getScope());
        if (previous.isPresent() && !previous.get().getId().equals(a.getId())) {
            DpoAppointment old = previous.get();
            old.end(AUTO_END_REASON, now, now);
            DpoAppointment ended = repo.save(old);
            events.publish(ended, DpoAppointmentEventPublisher.Action.ENDED);
        }
        a.activate(req.effectiveFrom(), req.regulatorNotifiedAt(),
                req.regulatorNotificationReference(), now);
        DpoAppointment saved = repo.save(a);
        events.publish(saved, DpoAppointmentEventPublisher.Action.ACTIVATED);
        return DpoAppointmentDto.View.of(saved);
    }

    public DpoAppointmentDto.View end(UUID id, DpoAppointmentDto.EndRequest req) {
        DpoAppointment a = loadForTenant(id);
        Instant now = Instant.now(clock);
        a.end(req.reason(), req.effectiveTo(), now);
        DpoAppointment saved = repo.save(a);
        events.publish(saved, DpoAppointmentEventPublisher.Action.ENDED);
        return DpoAppointmentDto.View.of(saved);
    }

    public DpoAppointmentDto.View cancel(UUID id, DpoAppointmentDto.CancelRequest req) {
        DpoAppointment a = loadForTenant(id);
        Instant now = Instant.now(clock);
        a.cancel(req.reason(), now);
        DpoAppointment saved = repo.save(a);
        events.publish(saved, DpoAppointmentEventPublisher.Action.CANCELLED);
        return DpoAppointmentDto.View.of(saved);
    }

    public void delete(UUID id) {
        DpoAppointment a = loadForTenant(id);
        if (!a.isProposed()) {
            throw new DpoAppointmentStateException(
                    "Only PROPOSED appointments can be deleted (active/ended preserved for audit)");
        }
        repo.delete(id);
        events.publish(a, DpoAppointmentEventPublisher.Action.DELETED);
    }

    public DpoAppointmentDto.View get(UUID id) {
        return DpoAppointmentDto.View.of(loadForTenant(id));
    }

    public List<DpoAppointmentDto.View> list(DpoAppointmentStatus status) {
        UUID tenantId = tenantProvider.requireTenantId();
        List<DpoAppointment> all = status == null
                ? repo.findByTenant(tenantId)
                : repo.findByTenantAndStatus(tenantId, status);
        return all.stream().map(DpoAppointmentDto.View::of).toList();
    }

    /** DPO actuellement en fonction pour ce scope (0 ou 1). */
    public Optional<DpoAppointmentDto.View> findActiveByScope(String scope) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (scope == null || scope.isBlank()) return Optional.empty();
        return repo.findActiveByScope(tenantId, scope).map(DpoAppointmentDto.View::of);
    }

    public DpoAppointmentDto.View getByReference(String reference) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (reference == null || reference.isBlank()) {
            throw new DpoAppointmentStateException("reference required");
        }
        return repo.findByTenantAndReference(tenantId, reference)
                .map(DpoAppointmentDto.View::of)
                .orElseThrow(() -> new DpoAppointmentNotFoundException(
                        UUID.fromString("00000000-0000-0000-0000-000000000000")));
    }

    private DpoAppointment loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        DpoAppointment a = repo.findById(id)
                .orElseThrow(() -> new DpoAppointmentNotFoundException(id));
        if (!a.getTenantId().equals(tenantId)) {
            throw new DpoAppointmentNotFoundException(id);
        }
        return a;
    }
}
