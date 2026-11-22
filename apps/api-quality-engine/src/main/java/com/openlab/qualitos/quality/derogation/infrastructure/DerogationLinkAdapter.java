package com.openlab.qualitos.quality.derogation.infrastructure;

import com.openlab.qualitos.quality.derogation.application.DerogationLinkBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Le lien qui conduit le manager dans QualitOS, sur le dossier a signer.
 *
 * <p>L'adresse publique vient de la CONFIGURATION et non de la requete en cours.
 * La deduire d'un en-tete `Host` ou `X-Forwarded-Host` laisserait un appelant
 * choisir le domaine d'un lien qui part par courriel a un collegue : c'est une
 * redirection ouverte, par courriel interne.
 */
@Component
public class DerogationLinkAdapter implements DerogationLinkBuilder {

    private final String baseUrl;

    public DerogationLinkAdapter(
            @Value("${qualitos.web.base-url:http://localhost:4200}") String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public String linkTo(UUID derogationId) {
        return baseUrl + pathTo(derogationId);
    }

    @Override
    public String pathTo(UUID derogationId) {
        return "/derogations/" + derogationId;
    }
}
