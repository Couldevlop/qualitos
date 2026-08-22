package com.openlab.qualitos.quality.product.infrastructure;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "product_components",
        indexes = @Index(name = "idx_product_components_product",
                columnList = "product_id, sequence_no"))
public class ProductComponentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(nullable = false, length = 120)
    private String reference;

    @Column(length = 250)
    private String label;

    @Column(precision = 12, scale = 4)
    private BigDecimal quantity;

    @Column(length = 24)
    private String unit;

    @Column(name = "supplier_id")
    private UUID supplierId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID v) { this.productId = v; }
    public int getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(int v) { this.sequenceNo = v; }
    public String getReference() { return reference; }
    public void setReference(String v) { this.reference = v; }
    public String getLabel() { return label; }
    public void setLabel(String v) { this.label = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public String getUnit() { return unit; }
    public void setUnit(String v) { this.unit = v; }
    public UUID getSupplierId() { return supplierId; }
    public void setSupplierId(UUID v) { this.supplierId = v; }
}
