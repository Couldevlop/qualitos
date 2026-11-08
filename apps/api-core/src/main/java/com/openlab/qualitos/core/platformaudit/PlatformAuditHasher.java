package com.openlab.qualitos.core.platformaudit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;

/**
 * L'empreinte SHA-256 d'un acte, chaînée sur celle du précédent.
 *
 * <p>Classe pure, sans Spring : elle se teste sans contexte, et c'est ce qui
 * permet de vérifier la chaîne d'un journal exporté depuis n'importe où, sans
 * la plateforme.
 *
 * <p><b>Le séparateur de champs est un caractère de contrôle</b>
 * ({@code U+001E}, record separator) et non un point-virgule ni une barre
 * verticale. Un séparateur imprimable est un séparateur que l'on peut TAPER :
 * un résumé contenant le caractère déplacerait la frontière entre deux champs
 * et deux actes différents pourraient produire la même empreinte. Un caractère
 * de contrôle n'arrive pas par un formulaire.
 *
 * <p><b>Pourquoi l'adresse IP et le navigateur entrent ici</b>, alors que le
 * journal du moteur de qualité les laisse hors de son empreinte : ils
 * décrivent la PROVENANCE d'un acte d'administration, et c'est une des
 * premières choses qu'une autorité recoupe (« ce changement de tarif vient-il
 * du réseau de l'éditeur ? »). Hors empreinte, ils seraient modifiables sans
 * que la vérification de chaîne s'en aperçoive — autant ne pas les écrire.
 */
public final class PlatformAuditHasher {

    private static final char SEP = '\u001E';

    private PlatformAuditHasher() {
    }

    public static String hash(PlatformAuditEvent event) {
        StringBuilder canonique = new StringBuilder(512);
        canonique.append(event.getSequenceNo()).append(SEP);
        canonique.append(texte(event.getOccurredAt())).append(SEP);
        canonique.append(nom(event.getActorType())).append(SEP);
        canonique.append(texte(event.getActorUserId())).append(SEP);
        canonique.append(nom(event.getAction())).append(SEP);
        canonique.append(texte(event.getTargetType())).append(SEP);
        canonique.append(texte(event.getTargetId())).append(SEP);
        canonique.append(texte(event.getTargetTenantId())).append(SEP);
        canonique.append(texte(event.getSummary())).append(SEP);
        canonique.append(texte(event.getPayloadJson())).append(SEP);
        canonique.append(texte(event.getIpAddress())).append(SEP);
        canonique.append(texte(event.getUserAgent())).append(SEP);
        canonique.append(texte(event.getPreviousHash()));
        return sha256(canonique.toString());
    }

    /** L'empreinte hexadécimale d'une chaîne, en minuscules, sur 64 caractères. */
    public static String sha256(String entree) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] condense = digest.digest(entree.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexa = new StringBuilder(condense.length * 2);
            for (byte octet : condense) {
                hexa.append(Character.forDigit((octet >> 4) & 0xF, 16));
                hexa.append(Character.forDigit(octet & 0xF, 16));
            }
            return hexa.toString();
        } catch (NoSuchAlgorithmException impossible) {
            // SHA-256 est exigé de toute implémentation de la plateforme Java.
            throw new IllegalStateException("SHA-256 indisponible", impossible);
        }
    }

    private static String texte(UUID valeur) {
        return valeur == null ? "" : valeur.toString();
    }

    private static String texte(Instant valeur) {
        return valeur == null ? "" : valeur.toString();
    }

    private static String texte(String valeur) {
        return valeur == null ? "" : valeur;
    }

    private static String nom(Enum<?> valeur) {
        return valeur == null ? "" : valeur.name();
    }
}
