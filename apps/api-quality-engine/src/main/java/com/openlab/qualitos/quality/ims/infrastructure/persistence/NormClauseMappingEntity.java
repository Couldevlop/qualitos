package com.openlab.qualitos.quality.ims.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Entité JPA pour la table {@code norm_clause_mapping} (V53).
 * Catalogue public — pas de filtre tenant_id (les mappings de normes
 * sont plate-forme, partagés entre tenants).
 */
@Entity
@Table(name = "norm_clause_mapping")
public class NormClauseMappingEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "source_standard_code", nullable = false, length = 100)
    private String sourceStandardCode;

    @Column(name = "source_clause_code", nullable = false, length = 30)
    private String sourceClauseCode;

    @Column(name = "target_standard_code", nullable = false, length = 100)
    private String targetStandardCode;

    @Column(name = "target_clause_code", nullable = false, length = 30)
    private String targetClauseCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 20)
    private RelationTypeJpa relationType;

    @Column(name = "confidence", nullable = false)
    private int confidence;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSourceStandardCode() { return sourceStandardCode; }
    public void setSourceStandardCode(String sourceStandardCode) { this.sourceStandardCode = sourceStandardCode; }
    public String getSourceClauseCode() { return sourceClauseCode; }
    public void setSourceClauseCode(String sourceClauseCode) { this.sourceClauseCode = sourceClauseCode; }
    public String getTargetStandardCode() { return targetStandardCode; }
    public void setTargetStandardCode(String targetStandardCode) { this.targetStandardCode = targetStandardCode; }
    public String getTargetClauseCode() { return targetClauseCode; }
    public void setTargetClauseCode(String targetClauseCode) { this.targetClauseCode = targetClauseCode; }
    public RelationTypeJpa getRelationType() { return relationType; }
    public void setRelationType(RelationTypeJpa relationType) { this.relationType = relationType; }
    public int getConfidence() { return confidence; }
    public void setConfidence(int confidence) { this.confidence = confidence; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public enum RelationTypeJpa { EQUIVALENT, COVERS, RELATED, REFERENCES }
}
