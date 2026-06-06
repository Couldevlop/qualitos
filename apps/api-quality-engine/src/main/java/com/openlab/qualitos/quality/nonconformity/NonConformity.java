package com.openlab.qualitos.quality.nonconformity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "non_conformities")
@Getter
@Setter
@NoArgsConstructor
public class NonConformity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    /** Code lisible généré côté service (ex. NC-2026-0001), unique par tenant. */
    @Column(nullable = false, length = 40, updatable = false)
    private String reference;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NcCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NcSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NcStatus status;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(length = 255)
    private String zone;

    /** Géolocalisation de la saisie terrain (optionnelle). */
    @Column(name = "geo_lat")
    private Double geoLat;

    @Column(name = "geo_lng")
    private Double geoLng;

    /** URLs des photos terrain, séparées par des retours ligne (pas de BLOB). */
    @Column(name = "photo_urls", columnDefinition = "TEXT")
    private String photoUrls;

    @Column(name = "reporter_id")
    private UUID reporterId;

    /** Lien d'escalade vers la CAPA créée à partir de cette NC. */
    @Column(name = "capa_case_id")
    private UUID capaCaseId;

    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = NcStatus.OPEN;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
