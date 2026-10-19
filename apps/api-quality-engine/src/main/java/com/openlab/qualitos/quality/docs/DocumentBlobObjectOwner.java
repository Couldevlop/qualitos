package com.openlab.qualitos.quality.docs;

import com.openlab.qualitos.quality.nonconformity.storage.StoredObjectOwner;
import org.springframework.stereotype.Component;

/**
 * Déclare au balayeur d'orphelins les fichiers que la GED revendique : ceux
 * qu'une ligne de {@code document_blobs} désigne (ADR 0094).
 */
@Component
public class DocumentBlobObjectOwner implements StoredObjectOwner {

    private final DocumentBlobRepository blobs;

    public DocumentBlobObjectOwner(DocumentBlobRepository blobs) {
        this.blobs = blobs;
    }

    @Override
    public boolean isReferenced(String objectKey) {
        return blobs.existsByObjectKey(objectKey);
    }

    @Override
    public String ownerName() {
        return "document-blob";
    }
}
