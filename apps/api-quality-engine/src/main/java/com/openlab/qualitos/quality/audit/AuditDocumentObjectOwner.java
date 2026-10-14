package com.openlab.qualitos.quality.audit;

import com.openlab.qualitos.quality.nonconformity.storage.StoredObjectOwner;
import org.springframework.stereotype.Component;

/**
 * Déclare au balayeur d'orphelins les binaires que les audits revendiquent :
 * ceux qu'une ligne de {@code audit_documents} désigne. Sans cette déclaration,
 * un balayage activé prendrait les documents d'audit pour des orphelins.
 */
@Component
public class AuditDocumentObjectOwner implements StoredObjectOwner {

    private final AuditDocumentRepository documents;

    public AuditDocumentObjectOwner(AuditDocumentRepository documents) {
        this.documents = documents;
    }

    @Override
    public boolean isReferenced(String objectKey) {
        return documents.existsByObjectKey(objectKey);
    }

    @Override
    public String ownerName() {
        return "audit-document";
    }
}
