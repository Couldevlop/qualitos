package com.openlab.qualitos.quality.airouting.application;

import com.openlab.qualitos.quality.airouting.domain.AiProvider;

import java.util.Map;
import java.util.Optional;

/**
 * Port : ce que l'ai-service sait répondre ici — le fournisseur appliqué à un
 * tenant sans politique, et, pour chacun, s'il est configuré.
 *
 * <p>{@link Optional#empty()} quand l'ai-service ne répond pas : l'écran le dit,
 * plutôt que d'afficher une disponibilité inventée.
 */
public interface AiProviderCatalog {

    Optional<Snapshot> describe();

    /**
     * @param platformDefault fournisseur d'un tenant sans politique ; vide si l'ai-service
     *                        en annonce un que l'engine ne connaît pas
     * @param configured      pour chaque fournisseur annoncé, s'il peut répondre
     */
    record Snapshot(Optional<AiProvider> platformDefault, Map<AiProvider, Boolean> configured) {

        public Snapshot {
            platformDefault = platformDefault == null ? Optional.empty() : platformDefault;
            configured = configured == null ? Map.of() : Map.copyOf(configured);
        }

        /** Vrai seulement quand l'ai-service dit explicitement « non configuré ». */
        public boolean refuses(AiProvider provider) {
            return Boolean.FALSE.equals(configured.get(provider));
        }
    }
}
