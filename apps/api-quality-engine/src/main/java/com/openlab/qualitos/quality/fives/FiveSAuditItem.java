package com.openlab.qualitos.quality.fives;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fives_audit_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_fives_audit_pillar",
                columnNames = {"audit_id", "pillar"}))
@Getter
@Setter
@NoArgsConstructor
public class FiveSAuditItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_id", nullable = false, updatable = false)
    private FiveSAudit audit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FiveSPillar pillar;

    /** Score du pilier, 0..10. */
    @Column(nullable = false)
    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "photo_url", length = 1024)
    private String photoUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
