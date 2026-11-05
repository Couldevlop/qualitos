package com.openlab.qualitos.core.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Les octets du logo d'un client.
 *
 * <p>Table à part de {@code tenant_identities} : l'identité (slogan, couleurs,
 * thème) est lue à chaque ouverture d'écran, le logo une fois par session puis
 * mis en cache par le navigateur. Les mélanger ferait transporter un demi
 * mégaoctet dans chaque lecture de marque.
 *
 * <p>Le pourquoi du stockage en base plutôt qu'en objets est écrit dans
 * {@code V10__tenant_identity.sql} — api-core n'a pas de port de stockage, et
 * en attacher un ferait dépendre l'affichage de tout écran d'un service tiers.
 */
@Entity
@Table(name = "tenant_logos")
@EntityListeners(AuditingEntityListener.class)
public class TenantLogo {

    @Id
    @Column(name = "tenant_id", updatable = false, nullable = false)
    private UUID tenantId;

    @Column(name = "content_type", nullable = false, length = 32)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;

    /** Empreinte du contenu : sert l'ETag, donc le 304 sans relire les octets. */
    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Lob
    @Column(name = "content", nullable = false)
    private byte[] content;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public int getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(int sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }

    public byte[] getContent() { return content; }
    public void setContent(byte[] content) { this.content = content; }
}
