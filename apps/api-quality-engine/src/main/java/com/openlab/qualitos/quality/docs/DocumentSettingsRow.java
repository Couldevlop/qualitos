package com.openlab.qualitos.quality.docs;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Le paramétrage GED d'un client (ADR 0094). Sa présence dit aussi que
 * l'arborescence et les processus par défaut ont été semés.
 */
@Entity
@Table(name = "document_settings")
@Getter
@Setter
@NoArgsConstructor
public class DocumentSettingsRow {

    static final int DEFAULT_REVIEW_MONTHS = 24;
    static final int DEFAULT_WARNING_DAYS = 30;
    static final int DEFAULT_MAX_FILE_MB = 25;
    /** Plafond de la plateforme : au-delà, le proxy et la limite multipart refusent. */
    static final int PLATFORM_MAX_FILE_MB = 50;

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "review_period_months", nullable = false)
    private int reviewPeriodMonths = DEFAULT_REVIEW_MONTHS;

    @Column(name = "review_warning_days", nullable = false)
    private int reviewWarningDays = DEFAULT_WARNING_DAYS;

    @Column(name = "max_file_mb", nullable = false)
    private int maxFileMb = DEFAULT_MAX_FILE_MB;

    @Column(name = "quota_mb")
    private Integer quotaMb;

    @Column(name = "obsolete_retention_years")
    private Integer obsoleteRetentionYears;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Les valeurs de la plateforme, pour un client qui n'a encore rien réglé. */
    static DocumentSettingsRow defaults(UUID tenantId) {
        DocumentSettingsRow row = new DocumentSettingsRow();
        row.setTenantId(tenantId);
        return row;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
