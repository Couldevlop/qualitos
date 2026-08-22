package com.openlab.qualitos.quality.product.infrastructure;

import com.openlab.qualitos.quality.product.domain.Product;
import com.openlab.qualitos.quality.product.domain.ProductComponent;
import com.openlab.qualitos.quality.product.domain.ProductOperation;
import com.openlab.qualitos.quality.product.domain.ProductStatus;

final class ProductMapper {
    private ProductMapper() {}

    static ProductJpaEntity toEntity(Product p, ProductJpaEntity target) {
        ProductJpaEntity e = target != null ? target : new ProductJpaEntity();
        if (p.getId() != null) e.setId(p.getId());
        e.setTenantId(p.getTenantId());
        e.setCode(p.getCode());
        e.setDesignation(p.getDesignation());
        e.setFamily(p.getFamily());
        e.setRevisionIndex(p.getRevisionIndex());
        e.setStatus(p.getStatus().name());
        e.setCustomerLabel(p.getCustomerLabel());
        e.setSiteLabel(p.getSiteLabel());
        e.setOwnerUserId(p.getOwnerUserId());
        e.setCreatedBy(p.getCreatedBy());
        e.setCreatedAt(p.getCreatedAt());
        e.setUpdatedAt(p.getUpdatedAt());
        return e;
    }

    static Product toDomain(ProductJpaEntity e) {
        return Product.rehydrate(
                e.getId(), e.getTenantId(), e.getCode(), e.getDesignation(),
                e.getFamily(), e.getRevisionIndex(), ProductStatus.valueOf(e.getStatus()),
                e.getCustomerLabel(), e.getSiteLabel(), e.getOwnerUserId(),
                e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }

    static ProductComponentJpaEntity toEntity(ProductComponent c, ProductComponentJpaEntity target) {
        ProductComponentJpaEntity e = target != null ? target : new ProductComponentJpaEntity();
        if (c.getId() != null) e.setId(c.getId());
        e.setTenantId(c.getTenantId());
        e.setProductId(c.getProductId());
        e.setSequenceNo(c.getSequenceNo());
        e.setReference(c.getReference());
        e.setLabel(c.getLabel());
        e.setQuantity(c.getQuantity());
        e.setUnit(c.getUnit());
        e.setSupplierId(c.getSupplierId());
        return e;
    }

    static ProductComponent toDomain(ProductComponentJpaEntity e) {
        return new ProductComponent(
                e.getId(), e.getTenantId(), e.getProductId(), e.getSequenceNo(),
                e.getReference(), e.getLabel(), e.getQuantity(), e.getUnit(), e.getSupplierId());
    }

    static ProductOperationJpaEntity toEntity(ProductOperation o, ProductOperationJpaEntity target) {
        ProductOperationJpaEntity e = target != null ? target : new ProductOperationJpaEntity();
        if (o.getId() != null) e.setId(o.getId());
        e.setTenantId(o.getTenantId());
        e.setProductId(o.getProductId());
        e.setSequenceNo(o.getSequenceNo());
        e.setCode(o.getCode());
        e.setLabel(o.getLabel());
        e.setWorkstation(o.getWorkstation());
        return e;
    }

    static ProductOperation toDomain(ProductOperationJpaEntity e) {
        return new ProductOperation(
                e.getId(), e.getTenantId(), e.getProductId(), e.getSequenceNo(),
                e.getCode(), e.getLabel(), e.getWorkstation());
    }
}
