package com.openlab.qualitos.quality.dashboards.export.application;

import com.openlab.qualitos.crypto.application.CryptoSuiteConfig;
import com.openlab.qualitos.crypto.application.HybridSignatureService;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;
import com.openlab.qualitos.crypto.domain.port.SignatureProvider;
import com.openlab.qualitos.crypto.infrastructure.BouncyCastleSignatureProvider;
import com.openlab.qualitos.crypto.infrastructure.InMemorySigningKeyProvider;
import com.openlab.qualitos.quality.dashboards.export.domain.DashboardExport;
import com.openlab.qualitos.quality.dashboards.export.domain.DashboardExportModel;
import com.openlab.qualitos.quality.dashboards.export.domain.DashboardExportRepository;
import com.openlab.qualitos.quality.dashboards.export.domain.DashboardPdfRenderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardExportServiceTest {

    static final Instant NOW = Instant.parse("2026-06-22T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID DASH = UUID.randomUUID();

    HybridSignatureService signer;
    InMemoryRepo repo;
    RecordingAudit audit;
    StubAnchor anchor;
    ContentRenderer renderer;
    DashboardExportService service;

    @BeforeEach
    void setup() {
        List<SignatureProvider> providers = List.of(
                new BouncyCastleSignatureProvider(SignatureAlgorithm.ED25519),
                new BouncyCastleSignatureProvider(SignatureAlgorithm.ML_DSA_65));
        signer = new HybridSignatureService(
                new CryptoSuiteConfig(), providers,
                new InMemorySigningKeyProvider("platform-v1",
                        algo -> new BouncyCastleSignatureProvider(algo).generateKeyPair()),
                CLOCK);
        repo = new InMemoryRepo();
        audit = new RecordingAudit();
        anchor = new StubAnchor();
        renderer = new ContentRenderer();
        service = new DashboardExportService(
                dashboardId -> "Exec dashboard",
                renderer, signer, anchor, repo, audit,
                code -> "https://app/api/v1/dashboards/public/exports/" + code + "/verify",
                new FixedTenant(), new SecureRandom(), CLOCK);
    }

    @Test
    void export_producesSignedAnchoredAuditedPdf() {
        var result = service.export(DASH, new DashboardExportDto.ExportCommand(List.of(
                new DashboardExportDto.WidgetSnapshot("CAPA", "kpi", List.of("a", "b")))));

        // PDF non-empty, named, integrity metadata present.
        assertThat(result.pdf()).isNotEmpty();
        assertThat(result.fileName()).startsWith("dashboard-exec-dashboard-").endsWith(".pdf");
        assertThat(result.sha256Hex()).matches("^[0-9a-f]{64}$");
        assertThat(result.verificationCode()).matches("^[A-Za-z0-9_-]{16,64}$");
        assertThat(result.anchorTxRef()).isEqualTo("stub-tx-" + result.sha256Hex().substring(0, 8));

        // Persisted + anchored + audited.
        assertThat(repo.byCode).containsKey(result.verificationCode());
        assertThat(anchor.calls).hasSize(1);
        assertThat(audit.calls).hasSize(1);
        assertThat(audit.calls.get(0).tenantId()).isEqualTo(TENANT);
        assertThat(audit.calls.get(0).sha256()).isEqualTo(result.sha256Hex());
    }

    @Test
    void export_signatureRoundTripsAgainstStoredFingerprint() {
        var result = service.export(DASH, new DashboardExportDto.ExportCommand(List.of()));
        var verification = service.verify(result.verificationCode());

        assertThat(verification.valid()).isTrue();
        assertThat(verification.sha256Hex()).isEqualTo(result.sha256Hex());
        assertThat(verification.anchorTxRef()).isEqualTo(result.anchorTxRef());
        assertThat(verification.dashboardName()).isEqualTo("Exec dashboard");
    }

    @Test
    void export_finalSha_matchesFinalRenderedPdfBytes() {
        // The stored/returned SHA must equal the SHA of the actual returned PDF bytes
        // (two-pass render keeps the printed footer consistent with the fingerprint).
        var result = service.export(DASH, new DashboardExportDto.ExportCommand(List.of()));
        assertThat(sha256(result.pdf())).isEqualTo(result.sha256Hex());
    }

    @Test
    void export_specialCharNameAndBlankWidgetTitle_handledGracefully() {
        DashboardExportService svc = new DashboardExportService(
                id -> "***",                       // name with no alnum → "export" stem
                renderer, signer, anchor, repo, audit,
                code -> "u", new FixedTenant(), new SecureRandom(), CLOCK);
        var result = svc.export(DASH, new DashboardExportDto.ExportCommand(List.of(
                new DashboardExportDto.WidgetSnapshot("  ", null, null))));
        assertThat(result.fileName()).startsWith("dashboard-export-");
    }

    @Test
    void export_veryLongName_isTruncatedInFileName() {
        DashboardExportService svc = new DashboardExportService(
                id -> "x".repeat(120),
                renderer, signer, anchor, repo, audit,
                code -> "u", new FixedTenant(), new SecureRandom(), CLOCK);
        var result = svc.export(DASH, new DashboardExportDto.ExportCommand(null));
        // 40-char stem cap.
        assertThat(result.fileName()).contains("dashboard-" + "x".repeat(40) + "-");
    }

    @Test
    void exportCommand_nullWidgets_normalisedToEmpty() {
        var cmd = new DashboardExportDto.ExportCommand(null);
        assertThat(cmd.widgets()).isEmpty();
        var snap = new DashboardExportDto.WidgetSnapshot("t", "kpi", null);
        assertThat(snap.dataLines()).isEmpty();
    }

    @Test
    void verify_unknownCode_returnsInvalidWithoutDetails() {
        var v = service.verify("UNKNOWNcode01234567");
        assertThat(v.valid()).isFalse();
        assertThat(v.sha256Hex()).isNull();
        assertThat(v.anchorTxRef()).isNull();
        assertThat(v.dashboardName()).isNull();
    }

    @Test
    void verify_tamperedFingerprint_returnsInvalid() {
        var result = service.export(DASH, new DashboardExportDto.ExportCommand(List.of()));
        // Tamper the stored fingerprint so the signature no longer matches.
        DashboardExport stored = repo.byCode.get(result.verificationCode());
        DashboardExport tampered = new DashboardExport(
                stored.getId(), stored.getTenantId(), stored.getUserId(), stored.getDashboardId(),
                stored.getDashboardName(), stored.getVerificationCode(),
                "0".repeat(64), stored.getSignatureEnvelope(), stored.getAnchorTxRef(), stored.getCreatedAt());
        repo.byCode.put(result.verificationCode(), tampered);

        assertThat(service.verify(result.verificationCode()).valid()).isFalse();
    }

    @Test
    void verify_corruptEnvelope_returnsInvalidNotThrow() {
        var result = service.export(DASH, new DashboardExportDto.ExportCommand(List.of()));
        DashboardExport stored = repo.byCode.get(result.verificationCode());
        DashboardExport corrupt = new DashboardExport(
                stored.getId(), stored.getTenantId(), stored.getUserId(), stored.getDashboardId(),
                stored.getDashboardName(), stored.getVerificationCode(),
                stored.getSha256Hex(), "!!!not-base64!!!", stored.getAnchorTxRef(), stored.getCreatedAt());
        repo.byCode.put(result.verificationCode(), corrupt);

        assertThat(service.verify(result.verificationCode()).valid()).isFalse();
    }

    @Test
    void export_doesNotReadTenantFromBody_usesProvider() {
        var result = service.export(DASH, new DashboardExportDto.ExportCommand(List.of()));
        assertThat(repo.byCode.get(result.verificationCode()).getTenantId()).isEqualTo(TENANT);
        assertThat(repo.byCode.get(result.verificationCode()).getUserId()).isEqualTo(USER);
    }

    // ---- test doubles ----

    /** Renders deterministic bytes derived from the model content (incl. its SHA). */
    static final class ContentRenderer implements DashboardPdfRenderPort {
        @Override
        public byte[] render(DashboardExportModel model, String verifyUrl) {
            // Deterministic bytes derived from the (stable) model + verify URL. The
            // service computes the SHA over THESE returned bytes, then signs/anchors it.
            String canonical = "%PDF-1.7\n" + model.dashboardName() + "|" + model.tenantId()
                    + "|" + model.widgets().size() + "|code=" + model.verificationCode()
                    + "|url=" + verifyUrl;
            return canonical.getBytes(StandardCharsets.UTF_8);
        }
    }

    final class FixedTenant implements ExportTenantProvider {
        @Override public UUID requireTenantId() { return TENANT; }
        @Override public UUID requireUserId() { return USER; }
    }

    static final class StubAnchor implements DashboardAnchorPort {
        final List<String> calls = new ArrayList<>();
        @Override public String submitRoot(UUID tenantId, String sha256Hex) {
            calls.add(sha256Hex);
            return "stub-tx-" + sha256Hex.substring(0, 8);
        }
    }

    record AuditCall(UUID tenantId, UUID userId, UUID dashboardId, String sha256, String anchorTxRef) {}

    static final class RecordingAudit implements DashboardExportAuditPort {
        final List<AuditCall> calls = new ArrayList<>();
        @Override public void recordExport(UUID tenantId, UUID actorUserId, UUID dashboardId,
                                           String sha256Hex, String anchorTxRef) {
            calls.add(new AuditCall(tenantId, actorUserId, dashboardId, sha256Hex, anchorTxRef));
        }
    }

    static final class InMemoryRepo implements DashboardExportRepository {
        final Map<String, DashboardExport> byCode = new HashMap<>();
        @Override public DashboardExport save(DashboardExport e) {
            if (e.getId() == null) e.assignId(UUID.randomUUID());
            byCode.put(e.getVerificationCode(), e);
            return e;
        }
        @Override public Optional<DashboardExport> findByVerificationCode(String code) {
            return Optional.ofNullable(byCode.get(code));
        }
    }

    static String sha256(byte[] b) {
        try {
            byte[] h = java.security.MessageDigest.getInstance("SHA-256").digest(b);
            StringBuilder sb = new StringBuilder();
            for (byte x : h) sb.append(Character.forDigit((x >> 4) & 0xF, 16))
                               .append(Character.forDigit(x & 0xF, 16));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
