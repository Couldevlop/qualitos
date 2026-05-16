package com.openlab.qualitos.quality.apikeys.infrastructure;

import com.openlab.qualitos.quality.apikeys.domain.ApiKeySecretGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Génère un prefix (8 chars base64url) + secret (32 octets base64url ≈ 43 chars).
 * Entropie effective ≥ 256 bits, format : {@code qos_<prefix>_<secret>}.
 *
 * SecureRandom partagé pour amortir le coût d'init (thread-safe en Java 17+).
 */
@Component
public class SecureRandomApiKeySecretGenerator implements ApiKeySecretGenerator {

    private static final SecureRandom RNG = new SecureRandom();
    private static final Base64.Encoder URL = Base64.getUrlEncoder().withoutPadding();

    static final int PREFIX_BYTES = 6;   // 8 chars base64url
    static final int SECRET_BYTES = 32;  // 256 bits

    @Override
    public Material generate() {
        String prefix = URL.encodeToString(randomBytes(PREFIX_BYTES));
        String secret = URL.encodeToString(randomBytes(SECRET_BYTES));
        return new Material(prefix, secret, "qos_" + prefix + "_" + secret);
    }

    private static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        RNG.nextBytes(b);
        return b;
    }
}
