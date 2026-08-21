package com.openlab.qualitos.quality.revisionrequests.application;

import java.util.UUID;

/**
 * Consigne au journal chaîné du tenant ce qui a été décidé d'une proposition.
 *
 * <p>Le refus est tracé autant que l'acceptation : ne pas le tracer laisserait
 * croire que la proposition n'a jamais existé, alors que « on n'a pas bougé » est
 * une décision qualité que l'auditeur voudra lire.
 */
public interface RevisionAuditPort {

    void record(UUID tenantId, UUID actorId, String action, UUID requestId,
                String summary, String detailsJson);
}
