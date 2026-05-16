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
    List<ApiKey> findExpirable(Instant now, int limit);
}
