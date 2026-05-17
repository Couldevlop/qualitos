package com.openlab.qualitos.quality.retention.application;

import com.openlab.qualitos.quality.retention.domain.RetentionRule;
import com.openlab.qualitos.quality.retention.domain.RetentionRuleNotFoundException;
import com.openlab.qualitos.quality.retention.domain.RetentionRuleRepository;
import com.openlab.qualitos.quality.retention.domain.RetentionRuleStateException;
import com.openlab.qualitos.quality.retention.domain.RetentionRuleStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Use cases pour les règles de rétention (RGPD Art. 5.1.e).
 *
 * Garantie clé : à l'activation, toute règle ACTIVE existante pour la même
 * (tenant, dataCategoryCode) est automatiquement archivée — invariant
 * "au plus une règle active par catégorie".
 *
 * Cross-tenant lookups yieldent 404 (OWASP A01 — no info leak).
 */
public class RetentionRuleService {

    private final RetentionRuleRepository repo;
    private final TenantProvider tenantProvider;
    private final RetentionRuleEventPublisher events;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public RetentionRuleService(RetentionRuleRepository repo,
                                TenantProvider tenantProvider, Clock clock) {
        this(repo, tenantProvider, new RetentionRuleEventPublisher.NoOp(), clock);
    }

    public RetentionRuleService(RetentionRuleRepository repo,
                                TenantProvider tenantProvider,
                                RetentionRuleEventPublisher events, Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.events = events;
        this.clock = clock;
    }

    public RetentionRuleDto.View create(RetentionRuleDto.CreateRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (req.createdByUserId() == null) {
            throw new RetentionRuleStateException("createdByUserId required");
        }
        Instant now = Instant.now(clock);
        RetentionRule r = RetentionRule.draft(tenantId,
                req.dataCategoryCode(), req.dataCategoryLabel(),
                req.retentionPeriod(), req.legalBasis(), req.lawfulBasisReference(),
                req.createdByUserId(), now);
        RetentionRule saved = repo.save(r);
        events.publish(saved, RetentionRuleEventPublisher.Action.CREATED);
        return RetentionRuleDto.View.of(saved);
    }

    public RetentionRuleDto.View edit(UUID id, RetentionRuleDto.EditRequest req) {
        RetentionRule r = loadForTenant(id);
        Instant now = Instant.now(clock);
        r.editDraft(req.dataCategoryLabel(), req.retentionPeriod(),
                req.legalBasis(), req.lawfulBasisReference(), now);
        RetentionRule saved = repo.save(r);
        events.publish(saved, RetentionRuleEventPublisher.Action.EDITED);
        return RetentionRuleDto.View.of(saved);
    }

    /** Activation — archive automatiquement la règle ACTIVE existante pour la
     *  même catégorie (invariant : 0 ou 1 ACTIVE par catégorie par tenant). */
    public RetentionRuleDto.View activate(UUID id) {
        RetentionRule r = loadForTenant(id);
        if (!r.isDraft()) {
            throw new RetentionRuleStateException("Only DRAFT rules can be activated");
        }
        Instant now = Instant.now(clock);
        Optional<RetentionRule> existing = repo.findActiveByCategory(
                r.getTenantId(), r.getDataCategoryCode());
        if (existing.isPresent() && !existing.get().getId().equals(r.getId())) {
            RetentionRule old = existing.get();
            old.archive(now);
            RetentionRule archived = repo.save(old);
            events.publish(archived, RetentionRuleEventPublisher.Action.ARCHIVED);
        }
        r.activate(now);
        RetentionRule saved = repo.save(r);
        events.publish(saved, RetentionRuleEventPublisher.Action.ACTIVATED);
        return RetentionRuleDto.View.of(saved);
    }

    public RetentionRuleDto.View archive(UUID id) {
        RetentionRule r = loadForTenant(id);
        Instant now = Instant.now(clock);
        r.archive(now);
        RetentionRule saved = repo.save(r);
        events.publish(saved, RetentionRuleEventPublisher.Action.ARCHIVED);
        return RetentionRuleDto.View.of(saved);
    }

    public void delete(UUID id) {
        RetentionRule r = loadForTenant(id);
        if (!r.isDraft()) {
            throw new RetentionRuleStateException(
                    "Only DRAFT rules can be deleted (active/archived rules preserved for audit)");
        }
        repo.delete(id);
        events.publish(r, RetentionRuleEventPublisher.Action.DELETED);
    }

    public RetentionRuleDto.View get(UUID id) {
        return RetentionRuleDto.View.of(loadForTenant(id));
    }

    public List<RetentionRuleDto.View> list(RetentionRuleStatus status) {
        UUID tenantId = tenantProvider.requireTenantId();
        List<RetentionRule> all = status == null
                ? repo.findByTenant(tenantId)
                : repo.findByTenantAndStatus(tenantId, status);
        return all.stream().map(RetentionRuleDto.View::of).toList();
    }

    /** Calcul de la date d'effacement pour un enregistrement (Art. 5.1.e).
     *  Renvoie {@code Optional.empty()} si aucune règle ACTIVE pour cette catégorie. */
    public Optional<RetentionRuleDto.ErasureEvaluation> evaluateErasure(
            String dataCategoryCode, Instant recordCreatedAt) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (dataCategoryCode == null || dataCategoryCode.isBlank()
                || recordCreatedAt == null) {
            return Optional.empty();
        }
        Instant now = Instant.now(clock);
        return repo.findActiveByCategory(tenantId, dataCategoryCode)
                .map(rule -> {
                    Instant erasureAt = rule.computeErasureAt(recordCreatedAt);
                    return new RetentionRuleDto.ErasureEvaluation(
                            dataCategoryCode, recordCreatedAt, erasureAt,
                            !now.isBefore(erasureAt), rule.getId(),
                            rule.getRetentionPeriod());
                });
    }

    private RetentionRule loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        RetentionRule r = repo.findById(id)
                .orElseThrow(() -> new RetentionRuleNotFoundException(id));
        if (!r.getTenantId().equals(tenantId)) {
            throw new RetentionRuleNotFoundException(id);
        }
        return r;
    }
}
