package com.openlab.qualitos.quality.training.competencymatrix.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Une personne inscrite dans la matrice de compétences, avec son atelier. */
@Entity
@Table(name = "competency_matrix_operators",
        uniqueConstraints = @UniqueConstraint(name = "uk_competency_matrix_operators_user",
                columnNames = {"tenant_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class MatrixOperatorJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_name", nullable = false, length = 250)
    private String userName;

    @Column(nullable = false, length = 60)
    private String workshop;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
