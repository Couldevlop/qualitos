package com.openlab.qualitos.quality.risk;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Ligne d'un FMEA. Le RPN est calculé côté serveur (sévérité × occurrence × détection)
 * et persisté pour permettre les requêtes "top 10 RPN" sans recalcul. {@code rpnAfter}
 * agrège les notes APRÈS action recommandée — un item dont {@code resultingSeverity}
 * n'est pas renseigné n'a pas de rpnAfter (null).
 *
 * Pour un Bow-tie, on réutilise : function = événement central, failureMode = menace,
 * failureEffect = conséquence. La sémantique reste portée par le projet parent.
 */
@Entity
@Table(name = "fmea_items",
        indexes = {
                @Index(name = "idx_fmea_item_project",
                        columnList = "project_id, sequence_no"),
                @Index(name = "idx_fmea_item_rpn",
                        columnList = "tenant_id, rpn")
        })
public class FmeaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "function_text", length = 500)
    private String function;

    @Column(name = "failure_mode", length = 500)
    private String failureMode;

    @Column(name = "failure_effect", length = 500)
    private String failureEffect;

    @Column(name = "failure_cause", length = 1000)
    private String failureCause;

    @Column(name = "current_controls", length = 1000)
    private String currentControls;

    @Column(nullable = false)
    private int severity;

    @Column(nullable = false)
    private int occurrence;

    @Column(nullable = false)
    private int detection;

    @Column(nullable = false)
    private int rpn;

    @Column(name = "recommended_action", length = 1000)
    private String recommendedAction;

    @Column(name = "action_owner_user_id")
    private UUID actionOwnerUserId;

    @Column(name = "action_due_date")
    private LocalDate actionDueDate;

    @Column(name = "resulting_severity")
    private Integer resultingSeverity;

    @Column(name = "resulting_occurrence")
    private Integer resultingOccurrence;

    @Column(name = "resulting_detection")
    private Integer resultingDetection;

    @Column(name = "rpn_after")
    private Integer rpnAfter;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        recomputeRpn();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        recomputeRpn();
    }

    /**
     * Recalcule rpn et rpnAfter à partir des composantes. Appelé automatiquement
     * par les callbacks JPA mais aussi exposé pour usage explicite côté service.
     */
    public void recomputeRpn() {
        this.rpn = severity * occurrence * detection;
        if (resultingSeverity != null && resultingOccurrence != null && resultingDetection != null) {
            this.rpnAfter = resultingSeverity * resultingOccurrence * resultingDetection;
        } else {
            this.rpnAfter = null;
        }
    }

    // getters / setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public int getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(int sequenceNo) { this.sequenceNo = sequenceNo; }

    public String getFunction() { return function; }
    public void setFunction(String function) { this.function = function; }

    public String getFailureMode() { return failureMode; }
    public void setFailureMode(String failureMode) { this.failureMode = failureMode; }

    public String getFailureEffect() { return failureEffect; }
    public void setFailureEffect(String failureEffect) { this.failureEffect = failureEffect; }

    public String getFailureCause() { return failureCause; }
    public void setFailureCause(String failureCause) { this.failureCause = failureCause; }

    public String getCurrentControls() { return currentControls; }
    public void setCurrentControls(String currentControls) { this.currentControls = currentControls; }

    public int getSeverity() { return severity; }
    public void setSeverity(int severity) { this.severity = severity; }

    public int getOccurrence() { return occurrence; }
    public void setOccurrence(int occurrence) { this.occurrence = occurrence; }

    public int getDetection() { return detection; }
    public void setDetection(int detection) { this.detection = detection; }

    public int getRpn() { return rpn; }
    public void setRpn(int rpn) { this.rpn = rpn; }

    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }

    public UUID getActionOwnerUserId() { return actionOwnerUserId; }
    public void setActionOwnerUserId(UUID actionOwnerUserId) { this.actionOwnerUserId = actionOwnerUserId; }

    public LocalDate getActionDueDate() { return actionDueDate; }
    public void setActionDueDate(LocalDate actionDueDate) { this.actionDueDate = actionDueDate; }

    public Integer getResultingSeverity() { return resultingSeverity; }
    public void setResultingSeverity(Integer resultingSeverity) { this.resultingSeverity = resultingSeverity; }

    public Integer getResultingOccurrence() { return resultingOccurrence; }
    public void setResultingOccurrence(Integer resultingOccurrence) { this.resultingOccurrence = resultingOccurrence; }

    public Integer getResultingDetection() { return resultingDetection; }
    public void setResultingDetection(Integer resultingDetection) { this.resultingDetection = resultingDetection; }

    public Integer getRpnAfter() { return rpnAfter; }
    public void setRpnAfter(Integer rpnAfter) { this.rpnAfter = rpnAfter; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
