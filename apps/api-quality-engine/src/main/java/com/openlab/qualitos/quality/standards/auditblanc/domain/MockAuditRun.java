package com.openlab.qualitos.quality.standards.auditblanc.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Agrégat — exécution d'un audit blanc IA avancé (Standards Hub §8.4 onglet 7).
 * Domaine PUR : aucune dépendance Spring/JPA.
 *
 * <p>Une exécution fige, pour une adoption de norme par un tenant : les questions
 * ciblées générées par l'IA, le rapport d'écarts (gap analysis) par clause avec
 * sa criticité, le plan de remédiation actionnable, et le score de préparation.
 * Le tenant ({@code tenantId}) et l'acteur ({@code createdByUserId}) proviennent
 * du JWT (jamais du body, OWASP A01, §18.2 #2/#5).
 */
public final class MockAuditRun {

    private UUID id;
    private final UUID tenantId;
    private final UUID adoptionId;
    private final UUID standardId;
    private final String standardCode;
    private final String standardName;
    private final double readiness;
    private final int majorCount;
    private final int minorCount;
    private final int observationCount;
    private final List<MockAuditQuestion> questions;
    private final List<ClauseGapFinding> gaps;
    private final List<RemediationAction> remediationPlan;
    private final String aiProvider;
    private final UUID createdByUserId;
    private final Instant createdAt;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public MockAuditRun(UUID id, UUID tenantId, UUID adoptionId, UUID standardId,
                        String standardCode, String standardName, double readiness,
                        int majorCount, int minorCount, int observationCount,
                        List<MockAuditQuestion> questions, List<ClauseGapFinding> gaps,
                        List<RemediationAction> remediationPlan, String aiProvider,
                        UUID createdByUserId, Instant createdAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.adoptionId = Objects.requireNonNull(adoptionId, "adoptionId");
        this.standardId = Objects.requireNonNull(standardId, "standardId");
        this.standardCode = requireText(standardCode, "standardCode");
        this.standardName = requireText(standardName, "standardName");
        if (readiness < 0d || readiness > 100d) {
            throw new IllegalArgumentException("readiness must be in [0, 100]");
        }
        this.readiness = readiness;
        this.majorCount = requireNonNegative(majorCount, "majorCount");
        this.minorCount = requireNonNegative(minorCount, "minorCount");
        this.observationCount = requireNonNegative(observationCount, "observationCount");
        this.questions = questions == null ? List.of() : List.copyOf(questions);
        this.gaps = gaps == null ? List.of() : List.copyOf(gaps);
        this.remediationPlan = remediationPlan == null ? List.of() : List.copyOf(remediationPlan);
        this.aiProvider = aiProvider == null ? "" : aiProvider;
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    /** Fabrique une exécution à partir de la matière assemblée par le service. */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public static MockAuditRun of(UUID tenantId, UUID adoptionId, UUID standardId,
                                  String standardCode, String standardName, double readiness,
                                  List<MockAuditQuestion> questions, List<ClauseGapFinding> gaps,
                                  List<RemediationAction> remediationPlan, String aiProvider,
                                  UUID createdByUserId, Instant now) {
        int[] counts = MockAuditAssembler.countByCriticality(gaps);
        return new MockAuditRun(null, tenantId, adoptionId, standardId,
                standardCode, standardName, readiness,
                counts[0], counts[1], counts[2],
                questions, gaps, remediationPlan, aiProvider, createdByUserId, now);
    }

    public void assignId(UUID newId) {
        this.id = newId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getAdoptionId() {
        return adoptionId;
    }

    public UUID getStandardId() {
        return standardId;
    }

    public String getStandardCode() {
        return standardCode;
    }

    public String getStandardName() {
        return standardName;
    }

    public double getReadiness() {
        return readiness;
    }

    public int getMajorCount() {
        return majorCount;
    }

    public int getMinorCount() {
        return minorCount;
    }

    public int getObservationCount() {
        return observationCount;
    }

    public int getQuestionCount() {
        return questions.size();
    }

    public List<MockAuditQuestion> getQuestions() {
        return questions;
    }

    public List<ClauseGapFinding> getGaps() {
        return gaps;
    }

    public List<RemediationAction> getRemediationPlan() {
        return remediationPlan;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " required");
        }
        return value;
    }

    private static int requireNonNegative(int value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must be >= 0");
        }
        return value;
    }
}
