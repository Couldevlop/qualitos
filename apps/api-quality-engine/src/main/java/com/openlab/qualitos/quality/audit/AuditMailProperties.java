package com.openlab.qualitos.quality.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration de la brique courriel (§4.4).
 *
 * <p>Hôte, port, identifiants et TLS relèvent de {@code spring.mail.*} (starter
 * Spring Mail), alimentés par variables d'environnement. Ne restent ici que les
 * deux réglages qui n'y ont pas leur place : l'interrupteur et l'expéditeur.
 *
 * <p>AUCUN secret par défaut en dur (§18.2.3) — le mot de passe SMTP n'apparaît
 * ni ici, ni dans application.yml, ni dans les valeurs Helm : il arrive par un
 * Secret dédié.
 */
@Component
@ConfigurationProperties(prefix = "qualitos.mail")
public class AuditMailProperties {

    /**
     * Interrupteur de l'envoi. OFF par défaut, comme le stockage objet et le relais
     * Kafka : une application démarrée sans serveur SMTP doit démarrer quand même,
     * et les rappels internes doivent continuer de fonctionner.
     */
    private boolean enabled = false;

    /**
     * Adresse d'expédition (« From »). Obligatoire dès que l'envoi est actif : la
     * plupart des relais refusent un message sans expéditeur, et un refus au
     * moment de l'envoi se découvre bien plus tard qu'un refus au démarrage.
     */
    private String from;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
}
