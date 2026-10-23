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

/** Un atelier du client ; {@code position} fixe l'ordre d'affichage. */
@Entity
@Table(name = "competency_workshops",
        uniqueConstraints = @UniqueConstraint(name = "uk_competency_workshops_name",
                columnNames = {"tenant_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
public class CompetencyWorkshopJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false)
    private int position;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
