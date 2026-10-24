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

/** Une compétence du catalogue suivie par la matrice, et son formateur désigné. */
@Entity
@Table(name = "competency_matrix_skills",
        uniqueConstraints = @UniqueConstraint(name = "uk_competency_matrix_skills_skill",
                columnNames = {"tenant_id", "skill_id"}))
@Getter
@Setter
@NoArgsConstructor
public class MatrixSkillJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "skill_id", nullable = false)
    private UUID skillId;

    @Column(name = "trainer_user_id", nullable = false)
    private UUID trainerUserId;

    @Column(name = "trainer_name", nullable = false, length = 250)
    private String trainerName;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
