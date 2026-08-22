package com.openlab.qualitos.quality.product.application;

import com.openlab.qualitos.quality.product.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID OTHER_TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-08-19T08:00:00Z");

    ProductRepository repo;
    TenantProvider tenants;
    ActorProvider actors;
    ProductService service;

    @BeforeEach
    void setUp() {
        repo = mock(ProductRepository.class);
        tenants = mock(TenantProvider.class);
        actors = mock(ActorProvider.class);
        when(tenants.requireTenantId()).thenReturn(TENANT);
        when(actors.currentUserId()).thenReturn(USER);
        service = new ProductService(repo, tenants, actors,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void creatingAProductRefusesACodeAlreadyTaken() {
        when(repo.existsByTenantAndCode(TENANT, "REF-4471")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new ProductDto.CreateCommand("REF-4471", "Support moteur", null, null, null, null, null)))
                .isInstanceOf(ProductCodeConflictException.class);

        verify(repo, never()).save(any());
    }

    @Test
    void theTenantComesFromTheContextNeverFromTheCommand() {
        when(repo.existsByTenantAndCode(TENANT, "REF-4471")).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.assignId(UUID.randomUUID());
            return p;
        });

        service.create(new ProductDto.CreateCommand(
                "REF-4471", "Support moteur", "Mécanique", "A", "Renault", "Site 2", USER));

        verify(repo).save(argThat(p -> p.getTenantId().equals(TENANT)));
    }

    @Test
    void aProductOfAnotherTenantIsNotFoundNeverForbidden() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void addingAComponentToAProductOfAnotherTenantIsRefused() {
        UUID id = UUID.randomUUID();
        Product foreign = Product.rehydrate(id, OTHER_TENANT, "REF-9", "Autre", null, null,
                ProductStatus.ACTIVE, null, null, null, USER, NOW, NOW);
        when(repo.findById(id)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.addComponent(id,
                new ProductDto.ComponentCommand(10, "CMP-1", "Vis", null, null, null)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void listingReturnsOnlyTheCurrentTenantProducts() {
        when(repo.findByTenant(TENANT)).thenReturn(List.of());

        service.list();

        verify(repo).findByTenant(TENANT);
    }

    // -------- couverture complémentaire : le reste de la surface du service --------

    @Test
    void getReturnsTheMappedView() {
        UUID id = UUID.randomUUID();
        Product product = ownProduct(id, ProductStatus.ACTIVE);
        when(repo.findById(id)).thenReturn(Optional.of(product));

        ProductDto.View view = service.get(id);

        assertThat(view.id()).isEqualTo(id);
        assertThat(view.code()).isEqualTo("REF-1");
    }

    @Test
    void updateRenamesAndDescribesThenSaves() {
        UUID id = UUID.randomUUID();
        Product product = ownProduct(id, ProductStatus.DRAFT);
        when(repo.findById(id)).thenReturn(Optional.of(product));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProductDto.View view = service.update(id, new ProductDto.UpdateCommand(
                "Nouveau nom", "Mécanique", "B", "Renault", "Site 3", USER));

        assertThat(view.designation()).isEqualTo("Nouveau nom");
        assertThat(view.family()).isEqualTo("Mécanique");
        verify(repo).save(product);
    }

    @Test
    void activateMovesStatusToActiveAndSaves() {
        UUID id = UUID.randomUUID();
        Product product = ownProduct(id, ProductStatus.DRAFT);
        when(repo.findById(id)).thenReturn(Optional.of(product));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProductDto.View view = service.activate(id);

        assertThat(view.status()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void markObsoleteMovesStatusToObsoleteAndSaves() {
        UUID id = UUID.randomUUID();
        Product product = ownProduct(id, ProductStatus.ACTIVE);
        when(repo.findById(id)).thenReturn(Optional.of(product));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProductDto.View view = service.markObsolete(id);

        assertThat(view.status()).isEqualTo(ProductStatus.OBSOLETE);
    }

    @Test
    void deleteChecksOwnershipBeforeDelegatingToTheRepository() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(ownProduct(id, ProductStatus.DRAFT)));

        service.delete(id);

        verify(repo).delete(id);
    }

    @Test
    void deleteOnAForeignProductNeverReachesTheRepository() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(
                Product.rehydrate(id, OTHER_TENANT, "REF-9", "Autre", null, null,
                        ProductStatus.ACTIVE, null, null, null, USER, NOW, NOW)));

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ProductNotFoundException.class);

        verify(repo, never()).delete(any());
    }

    @Test
    void componentsListsTheOwnedProductComponents() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(ownProduct(id, ProductStatus.ACTIVE)));
        ProductComponent component = new ProductComponent(UUID.randomUUID(), TENANT, id,
                10, "CMP-1", "Vis", BigDecimal.ONE, "PCE", null);
        when(repo.componentsOf(id)).thenReturn(List.of(component));

        List<ProductDto.ComponentView> views = service.components(id);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).reference()).isEqualTo("CMP-1");
    }

    @Test
    void addComponentSavesUnderTheParentProduct() {
        UUID id = UUID.randomUUID();
        Product product = ownProduct(id, ProductStatus.ACTIVE);
        when(repo.findById(id)).thenReturn(Optional.of(product));
        when(repo.saveComponent(any())).thenAnswer(inv -> {
            ProductComponent c = inv.getArgument(0);
            c.assignId(UUID.randomUUID());
            return c;
        });

        ProductDto.ComponentView view = service.addComponent(id,
                new ProductDto.ComponentCommand(10, "CMP-1", "Vis", BigDecimal.TEN, "PCE", null));

        assertThat(view.id()).isNotNull();
        verify(repo).saveComponent(argThat(c ->
                c.getTenantId().equals(TENANT) && c.getProductId().equals(id)));
    }

    @Test
    void updateComponentReplacesTheExistingLine() {
        UUID productId = UUID.randomUUID();
        UUID componentId = UUID.randomUUID();
        when(repo.findById(productId)).thenReturn(Optional.of(ownProduct(productId, ProductStatus.ACTIVE)));
        ProductComponent existing = new ProductComponent(componentId, TENANT, productId,
                10, "CMP-1", "Vis", null, null, null);
        when(repo.findComponent(componentId)).thenReturn(Optional.of(existing));
        when(repo.saveComponent(any())).thenAnswer(inv -> inv.getArgument(0));

        ProductDto.ComponentView view = service.updateComponent(productId, componentId,
                new ProductDto.ComponentCommand(20, "CMP-2", "Écrou", null, null, null));

        assertThat(view.reference()).isEqualTo("CMP-2");
        assertThat(view.sequenceNo()).isEqualTo(20);
    }

    @Test
    void updateComponentOfAnUnrelatedProductIsRefused() {
        UUID productId = UUID.randomUUID();
        UUID otherProductId = UUID.randomUUID();
        UUID componentId = UUID.randomUUID();
        when(repo.findById(productId)).thenReturn(Optional.of(ownProduct(productId, ProductStatus.ACTIVE)));
        ProductComponent foreignComponent = new ProductComponent(componentId, TENANT, otherProductId,
                10, "CMP-1", "Vis", null, null, null);
        when(repo.findComponent(componentId)).thenReturn(Optional.of(foreignComponent));

        assertThatThrownBy(() -> service.updateComponent(productId, componentId,
                new ProductDto.ComponentCommand(10, "CMP-1", "Vis", null, null, null)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void updateComponentThatDoesNotExistIsRefused() {
        UUID productId = UUID.randomUUID();
        UUID componentId = UUID.randomUUID();
        when(repo.findById(productId)).thenReturn(Optional.of(ownProduct(productId, ProductStatus.ACTIVE)));
        when(repo.findComponent(componentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateComponent(productId, componentId,
                new ProductDto.ComponentCommand(10, "CMP-1", "Vis", null, null, null)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void deleteComponentChecksTheFullOwnershipChain() {
        UUID productId = UUID.randomUUID();
        UUID componentId = UUID.randomUUID();
        when(repo.findById(productId)).thenReturn(Optional.of(ownProduct(productId, ProductStatus.ACTIVE)));
        when(repo.findComponent(componentId)).thenReturn(Optional.of(
                new ProductComponent(componentId, TENANT, productId, 10, "CMP-1", "Vis", null, null, null)));

        service.deleteComponent(productId, componentId);

        verify(repo).deleteComponent(componentId);
    }

    @Test
    void operationsListsTheOwnedProductOperations() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(ownProduct(id, ProductStatus.ACTIVE)));
        ProductOperation operation = new ProductOperation(UUID.randomUUID(), TENANT, id,
                10, "OP-10", "Perçage", "Poste 1");
        when(repo.operationsOf(id)).thenReturn(List.of(operation));

        List<ProductDto.OperationView> views = service.operations(id);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).code()).isEqualTo("OP-10");
    }

    @Test
    void addOperationSavesUnderTheParentProduct() {
        UUID id = UUID.randomUUID();
        Product product = ownProduct(id, ProductStatus.ACTIVE);
        when(repo.findById(id)).thenReturn(Optional.of(product));
        when(repo.saveOperation(any())).thenAnswer(inv -> {
            ProductOperation o = inv.getArgument(0);
            o.assignId(UUID.randomUUID());
            return o;
        });

        ProductDto.OperationView view = service.addOperation(id,
                new ProductDto.OperationCommand(10, "OP-10", "Perçage", "Poste 1"));

        assertThat(view.id()).isNotNull();
        verify(repo).saveOperation(argThat(o ->
                o.getTenantId().equals(TENANT) && o.getProductId().equals(id)));
    }

    @Test
    void updateOperationReplacesTheExistingLine() {
        UUID productId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();
        when(repo.findById(productId)).thenReturn(Optional.of(ownProduct(productId, ProductStatus.ACTIVE)));
        ProductOperation existing = new ProductOperation(operationId, TENANT, productId,
                10, "OP-10", "Perçage", "Poste 1");
        when(repo.findOperation(operationId)).thenReturn(Optional.of(existing));
        when(repo.saveOperation(any())).thenAnswer(inv -> inv.getArgument(0));

        ProductDto.OperationView view = service.updateOperation(productId, operationId,
                new ProductDto.OperationCommand(20, "OP-20", "Fraisage", "Poste 2"));

        assertThat(view.code()).isEqualTo("OP-20");
        assertThat(view.workstation()).isEqualTo("Poste 2");
    }

    @Test
    void updateOperationOfAForeignTenantIsRefused() {
        UUID productId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();
        when(repo.findById(productId)).thenReturn(Optional.of(ownProduct(productId, ProductStatus.ACTIVE)));
        ProductOperation foreign = new ProductOperation(operationId, OTHER_TENANT, productId,
                10, "OP-10", "Perçage", "Poste 1");
        when(repo.findOperation(operationId)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.updateOperation(productId, operationId,
                new ProductDto.OperationCommand(10, "OP-10", "Perçage", "Poste 1")))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void updateOperationThatDoesNotExistIsRefused() {
        UUID productId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();
        when(repo.findById(productId)).thenReturn(Optional.of(ownProduct(productId, ProductStatus.ACTIVE)));
        when(repo.findOperation(operationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateOperation(productId, operationId,
                new ProductDto.OperationCommand(10, "OP-10", "Perçage", "Poste 1")))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void deleteOperationChecksTheFullOwnershipChain() {
        UUID productId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();
        when(repo.findById(productId)).thenReturn(Optional.of(ownProduct(productId, ProductStatus.ACTIVE)));
        when(repo.findOperation(operationId)).thenReturn(Optional.of(
                new ProductOperation(operationId, TENANT, productId, 10, "OP-10", "Perçage", "Poste 1")));

        service.deleteOperation(productId, operationId);

        verify(repo).deleteOperation(operationId);
    }

    private static Product ownProduct(UUID id, ProductStatus status) {
        return Product.rehydrate(id, TENANT, "REF-1", "Produit 1", null, null,
                status, null, null, null, USER, NOW, NOW);
    }
}
