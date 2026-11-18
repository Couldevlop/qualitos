package com.openlab.qualitos.core.secret;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Le coffre des secrets relisibles")
class AesGcmSecretCipherTest {

    private static final String CLE =
            Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    private final AesGcmSecretCipher coffre =
            new AesGcmSecretCipher(new SecretProperties(CLE));

    @Test
    @DisplayName("Un secret chiffre se retrouve a l'identique")
    void allerRetour() {
        String mdp = "s3cr3t-smtp-du-client";

        assertThat(coffre.dechiffrer(coffre.chiffrer(mdp))).isEqualTo(mdp);
    }

    @Test
    @DisplayName("Les accents et l'unicode traversent sans se perdre")
    void unicode() {
        // Un mot de passe genere par un client peut contenir n'importe quoi.
        // Un encodage par defaut mal choisi ne se verrait qu'au premier envoi
        // refuse par son serveur, des mois plus tard.
        String mdp = "mot-de-passé-à-clé-€-中文";

        assertThat(coffre.dechiffrer(coffre.chiffrer(mdp))).isEqualTo(mdp);
    }

    @Test
    @DisplayName("Le MEME secret ne donne jamais deux fois le meme cryptogramme")
    void nonceNeuf() {
        String mdp = "identique";

        String a = coffre.chiffrer(mdp);
        String b = coffre.chiffrer(mdp);

        // Reutiliser un nonce avec la meme cle est LA faute qui casse GCM : elle
        // ne fuite pas seulement les messages, elle laisse forger des
        // cryptogrammes valides. Deux chiffrements identiques la revelent.
        assertThat(a).isNotEqualTo(b);
        assertThat(coffre.dechiffrer(a)).isEqualTo(mdp);
        assertThat(coffre.dechiffrer(b)).isEqualTo(mdp);
    }

    @Test
    @DisplayName("Un cryptogramme altere d'un seul BIT est REFUSE")
    void alterationRefusee() {
        String scelle = coffre.chiffrer("mot-de-passe");
        String[] parts = scelle.split("\\.", 3);
        // On altere les OCTETS, pas le texte. Retourner le dernier caractere
        // base64 ne prouverait rien : sans remplissage, il porte des bits
        // INUTILISES, et « A » devient « B » sans qu'aucun octet ne change --
        // le banc passerait alors en ne testant rien.
        byte[] octets = Base64.getUrlDecoder().decode(parts[2]);
        octets[0] ^= 0x01;
        String altere = parts[0] + "." + parts[1] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(octets);

        // GCM AUTHENTIFIE : sans cela, quelqu'un qui ecrit en base -- une
        // injection, un acces de maintenance -- pourrait faire pointer le mot
        // de passe SMTP d'un client vers ce qu'il veut sans que rien ne s'en
        // apercoive.
        assertThatThrownBy(() -> coffre.dechiffrer(altere))
                .isInstanceOf(SecretCipherException.class);
    }

    @Test
    @DisplayName("Une AUTRE cle ne dechiffre pas, elle echoue")
    void autreCle() {
        String scelle = coffre.chiffrer("mot-de-passe");
        AesGcmSecretCipher autre = new AesGcmSecretCipher(new SecretProperties(
                Base64.getEncoder().encodeToString("ffffffffffffffffffffffffffffffff".getBytes())));

        // On ne rend JAMAIS une valeur approximative : un mot de passe a demi
        // dechiffre partirait vers un serveur de messagerie tiers.
        assertThatThrownBy(() -> autre.dechiffrer(scelle))
                .isInstanceOf(SecretCipherException.class);
    }

    @Test
    @DisplayName("Le cryptogramme porte sa version des le premier jour")
    void versionne() {
        // Sans ce prefixe, changer d'algorithme obligerait a DEVINER le format
        // des valeurs deja en base -- et c'est precisement ce qui empeche les
        // migrations cryptographiques d'avoir lieu (crypto-agilite, §11.4).
        assertThat(coffre.chiffrer("x")).startsWith("v1.");
    }

    @Test
    @DisplayName("Chiffrer du vide est refuse, pas tolere")
    void videRefuse() {
        // Un cryptogramme qui se dechiffre en vide se lirait, une fois stocke,
        // comme « un mot de passe est configure » alors qu'il n'y en a pas.
        assertThatThrownBy(() -> coffre.chiffrer("")).isInstanceOf(SecretCipherException.class);
        assertThatThrownBy(() -> coffre.chiffrer(null)).isInstanceOf(SecretCipherException.class);
    }

    @Test
    @DisplayName("Ce qui n'est pas un cryptogramme de ce coffre est refuse")
    void formatInconnu() {
        assertThatThrownBy(() -> coffre.dechiffrer("pas-du-tout"))
                .isInstanceOf(SecretCipherException.class);
        assertThatThrownBy(() -> coffre.dechiffrer("v2.abc.def"))
                .isInstanceOf(SecretCipherException.class);
        assertThatThrownBy(() -> coffre.dechiffrer("v1.trop-court.AAAA"))
                .isInstanceOf(SecretCipherException.class);
        assertThatThrownBy(() -> coffre.dechiffrer("  "))
                .isInstanceOf(SecretCipherException.class);
        assertThatThrownBy(() -> coffre.dechiffrer(null))
                .isInstanceOf(SecretCipherException.class);
    }

    @Test
    @DisplayName("Une cle absente ou mal formee arrete le DEMARRAGE, pas le premier envoi")
    void cleInvalide() {
        // Decouverte au premier enregistrement d'une messagerie, la faute
        // tomberait sur un utilisateur des mois apres le deploiement, pour une
        // erreur d'exploitation qu'il ne peut pas corriger.
        assertThatThrownBy(() -> new SecretProperties(null).cleDechiffree())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("obligatoire");
        assertThatThrownBy(() -> new SecretProperties("  ").cleDechiffree())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new SecretProperties("pas du base64 !!").cleDechiffree())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("base64");
        assertThatThrownBy(() -> new SecretProperties(
                Base64.getEncoder().encodeToString("trop court".getBytes())).cleDechiffree())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("32 octets");
    }

    @Test
    @DisplayName("Sans cle, le coffre REFUSE -- il ne stocke pas en clair")
    void sansCleOnRefuse() {
        SecretCipher absent = new SecretCipherConfiguration()
                .secretCipher(new SecretProperties(null));

        // Un repli silencieux sur le stockage en clair est la facon dont ces
        // choses arrivent vraiment : la fonctionnalite marche, personne ne
        // remarque rien, et les mots de passe de tous les clients dorment en
        // clair dans une colonne.
        assertThatThrownBy(() -> absent.chiffrer("mot-de-passe"))
                .isInstanceOf(SecretCipherException.class)
                .hasMessageContaining("ne sera pas enregistree en clair");
        // Il echoue AUSSI en lecture : accepter de lire laisserait croire
        // qu'une messagerie deja configuree fonctionne encore apres la perte
        // de la cle, alors que ses envois echoueraient un par un.
        assertThatThrownBy(() -> absent.dechiffrer("v1.a.b"))
                .isInstanceOf(SecretCipherException.class);
    }

    @Test
    @DisplayName("Avec une cle, c'est le vrai coffre qui est monte")
    void avecCleLeVraiCoffre() {
        SecretCipher monte = new SecretCipherConfiguration()
                .secretCipher(new SecretProperties(CLE));

        assertThat(monte).isInstanceOf(AesGcmSecretCipher.class);
        assertThat(monte.dechiffrer(monte.chiffrer("ok"))).isEqualTo("ok");
    }
}
