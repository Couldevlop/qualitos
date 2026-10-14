package com.openlab.qualitos.quality.audit;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Rapport d'audit (§4.4, ISO 19011 §6.5) : un brouillon qui se retouche, puis une
 * émission qui le fige.
 *
 * <p><b>Ce qui est réutilisé, et pourquoi.</b> La synthèse rédigée reste dans
 * {@code audit_plans.report_summary}, où l'écrivent déjà la génération par IA et
 * la clôture : le rapport la lit et l'écrit au même endroit. Une seconde colonne
 * aurait fait deux synthèses, et personne n'aurait su laquelle fait foi.
 *
 * <p><b>L'émission.</b> Elle n'a lieu que sur un audit CLÔTURÉ — un rapport remis
 * pendant que les constats courent encore affirmerait une conclusion que la suite
 * peut démentir. Elle exige une synthèse et une conclusion, recopie les chiffres
 * clés, calcule l'empreinte du contenu et l'inscrit au journal chaîné du tenant :
 * c'est ce journal que l'ancrage périodique scelle (§11.5). L'émetteur vient du
 * jeton, jamais du corps de la requête (§18.2 #2).
 */
@Service
@Transactional
public class AuditReportService {

    static final String RESOURCE_TYPE = "audit_report";
    static final String ACTION_ISSUED = "audit.report.issued";

    private final AuditPlanRepository plans;
    private final AuditReportRepository reports;
    private final AuditEventService auditEvents;
    private final Clock clock;

    public AuditReportService(AuditPlanRepository plans,
                              AuditReportRepository reports,
                              AuditEventService auditEvents,
                              Clock clock) {
        this.plans = plans;
        this.reports = reports;
        this.auditEvents = auditEvents;
        this.clock = clock;
    }

    /**
     * Le rapport de l'audit. Sans brouillon enregistré, un rapport vierge est
     * rendu — avec ses chiffres clés déjà calculés — plutôt qu'un 404 : l'écran
     * a toujours une section « Rapport » à montrer.
     */
    @Transactional(readOnly = true)
    public AuditReportDto.View get(UUID planId) {
        UUID tenantId = requireTenantId();
        AuditPlan plan = loadPlan(planId, tenantId);
        return reports.findByTenantIdAndPlanId(tenantId, planId)
                .map(r -> view(plan, r))
                .orElseGet(() -> blankView(plan));
    }

    /** Enregistre le brouillon. Refusé sur un audit annulé et sur un rapport déjà émis. */
    public AuditReportDto.View saveDraft(UUID planId, AuditReportDto.DraftRequest req) {
        UUID tenantId = requireTenantId();
        AuditPlan plan = loadPlan(planId, tenantId);
        if (plan.getStatus() == AuditStatus.CANCELLED) {
            throw new AuditStateException("A cancelled audit has no report");
        }
        AuditReport report = reports.findByTenantIdAndPlanId(tenantId, planId)
                .orElseGet(() -> newDraft(tenantId, planId));
        if (report.isIssued()) {
            throw new AuditStateException("The audit report has been issued and can no longer be edited");
        }
        plan.setReportSummary(trimToNull(req.summary()));
        report.setStrengths(trimToNull(req.strengths()));
        report.setImprovements(trimToNull(req.improvements()));
        report.setConclusion(req.conclusion());
        report.setUpdatedBy(CurrentUser.userId().orElse(null));
        plans.save(plan);
        return view(plan, reports.save(report));
    }

    /**
     * Émet le rapport : le contenu est figé, son empreinte calculée et inscrite au
     * journal chaîné. Aucun retour en brouillon n'est prévu — un rapport remis se
     * corrige par un nouvel audit, pas en réécrivant celui qui a été remis.
     */
    public AuditReportDto.View issue(UUID planId) {
        UUID tenantId = requireTenantId();
        AuditPlan plan = loadPlan(planId, tenantId);
        if (plan.getStatus() != AuditStatus.COMPLETED) {
            throw new AuditStateException(
                    "The audit report can only be issued once the audit is COMPLETED, not " + plan.getStatus());
        }
        AuditReport report = reports.findByTenantIdAndPlanId(tenantId, planId)
                .orElseGet(() -> newDraft(tenantId, planId));
        if (report.isIssued()) {
            throw new AuditStateException("The audit report has already been issued on " + report.getIssuedAt());
        }
        String summary = trimToNull(plan.getReportSummary());
        if (summary == null) {
            throw new AuditReportIncompleteException("The report needs a written summary before it is issued");
        }
        if (report.getConclusion() == null) {
            throw new AuditReportIncompleteException("The report needs a conclusion before it is issued");
        }

        UUID issuer = CurrentUser.requireUserId();
        String issuerName = CurrentUser.displayName().orElse(null);
        Instant now = Instant.now(clock);
        AuditReportDto.KeyFigures figures = liveFigures(plan);

        report.setMajorNcCount(figures.majorNc());
        report.setMinorNcCount(figures.minorNc());
        report.setObservationCount(figures.observation());
        report.setOpportunityCount(figures.opportunity());
        report.setConformityCount(figures.conformity());
        report.setConformityScore(figures.conformityScore());
        report.setIssuedSummary(summary);
        report.setIssuedAt(now);
        report.setIssuedBy(issuer);
        report.setIssuedByName(issuerName);
        report.setUpdatedBy(issuer);
        report.setContentSha256(fingerprint(plan, report, figures, now, issuer));
        report.setStatus(AuditReportStatus.ISSUED);
        AuditReport saved = reports.save(report);

        auditEvents.recordForTenant(tenantId, new AuditEventDto.RecordEventRequest(
                null,
                ActorType.USER,
                issuer,
                ACTION_ISSUED,
                RESOURCE_TYPE,
                saved.getId(),
                "Rapport d'audit émis pour " + plan.getReference(),
                payload(plan, saved),
                null,
                null));
        return view(plan, saved);
    }

    // --- vues ---

    private AuditReportDto.View view(AuditPlan plan, AuditReport r) {
        if (r.isIssued()) {
            int total = r.getMajorNcCount() + r.getMinorNcCount() + r.getObservationCount()
                    + r.getOpportunityCount() + r.getConformityCount();
            AuditReportDto.KeyFigures frozen = new AuditReportDto.KeyFigures(
                    r.getMajorNcCount(), r.getMinorNcCount(), r.getObservationCount(),
                    r.getOpportunityCount(), r.getConformityCount(), total, r.getConformityScore());
            return new AuditReportDto.View(r.getId(), plan.getId(), r.getStatus(), r.getIssuedSummary(),
                    r.getStrengths(), r.getImprovements(), r.getConclusion(), frozen,
                    r.getIssuedAt(), r.getIssuedBy(), r.getIssuedByName(), r.getContentSha256(),
                    r.getUpdatedAt());
        }
        return new AuditReportDto.View(r.getId(), plan.getId(), r.getStatus(), plan.getReportSummary(),
                r.getStrengths(), r.getImprovements(), r.getConclusion(), liveFigures(plan),
                null, null, null, null, r.getUpdatedAt());
    }

    private AuditReportDto.View blankView(AuditPlan plan) {
        return new AuditReportDto.View(null, plan.getId(), AuditReportStatus.DRAFT, plan.getReportSummary(),
                null, null, null, liveFigures(plan), null, null, null, null, null);
    }

    static AuditReportDto.KeyFigures liveFigures(AuditPlan plan) {
        List<AuditFinding> findings = plan.getFindings();
        int major = count(findings, FindingType.MAJOR_NC);
        int minor = count(findings, FindingType.MINOR_NC);
        int observation = count(findings, FindingType.OBSERVATION);
        int opportunity = count(findings, FindingType.OPPORTUNITY);
        int conformity = count(findings, FindingType.CONFORMITY);
        return new AuditReportDto.KeyFigures(major, minor, observation, opportunity, conformity,
                findings.size(), AuditService.computeConformityScore(plan));
    }

    private static int count(List<AuditFinding> findings, FindingType type) {
        return (int) findings.stream().filter(f -> f.getType() == type).count();
    }

    // --- empreinte et journal ---

    /**
     * Empreinte SHA-256 du contenu émis, sur une forme canonique champ par champ.
     * Le séparateur est un caractère de contrôle qu'aucune saisie ne contient :
     * deux contenus différents ne peuvent pas se concaténer en la même chaîne.
     */
    static String fingerprint(AuditPlan plan, AuditReport r, AuditReportDto.KeyFigures f,
                              Instant issuedAt, UUID issuer) {
        String sep = "\u001f";
        String canonical = String.join(sep,
                String.valueOf(plan.getId()),
                String.valueOf(plan.getReference()),
                String.valueOf(r.getConclusion()),
                String.valueOf(r.getIssuedSummary()),
                String.valueOf(r.getStrengths()),
                String.valueOf(r.getImprovements()),
                f.majorNc() + "/" + f.minorNc() + "/" + f.observation() + "/"
                        + f.opportunity() + "/" + f.conformity(),
                String.valueOf(f.conformityScore()),
                issuedAt.toString(),
                String.valueOf(issuer));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /**
     * Ce que le journal retient : quel audit, quelle conclusion, quels chiffres,
     * et l'empreinte. Pas le texte : il est long, libre, et le journal se relit et
     * s'exporte ; l'empreinte suffit à prouver qu'il n'a pas bougé.
     */
    private static String payload(AuditPlan plan, AuditReport r) {
        return "{\"planId\":\"" + plan.getId()
                + "\",\"reference\":" + jsonString(plan.getReference())
                + ",\"conclusion\":\"" + r.getConclusion()
                + "\",\"majorNc\":" + r.getMajorNcCount()
                + ",\"minorNc\":" + r.getMinorNcCount()
                + ",\"observation\":" + r.getObservationCount()
                + ",\"opportunity\":" + r.getOpportunityCount()
                + ",\"conformity\":" + r.getConformityCount()
                + ",\"sha256\":\"" + r.getContentSha256() + "\"}";
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    // --- garde-fous ---

    private AuditReport newDraft(UUID tenantId, UUID planId) {
        AuditReport r = new AuditReport();
        r.setTenantId(tenantId);
        r.setPlanId(planId);
        r.setStatus(AuditReportStatus.DRAFT);
        return r;
    }

    private AuditPlan loadPlan(UUID planId, UUID tenantId) {
        return plans.findByIdAndTenantId(planId, tenantId)
                .orElseThrow(() -> new AuditPlanNotFoundException(planId));
    }

    private static UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
