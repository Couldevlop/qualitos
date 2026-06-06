package com.openlab.qualitos.quality.nonconformity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Métadonnées d'une photo de Non-Conformité (§4.3). Le binaire vit dans un
 * stockage objet S3-compatible ; seule la clé d'objet (tenantisée) est persistée.
 */
@Entity
@Table(name = "nc_photos")
@Getter
@Setter
@NoArgsConstructor
public class NcPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "nc_id", nullable = false, updatable = false)
    private UUID ncId;

    /** Clé d'objet dans le stockage : tenants/{tenantId}/nc/{ncId}/{uuid}.{ext}. */
    @Column(name = "object_key", nullable = false, length = 512, updatable = false)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
