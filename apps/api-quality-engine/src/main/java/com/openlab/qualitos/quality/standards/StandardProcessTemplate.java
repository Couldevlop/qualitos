package com.openlab.qualitos.quality.standards;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Processus type lié à une norme (Standards Hub §8.4 onglet 4, §8.3 processes_required).
 * Platform-level. {@code mapsToClauses} permet la matrice de co-couverture
 * « quels processus couvrent quelles clauses ». Un modèle BPMN optionnel peut être
 * référencé via {@code bpmn_uri}.
 */
@Entity
@Table(name = "standard_process_templates",
        uniqueConstraints = @UniqueConstraint(name = "uk_spt_standard_code",
                columnNames = {"standard_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
public class StandardProcessTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_id", nullable = false, updatable = false)
    private Standard standard;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Clauses couvertes par ce processus (CSV, ex: "9.3"). */
    @Column(name = "maps_to_clauses", columnDefinition = "TEXT")
    private String mapsToClauses;

    /** Modèle BPMN 2.0 (classpath uri), optionnel. */
    @Column(name = "bpmn_uri", length = 1024)
    private String bpmnUri;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;
}
