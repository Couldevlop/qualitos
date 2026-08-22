package com.openlab.qualitos.quality.controlplan.infrastructure;

import com.openlab.qualitos.crypto.application.HybridSignatureService;
import com.openlab.qualitos.crypto.domain.model.SignatureEnvelope;
import com.openlab.qualitos.quality.blockchain.domain.BlockchainAnchorPort;
import com.openlab.qualitos.quality.controlplan.application.ControlPlanSealPort;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Signe puis ancre l'empreinte d'un control plan approuvé.
 *
 * <p>La signature est hybride (Ed25519 + ML-DSA-65) : un document de production
 * reste opposable des années, et un jour où le classique ne tiendra plus, le
 * post-quantique tiendra encore (§11.4).
 *
 * <p>L'ancrage réutilise le port de la plateforme : stub en développement,
 * Hyperledger Fabric en production, sans que le module qualité ait à savoir
 * lequel des deux répond.
 */
@Component
public class ControlPlanSealAdapter implements ControlPlanSealPort {

    /**
     * Contexte de signature. Il sépare les usages : une signature de control plan
     * ne doit pas pouvoir être présentée comme une signature de certificat de
     * formation, alors même que les deux signent un SHA-256 de 64 caractères.
     */
    static final String SIGN_CONTEXT = "control-plan";

    private final HybridSignatureService signer;
    private final BlockchainAnchorPort blockchain;

    public ControlPlanSealAdapter(HybridSignatureService signer, BlockchainAnchorPort blockchain) {
        this.signer = signer;
        this.blockchain = blockchain;
    }

    @Override
    public Seal seal(UUID tenantId, String sha256Hex) {
        SignatureEnvelope envelope =
                signer.sign(SIGN_CONTEXT, sha256Hex.getBytes(StandardCharsets.UTF_8));
        // L'ancrage vient APRÈS la signature : ancrer une empreinte qu'on n'aurait
        // pas su signer laisserait sur la chaîne la trace d'un document dont
        // personne ne peut prouver l'origine.
        String txRef = blockchain.submitRoot(tenantId, sha256Hex);
        return new Seal(envelope.encode(), txRef);
    }
}
