package com.openlab.qualitos.quality.dashboards.export.application;

import com.openlab.qualitos.crypto.application.HybridSignatureService;
import com.openlab.qualitos.crypto.domain.model.SignatureEnvelope;
import com.openlab.qualitos.quality.dashboards.export.domain.DashboardExport;
import com.openlab.qualitos.quality.dashboards.export.domain.DashboardExportModel;
import com.openlab.qualitos.quality.dashboards.export.domain.DashboardExportRepository;
import com.openlab.qualitos.quality.dashboards.export.domain.DashboardPdfRenderPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Use case — produce an official, signed &amp; anchored PDF export of a dashboard
 * (CLAUDE.md §7.3 "Export PDF signé blockchain" / §7.4 "PDF avec signature
 * ML-DSA + QR code blockchain"), and verify it publicly.
 *
 * <p>Orchestration (no framework / no PDF lib here — ports only, hexagonal arch):
 * <ol>
 *   <li>load the dashboard name (tenant-scoped, visibility enforced);</li>
 *   <li>mint a verification code and build the public verify URL;</li>
 *   <li>render the PDF (with embedded QR) via {@link DashboardPdfRenderPort};</li>
 *   <li>compute SHA-256 of the PDF bytes;</li>
 *   <li>sign the fingerprint with the hybrid Ed25519+ML-DSA-65 envelope (ADR 0011);</li>
 *   <li>anchor the fingerprint on the blockchain (port);</li>
 *   <li>persist the receipt and journal the auditable action (§18.2 #5).</li>
 * </ol>
 *
 * <p>Multi-tenant: tenant + user come from the JWT via {@link ExportTenantProvider}
 * — never the body (§18.2 #2). The public verify path takes no tenant context: the
 * opaque code is the authority, and it returns only integrity facts (OWASP A01).
 */
public class DashboardExportService {

    /** Crypto suite context (ADR 0011) — reuses the signed audit-artifact suite. */
    private static final String SIGN_CONTEXT = "audit-report";
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(java.time.ZoneOffset.UTC);

    private final DashboardLayoutLoaderPort layoutLoader;
    private final DashboardPdfRenderPort renderer;
    private final HybridSignatureService signer;
    private final DashboardAnchorPort anchor;
    private final DashboardExportRepository repository;
    private final DashboardExportAuditPort audit;
    private final VerifyUrlBuilder verifyUrlBuilder;
    private final ExportTenantProvider tenantProvider;
    private final SecureRandom random;
    private final Clock clock;

    public DashboardExportService(DashboardLayoutLoaderPort layoutLoader,
                                  DashboardPdfRenderPort renderer,
                                  HybridSignatureService signer,
                                  DashboardAnchorPort anchor,
                                  DashboardExportRepository repository,
                                  DashboardExportAuditPort audit,
                                  VerifyUrlBuilder verifyUrlBuilder,
                                  ExportTenantProvider tenantProvider,
                                  SecureRandom random,
                                  Clock clock) {
        this.layoutLoader = layoutLoader;
        this.renderer = renderer;
        this.signer = signer;
        this.anchor = anchor;
        this.repository = repository;
        this.audit = audit;
        this.verifyUrlBuilder = verifyUrlBuilder;
        this.tenantProvider = tenantProvider;
        this.random = random;
        this.clock = clock;
    }

    /** Produce a signed, anchored PDF export of {@code dashboardId} for the current tenant. */
    public DashboardExportDto.ExportResult export(UUID dashboardId,
                                                  DashboardExportDto.ExportCommand command) {
        UUID tenantId = tenantProvider.requireTenantId();
        UUID userId = tenantProvider.requireUserId();

        String dashboardName = layoutLoader.requireVisibleName(dashboardId);
        Instant now = Instant.now(clock);
        String code = newVerificationCode();
        String verifyUrl = verifyUrlBuilder.verifyUrl(code);

        DashboardExportModel model = new DashboardExportModel(
                dashboardName, tenantId.toString(), dashboardId.toString(), now,
                toModelWidgets(command), code);

        // Single-pass render: the PDF carries the stable verification code + QR but
        // NOT its own fingerprint, so the SHA-256 of the returned bytes is exactly
        // what we sign, anchor and store (round-trip consistent by construction).
        byte[] pdf = renderer.render(model, verifyUrl);
        String sha256 = sha256(pdf);

        // Sign the canonical fingerprint (hybrid Ed25519 + ML-DSA-65).
        SignatureEnvelope envelope = signer.sign(SIGN_CONTEXT, sha256.getBytes(StandardCharsets.UTF_8));
        String encodedEnvelope = envelope.encode();

        // Anchor the fingerprint (stub in dev, Hyperledger Fabric in prod).
        String anchorTxRef = anchor.submitRoot(tenantId, sha256);

        DashboardExport export = DashboardExport.create(
                tenantId, userId, dashboardId, dashboardName, code,
                sha256, encodedEnvelope, anchorTxRef, now);
        repository.save(export);

        // §18.2 #5 — an official signed export is an auditable action.
        audit.recordExport(tenantId, userId, dashboardId, sha256, anchorTxRef);

        String fileName = "dashboard-" + safeStem(dashboardName) + "-" + STAMP.format(now) + ".pdf";
        return new DashboardExportDto.ExportResult(
                pdf, fileName, code, sha256, anchorTxRef, now);
    }

    /**
     * Public verification: re-validate the stored signature over the stored
     * fingerprint. Unknown code → {@code valid=false} with no details.
     */
    public DashboardExportDto.VerificationResult verify(String code) {
        Optional<DashboardExport> found = repository.findByVerificationCode(code);
        if (found.isEmpty()) {
            return DashboardExportDto.VerificationResult.unknown(code);
        }
        DashboardExport export = found.get();
        boolean valid;
        try {
            SignatureEnvelope envelope = SignatureEnvelope.decode(export.getSignatureEnvelope());
            valid = signer.verify(export.getSha256Hex().getBytes(StandardCharsets.UTF_8), envelope);
        } catch (RuntimeException ex) {
            valid = false; // tampered / undecodable envelope → not valid
        }
        return new DashboardExportDto.VerificationResult(
                valid, code, export.getSha256Hex(), export.getAnchorTxRef(),
                export.getDashboardName(), export.getCreatedAt());
    }

    // ---- helpers ----

    private List<DashboardExportModel.Widget> toModelWidgets(DashboardExportDto.ExportCommand command) {
        List<DashboardExportModel.Widget> out = new ArrayList<>();
        for (DashboardExportDto.WidgetSnapshot s : command.widgets()) {
            out.add(new DashboardExportModel.Widget(
                    s.title() == null || s.title().isBlank() ? "(sans titre)" : s.title(),
                    s.type() == null ? "" : s.type(),
                    s.dataLines()));
        }
        return out;
    }

    /** 24-byte (32-char Base64URL) cryptographically random verification code. */
    private String newVerificationCode() {
        byte[] buf = new byte[24];
        random.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String safeStem(String name) {
        String s = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        if (s.length() > 40) s = s.substring(0, 40);
        return s.isBlank() ? "export" : s;
    }

    private static String sha256(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content);
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) hex.append(Character.forDigit((b >> 4) & 0xF, 16))
                                   .append(Character.forDigit(b & 0xF, 16));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
