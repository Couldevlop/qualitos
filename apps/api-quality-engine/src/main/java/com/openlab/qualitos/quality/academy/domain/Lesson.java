package com.openlab.qualitos.quality.academy.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

/**
 * Leçon ordonnée d'un module (texte, vidéo, simulation ou ressource externe).
 *
 * <p>Le corps {@code body} est un champ texte long : pattern projet
 * {@code @Column(columnDefinition="TEXT")} + {@code @JdbcTypeCode(LONGVARCHAR)}
 * (jamais {@code @Lob String} — bug oid connu sous PostgreSQL).</p>
 */
@Entity
@Table(name = "academy_lessons",
        uniqueConstraints = @UniqueConstraint(name = "uk_academy_lesson_module_order",
                columnNames = {"module_id", "order_index"}),
        indexes = @Index(name = "idx_academy_lesson_module", columnList = "module_id"))
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "module_id", nullable = false)
    private UUID moduleId;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 32)
    private LessonContentType contentType;

    @Column(columnDefinition = "TEXT")
    @JdbcTypeCode(Types.LONGVARCHAR)
    private String body;

    @Column(name = "media_url", length = 1000)
    private String mediaUrl;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (contentType == null) contentType = LessonContentType.TEXT;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getModuleId() { return moduleId; }
    public void setModuleId(UUID moduleId) { this.moduleId = moduleId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LessonContentType getContentType() { return contentType; }
    public void setContentType(LessonContentType contentType) { this.contentType = contentType; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
