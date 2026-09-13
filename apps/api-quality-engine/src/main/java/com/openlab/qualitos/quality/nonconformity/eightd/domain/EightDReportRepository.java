package com.openlab.qualitos.quality.nonconformity.eightd.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Port de persistance du rapport 8D. Posé dans le domaine : c'est lui qui dit ce
 * dont il a besoin, l'infrastructure s'y plie.
 *
 * <p>Toutes les signatures portent le tenant — sauf la recherche par code de
 * vérification, qui sert la route PUBLIQUE et n'a par construction aucun contexte
 * de tenant : le code opaque EST l'autorité, et la réponse ne rend que des faits
 * d'intégrité.
 */
public interface EightDReportRepository {

    EightDReport save(EightDReport report);

    Optional<EightDReport> findByNc(UUID tenantId, UUID ncId);

    Optional<EightDReport> findByVerificationCode(String verificationCode);
}
