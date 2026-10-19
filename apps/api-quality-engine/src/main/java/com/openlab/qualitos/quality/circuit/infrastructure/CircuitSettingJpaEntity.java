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

/** Le circuit d'un client pour un type d'objet est-il actif ? Sans ligne, oui (ADR 0095). */
@Entity
@Table(name = "circuit_settings")
@IdClass(CircuitSettingJpaEntity.Key.class)
@Getter
@Setter
@NoArgsConstructor
public class CircuitSettingJpaEntity {

    @Id
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Id
    @Column(nullable = false, updatable = false, length = 40)
    private String subject;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "updated_by")
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
    }
}
