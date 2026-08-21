package com.openlab.qualitos.quality.product.infrastructure;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "product_operations",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_operations_code",
                columnNames = {"product_id", "code"}),
        indexes = @Index(name = "idx_product_operations_product",
                columnList = "product_id, sequence_no"))
public class ProductOperationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(nullable = false, length = 32)
    private String code;

    @Column(nullable = false, length = 250)
    private String label;

    @Column(length = 120)
    private String workstation;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID v) { this.productId = v; }
    public int getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(int v) { this.sequenceNo = v; }
    public String getCode() { return code; }
    public void setCode(String v) { this.code = v; }
    public String getLabel() { return label; }
    public void setLabel(String v) { this.label = v; }
    public String getWorkstation() { return workstation; }
    public void setWorkstation(String v) { this.workstation = v; }
}
