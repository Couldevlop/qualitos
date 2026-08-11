package com.openlab.qualitos.quality.capa;

import com.openlab.qualitos.quality.nonconformity.storage.StoredObjectOwner;
import org.springframework.stereotype.Component;

/**
 * Déclare au balayeur d'orphelins les binaires que le module CAPA revendique
 * encore : ceux qui sont désignés par une ligne de {@code capa_evidences}.
 *
 * <p>Un adaptateur plutôt qu'un dépôt exposé directement : le balayeur vit dans
 * le paquet stockage et n'a pas à connaître les preuves CAPA, pas plus que les
 * preuves CAPA n'ont à connaître le balayage.
 */
@Component
public class CapaEvidenceObjectOwner implements StoredObjectOwner {

    private final CapaEvidenceRepository evidences;

    public CapaEvidenceObjectOwner(CapaEvidenceRepository evidences) {
        this.evidences = evidences;
    }

    @Override
    public boolean isReferenced(String objectKey) {
        return evidences.existsByObjectKey(objectKey);
    }

    @Override
    public String ownerName() {
        return "capa-evidence";
    }
}
