package com.openlab.qualitos.quality.standards.normdoc.dossier.infrastructure;

import com.openlab.qualitos.crypto.application.HybridSignatureService;
import com.openlab.qualitos.crypto.domain.model.SignatureEnvelope;
import com.openlab.qualitos.quality.blockchain.domain.BlockchainAnchorPort;
import com.openlab.qualitos.quality.standards.normdoc.dossier.application.DossierIntegrityPort;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Adapter du port {@link DossierIntegrityPort} : scelle l'intégrité d'un dossier
 * finalisé. Calcule l'empreinte SHA-256 du contenu agrégé, la signe en hybride
 * (Ed25519 + ML-DSA-65) via {@link HybridSignatureService} et ancre l'empreinte
 * en blockchain via {@link BlockchainAnchorPort} (preuve audit-ready, §8.7 /
 * §11.3/§11.4). Réutilise l'infra crypto/blockchain existante du module standards
 * (même contexte de suite que les dossiers de certification).
 */
@Component
public class SignedAnchorDossierIntegrityAdapter implements DossierIntegrityPort {

    /** Contexte de suite crypto pour les artefacts de preuve signés (ADR 0011). */
    private static final String SIGN_CONTEXT = "audit-report";

    private final HybridSignatureService signer;
    private final BlockchainAnchorPort blockchain;

    public SignedAnchorDossierIntegrityAdapter(HybridSignatureService signer,
                                               BlockchainAnchorPort blockchain) {
        this.signer = signer;
        this.blockchain = blockchain;
    }

    @Override
    public Sealed seal(UUID tenantId, String canonicalContent) {
        String sha256 = sha256(canonicalContent);
        SignatureEnvelope envelope = signer.sign(
                SIGN_CONTEXT, sha256.getBytes(StandardCharsets.UTF_8));
        String signature = envelope.encode();
        String anchorTxRef = blockchain.submitRoot(tenantId, sha256);
        return new Sealed(sha256, signature, anchorTxRef);
    }

    private String sha256(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte x : hash) {
                hex.append(String.format("%02x", x));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
