package com.openlab.qualitos.quality.standards.auditblanc;

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
import com.openlab.qualitos.quality.standards.auditblanc.infrastructure.TenantEvidenceAdoptionLookup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** Confrontation aux preuves réelles du tenant (§8.4 onglet 7 / §8.7). */
@ExtendWith(MockitoExtension.class)
class TenantEvidenceAdoptionLookupTest {

    @Mock TenantStandardRepository adoptions;
    @Mock RequirementEvidenceRepository evidences;

    private static final UUID TENANT = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID ADOPTION = UUID.fromString("cccccccc-0000-0000-0000-000000000003");
    private static final UUID STD = UUID.fromString("dddddddd-0000-0000-0000-000000000004");

    private final MockAuditTenantProvider tenantProvider = () -> TENANT;

    private TenantEvidenceAdoptionLookup lookup() {
        return new TenantEvidenceAdoptionLookup(adoptions, evidences, tenantProvider);
    }

    private static StandardRequirement req(String code, ObligationLevel obligation, RiskLevel risk) {
        StandardRequirement r = new StandardRequirement();
        r.setId(UUID.randomUUID());
        r.setCode(code);
        r.setText("Exigence " + code);
        r.setObligation(obligation);
        r.setRiskIfMissing(risk);
        r.setOrderIndex(1);
        return r;
    }

    private static StandardClause clause(String code, String title, List<StandardRequirement> reqs) {
        StandardClause c = new StandardClause();
        c.setId(UUID.randomUUID());
        c.setCode(code);
        c.setTitle(title);
        c.setOrderIndex(1);
        c.setRequirements(reqs);
        return c;
    }

    private TenantStandard buildAdoption(List<StandardClause> clauses) {
        StandardSection section = new StandardSection();
        section.setId(UUID.randomUUID());
        section.setCode("8");
        section.setTitle("Réalisation");
        section.setOrderIndex(1);
        section.setClauses(clauses);

        Standard std = new Standard();
        std.setId(STD);
        std.setCode("iso-9001");
        std.setFullName("ISO 9001:2015");
        std.setApplicableIndustries("manufacturing");
        std.setSections(new ArrayList<>(List.of(section)));

        TenantStandard ts = new TenantStandard();
        ts.setId(ADOPTION);
        ts.setTenantId(TENANT);
        ts.setStandard(std);
        return ts;
    }

