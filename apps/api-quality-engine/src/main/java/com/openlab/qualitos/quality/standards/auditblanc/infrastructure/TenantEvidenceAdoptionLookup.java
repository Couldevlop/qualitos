package com.openlab.qualitos.quality.standards.auditblanc.infrastructure;

import com.openlab.qualitos.quality.standards.ObligationLevel;
import com.openlab.qualitos.quality.standards.RequirementEvidence;
import com.openlab.qualitos.quality.standards.RequirementEvidenceRepository;
import com.openlab.qualitos.quality.standards.RiskLevel;
import com.openlab.qualitos.quality.standards.Standard;
import com.openlab.qualitos.quality.standards.StandardClause;
import com.openlab.qualitos.quality.standards.StandardRequirement;
import com.openlab.qualitos.quality.standards.StandardSection;
import com.openlab.qualitos.quality.standards.TenantStandard;
import com.openlab.qualitos.quality.standards.TenantStandardRepository;
import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditAdoptionLookup;
import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditTenantProvider;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditClause;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Adapter du port {@link MockAuditAdoptionLookup} : calcule la matière de
 * l'audit blanc à partir des <b>preuves réelles</b> liées par le tenant dans le
 * Standards Hub (table {@code requirement_evidences}) — aucune donnée inventée
 * (Standards Hub §8.4 onglet 7, moteur d'alignement §8.7).
 *
 * <p>Sécurité multi-tenant : l'adoption est chargée par
 * {@code findByIdAndTenantId} (tenant issu du JWT) ; une adoption d'un autre
 * tenant renvoie {@link Optional#empty()} → 404 (OWASP A01, §18.2 #2). Les
 * preuves confrontées sont strictement celles de l'adoption du tenant courant.
 */
@Component
public class TenantEvidenceAdoptionLookup implements MockAuditAdoptionLookup {

    private final TenantStandardRepository adoptions;
    private final RequirementEvidenceRepository evidences;
    private final MockAuditTenantProvider tenantProvider;

    public TenantEvidenceAdoptionLookup(
            TenantStandardRepository adoptions,
            RequirementEvidenceRepository evidences,
            @Qualifier("mockAuditTenantContextProvider") MockAuditTenantProvider tenantProvider) {
        this.adoptions = adoptions;
        this.evidences = evidences;
        this.tenantProvider = tenantProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdoptionMatter> findMatter(UUID adoptionId) {
        UUID tenantId = tenantProvider.requireTenantId();
        Optional<TenantStandard> found = adoptions.findByIdAndTenantId(adoptionId, tenantId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        TenantStandard ts = found.get();
        Standard std = ts.getStandard();

        // Exigences démontrées par au moins une preuve (strictement ce tenant/adoption).
        Set<UUID> covered = new HashSet<>();
        for (RequirementEvidence ev : evidences.findByTenantStandardId(adoptionId)) {
            covered.add(ev.getRequirement().getId());
        }

        List<MockAuditClause> clauses = new ArrayList<>();
        for (StandardSection section : std.getSections()) {
            for (StandardClause clause : section.getClauses()) {
                MockAuditClause mac = toClause(clause, covered);
                if (mac != null) {
                    clauses.add(mac);
                }
            }
        }

        String industry = std.getApplicableIndustries();
        return Optional.of(new AdoptionMatter(
                ts.getId(), std.getId(), std.getCode(), std.getFullName(),
                (industry == null || industry.isBlank()) ? "all" : industry,
                clauses));
    }

    /**
     * Projette une clause normative sur une clause d'audit : agrège le caractère
     * (MUST si au moins une exigence l'est) et le risque (le plus élevé parmi les
     * exigences obligatoires, sinon parmi toutes), et compte les preuves.
     */
    private static MockAuditClause toClause(StandardClause clause, Set<UUID> covered) {
        List<StandardRequirement> requirements = clause.getRequirements();
        if (requirements.isEmpty()) {
            return null; // pas d'exigence → rien à auditer
        }
        int total = requirements.size();
        int coveredCount = 0;
        boolean anyMust = false;
        RiskLevel mustRisk = null;
        RiskLevel anyRisk = null;
        for (StandardRequirement req : requirements) {
            if (covered.contains(req.getId())) {
                coveredCount++;
            }
            anyRisk = maxRisk(anyRisk, req.getRiskIfMissing());
            if (req.getObligation() == ObligationLevel.MUST) {
                anyMust = true;
                mustRisk = maxRisk(mustRisk, req.getRiskIfMissing());
            }
        }
        ObligationLevel obligation = anyMust ? ObligationLevel.MUST : weakest(requirements);
        RiskLevel risk = anyMust ? orMedium(mustRisk) : orMedium(anyRisk);
        return new MockAuditClause(
                clause.getCode(), clause.getTitle(), obligation, risk,
                total, coveredCount, List.of());
    }

    /** Caractère le plus exigeant hors MUST : SHOULD prime sur MAY. */
    private static ObligationLevel weakest(List<StandardRequirement> requirements) {
        boolean anyShould = false;
        for (StandardRequirement req : requirements) {
            if (req.getObligation() == ObligationLevel.SHOULD) {
                anyShould = true;
                break;
            }
        }
        return anyShould ? ObligationLevel.SHOULD : ObligationLevel.MAY;
    }

    private static RiskLevel maxRisk(RiskLevel current, RiskLevel candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null) {
            return candidate;
        }
        return candidate.ordinal() > current.ordinal() ? candidate : current;
    }

    private static RiskLevel orMedium(RiskLevel risk) {
        return risk == null ? RiskLevel.MEDIUM : risk;
    }
}
