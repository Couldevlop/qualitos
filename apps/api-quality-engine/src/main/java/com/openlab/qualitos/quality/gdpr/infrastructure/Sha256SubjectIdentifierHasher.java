package com.openlab.qualitos.quality.gdpr.infrastructure;

import com.openlab.qualitos.quality.gdpr.domain.SubjectIdentifierHasher;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** SHA-256 hex (64 chars). Pas de salt — un même identifiant doit hasher
 *  de façon déterministe pour permettre la recherche (RGPD Art. 15 - retrouver
 *  toutes les demandes d'une même personne sans stocker son email en clair). */
@Component
public class Sha256SubjectIdentifierHasher implements SubjectIdentifierHasher {

    @Override
    public String hash(String rawIdentifier) {
        if (rawIdentifier == null) throw new IllegalArgumentException("rawIdentifier required");
        try {
            byte[] d = MessageDigest.getInstance("SHA-256")
                    .digest(rawIdentifier.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(d.length * 2);
            for (byte b : d) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
