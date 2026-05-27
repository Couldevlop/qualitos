package com.openlab.qualitos.quality.blockchain.application;

import com.openlab.qualitos.crypto.application.HybridSignatureService;
import com.openlab.qualitos.crypto.domain.model.SignatureEnvelope;
import com.openlab.qualitos.quality.blockchain.domain.AnchorReadPort;
import com.openlab.qualitos.quality.blockchain.domain.AnchorVerificationResult;
import com.openlab.qualitos.quality.blockchain.domain.MerkleTree;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Vérifie l'intégrité d'un événement d'audit ancré (ADR 0012 Phase A) :
 * recalcule la racine Merkle de son lot depuis les événements actuels, la
 * confronte au reçu signé, et vérifie la signature hybride du reçu.
 *
 * <p>Aucune dépendance d'infrastructure : ne dépend que du port
 * {@link AnchorReadPort}, de {@link MerkleTree} (domaine) et du service de
 * signature (application crypto).
 */
public class AnchorVerificationService {

    private final AnchorReadPort read;
    private final HybridSignatureService signer;

    public AnchorVerificationService(AnchorReadPort read, HybridSignatureService signer) {
        this.read = read;
        this.signer = signer;
    }

    public AnchorVerificationResult verify(UUID tenantId, String integrityHash) {
        if (integrityHash == null || integrityHash.isBlank()) {
            return AnchorVerificationResult.notAnchored("hash manquant");
        }
        Optional<String> txRefOpt = read.txRefForEvent(tenantId, integrityHash);
        if (txRefOpt.isEmpty()) {
            return AnchorVerificationResult.notAnchored("événement inconnu ou non ancré");
        }
        String txRef = txRefOpt.get();

        Optional<AnchorReadPort.ReceiptView> receiptOpt = read.receipt(tenantId, txRef);
        if (receiptOpt.isEmpty()) {
            return AnchorVerificationResult.notAnchored("reçu d'ancrage introuvable: " + txRef);
        }
        AnchorReadPort.ReceiptView receipt = receiptOpt.get();

        List<String> leaves = read.integrityHashesForTxRef(tenantId, txRef);
        if (leaves.isEmpty() || !leaves.contains(integrityHash)) {
            return AnchorVerificationResult.tampered(
                    "événement absent de son lot d'ancrage", txRef, receipt.merkleRoot());
        }

        String recomputedRoot = MerkleTree.root(leaves);
        if (!recomputedRoot.equals(receipt.merkleRoot())) {
            return AnchorVerificationResult.tampered(
                    "racine Merkle recalculée ≠ reçu (lot altéré)", txRef, receipt.merkleRoot());
        }

        boolean signatureValid = signer.verify(
                receipt.merkleRoot().getBytes(StandardCharsets.UTF_8),
                SignatureEnvelope.decode(receipt.signature()));
        if (!signatureValid) {
            return AnchorVerificationResult.tampered(
                    "signature du reçu invalide", txRef, receipt.merkleRoot());
        }
        return AnchorVerificationResult.verified(txRef, receipt.merkleRoot());
    }
}
