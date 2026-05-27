package com.openlab.qualitos.quality.standards;

import com.openlab.qualitos.crypto.application.CryptoSuiteConfig;
import com.openlab.qualitos.crypto.application.HybridSignatureService;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;
import com.openlab.qualitos.crypto.domain.model.SignatureEnvelope;
import com.openlab.qualitos.crypto.domain.port.SignatureProvider;
import com.openlab.qualitos.crypto.infrastructure.BouncyCastleSignatureProvider;
import com.openlab.qualitos.crypto.infrastructure.InMemorySigningKeyProvider;
import com.openlab.qualitos.quality.blockchain.domain.BlockchainAnchorPort;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificationBlancServiceTest {

    @Mock StandardsService standards;
    @Mock BlockchainAnchorPort blockchain;
    CertificationBlancService service;
    HybridSignatureService signer;

    static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-000000000099");
    static final UUID ADOPTION = UUID.randomUUID();
    static final UUID STD = UUID.randomUUID();

    @BeforeEach
    void setup() {
        List<SignatureProvider> providers = List.of(
                new BouncyCastleSignatureProvider(SignatureAlgorithm.ED25519),
                new BouncyCastleSignatureProvider(SignatureAlgorithm.ML_DSA_65));
        signer = new HybridSignatureService(
                new CryptoSuiteConfig(), providers,
                new InMemorySigningKeyProvider("platform-v1",
                        algo -> new BouncyCastleSignatureProvider(algo).generateKeyPair()),
                Clock.systemUTC());
        service = new CertificationBlancService(standards, blockchain, signer);
        TenantContext.setTenantId(TENANT.toString());
        lenient().when(blockchain.submitRoot(eq(TENANT), any())).thenReturn("stub-tx-cb");
        stubAdoption("SMQ siège");
    }

    @AfterEach
    void clr() { TenantContext.clear(); }

    @Test
    void cleanSystem_isCertifiable_withCertificate() {
        // §4-10 totalement couverts, aucun écart.
        stubAlignment(fullSections());
        stubAudit(List.of());

        var r = service.simulate(ADOPTION);

        assertThat(r.decision()).isEqualTo("CERTIFIABLE");
        assertThat(r.majorNonConformities()).isZero();
        assertThat(r.minorNonConformities()).isZero();
        assertThat(r.stage1().passed()).isTrue();
        assertThat(r.stage2().passed()).isTrue();
        assertThat(r.certificate()).isNotNull();
        assertThat(r.certificate().certificateNumber()).contains("QOS-BLANC").contains("ISO-9001");
        assertThat(r.certificate().disclaimer()).contains("sans valeur d'accréditation");
        // 36 mois ≈ 1080 jours de validité.
        assertThat(r.certificate().expiresAt()).isAfter(r.certificate().issuedAt());
    }

    @Test
    void onlyMinorNc_isCertifiableUnderReserve() {
        // Documentation OK ; un MUST risque MOYEN non couvert en §8 → NC mineure.
        stubAlignment(fullSections());
        stubAudit(List.of(finding("8", "8.2", "8.2.1", ObligationLevel.MUST, RiskLevel.MEDIUM)));

        var r = service.simulate(ADOPTION);

        assertThat(r.decision()).isEqualTo("CERTIFIABLE_SOUS_RESERVE");
        assertThat(r.minorNonConformities()).isEqualTo(1);
        assertThat(r.majorNonConformities()).isZero();
        assertThat(r.stage1().passed()).isTrue();
        assertThat(r.stage2().passed()).isTrue();
        assertThat(r.certificate()).isNotNull();
        assertThat(r.certificate().conditions()).contains("3 mois");
        assertThat(r.nonConformities()).singleElement()
                .satisfies(nc -> assertThat(nc.type()).isEqualTo("MINOR"));
    }

    @Test
    void majorNcOnSite_isNotCertifiable_noCertificate() {
        // Documentation OK ; un MUST risque ÉLEVÉ non couvert en §8 → NC majeure terrain.
        stubAlignment(fullSections());
        stubAudit(List.of(finding("8", "8.7", "8.7", ObligationLevel.MUST, RiskLevel.CRITICAL)));

        var r = service.simulate(ADOPTION);

        assertThat(r.decision()).isEqualTo("NON_CERTIFIABLE");
        assertThat(r.majorNonConformities()).isEqualTo(1);
        assertThat(r.stage1().passed()).isTrue();
        assertThat(r.stage2().passed()).isFalse();
        assertThat(r.certificate()).isNull();
    }

    @Test
    void incompleteDocumentation_adjourns_stage2NotConducted() {
        // Couverture §4-7 faible (50 %) → étape 1 non franchie → ajournement.
        List<StandardsDto.SectionAlignment> sections = new ArrayList<>();
        sections.add(section("4", 10, 5));
        sections.add(section("5", 10, 5));
        sections.add(section("6", 10, 5));
        sections.add(section("7", 10, 5));
        sections.add(section("8", 10, 10));
        stubAlignment(sections);
        stubAudit(List.of());

        var r = service.simulate(ADOPTION);

        assertThat(r.decision()).isEqualTo("AJOURNE");
        assertThat(r.stage1().passed()).isFalse();
        assertThat(r.stage2().passed()).isFalse();
        assertThat(r.stage2().summary()).contains("étape 1");
        assertThat(r.certificate()).isNull();
    }

    @Test
    void verdict_isSignedAndAnchored_signatureVerifies() {
        stubAlignment(fullSections());
        stubAudit(List.of());

        var r = service.simulate(ADOPTION);

        assertThat(r.anchorTxRef()).isEqualTo("stub-tx-cb");
        assertThat(r.sha256()).hasSize(64).matches("[0-9a-f]{64}");
        SignatureEnvelope envelope = SignatureEnvelope.decode(r.signature());
        assertThat(envelope.parts()).extracting(SignatureEnvelope.Part::algorithm)
                .containsExactly(SignatureAlgorithm.ED25519, SignatureAlgorithm.ML_DSA_65);
        assertThat(signer.verify(r.sha256().getBytes(StandardCharsets.UTF_8), envelope)).isTrue();
    }

    // ===== stubs =====

    private void stubAdoption(String scope) {
        lenient().when(standards.getAdoption(ADOPTION)).thenReturn(new StandardsDto.AdoptionResponse(
                ADOPTION, TENANT, STD, "iso-9001", "ISO 9001:2015",
                AdoptionStatus.IN_PROGRESS, scope, null, null, "AFNOR Certification",
                null, null, Instant.now(), Instant.now()));
        lenient().when(standards.getStandard(STD)).thenReturn(new StandardsDto.StandardDetail(
                STD, "iso-9001", "ISO 9001:2015 — Systèmes de management de la qualité",
                "ISO", "2015", null, "HLS", "all", "desc", true, 36, null,
                StandardStatus.PUBLISHED, List.of()));
    }

    private void stubAlignment(List<StandardsDto.SectionAlignment> sections) {
        int tot = sections.stream().mapToInt(StandardsDto.SectionAlignment::totalRequirements).sum();
        int cov = sections.stream().mapToInt(StandardsDto.SectionAlignment::coveredRequirements).sum();
        when(standards.computeAlignment(ADOPTION)).thenReturn(new StandardsDto.AlignmentReport(
                ADOPTION, STD, "iso-9001", tot == 0 ? 0 : cov * 100d / tot, tot, cov, tot, cov, sections));
    }

    private void stubAudit(List<StandardsDto.AuditFinding> findings) {
        when(standards.computeAuditBlanc(ADOPTION)).thenReturn(new StandardsDto.AuditBlancReport(
                ADOPTION, STD, "iso-9001", "ISO 9001:2015", Instant.now(),
                80d, 50, 45, 40, 38, 0, 0, 0, "QUASI PRÊT", findings));
    }

    private List<StandardsDto.SectionAlignment> fullSections() {
        List<StandardsDto.SectionAlignment> s = new ArrayList<>();
        for (String code : List.of("4", "5", "6", "7", "8", "9", "10")) {
            s.add(section(code, 5, 5));
        }
        return s;
    }

    private StandardsDto.SectionAlignment section(String code, int total, int covered) {
        return new StandardsDto.SectionAlignment(
                UUID.randomUUID(), code, "Section " + code,
                total == 0 ? 0 : covered * 100d / total, total, covered, List.of());
    }

    private StandardsDto.AuditFinding finding(String section, String clause, String req,
                                              ObligationLevel obligation, RiskLevel risk) {
        String severity = obligation == ObligationLevel.MUST
                && (risk == RiskLevel.HIGH || risk == RiskLevel.CRITICAL) ? "CRITICAL"
                : (obligation == ObligationLevel.MUST ? "MAJOR" : "MINOR");
        return new StandardsDto.AuditFinding(
                UUID.randomUUID(), section, clause, req, "Exigence " + req,
                obligation, risk, severity, "Procédure", "Couvrir " + req, 1);
    }
}
