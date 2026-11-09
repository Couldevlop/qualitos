package com.openlab.qualitos.quality.fives;

import com.openlab.qualitos.quality.nonconformity.storage.StoredObjectOwner;
import org.springframework.stereotype.Component;

/**
 * Déclare au balayeur d'orphelins les binaires que le module 5S revendique
 * encore : ceux qu'une ligne de {@code fives_item_photos} désigne.
 *
 * <p>Sans cet adaptateur, le balayage prendrait toute photo de terrain pour un
 * orphelin passé le délai de grâce et l'effacerait — l'audit montrerait alors des
 * vignettes brisées, et rien ne dirait pourquoi.
 */
@Component
public class FiveSItemPhotoObjectOwner implements StoredObjectOwner {

    private final FiveSItemPhotoRepository photos;

    public FiveSItemPhotoObjectOwner(FiveSItemPhotoRepository photos) {
        this.photos = photos;
    }

    @Override
    public boolean isReferenced(String objectKey) {
        return photos.existsByObjectKey(objectKey);
    }

    @Override
    public String ownerName() {
        return "fives-item-photo";
    }
}
