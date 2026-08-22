package com.openlab.qualitos.quality.product.application;

import com.openlab.qualitos.quality.product.domain.Product;
import com.openlab.qualitos.quality.product.domain.ProductCodeConflictException;
import com.openlab.qualitos.quality.product.domain.ProductComponent;
import com.openlab.qualitos.quality.product.domain.ProductNotFoundException;
import com.openlab.qualitos.quality.product.domain.ProductOperation;
import com.openlab.qualitos.quality.product.domain.ProductRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases — référentiel Produit.
 *
 * <p>Garantie clé (OWASP A01) : toute lecture/écriture qui cible une ressource par
 * identifiant revérifie l'appartenance au tenant courant, obtenu du contexte de
 * sécurité — jamais d'une valeur du corps de requête. Un produit d'un autre tenant
 * répond 404, jamais 403 : un 403 confirmerait son existence. Les routes imbriquées
 * (composant, opération) revérifient la chaîne complète — produit ET composant/
 * opération — pas seulement la feuille.
 */
public class ProductService {

    private final ProductRepository repo;
    private final TenantProvider tenants;
    private final ActorProvider actors;
    private final Clock clock;

    public ProductService(ProductRepository repo, TenantProvider tenants,
                          ActorProvider actors, Clock clock) {
        this.repo = repo;
        this.tenants = tenants;
        this.actors = actors;
        this.clock = clock;
    }

    public List<ProductDto.View> list() {
        UUID tenant = tenants.requireTenantId();
        return repo.findByTenant(tenant).stream().map(ProductDto.View::of).toList();
    }

    public ProductDto.View get(UUID id) {
        return ProductDto.View.of(requireOwned(id));
    }

    public ProductDto.View create(ProductDto.CreateCommand cmd) {
        UUID tenant = tenants.requireTenantId();
        if (repo.existsByTenantAndCode(tenant, cmd.code())) {
            throw new ProductCodeConflictException(cmd.code());
        }
        Instant now = Instant.now(clock);
        Product product = Product.create(tenant, cmd.code(), cmd.designation(), actors.currentUserId(), now);
        product.describe(cmd.family(), cmd.revisionIndex(), cmd.customerLabel(),
                cmd.siteLabel(), cmd.ownerUserId());
        return ProductDto.View.of(repo.save(product));
    }

    public ProductDto.View update(UUID id, ProductDto.UpdateCommand cmd) {
        Product product = requireOwned(id);
        product.rename(cmd.designation());
        product.describe(cmd.family(), cmd.revisionIndex(), cmd.customerLabel(),
                cmd.siteLabel(), cmd.ownerUserId());
        return ProductDto.View.of(repo.save(product));
    }

    public ProductDto.View activate(UUID id) {
        Product product = requireOwned(id);
        product.activate();
        return ProductDto.View.of(repo.save(product));
    }

    public ProductDto.View markObsolete(UUID id) {
        Product product = requireOwned(id);
        product.markObsolete();
        return ProductDto.View.of(repo.save(product));
    }

    public void delete(UUID id) {
        requireOwned(id);
        repo.delete(id);
    }

    public List<ProductDto.ComponentView> components(UUID productId) {
        requireOwned(productId);
        return repo.componentsOf(productId).stream().map(ProductDto.ComponentView::of).toList();
    }

    public ProductDto.ComponentView addComponent(UUID productId, ProductDto.ComponentCommand cmd) {
        Product product = requireOwned(productId);
        ProductComponent component = new ProductComponent(null, product.getTenantId(), product.getId(),
                cmd.sequenceNo(), cmd.reference(), cmd.label(), cmd.quantity(), cmd.unit(), cmd.supplierId());
        return ProductDto.ComponentView.of(repo.saveComponent(component));
    }

    public ProductDto.ComponentView updateComponent(UUID productId, UUID componentId,
                                                     ProductDto.ComponentCommand cmd) {
        ProductComponent existing = requireOwnedComponent(productId, componentId);
        ProductComponent updated = new ProductComponent(existing.getId(), existing.getTenantId(),
                existing.getProductId(), cmd.sequenceNo(), cmd.reference(), cmd.label(),
                cmd.quantity(), cmd.unit(), cmd.supplierId());
        return ProductDto.ComponentView.of(repo.saveComponent(updated));
    }

    public void deleteComponent(UUID productId, UUID componentId) {
        requireOwnedComponent(productId, componentId);
        repo.deleteComponent(componentId);
    }

    public List<ProductDto.OperationView> operations(UUID productId) {
        requireOwned(productId);
        return repo.operationsOf(productId).stream().map(ProductDto.OperationView::of).toList();
    }

    public ProductDto.OperationView addOperation(UUID productId, ProductDto.OperationCommand cmd) {
        Product product = requireOwned(productId);
        ProductOperation operation = new ProductOperation(null, product.getTenantId(), product.getId(),
                cmd.sequenceNo(), cmd.code(), cmd.label(), cmd.workstation());
        return ProductDto.OperationView.of(repo.saveOperation(operation));
    }

    public ProductDto.OperationView updateOperation(UUID productId, UUID operationId,
                                                     ProductDto.OperationCommand cmd) {
        ProductOperation existing = requireOwnedOperation(productId, operationId);
        ProductOperation updated = new ProductOperation(existing.getId(), existing.getTenantId(),
                existing.getProductId(), cmd.sequenceNo(), cmd.code(), cmd.label(), cmd.workstation());
        return ProductDto.OperationView.of(repo.saveOperation(updated));
    }

    public void deleteOperation(UUID productId, UUID operationId) {
        requireOwnedOperation(productId, operationId);
        repo.deleteOperation(operationId);
    }

    private Product requireOwned(UUID id) {
        UUID tenant = tenants.requireTenantId();
        Product product = repo.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        if (!product.getTenantId().equals(tenant)) {
            // 404 et non 403 : un 403 confirmerait l'existence du produit d'un autre tenant.
            throw new ProductNotFoundException(id);
        }
        return product;
    }

    private ProductComponent requireOwnedComponent(UUID productId, UUID componentId) {
        Product parent = requireOwned(productId);
        ProductComponent component = repo.findComponent(componentId)
                .orElseThrow(() -> new ProductNotFoundException(componentId));
        if (!component.getProductId().equals(parent.getId())
                || !component.getTenantId().equals(parent.getTenantId())) {
            throw new ProductNotFoundException(componentId);
        }
        return component;
    }

    private ProductOperation requireOwnedOperation(UUID productId, UUID operationId) {
        Product parent = requireOwned(productId);
        ProductOperation operation = repo.findOperation(operationId)
                .orElseThrow(() -> new ProductNotFoundException(operationId));
        if (!operation.getProductId().equals(parent.getId())
                || !operation.getTenantId().equals(parent.getTenantId())) {
            throw new ProductNotFoundException(operationId);
        }
        return operation;
    }
}
