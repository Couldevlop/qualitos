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
 * prouve. Deux niveaux de rattachement coexistent (ADR 0050 puis 0052) : la
 * pièce vaut pour le DOSSIER — le niveau que désigne la norme — ou pour UNE
 * action précise, quand c'est cette action qu'il faut justifier ligne à ligne
 * dans le tableau.
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

    /**
     * Action visée par la pièce, ou {@code null} quand la pièce vaut pour le
     * dossier entier (comportement d'origine, ADR 0050).
     *
     * <p>Non modifiable après coup : déplacer une preuve d'une action à une
     * autre reviendrait à réattribuer une pièce dans un dossier d'audit sans
     * que rien ne le dise. On retire, puis on reverse — et les deux gestes se
     * consignent (§11.5).
     */
    @Column(name = "action_id", updatable = false)
    private UUID actionId;

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
