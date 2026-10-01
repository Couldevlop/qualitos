package com.openlab.qualitos.quality.riskregister.infrastructure;

import com.openlab.qualitos.quality.riskregister.domain.Identification;
import com.openlab.qualitos.quality.riskregister.domain.Opportunity;
import com.openlab.qualitos.quality.riskregister.domain.OpportunityAction;
import com.openlab.qualitos.quality.riskregister.domain.Rating;
import com.openlab.qualitos.quality.riskregister.domain.RegisterEvent;
import com.openlab.qualitos.quality.riskregister.domain.RegisterItemKind;
import com.openlab.qualitos.quality.riskregister.domain.RegisterNotFoundException;
import com.openlab.qualitos.quality.riskregister.domain.RegisterOrigin;
import com.openlab.qualitos.quality.riskregister.domain.RegisterRepositories;
import com.openlab.qualitos.quality.riskregister.domain.RegisterRequirement;
import com.openlab.qualitos.quality.riskregister.domain.Risk;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Les adaptateurs JPA des quatre ports du registre.
 *
 * <p>Une fiche déjà identifiée est relue dans SON tenant avant d'être écrite :
 * si elle a disparu, on rend 404 plutôt que de la recréer sous un identifiant
 * neuf — et un identifiant d'un autre client ne trouve rien.
 */
public final class RegisterRepositoryAdapters {

    private RegisterRepositoryAdapters() {}

    public static final class Risks implements RegisterRepositories.Risks {

        private final RiskJpaRepository jpa;

        public Risks(RiskJpaRepository jpa) {
            this.jpa = jpa;
        }

        @Override
        public Risk save(Risk r) {
            RiskJpaEntity e;
            if (r.getId() == null) {
                e = new RiskJpaEntity();
                e.setTenantId(r.getTenantId());
                e.setReference(r.getReference());
                e.setSourceId(r.getSourceId());
                e.setCreatedBy(r.getCreatedBy());
                e.setCreatedAt(r.getCreatedAt());
            } else {
                e = jpa.findByIdAndTenantId(r.getId(), r.getTenantId())
                        .orElseThrow(() -> new RegisterNotFoundException("Risk", r.getId()));
            }
            Identification i = r.getIdentification();
            e.setTitle(i.title());
            e.setType(i.type());
            e.setProcess(i.process());
            e.setSite(i.site());
            e.setOwner(i.owner());
            e.setOrigin(i.origin());
            e.setOriginRef(i.originRef());
            e.setRequirements(encode(i.requirements()));
            e.setCause(r.getCause());
            e.setEffect(r.getEffect());
            e.setGrossSeverity(r.getGross().first());
            e.setGrossProbability(r.getGross().second());
            e.setResidualSeverity(r.getResidual() == null ? null : r.getResidual().first());
            e.setResidualProbability(r.getResidual() == null ? null : r.getResidual().second());
            e.setDecision(r.getDecision());
            e.setStatus(r.getStatus());
            e.setNextReviewOn(r.getNextReviewOn());
            e.setEffectivenessCriterion(r.getEffectivenessCriterion());
            e.setUpdatedAt(r.getUpdatedAt());
            return toDomain(jpa.save(e));
        }

        @Override
        public Optional<Risk> findByIdAndTenant(UUID id, UUID tenantId) {
            return jpa.findByIdAndTenantId(id, tenantId).map(Risks::toDomain);
        }

        @Override
        public List<Risk> findByTenant(UUID tenantId) {
            return jpa.findByTenantId(tenantId).stream().map(Risks::toDomain).toList();
        }

        @Override
        public long countByTenant(UUID tenantId) {
            return jpa.countByTenantId(tenantId);
        }

        @Override
        public boolean referenceTaken(UUID tenantId, String reference) {
            return jpa.existsByTenantIdAndReference(tenantId, reference);
        }

        @Override
        public List<Risk> findBySource(UUID tenantId, RegisterOrigin origin, UUID sourceId) {
            return jpa.findByTenantIdAndOriginAndSourceId(tenantId, origin, sourceId).stream()
                    .map(Risks::toDomain).toList();
        }

