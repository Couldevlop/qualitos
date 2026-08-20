package com.openlab.qualitos.quality.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Ce que la plateforme accepte comme preuve qu'un second facteur a été présenté.
 *
 * <p>Deux revendications plutôt qu'une : un realm Keycloak publie {@code acr}
 * quand une carte de niveaux d'authentification y est configurée, et {@code amr}
 * quand un <i>protocol mapper</i> le pose. Les valeurs acceptées sont
 * paramétrables parce qu'un realm nomme ses niveaux comme il veut — « 2 », « gold »
 * ou « mfa » désignent la même chose selon l'installation.
 *
 * <p>{@code enforced} vaut {@code true} par défaut, et c'est délibéré : un
 * interrupteur de sécurité dont le défaut est « ouvert » finit par rester ouvert.
 * Le désactiver est une décision d'exploitation, pas un réglage d'oubli.
 */
@Component
@ConfigurationProperties(prefix = "qualitos.security.step-up")
public class StepUpProperties {

    private boolean enforced = true;

    /** Niveaux d'authentification qui valent second facteur. */
    private List<String> acceptedAcr = new ArrayList<>(List.of("2", "3", "gold", "mfa"));

    /**
     * Méthodes d'authentification qui valent second facteur (RFC 8176) :
     * code à usage unique, jeton matériel ou logiciel, biométrie.
     */
    private List<String> acceptedAmr =
            new ArrayList<>(List.of("otp", "mfa", "hwk", "swk", "face", "fpt", "iris"));

    public boolean isEnforced() { return enforced; }
    public void setEnforced(boolean enforced) { this.enforced = enforced; }

    public List<String> getAcceptedAcr() { return acceptedAcr; }
    public void setAcceptedAcr(List<String> acceptedAcr) { this.acceptedAcr = acceptedAcr; }

    public List<String> getAcceptedAmr() { return acceptedAmr; }
    public void setAcceptedAmr(List<String> acceptedAmr) { this.acceptedAmr = acceptedAmr; }
}
