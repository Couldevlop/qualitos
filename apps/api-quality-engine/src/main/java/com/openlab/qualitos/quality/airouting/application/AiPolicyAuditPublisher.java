package com.openlab.qualitos.quality.airouting.application;

import com.openlab.qualitos.quality.airouting.domain.AiProvider;

import java.util.Optional;
import java.util.UUID;

/**
 * Port d'audit (OWASP A09) : où partent les prompts d'un tenant est une décision de
 * conformité ; qui l'a prise, et quand, doit se retrouver dans le journal chaîné.
 */
public interface AiPolicyAuditPublisher {

    void pinned(UUID tenantId, AiProvider provider, Optional<AiProvider> previous, UUID actor);

    void released(UUID tenantId, AiProvider previous, UUID actor);
}
