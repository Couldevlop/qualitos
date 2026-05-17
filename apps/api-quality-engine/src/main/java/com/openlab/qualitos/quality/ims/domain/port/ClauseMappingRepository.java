package com.openlab.qualitos.quality.ims.domain.port;

import com.openlab.qualitos.quality.ims.domain.model.ClauseMapping;

import java.util.List;

/**
 * Port domain — accès aux mappings de clauses entre normes (HLS / sector overlays).
 * Implémenté par l'infrastructure JPA.
 */
public interface ClauseMappingRepository {

    /**
     * Tous les mappings dont la source est une des normes données.
     * Set vide → retourne liste vide.
     */
    List<ClauseMapping> findMappingsBetween(List<String> standardCodes);
}
