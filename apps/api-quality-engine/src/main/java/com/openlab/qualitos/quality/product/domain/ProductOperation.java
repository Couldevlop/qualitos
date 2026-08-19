package com.openlab.qualitos.quality.product.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Opération de la gamme. C'est elle que désignent une ligne de PFMEA et une ligne
 * de Control Plan : sans elle, les deux documents parlent d'un « poste » en texte
 * libre et ne peuvent pas être recoupés.
 */
public final class ProductOperation {

    private UUID id;
    private final UUID tenantId;
    private final UUID productId;
    private int sequenceNo;
    private String code;
    private String label;
    private String workstation;

    public ProductOperation(UUID id, UUID tenantId, UUID productId, int sequenceNo,
                            String code, String label, String workstation) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.productId = Objects.requireNonNull(productId, "productId");
        this.sequenceNo = sequenceNo;
        this.code = require(code, "code", 32);
        this.label = require(label, "label", 250);
        this.workstation = workstation;
    }

    private static String require(String value, String field, int max) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty() || trimmed.length() > max) {
            throw new IllegalArgumentException("Invalid operation " + field);
        }
        return trimmed;
    }

    public void assignId(UUID id) { this.id = id; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getProductId() { return productId; }
    public int getSequenceNo() { return sequenceNo; }
    public String getCode() { return code; }
    public String getLabel() { return label; }
    public String getWorkstation() { return workstation; }
}
