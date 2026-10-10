package com.openlab.qualitos.quality.riskregister.infrastructure;

import com.openlab.qualitos.quality.audit.AuditFinding;
import com.openlab.qualitos.quality.audit.AuditFindingRepository;
import com.openlab.qualitos.quality.audit.AuditPlan;
import com.openlab.qualitos.quality.audit.FindingType;
import com.openlab.qualitos.quality.change.ChangeRequest;
import com.openlab.qualitos.quality.change.ChangeRequestRepository;
import com.openlab.qualitos.quality.nonconformity.NcSeverity;
import com.openlab.qualitos.quality.nonconformity.NonConformity;
import com.openlab.qualitos.quality.nonconformity.NonConformityRepository;
import com.openlab.qualitos.quality.risk.ActionPriority;
import com.openlab.qualitos.quality.risk.FmeaItem;
import com.openlab.qualitos.quality.risk.FmeaItemRepository;
import com.openlab.qualitos.quality.risk.FmeaProject;
import com.openlab.qualitos.quality.risk.FmeaProjectRepository;
import com.openlab.qualitos.quality.riskregister.application.RiskSourceCatalog.SourceDraft;
import com.openlab.qualitos.quality.riskregister.domain.RegisterOrigin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PlatformRiskSourceCatalogTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID AUTRE = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final UUID PROJET = UUID.randomUUID();

    FmeaItemRepository items;
    FmeaProjectRepository projects;
    NonConformityRepository ncs;
    AuditFindingRepository findings;
    ChangeRequestRepository changes;
    PlatformRiskSourceCatalog catalog;

    @BeforeEach
    void setUp() {
        items = mock(FmeaItemRepository.class);
        projects = mock(FmeaProjectRepository.class);
        ncs = mock(NonConformityRepository.class);
        findings = mock(AuditFindingRepository.class);
        changes = mock(ChangeRequestRepository.class);
        catalog = new PlatformRiskSourceCatalog(items, projects, ncs, findings, changes, viewAll -> Optional.empty());
    }

    FmeaItem ligne(int s, int o, int d, ActionPriority ap) {
        FmeaItem item = mock(FmeaItem.class);
        when(item.getProjectId()).thenReturn(PROJET);
        when(item.getSequenceNo()).thenReturn(3);
        when(item.getFailureMode()).thenReturn("Cordon poreux");
        when(item.getFailureCause()).thenReturn("Buse usée");
        when(item.getFailureEffect()).thenReturn("Fuite");
        when(item.getSeverity()).thenReturn(s);
        when(item.getOccurrence()).thenReturn(o);
        when(item.getRpn()).thenReturn(s * o * d);
        when(item.getActionPriority()).thenReturn(ap);
        when(items.findByIdAndTenantId(ID, TENANT)).thenReturn(Optional.of(item));
        FmeaProject p = mock(FmeaProject.class);
        when(p.getTenantId()).thenReturn(TENANT);
        when(p.getCode()).thenReturn("PFMEA-7");
        when(p.getCriticalRpnThreshold()).thenReturn(200);
        when(projects.findById(PROJET)).thenReturn(Optional.of(p));
        return item;
    }

    @Test
    void uneLigneAuDessusDuSeuilProposeUnRisqueCoteSurCinq() {
        ligne(9, 5, 5, ActionPriority.MEDIUM);

        SourceDraft d = catalog.find(RegisterOrigin.FMEA, TENANT, ID).orElseThrow();

        assertThat(d.originRef()).isEqualTo("PFMEA-7 #3");
        assertThat(d.title()).isEqualTo("Cordon poreux");
        assertThat(d.cause()).isEqualTo("Buse usée");
        assertThat(d.effect()).isEqualTo("Fuite");
        assertThat(d.grossSeverity()).isEqualTo(5);
        assertThat(d.grossProbability()).isEqualTo(3);
        assertThat(d.eligible()).isTrue();
        assertThat(d.reason()).isNull();
    }

    @Test
    void uneLigneSousLeSeuilEstConnueMaisNonEligible() {
        ligne(3, 3, 3, ActionPriority.LOW);

        SourceDraft d = catalog.find(RegisterOrigin.FMEA, TENANT, ID).orElseThrow();

        assertThat(d.eligible()).isFalse();
        assertThat(d.reason()).isEqualTo(PlatformRiskSourceCatalog.BELOW_THRESHOLD);
    }

    @Test
    void unePrioriteHauteSuffitMemeSousLeSeuilRpn() {
        ligne(9, 2, 2, ActionPriority.HIGH);
        assertThat(catalog.find(RegisterOrigin.FMEA, TENANT, ID).orElseThrow().eligible()).isTrue();
    }

    @Test
    void uneLigneDontLeProjetEstDUnAutreClientEstIntrouvable() {
        ligne(9, 5, 5, ActionPriority.HIGH);
        FmeaProject etranger = mock(FmeaProject.class);
        when(etranger.getTenantId()).thenReturn(AUTRE);
        when(projects.findById(PROJET)).thenReturn(Optional.of(etranger));

        assertThat(catalog.find(RegisterOrigin.FMEA, TENANT, ID)).isEmpty();
        assertThat(catalog.find(RegisterOrigin.FMEA, AUTRE, ID)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"1,1", "2,1", "3,2", "8,4", "9,5", "10,5"})
    void laNoteSurDixSeDiviseParDeuxArrondieAuDessus(int surDix, int surCinq) {
        assertThat(PlatformRiskSourceCatalog.demi(surDix)).isEqualTo(surCinq);
    }

    @Test
    void uneNoteNulleNeSeConvertitPas() {
        assertThat(PlatformRiskSourceCatalog.demi(0)).isNull();
    }

    @Test
    void uneNonConformiteDonneSaReferenceEtSaGravite() {
        NonConformity nc = mock(NonConformity.class);
        when(nc.getReference()).thenReturn("NC-2026-0042");
        when(nc.getTitle()).thenReturn("Fuite circuit");
        when(nc.getRootCause()).thenReturn(" ");
        when(nc.getDescription()).thenReturn("Fuite constatée");
        when(nc.getSeverity()).thenReturn(NcSeverity.MAJOR);
        when(ncs.findByIdAndTenantId(ID, TENANT)).thenReturn(Optional.of(nc));

        SourceDraft d = catalog.find(RegisterOrigin.NON_CONFORMITY, TENANT, ID).orElseThrow();

        assertThat(d.originRef()).isEqualTo("NC-2026-0042");
        assertThat(d.cause()).isNull();
        assertThat(d.effect()).isEqualTo("Fuite constatée");
        assertThat(d.grossSeverity()).isEqualTo(4);
        assertThat(d.grossProbability()).isNull();
        assertThat(d.eligible()).isTrue();
        assertThat(catalog.find(RegisterOrigin.NON_CONFORMITY, AUTRE, ID)).isEmpty();
    }

    AuditFinding constat(FindingType type, UUID tenantDuPlan) {
        AuditPlan plan = mock(AuditPlan.class);
        when(plan.getTenantId()).thenReturn(tenantDuPlan);
        when(plan.getReference()).thenReturn("AUD-2026-0001");
        AuditFinding f = mock(AuditFinding.class);
        when(f.getPlan()).thenReturn(plan);
        when(f.getType()).thenReturn(type);
        when(f.getDescription()).thenReturn("Étalonnage échu sur la balance B2");
        when(f.getClauseRef()).thenReturn("7.1.5");
        when(findings.findById(ID)).thenReturn(Optional.of(f));
        return f;
    }

    @Test
    void unEcartDAuditSeVerifieParSonPlan() {
        constat(FindingType.MAJOR_NC, TENANT);

        SourceDraft d = catalog.find(RegisterOrigin.AUDIT, TENANT, ID).orElseThrow();
        assertThat(d.originRef()).isEqualTo("AUD-2026-0001 · 7.1.5");
        assertThat(d.grossSeverity()).isEqualTo(4);
        assertThat(d.eligible()).isTrue();

        assertThat(catalog.find(RegisterOrigin.AUDIT, AUTRE, ID)).isEmpty();
    }

    @Test
    void unConstatDeConformiteNeJustifiePasUnRisque() {
        constat(FindingType.CONFORMITY, TENANT);

        SourceDraft d = catalog.find(RegisterOrigin.AUDIT, TENANT, ID).orElseThrow();
        assertThat(d.eligible()).isFalse();
        assertThat(d.reason()).isEqualTo(PlatformRiskSourceCatalog.NOT_A_GAP);
    }

    @Test
    void unChangementDonneSonAnalyseDeRisqueEtDImpact() {
        ChangeRequest c = mock(ChangeRequest.class);
        when(c.getTenantId()).thenReturn(TENANT);
        when(c.getCode()).thenReturn("CHG-2026-0007");
        when(c.getTitle()).thenReturn("Nouveau fournisseur de flux");
        when(c.getRiskAssessment()).thenReturn("Compatibilité chimique");
        when(c.getImpactSummary()).thenReturn("Qualification à refaire");
        when(changes.findById(ID)).thenReturn(Optional.of(c));

        SourceDraft d = catalog.find(RegisterOrigin.CHANGE, TENANT, ID).orElseThrow();
        assertThat(d.originRef()).isEqualTo("CHG-2026-0007");
        assertThat(d.cause()).isEqualTo("Compatibilité chimique");
        assertThat(d.effect()).isEqualTo("Qualification à refaire");
        assertThat(catalog.find(RegisterOrigin.CHANGE, AUTRE, ID)).isEmpty();
    }

    @Test
    void uneOrigineSansObjetOuUnArgumentNulNeRendRien() {
        assertThat(catalog.find(RegisterOrigin.DIRECT, TENANT, ID)).isEmpty();
        assertThat(catalog.find(null, TENANT, ID)).isEmpty();
        assertThat(catalog.find(RegisterOrigin.FMEA, null, ID)).isEmpty();
        assertThat(catalog.find(RegisterOrigin.FMEA, TENANT, null)).isEmpty();
        verifyNoInteractions(items, ncs, findings, changes);
    }

    @Test
    void unTexteTropLongEstRogne() {
        assertThat(PlatformRiskSourceCatalog.borne("x".repeat(300), 255)).hasSize(255);
        assertThat(PlatformRiskSourceCatalog.borne(null, 10)).isNull();
    }
}
