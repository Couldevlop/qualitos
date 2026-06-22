package com.openlab.qualitos.quality.academy.application;

import com.openlab.qualitos.crypto.application.CryptoSuiteConfig;
import com.openlab.qualitos.crypto.application.HybridSignatureService;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;
import com.openlab.qualitos.crypto.domain.port.SignatureProvider;
import com.openlab.qualitos.crypto.infrastructure.BouncyCastleSignatureProvider;
import com.openlab.qualitos.crypto.infrastructure.InMemorySigningKeyProvider;
import com.openlab.qualitos.quality.academy.domain.*;
import com.openlab.qualitos.quality.academy.infrastructure.AcademyCertificateRepository;
import com.openlab.qualitos.quality.blockchain.domain.BlockchainAnchorPort;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademyCertificateServiceTest {

    @Mock AcademyCertificateRepository repo;
    @Mock BlockchainAnchorPort blockchain;
    HybridSignatureService signer;
    AcademyCertificateService service;

    static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    static final UUID USER = UUID.randomUUID();
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-22T08:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        List<SignatureProvider> providers = List.of(
                new BouncyCastleSignatureProvider(SignatureAlgorithm.ED25519),
                new BouncyCastleSignatureProvider(SignatureAlgorithm.ML_DSA_65));
        signer = new HybridSignatureService(new CryptoSuiteConfig(), providers,
                new InMemorySigningKeyProvider("platform-v1",
                        algo -> new BouncyCastleSignatureProvider(algo).generateKeyPair()),
                Clock.systemUTC());
        service = new AcademyCertificateService(repo, signer, blockchain, CLOCK,
                "https://qualitos.io/verify/academy");
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void clear() { TenantContext.clear(); }

    private AcademyCourse course() {
        AcademyCourse c = new AcademyCourse();
        c.setId(UUID.randomUUID());
        c.setTenantId(TENANT);
        c.setCode("iso-9001-basics");
        c.setTitle("ISO 9001 — Bases");
        c.setPassingScore(70);
        return c;
    }

    private AcademyEnrollment enrollment(AcademyCourse c) {
        AcademyEnrollment e = new AcademyEnrollment();
        e.setId(UUID.randomUUID());
        e.setTenantId(TENANT);
        e.setUserId(USER);
        e.setCourseId(c.getId());
        e.setStatus(AcademyEnrollmentStatus.COMPLETED);
        e.setFinalScore(88);
        return e;
    }

    @Test
    void issue_signsAndAnchors_andPersistsCertificate() {
        AcademyCourse c = course();
        AcademyEnrollment e = enrollment(c);
        when(repo.findByTenantIdAndEnrollmentId(TENANT, e.getId())).thenReturn(Optional.empty());
        when(blockchain.submitRoot(eq(TENANT), any())).thenReturn("stub-tx-1");
        when(repo.save(any(AcademyCertificate.class))).thenAnswer(inv -> inv.getArgument(0));

        AcademyDto.CertificateResponse r = service.issue(e, c);

        ArgumentCaptor<AcademyCertificate> cap = ArgumentCaptor.forClass(AcademyCertificate.class);
        verify(repo).save(cap.capture());
        AcademyCertificate saved = cap.getValue();
        assertThat(saved.getSha256()).matches("[0-9a-f]{64}");
        assertThat(saved.getSignature()).isNotBlank();
        assertThat(saved.getAnchorTxRef()).isEqualTo("stub-tx-1");
        assertThat(saved.getUserId()).isEqualTo(USER);
        assertThat(saved.getFinalScore()).isEqualTo(88);

        assertThat(r.code()).isNotBlank();
        assertThat(r.verifyUrl()).endsWith(r.code());
        assertThat(r.htmlContent()).contains("Certificat de complétion").contains("ISO 9001 — Bases");
    }

    @Test
    void issue_isIdempotent_returnsExistingCertificate() {
        AcademyCourse c = course();
        AcademyEnrollment e = enrollment(c);
        AcademyCertificate existing = new AcademyCertificate();
        existing.setId(UUID.randomUUID());
        existing.setTenantId(TENANT);
        existing.setUserId(USER);
        existing.setCourseId(c.getId());
        existing.setEnrollmentId(e.getId());
        existing.setCode("EXISTING-CODE");
        existing.setCourseCode("iso-9001-basics");
        existing.setCourseTitle("ISO 9001 — Bases");
        existing.setFinalScore(88);
        existing.setSha256("a".repeat(64));
        existing.setSignature("sig");
        existing.setIssuedAt(Instant.now(CLOCK));
        when(repo.findByTenantIdAndEnrollmentId(TENANT, e.getId())).thenReturn(Optional.of(existing));

        AcademyDto.CertificateResponse r = service.issue(e, c);

        assertThat(r.code()).isEqualTo("EXISTING-CODE");
        verify(repo, never()).save(any());
        verify(blockchain, never()).submitRoot(any(), any());
    }

    @Test
    void verify_validForGenuineCertificate() {
        // Émet réellement puis vérifie le même certificat.
        AcademyCourse c = course();
        AcademyEnrollment e = enrollment(c);
        when(repo.findByTenantIdAndEnrollmentId(TENANT, e.getId())).thenReturn(Optional.empty());
        when(blockchain.submitRoot(eq(TENANT), any())).thenReturn("stub-tx");
        ArgumentCaptor<AcademyCertificate> cap = ArgumentCaptor.forClass(AcademyCertificate.class);
        when(repo.save(any(AcademyCertificate.class))).thenAnswer(inv -> inv.getArgument(0));
        service.issue(e, c);
        verify(repo).save(cap.capture());
        AcademyCertificate minted = cap.getValue();

        when(repo.findByCode(minted.getCode())).thenReturn(Optional.of(minted));
        AcademyDto.CertificateVerification v = service.verify(minted.getCode());

        assertThat(v.signatureValid()).isTrue();
        assertThat(v.valid()).isTrue();
        assertThat(v.courseCode()).isEqualTo("iso-9001-basics");
        assertThat(v.finalScore()).isEqualTo(88);
    }

    @Test
    void verify_detectsTamperedSha256() {
        AcademyCourse c = course();
        AcademyEnrollment e = enrollment(c);
        when(repo.findByTenantIdAndEnrollmentId(TENANT, e.getId())).thenReturn(Optional.empty());
        when(blockchain.submitRoot(eq(TENANT), any())).thenReturn("stub-tx");
        ArgumentCaptor<AcademyCertificate> cap = ArgumentCaptor.forClass(AcademyCertificate.class);
        when(repo.save(any(AcademyCertificate.class))).thenAnswer(inv -> inv.getArgument(0));
        service.issue(e, c);
        verify(repo).save(cap.capture());
        AcademyCertificate minted = cap.getValue();
        // Altère l'empreinte : la signature ne valide plus.
        minted.setSha256("0".repeat(64));

        when(repo.findByCode(minted.getCode())).thenReturn(Optional.of(minted));
        AcademyDto.CertificateVerification v = service.verify(minted.getCode());

        assertThat(v.signatureValid()).isFalse();
        assertThat(v.valid()).isFalse();
    }

    @Test
    void verify_expiredCertificate_isInvalidEvenIfSignatureValid() {
        AcademyCourse c = course();
        AcademyEnrollment e = enrollment(c);
        e.setExpiresOn(LocalDate.of(2020, 1, 1)); // expiré
        when(repo.findByTenantIdAndEnrollmentId(TENANT, e.getId())).thenReturn(Optional.empty());
        when(blockchain.submitRoot(eq(TENANT), any())).thenReturn("stub-tx");
        ArgumentCaptor<AcademyCertificate> cap = ArgumentCaptor.forClass(AcademyCertificate.class);
        when(repo.save(any(AcademyCertificate.class))).thenAnswer(inv -> inv.getArgument(0));
        service.issue(e, c);
        verify(repo).save(cap.capture());
        AcademyCertificate minted = cap.getValue();

        when(repo.findByCode(minted.getCode())).thenReturn(Optional.of(minted));
        AcademyDto.CertificateVerification v = service.verify(minted.getCode());

        assertThat(v.signatureValid()).isTrue();
        assertThat(v.valid()).isFalse(); // expiré
    }

    @Test
    void verify_unknownCode_throwsNotFound() {
        when(repo.findByCode("nope")).thenReturn(Optional.empty());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.verify("nope"))
                .isInstanceOf(AcademyNotFoundException.class);
    }

    @Test
    void renderHtml_escapesXss() {
        String html = service.renderHtml("code1", "c<script>", "<b>Titre</b>", USER, 90,
                Instant.now(CLOCK), null);
        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;b&gt;Titre&lt;/b&gt;");
    }
}
