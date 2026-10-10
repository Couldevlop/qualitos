package com.openlab.qualitos.licensing.application;

import com.openlab.qualitos.crypto.domain.model.KeyMaterial;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;
import com.openlab.qualitos.crypto.domain.model.SignatureEnvelope;
import com.openlab.qualitos.crypto.infrastructure.BouncyCastleSignatureProvider;
import com.openlab.qualitos.licensing.domain.License;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Émet une licence : côté ÉDITEUR seulement, avec ses clés privées, hors des
 * installations clientes (ADR 0082). Aucune installation ne porte de clé privée :
 * elles ne savent que vérifier.
 */
public final class LicenseIssuer {

    static final String SUITE = "qualitos-license";

    private final Map<SignatureAlgorithm, KeyMaterial> keys;
    private final Clock clock;

    public LicenseIssuer(Map<SignatureAlgorithm, KeyMaterial> editorKeys, Clock clock) {
        for (SignatureAlgorithm a : LicenseVerifier.REQUIRED) {
            if (!editorKeys.containsKey(a)) {
                throw new IllegalArgumentException("Clé privée de l'éditeur manquante : " + a);
            }
        }
        this.keys = Map.copyOf(editorKeys);
        this.clock = clock;
    }

    /** Le contenu du fichier de licence, prêt à remettre au client. */
    public String issue(License license) {
        byte[] payload = LicenseCodec.toBytes(license);
        List<SignatureEnvelope.Part> parts = LicenseVerifier.REQUIRED.stream().sorted().map(a -> {
            KeyMaterial km = keys.get(a);
            byte[] sig = new BouncyCastleSignatureProvider(a).sign(km.privateKey(), payload);
            return new SignatureEnvelope.Part(a, km.publicKey(), sig);
        }).toList();
        SignatureEnvelope envelope = new SignatureEnvelope(SignatureEnvelope.CURRENT_VERSION, SUITE,
                keyRef(keys.get(SignatureAlgorithm.ML_DSA_65).publicKey()), clock.instant(), parts);
        return new LicenseFile(payload, envelope.encode()).write();
    }

    /** L'empreinte courte de la clé ML-DSA de l'éditeur : dit quelle clé a signé, sans rien prouver. */
    static String keyRef(byte[] publicKey) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(publicKey);
            return "editor-" + HexFormat.of().formatHex(h, 0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
