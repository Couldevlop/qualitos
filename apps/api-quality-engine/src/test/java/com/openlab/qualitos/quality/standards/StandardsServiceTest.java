package com.openlab.qualitos.quality.standards;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StandardsServiceTest {

    @Mock StandardRepository standardRepo;
    @Mock StandardRequirementRepository requirementRepo;
    @Mock TenantStandardRepository tenantStandardRepo;
    @Mock RequirementEvidenceRepository evidenceRepo;
    @Mock CertificationRoadmapStageRepository roadmapRepo;
    @InjectMocks StandardsService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID LEAD = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();

    @BeforeEach void ctx() { TenantContext.setTenantId(TENANT.toString()); }
    @AfterEach  void clr() { TenantContext.clear(); }

    // --- catalog: list ---
    @Test
    void listStandards_noFilter() {
        Pageable p = PageRequest.of(0, 10);
        Standard s = std("iso-9001", StandardStatus.PUBLISHED);
        when(standardRepo.findAll(p)).thenReturn(new PageImpl<>(List.of(s)));
        Page<StandardsDto.StandardSummary> r = service.listStandards(null, null, p);
        assertThat(r.getContent()).hasSize(1);
        verify(standardRepo, never()).findByStatus(any(), any());
        verify(standardRepo, never()).findByFamily(any(), any());
    }

    @Test
    void listStandards_byStatus() {
        Pageable p = PageRequest.of(0, 10);
        when(standardRepo.findByStatus(StandardStatus.DEPRECATED, p))
                .thenReturn(new PageImpl<>(List.of(std("old", StandardStatus.DEPRECATED))));
        service.listStandards(StandardStatus.DEPRECATED, null, p);
        verify(standardRepo).findByStatus(StandardStatus.DEPRECATED, p);
    }

    @Test
    void listStandards_byFamily() {
        Pageable p = PageRequest.of(0, 10);
        when(standardRepo.findByFamily("HLS", p))
                .thenReturn(new PageImpl<>(List.of(std("iso-9001", StandardStatus.PUBLISHED))));
        service.listStandards(null, "HLS", p);
        verify(standardRepo).findByFamily("HLS", p);
    }

    @Test
    void listStandards_statusTakesPrecedenceOverFamily() {
        Pageable p = PageRequest.of(0, 10);
        when(standardRepo.findByStatus(StandardStatus.PUBLISHED, p))
                .thenReturn(new PageImpl<>(List.of()));
        service.listStandards(StandardStatus.PUBLISHED, "HLS", p);
        verify(standardRepo).findByStatus(StandardStatus.PUBLISHED, p);
        verify(standardRepo, never()).findByFamily(any(), any());
    }

    // --- catalog: get ---
    @Test
    void getStandard_withFullTree() {
        Standard s = stdWithTree();
        when(standardRepo.findById(s.getId())).thenReturn(Optional.of(s));
        StandardsDto.StandardDetail r = service.getStandard(s.getId());
        assertThat(r.sections()).hasSize(1);
        assertThat(r.sections().get(0).clauses()).hasSize(1);
        assertThat(r.sections().get(0).clauses().get(0).requirements()).hasSize(2);
    }

    @Test
    void getStandard_notFound() {
        UUID id = UUID.randomUUID();
        when(standardRepo.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getStandard(id))
                .isInstanceOf(StandardNotFoundException.class);
    }

    @Test
    void getStandardByCode_found() {
        Standard s = std("iso-9001", StandardStatus.PUBLISHED);
        when(standardRepo.findByCode("iso-9001")).thenReturn(Optional.of(s));
        assertThat(service.getStandardByCode("iso-9001").code()).isEqualTo("iso-9001");
    }

    @Test
    void getStandardByCode_notFound() {
        when(standardRepo.findByCode("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getStandardByCode("nope"))
                .isInstanceOf(StandardNotFoundException.class);
    }

    // --- adoptions ---
    @Test
    void listAdoptions_noFilter() {
        Pageable p = PageRequest.of(0, 10);
        when(tenantStandardRepo.findByTenantId(TENANT, p))
                .thenReturn(new PageImpl<>(List.of(adoption(AdoptionStatus.PLANNING))));
        Page<StandardsDto.AdoptionResponse> r = service.listAdoptions(null, p);
        assertThat(r.getContent()).hasSize(1);
        verify(tenantStandardRepo, never()).findByTenantIdAndStatus(any(), any(), any());
    }

    @Test
    void listAdoptions_byStatus() {
        Pageable p = PageRequest.of(0, 10);
        when(tenantStandardRepo.findByTenantIdAndStatus(TENANT, AdoptionStatus.CERTIFIED, p))
                .thenReturn(new PageImpl<>(List.of(adoption(AdoptionStatus.CERTIFIED))));
        Page<StandardsDto.AdoptionResponse> r = service.listAdoptions(AdoptionStatus.CERTIFIED, p);
        assertThat(r.getContent().get(0).status()).isEqualTo(AdoptionStatus.CERTIFIED);
    }

    @Test
    void getAdoption_found() {
        TenantStandard ts = adoption(AdoptionStatus.PLANNING);
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        assertThat(service.getAdoption(ts.getId()).id()).isEqualTo(ts.getId());
    }

    @Test
    void getAdoption_notFound() {
        UUID id = UUID.randomUUID();
        when(tenantStandardRepo.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getAdoption(id))
                .isInstanceOf(TenantStandardNotFoundException.class);
    }

    @Test
    void adopt_success() {
        Standard s = std("iso-9001", StandardStatus.PUBLISHED);
        when(standardRepo.findById(s.getId())).thenReturn(Optional.of(s));
        when(tenantStandardRepo.existsByTenantIdAndStandardId(TENANT, s.getId())).thenReturn(false);
        when(tenantStandardRepo.save(any())).thenAnswer(inv -> {
            TenantStandard t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            t.setCreatedAt(Instant.now());
            t.setUpdatedAt(Instant.now());
            return t;
        });

        StandardsDto.AdoptionResponse r = service.adopt(new StandardsDto.AdoptRequest(
                s.getId(), "scope ISO", LocalDate.now().plusMonths(12), LEAD, "AFNOR"));

        assertThat(r.status()).isEqualTo(AdoptionStatus.PLANNING);
        assertThat(r.standardCode()).isEqualTo("iso-9001");
    }

    @Test
    void adopt_missingTenant_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.adopt(new StandardsDto.AdoptRequest(
                UUID.randomUUID(), null, null, null, null)))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void adopt_standardNotFound() {
        UUID id = UUID.randomUUID();
        when(standardRepo.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.adopt(new StandardsDto.AdoptRequest(
                id, null, null, null, null)))
                .isInstanceOf(StandardNotFoundException.class);
    }

    @Test
    void adopt_deprecatedStandard_throws() {
        Standard s = std("old", StandardStatus.DEPRECATED);
        when(standardRepo.findById(s.getId())).thenReturn(Optional.of(s));
        assertThatThrownBy(() -> service.adopt(new StandardsDto.AdoptRequest(
                s.getId(), null, null, null, null)))
                .isInstanceOf(AdoptionConflictException.class);
    }

    @Test
    void adopt_alreadyAdopted_throws() {
        Standard s = std("iso-9001", StandardStatus.PUBLISHED);
        when(standardRepo.findById(s.getId())).thenReturn(Optional.of(s));
        when(tenantStandardRepo.existsByTenantIdAndStandardId(TENANT, s.getId())).thenReturn(true);
        assertThatThrownBy(() -> service.adopt(new StandardsDto.AdoptRequest(
                s.getId(), null, null, null, null)))
                .isInstanceOf(AdoptionConflictException.class)
                .hasMessageContaining("already adopted");
    }

    @Test
    void updateAdoption_success() {
        TenantStandard ts = adoption(AdoptionStatus.IN_PROGRESS);
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        when(tenantStandardRepo.save(ts)).thenReturn(ts);
        UUID newLead = UUID.randomUUID();
        LocalDate when = LocalDate.now().plusMonths(6);
        service.updateAdoption(ts.getId(),
                new StandardsDto.UpdateAdoptionRequest("new scope", when, newLead, "BSI"));
        assertThat(ts.getScopeDescription()).isEqualTo("new scope");
        assertThat(ts.getLeadAuditorId()).isEqualTo(newLead);
        assertThat(ts.getCertificationBody()).isEqualTo("BSI");
        assertThat(ts.getTargetCertificationDate()).isEqualTo(when);
    }

    @Test
    void updateAdoption_withdrawn_throws() {
        TenantStandard ts = adoption(AdoptionStatus.WITHDRAWN);
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        assertThatThrownBy(() -> service.updateAdoption(ts.getId(),
                new StandardsDto.UpdateAdoptionRequest("x", null, null, null)))
                .isInstanceOf(AdoptionStateException.class);
    }

    @Test
    void startProgress_success() {
        TenantStandard ts = adoption(AdoptionStatus.PLANNING);
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        when(tenantStandardRepo.save(ts)).thenReturn(ts);
        service.startProgress(ts.getId());
        assertThat(ts.getStatus()).isEqualTo(AdoptionStatus.IN_PROGRESS);
    }

    @Test
    void startProgress_notPlanning_throws() {
        TenantStandard ts = adoption(AdoptionStatus.IN_PROGRESS);
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        assertThatThrownBy(() -> service.startProgress(ts.getId()))
                .isInstanceOf(AdoptionStateException.class);
    }

    @Test
    void certify_success_autoComputesExpiry() {
        TenantStandard ts = adoption(AdoptionStatus.IN_PROGRESS);
        ts.getStandard().setRecertificationCycleMonths(36);
        Instant when = Instant.now();
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        when(tenantStandardRepo.save(ts)).thenReturn(ts);

        service.certify(ts.getId(), new StandardsDto.CertifyRequest(when, null));

        assertThat(ts.getStatus()).isEqualTo(AdoptionStatus.CERTIFIED);
        assertThat(ts.getCertifiedAt()).isEqualTo(when);
        assertThat(ts.getExpiresAt()).isNotNull();
        assertThat(ts.getExpiresAt()).isAfter(when);
    }

    @Test
    void certify_success_explicitExpiry() {
        TenantStandard ts = adoption(AdoptionStatus.IN_PROGRESS);
        Instant when = Instant.now();
        Instant expires = when.plusSeconds(36 * 30L * 86400);
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        when(tenantStandardRepo.save(ts)).thenReturn(ts);
        service.certify(ts.getId(), new StandardsDto.CertifyRequest(when, expires));
        assertThat(ts.getExpiresAt()).isEqualTo(expires);
    }

    @Test
    void certify_notInProgress_throws() {
        TenantStandard ts = adoption(AdoptionStatus.PLANNING);
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        assertThatThrownBy(() -> service.certify(ts.getId(),
                new StandardsDto.CertifyRequest(Instant.now(), null)))
                .isInstanceOf(AdoptionStateException.class);
    }

    @Test
    void markSurveillance_fromCertified() {
        TenantStandard ts = adoption(AdoptionStatus.CERTIFIED);
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        when(tenantStandardRepo.save(ts)).thenReturn(ts);
        service.markSurveillance(ts.getId());
        assertThat(ts.getStatus()).isEqualTo(AdoptionStatus.SURVEILLANCE);
    }

    @Test
    void markSurveillance_notCertified_throws() {
        TenantStandard ts = adoption(AdoptionStatus.IN_PROGRESS);
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        assertThatThrownBy(() -> service.markSurveillance(ts.getId()))
                .isInstanceOf(AdoptionStateException.class);
    }

    @Test
    void withdraw_success() {
        TenantStandard ts = adoption(AdoptionStatus.IN_PROGRESS);
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        when(tenantStandardRepo.save(ts)).thenReturn(ts);
        service.withdraw(ts.getId());
        assertThat(ts.getStatus()).isEqualTo(AdoptionStatus.WITHDRAWN);
    }

    @Test
    void withdraw_alreadyWithdrawn_throws() {
        TenantStandard ts = adoption(AdoptionStatus.WITHDRAWN);
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        assertThatThrownBy(() -> service.withdraw(ts.getId()))
                .isInstanceOf(AdoptionStateException.class);
    }

    @Test
    void deleteAdoption_success() {
        TenantStandard ts = adoption(AdoptionStatus.PLANNING);
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        service.deleteAdoption(ts.getId());
        verify(tenantStandardRepo).delete(ts);
    }

    @Test
    void deleteAdoption_certified_throws() {
        TenantStandard ts = adoption(AdoptionStatus.CERTIFIED);
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        assertThatThrownBy(() -> service.deleteAdoption(ts.getId()))
                .isInstanceOf(AdoptionStateException.class);
    }

    // --- evidence ---
    @Test
    void linkEvidence_success() {
        Standard s = stdWithTree();
        TenantStandard ts = adoption(AdoptionStatus.IN_PROGRESS);
        ts.setStandard(s);
        StandardRequirement req = s.getSections().get(0).getClauses().get(0).getRequirements().get(0);
        UUID docRef = UUID.randomUUID();

        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        when(requirementRepo.findById(req.getId())).thenReturn(Optional.of(req));
        when(evidenceRepo.existsByTenantStandardIdAndRequirementIdAndEvidenceRefId(
                ts.getId(), req.getId(), docRef)).thenReturn(false);
        when(evidenceRepo.save(any())).thenAnswer(inv -> {
            RequirementEvidence ev = inv.getArgument(0);
            ev.setId(UUID.randomUUID());
            ev.setLinkedAt(Instant.now());
            return ev;
        });

        StandardsDto.EvidenceResponse r = service.linkEvidence(ts.getId(),
                new StandardsDto.LinkEvidenceRequest(
                        req.getId(), EvidenceType.DOCUMENT, docRef, null, "note", USER));

        assertThat(r.evidenceType()).isEqualTo(EvidenceType.DOCUMENT);
        assertThat(r.evidenceRefId()).isEqualTo(docRef);
    }

    @Test
    void linkEvidence_withdrawnAdoption_throws() {
        TenantStandard ts = adoption(AdoptionStatus.WITHDRAWN);
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        assertThatThrownBy(() -> service.linkEvidence(ts.getId(),
                new StandardsDto.LinkEvidenceRequest(UUID.randomUUID(), EvidenceType.DOCUMENT,
                        UUID.randomUUID(), null, null, USER)))
                .isInstanceOf(AdoptionStateException.class);
    }

    @Test
    void linkEvidence_requirementNotFound_throws() {
        TenantStandard ts = adoption(AdoptionStatus.IN_PROGRESS);
        UUID reqId = UUID.randomUUID();
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        when(requirementRepo.findById(reqId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.linkEvidence(ts.getId(),
                new StandardsDto.LinkEvidenceRequest(reqId, EvidenceType.DOCUMENT,
                        UUID.randomUUID(), null, null, USER)))
                .isInstanceOf(RequirementNotFoundException.class);
    }

    @Test
    void linkEvidence_requirementFromDifferentStandard_throws() {
        Standard s1 = stdWithTree();
        Standard s2 = std("other", StandardStatus.PUBLISHED);
        TenantStandard ts = adoption(AdoptionStatus.IN_PROGRESS);
        ts.setStandard(s2); // adopté sur s2
        StandardRequirement req = s1.getSections().get(0).getClauses().get(0).getRequirements().get(0);

        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        when(requirementRepo.findById(req.getId())).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> service.linkEvidence(ts.getId(),
                new StandardsDto.LinkEvidenceRequest(req.getId(), EvidenceType.DOCUMENT,
                        UUID.randomUUID(), null, null, USER)))
                .isInstanceOf(AdoptionStateException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void linkEvidence_duplicate_throws() {
        Standard s = stdWithTree();
        TenantStandard ts = adoption(AdoptionStatus.IN_PROGRESS);
        ts.setStandard(s);
        StandardRequirement req = s.getSections().get(0).getClauses().get(0).getRequirements().get(0);
        UUID docRef = UUID.randomUUID();

        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        when(requirementRepo.findById(req.getId())).thenReturn(Optional.of(req));
        when(evidenceRepo.existsByTenantStandardIdAndRequirementIdAndEvidenceRefId(
                ts.getId(), req.getId(), docRef)).thenReturn(true);

        assertThatThrownBy(() -> service.linkEvidence(ts.getId(),
                new StandardsDto.LinkEvidenceRequest(req.getId(), EvidenceType.DOCUMENT,
                        docRef, null, null, USER)))
                .isInstanceOf(AdoptionConflictException.class);
    }

    @Test
    void listEvidence_returnsAll() {
        TenantStandard ts = adoption(AdoptionStatus.IN_PROGRESS);
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        when(evidenceRepo.findByTenantStandardId(ts.getId())).thenReturn(List.of(evidence(ts)));
        assertThat(service.listEvidence(ts.getId())).hasSize(1);
    }

    @Test
    void deleteEvidence_success() {
        TenantStandard ts = adoption(AdoptionStatus.IN_PROGRESS);
        RequirementEvidence ev = evidence(ts);
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        when(evidenceRepo.findByIdAndTenantStandardId(ev.getId(), ts.getId())).thenReturn(Optional.of(ev));
        service.deleteEvidence(ts.getId(), ev.getId());
        verify(evidenceRepo).delete(ev);
    }

    @Test
    void deleteEvidence_notFound_throws() {
        TenantStandard ts = adoption(AdoptionStatus.IN_PROGRESS);
        UUID eid = UUID.randomUUID();
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        when(evidenceRepo.findByIdAndTenantStandardId(eid, ts.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteEvidence(ts.getId(), eid))
                .isInstanceOf(EvidenceNotFoundException.class);
    }

    // --- alignment ---
    @Test
    void computeAlignment_partialCoverage() {
        Standard s = stdWithTree();
        TenantStandard ts = adoption(AdoptionStatus.IN_PROGRESS);
        ts.setStandard(s);
        StandardRequirement r1 = s.getSections().get(0).getClauses().get(0).getRequirements().get(0);
        // r1 covered, r2 not covered
        RequirementEvidence ev = new RequirementEvidence();
        ev.setRequirement(r1);
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        when(evidenceRepo.findByTenantStandardId(ts.getId())).thenReturn(List.of(ev));

        StandardsDto.AlignmentReport report = service.computeAlignment(ts.getId());

        assertThat(report.totalRequirements()).isEqualTo(2);
        assertThat(report.coveredRequirements()).isEqualTo(1);
        assertThat(report.overallScore()).isEqualTo(50d);
        assertThat(report.sections()).hasSize(1);
        assertThat(report.sections().get(0).clauses().get(0).score()).isEqualTo(50d);
        // r1 is MUST → 1 must covered out of 2 must (both reqs are MUST in fixture)
        assertThat(report.totalMustRequirements()).isEqualTo(2);
        assertThat(report.coveredMustRequirements()).isEqualTo(1);
    }

    @Test
    void computeAlignment_fullCoverage() {
        Standard s = stdWithTree();
        TenantStandard ts = adoption(AdoptionStatus.IN_PROGRESS);
        ts.setStandard(s);
        List<StandardRequirement> reqs = s.getSections().get(0).getClauses().get(0).getRequirements();
        RequirementEvidence ev1 = new RequirementEvidence();
        ev1.setRequirement(reqs.get(0));
        RequirementEvidence ev2 = new RequirementEvidence();
        ev2.setRequirement(reqs.get(1));

        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        when(evidenceRepo.findByTenantStandardId(ts.getId())).thenReturn(List.of(ev1, ev2));

        StandardsDto.AlignmentReport report = service.computeAlignment(ts.getId());
        assertThat(report.overallScore()).isEqualTo(100d);
    }

    @Test
    void computeAlignment_emptyStandard_returnsZero() {
        Standard s = std("empty", StandardStatus.PUBLISHED);
        TenantStandard ts = adoption(AdoptionStatus.IN_PROGRESS);
        ts.setStandard(s);
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        when(evidenceRepo.findByTenantStandardId(ts.getId())).thenReturn(List.of());

        StandardsDto.AlignmentReport report = service.computeAlignment(ts.getId());
        assertThat(report.totalRequirements()).isZero();
        assertThat(report.overallScore()).isZero();
    }

    // --- audit blanc (§8.7) ---
    @Test
    void auditBlanc_noEvidence_allRequirementsAreFindings() {
        Standard s = stdWithTree(); // 2 exigences MUST, 0 preuve
        TenantStandard ts = adoption(AdoptionStatus.IN_PROGRESS);
        ts.setStandard(s);
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        when(evidenceRepo.findByTenantStandardId(ts.getId())).thenReturn(List.of());

        StandardsDto.AuditBlancReport r = service.computeAuditBlanc(ts.getId());

        assertThat(r.totalRequirements()).isEqualTo(2);
        assertThat(r.coveredRequirements()).isZero();
        assertThat(r.mustTotal()).isEqualTo(2);
        assertThat(r.readinessScore()).isZero();
        assertThat(r.findings()).hasSize(2);
        assertThat(r.verdict()).contains("NON PRÊT");
    }

    @Test
    void auditBlanc_criticalRiskMustGap_isCriticalSeverityAndTopPriority() {
        Standard s = stdWithTree();
        StandardRequirement r1 = s.getSections().get(0).getClauses().get(0).getRequirements().get(0);
        r1.setRiskIfMissing(RiskLevel.CRITICAL);
        TenantStandard ts = adoption(AdoptionStatus.IN_PROGRESS);
        ts.setStandard(s);
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        when(evidenceRepo.findByTenantStandardId(ts.getId())).thenReturn(List.of());

        StandardsDto.AuditBlancReport r = service.computeAuditBlanc(ts.getId());

        assertThat(r.criticalGaps()).isEqualTo(1);
        // findings triés par priorité → le critique en tête
        assertThat(r.findings().get(0).findingSeverity()).isEqualTo("CRITICAL");
        assertThat(r.findings().get(0).remediationPriority()).isEqualTo(1);
        assertThat(r.findings().get(0).remediationAction()).contains(r1.getCode());
    }

    @Test
    void auditBlanc_partialCoverage_readinessReflectsMustCoverage() {
        Standard s = stdWithTree();
        StandardRequirement r1 = s.getSections().get(0).getClauses().get(0).getRequirements().get(0);
        TenantStandard ts = adoption(AdoptionStatus.IN_PROGRESS);
        ts.setStandard(s);
        RequirementEvidence ev = new RequirementEvidence();
        ev.setRequirement(r1);
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        when(evidenceRepo.findByTenantStandardId(ts.getId())).thenReturn(List.of(ev));

        StandardsDto.AuditBlancReport r = service.computeAuditBlanc(ts.getId());

        assertThat(r.mustCovered()).isEqualTo(1);
        assertThat(r.readinessScore()).isEqualTo(50d);
        assertThat(r.findings()).hasSize(1);
    }

    // --- roadmap de certification (§8.5) ---
    @Test
    void adopt_generatesNineteenStageRoadmap() {
        Standard s = std("iso-9001", StandardStatus.PUBLISHED);
        when(standardRepo.findById(s.getId())).thenReturn(Optional.of(s));
        when(tenantStandardRepo.existsByTenantIdAndStandardId(TENANT, s.getId())).thenReturn(false);
        when(tenantStandardRepo.save(any())).thenAnswer(inv -> {
            TenantStandard t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            t.setCreatedAt(Instant.now());
            t.setUpdatedAt(Instant.now());
            return t;
        });

        service.adopt(new StandardsDto.AdoptRequest(s.getId(), "scope", null, null, null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CertificationRoadmapStage>> captor = ArgumentCaptor.forClass(List.class);
        verify(roadmapRepo).saveAll(captor.capture());
        List<CertificationRoadmapStage> stages = captor.getValue();
        assertThat(stages).hasSize(19);
        assertThat(stages.get(0).getStepNumber()).isEqualTo(1);
        assertThat(stages.get(0).getStatus()).isEqualTo(StageStatus.NOT_STARTED);
        assertThat(stages).allSatisfy(st -> assertThat(st.getTenantId()).isEqualTo(TENANT));
    }

    @Test
    void getRoadmap_computesCompletionExcludingSkipped() {
        TenantStandard ts = adoption(AdoptionStatus.IN_PROGRESS);
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        // 4 étapes : 2 DONE, 1 SKIPPED, 1 NOT_STARTED → progression = 2 / (4-1) = 66.67%
        when(roadmapRepo.findByTenantStandardIdOrderByOrderIndexAsc(ts.getId())).thenReturn(List.of(
                stage(1, StageStatus.DONE), stage(2, StageStatus.DONE),
                stage(3, StageStatus.SKIPPED), stage(4, StageStatus.NOT_STARTED)));

        StandardsDto.RoadmapSummary r = service.getRoadmap(ts.getId());

        assertThat(r.totalStages()).isEqualTo(4);
        assertThat(r.doneStages()).isEqualTo(2);
        assertThat(r.skippedStages()).isEqualTo(1);
        assertThat(r.completionPercent()).isEqualTo(200d / 3);
        assertThat(r.stages()).hasSize(4);
    }

    @Test
    void updateStage_success() {
        TenantStandard ts = adoption(AdoptionStatus.IN_PROGRESS);
        CertificationRoadmapStage st = stage(2, StageStatus.NOT_STARTED);
        UUID assignee = UUID.randomUUID();
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        when(roadmapRepo.findByIdAndTenantStandardId(st.getId(), ts.getId())).thenReturn(Optional.of(st));
        when(roadmapRepo.save(st)).thenReturn(st);

        service.updateStage(ts.getId(), st.getId(), new StandardsDto.UpdateStageRequest(
                StageStatus.IN_PROGRESS, assignee, LocalDate.now(), null, null, null, "go"));

        assertThat(st.getStatus()).isEqualTo(StageStatus.IN_PROGRESS);
        assertThat(st.getAssigneeId()).isEqualTo(assignee);
        assertThat(st.getNotes()).isEqualTo("go");
    }

    @Test
    void updateStage_notFound_throws() {
        TenantStandard ts = adoption(AdoptionStatus.IN_PROGRESS);
        UUID stageId = UUID.randomUUID();
        when(tenantStandardRepo.findByIdAndTenantId(ts.getId(), TENANT)).thenReturn(Optional.of(ts));
        when(roadmapRepo.findByIdAndTenantStandardId(stageId, ts.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateStage(ts.getId(), stageId,
                new StandardsDto.UpdateStageRequest(StageStatus.DONE, null, null, null, null, null, null)))
                .isInstanceOf(RoadmapStageNotFoundException.class);
    }

    private CertificationRoadmapStage stage(int step, StageStatus status) {
        CertificationRoadmapStage st = new CertificationRoadmapStage();
        st.setId(UUID.randomUUID());
        st.setTenantId(TENANT);
        st.setStepNumber(step);
        st.setOrderIndex(step);
        st.setName("Étape " + step);
        st.setStatus(status);
        return st;
    }

    // --- helpers ---
    private Standard std(String code, StandardStatus status) {
        Standard s = new Standard();
        s.setId(UUID.randomUUID());
        s.setCode(code);
        s.setFullName("Norme " + code);
        s.setCurrentVersion("2015");
        s.setStatus(status);
        s.setCreatedAt(Instant.now());
        s.setUpdatedAt(Instant.now());
        return s;
    }

    private Standard stdWithTree() {
        Standard s = std("iso-9001", StandardStatus.PUBLISHED);
        StandardSection sec = new StandardSection();
        sec.setId(UUID.randomUUID());
        sec.setStandard(s);
        sec.setCode("4");
        sec.setTitle("Contexte");
        sec.setOrderIndex(0);
        StandardClause cl = new StandardClause();
        cl.setId(UUID.randomUUID());
        cl.setSection(sec);
        cl.setCode("4.1");
        cl.setTitle("Compréhension");
        cl.setOrderIndex(0);
        StandardRequirement r1 = new StandardRequirement();
        r1.setId(UUID.randomUUID());
        r1.setClause(cl);
        r1.setCode("4.1.1");
        r1.setText("Doit déterminer enjeux");
        r1.setObligation(ObligationLevel.MUST);
        r1.setOrderIndex(0);
        StandardRequirement r2 = new StandardRequirement();
        r2.setId(UUID.randomUUID());
        r2.setClause(cl);
        r2.setCode("4.1.2");
        r2.setText("Doit revoir");
        r2.setObligation(ObligationLevel.MUST);
        r2.setOrderIndex(1);
        cl.getRequirements().add(r1);
        cl.getRequirements().add(r2);
        sec.getClauses().add(cl);
        s.getSections().add(sec);
        return s;
    }

    private TenantStandard adoption(AdoptionStatus status) {
        TenantStandard ts = new TenantStandard();
        ts.setId(UUID.randomUUID());
        ts.setTenantId(TENANT);
        ts.setStandard(std("iso-9001", StandardStatus.PUBLISHED));
        ts.setStatus(status);
        ts.setCreatedAt(Instant.now());
        ts.setUpdatedAt(Instant.now());
        return ts;
    }

    private RequirementEvidence evidence(TenantStandard ts) {
        RequirementEvidence ev = new RequirementEvidence();
        ev.setId(UUID.randomUUID());
        ev.setTenantId(TENANT);
        ev.setTenantStandard(ts);
        StandardRequirement r = new StandardRequirement();
        r.setId(UUID.randomUUID());
        r.setCode("X");
        r.setClause(new StandardClause());
        ev.setRequirement(r);
        ev.setEvidenceType(EvidenceType.DOCUMENT);
        ev.setLinkedBy(USER);
        ev.setLinkedAt(Instant.now());
        return ev;
    }
}
