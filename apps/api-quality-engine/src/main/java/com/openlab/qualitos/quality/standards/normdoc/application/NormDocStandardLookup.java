package com.openlab.qualitos.quality.standards.normdoc.application;

import java.util.Optional;
import java.util.UUID;

/**
 * Port de lecture du catalogue normatif. Résout le code + nom d'une norme à
 * partir de son identifiant, pour pré-remplir la commande de génération sans
 * coupler l'application à l'entité JPA {@code Standard}. L'implémentation
 * ({@code StandardLookupAdapter}) filtre par le tenant courant : le catalogue
 * mêle des normes de plateforme et des référentiels appartenant à un tenant
 * (V108), invisibles aux autres.
 */
public interface NormDocStandardLookup {

    /** Norme connue ? renvoie son code stable + nom complet. */
    Optional<StandardRef> findById(UUID standardId);

    record StandardRef(UUID id, String code, String fullName) {}
}
