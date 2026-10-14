package com.openlab.qualitos.quality.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Document joint à un audit : convocation, plan d'audit signé, photos, relevés,
 * rapport de l'organisme certificateur…
 *
 * <p>Comme pour les preuves CAPA, seule la métadonnée vit ici ; le binaire est
 * dans le stockage objet, sous une clé tenantisée construite par le serveur.
 */
@Entity
@Table(name = "audit_documents")
@Getter
@Setter
@NoArgsConstructor
public class AuditDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "plan_id", nullable = false, updatable = false)
    private UUID planId;

    /** Clé d'objet : tenants/{tenantId}/audits/{planId}/{uuid}.{ext}. */
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
