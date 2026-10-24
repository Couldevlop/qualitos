package com.openlab.qualitos.quality.dmaic.resolution.infrastructure;

import com.openlab.qualitos.quality.nonconformity.storage.StoredObjectOwner;
import org.springframework.stereotype.Component;

/**
 * Déclare au balayeur d'orphelins les photos que la fiche de résolution DMAIC
 * revendique encore : celles qu'une ligne de {@code dmaic_resolution_photos}
 * désigne. Sans ce propriétaire, le balayeur les effacerait.
 */
@Component
public class ResolutionPhotoObjectOwner implements StoredObjectOwner {

    private final ResolutionPhotoJpaRepository photos;

    public ResolutionPhotoObjectOwner(ResolutionPhotoJpaRepository photos) {
        this.photos = photos;
    }

    @Override
    public boolean isReferenced(String objectKey) {
        return photos.existsByObjectKey(objectKey);
    }

    @Override
    public String ownerName() {
        return "dmaic-resolution-photo";
    }
}
