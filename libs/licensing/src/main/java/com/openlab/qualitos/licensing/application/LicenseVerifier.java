package com.openlab.qualitos.licensing.application;

import com.openlab.qualitos.crypto.domain.CryptoException;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;
import com.openlab.qualitos.crypto.domain.model.SignatureEnvelope;
import com.openlab.qualitos.crypto.domain.port.SignatureProvider;
import com.openlab.qualitos.crypto.infrastructure.BouncyCastleSignatureProvider;
import com.openlab.qualitos.licensing.domain.License;
import com.openlab.qualitos.licensing.domain.LicenseException;

import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Vérifie une licence sans aucun appel réseau (ADR 0082).
 *
 * <p>La signature est HYBRIDE : Ed25519 et ML-DSA-65 doivent l'être toutes les
 * deux, chacune par la clé publique de l'éditeur ÉPINGLÉE dans l'application. La
 * clé que porte le fichier n'est jamais crue sur parole : elle doit être celle
 * qu'on attend, sinon n'importe qui signerait sa propre licence avec sa propre
 * clé. Une partie de signature en trop, ou d'un algorithme inattendu, est refusée.
 */
public final class LicenseVerifier {

    /** Les deux algorithmes exigés : classique, et résistant au quantique. */
    public static final Set<SignatureAlgorithm> REQUIRED = Set.of(SignatureAlgorithm.ED25519,
            SignatureAlgorithm.ML_DSA_65);

    private final Map<SignatureAlgorithm, byte[]> trusted;
    private final Map<SignatureAlgorithm, SignatureProvider> providers = new EnumMap<>(SignatureAlgorithm.class);

    public LicenseVerifier(Map<SignatureAlgorithm, byte[]> trustedPublicKeys) {
        for (SignatureAlgorithm a : REQUIRED) {
            byte[] k = trustedPublicKeys.get(a);
            if (k == null || k.length == 0) {
                throw new IllegalArgumentException("Clé publique de l'éditeur manquante : " + a);
            }
            providers.put(a, new BouncyCastleSignatureProvider(a));
        }
        this.trusted = new EnumMap<>(trustedPublicKeys);
    }

    /** La licence que porte le fichier, si — et seulement si — l'éditeur l'a signée. */
    public License verify(String fileContent) {
        LicenseFile file = LicenseFile.parse(fileContent);
        SignatureEnvelope envelope;
        try {
            envelope = SignatureEnvelope.decode(file.signature());
        } catch (IllegalArgumentException | UncheckedIOException e) {
            throw new LicenseException("Signature de licence illisible.", e);
        }
        List<SignatureEnvelope.Part> parts = envelope.parts();
        if (parts.size() != REQUIRED.size()
                || !parts.stream().map(SignatureEnvelope.Part::algorithm).toList().containsAll(REQUIRED)) {
            throw new LicenseException("La licence doit être signée en Ed25519 et en ML-DSA-65.");
        }
        for (SignatureEnvelope.Part part : parts) {
            if (!MessageDigest.isEqual(trusted.get(part.algorithm()), part.publicKey())) {
                throw new LicenseException("La licence n'a pas été signée par l'éditeur de QualitOS.");
            }
            boolean ok;
            try {
                ok = providers.get(part.algorithm()).verify(part.publicKey(), file.payload(), part.signature());
            } catch (CryptoException e) {
                ok = false;
            }
            if (!ok) {
                throw new LicenseException("Signature de licence invalide : le fichier a été modifié.");
            }
        }
        return LicenseCodec.fromBytes(file.payload());
    }
}
