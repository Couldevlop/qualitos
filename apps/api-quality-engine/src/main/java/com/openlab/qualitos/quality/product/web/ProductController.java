package com.openlab.qualitos.quality.product.web;

import com.openlab.qualitos.quality.product.application.ProductDto;
import com.openlab.qualitos.quality.product.application.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.openlab.qualitos.quality.config.RequiresModule;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints — référentiel Produit.
 *
 * <p>Le tenant vient du contexte de sécurité, jamais du corps de la requête. La
 * lecture est ouverte à tout utilisateur authentifié ; l'écriture est réservée
 * aux rôles qui pilotent le système qualité (OWASP A01).
 */
@RestController
/**
 * Écritures réservées aux tenants ayant souscrit le module « product » (§10.4).
 *
 * <p>Le référentiel produit est le SUJET du PFMEA et du control plan : sans lui,
 * les deux n'ont rien à décrire. D'où sa présence dans leurs dépendances.
 */
@RequiresModule("product")
@RequestMapping("/api/v1/products")
@Validated
@PreAuthorize("isAuthenticated()")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductDto.View> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public ProductDto.View get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','ADMIN_TENANT','SUPER_ADMIN')")
    public ProductDto.View create(@Valid @RequestBody ProductWebDto.CreateRequest req) {
        return service.create(new ProductDto.CreateCommand(
                req.code(), req.designation(), req.family(), req.revisionIndex(),
                req.customerLabel(), req.siteLabel(), req.ownerUserId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','ADMIN_TENANT','SUPER_ADMIN')")
    public ProductDto.View update(@PathVariable UUID id,
                                  @Valid @RequestBody ProductWebDto.UpdateRequest req) {
        return service.update(id, new ProductDto.UpdateCommand(
                req.designation(), req.family(), req.revisionIndex(),
                req.customerLabel(), req.siteLabel(), req.ownerUserId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','ADMIN_TENANT','SUPER_ADMIN')")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','ADMIN_TENANT','SUPER_ADMIN')")
    public ProductDto.View activate(@PathVariable UUID id) {
        return service.activate(id);
    }

    @PostMapping("/{id}/obsolete")
    @PreAuthorize("hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','ADMIN_TENANT','SUPER_ADMIN')")
    public ProductDto.View markObsolete(@PathVariable UUID id) {
        return service.markObsolete(id);
    }

    @GetMapping("/{id}/components")
    public List<ProductDto.ComponentView> components(@PathVariable UUID id) {
        return service.components(id);
    }

    @PostMapping("/{id}/components")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','ADMIN_TENANT','SUPER_ADMIN')")
    public ProductDto.ComponentView addComponent(@PathVariable UUID id,
                                                  @Valid @RequestBody ProductWebDto.ComponentRequest req) {
        return service.addComponent(id, new ProductDto.ComponentCommand(
                req.sequenceNo(), req.reference(), req.label(),
                req.quantity(), req.unit(), req.supplierId()));
    }

    @PutMapping("/{id}/components/{componentId}")
    @PreAuthorize("hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','ADMIN_TENANT','SUPER_ADMIN')")
    public ProductDto.ComponentView updateComponent(@PathVariable UUID id,
                                                     @PathVariable UUID componentId,
                                                     @Valid @RequestBody ProductWebDto.ComponentRequest req) {
        return service.updateComponent(id, componentId, new ProductDto.ComponentCommand(
                req.sequenceNo(), req.reference(), req.label(),
                req.quantity(), req.unit(), req.supplierId()));
    }

    @DeleteMapping("/{id}/components/{componentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','ADMIN_TENANT','SUPER_ADMIN')")
    public void deleteComponent(@PathVariable UUID id, @PathVariable UUID componentId) {
        service.deleteComponent(id, componentId);
    }

    @GetMapping("/{id}/operations")
    public List<ProductDto.OperationView> operations(@PathVariable UUID id) {
        return service.operations(id);
    }

    @PostMapping("/{id}/operations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','ADMIN_TENANT','SUPER_ADMIN')")
    public ProductDto.OperationView addOperation(@PathVariable UUID id,
                                                  @Valid @RequestBody ProductWebDto.OperationRequest req) {
        return service.addOperation(id, new ProductDto.OperationCommand(
                req.sequenceNo(), req.code(), req.label(), req.workstation()));
    }

    @PutMapping("/{id}/operations/{operationId}")
    @PreAuthorize("hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','ADMIN_TENANT','SUPER_ADMIN')")
    public ProductDto.OperationView updateOperation(@PathVariable UUID id,
                                                     @PathVariable UUID operationId,
                                                     @Valid @RequestBody ProductWebDto.OperationRequest req) {
        return service.updateOperation(id, operationId, new ProductDto.OperationCommand(
                req.sequenceNo(), req.code(), req.label(), req.workstation()));
    }

    @DeleteMapping("/{id}/operations/{operationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','ADMIN_TENANT','SUPER_ADMIN')")
    public void deleteOperation(@PathVariable UUID id, @PathVariable UUID operationId) {
        service.deleteOperation(id, operationId);
    }
}
