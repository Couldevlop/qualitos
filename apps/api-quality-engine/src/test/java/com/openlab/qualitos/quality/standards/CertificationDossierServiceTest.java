package com.openlab.qualitos.quality.standards;

import com.openlab.qualitos.quality.blockchain.domain.BlockchainAnchorPort;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificationDossierServiceTest {

    @Mock StandardsService standards;
    @Mock BlockchainAnchorPort blockchain;
    CertificationDossierService dossierService;

    static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-000000000099");
    static final UUID ADOPTION = UUID.randomUUID();
    static final UUID STD = UUID.randomUUID();

    @BeforeEach void setup() {
        dossierService = new CertificationDossierService(standards, blockchain);
        TenantContext.setTenantId(TENANT.toString());
    }
    @AfterEach void clr() { TenantContext.clear(); }

    @Test
    void generate_assemblesDossier_computesHash_andAnchors() {
        stubStandardsPort("SMQ siège");
        when(blockchain.submitRoot(eq(TENANT), any())).thenReturn("stub-tx-1234");

        StandardsDto.DossierResponse r = dossierService.generate(ADOPTION);

        assertThat(r.standardCode()).isEqualTo("iso-9001");
        assertThat(r.sha256()).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(r.anchorTxRef()).isEqualTo("stub-tx-1234");
        assertThat(r.contentType()).isEqualTo("text/html");
        assertThat(r.fileName()).contains("iso-9001").endsWith(".html");
        assertThat(r.htmlContent())
                .contains("Dossier de certification")
                .contains("Roadmap de certification")
                .contains("Audit blanc")
                .contains("SMQ siège");
        assertThat(r.evidenceCount()).isEqualTo(1);
    }

    @Test
    void generate_escapesHtml_preventingStoredXss() {
        // Périmètre malveillant : doit être échappé dans le HTML (OWASP A03).
        stubStandardsPort("<script>alert('x')</script>");
        when(blockchain.submitRoot(eq(TENANT), any())).thenReturn("stub-tx");

        StandardsDto.DossierResponse r = dossierService.generate(ADOPTION);

        assertThat(r.htmlContent()).doesNotContain("<script>alert");
        assertThat(r.htmlContent()).contains("&lt;script&gt;");
    }

    private void stubStandardsPort(String scope) {
        when(standards.getAdoption(ADOPTION)).thenReturn(new StandardsDto.AdoptionResponse(
                ADOPTION, TENANT, STD, "iso-9001", "ISO 9001:2015",
                AdoptionStatus.IN_PROGRESS, scope, null, null, "AFNOR",
                null, null, Instant.now(), Instant.now()));
        when(standards.getStandard(STD)).thenReturn(new StandardsDto.StandardDetail(
                STD, "iso-9001", "ISO 9001:2015 — Systèmes de management de la qualité",
                "ISO", "2015", null, "HLS", "all", "desc", true, 36, null,
                StandardStatus.PUBLISHED, List.of()));
        when(standards.getRoadmap(ADOPTION)).thenReturn(new StandardsDto.RoadmapSummary(
                ADOPTION, 19, 2, 1, 0, 10.5,
                List.of(new StandardsDto.RoadmapStageResponse(
                        UUID.randomUUID(), 1, "Cadrage", "desc", "2-4 sem", "livrables",
                        "Direction", "PDCA", StageStatus.DONE, null,
                        null, null, null, null, null, 1))));
        when(standards.computeAlignment(ADOPTION)).thenReturn(new StandardsDto.AlignmentReport(
                ADOPTION, STD, "iso-9001", 50d, 42, 21, 40, 20,
                List.of(new StandardsDto.SectionAlignment(
                        UUID.randomUUID(), "4", "Contexte", 50d, 5, 2, List.of()))));
        when(standards.computeAuditBlanc(ADOPTION)).thenReturn(new StandardsDto.AuditBlancReport(
                ADOPTION, STD, "iso-9001", "ISO 9001:2015", Instant.now(),
                50d, 42, 21, 40, 20, 1, 1, 0, "NON PRÊT — écarts majeurs à corriger",
                List.of(new StandardsDto.AuditFinding(
                        UUID.randomUUID(), "8", "8.7", "8.7", "Maîtrise NC",
                        ObligationLevel.MUST, RiskLevel.CRITICAL, "CRITICAL",
                        "Procédure NC", "Couvrir l'exigence 8.7 ...", 1))));
        when(standards.listEvidence(ADOPTION)).thenReturn(List.of(
                new StandardsDto.EvidenceResponse(
                        UUID.randomUUID(), ADOPTION, UUID.randomUUID(), "4.1",
                        EvidenceType.DOCUMENT, UUID.randomUUID(), null, "note",
                        UUID.randomUUID(), Instant.now())));
    }
}
