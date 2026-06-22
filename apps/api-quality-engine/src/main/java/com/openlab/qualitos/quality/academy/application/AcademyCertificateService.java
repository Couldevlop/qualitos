package com.openlab.qualitos.quality.academy.application;

import com.openlab.qualitos.crypto.application.HybridSignatureService;
import com.openlab.qualitos.crypto.domain.model.SignatureEnvelope;
import com.openlab.qualitos.quality.academy.domain.*;
import com.openlab.qualitos.quality.academy.infrastructure.AcademyCertificateRepository;
import com.openlab.qualitos.quality.blockchain.domain.BlockchainAnchorPort;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Émission et vérification des certificats de complétion (§19.3).
 *
 * <p>RÉUTILISE l'infra crypto/ancrage du Standards Hub (aucune crypto
 * réimplémentée) : rend un certificat HTML auto-contenu, calcule son SHA-256, le
 * signe via {@link HybridSignatureService} (Ed25519 + ML-DSA-65, contexte
 * {@code "certificate"}) et ancre l'empreinte via {@link BlockchainAnchorPort}.</p>
 *
 * <p>La vérification publique par code QR ne filtre PAS par tenant — le code
 * <b>est</b> l'autorité (UUID non devinable). Elle recontrôle la signature
 * hybride contre la clé plateforme épinglée : un certificat falsifié échoue.</p>
 */
@Service
public class AcademyCertificateService {