        static Risk toDomain(RiskJpaEntity e) {
            Rating residuelle = e.getResidualSeverity() == null || e.getResidualProbability() == null
                    ? null : new Rating(e.getResidualSeverity(), e.getResidualProbability());
            return new Risk(e.getId(), e.getTenantId(), e.getReference(), e.getSourceId(),
                    new Identification(e.getTitle(), e.getType(), e.getProcess(), e.getSite(),
                            e.getOwner(), e.getOrigin(), e.getOriginRef(), decode(e.getRequirements())),
                    e.getCause(), e.getEffect(), new Rating(e.getGrossSeverity(), e.getGrossProbability()),
                    residuelle, e.getDecision(), e.getStatus(), e.getNextReviewOn(),
                    e.getEffectivenessCriterion(), e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
        }
    }

    public static final class Opportunities implements RegisterRepositories.Opportunities {

        private final OpportunityJpaRepository jpa;

        public Opportunities(OpportunityJpaRepository jpa) {
            this.jpa = jpa;
        }

        @Override
        public Opportunity save(Opportunity o) {
            OpportunityJpaEntity e;
            if (o.getId() == null) {
                e = new OpportunityJpaEntity();
                e.setTenantId(o.getTenantId());
                e.setReference(o.getReference());
                e.setCreatedBy(o.getCreatedBy());
                e.setCreatedAt(o.getCreatedAt());
            } else {
                e = jpa.findByIdAndTenantId(o.getId(), o.getTenantId())
                        .orElseThrow(() -> new RegisterNotFoundException("Opportunity", o.getId()));
            }
            Identification i = o.getIdentification();
            e.setTitle(i.title());
            e.setType(i.type());
            e.setProcess(i.process());
            e.setSite(i.site());
            e.setOwner(i.owner());
            e.setOrigin(i.origin());
            e.setOriginRef(i.originRef());
            e.setRequirements(encode(i.requirements()));
            e.setTargetDate(o.getTargetDate());
            e.setContext(o.getContext());
            e.setBenefit(o.getBenefit());
            e.setGain(o.getEvaluation().first());
            e.setFeasibility(o.getEvaluation().second());
            e.setDecision(o.getDecision());
            e.setStatus(o.getStatus());
            e.setBenefitCriterion(o.getBenefitCriterion());
            e.setUpdatedAt(o.getUpdatedAt());
            return toDomain(jpa.save(e));
        }

        @Override
        public Optional<Opportunity> findByIdAndTenant(UUID id, UUID tenantId) {
            return jpa.findByIdAndTenantId(id, tenantId).map(Opportunities::toDomain);
        }

        @Override
        public List<Opportunity> findByTenant(UUID tenantId) {
            return jpa.findByTenantId(tenantId).stream().map(Opportunities::toDomain).toList();
        }

        @Override
        public long countByTenant(UUID tenantId) {
            return jpa.countByTenantId(tenantId);
        }

        @Override
        public boolean referenceTaken(UUID tenantId, String reference) {
            return jpa.existsByTenantIdAndReference(tenantId, reference);
        }

        static Opportunity toDomain(OpportunityJpaEntity e) {
            return new Opportunity(e.getId(), e.getTenantId(), e.getReference(),
                    new Identification(e.getTitle(), e.getType(), e.getProcess(), e.getSite(),
                            e.getOwner(), e.getOrigin(), e.getOriginRef(), decode(e.getRequirements())),
                    e.getTargetDate(), e.getContext(), e.getBenefit(),
                    new Rating(e.getGain(), e.getFeasibility()), e.getDecision(), e.getStatus(),
                    e.getBenefitCriterion(), e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
        }
    }

    public static final class Actions implements RegisterRepositories.Actions {

        private final OpportunityActionJpaRepository jpa;

        public Actions(OpportunityActionJpaRepository jpa) {
            this.jpa = jpa;
        }

