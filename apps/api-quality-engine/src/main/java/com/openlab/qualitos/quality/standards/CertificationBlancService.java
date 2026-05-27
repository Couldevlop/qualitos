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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Simulation de l'audit de certification — « certification à blanc » (CLAUDE.md
 * §8.5 étapes 14-15). Distincte de l'audit blanc ({@link StandardsService#computeAuditBlanc}) :
 * là où l'audit blanc mesure la <i>préparation</i> (couverture des exigences),
 * la certification à blanc reproduit la <b>décision</b> d'un organisme certificateur
 * accrédité ISO/IEC 17021-1 :
 *
 * <ol>
 *   <li><b>Étape 1 — revue documentaire</b> (§4-7) : le système de management est-il
 *       établi et documenté ? Si non, l'audit est ajourné (l'étape 2 n'a pas lieu).</li>
 *   <li><b>Étape 2 — audit terrain</b> (§8-10) : le système est-il mis en œuvre et
 *       efficace ? Relève les non-conformités.</li>
 * </ol>
 *
 * Les exigences non couvertes sont classées en <b>NC majeure</b> (exigence MUST à
 * risque élevé non satisfaite — doute significatif), <b>NC mineure</b> (MUST à risque
 * faible/moyen, défaillance isolée) ou <b>observation</b> (SHOULD/MAY). La règle de
 * décision (ISO/IEC 17021-1 §9.4) : pas de certification tant qu'une NC majeure
 * subsiste ; certification possible sous réserve de lever les NC mineures sous 3 mois.
 *
 * <p>Clean architecture : ce service d'application orchestre {@link StandardsService}
 * et dépend des ports {@link BlockchainAnchorPort} / {@link HybridSignatureService}.
 * Le verdict est haché (SHA-256), signé (hybride Ed25519+ML-DSA-65, ADR 0011) et ancré
 * pour en faire une preuve vérifiable a posteriori (audit-ready, §8.7).
 *
 * <p>Le certificat émis porte un avertissement : <i>simulation, sans valeur
 * d'accréditation</i> (§22.5 : seul un audit réel signé MFA + ancré fait foi).
 */
@Service
public class CertificationBlancService {

    /** Sections HLS relevant de la revue documentaire (système conçu/documenté). */
    private static final Set<String> STAGE1_SECTIONS = Set.of("4", "5", "6", "7");

    /** Contexte de suite crypto pour les artefacts de preuve signés (ADR 0011). */
    private static final String SIGN_CONTEXT = "audit-report";

    private static final String CERTIFIABLE = "CERTIFIABLE";
    private static final String CERTIFIABLE_SOUS_RESERVE = "CERTIFIABLE_SOUS_RESERVE";
    private static final String NON_CERTIFIABLE = "NON_CERTIFIABLE";
    private static final String AJOURNE = "AJOURNE";

    private final StandardsService standards;
    private final BlockchainAnchorPort blockchain;
    private final HybridSignatureService signer;

    public CertificationBlancService(StandardsService standards,
                                     BlockchainAnchorPort blockchain,
                                     HybridSignatureService signer) {
        this.standards = standards;
        this.blockchain = blockchain;
        this.signer = signer;
    }

