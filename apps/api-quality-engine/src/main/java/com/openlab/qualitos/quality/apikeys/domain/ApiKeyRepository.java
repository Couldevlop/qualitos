package com.openlab.qualitos.quality.apikeys.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository {

    ApiKey save(ApiKey key);

    Optional<ApiKey> findById(UUID id);

    /** Lookup secret par prefix (clé candidate). Renvoie 0 ou 1 (prefix UNIQUE). */
    Optional<ApiKey> findByPrefix(String prefix);

    List<ApiKey> findAllByTenantId(UUID tenantId);

    /** Clés actives dont l'expiration est passée (scheduler). */
    /**
     * Clés actives dont l'échéance est dépassée, POUR UN TENANT DONNÉ.
     *
     * <p>Le tenant est un paramètre obligatoire, pas une commodité : sans lui, un
     * administrateur de tenant déclenchant le balayage ferait expirer les clés des
     * autres tenants (violation §18.2 #2 / OWASP A01).
     */
    List<ApiKey> findExpirable(UUID tenantId, Instant now, int limit);
}
