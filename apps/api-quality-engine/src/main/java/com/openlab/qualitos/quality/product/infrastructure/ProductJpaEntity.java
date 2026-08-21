package com.openlab.qualitos.quality.product.infrastructure;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products",
        uniqueConstraints = @UniqueConstraint(name = "uk_products_tenant_code",
                columnNames = {"tenant_id", "code"}),
        indexes = @Index(name = "idx_products_tenant_status", columnList = "tenant_id, status"))
public class ProductJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 250)
    private String designation;

    @Column(length = 120)
    private String family;

    @Column(name = "revision_index", length = 16)
    private String revisionIndex;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "customer_label", length = 250)
    private String customerLabel;

    @Column(name = "site_label", length = 250)
    private String siteLabel;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public String getCode() { return code; }
    public void setCode(String v) { this.code = v; }
    public String getDesignation() { return designation; }
    public void setDesignation(String v) { this.designation = v; }
    public String getFamily() { return family; }
    public void setFamily(String v) { this.family = v; }
    public String getRevisionIndex() { return revisionIndex; }
    public void setRevisionIndex(String v) { this.revisionIndex = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getCustomerLabel() { return customerLabel; }
    public void setCustomerLabel(String v) { this.customerLabel = v; }
    public String getSiteLabel() { return siteLabel; }
    public void setSiteLabel(String v) { this.siteLabel = v; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(UUID v) { this.ownerUserId = v; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID v) { this.createdBy = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