    @Test
    void computesClauseMatter_fromRealEvidence() {
        StandardRequirement r1 = req("8.1.1", ObligationLevel.MUST, RiskLevel.CRITICAL);
        StandardRequirement r2 = req("8.1.2", ObligationLevel.SHOULD, RiskLevel.LOW);
        StandardClause c81 = clause("8.1", "Maîtrise opérationnelle", List.of(r1, r2));
        StandardRequirement r3 = req("8.2.1", ObligationLevel.MUST, RiskLevel.HIGH);
        StandardClause c82 = clause("8.2", "Exigences produit", List.of(r3));

        when(adoptions.findByIdAndTenantId(ADOPTION, TENANT))
                .thenReturn(Optional.of(buildAdoption(List.of(c81, c82))));
        // Seule r1 est couverte par une preuve liée à cette adoption.
        RequirementEvidence ev = new RequirementEvidence();
        ev.setRequirement(r1);
        when(evidences.findByTenantStandardId(ADOPTION)).thenReturn(List.of(ev));

        Optional<MockAuditAdoptionLookup.AdoptionMatter> matter = lookup().findMatter(ADOPTION);

        assertThat(matter).isPresent();
        MockAuditAdoptionLookup.AdoptionMatter m = matter.get();
        assertThat(m.standardCode()).isEqualTo("iso-9001");
        assertThat(m.industry()).isEqualTo("manufacturing");
        assertThat(m.clauses()).hasSize(2);

        MockAuditClause mc81 = m.clauses().stream()
                .filter(c -> c.clauseCode().equals("8.1")).findFirst().orElseThrow();
        // 8.1 : MUST (car r1 est MUST), risque le plus élevé parmi les MUST = CRITICAL,
        // 1/2 exigences couvertes (r1 seulement).
        assertThat(mc81.obligation()).isEqualTo(ObligationLevel.MUST);
        assertThat(mc81.risk()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(mc81.totalRequirements()).isEqualTo(2);
        assertThat(mc81.coveredRequirements()).isEqualTo(1);

        MockAuditClause mc82 = m.clauses().stream()
                .filter(c -> c.clauseCode().equals("8.2")).findFirst().orElseThrow();
        assertThat(mc82.coveredRequirements()).isZero();
    }

    @Test
    void clauseWithoutRequirements_isSkipped() {
        StandardClause empty = clause("9.9", "Vide", List.of());
        when(adoptions.findByIdAndTenantId(ADOPTION, TENANT))
                .thenReturn(Optional.of(buildAdoption(List.of(empty))));
        when(evidences.findByTenantStandardId(ADOPTION)).thenReturn(List.of());

        MockAuditAdoptionLookup.AdoptionMatter m = lookup().findMatter(ADOPTION).orElseThrow();
        assertThat(m.clauses()).isEmpty();
    }

    @Test
    void nonMustClause_usesWeakestObligationAndAnyRisk() {
        StandardRequirement s1 = req("a.1", ObligationLevel.SHOULD, RiskLevel.HIGH);
        StandardRequirement m1 = req("a.2", ObligationLevel.MAY, RiskLevel.LOW);
        StandardClause c = clause("a", "Recommandations", List.of(s1, m1));
        when(adoptions.findByIdAndTenantId(ADOPTION, TENANT))
                .thenReturn(Optional.of(buildAdoption(List.of(c))));
        when(evidences.findByTenantStandardId(ADOPTION)).thenReturn(List.of());

        MockAuditClause mc = lookup().findMatter(ADOPTION).orElseThrow().clauses().get(0);
        assertThat(mc.obligation()).isEqualTo(ObligationLevel.SHOULD);
        assertThat(mc.risk()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void onlyMayClause_isMay() {
        StandardRequirement m1 = req("b.1", ObligationLevel.MAY, RiskLevel.LOW);
        StandardClause c = clause("b", "Informatif", List.of(m1));
        when(adoptions.findByIdAndTenantId(ADOPTION, TENANT))
                .thenReturn(Optional.of(buildAdoption(List.of(c))));
        when(evidences.findByTenantStandardId(ADOPTION)).thenReturn(List.of());

        MockAuditClause mc = lookup().findMatter(ADOPTION).orElseThrow().clauses().get(0);
        assertThat(mc.obligation()).isEqualTo(ObligationLevel.MAY);
    }

    @Test
    void nullRisk_defaultsToMedium() {
        StandardRequirement r = req("c.1", ObligationLevel.MUST, null);
        StandardClause c = clause("c", "Sans risque", List.of(r));
        when(adoptions.findByIdAndTenantId(ADOPTION, TENANT))
                .thenReturn(Optional.of(buildAdoption(List.of(c))));
        when(evidences.findByTenantStandardId(ADOPTION)).thenReturn(List.of());

        MockAuditClause mc = lookup().findMatter(ADOPTION).orElseThrow().clauses().get(0);
        assertThat(mc.risk()).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    void blankIndustry_defaultsToAll() {
        StandardRequirement r = req("d.1", ObligationLevel.MUST, RiskLevel.HIGH);
        StandardClause c = clause("d", "Clause", List.of(r));
        TenantStandard ts = buildAdoption(List.of(c));
        ts.getStandard().setApplicableIndustries(null);
        when(adoptions.findByIdAndTenantId(ADOPTION, TENANT)).thenReturn(Optional.of(ts));
        when(evidences.findByTenantStandardId(ADOPTION)).thenReturn(List.of());

        assertThat(lookup().findMatter(ADOPTION).orElseThrow().industry()).isEqualTo("all");
    }

    @Test
    void mixedNullAndPresentRisks_takesMaxAndIgnoresNull() {
        // Deux MUST : l'un risque HIGH, l'autre risque null → le max retenu est HIGH
        // (branche candidate==null de maxRisk). Une SHOULD null aussi (anyRisk).
        StandardRequirement r1 = req("e.1", ObligationLevel.MUST, RiskLevel.HIGH);
        StandardRequirement r2 = req("e.2", ObligationLevel.MUST, null);
        StandardRequirement r3 = req("e.3", ObligationLevel.SHOULD, null);
        StandardClause c = clause("e", "Mixte", List.of(r1, r2, r3));
        when(adoptions.findByIdAndTenantId(ADOPTION, TENANT))
                .thenReturn(Optional.of(buildAdoption(List.of(c))));
        when(evidences.findByTenantStandardId(ADOPTION)).thenReturn(List.of());

        MockAuditClause mc = lookup().findMatter(ADOPTION).orElseThrow().clauses().get(0);
        assertThat(mc.obligation()).isEqualTo(ObligationLevel.MUST);
        assertThat(mc.risk()).isEqualTo(RiskLevel.HIGH);
    }

    /** Cross-tenant : une adoption hors tenant courant → empty (404 côté service). */
    @Test
    void crossTenantAdoption_returnsEmpty() {
        when(adoptions.findByIdAndTenantId(ADOPTION, TENANT)).thenReturn(Optional.empty());
        assertThat(lookup().findMatter(ADOPTION)).isEmpty();
    }
}
