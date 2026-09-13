package com.openlab.qualitos.quality.nonconformity.eightd.infrastructure;

import com.openlab.qualitos.crypto.application.HybridSignatureService;
import com.openlab.qualitos.crypto.domain.model.SignatureEnvelope;
import com.openlab.qualitos.quality.blockchain.domain.BlockchainAnchorPort;
import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDSealPort;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Signe puis ancre l'empreinte d'un rapport 8D émis — en réutilisant le moteur de
 * la plateforme, pas un second.
 *
 * <p>Signature hybride (Ed25519 + ML-DSA-65, ADR 0011) : un 8D remis à un client
 * automobile reste opposable des années, et le jour où le classique ne tiendra
 * plus, le post-quantique tiendra encore (§11.4). Ancrage par le port commun :
 * reçu chaîné signé en développement, Hyperledger Fabric en production, sans que
 * ce module ait à savoir lequel des deux répond.
 */
@Component
public class EightDSealAdapter implements EightDSealPort {

    /**
     * Contexte de suite crypto. Il sépare les usages : une signature de rapport 8D
     * ne doit pas pouvoir être présentée comme une signature de control plan ou de
     * certificat de formation, alors même que les trois signent un SHA-256 de
     * 64 caractères.
     */
    static final String SIGN_CONTEXT = "eightd-report";

    private final HybridSignatureService signer;
    private final BlockchainAnchorPort blockchain;

    public EightDSealAdapter(HybridSignatureService signer, BlockchainAnchorPort blockchain) {
        this.signer = signer;
        this.blockchain = blockchain;
    }

    @Override
    public Seal seal(UUID tenantId, String sha256Hex) {
        SignatureEnvelope envelope =
                signer.sign(SIGN_CONTEXT, sha256Hex.getBytes(StandardCharsets.UTF_8));
        // L'ancrage vient APRÈS la signature : ancrer une empreinte qu'on n'aurait pas
        // su signer laisserait sur la chaîne la trace d'un document dont personne ne
        // peut prouver l'origine.
        String txRef = blockchain.submitRoot(tenantId, sha256Hex);
        return new Seal(envelope.encode(), txRef);
    }

    @Override
    public boolean verify(String signature, String sha256Hex) {
        if (signature == null || sha256Hex == null) {
            return false;
        }
        try {
            SignatureEnvelope envelope = SignatureEnvelope.decode(signature);
            return signer.verify(sha256Hex.getBytes(StandardCharsets.UTF_8), envelope);
        } catch (RuntimeException ex) {
            // Enveloppe illisible ou altérée : invalide, et rien de plus. Lever ici
            // ferait répondre 500 à la route publique de vérification, ce qui
            // distinguerait « altéré » de « inconnu ».
            return false;
        }
    }
}
