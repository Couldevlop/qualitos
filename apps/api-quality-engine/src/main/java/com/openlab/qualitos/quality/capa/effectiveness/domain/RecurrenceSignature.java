package com.openlab.qualitos.quality.capa.effectiveness.domain;

import java.util.UUID;

/**
 * Ce qui définit « le même problème », pour décider qu'une non-conformité
 * postérieure est une récidive et non un défaut sans rapport.
 *
 * <p>Deux précisions possibles, et l'écart entre elles est important à lire :
 *
 * <ul>
 *   <li><b>Précise</b> — le produit ET le mode de défaillance du PFMEA. C'est le
 *       rapprochement qui a du sens : « la même défaillance, sur la même
 *       pièce ». Il n'existe que depuis le lot Produit/PFMEA, qui a posé ces
 *       deux liens sur la non-conformité.</li>
 *   <li><b>Approchée</b> — la catégorie seule (produit, procédé, documentation,
 *       fournisseur…). Beaucoup plus large : deux défauts de catégorie
 *       « procédé » n'ont aucune raison d'être le même problème. Le taux calculé
 *       sur cette base est indicatif, et l'API le dit.</li>
 * </ul>
 *
 * <p>Quand la CAPA ne vient pas d'une non-conformité — audit, réclamation,
 * décision interne — il n'y a rien à recouper : la signature est absente, et
 * l'efficacité ne se mesure pas. Inventer un rapprochement produirait des
 * récidives imaginaires.
 */
public record RecurrenceSignature(UUID productId, UUID fmeaItemId, String category) {

    /** Aucune signature : la CAPA ne se rattache à rien de comparable. */
    public static final RecurrenceSignature NONE = new RecurrenceSignature(null, null, null);

    public static RecurrenceSignature precise(UUID productId, UUID fmeaItemId) {
        if (productId == null || fmeaItemId == null) {
            throw new IllegalArgumentException(
                    "Une signature précise exige le produit ET le mode de défaillance");
        }
        return new RecurrenceSignature(productId, fmeaItemId, null);
    }

    public static RecurrenceSignature byCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Catégorie requise");
        }
        return new RecurrenceSignature(null, null, category);
    }

    /**
     * Construit la signature la plus précise que les données permettent. C'est
     * ici, et à un seul endroit, que se décide la préférence : le mode de
     * défaillance l'emporte sur la catégorie dès qu'il existe.
     */
    public static RecurrenceSignature of(UUID productId, UUID fmeaItemId, String category) {
        if (productId != null && fmeaItemId != null) {
            return precise(productId, fmeaItemId);
        }
        if (category != null && !category.isBlank()) {
            return byCategory(category);
        }
        return NONE;
    }

    public boolean isPrecise() {
        return productId != null && fmeaItemId != null;
    }

    public boolean isMeasurable() {
        return isPrecise() || (category != null && !category.isBlank());
    }
}
