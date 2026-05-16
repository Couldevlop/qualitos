package com.openlab.qualitos.quality.webhooks;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Calcul de signature HMAC-SHA256 pour authenticite + integrite des webhooks
 * sortants (CLAUDE.md §13.2).
 *
 * Signature : HMAC-SHA256(secret, timestamp + "." + payload)
 * Format header : "sha256=<hex>"
 *
 * Anti-replay : le timestamp inclus dans la signature permet au consommateur
 * de rejeter les requetes plus vieilles que ~5 min.
 */
@Component
public class HmacSigner {

    static final String ALGORITHM = "HmacSHA256";

    /**
     * Calcule la signature pour un payload + timestamp donnes.
     * @param secret clé HMAC (≥ 16 octets recommandé)
     * @param timestampMs epoch millisecondes à inclure dans le tampon signé
     * @param payload corps de la requête (JSON serialisé)
     * @return signature au format "sha256=<hex 64 chars>"
     */
    public String sign(String secret, long timestampMs, String payload) {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalArgumentException("secret is required");
        }
        if (payload == null) {
            payload = "";
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            String signed = timestampMs + "." + payload;
            byte[] sig = mac.doFinal(signed.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(sig);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC computation failed", e);
        }
    }

    /**
     * Vérification constante-temps pour éviter les attaques par timing.
     * @return true si la signature attendue match.
     */
    public boolean verify(String secret, long timestampMs, String payload, String signature) {
        String expected = sign(secret, timestampMs, payload);
        if (expected.length() != (signature == null ? 0 : signature.length())) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }
}
