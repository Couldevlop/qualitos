package com.openlab.qualitos.quality.revisionrequests.application;

import java.util.Optional;
import java.util.UUID;

/**
 * Retrouve une non-conformite par sa reference humaine.
 *
 * <p>C'est la reference — « NC-2026-0143 » — qu'un dossier CAPA conserve de son
 * origine, et non l'identifiant technique : la boucle CAPA doit donc repartir
 * de la.
 */
public interface NcLookupPort {

    Optional<NcRef> findByReference(UUID tenantId, String reference);

    /** Le strict necessaire : de quel produit et de quel mode de defaillance il s'agit. */
    record NcRef(UUID id, UUID productId, UUID fmeaItemId) {}
}
