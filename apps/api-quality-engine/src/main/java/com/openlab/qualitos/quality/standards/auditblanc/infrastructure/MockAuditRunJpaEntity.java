package com.openlab.qualitos.quality.standards.auditblanc.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

/**
 * Persistance d'une exécution d'audit blanc IA avancé (Standards Hub §8.4
 * onglet 7). Les questions, écarts et plan de remédiation sont stockés en JSON
 * via {@code @JdbcTypeCode(LONGVARCHAR)} sur des colonnes TEXT — jamais
 * {@code @Lob String} (cf. consigne migration).
 */
@Entity
@Table(name = "standard_mock_audits",
        indexes = {
                @Index(name = "idx_sma_tenant", columnList = "tenant_id"),
                @Index(name = "idx_sma_tenant_adoption", columnList = "tenant_id, adoption_id")
        })
public class MockAuditRunJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "adoption_id", nullable = false)
    private UUID adoptionId;

    @Column(name = "standard_id", nullable = false)
    private UUID standardId;

    @Column(name = "standard_code", nullable = false, length = 100)
    private String standardCode;

    @Column(name = "standard_name", nullable = false, length = 500)
    private String standardName;

    @Column(nullable = false)
    private double readiness;

    @Column(name = "major_count", nullable = false)
    private int majorCount;

    @Column(name = "minor_count", nullable = false)
    private int minorCount;

    @Column(name = "observation_count", nullable = false)
    private int observationCount;

    @Column(name = "question_count", nullable = false)
    private int questionCount;

    @Column(name = "questions_json", columnDefinition = "TEXT", nullable = false)
    @JdbcTypeCode(Types.LONGVARCHAR)
    private String questionsJson;

    @Column(name = "gaps_json", columnDefinition = "TEXT", nullable = false)
    @JdbcTypeCode(Types.LONGVARCHAR)
    private String gapsJson;

    @Column(name = "remediation_json", columnDefinition = "TEXT", nullable = false)
    @JdbcTypeCode(Types.LONGVARCHAR)
    private String remediationJson;

    @Column(name = "ai_provider", length = 100)
    private String aiProvider;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getAdoptionId() { return adoptionId; }
    public void setAdoptionId(UUID v) { this.adoptionId = v; }
    public UUID getStandardId() { return standardId; }
    public void setStandardId(UUID v) { this.standardId = v; }
    public String getStandardCode() { return standardCode; }
    public void setStandardCode(String v) { this.standardCode = v; }
    public String getStandardName() { return standardName; }
    public void setStandardName(String v) { this.standardName = v; }
    public double getReadiness() { return readiness; }
    public void setReadiness(double v) { this.readiness = v; }
    public int getMajorCount() { return majorCount; }
    public void setMajorCount(int v) { this.majorCount = v; }
    public int getMinorCount() { return minorCount; }
    public void setMinorCount(int v) { this.minorCount = v; }
    public int getObservationCount() { return observationCount; }
    public void setObservationCount(int v) { this.observationCount = v; }
    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int v) { this.questionCount = v; }
    public String getQuestionsJson() { return questionsJson; }
    public void setQuestionsJson(String v) { this.questionsJson = v; }
    public String getGapsJson() { return gapsJson; }
    public void setGapsJson(String v) { this.gapsJson = v; }
    public String getRemediationJson() { return remediationJson; }
    public void setRemediationJson(String v) { this.remediationJson = v; }
    public String getAiProvider() { return aiProvider; }
    public void setAiProvider(String v) { this.aiProvider = v; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID v) { this.createdByUserId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
}
