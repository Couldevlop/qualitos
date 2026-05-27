package com.openlab.qualitos.quality.standards;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entrée de veille normative (Standards Hub §8.4 onglet 8, §8.10) : une version /
 * révision d'une norme avec son statut, ses dates et l'analyse d'impact. Platform-level.
 */
@Entity
@Table(name = "standard_revisions",
        uniqueConstraints = @UniqueConstraint(name = "uk_srev_standard_version",
                columnNames = {"standard_id", "version"}))
@Getter
@Setter
@NoArgsConstructor
public class StandardRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_id", nullable = false, updatable = false)
    private Standard standard;

    /** Version / millésime (ex: "2015", "2022", "2026 (planifiée)"). */
    @Column(nullable = false, length = 60)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RevisionStatus status;

    @Column(name = "published_date")
    private LocalDate publishedDate;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "impact_note", columnDefinition = "TEXT")
    private String impactNote;

    @Column(name = "source_url", length = 1024)
    private String sourceUrl;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;
}
