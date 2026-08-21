package com.openlab.qualitos.quality.product.infrastructure;

import com.openlab.qualitos.quality.product.domain.Product;
import com.openlab.qualitos.quality.product.domain.ProductComponent;
import com.openlab.qualitos.quality.product.domain.ProductOperation;
import com.openlab.qualitos.quality.product.domain.ProductStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID OWNER = UUID.randomUUID();
    static final Instant CREATED = Instant.parse("2026-05-16T10:00:00Z");
    static final Instant UPDATED = Instant.parse("2026-06-01T09:30:00Z");

    @Test
    void roundtrip_fullyPopulatedObsoleteProduct_preservesEveryField() throws Exception {
        Product p = Product.rehydrate(
                UUID.randomUUID(), TENANT, "REF-4471", "Support moteur",
                "Chassis", "B", ProductStatus.OBSOLETE,
                "Client X", "Site Nord", OWNER,
                USER, CREATED, UPDATED);

        ProductJpaEntity e = invokeToEntity(p, null);
        assertThat(e.getId()).isEqualTo(p.getId());
        assertThat(e.getTenantId()).isEqualTo(TENANT);
        assertThat(e.getCode()).isEqualTo("REF-4471");
        assertThat(e.getDesignation()).isEqualTo("Support moteur");
        assertThat(e.getFamily()).isEqualTo("Chassis");
        assertThat(e.getRevisionIndex()).isEqualTo("B");
        assertThat(e.getStatus()).isEqualTo("OBSOLETE");
        assertThat(e.getCustomerLabel()).isEqualTo("Client X");
        assertThat(e.getSiteLabel()).isEqualTo("Site Nord");
        assertThat(e.getOwnerUserId()).isEqualTo(OWNER);
        assertThat(e.getCreatedBy()).isEqualTo(USER);
        assertThat(e.getCreatedAt()).isEqualTo(CREATED);
        assertThat(e.getUpdatedAt()).isEqualTo(UPDATED);

        Product back = invokeToDomain(e);
        assertThat(back.getId()).isEqualTo(p.getId());
        assertThat(back.getTenantId()).isEqualTo(TENANT);
        assertThat(back.getCode()).isEqualTo("REF-4471");
        assertThat(back.getDesignation()).isEqualTo("Support moteur");
        assertThat(back.getFamily()).isEqualTo("Chassis");
        assertThat(back.getRevisionIndex()).isEqualTo("B");
        assertThat(back.getStatus()).isEqualTo(ProductStatus.OBSOLETE);
        assertThat(back.getCustomerLabel()).isEqualTo("Client X");
        assertThat(back.getSiteLabel()).isEqualTo("Site Nord");
        assertThat(back.getOwnerUserId()).isEqualTo(OWNER);
        assertThat(back.getCreatedBy()).isEqualTo(USER);
        assertThat(back.getCreatedAt()).isEqualTo(CREATED);
        assertThat(back.getUpdatedAt()).isEqualTo(UPDATED);
    }

    @Test
    void roundtrip_nullOptionalFields_surviveTheTrip() throws Exception {
        Product p = Product.create(TENANT, "REF-1", "Piece", USER, CREATED);

        ProductJpaEntity e = invokeToEntity(p, null);
        assertThat(e.getFamily()).isNull();
        assertThat(e.getCustomerLabel()).isNull();
        assertThat(e.getSiteLabel()).isNull();
        assertThat(e.getOwnerUserId()).isNull();
        assertThat(e.getRevisionIndex()).isNull();

        Product back = invokeToDomain(e);
        assertThat(back.getFamily()).isNull();
        assertThat(back.getCustomerLabel()).isNull();
        assertThat(back.getSiteLabel()).isNull();
        assertThat(back.getOwnerUserId()).isNull();
        assertThat(back.getRevisionIndex()).isNull();
        assertThat(back.getStatus()).isEqualTo(ProductStatus.DRAFT);
    }

    @Test
    void roundtrip_component_withSupplier_preservesEveryField() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        ProductComponent c = new ProductComponent(UUID.randomUUID(), TENANT, productId, 3,
                "BOLT-M6", "Vis M6", new BigDecimal("2.5000"), "pcs", supplierId);

        ProductComponentJpaEntity e = invokeComponentToEntity(c, null);
        assertThat(e.getId()).isEqualTo(c.getId());
        assertThat(e.getTenantId()).isEqualTo(TENANT);
        assertThat(e.getProductId()).isEqualTo(productId);
        assertThat(e.getSequenceNo()).isEqualTo(3);
        assertThat(e.getReference()).isEqualTo("BOLT-M6");
        assertThat(e.getLabel()).isEqualTo("Vis M6");
        assertThat(e.getQuantity()).isEqualByComparingTo("2.5000");
        assertThat(e.getUnit()).isEqualTo("pcs");
        assertThat(e.getSupplierId()).isEqualTo(supplierId);

        ProductComponent back = invokeComponentToDomain(e);
        assertThat(back.getId()).isEqualTo(c.getId());
        assertThat(back.getTenantId()).isEqualTo(TENANT);
        assertThat(back.getProductId()).isEqualTo(productId);
        assertThat(back.getSequenceNo()).isEqualTo(3);
        assertThat(back.getReference()).isEqualTo("BOLT-M6");
        assertThat(back.getLabel()).isEqualTo("Vis M6");
        assertThat(back.getQuantity()).isEqualByComparingTo("2.5000");
        assertThat(back.getUnit()).isEqualTo("pcs");
        assertThat(back.getSupplierId()).isEqualTo(supplierId);
    }

    @Test
    void roundtrip_component_withoutSupplier_survivesTheTrip() throws Exception {
        ProductComponent c = new ProductComponent(null, TENANT, UUID.randomUUID(), 1,
                "PLATE-01", "Plaque", BigDecimal.ONE, "pcs", null);

        ProductComponentJpaEntity e = invokeComponentToEntity(c, null);
        assertThat(e.getSupplierId()).isNull();

        ProductComponent back = invokeComponentToDomain(e);
        assertThat(back.getSupplierId()).isNull();
    }

    @Test
    void roundtrip_operation_preservesEveryField() throws Exception {
        UUID productId = UUID.randomUUID();
        ProductOperation o = new ProductOperation(UUID.randomUUID(), TENANT, productId, 10,
                "OP-010", "Perçage", "Poste 3");

        ProductOperationJpaEntity e = invokeOperationToEntity(o, null);
        assertThat(e.getId()).isEqualTo(o.getId());
        assertThat(e.getTenantId()).isEqualTo(TENANT);
        assertThat(e.getProductId()).isEqualTo(productId);
        assertThat(e.getSequenceNo()).isEqualTo(10);
        assertThat(e.getCode()).isEqualTo("OP-010");
        assertThat(e.getLabel()).isEqualTo("Perçage");
        assertThat(e.getWorkstation()).isEqualTo("Poste 3");

        ProductOperation back = invokeOperationToDomain(e);
        assertThat(back.getId()).isEqualTo(o.getId());
        assertThat(back.getTenantId()).isEqualTo(TENANT);
        assertThat(back.getProductId()).isEqualTo(productId);
        assertThat(back.getSequenceNo()).isEqualTo(10);
        assertThat(back.getCode()).isEqualTo("OP-010");
        assertThat(back.getLabel()).isEqualTo("Perçage");
        assertThat(back.getWorkstation()).isEqualTo("Poste 3");
    }

    @Test
    void roundtrip_operation_withoutWorkstation_survivesTheTrip() throws Exception {
        ProductOperation o = new ProductOperation(null, TENANT, UUID.randomUUID(), 1,
                "OP-001", "Contrôle", null);

        ProductOperationJpaEntity e = invokeOperationToEntity(o, null);
        assertThat(e.getWorkstation()).isNull();

        ProductOperation back = invokeOperationToDomain(e);
        assertThat(back.getWorkstation()).isNull();
    }

    private static ProductJpaEntity invokeToEntity(Product p, ProductJpaEntity target) throws Exception {
        Method m = ProductMapper.class.getDeclaredMethod(
                "toEntity", Product.class, ProductJpaEntity.class);
        m.setAccessible(true);
        return (ProductJpaEntity) m.invoke(null, p, target);
    }

    private static Product invokeToDomain(ProductJpaEntity e) throws Exception {
        Method m = ProductMapper.class.getDeclaredMethod("toDomain", ProductJpaEntity.class);
        m.setAccessible(true);
        return (Product) m.invoke(null, e);
    }

    private static ProductComponentJpaEntity invokeComponentToEntity(
            ProductComponent c, ProductComponentJpaEntity target) throws Exception {
        Method m = ProductMapper.class.getDeclaredMethod(
                "toEntity", ProductComponent.class, ProductComponentJpaEntity.class);
        m.setAccessible(true);
        return (ProductComponentJpaEntity) m.invoke(null, c, target);
    }

    private static ProductComponent invokeComponentToDomain(ProductComponentJpaEntity e) throws Exception {
        Method m = ProductMapper.class.getDeclaredMethod("toDomain", ProductComponentJpaEntity.class);
        m.setAccessible(true);
        return (ProductComponent) m.invoke(null, e);
    }

    private static ProductOperationJpaEntity invokeOperationToEntity(
            ProductOperation o, ProductOperationJpaEntity target) throws Exception {
        Method m = ProductMapper.class.getDeclaredMethod(
                "toEntity", ProductOperation.class, ProductOperationJpaEntity.class);
        m.setAccessible(true);
        return (ProductOperationJpaEntity) m.invoke(null, o, target);
    }

    private static ProductOperation invokeOperationToDomain(ProductOperationJpaEntity e) throws Exception {
        Method m = ProductMapper.class.getDeclaredMethod("toDomain", ProductOperationJpaEntity.class);
        m.setAccessible(true);
        return (ProductOperation) m.invoke(null, e);
    }
}
