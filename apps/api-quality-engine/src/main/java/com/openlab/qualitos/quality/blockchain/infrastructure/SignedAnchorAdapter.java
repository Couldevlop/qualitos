package com.openlab.qualitos.quality.blockchain.infrastructure;

import com.openlab.qualitos.crypto.application.HybridSignatureService;
import com.openlab.qualitos.crypto.domain.model.SignatureEnvelope;
import com.openlab.qualitos.quality.blockchain.domain.BlockchainAnchorPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Ancrage Phase A (ADR 0012) : signe le Merkle root avec une enveloppe hybride
 * (Ed25519 + ML-DSA-65, ADR 0011) et persiste un <b>reçu chaîné append-only</b>.
 * Notarisation cryptographiquement vérifiable, sans réseau Fabric (Phase B).
 *
 * <p>Le {@code txRef} retourné est l'id du reçu — stocké sur chaque
 * {@code AuditEvent} du lot ({@code blockchainTxRef}) par {@code AnchoringService}.
 */
@Component
@Profile("!test")
public class SignedAnchorAdapter implements BlockchainAnchorPort {

    private static final Logger log = LoggerFactory.getLogger(SignedAnchorAdapter.class);

    /** Hash de genèse (premier reçu d'un tenant) — 64 zéros hex. */
    static final String GENESIS_HASH = "0".repeat(64);

    /** Contexte de suite crypto pour les blocs d'ancrage (ADR 0011). */
    private static final String SIGN_CONTEXT = "blockchain-block";

    private final AnchorReceiptRepository receipts;
    private final HybridSignatureService signer;
    private final Clock clock;

    public SignedAnchorAdapter(AnchorReceiptRepository receipts,
                               HybridSignatureService signer,
                               Clock clock) {
        this.receipts = receipts;
        this.signer = signer;
        this.clock = clock;
    }

    @Override
    @Transactional
    public String submitRoot(UUID tenantId, String merkleRootHex) {
        AnchorReceiptEntity prev =
                receipts.findTopByTenantIdOrderBySeqNoDesc(tenantId).orElse(null);
        String prevHash = prev != null ? prev.getReceiptHash() : GENESIS_HASH;
        long seqNo = prev != null ? prev.getSeqNo() + 1 : 1L;

        Instant signedAt = Instant.now(clock);
        SignatureEnvelope envelope =
                signer.sign(SIGN_CONTEXT, merkleRootHex.getBytes(StandardCharsets.UTF_8));
        String receiptHash = sha256Hex(
                merkleRootHex + "|" + prevHash + "|" + signedAt.toEpochMilli() + "|" + seqNo);

        AnchorReceiptEntity receipt = new AnchorReceiptEntity(
                UUID.randomUUID(), tenantId, seqNo, merkleRootHex, prevHash,
                receiptHash, envelope.encode(), signedAt, signedAt);
        receipts.save(receipt);

        log.info("[anchor] tenant={} seq={} root={} receipt={}",
                tenantId, seqNo, merkleRootHex, receipt.getId());
        return receipt.getId().toString();
    }

    private static String sha256Hex(String input) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(d.length * 2);
            for (byte b : d) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
