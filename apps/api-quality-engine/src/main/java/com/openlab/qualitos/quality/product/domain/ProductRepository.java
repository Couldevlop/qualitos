package com.openlab.qualitos.quality.product.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends ProductLookup {

    Product save(Product product);

    List<Product> findByTenant(UUID tenantId);

    boolean existsByTenantAndCode(UUID tenantId, String code);

    void delete(UUID id);

    ProductComponent saveComponent(ProductComponent component);

    List<ProductComponent> componentsOf(UUID productId);

    Optional<ProductComponent> findComponent(UUID id);

    void deleteComponent(UUID id);

    ProductOperation saveOperation(ProductOperation operation);

    Optional<ProductOperation> findOperation(UUID id);

    void deleteOperation(UUID id);
}
