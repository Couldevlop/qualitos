package com.openlab.qualitos.quality.airouting.domain;

/**
 * L'ai-service déclare ce fournisseur inutilisable (clé absente) : y épingler le
 * tenant couperait toutes ses fonctions d'IA. Mappée en 422.
 */
public class AiProviderNotConfiguredException extends RuntimeException {

    private final transient AiProvider provider;

    public AiProviderNotConfiguredException(AiProvider provider) {
        super("Le fournisseur d'IA « " + provider.wireName() + " » n'est pas configuré sur cette plateforme.");
        this.provider = provider;
    }

    public AiProvider provider() {
        return provider;
    }
}
