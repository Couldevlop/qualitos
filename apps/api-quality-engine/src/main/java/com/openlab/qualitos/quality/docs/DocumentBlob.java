package com.openlab.qualitos.quality.docs;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Un fichier stocké UNE fois par contenu chez un client (ADR 0094) : deux versions
 * au même fichier partagent la même ligne et le même objet. L'empreinte et la
 * taille d'origine décrivent le fichier déposé ; la taille stockée et l'encodage
 * disent ce que le stockage conserve réellement.
 */
@Entity
@Table(name = "document_blobs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_document_blobs_sha", columnNames = {"tenant_id", "sha256"}),
                @UniqueConstraint(name = "uq_document_blobs_object_key", columnNames = {"object_key"})
        })
@Getter
@Setter
@NoArgsConstructor
public class DocumentBlob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 64, updatable = false)
    private String sha256;

    @Column(name = "object_key", nullable = false, length = 512, updatable = false)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 150, updatable = false)
    private String contentType;

    @Column(name = "original_size", nullable = false, updatable = false)
    private long originalSize;

    @Column(name = "stored_size", nullable = false, updatable = false)
    private long storedSize;

    /** {@code gzip} si le stockage garde une copie compressée, {@code null} sinon. */
    @Column(name = "content_encoding", length = 10, updatable = false)
    private String contentEncoding;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }
}
