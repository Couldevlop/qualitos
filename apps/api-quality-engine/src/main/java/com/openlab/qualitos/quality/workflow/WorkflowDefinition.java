package com.openlab.qualitos.quality.workflow;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

/**
 * Définition de workflow BPMN 2.0 conçue dans le designer no-code (§5.4).
 *
 * <p>Le diagramme est stocké tel quel sous forme de XML BPMN. On évite
 * {@code @Lob String} qui mappe vers un {@code oid} PostgreSQL (large object,
 * géré hors-ligne et fragile) : on force une colonne {@code TEXT} via
 * {@code @Column(columnDefinition = "TEXT") + @JdbcTypeCode(LONGVARCHAR)}
 * (même pattern qu'{@code IotDevice.metadataJson} / {@code AuditEvent.payloadJson}).</p>
 *
 * <p>Multi-tenant strict : le {@code tenantId} provient TOUJOURS du JWT, jamais
 * du body. {@code createdBy}/{@code updatedBy} sont l'acteur authentifié (sub).</p>
 */
@Entity
@Table(name = "workflow_definitions")
@Getter
@Setter
@NoArgsConstructor
public class WorkflowDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    @JdbcTypeCode(Types.LONGVARCHAR)
    private String description;

    /** Diagramme BPMN 2.0 sérialisé (XML). TEXT côté DB, jamais @Lob→oid. */
    @Column(name = "bpmn_xml", nullable = false, columnDefinition = "TEXT")
    @JdbcTypeCode(Types.LONGVARCHAR)
    private String bpmnXml;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkflowStatus status;

    /** Incrémenté à chaque mise à jour du contenu (versioning fonctionnel, pas optimistic lock). */
    @Column(nullable = false)
    private int version;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = WorkflowStatus.DRAFT;
        }
        if (this.version < 1) {
            this.version = 1;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
