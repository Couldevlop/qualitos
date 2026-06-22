package com.openlab.qualitos.quality.standards.normdoc.dossier.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de persistance des dossiers documentaires (clean architecture :
 * interface dans le domaine, adapter JPA dans l'infrastructure). Le filtrage par
 * tenant est assuré par l'adapter (tenant issu du JWT).
 */
public interface DossierRepository {

    DocumentationDossier save(DocumentationDossier dossier);

    Optional<DocumentationDossier> findById(UUID id);

    List<DocumentationDossier> findByTenant(UUID tenantId);
}
