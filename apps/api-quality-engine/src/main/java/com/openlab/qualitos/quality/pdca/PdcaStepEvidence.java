package com.openlab.qualitos.quality.pdca;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Pièce jointe apportée en preuve à UNE étape d'un cycle PDCA (§3.1, ADR 0061).
 *
 * <p>Une étape déclarée faite sans document ne prouve rien : elle affirme. La
 * preuve de la mise en place d'une action est toujours un document — relevé
 * signé, procédure approuvée, constat photographique — et c'est ce document que
 * l'auditeur réclame quand il demande à voir le cycle.
 *
 * <p>Comme pour les preuves CAPA et les photos de non-conformité, seule la
 * métadonnée est persistée ici ; le binaire vit dans le stockage objet, sous une
 * clé tenantisée. La base ne porte jamais d'octets de fichier.
 */
@Entity
@Table(name = "pdca_step_evidences")
@Getter
@Setter
@NoArgsConstructor
public class PdcaStepEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Tenant propriétaire, dupliqué depuis le cycle.
     *
     * <p>La colonne est redondante avec {@code pdca_cycles.tenant_id}, et c'est
     * délibéré : toute lecture de preuve filtre alors sur un seul index sans
     * jointure, et une requête qui oublierait le filtre de cycle resterait
     * malgré tout enfermée dans son tenant.
     */
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "cycle_id", nullable = false, updatable = false)
    private UUID cycleId;

    /**
     * Étape justifiée par la pièce. Jamais nulle, à la différence des preuves
     * CAPA : dans un cycle PDCA, c'est l'étape qui se prouve — le cycle, lui, ne
     * se prouve que par la somme de ses étapes.
     *
     * <p>Non modifiable après coup : déplacer une preuve d'une étape à une autre
     * reviendrait à réattribuer une pièce dans un dossier d'audit sans que rien
     * ne le dise. On retire, puis on reverse — et les deux gestes se consignent.
     */
    @Column(name = "step_id", nullable = false, updatable = false)
    private UUID stepId;

    /** Clé d'objet : tenants/{tenantId}/pdca/{cycleId}/steps/{stepId}/{uuid}.{ext}. */
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
