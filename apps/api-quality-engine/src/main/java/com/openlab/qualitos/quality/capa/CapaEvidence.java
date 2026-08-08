package com.openlab.qualitos.quality.capa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Pièce jointe apportée en preuve à un dossier CAPA (§4.2, ISO 9001 §10.2).
 *
 * <p>Une CAPA se clôt sur une vérification d'efficacité, et l'efficacité se
 * prouve. La preuve se rattache au DOSSIER et non à l'action : c'est le niveau
 * où elle a valeur d'audit.
 *
 * <p>Comme pour les photos de non-conformité, seule la métadonnée est persistée
 * ici ; le binaire vit dans le stockage objet, sous une clé tenantisée.
 */
@Entity
@Table(name = "capa_evidences")
@Getter
@Setter
@NoArgsConstructor
public class CapaEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "capa_id", nullable = false, updatable = false)
    private UUID capaId;

    /** Clé d'objet : tenants/{tenantId}/capa/{capaId}/{uuid}.{ext}. */
    @Column(name = "object_key", nullable = false, length = 512, updatable = false)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    /**
     * Qui a versé la pièce. Une preuve anonyme se défend mal devant un auditeur :
     * la question « qui l'a produite ? » vient toujours.
     */
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
