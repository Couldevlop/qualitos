package com.openlab.qualitos.quality.supplier;

import com.openlab.qualitos.quality.nonconformity.storage.StoredObjectOwner;
import org.springframework.stereotype.Component;

/**
 * Déclare au balayeur d'orphelins les binaires que les fournisseurs revendiquent :
 * ceux qu'une ligne de {@code supplier_documents} désigne.
 */
@Component
public class SupplierDocumentObjectOwner implements StoredObjectOwner {

    private final SupplierDocumentRepository documents;

    public SupplierDocumentObjectOwner(SupplierDocumentRepository documents) {
        this.documents = documents;
    }

    @Override
    public boolean isReferenced(String objectKey) {
        return documents.existsByObjectKey(objectKey);
    }

    @Override
    public String ownerName() {
        return "supplier-document";
    }
}
