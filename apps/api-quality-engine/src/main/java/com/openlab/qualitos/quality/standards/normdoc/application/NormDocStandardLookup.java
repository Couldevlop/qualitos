package com.openlab.qualitos.quality.standards.normdoc.application;

import java.util.Optional;
import java.util.UUID;

/**
 * Port de lecture du catalogue normatif (platform-level). Résout le code + nom
 * d'une norme à partir de son identifiant, pour pré-remplir la commande de
 * génération sans coupler l'application à l'entité JPA {@code Standard}.
 */
public interface NormDocStandardLookup {

    /** Norme connue ? renvoie son code stable + nom complet. */
    Optional<StandardRef> findById(UUID standardId);

    record StandardRef(UUID id, String code, String fullName) {}
}
