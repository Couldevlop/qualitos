package com.openlab.qualitos.quality.controlplan.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * La seule question que le control plan pose au PFMEA : « quel produit couvre la
 * ligne d'analyse que cette ligne de contrôle prétend citer ? »
 *
 * <p>Le port est ici, dans le domaine, plutôt qu'un dépôt Spring Data importé
 * directement : le domaine dicte ce dont il a besoin, l'infrastructure s'y plie —
 * et un test n'a pas à monter un contexte JPA pour vérifier une règle métier.
 */
public interface FmeaItemLookup {

    /**
     * Vide si la ligne est inconnue, appartient à un autre tenant, ou relève d'un
     * projet FMEA qui ne couvre aucun produit.
     */
    Optional<UUID> productCoveredBy(UUID fmeaItemId);
}
