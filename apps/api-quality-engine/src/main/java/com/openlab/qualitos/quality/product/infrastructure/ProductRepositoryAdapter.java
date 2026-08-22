package com.openlab.qualitos.quality.product.infrastructure;

import com.openlab.qualitos.quality.product.application.TenantProvider;
import com.openlab.qualitos.quality.product.domain.Product;
import com.openlab.qualitos.quality.product.domain.ProductComponent;
import com.openlab.qualitos.quality.product.domain.ProductOperation;
import com.openlab.qualitos.quality.product.domain.ProductRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository jpa;
    private final ProductComponentJpaRepository jpaComponent;
    private final ProductOperationJpaRepository jpaOperation;
    private final TenantProvider tenantProvider;

    public ProductRepositoryAdapter(
            ProductJpaRepository jpa,
            ProductComponentJpaRepository jpaComponent,
            ProductOperationJpaRepository jpaOperation,
            @Qualifier("productTenantContextProvider") TenantProvider tenantProvider) {
        this.jpa = jpa;
        this.jpaComponent = jpaComponent;
        this.jpaOperation = jpaOperation;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public Product save(Product product) {
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(product.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        ProductJpaEntity existing = product.getId() != null
                ? jpa.findByIdAndTenantId(product.getId(), currentTenant).orElse(null)
                : null;
        ProductJpaEntity saved = jpa.save(ProductMapper.toEntity(product, existing));
        Product out = ProductMapper.toDomain(saved);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<Product> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant).map(ProductMapper::toDomain);
    }

    @Override
    public List<Product> findByTenant(UUID tenantId) {
        return jpa.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .map(ProductMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByTenantAndCode(UUID tenantId, String code) {
        return jpa.existsByTenantIdAndCode(tenantId, code);
    }

    @Override
    public void delete(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        jpa.findByIdAndTenantId(id, tenant).ifPresent(jpa::delete);
    }

    @Override
    public ProductComponent saveComponent(ProductComponent component) {
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(component.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        ProductComponentJpaEntity existing = component.getId() != null
                ? jpaComponent.findByIdAndTenantId(component.getId(), currentTenant).orElse(null)
                : null;
        ProductComponentJpaEntity saved =
                jpaComponent.save(ProductMapper.toEntity(component, existing));
        ProductComponent out = ProductMapper.toDomain(saved);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public List<ProductComponent> componentsOf(UUID productId) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpaComponent.findByProductIdAndTenantIdOrderBySequenceNoAsc(productId, tenant)
                .stream()
                .map(ProductMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ProductComponent> findComponent(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpaComponent.findByIdAndTenantId(id, tenant).map(ProductMapper::toDomain);
    }

    @Override
    public void deleteComponent(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        jpaComponent.findByIdAndTenantId(id, tenant).ifPresent(jpaComponent::delete);
    }

    @Override
    public ProductOperation saveOperation(ProductOperation operation) {
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(operation.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        ProductOperationJpaEntity existing = operation.getId() != null
                ? jpaOperation.findByIdAndTenantId(operation.getId(), currentTenant).orElse(null)
                : null;
        ProductOperationJpaEntity saved =
                jpaOperation.save(ProductMapper.toEntity(operation, existing));
        ProductOperation out = ProductMapper.toDomain(saved);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public List<ProductOperation> operationsOf(UUID productId) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpaOperation.findByProductIdAndTenantIdOrderBySequenceNoAsc(productId, tenant)
                .stream()
                .map(ProductMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ProductOperation> findOperation(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpaOperation.findByIdAndTenantId(id, tenant).map(ProductMapper::toDomain);
    }

    @Override
    public void deleteOperation(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        jpaOperation.findByIdAndTenantId(id, tenant).ifPresent(jpaOperation::delete);
    }
}
