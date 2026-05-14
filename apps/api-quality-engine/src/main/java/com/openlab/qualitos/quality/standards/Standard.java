package com.openlab.qualitos.quality.standards;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entrée du catalogue normatif (référentiel global, partagé entre tous les tenants).
 * Pas de tenant_id : ces données sont "platform-level", maintenues par le super-admin.
 */
@Entity
@Table(name = "standards")
@Getter
@Setter
@NoArgsConstructor
public class Standard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Code stable, ex: "iso-9001", "iso-27001", "iatf-16949". */
    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "full_name", nullable = false, length = 500)
    private String fullName;

    @Column(length = 100)
    private String publisher;

    /** Version courante (ex: "2015", "2022"). */
    @Column(name = "current_version", nullable = false, length = 30)
    private String currentVersion;

    @Column(name = "publication_date")
    private LocalDate publicationDate;

    /** Famille : HLS, INDUSTRIAL, HEALTHCARE, FOOD, IT, FINANCE, ENERGY, ... */
    @Column(length = 50)
    private String family;

    /** Liste CSV des codes secteurs applicables (ex: "all" ou "auto,aerospace"). */
    @Column(name = "applicable_industries", columnDefinition = "TEXT")
    private String applicableIndustries;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "certification_body_required", nullable = false)
    private boolean certificationBodyRequired;

    /** Cycle de recertification en mois (ex: 36 pour ISO 9001). */
    @Column(name = "recertification_cycle_months")
    private Integer recertificationCycleMonths;

    /** Liste CSV des codes de normes liées (HLS family, parents). */
    @Column(name = "related_norm_codes", columnDefinition = "TEXT")
    private String relatedNormCodes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StandardStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "standard", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<StandardSection> sections = new ArrayList<>();

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = StandardStatus.PUBLISHED;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
