package com.openlab.qualitos.quality.standards;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "standard_requirements",
        uniqueConstraints = @UniqueConstraint(name = "uk_requirement_clause_code",
                columnNames = {"clause_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
public class StandardRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clause_id", nullable = false, updatable = false)
    private StandardClause clause;

    /** Code d'exigence (ex: "4.1.1"). */
    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ObligationLevel obligation;

    /** Types de preuves attendues (CSV/texte libre). */
    @Column(name = "evidence_types", columnDefinition = "TEXT")
    private String evidenceTypes;

    @Column(name = "measurable_criteria", columnDefinition = "TEXT")
    private String measurableCriteria;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_if_missing", length = 20)
    private RiskLevel riskIfMissing;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;
}
