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

    /**
     * Hash SHA-256 hex pour ancrage blockchain (32B → 64 hex chars) : celui du
     * fichier tel que déposé quand la version en porte un, celui du texte sinon.
     */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    /** Le fichier de la version, partagé avec toute autre version au même contenu (ADR 0094). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blob_id")
    private DocumentBlob blob;

    /** Le nom d'origine du fichier, assaini ; il s'affiche et nomme le téléchargement. */
    @Column(name = "file_name", length = 255)
    private String fileName;

    /** Quand la version a cessé de faire foi : point de départ de sa conservation. */
    @Column(name = "obsoleted_at")
    private Instant obsoletedAt;

    /** Le fichier a été retiré au terme de sa conservation ; l'empreinte reste. */
    @Column(name = "file_purged_at")
    private Instant filePurgedAt;

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

    /** Le dernier refus : qui, quand, pourquoi (la version est revenue en brouillon). */
    @Column(name = "rejected_by")
    private UUID rejectedBy;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

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
