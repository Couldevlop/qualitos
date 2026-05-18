package com.openlab.qualitos.quality.marketplace.infrastructure;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marketplace_packs")
public class MarketplacePackJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "pack_id", nullable = false, length = 64, updatable = false)
    private String packId;

    @Column(name = "version", nullable = false, length = 32, updatable = false)
    private String version;

    @Column(name = "publisher", nullable = false, length = 120)
    private String publisher;

    @Column(name = "title", nullable = false, length = 250)
    private String title;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "sector", nullable = false, length = 80)
    private String sector;

    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    @Column(name = "currency", nullable = false, length = 8)
    private String currency;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "signature_hash", length = 128)
    private String signatureHash;

    @Column(name = "manifest_url", nullable = false, length = 2000)
    private String manifestUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getPackId() { return packId; }
    public void setPackId(String packId) { this.packId = packId; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
    public int getPriceCents() { return priceCents; }
    public void setPriceCents(int priceCents) { this.priceCents = priceCents; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public UUID getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(UUID verifiedBy) { this.verifiedBy = verifiedBy; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public String getSignatureHash() { return signatureHash; }
    public void setSignatureHash(String signatureHash) { this.signatureHash = signatureHash; }
    public String getManifestUrl() { return manifestUrl; }
    public void setManifestUrl(String manifestUrl) { this.manifestUrl = manifestUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
