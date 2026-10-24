package com.openlab.qualitos.quality.supplier;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Document joint à une NC fournisseur (8D, preuves de CAPA) ou à un certificat.
 *
 * <p>Seule la métadonnée vit ici ; le binaire est dans le stockage objet, sous
 * une clé tenantisée construite par le serveur (même motif que les documents
 * d'audit).
 */
@Entity
@Table(name = "supplier_documents")
@Getter
@Setter
@NoArgsConstructor
public class SupplierDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "supplier_id", nullable = false, updatable = false)
    private UUID supplierId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 32, updatable = false)
    private SupplierDocumentOwnerType ownerType;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SupplierDocumentCategory category;

    /** Clé d'objet : tenants/{tenantId}/suppliers/{supplierId}/{owner}/{ownerId}/{uuid}.{ext}. */
    @Column(name = "object_key", nullable = false, length = 512, updatable = false)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    /** Nom d'origine, assaini, conservé pour l'affichage — jamais réinjecté dans la clé. */
    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
