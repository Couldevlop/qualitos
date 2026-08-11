package com.openlab.qualitos.quality.nonconformity;

import com.openlab.qualitos.quality.nonconformity.storage.StoredObjectOwner;
import org.springframework.stereotype.Component;

/**
 * Déclare au balayeur d'orphelins les binaires que le module Non-conformité
 * revendique encore : ceux qui sont désignés par une ligne de {@code nc_photos}.
 */
@Component
public class NcPhotoObjectOwner implements StoredObjectOwner {

    private final NcPhotoRepository photos;

    public NcPhotoObjectOwner(NcPhotoRepository photos) {
        this.photos = photos;
    }

    @Override
    public boolean isReferenced(String objectKey) {
        return photos.existsByObjectKey(objectKey);
    }

    @Override
    public String ownerName() {
        return "nc-photo";
    }
}
