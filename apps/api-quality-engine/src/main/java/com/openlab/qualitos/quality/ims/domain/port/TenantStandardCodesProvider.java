package com.openlab.qualitos.quality.ims.domain.port;

import java.util.List;

/**
 * Port — récupère les normes adoptées par le tenant courant.
 * Le tenant est lu côté infrastructure via {@code TenantContext} (JWT).
 */
public interface TenantStandardCodesProvider {

    /**
     * @return liste des codes de normes adoptées par le tenant courant (ex. "iso-9001", "iso-14001").
     */
    List<String> findAdoptedStandardCodes();
}
