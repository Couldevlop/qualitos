package com.openlab.qualitos.quality.costofquality.infrastructure;

import com.openlab.qualitos.quality.costofquality.domain.CoqCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** {@code tenant_id} nul : libellé du catalogue livré, commun à tous les clients. */
@Entity
@Table(name = "coq_labels")
@Getter
@Setter
@NoArgsConstructor
public class CoqLabelJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", updatable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32, updatable = false)
    private CoqCategory category;

    @Column(length = 64, updatable = false)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "part_control", nullable = false)
    private boolean partControl;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
