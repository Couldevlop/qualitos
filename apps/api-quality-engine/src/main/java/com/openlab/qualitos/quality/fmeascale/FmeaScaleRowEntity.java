package com.openlab.qualitos.quality.fmeascale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Une ligne de barème REDÉFINIE par un tenant.
 *
 * <p>Absence de lignes = barème de référence. Un tenant qui n'a rien changé n'a
 * rien ici : c'est ce qui permet de distinguer « jamais touché » de « redéfini
 * à l'identique », distinction qu'un auditeur vient précisément chercher.
 */
@Entity
@Table(name = "fmea_rating_scale_rows")
public class FmeaScaleRowEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "kind", nullable = false, length = 20)
    private String kind;

    @Column(name = "score", nullable = false)
    private short score;

    @Column(name = "label", nullable = false, length = 120)
    private String label;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "time_period", length = 120)
    private String timePeriod;

    @Column(name = "failure_rate", length = 120)
    private String failureRate;

    /**
     * Qui a redéfini la règle de cotation, et quand. Changer un barème rend
     * incomparables les RPN cotés avant et après : c'est une décision de
     * politique qualité, elle doit porter un nom et une date.
     */
    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public String getKind() { return kind; }
    public void setKind(String v) { this.kind = v; }
    public short getScore() { return score; }
    public void setScore(short v) { this.score = v; }
    public String getLabel() { return label; }
    public void setLabel(String v) { this.label = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getTimePeriod() { return timePeriod; }
    public void setTimePeriod(String v) { this.timePeriod = v; }
    public String getFailureRate() { return failureRate; }
    public void setFailureRate(String v) { this.failureRate = v; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID v) { this.updatedBy = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
