package com.openlab.qualitos.core.secret;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Base64;

/**
 * La clé qui chiffre les secrets relisibles de la plateforme.
 *
 * <pre>
 * qualitos.secret:
 *   key: ${QUALITOS_SECRET_KEY}   # 32 octets, en base64
 * </pre>
 *
 * <p>Elle vient de l'environnement, jamais du dépôt ({@code CLAUDE.md}
 * §18.2.3). Une clé écrite dans un fichier de configuration versionné
 * annulerait tout l'exercice : le chiffrement ne protège une base volée que
 * tant que la clé n'est pas volée avec elle.
 *
 * <p>Elle est délibérément <b>distincte</b> des autres secrets de
 * déploiement. La partager avec, disons, le secret du compte de service
 * Keycloak ferait d'une rotation l'une un incident sur l'autre, et une clé
 * qu'on hésite à faire tourner est une clé qui ne tourne jamais (rotation 90j,
 * §11.1 A02).
 */
@ConfigurationProperties(prefix = "qualitos.secret")
public record SecretProperties(String key) {

    /** AES-256 : 32 octets, pas un de plus ni de moins. */
    private static final int OCTETS_ATTENDUS = 32;

    /**
     * Décode la clé, et refuse de démarrer si elle ne tient pas debout.
     *
     * <p>Le contrôle est fait au DÉMARRAGE, pas au premier chiffrement. Une
     * clé mal formée découverte au premier enregistrement d'une messagerie
     * donnerait une erreur à l'utilisateur, des mois après le déploiement, pour
     * une faute d'exploitation qu'il ne peut pas corriger. Mieux vaut que
     * l'application refuse de démarrer, là où la faute se voit et se répare.
     *
     * @throws IllegalStateException si la clé est absente, mal encodée, ou
     *                               n'a pas la bonne longueur
     */
    public byte[] cleDechiffree() {
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "qualitos.secret.key est obligatoire : les secrets des messageries ne peuvent "
                            + "pas etre chiffres sans cle. Fournir 32 octets en base64 "
                            + "(QUALITOS_SECRET_KEY).");
        }
        byte[] octets;
        try {
            octets = Base64.getDecoder().decode(key.strip());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "qualitos.secret.key n'est pas du base64 valide.", e);
        }
        if (octets.length != OCTETS_ATTENDUS) {
            // On dit la longueur TROUVEE, pas la cle : un journal de demarrage
            // se lit a plusieurs, et il se conserve.
            throw new IllegalStateException(
                    "qualitos.secret.key doit faire " + OCTETS_ATTENDUS + " octets une fois decodee "
                            + "(AES-256), et en fait " + octets.length + ".");
        }
        return octets;
    }
}
