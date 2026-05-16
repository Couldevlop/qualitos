package com.openlab.qualitos.quality.consent.infrastructure;

import com.openlab.qualitos.quality.consent.domain.SubjectIdentifierHasher;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** SHA-256 hex (64 chars). Pas de salt — hash déterministe pour permettre la
 *  recherche "consentements de cette personne" sans stocker la PII brute. */
@Component("consentSha256Hasher")
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