    @Transactional(readOnly = true)
    public StandardsDto.CertificationBlancReport simulate(UUID adoptionId) {
        StandardsDto.AdoptionResponse adoption = standards.getAdoption(adoptionId);
        StandardsDto.StandardDetail standard = standards.getStandard(adoption.standardId());
        StandardsDto.AlignmentReport alignment = standards.computeAlignment(adoptionId);
        StandardsDto.AuditBlancReport audit = standards.computeAuditBlanc(adoptionId);

        // --- Classer chaque écart (exigence non couverte) en NC / observation ---
        List<StandardsDto.NonConformity> ncs = new ArrayList<>();
        int major = 0, minor = 0, observations = 0;
        int s1Major = 0, s2Major = 0;
        List<String> s1Watch = new ArrayList<>();
        List<String> s2Watch = new ArrayList<>();

        for (StandardsDto.AuditFinding f : audit.findings()) {
            String type = ncType(f.obligation(), f.riskIfMissing());
            boolean stage1 = STAGE1_SECTIONS.contains(f.sectionCode());
            switch (type) {
                case "MAJOR" -> {
                    major++;
                    if (stage1) s1Major++; else s2Major++;
                }
                case "MINOR" -> {
                    minor++;
                    (stage1 ? s1Watch : s2Watch).add(f.requirementCode());
                }
                default -> observations++;
            }
            if (!"OBSERVATION".equals(type)) {
                ncs.add(new StandardsDto.NonConformity(
                        f.requirementId(), f.sectionCode(), f.clauseCode(), f.requirementCode(),
                        f.requirementText(), type, descriptionFor(type, f),
                        correctionFor(f)));
            }
        }
        // Tri : NC majeures d'abord, puis mineures (codes lisibles côté rapport).
        ncs.sort((a, b) -> {
            int byType = rank(a.type()) - rank(b.type());
            return byType != 0 ? byType : a.requirementCode().compareTo(b.requirementCode());
        });

        // --- Scores par périmètre d'étape (proxy : couverture d'exigences) ---
        int s1Cov = 0, s1Tot = 0, s2Cov = 0, s2Tot = 0;
        for (StandardsDto.SectionAlignment sec : alignment.sections()) {
            if (STAGE1_SECTIONS.contains(sec.sectionCode())) {
                s1Cov += sec.coveredRequirements();
                s1Tot += sec.totalRequirements();
            } else {
                s2Cov += sec.coveredRequirements();
                s2Tot += sec.totalRequirements();
            }
        }
        // Repli pour les normes hors HLS (sections non numérotées 4-7) : la revue
        // documentaire porte alors sur l'ensemble (alignement global).
        if (s1Tot == 0) {
            s1Cov = alignment.coveredRequirements();
            s1Tot = alignment.totalRequirements();
        }
        double s1Score = pct(s1Cov, s1Tot);
        double s2Score = pct(s2Cov, s2Tot);

        // --- Étape 1 : revue documentaire ---
        // Franchie si le système documentaire est suffisamment établi (≥ 70 %)
        // et sans NC majeure sur les fondations (§4-7).
        boolean stage1Passed = s1Score >= 70d && s1Major == 0;
        String stage1Summary = stage1Passed
                ? "Système de management établi et documenté — passage à l'étape 2 autorisé."
                : (s1Major > 0
                    ? s1Major + " non-conformité(s) majeure(s) sur le système documenté — étape 2 ajournée."
                    : "Documentation incomplète (" + Math.round(s1Score) + " %) — étape 2 ajournée.");
        StandardsDto.AuditStageResult stage1 = new StandardsDto.AuditStageResult(
                1, "Revue documentaire (§4-7)", stage1Passed, s1Score, stage1Summary, s1Watch);

        // --- Étape 2 : audit terrain (uniquement si étape 1 franchie) ---
        boolean stage2Conducted = stage1Passed;
        boolean stage2Passed = stage2Conducted && s2Major == 0;
        String stage2Summary;
        if (!stage2Conducted) {
            stage2Summary = "Non réalisée — l'étape 1 doit être franchie au préalable.";
        } else if (s2Major > 0) {
            stage2Summary = s2Major + " non-conformité(s) majeure(s) terrain à lever avant certification.";
        } else if (!s2Watch.isEmpty()) {
            stage2Summary = "Mise en œuvre conforme — " + s2Watch.size()
                    + " non-conformité(s) mineure(s) à traiter sous 3 mois.";
        } else {
            stage2Summary = "Mise en œuvre conforme — aucune non-conformité terrain.";
        }
        StandardsDto.AuditStageResult stage2 = new StandardsDto.AuditStageResult(
                2, "Audit terrain (§8-10)", stage2Passed, s2Score, stage2Summary, s2Watch);

        // --- Décision de certification (ISO/IEC 17021-1 §9.4) ---
        String decision;
        if (!stage1Passed) {
            decision = AJOURNE;
        } else if (major > 0) {
            decision = NON_CERTIFIABLE;
        } else if (minor > 0) {
            decision = CERTIFIABLE_SOUS_RESERVE;
        } else {
            decision = CERTIFIABLE;
        }
        String decisionLabel = labelFor(decision, major, minor);

        Instant now = Instant.now();
        StandardsDto.MockCertificate certificate = buildCertificate(decision, adoption, standard, now);

        // --- Preuve d'intégrité : hash + signature hybride + ancrage du verdict ---
        String canonical = standard.code() + "|" + decision + "|maj=" + major + "|min=" + minor
                + "|obs=" + observations + "|cert=" + (certificate == null ? "-" : certificate.certificateNumber())
                + "|" + now;
        String sha256 = sha256(canonical);
        SignatureEnvelope envelope = signer.sign(SIGN_CONTEXT, sha256.getBytes(StandardCharsets.UTF_8));
        UUID tenantId = UUID.fromString(TenantContext.getTenantId());
        String anchorTxRef = blockchain.submitRoot(tenantId, sha256);

        return new StandardsDto.CertificationBlancReport(
                adoptionId, standard.id(), standard.code(), standard.fullName(), now,
                stage1, stage2, major, minor, observations,
                decision, decisionLabel, ncs, certificate,
                sha256, anchorTxRef, envelope.encode());
    }

