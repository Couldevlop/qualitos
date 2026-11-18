package com.openlab.qualitos.core.secret;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM, la clé venant de la configuration.
 *
 * <p>GCM et pas CBC : il <b>authentifie</b>. Un cryptogramme modifié d'un seul
 * octet est rejeté au déchiffrement au lieu de rendre des octets plausibles.
 * Sans cette propriété, quelqu'un qui écrit en base — une injection, un accès
 * de maintenance — pourrait faire pointer le mot de passe SMTP d'un client vers
 * ce qu'il veut sans que rien ne s'en aperçoive.
 *
 * <p>Format stocké : {@code v1.<nonce base64url>.<chiffré+tag base64url>}. Le
 * préfixe de version est là dès le premier jour. Sans lui, changer
 * d'algorithme obligerait à deviner le format des valeurs déjà en base, et
 * c'est précisément ce qui empêche les migrations cryptographiques d'avoir
 * lieu (crypto-agilité, {@code CLAUDE.md} §11.4).
 *
 * <p>Un nonce NEUF à chaque chiffrement, tiré d'un {@link SecureRandom}.
 * Réutiliser un nonce avec la même clé est la faute qui casse GCM — elle ne
 * fuite pas seulement les messages, elle laisse forger des cryptogrammes
 * valides. On ne le dérive donc de rien : ni du client, ni de l'horloge, ni du
 * contenu.
 */
public class AesGcmSecretCipher implements SecretCipher {

    /** 96 bits : la taille recommandée pour GCM, et celle qui évite une re-dérivation interne. */
    private static final int NONCE_OCTETS = 12;

    /** 128 bits d'étiquette d'authentification — le maximum que GCM propose. */
    private static final int TAG_BITS = 128;

    private static final String PREFIXE = "v1";
    private static final Base64.Encoder ENCODEUR = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODEUR = Base64.getUrlDecoder();

    private final SecretKey cle;
    private final SecureRandom alea = new SecureRandom();

    public AesGcmSecretCipher(SecretProperties props) {
        this.cle = new SecretKeySpec(props.cleDechiffree(), "AES");
    }

    @Override
    public String chiffrer(String clair) {
        if (clair == null || clair.isEmpty()) {
            // Chiffrer du vide produirait un cryptogramme qui se dechiffre en
            // vide : stocke, il se lirait comme « un mot de passe est
            // configure » alors qu'il n'y en a pas.
            throw new SecretCipherException("un secret vide ne se chiffre pas");
        }
        byte[] nonce = new byte[NONCE_OCTETS];
        alea.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, cle, new GCMParameterSpec(TAG_BITS, nonce));
            byte[] scelle = cipher.doFinal(clair.getBytes(StandardCharsets.UTF_8));
            return PREFIXE + "." + ENCODEUR.encodeToString(nonce) + "."
                    + ENCODEUR.encodeToString(scelle);
        } catch (GeneralSecurityException e) {
            throw new SecretCipherException("le chiffrement du secret a echoue", e);
        }
    }

    @Override
    public String dechiffrer(String cryptogramme) {
        if (cryptogramme == null || cryptogramme.isBlank()) {
            throw new SecretCipherException("il n'y a rien a dechiffrer");
        }
        String[] parts = cryptogramme.split("\\.", 3);
        if (parts.length != 3 || !PREFIXE.equals(parts[0])) {
            throw new SecretCipherException("format de secret inconnu");
        }
        byte[] nonce;
        byte[] scelle;
        try {
            nonce = DECODEUR.decode(parts[1]);
            scelle = DECODEUR.decode(parts[2]);
        } catch (IllegalArgumentException e) {
            throw new SecretCipherException("le secret stocke n'est pas lisible", e);
        }
        if (nonce.length != NONCE_OCTETS) {
            throw new SecretCipherException("le secret stocke n'est pas lisible");
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, cle, new GCMParameterSpec(TAG_BITS, nonce));
            return new String(cipher.doFinal(scelle), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            // L'etiquette GCM n'est pas tombee juste : altere, tronque, ou
            // chiffre avec une autre cle. On ne distingue pas, et on ne rend
            // SURTOUT PAS une valeur approximative -- elle partirait vers un
            // serveur de messagerie tiers.
            throw new SecretCipherException("le secret stocke n'a pas pu etre dechiffre", e);
        } finally {
            Arrays.fill(scelle, (byte) 0);
        }
    }
}
