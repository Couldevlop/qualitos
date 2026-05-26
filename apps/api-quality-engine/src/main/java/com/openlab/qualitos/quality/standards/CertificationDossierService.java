package com.openlab.qualitos.quality.standards;

import com.openlab.qualitos.crypto.application.HybridSignatureService;
import com.openlab.qualitos.crypto.domain.model.SignatureEnvelope;
import com.openlab.qualitos.quality.blockchain.domain.BlockchainAnchorPort;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Génère le dossier de certification de bout en bout (CLAUDE.md §8.4 onglet 6) :
 * assemble exigences, roadmap, alignement, audit blanc et preuves en un document
 * HTML imprimable (→ PDF), calcule son empreinte SHA-256 et l'ancre via le port
 * blockchain (preuve d'intégrité audit-ready, §8.7).
 *
 * Clean architecture : ce service d'application orchestre {@link StandardsService}
 * (cas d'usage métier) et dépend du port {@link BlockchainAnchorPort} (abstraction,
 * implémentée par un adapter — stub en dev, Hyperledger en prod). Aucune logique
 * d'infrastructure ni de rendu n'est portée par le domaine.
 *
 * OWASP : tout contenu dynamique injecté dans le HTML est échappé ({@link #esc})
 * pour prévenir le XSS stocké (A03). L'isolation tenant est garantie par
 * {@link StandardsService} (chargement filtré par tenant issu du JWT).
 */
@Service
public class CertificationDossierService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ISO_INSTANT;

    /** Contexte de suite crypto pour les artefacts de preuve signés (ADR 0011). */
    private static final String SIGN_CONTEXT = "audit-report";

    private final StandardsService standards;
    private final BlockchainAnchorPort blockchain;
    private final HybridSignatureService signer;

    public CertificationDossierService(StandardsService standards,
                                       BlockchainAnchorPort blockchain,
                                       HybridSignatureService signer) {
        this.standards = standards;
        this.blockchain = blockchain;
        this.signer = signer;
    }

    @Transactional(readOnly = true)
    public StandardsDto.DossierResponse generate(UUID adoptionId) {
        StandardsDto.AdoptionResponse adoption = standards.getAdoption(adoptionId);
        StandardsDto.StandardDetail standard = standards.getStandard(adoption.standardId());
        StandardsDto.RoadmapSummary roadmap = standards.getRoadmap(adoptionId);
        StandardsDto.AlignmentReport alignment = standards.computeAlignment(adoptionId);
        StandardsDto.AuditBlancReport audit = standards.computeAuditBlanc(adoptionId);
        List<StandardsDto.EvidenceResponse> evidence = standards.listEvidence(adoptionId);

        Instant now = Instant.now();
        String html = renderHtml(adoption, standard, roadmap, alignment, audit, evidence, now);
        String sha256 = sha256(html);

        // Signature hybride (Ed25519 + ML-DSA-65) du SHA-256 du dossier : preuve
        // d'intégrité signée, résistante au quantique, vérifiable a posteriori (ADR 0011).
        SignatureEnvelope envelope = signer.sign(SIGN_CONTEXT, sha256.getBytes(StandardCharsets.UTF_8));
        String signature = envelope.encode();

        // Ancrage de l'empreinte (preuve d'intégrité). En dev, adapter stub.
        UUID tenantId = UUID.fromString(TenantContext.getTenantId());
        String anchorTxRef = blockchain.submitRoot(tenantId, sha256);

        String fileName = "dossier-certification-" + standard.code() + "-"
                + now.toString().replace(":", "").substring(0, 15) + ".html";

        return new StandardsDto.DossierResponse(
                adoptionId, standard.code(), standard.fullName(), now,
                sha256, anchorTxRef, fileName, "text/html",
                audit.readinessScore(), roadmap.completionPercent(),
                evidence.size(), html, signature);
    }

    // ===== rendu HTML (auto-contenu, imprimable) =====

    private String renderHtml(StandardsDto.AdoptionResponse adoption,
                              StandardsDto.StandardDetail standard,
                              StandardsDto.RoadmapSummary roadmap,
                              StandardsDto.AlignmentReport alignment,
                              StandardsDto.AuditBlancReport audit,
                              List<StandardsDto.EvidenceResponse> evidence,
                              Instant generatedAt) {
        StringBuilder b = new StringBuilder(8192);
        b.append("<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\">");
        b.append("<title>Dossier de certification — ").append(esc(standard.code())).append("</title>");
        b.append("<style>")
         .append("body{font-family:Inter,Arial,sans-serif;color:#1a1a2e;margin:40px;line-height:1.5}")
         .append("h1{font-size:26px;border-bottom:3px solid #3949ab;padding-bottom:8px}")
         .append("h2{font-size:18px;color:#3949ab;margin-top:32px;border-bottom:1px solid #ddd;padding-bottom:4px}")
         .append("table{border-collapse:collapse;width:100%;margin:12px 0;font-size:13px}")
         .append("th,td{border:1px solid #ccc;padding:6px 8px;text-align:left;vertical-align:top}")
         .append("th{background:#eef1fb}")
         .append(".kpi{display:inline-block;margin:6px 18px 6px 0;font-size:14px}")
         .append(".kpi b{font-size:20px;color:#3949ab;display:block}")
         .append(".crit{color:#b71c1c;font-weight:bold}.maj{color:#e65100}.min{color:#666}")
         .append(".done{color:#2e7d32}.muted{color:#888}")
         .append(".footer{margin-top:40px;padding:14px;background:#f5f5f7;border-radius:6px;font-size:12px;word-break:break-all}")
         .append("</style></head><body>");

        // Cover
        b.append("<h1>Dossier de certification</h1>");
        b.append("<p><b>").append(esc(standard.fullName())).append("</b><br>");
        b.append("Éditeur : ").append(esc(nz(standard.publisher()))).append(" — Version ")
         .append(esc(nz(standard.currentVersion()))).append("</p>");
        b.append("<table>")
         .append(row("Périmètre", nz(adoption.scopeDescription())))
         .append(row("Statut de l'adoption", String.valueOf(adoption.status())))
         .append(row("Organisme certificateur", nz(adoption.certificationBody())))
         .append(row("Date cible de certification",
                 adoption.targetCertificationDate() == null ? "—" : adoption.targetCertificationDate().toString()))
         .append(row("Généré le", TS.format(generatedAt)))
         .append("</table>");

        // KPIs synthèse
        b.append("<h2>Synthèse</h2>");
        b.append("<div class=\"kpi\">Préparation (MUST)<b>")
         .append(pct(audit.readinessScore())).append("</b></div>");
        b.append("<div class=\"kpi\">Alignement global<b>")
         .append(pct(alignment.overallScore())).append("</b></div>");
        b.append("<div class=\"kpi\">Roadmap<b>")
         .append(pct(roadmap.completionPercent())).append("</b></div>");
        b.append("<div class=\"kpi\">Écarts critiques<b class=\"crit\">")
         .append(audit.criticalGaps()).append("</b></div>");
        b.append("<div class=\"kpi\">Preuves liées<b>").append(evidence.size()).append("</b></div>");
        b.append("<p><b>Verdict audit blanc :</b> ").append(esc(audit.verdict())).append("</p>");

        // Roadmap
        b.append("<h2>Roadmap de certification (").append(roadmap.totalStages()).append(" étapes)</h2>");
        b.append("<table><tr><th>#</th><th>Étape</th><th>Durée</th><th>Responsable</th><th>Statut</th></tr>");
        for (StandardsDto.RoadmapStageResponse s : roadmap.stages()) {
            b.append("<tr><td>").append(s.stepNumber()).append("</td><td>").append(esc(s.name()))
             .append("</td><td>").append(esc(nz(s.typicalDuration()))).append("</td><td>")
             .append(esc(nz(s.responsibleRole()))).append("</td><td>")
             .append(statusBadge(String.valueOf(s.status()))).append("</td></tr>");
        }
        b.append("</table>");

        // Alignement par section
        b.append("<h2>Alignement par section</h2>");
        b.append("<table><tr><th>Section</th><th>Couverture</th><th>Exigences couvertes</th></tr>");
        for (StandardsDto.SectionAlignment s : alignment.sections()) {
            b.append("<tr><td>§").append(esc(s.sectionCode())).append(" ").append(esc(s.sectionTitle()))
             .append("</td><td>").append(pct(s.score())).append("</td><td>")
             .append(s.coveredRequirements()).append(" / ").append(s.totalRequirements())
             .append("</td></tr>");
        }
        b.append("</table>");

        // Audit blanc — écarts
        b.append("<h2>Audit blanc — écarts &amp; plan de remédiation (")
         .append(audit.findings().size()).append(")</h2>");
        if (audit.findings().isEmpty()) {
            b.append("<p class=\"done\">Aucun écart : toutes les exigences sont couvertes par une preuve.</p>");
        } else {
            b.append("<table><tr><th>Exigence</th><th>Sévérité</th><th>Action de remédiation</th></tr>");
            for (StandardsDto.AuditFinding f : audit.findings()) {
                b.append("<tr><td>").append(esc(f.requirementCode())).append("</td><td class=\"")
                 .append(sevClass(f.findingSeverity())).append("\">").append(esc(f.findingSeverity()))
                 .append("</td><td>").append(esc(f.remediationAction())).append("</td></tr>");
            }
            b.append("</table>");
        }

        // Preuves
        b.append("<h2>Preuves liées (").append(evidence.size()).append(")</h2>");
        if (evidence.isEmpty()) {
            b.append("<p class=\"muted\">Aucune preuve liée pour l'instant.</p>");
        } else {
            b.append("<table><tr><th>Exigence</th><th>Type</th><th>Référence</th></tr>");
            for (StandardsDto.EvidenceResponse e : evidence) {
                b.append("<tr><td>").append(esc(nz(e.requirementCode()))).append("</td><td>")
                 .append(esc(String.valueOf(e.evidenceType()))).append("</td><td>")
                 .append(esc(e.evidenceUri() != null ? e.evidenceUri()
                         : String.valueOf(e.evidenceRefId()))).append("</td></tr>");
            }
            b.append("</table>");
        }

        // Intégrité
        b.append("<div class=\"footer\"><b>Intégrité du dossier</b><br>")
         .append("L'empreinte SHA-256 de ce document et sa référence d'ancrage blockchain ")
         .append("sont fournies dans les métadonnées de génération (vérifiables a posteriori).<br>")
         .append("Document généré par QualitOS — Standards Hub.</div>");
        b.append("</body></html>");
        return b.toString();
    }

    private String row(String k, String v) {
        return "<tr><th style=\"width:240px\">" + esc(k) + "</th><td>" + esc(v) + "</td></tr>";
    }

    private String statusBadge(String status) {
        String cls = "DONE".equals(status) ? "done" : ("SKIPPED".equals(status) ? "muted" : "");
        return "<span class=\"" + cls + "\">" + esc(status) + "</span>";
    }

    private String sevClass(String sev) {
        return switch (sev) {
            case "CRITICAL" -> "crit";
            case "MAJOR" -> "maj";
            default -> "min";
        };
    }

    private String pct(double v) {
        return Math.round(v) + " %";
    }

    private String nz(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    /** Échappement HTML — prévention XSS stocké (OWASP A03). */
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
}
