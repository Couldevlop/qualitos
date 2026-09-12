package com.openlab.qualitos.quality.apqp;

import com.openlab.qualitos.quality.nonconformity.storage.StoredObjectOwner;
import org.springframework.stereotype.Component;

/**
 * Déclare au balayeur d'orphelins les binaires que le module APQP revendique
 * encore : ceux qu'une ligne de {@code apqp_deliverable_evidences} désigne.
 *
 * <p>Sans cet adaptateur, le balayage prendrait toute pièce de livrable pour un
 * orphelin passé le délai de grâce et l'effacerait — le dossier PPAP pointerait
 * alors vers des documents disparus, et rien ne dirait pourquoi.
 *
 * <p>Un adaptateur plutôt qu'un dépôt exposé directement : le balayeur vit dans le
 * paquet stockage et n'a pas à connaître les preuves APQP, pas plus que les
 * preuves APQP n'ont à connaître le balayage.
 */
@Component
public class ApqpDeliverableEvidenceObjectOwner implements StoredObjectOwner {

    private final ApqpDeliverableEvidenceRepository evidences;

    public ApqpDeliverableEvidenceObjectOwner(ApqpDeliverableEvidenceRepository evidences) {
        this.evidences = evidences;
    }

    @Override
    public boolean isReferenced(String objectKey) {
        return evidences.existsByObjectKey(objectKey);
    }

    @Override
    public String ownerName() {
        return "apqp-deliverable-evidence";
    }
}