    // ===== classification ISO/IEC 17021-1 =====

    /** MUST + risque élevé → NC majeure ; MUST → NC mineure ; SHOULD/MAY → observation. */
    private String ncType(ObligationLevel obligation, RiskLevel risk) {
        if (obligation == ObligationLevel.MUST) {
            return (risk == RiskLevel.HIGH || risk == RiskLevel.CRITICAL) ? "MAJOR" : "MINOR";
        }
        return "OBSERVATION";
    }

    private int rank(String type) {
        return switch (type) {
            case "MAJOR" -> 0;
            case "MINOR" -> 1;
            default -> 2;
        };
    }

    private String descriptionFor(String type, StandardsDto.AuditFinding f) {
        String prefix = switch (type) {
            case "MAJOR" -> "NC majeure";
            case "MINOR" -> "NC mineure";
            default -> "Observation";
        };
        return prefix + " — exigence " + f.requirementCode()
                + " non démontrée par une preuve : " + f.requirementText();
    }

    private String correctionFor(StandardsDto.AuditFinding f) {
        String evidence = f.expectedEvidence();
        String base = "Mettre en œuvre l'exigence " + f.requirementCode()
                + " et fournir la preuve correspondante.";
        if (evidence != null && !evidence.isBlank()) {
            return base + " Preuve attendue : " + evidence + ".";
        }
        return base;
    }

    private String labelFor(String decision, int major, int minor) {
        return switch (decision) {
            case CERTIFIABLE -> "Recommandé pour certification — aucune non-conformité.";
            case CERTIFIABLE_SOUS_RESERVE -> "Recommandé sous réserve — " + minor
                    + " NC mineure(s) à lever sous 3 mois.";
            case NON_CERTIFIABLE -> "Non recommandé — " + major
                    + " NC majeure(s) à lever avant un nouvel audit.";
            default -> "Ajourné — système de management documentaire incomplet (étape 1 non franchie).";
        };
    }

    private StandardsDto.MockCertificate buildCertificate(
            String decision, StandardsDto.AdoptionResponse adoption,
            StandardsDto.StandardDetail standard, Instant now) {
        if (!CERTIFIABLE.equals(decision) && !CERTIFIABLE_SOUS_RESERVE.equals(decision)) {
            return null; // pas de certificat si NC majeures ou ajournement
        }
        int months = standard.recertificationCycleMonths() != null
                ? standard.recertificationCycleMonths() : 36;
        Instant expires = now.plus(months * 30L, ChronoUnit.DAYS);
        String number = "QOS-BLANC/" + standard.code().toUpperCase() + "/"
                + now.toString().substring(0, 4) + "/"
                + Integer.toHexString(adoption.id().hashCode()).toUpperCase();
        String conditions = CERTIFIABLE_SOUS_RESERVE.equals(decision)
                ? "Émission conditionnée à la levée des NC mineures sous 3 mois (plan d'action accepté)."
                : "Aucune réserve.";
        return new StandardsDto.MockCertificate(
                number, standard.code(),
                adoption.scopeDescription() == null ? "—" : adoption.scopeDescription(),
                adoption.certificationBody() == null ? "—" : adoption.certificationBody(),
                now, expires, conditions,
                "SIMULATION (audit à blanc) — sans valeur d'accréditation. "
                        + "Seul un audit réalisé par un organisme accrédité (COFRAC/UKAS…) délivre un certificat valide.");
    }

    private double pct(int covered, int total) {
        return total == 0 ? 0d : (covered * 100d) / total;
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
