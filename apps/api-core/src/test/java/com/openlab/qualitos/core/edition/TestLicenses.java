package com.openlab.qualitos.core.edition;

import com.openlab.qualitos.crypto.domain.model.KeyMaterial;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;
import com.openlab.qualitos.crypto.infrastructure.BouncyCastleSignatureProvider;
import com.openlab.qualitos.licensing.application.LicenseIssuer;
import com.openlab.qualitos.licensing.application.LicenseVerifier;
import com.openlab.qualitos.licensing.application.Licensing;
import com.openlab.qualitos.licensing.domain.License;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * De vraies licences pour les bancs du moteur, signées par des clés de TEST
 * (jamais celles de l'éditeur) et vérifiées par un vérificateur qui les épingle.
 */
public final class TestLicenses {

    public static final Instant ISSUED = Instant.parse("2026-10-01T00:00:00Z");
    public static final Instant EXPIRES = ISSUED.plus(Duration.ofDays(365));

    private static final Map<SignatureAlgorithm, KeyMaterial> KEYS = new EnumMap<>(SignatureAlgorithm.class);

    static {
        KEYS.put(SignatureAlgorithm.ED25519, new BouncyCastleSignatureProvider(SignatureAlgorithm.ED25519)
                .generateKeyPair());
        KEYS.put(SignatureAlgorithm.ML_DSA_65, new BouncyCastleSignatureProvider(SignatureAlgorithm.ML_DSA_65)
                .generateKeyPair());
    }

    private TestLicenses() {}

    public static License license(UUID tenant, String tier, Set<String> modules, int maxUsers) {
        return new License("LIC-TEST-1", "Client de test", tenant, tier, modules, maxUsers, ISSUED, ISSUED,
                EXPIRES, 30);
    }

    public static String file(License license) {
        return new LicenseIssuer(KEYS, Clock.fixed(ISSUED, java.time.ZoneOffset.UTC)).issue(license);
    }

    public static LicenseVerifier verifier() {
        Map<SignatureAlgorithm, byte[]> pub = new EnumMap<>(SignatureAlgorithm.class);
        KEYS.forEach((a, k) -> pub.put(a, k.publicKey()));
        return new LicenseVerifier(pub);
    }

    /** Une installation on-premise portant cette licence, à l'instant donné. */
    public static Licensing onPrem(License license, Instant now) {
        String f = file(license);
        return Licensing.onPrem(() -> Optional.of(f), verifier(), Clock.fixed(now, java.time.ZoneOffset.UTC));
    }

    /** Une installation on-premise sans licence. */
    public static Licensing unlicensed(Instant now) {
        return Licensing.onPrem(Optional::empty, verifier(), Clock.fixed(now, java.time.ZoneOffset.UTC));
    }
}
