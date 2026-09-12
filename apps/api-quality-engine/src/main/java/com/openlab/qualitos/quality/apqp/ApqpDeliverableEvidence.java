package com.openlab.qualitos.quality.apqp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Pièce versée en preuve d'UN livrable APQP.
 *
 * <p>Un livrable coché sans document n'affirme que lui-même. Ce que l'auditeur
 * réclame, c'est la pièce : la spécification, le rapport de revue, le formulaire
 * d'approbation PPAP. Ces pièces arrivent par courriel, en {@code .docx} — et
 * jusqu'ici elles n'avaient nulle part où se ranger.
 *
 * <p>Comme pour les preuves PDCA et CAPA, seule la métadonnée est persistée ici ;
 * le binaire vit dans le stockage objet sous une clé tenantisée. La base ne porte
 * jamais d'octets de fichier.
 */
@Entity
@Table(name = "apqp_deliverable_evidences")
@Getter
@Setter
@NoArgsConstructor
public class ApqpDeliverableEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Tenant propriétaire, dupliqué depuis la phase.
     *
     * <p>Redondant avec {@code apqp_phases.tenant_id}, et délibérément : toute
     * lecture filtre alors sur un seul index sans jointure, et une requête qui
     * oublierait le filtre de livrable resterait malgré tout enfermée dans son
     * client.
     */
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "phase_id", nullable = false, updatable = false)
    private UUID phaseId;

    /**
     * Le livrable prouvé. Non modifiable après coup : déplacer une pièce d'un
     * livrable à un autre reviendrait à réattribuer un document dans un dossier
     * PPAP sans que rien ne le dise. On retire, puis on reverse — et les deux
     * gestes se consignent.
     */
    @Column(name = "deliverable_id", nullable = false, updatable = false)
    private UUID deliverableId;

    /**
     * Clé d'objet :
     * {@code tenants/{tenantId}/apqp/{phaseId}/deliverables/{deliverableId}/{uuid}.{ext}}.
     */
    @Column(name = "object_key", nullable = false, length = 512, updatable = false)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    /** Qui a versé la pièce. Une preuve anonyme se défend mal devant un auditeur. */
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
