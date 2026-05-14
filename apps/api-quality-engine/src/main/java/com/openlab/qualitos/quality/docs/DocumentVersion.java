package com.openlab.qualitos.quality.docs;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_versions",
        uniqueConstraints = @UniqueConstraint(name = "uk_doc_version_number",
                columnNames = {"document_id", "version_number"}))
@Getter
@Setter
@NoArgsConstructor
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false, updatable = false)
    private Document document;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(columnDefinition = "TEXT")
    private String content;

    /** URI optionnelle vers le binaire stocké (S3/MinIO/Vault). */
    @Column(name = "content_uri", length = 1024)
    private String contentUri;

    /** Hash SHA-256 hex pour ancrage blockchain (32B → 64 hex chars). */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "change_note", columnDefinition = "TEXT")
    private String changeNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VersionStatus status;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    /** Hash blockchain (rempli après ancrage). */
    @Column(name = "blockchain_tx_hash", length = 128)
    private String blockchainTxHash;

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
            this.status = VersionStatus.DRAFT;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
