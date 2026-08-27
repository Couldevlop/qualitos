package com.openlab.qualitos.quality.pdca;

import com.openlab.qualitos.quality.nonconformity.storage.StoredObjectOwner;
import org.springframework.stereotype.Component;

/**
 * Déclare au balayeur d'orphelins les binaires que le module PDCA revendique
 * encore : ceux qui sont désignés par une ligne de {@code pdca_step_evidences}.
 *
 * <p>Sans cet adaptateur, le balayage prendrait toute preuve d'étape pour un
 * orphelin passé le délai de grâce et l'effacerait — le cycle pointerait alors
 * vers des documents disparus, et rien ne dirait pourquoi.
 *
 * <p>Un adaptateur plutôt qu'un dépôt exposé directement : le balayeur vit dans
 * le paquet stockage et n'a pas à connaître les preuves PDCA, pas plus que les
 * preuves PDCA n'ont à connaître le balayage.
 */
@Component
public class PdcaStepEvidenceObjectOwner implements StoredObjectOwner {

    private final PdcaStepEvidenceRepository evidences;

    public PdcaStepEvidenceObjectOwner(PdcaStepEvidenceRepository evidences) {
        this.evidences = evidences;
    }

    @Override
    public boolean isReferenced(String objectKey) {
        return evidences.existsByObjectKey(objectKey);
    }

    @Override
    public String ownerName() {
        return "pdca-step-evidence";
    }
}
