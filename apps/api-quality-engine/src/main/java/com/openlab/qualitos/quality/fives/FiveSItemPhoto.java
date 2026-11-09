package com.openlab.qualitos.quality.fives;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * La photo prise sur le terrain, attachée à UN pilier d'un audit 5S (§3.2).
 *
 * <p>Une note de 3 sur 10 ne dit pas ce qui a été vu. La photo, elle, le montre,
 * et c'est elle qu'on ressort trois mois plus tard pour savoir si la zone a
 * vraiment changé. Jusqu'ici l'audit ne portait qu'une colonne {@code photo_url}
 * à remplir à la main : personne ne tape une adresse d'image au milieu d'un
 * atelier.
 *
 * <p>Le pilier est désigné par son nom, pas par l'identifiant de la ligne de
 * notation. C'est voulu, et c'est ce qui rend la prise de vue utilisable hors
 * ligne : en zone blanche, la notation part dans la file d'attente et n'a donc
 * aucun identifiant serveur. Le pilier, lui, est connu de l'auditeur avant même
 * qu'il n'ait de réseau.
 *
 * <p>Seule la métadonnée vit ici ; les octets vivent dans le stockage objet sous
 * une clé tenantisée, comme les preuves PDCA, CAPA et APQP.
 */
@Entity
@Table(name = "fives_item_photos")
@Getter
@Setter
@NoArgsConstructor
public class FiveSItemPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Client propriétaire, recopié depuis l'audit.
     *
     * <p>Redondant avec {@code fives_audits.tenant_id}, et délibérément : la
     * colonne fait entrer la table dans la politique de cloisonnement (ADR 0086)
     * et permet de lire sur un seul index sans jointure. Une requête qui
     * oublierait le filtre d'audit resterait malgré tout enfermée dans son client.
     */
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "audit_id", nullable = false, updatable = false)
    private UUID auditId;

    /**
     * Le pilier photographié. Non modifiable : déplacer une photo d'un pilier à
     * un autre réattribuerait une preuve sans que rien ne le dise. On retire,
     * puis on reprend la photo.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pillar", nullable = false, length = 20, updatable = false)
    private FiveSPillar pillar;

    /** Clé d'objet : {@code tenants/{tenantId}/fives/{auditId}/{pillar}/{uuid}.{ext}}. */
    @Column(name = "object_key", nullable = false, length = 512, updatable = false)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    /** Qui a pris la photo. Une preuve anonyme se défend mal devant un auditeur. */
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
