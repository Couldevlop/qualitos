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
 * Adoption d'un standard par un tenant.
 * Représente l'engagement / le projet de certification du tenant sur une norme.
 */
@Entity
@Table(name = "tenant_standards",
        uniqueConstraints = @UniqueConstraint(name = "uk_tenant_standard",
                columnNames = {"tenant_id", "standard_id"}))
@Getter
@Setter
@NoArgsConstructor
public class TenantStandard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_id", nullable = false, updatable = false)
    private Standard standard;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdoptionStatus status;

    @Column(name = "scope_description", columnDefinition = "TEXT")
    private String scopeDescription;

    @Column(name = "target_certification_date")
    private LocalDate targetCertificationDate;

    @Column(name = "lead_auditor_id")
    private UUID leadAuditorId;

    @Column(name = "certification_body", length = 255)
    private String certificationBody;

    @Column(name = "certified_at")
    private Instant certifiedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "tenantStandard", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RequirementEvidence> evidences = new ArrayList<>();

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = AdoptionStatus.PLANNING;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
