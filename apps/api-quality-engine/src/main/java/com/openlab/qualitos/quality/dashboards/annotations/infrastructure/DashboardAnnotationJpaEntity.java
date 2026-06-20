package com.openlab.qualitos.quality.dashboards.annotations.infrastructure;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dashboard_annotations",
        indexes = {
                @Index(name = "idx_da_tenant_chart",
                        columnList = "tenant_id, chart_key, created_at"),
                @Index(name = "idx_da_tenant_author",
                        columnList = "tenant_id, author_id")
        })
public class DashboardAnnotationJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "author_id", nullable = false, updatable = false)
    private UUID authorId;

    @Column(name = "chart_key", nullable = false, length = 64, updatable = false)
    private String chartKey;

    @Column(name = "anchor_label", length = 120, updatable = false)
    private String anchorLabel;

    @Column(name = "body", nullable = false, length = 2000, updatable = false)
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getAuthorId() { return authorId; }
    public void setAuthorId(UUID authorId) { this.authorId = authorId; }
    public String getChartKey() { return chartKey; }
    public void setChartKey(String chartKey) { this.chartKey = chartKey; }
    public String getAnchorLabel() { return anchorLabel; }
    public void setAnchorLabel(String anchorLabel) { this.anchorLabel = anchorLabel; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
