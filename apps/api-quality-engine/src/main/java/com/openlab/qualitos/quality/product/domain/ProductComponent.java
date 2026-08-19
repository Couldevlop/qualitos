package com.openlab.qualitos.quality.product.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Ligne de nomenclature. Le fournisseur est optionnel : une pièce peut être fabriquée en interne. */
public final class ProductComponent {

    private UUID id;
    private final UUID tenantId;
    private final UUID productId;
    private int sequenceNo;
    private String reference;
    private String label;
    private BigDecimal quantity;
    private String unit;
    private UUID supplierId;

    public ProductComponent(UUID id, UUID tenantId, UUID productId, int sequenceNo,
                            String reference, String label, BigDecimal quantity,
                            String unit, UUID supplierId) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.productId = Objects.requireNonNull(productId, "productId");
        this.sequenceNo = sequenceNo;
        this.reference = require(reference, "reference", 120);
        this.label = label;
        this.quantity = quantity;
        this.unit = unit;
        this.supplierId = supplierId;
    }

    private static String require(String value, String field, int max) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty() || trimmed.length() > max) {
            throw new IllegalArgumentException("Invalid component " + field);
        }
        return trimmed;
    }

    public void assignId(UUID id) { this.id = id; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getProductId() { return productId; }
    public int getSequenceNo() { return sequenceNo; }
    public String getReference() { return reference; }
    public String getLabel() { return label; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public UUID getSupplierId() { return supplierId; }
}
