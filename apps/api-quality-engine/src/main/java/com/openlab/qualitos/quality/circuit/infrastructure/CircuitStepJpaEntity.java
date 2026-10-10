package com.openlab.qualitos.quality.circuit.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/** Une étape du circuit réglé par un client pour un type d'objet. */
@Entity
@Table(name = "circuit_steps")
@IdClass(CircuitStepJpaEntity.Key.class)
@Getter
@Setter
@NoArgsConstructor
public class CircuitStepJpaEntity {

    @Id
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Id
    @Column(nullable = false, updatable = false, length = 40)
    private String subject;

    @Id
    @Column(nullable = false, updatable = false)
    private int ordinal;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "role_code", nullable = false, length = 64)
    private String roleCode;

    @Column(name = "min_approvals", nullable = false)
    private int minApprovals;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private UUID tenantId;
        private String subject;
        private int ordinal;
    }
}
