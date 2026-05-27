package com.openlab.qualitos.quality.standards;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Modèle de document normatif (Standards Hub §8.4 onglet 3, §8.3 documents_required).
 * Platform-level (rattaché à un {@link Standard}, pas de tenant_id). Le fichier
 * modèle est servi depuis le classpath via {@code template_uri}.
 */
@Entity
@Table(name = "standard_document_templates",
        uniqueConstraints = @UniqueConstraint(name = "uk_sdt_standard_code",
                columnNames = {"standard_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
public class StandardDocumentTemplate {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentObligation obligation;

    @Column(name = "template_uri", nullable = false, length = 1024)
    private String templateUri;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "fingerprint_sha256", length = 64)
    private String fingerprintSha256;

    /** Catégorie (MANUAL, POLICY, PROCEDURE, RECORD, FORM, WORK_INSTRUCTION…). */
    @Column(length = 40)
    private String category;

    /** Clauses couvertes par ce document (CSV, ex: "7.5,4.4"). */
    @Column(name = "maps_to_clauses", columnDefinition = "TEXT")
    private String mapsToClauses;

    @Column(name = "order_index")
    private Integer orderIndex;
}
