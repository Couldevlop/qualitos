package com.openlab.qualitos.quality.revisionrequests.application;

import java.time.Instant;
import java.util.UUID;

/**
 * L'historique dont le calcul d'occurrence a besoin, et rien de plus.
 *
 * <p>Inversion de dependance : le moteur ne connait pas le module des
 * non-conformites, seulement le service qu'il en attend.
 */
public interface NcHistoryPort {

    int countForProductAndFailureMode(UUID tenantId, UUID productId, UUID fmeaItemId, Instant since);
}
