package com.openlab.qualitos.core.secret;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Choisit le coffre, et refuse de faire semblant quand il n'y en a pas.
 *
 * <p>Deux mauvaises reponses etaient possibles ici, et on les ecarte toutes
 * les deux.
 *
 * <p><b>Refuser de demarrer sans cle</b> aurait casse chaque deploiement
 * existant -- et chaque poste de developpement -- pour une fonctionnalite
 * qu'aucun d'eux n'utilise encore. Une migration qui arrete la production
 * n'est pas une migration, c'est une panne.
 *
 * <p><b>Stocker en clair faute de cle</b> aurait ete bien pire : la
 * fonctionnalite marcherait, personne ne remarquerait rien, et les mots de
 * passe des messageries de tous les clients dormiraient en clair dans une
 * colonne. Un repli silencieux sur l'option non sure est la facon dont ces
 * choses arrivent vraiment.
 *
 * <p>Alors sans cle, l'application DEMARRE et le coffre REFUSE. C'est le meme
 * parti que le compte de service Keycloak : sans secret, creer un compte
 * repond une erreur franche plutot que de laisser un compte fantome.
 */
@Configuration
@EnableConfigurationProperties(SecretProperties.class)
public class SecretCipherConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SecretCipherConfiguration.class);

    @Bean
    public SecretCipher secretCipher(SecretProperties props) {
        if (props.key() == null || props.key().isBlank()) {
            log.warn("secret.cipher.absent : QUALITOS_SECRET_KEY n'est pas fournie. "
                    + "Les messageries par organisation seront REFUSEES (jamais stockees en clair).");
            return new CoffreAbsent();
        }
        return new AesGcmSecretCipher(props);
    }

    /**
     * Le coffre qui n'existe pas : il refuse, il ne contourne pas.
     *
     * <p>Il echoue AUSSI au dechiffrement, et pas seulement au chiffrement. Un
     * coffre qui refuse d'ecrire mais accepte de lire laisserait croire qu'une
     * messagerie deja configuree continue de fonctionner apres la perte de la
     * cle -- alors que ses envois echoueraient un par un, sans que l'ecran le
     * dise.
     */
    static final class CoffreAbsent implements SecretCipher {

        private static final String RAISON =
                "Le chiffrement des secrets n'est pas configure sur cette installation "
                        + "(QUALITOS_SECRET_KEY). Une messagerie ne sera pas enregistree en clair.";

        @Override
        public String chiffrer(String clair) {
            throw new SecretCipherException(RAISON);
        }

        @Override
        public String dechiffrer(String cryptogramme) {
            throw new SecretCipherException(RAISON);
        }
    }
}