        @Override
        public OpportunityAction save(OpportunityAction a) {
            OpportunityActionJpaEntity e;
            if (a.getId() == null) {
                e = new OpportunityActionJpaEntity();
                e.setTenantId(a.getTenantId());
                e.setOpportunityId(a.getOpportunityId());
                e.setNumber(a.getNumber());
                e.setCreatedAt(a.getCreatedAt());
            } else {
                e = jpa.findByIdAndTenantId(a.getId(), a.getTenantId())
                        .orElseThrow(() -> new RegisterNotFoundException("Action", a.getId()));
            }
            e.setTitle(a.getTitle());
            e.setDueDate(a.getDueDate());
            e.setStatus(a.getStatus());
            e.setUpdatedAt(a.getUpdatedAt());
            return toDomain(jpa.save(e));
        }

        @Override
        public Optional<OpportunityAction> findByIdAndTenant(UUID id, UUID tenantId) {
            return jpa.findByIdAndTenantId(id, tenantId).map(Actions::toDomain);
        }

        @Override
        public List<OpportunityAction> findByOpportunity(UUID tenantId, UUID opportunityId) {
            return jpa.findByTenantIdAndOpportunityId(tenantId, opportunityId).stream()
                    .map(Actions::toDomain).toList();
        }

        @Override
        public int maxNumber(UUID tenantId) {
            return jpa.maxNumber(tenantId);
        }

        @Override
        public void delete(OpportunityAction action) {
            jpa.findByIdAndTenantId(action.getId(), action.getTenantId()).ifPresent(jpa::delete);
        }

        static OpportunityAction toDomain(OpportunityActionJpaEntity e) {
            return new OpportunityAction(e.getId(), e.getTenantId(), e.getOpportunityId(), e.getNumber(),
                    e.getTitle(), e.getDueDate(), e.getStatus(), e.getCreatedAt(), e.getUpdatedAt());
        }
    }

    public static final class Events implements RegisterRepositories.Events {

        private final RegisterEventJpaRepository jpa;

        public Events(RegisterEventJpaRepository jpa) {
            this.jpa = jpa;
        }

        /** Un événement ne se réécrit jamais : on n'insère que du neuf. */
        @Override
        public RegisterEvent save(RegisterEvent ev) {
            RegisterEventJpaEntity e = new RegisterEventJpaEntity();
            e.setTenantId(ev.tenantId());
            e.setItemKind(ev.itemKind());
            e.setItemId(ev.itemId());
            e.setEventType(ev.type());
            e.setFromValue(ev.fromValue());
            e.setToValue(ev.toValue());
            e.setDetail(ev.detail());
            e.setActorId(ev.actorId());
            e.setOccurredAt(ev.at());
            return toDomain(jpa.save(e));
        }

        @Override
        public List<RegisterEvent> findByItem(UUID tenantId, RegisterItemKind kind, UUID itemId) {
            return jpa.findByTenantIdAndItemKindAndItemIdOrderByOccurredAtDesc(tenantId, kind, itemId)
                    .stream().map(Events::toDomain).toList();
        }

        static RegisterEvent toDomain(RegisterEventJpaEntity e) {
            return new RegisterEvent(e.getId(), e.getTenantId(), e.getItemKind(), e.getItemId(),
                    e.getEventType(), e.getFromValue(), e.getToValue(), e.getDetail(), e.getActorId(),
                    e.getOccurredAt());
        }
    }

    // ---------- exigences ----------

    static String encode(Set<RegisterRequirement> exigences) {
        return exigences.stream().sorted().map(Enum::name).collect(Collectors.joining(","));
    }

    /**
     * Un code inconnu est ignoré, pas fatal : une exigence retirée du catalogue
     * ne doit pas rendre illisible la fiche qui la cochait.
     */
    static Set<RegisterRequirement> decode(String codes) {
        Set<RegisterRequirement> set = EnumSet.noneOf(RegisterRequirement.class);
        if (codes == null || codes.isBlank()) {
            return set;
        }
        Arrays.stream(codes.split(","))
                .map(String::strip)
                .forEach(code -> Arrays.stream(RegisterRequirement.values())
                        .filter(r -> r.name().equals(code))
                        .findFirst()
                        .ifPresent(set::add));
        return set;
    }
}
