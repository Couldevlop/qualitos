package com.openlab.qualitos.quality.blockchain.application;

import com.openlab.qualitos.crypto.application.CryptoSuiteConfig;
import com.openlab.qualitos.crypto.application.HybridSignatureService;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;
import com.openlab.qualitos.crypto.domain.port.SignatureProvider;
import com.openlab.qualitos.crypto.infrastructure.BouncyCastleSignatureProvider;
import com.openlab.qualitos.crypto.infrastructure.InMemorySigningKeyProvider;
import com.openlab.qualitos.quality.blockchain.domain.AnchorReadPort;
import com.openlab.qualitos.quality.blockchain.domain.AnchorVerificationResult;
import com.openlab.qualitos.quality.blockchain.domain.MerkleTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnchorVerificationServiceTest {

    @Mock AnchorReadPort read;

    static final UUID TENANT = UUID.randomUUID();
    static final String TX = UUID.randomUUID().toString();
    static final List<String> LEAVES = List.of("h1", "h2", "h3");
    static final String ROOT = MerkleTree.root(LEAVES);

    HybridSignatureService signer;
    AnchorVerificationService service;

    private static HybridSignatureService newSigner(String keyRef) {
        return new HybridSignatureService(
                new CryptoSuiteConfig(),
                List.<SignatureProvider>of(
                        new BouncyCastleSignatureProvider(SignatureAlgorithm.ED25519),
                        new BouncyCastleSignatureProvider(SignatureAlgorithm.ML_DSA_65)),
                new InMemorySigningKeyProvider(keyRef,
                        algo -> new BouncyCastleSignatureProvider(algo).generateKeyPair()),
                Clock.systemUTC());
    }

    private String signatureOver(String root, HybridSignatureService s) {
        return s.sign("blockchain-block", root.getBytes(StandardCharsets.UTF_8)).encode();
    }

    @BeforeEach
    void setup() {
        signer = newSigner("platform-v1");
        service = new AnchorVerificationService(read, signer);
    }

    @Test
    void verify_returnsVerified_whenRootAndSignatureMatch() {
        when(read.txRefForEvent(TENANT, "h2")).thenReturn(Optional.of(TX));
        when(read.receipt(TENANT, TX)).thenReturn(
                Optional.of(new AnchorReadPort.ReceiptView(ROOT, signatureOver(ROOT, signer))));
        when(read.integrityHashesForTxRef(TENANT, TX)).thenReturn(LEAVES);

        AnchorVerificationResult result = service.verify(TENANT, "h2");

        assertThat(result.status()).isEqualTo(AnchorVerificationResult.Status.VERIFIED);
        assertThat(result.txRef()).isEqualTo(TX);
        assertThat(result.merkleRoot()).isEqualTo(ROOT);
    }

    @Test
    void verify_notAnchored_whenEventUnknown() {
        when(read.txRefForEvent(TENANT, "x")).thenReturn(Optional.empty());
        assertThat(service.verify(TENANT, "x").status())
                .isEqualTo(AnchorVerificationResult.Status.NOT_ANCHORED);
    }

    @Test
    void verify_notAnchored_whenReceiptMissing() {
        when(read.txRefForEvent(TENANT, "h2")).thenReturn(Optional.of(TX));
        when(read.receipt(TENANT, TX)).thenReturn(Optional.empty());
        assertThat(service.verify(TENANT, "h2").status())
                .isEqualTo(AnchorVerificationResult.Status.NOT_ANCHORED);
    }

    @Test
    void verify_tampered_whenEventAbsentFromBatch() {
        when(read.txRefForEvent(TENANT, "h2")).thenReturn(Optional.of(TX));
        when(read.receipt(TENANT, TX)).thenReturn(
                Optional.of(new AnchorReadPort.ReceiptView(ROOT, signatureOver(ROOT, signer))));
        when(read.integrityHashesForTxRef(TENANT, TX)).thenReturn(List.of("h1", "h3")); // h2 retiré

        assertThat(service.verify(TENANT, "h2").status())
                .isEqualTo(AnchorVerificationResult.Status.TAMPERED);
    }

    @Test
    void verify_tampered_whenRecomputedRootDiffers() {
        when(read.txRefForEvent(TENANT, "h2")).thenReturn(Optional.of(TX));
        when(read.receipt(TENANT, TX)).thenReturn(
                Optional.of(new AnchorReadPort.ReceiptView(ROOT, signatureOver(ROOT, signer))));
        // Le lot courant diffère de celui qui a produit ROOT → racine recalculée ≠ reçu.
        when(read.integrityHashesForTxRef(TENANT, TX)).thenReturn(List.of("h2", "h9"));

        assertThat(service.verify(TENANT, "h2").status())
                .isEqualTo(AnchorVerificationResult.Status.TAMPERED);
    }

    @Test
    void verify_tampered_whenSignatureFromForeignKey() {
        HybridSignatureService attacker = newSigner("attacker-v1");
        when(read.txRefForEvent(TENANT, "h2")).thenReturn(Optional.of(TX));
        when(read.receipt(TENANT, TX)).thenReturn(
                Optional.of(new AnchorReadPort.ReceiptView(ROOT, signatureOver(ROOT, attacker))));
        when(read.integrityHashesForTxRef(TENANT, TX)).thenReturn(LEAVES);

        // Root correct mais signé par une autre clé → épinglage échoue → TAMPERED.
        assertThat(service.verify(TENANT, "h2").status())
                .isEqualTo(AnchorVerificationResult.Status.TAMPERED);
    }

    @Test
    void verify_notAnchored_whenHashBlank() {
        assertThat(service.verify(TENANT, "  ").status())
                .isEqualTo(AnchorVerificationResult.Status.NOT_ANCHORED);
    }
}
