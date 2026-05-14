package com.openlab.qualitos.quality.capa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "capa_cases")
@Getter
@Setter
@NoArgsConstructor
public class CapaCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CapaType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CapaCriticity criticity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CapaStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private CapaSourceType sourceType;

    @Column(name = "source_ref", length = 255)
    private String sourceRef;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    /** Lien optionnel vers la cause Ishikawa identifiée comme racine. */
    @Column(name = "root_cause_id")
    private UUID rootCauseId;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    /** Vérification de l'efficacité du CAPA après mise en oeuvre (true si efficace). */
    @Column(name = "effectiveness_verified")
    private Boolean effectivenessVerified;

    @Column(name = "effectiveness_verified_at")
    private Instant effectivenessVerifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "capa", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<CapaAction> actions = new ArrayList<>();

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = CapaStatus.OPEN;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
