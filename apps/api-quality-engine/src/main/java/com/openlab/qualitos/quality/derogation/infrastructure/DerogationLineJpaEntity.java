package com.openlab.qualitos.quality.derogation.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Une ligne d'ecart telle qu'elle est stockee.
 *
 * <p>La contrainte d'unicite sur (dossier, rang) est DECLAREE ici et pas
 * seulement dans la migration : sans elle, le banc sur H2 ne verrait pas une
 * collision de rang que PostgreSQL refuserait en production. Une contrainte que
 * les tests ne connaissent pas ne protege que la prod, et trop tard.
 */
@Entity
@Table(name = "derogation_deviation_lines",
        uniqueConstraints = @UniqueConstraint(name = "uq_derogation_lines_position",
                columnNames = {"derogation_id", "position"}),
        indexes = @Index(name = "ix_derogation_lines_dossier",
                columnList = "tenant_id, derogation_id, position"))
public class DerogationLineJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "derogation_id", nullable = false, updatable = false)
    private UUID derogationId;

    @Column(nullable = false)
    private short position;

    @Column(length = 500)
    private String specification;

    @Column(name = "actual_value", length = 500)
    private String actualValue;

    @Column(name = "non_conforming_qty", precision = 18, scale = 3)
    private BigDecimal nonConformingQty;

    public UUID getId() {
        return id;
    }

    public void setId(UUID v) {
        this.id = v;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID v) {
        this.tenantId = v;
    }

    public UUID getDerogationId() {
        return derogationId;
    }

    public void setDerogationId(UUID v) {
        this.derogationId = v;
    }

    public short getPosition() {
        return position;
    }

    public void setPosition(short v) {
        this.position = v;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String v) {
        this.specification = v;
    }

    public String getActualValue() {
        return actualValue;
    }

    public void setActualValue(String v) {
        this.actualValue = v;
    }

    public BigDecimal getNonConformingQty() {
        return nonConformingQty;
    }

    public void setNonConformingQty(BigDecimal v) {
        this.nonConformingQty = v;
    }
}
