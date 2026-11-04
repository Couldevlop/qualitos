package com.openlab.qualitos.quality.stakeholders.infrastructure;

import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.stakeholders.domain.Person;
import com.openlab.qualitos.quality.stakeholders.domain.Stakeholder;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderCategory;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderRepository;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderRequirement;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Traduit l'agrégat du domaine en lignes, et l'inverse.
 *
 * <p>La traduction est ici et nulle part ailleurs : le domaine ne connaît pas
 * JPA, et le service ne connaît pas les entités.
 */
public class StakeholderRepositoryAdapter implements StakeholderRepository {

    private final StakeholderJpaRepository repository;

    public StakeholderRepositoryAdapter(StakeholderJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Stakeholder save(Stakeholder stakeholder) {
        // findByIdAndTenantId, et non findById : la convention de l'ADR 0001 vaut
        // aussi quand l'agrégat vient déjà d'un chargement filtré. Une barrière
        // qui ne coûte rien se garde.
        StakeholderJpaEntity e = repository
                .findByIdAndTenantId(stakeholder.id(), stakeholder.tenantId())
                .orElseGet(() -> {
                    StakeholderJpaEntity neuf = new StakeholderJpaEntity();
                    neuf.setId(stakeholder.id());
                    neuf.setTenantId(stakeholder.tenantId());
                    neuf.setCreatedAt(Instant.now());
                    neuf.setCreatedBy(CurrentUser.userId().orElse(null));
                    return neuf;
                });
        e.setName(stakeholder.name());
        e.setCategory(stakeholder.category());
        e.setSide(stakeholder.side());
        e.setDescription(stakeholder.description());
        e.setContact(stakeholder.contact());
        e.setSites(stakeholder.sites());
        e.setNeeds(stakeholder.needs());
        e.setRequirementsSummary(stakeholder.requirementsSummary());
        e.setRelevance(stakeholder.relevance());
        e.setRelevanceJustification(stakeholder.relevanceJustification());
        e.setRisksAndOpportunities(stakeholder.risksAndOpportunities());
        e.setQualityImpact(stakeholder.qualityImpact());
        e.setRegulatoryImpact(stakeholder.regulatoryImpact());
        e.setOwnerAccountId(stakeholder.owner() == null ? null : stakeholder.owner().accountId());
        e.setOwnerName(stakeholder.owner() == null ? null : stakeholder.owner().name());
        e.setControls(stakeholder.controls());
        e.setIndicators(stakeholder.indicators());
        e.setDocuments(stakeholder.documents());
        e.setActions(stakeholder.actions());
        e.setNextReviewOn(stakeholder.nextReviewOn());
        e.setLastReviewedOn(stakeholder.lastReviewedOn());
        e.setUpdatedAt(Instant.now());
        e.setUpdatedBy(CurrentUser.userId().orElse(null));
        syncRequirements(e, stakeholder.requirements());
        return toDomain(repository.save(e));
    }

    /**
     * Aligne les lignes filles sur l'agrégat : les disparues sont retirées (la
     * cascade les supprime), les nouvelles ajoutées, les autres mises à jour en
     * place.
     *
     * <p>On ne vide pas la collection pour la reconstruire : Hibernate
     * supprimerait puis réinsérerait chaque ligne à chaque enregistrement, et
     * les identifiants des exigences — qui portent les liens vers les autres
     * modules — changeraient sous les pieds de l'utilisateur.
     */
    private void syncRequirements(StakeholderJpaEntity e, List<StakeholderRequirement> wanted) {
        Map<UUID, StakeholderRequirementJpaEntity> existing = new HashMap<>();
        for (StakeholderRequirementJpaEntity r : e.getRequirements()) {
            existing.put(r.getId(), r);
        }
        List<StakeholderRequirementJpaEntity> next = new ArrayList<>(wanted.size());
        for (int i = 0; i < wanted.size(); i++) {
            StakeholderRequirement r = wanted.get(i);
            StakeholderRequirementJpaEntity row = existing.get(r.id());
            if (row == null) {
                row = new StakeholderRequirementJpaEntity();
                row.setId(r.id());
                row.setStakeholder(e);
            }
            // Le tenant vient de la fiche parente, jamais d'une requête : c'est
            // lui qui place la ligne sous la politique de cloisonnement.
            row.setTenantId(e.getTenantId());
            row.setPosition(i);
            row.setNeed(r.need());
            row.setRequirement(r.requirement());
            row.setOrigin(r.origin());
            row.setNature(r.nature());
            row.setRisk(r.risk());
            row.setRiskLevel(r.riskLevel());
            row.setControl(r.control());
            row.setIndicator(r.indicator());
            row.setResponsible(r.responsible());
            row.setEvidence(r.evidence());
            row.setStatus(r.status());
            row.setRiskId(r.riskId());
            row.setNcId(r.ncId());
            row.setAuditId(r.auditId());
            row.setDocumentId(r.documentId());
            row.setCapaId(r.capaId());
            next.add(row);
        }
        e.getRequirements().clear();
        e.getRequirements().addAll(next);
    }

    @Override
    public Optional<Stakeholder> findByIdAndTenant(UUID id, UUID tenantId) {
        return repository.findByIdAndTenantId(id, tenantId).map(StakeholderRepositoryAdapter::toDomain);
    }

    @Override
    public List<Stakeholder> findByTenant(UUID tenantId) {
        return repository.findByTenantId(tenantId).stream()
                .map(StakeholderRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public boolean nameTaken(UUID tenantId, StakeholderCategory category, String name, UUID exceptId) {
        return repository.findByTenantIdAndCategoryAndNameIgnoreCase(tenantId, category, name).stream()
                .anyMatch(e -> exceptId == null || !e.getId().equals(exceptId));
    }

    @Override
    public void delete(Stakeholder stakeholder) {
        repository.findByIdAndTenantId(stakeholder.id(), stakeholder.tenantId())
                .ifPresent(repository::delete);
    }

    static Stakeholder toDomain(StakeholderJpaEntity e) {
        Stakeholder s = new Stakeholder(e.getId(), e.getTenantId(), e.getName(), e.getCategory(),
                e.getSide());
        s.describe(e.getDescription(), e.getContact(), e.getSites());
        // Les règles de saisie ont déjà été appliquées à l'écriture ; on repose
        // ici l'état tel qu'il a été enregistré, sans le rejouer — une règle
        // durcie après coup ne doit pas rendre illisible une fiche existante.
        s.restoreAnalysis(e.getNeeds(), e.getRequirementsSummary(), e.getRelevance(),
                e.getRelevanceJustification(), e.getRisksAndOpportunities());
        s.rate(e.getQualityImpact(), e.getRegulatoryImpact());
        if (e.getOwnerAccountId() != null && e.getOwnerName() != null) {
            s.entrustTo(new Person(e.getOwnerAccountId(), e.getOwnerName()));
        }
        s.steer(e.getControls(), e.getIndicators(), e.getDocuments(), e.getActions(),
                e.getNextReviewOn());
        s.restoreLastReview(e.getLastReviewedOn());
        for (StakeholderRequirementJpaEntity r : e.getRequirements()) {
            s.restoreRequirement(new StakeholderRequirement(r.getId(), r.getNeed(), r.getRequirement(),
                    r.getOrigin(), r.getNature(), r.getRisk(), r.getRiskLevel(), r.getControl(),
                    r.getIndicator(), r.getResponsible(), r.getEvidence(), r.getStatus(),
                    r.getRiskId(), r.getNcId(), r.getAuditId(), r.getDocumentId(), r.getCapaId()));
        }
        return s;
    }
}
