package com.openlab.qualitos.quality.apikeys.infrastructure;

import com.openlab.qualitos.quality.apikeys.domain.ApiKeyHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adapter bcrypt strength 12 (≈ 250 ms / hash sur matériel récent — coût
 * intentionnel pour résister au brute-force). Spring Security {@code matches}
 * fait une comparaison à temps constant.
 */
@Component
public class BCryptApiKeyHasher implements ApiKeyHasher {

    static final int STRENGTH = 12;

    private final BCryptPasswordEncoder encoder;

    public BCryptApiKeyHasher() {
        this.encoder = new BCryptPasswordEncoder(STRENGTH);
    }

    @Override
    public String hash(String rawSecret) { return encoder.encode(rawSecret); }

    @Override
    public boolean matches(String rawSecret, String hashedSecret) {
        if (rawSecret == null || hashedSecret == null) return false;
        return encoder.matches(rawSecret, hashedSecret);
    }
}