    /** Contexte de suite crypto (registré dans CryptoSuiteConfig → Ed25519 + ML-DSA-65). */
    static final String SIGN_CONTEXT = "certificate";

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneOffset.UTC);

    private final AcademyCertificateRepository certificates;
    private final HybridSignatureService signer;
    private final BlockchainAnchorPort blockchain;
    private final Clock clock;
    private final String publicBaseUrl;

    @org.springframework.beans.factory.annotation.Autowired
    public AcademyCertificateService(AcademyCertificateRepository certificates,
                                     HybridSignatureService signer,
                                     BlockchainAnchorPort blockchain,
                                     org.springframework.core.env.Environment env) {
        this(certificates, signer, blockchain, Clock.systemUTC(),
                env.getProperty("qualitos.academy.public-base-url",
                        "https://qualitos.io/verify/academy"));
    }

    AcademyCertificateService(AcademyCertificateRepository certificates,
                              HybridSignatureService signer,
                              BlockchainAnchorPort blockchain,
                              Clock clock,
                              String publicBaseUrl) {
        this.certificates = certificates;
        this.signer = signer;
        this.blockchain = blockchain;
        this.clock = clock;
        this.publicBaseUrl = publicBaseUrl;
    }

    /**
     * Émet (ou réutilise) le certificat d'une inscription complétée. Idempotent :
     * un second appel renvoie le certificat existant (clé unique sur enrollment).
     */
    @Transactional
    public AcademyDto.CertificateResponse issue(AcademyEnrollment enrollment, AcademyCourse course) {
        UUID tenantId = requireTenantId();
        return certificates.findByTenantIdAndEnrollmentId(tenantId, enrollment.getId())
                .map(this::toResponseWithHtml)
                .orElseGet(() -> toResponseWithHtml(mint(tenantId, enrollment, course)));
    }

    private AcademyCertificate mint(UUID tenantId, AcademyEnrollment enrollment, AcademyCourse course) {
        Instant now = Instant.now(clock);
        String code = UUID.randomUUID().toString();
        int score = enrollment.getFinalScore() == null ? 0 : enrollment.getFinalScore();

        String html = renderHtml(code, course, enrollment.getUserId(), score, now, enrollment.getExpiresOn());
        String sha256 = sha256(html);

        // Signature hybride (Ed25519 + ML-DSA-65) du SHA-256 — preuve d'intégrité
        // résistante au quantique, vérifiable a posteriori (réutilise l'infra crypto).
        SignatureEnvelope envelope = signer.sign(SIGN_CONTEXT, sha256.getBytes(StandardCharsets.UTF_8));

        // Ancrage de l'empreinte (stub en dev/test, Phase A/Fabric en prod).
        String anchorTxRef = blockchain.submitRoot(tenantId, sha256);

        AcademyCertificate cert = new AcademyCertificate();
        cert.setTenantId(tenantId);
        cert.setUserId(enrollment.getUserId());
        cert.setCourseId(course.getId());
        cert.setEnrollmentId(enrollment.getId());
        cert.setCode(code);
        cert.setCourseCode(course.getCode());
        cert.setCourseTitle(course.getTitle());
        cert.setFinalScore(score);
        cert.setSha256(sha256);
        cert.setSignature(envelope.encode());
        cert.setAnchorTxRef(anchorTxRef);
        cert.setIssuedAt(now);
        cert.setExpiresOn(enrollment.getExpiresOn());
        return certificates.save(cert);
    }

    /** Relit le certificat d'une inscription (tenant courant). 404 si absent. */
    @Transactional(readOnly = true)
    public AcademyDto.CertificateResponse getByEnrollment(UUID enrollmentId) {
        UUID tenantId = requireTenantId();
        AcademyCertificate cert = certificates.findByTenantIdAndEnrollmentId(tenantId, enrollmentId)
                .orElseThrow(() -> new AcademyNotFoundException("Certificate for enrollment", enrollmentId));
        return toResponseWithHtml(cert);
    }

    /**
     * Vérification publique par code (QR). Pas de filtre tenant. Revalide la
     * signature hybride : statut {@code signatureValid=false} si falsifiée.
     */
    @Transactional(readOnly = true)
    public AcademyDto.CertificateVerification verify(String code) {
        AcademyCertificate cert = certificates.findByCode(code)
                .orElseThrow(() -> new AcademyNotFoundException("Certificate", code));
        boolean signatureValid;
        try {
            SignatureEnvelope envelope = SignatureEnvelope.decode(cert.getSignature());
            signatureValid = signer.verify(cert.getSha256().getBytes(StandardCharsets.UTF_8), envelope);
        } catch (RuntimeException ex) {
            signatureValid = false;
        }
        LocalDate today = LocalDate.now(clock);
        boolean notExpired = cert.getExpiresOn() == null || !cert.getExpiresOn().isBefore(today);
        boolean valid = signatureValid && notExpired;
        return new AcademyDto.CertificateVerification(
                cert.getCode(), valid, cert.getCourseCode(), cert.getCourseTitle(),
                cert.getFinalScore(), cert.getIssuedAt(), cert.getExpiresOn(),
                cert.getSha256(), cert.getAnchorTxRef(), signatureValid);
    }

    private AcademyDto.CertificateResponse toResponseWithHtml(AcademyCertificate cert) {
        // Le HTML est re-rendu de façon déterministe à partir des données persistées,
        // garantissant que le même SHA-256 est reproductible pour la vérification.
        String html = renderHtml(cert.getCode(),
                cert.getCourseCode(), cert.getCourseTitle(), cert.getUserId(),
                cert.getFinalScore(), cert.getIssuedAt(), cert.getExpiresOn());
        return new AcademyDto.CertificateResponse(
                cert.getId(), cert.getEnrollmentId(), cert.getCourseId(), cert.getCode(),
                cert.getCourseCode(), cert.getCourseTitle(), cert.getFinalScore(),
                cert.getSha256(), cert.getAnchorTxRef(), cert.getIssuedAt(), cert.getExpiresOn(),
                publicBaseUrl + "/" + cert.getCode(), html);
    }

    private String renderHtml(String code, AcademyCourse course, UUID userId,
                              int score, Instant issuedAt, LocalDate expiresOn) {
        return renderHtml(code, course.getCode(), course.getTitle(), userId, score, issuedAt, expiresOn);
    }

    /**
     * Rend un certificat HTML auto-contenu (imprimable → PDF côté client). Tout
     * contenu dynamique est échappé (OWASP A03 — anti-XSS stocké).
     */
    String renderHtml(String code, String courseCode, String courseTitle, UUID userId,
                      int score, Instant issuedAt, LocalDate expiresOn) {
        String verifyUrl = publicBaseUrl + "/" + code;
        StringBuilder b = new StringBuilder(2048);
        b.append("<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\">");
        b.append("<title>Certificat — ").append(esc(courseTitle)).append("</title>");
        b.append("<style>")
         .append("body{font-family:Inter,Arial,sans-serif;color:#1a1a2e;margin:0;padding:48px;")
         .append("background:#fcfcfd}")
         .append(".cert{max-width:820px;margin:0 auto;border:2px solid #2563d6;border-radius:18px;")
         .append("padding:48px 56px;background:#fff;box-shadow:0 6px 30px rgba(37,99,214,.08)}")
         .append("h1{font-size:30px;color:#2563d6;margin:0 0 4px;letter-spacing:.5px}")
         .append(".sub{color:#6b7382;font-size:14px;margin-bottom:28px}")
         .append(".name{font-size:22px;font-weight:700;margin:18px 0 6px}")
         .append(".course{font-size:18px;margin:6px 0 18px}")
         .append(".meta{font-size:13px;color:#4b5563;line-height:1.7}")
         .append(".score{display:inline-block;font-size:22px;color:#047857;font-weight:700}")
         .append(".footer{margin-top:34px;padding-top:18px;border-top:1px solid #e3e6eb;")
         .append("font-size:11px;color:#888;word-break:break-all}")
         .append("</style></head><body><div class=\"cert\">");
        b.append("<h1>Certificat de complétion</h1>");
        b.append("<div class=\"sub\">QualitOS Academy — §19.3</div>");
        b.append("<div class=\"meta\">Décerné à l'apprenant</div>");
        b.append("<div class=\"name\">").append(esc(userId.toString())).append("</div>");
        b.append("<div class=\"meta\">pour la complétion réussie du cours</div>");
        b.append("<div class=\"course\"><b>").append(esc(courseTitle)).append("</b> (")
         .append(esc(courseCode)).append(")</div>");
        b.append("<div class=\"meta\">Score final : <span class=\"score\">").append(score)
         .append(" %</span><br>");
        b.append("Délivré le ").append(esc(DATE_FMT.format(issuedAt)));
        if (expiresOn != null) {
            b.append(" — valide jusqu'au ").append(esc(expiresOn.toString()));
        }
        b.append("</div>");
        b.append("<div class=\"footer\">")
         .append("Code de vérification : <b>").append(esc(code)).append("</b><br>")
         .append("Vérifiable en ligne : ").append(esc(verifyUrl)).append("<br>")
         .append("Certificat signé (Ed25519 + ML-DSA-65) et ancré blockchain — QualitOS.")
         .append("</div>");
        b.append("</div></body></html>");
        return b.toString();
    }

    private String esc(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#x27;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    private String sha256(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte x : hash) hex.append(String.format("%02x", x));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
