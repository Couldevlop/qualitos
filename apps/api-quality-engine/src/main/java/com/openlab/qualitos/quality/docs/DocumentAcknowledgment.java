package com.openlab.qualitos.quality.docs;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_acknowledgments",
        uniqueConstraints = @UniqueConstraint(name = "uk_doc_ack_version_user",
                columnNames = {"version_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class DocumentAcknowledgment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false, updatable = false)
    private DocumentVersion version;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "acknowledged_at", nullable = false, updatable = false)
    private Instant acknowledgedAt;

    @PrePersist
    void prePersist() {
        if (this.acknowledgedAt == null) {
            this.acknowledgedAt = Instant.now();
        }
    }
}
