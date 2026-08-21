package com.openlab.qualitos.quality.product.infrastructure;

import com.openlab.qualitos.quality.product.application.TenantProvider;
import com.openlab.qualitos.quality.product.domain.Product;
import com.openlab.qualitos.quality.product.domain.ProductComponent;
import com.openlab.qualitos.quality.product.domain.ProductOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductRepositoryAdapterTest {

    @Mock ProductJpaRepository jpa;
    @Mock ProductComponentJpaRepository jpaComponent;
    @Mock ProductOperationJpaRepository jpaOperation;
    @Mock TenantProvider tenantProvider;

    ProductRepositoryAdapter adapter;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-08-19T08:00:00Z");

    @BeforeEach
    void setup() {
        adapter = new ProductRepositoryAdapter(jpa, jpaComponent, jpaOperation, tenantProvider);
    }

    private Product product() {
        return Product.create(TENANT, "REF-1", "Piece", USER, NOW);
    }

    // --- Product ---

    @Test
    void save_newProduct_persists() {
        Product p = product();
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpa.save(any())).thenAnswer(inv -> {
            ProductJpaEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        Product out = adapter.save(p);

        assertThat(out.getId()).isNotNull();
        verify(jpa, never()).findByIdAndTenantId(any(), any());
    }

    @Test
    void save_existingProduct_reloadsTarget() {
        Product p = product();
        p.assignId(UUID.randomUUID());
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpa.findByIdAndTenantId(p.getId(), TENANT))
                .thenReturn(Optional.of(entityOf(p)));
        when(jpa.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product out = adapter.save(p);

        assertThat(out.getId()).isEqualTo(p.getId());
        verify(jpa).findByIdAndTenantId(p.getId(), TENANT);
    }

    @Test
    void save_crossTenant_rejected() {
        Product p = Product.create(UUID.randomUUID(), "REF-1", "Piece", USER, NOW);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);

        assertThatThrownBy(() -> adapter.save(p))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cross-tenant");
    }

    @Test
    void findById_present() {
        Product p = product();
        p.assignId(UUID.randomUUID());
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpa.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(entityOf(p)));

        assertThat(adapter.findById(p.getId())).isPresent();
    }

    @Test
    void findById_absent() {
        UUID id = UUID.randomUUID();
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpa.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());

        assertThat(adapter.findById(id)).isEmpty();
    }

    @Test
    void findByTenant_mapsResults() {
        Product p = product();
        p.assignId(UUID.randomUUID());
        when(jpa.findByTenantIdOrderByCodeAsc(TENANT)).thenReturn(List.of(entityOf(p)));

        assertThat(adapter.findByTenant(TENANT)).hasSize(1);
    }

    @Test
    void existsByTenantAndCode_delegates() {
        when(jpa.existsByTenantIdAndCode(TENANT, "REF-1")).thenReturn(true);

        assertThat(adapter.existsByTenantAndCode(TENANT, "REF-1")).isTrue();
    }

    @Test
    void delete_removesWhenFoundInTenant() {
        UUID id = UUID.randomUUID();
        Product p = product();
        p.assignId(id);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpa.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.of(entityOf(p)));

        adapter.delete(id);

        verify(jpa).delete(any(ProductJpaEntity.class));
    }

    @Test
    void delete_noOpWhenNotFoundInTenant() {
        UUID id = UUID.randomUUID();
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpa.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());

        adapter.delete(id);

        verify(jpa, never()).delete(any(ProductJpaEntity.class));
    }

    // --- ProductComponent ---

    private ProductComponent component(UUID tenant, UUID productId) {
        return new ProductComponent(null, tenant, productId, 1, "BOLT-M6", "Vis M6",
                BigDecimal.ONE, "pcs", null);
    }

    @Test
    void saveComponent_new_persists() {
        UUID productId = UUID.randomUUID();
        ProductComponent c = component(TENANT, productId);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpaComponent.save(any())).thenAnswer(inv -> {
            ProductComponentJpaEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        ProductComponent out = adapter.saveComponent(c);

        assertThat(out.getId()).isNotNull();
    }

    @Test
    void saveComponent_existing_reloadsTarget() {
        UUID productId = UUID.randomUUID();
        ProductComponent c = component(TENANT, productId);
        c.assignId(UUID.randomUUID());
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpaComponent.findByIdAndTenantId(c.getId(), TENANT))
                .thenReturn(Optional.of(componentEntityOf(c)));
        when(jpaComponent.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProductComponent out = adapter.saveComponent(c);

        assertThat(out.getId()).isEqualTo(c.getId());
        verify(jpaComponent).findByIdAndTenantId(c.getId(), TENANT);
    }

    @Test
    void saveComponent_crossTenant_rejected() {
        ProductComponent c = component(UUID.randomUUID(), UUID.randomUUID());
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);

        assertThatThrownBy(() -> adapter.saveComponent(c))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cross-tenant");
    }

    @Test
    void componentsOf_scopedByTenant() {
        UUID productId = UUID.randomUUID();
        ProductComponent c = component(TENANT, productId);
        c.assignId(UUID.randomUUID());
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpaComponent.findByProductIdAndTenantIdOrderBySequenceNoAsc(productId, TENANT))
                .thenReturn(List.of(componentEntityOf(c)));

        assertThat(adapter.componentsOf(productId)).hasSize(1);
    }

    @Test
    void findComponent_present() {
        UUID productId = UUID.randomUUID();
        ProductComponent c = component(TENANT, productId);
        c.assignId(UUID.randomUUID());
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpaComponent.findByIdAndTenantId(c.getId(), TENANT))
                .thenReturn(Optional.of(componentEntityOf(c)));

        assertThat(adapter.findComponent(c.getId())).isPresent();
    }

    @Test
    void deleteComponent_removesWhenFoundInTenant() {
        UUID id = UUID.randomUUID();
        ProductComponent c = component(TENANT, UUID.randomUUID());
        c.assignId(id);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpaComponent.findByIdAndTenantId(id, TENANT))
                .thenReturn(Optional.of(componentEntityOf(c)));

        adapter.deleteComponent(id);

        verify(jpaComponent).delete(any(ProductComponentJpaEntity.class));
    }

    @Test
    void deleteComponent_noOpWhenNotFoundInTenant() {
        UUID id = UUID.randomUUID();
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpaComponent.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());

        adapter.deleteComponent(id);

        verify(jpaComponent, never()).delete(any(ProductComponentJpaEntity.class));
    }

    // --- ProductOperation ---

    private ProductOperation operation(UUID tenant, UUID productId) {
        return new ProductOperation(null, tenant, productId, 10, "OP-010", "Perçage", "Poste 3");
    }

    @Test
    void saveOperation_new_persists() {
        UUID productId = UUID.randomUUID();
        ProductOperation o = operation(TENANT, productId);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpaOperation.save(any())).thenAnswer(inv -> {
            ProductOperationJpaEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        ProductOperation out = adapter.saveOperation(o);

        assertThat(out.getId()).isNotNull();
    }

    @Test
    void saveOperation_existing_reloadsTarget() {
        UUID productId = UUID.randomUUID();
        ProductOperation o = operation(TENANT, productId);
        o.assignId(UUID.randomUUID());
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpaOperation.findByIdAndTenantId(o.getId(), TENANT))
                .thenReturn(Optional.of(operationEntityOf(o)));
        when(jpaOperation.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProductOperation out = adapter.saveOperation(o);

        assertThat(out.getId()).isEqualTo(o.getId());
        verify(jpaOperation).findByIdAndTenantId(o.getId(), TENANT);
    }

    @Test
    void saveOperation_crossTenant_rejected() {
        ProductOperation o = operation(UUID.randomUUID(), UUID.randomUUID());
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);

        assertThatThrownBy(() -> adapter.saveOperation(o))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cross-tenant");
    }

    @Test
    void operationsOf_scopedByTenant() {
        UUID productId = UUID.randomUUID();
        ProductOperation o = operation(TENANT, productId);
        o.assignId(UUID.randomUUID());
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpaOperation.findByProductIdAndTenantIdOrderBySequenceNoAsc(productId, TENANT))
                .thenReturn(List.of(operationEntityOf(o)));

        assertThat(adapter.operationsOf(productId)).hasSize(1);
    }

    @Test
    void findOperation_present() {
        UUID productId = UUID.randomUUID();
        ProductOperation o = operation(TENANT, productId);
        o.assignId(UUID.randomUUID());
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpaOperation.findByIdAndTenantId(o.getId(), TENANT))
                .thenReturn(Optional.of(operationEntityOf(o)));

        assertThat(adapter.findOperation(o.getId())).isPresent();
    }

    @Test
    void deleteOperation_removesWhenFoundInTenant() {
        UUID id = UUID.randomUUID();
        ProductOperation o = operation(TENANT, UUID.randomUUID());
        o.assignId(id);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpaOperation.findByIdAndTenantId(id, TENANT))
                .thenReturn(Optional.of(operationEntityOf(o)));

        adapter.deleteOperation(id);

        verify(jpaOperation).delete(any(ProductOperationJpaEntity.class));
    }

    @Test
    void deleteOperation_noOpWhenNotFoundInTenant() {
        UUID id = UUID.randomUUID();
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpaOperation.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());

        adapter.deleteOperation(id);

        verify(jpaOperation, never()).delete(any(ProductOperationJpaEntity.class));
    }

    // --- helpers: build a JPA entity mirroring a domain object, without exercising ProductMapper ---

    private static ProductJpaEntity entityOf(Product p) {
        ProductJpaEntity e = new ProductJpaEntity();
        e.setId(p.getId());
        e.setTenantId(p.getTenantId());
        e.setCode(p.getCode());
        e.setDesignation(p.getDesignation());
        e.setStatus(p.getStatus().name());
        e.setCreatedBy(p.getCreatedBy());
        e.setCreatedAt(p.getCreatedAt());
        e.setUpdatedAt(p.getUpdatedAt());
        return e;
    }

    private static ProductComponentJpaEntity componentEntityOf(ProductComponent c) {
        ProductComponentJpaEntity e = new ProductComponentJpaEntity();
        e.setId(c.getId());
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

    private static ProductOperationJpaEntity operationEntityOf(ProductOperation o) {
        ProductOperationJpaEntity e = new ProductOperationJpaEntity();
        e.setId(o.getId());
        e.setTenantId(o.getTenantId());
        e.setProductId(o.getProductId());
        e.setSequenceNo(o.getSequenceNo());
        e.setCode(o.getCode());
        e.setLabel(o.getLabel());
        e.setWorkstation(o.getWorkstation());
        return e;
    }
}
