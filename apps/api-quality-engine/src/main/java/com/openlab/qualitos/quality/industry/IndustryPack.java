package com.openlab.qualitos.quality.industry;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Catalogue d'Industry Pack (CLAUDE.md §5).
 *
 * Source de vérité : fichiers YAML embarqués sous {@code classpath:industry-packs/*.yml}
 * + packs partenaires chargés depuis le marketplace (V2). Le {@link IndustryPackLoader}
 * upsert ces définitions au démarrage. Les colonnes texte JSON sont opaques côté DB
 * (PostgreSQL JSONB recommandé mais on reste portable en TEXT pour H2 dev).
 */
@Entity
@Table(name = "industry_packs",
        uniqueConstraints = @UniqueConstraint(name = "uk_industry_pack_code", columnNames = "code"))
public class IndustryPack {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 32)
    private String version;

    @Column(length = 16)
    private String locale;

    /** Tags / industries applicables, CSV. Ex : "iso-9001,iatf-16949,manufacturing". */
    @Column(name = "tags_csv", length = 1000)
    private String tagsCsv;

    /** Manifest complet (YAML re-sérialisé en JSON) pour traçabilité audit. */
    @Lob
    @Column(name = "manifest_json", nullable = false)
    private String manifestJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    // getters / setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public String getTagsCsv() { return tagsCsv; }
    public void setTagsCsv(String tagsCsv) { this.tagsCsv = tagsCsv; }

    public String getManifestJson() { return manifestJson; }
    public void setManifestJson(String manifestJson) { this.manifestJson = manifestJson; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
