package com.openlab.qualitos.quality.blockchain.infrastructure;

import com.openlab.qualitos.crypto.application.CryptoSuiteConfig;
import com.openlab.qualitos.crypto.application.HybridSignatureService;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;
import com.openlab.qualitos.crypto.domain.model.SignatureEnvelope;
import com.openlab.qualitos.crypto.domain.port.SignatureProvider;
import com.openlab.qualitos.crypto.infrastructure.BouncyCastleSignatureProvider;
import com.openlab.qualitos.crypto.infrastructure.InMemorySigningKeyProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignedAnchorAdapterTest {

    @Mock AnchorReceiptRepository receipts;

    static final UUID TENANT = UUID.randomUUID();
    static final String ROOT = "a".repeat(64);
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-26T12:00:00Z"), ZoneOffset.UTC);

    HybridSignatureService signer;
    SignedAnchorAdapter adapter;

    @BeforeEach
    void setup() {
        signer = new HybridSignatureService(
                new CryptoSuiteConfig(),
                java.util.List.<SignatureProvider>of(
                        new BouncyCastleSignatureProvider(SignatureAlgorithm.ED25519),
                        new BouncyCastleSignatureProvider(SignatureAlgorithm.ML_DSA_65)),
                new InMemorySigningKeyProvider("platform-v1",
                        algo -> new BouncyCastleSignatureProvider(algo).generateKeyPair()),
                CLOCK);
        adapter = new SignedAnchorAdapter(receipts, signer, CLOCK);
    }

    @Test
    void submitRoot_genesis_signsAndPersistsChainedReceipt() {
        when(receipts.findTopByTenantIdOrderBySeqNoDesc(TENANT)).thenReturn(Optional.empty());

        String txRef = adapter.submitRoot(TENANT, ROOT);

        ArgumentCaptor<AnchorReceiptEntity> captor = ArgumentCaptor.forClass(AnchorReceiptEntity.class);
        verify(receipts).save(captor.capture());
        AnchorReceiptEntity saved = captor.getValue();

        assertThat(txRef).isEqualTo(saved.getId().toString());
        assertThat(saved.getTenantId()).isEqualTo(TENANT);
        assertThat(saved.getSeqNo()).isEqualTo(1L);
        assertThat(saved.getMerkleRoot()).isEqualTo(ROOT);
        assertThat(saved.getPrevReceiptHash()).isEqualTo(SignedAnchorAdapter.GENESIS_HASH);
        assertThat(saved.getReceiptHash()).matches("[0-9a-f]{64}");

        // La signature persistée est une vraie enveloppe hybride vérifiable sur le root.
        SignatureEnvelope envelope = SignatureEnvelope.decode(saved.getSignature());
        assertThat(envelope.parts()).extracting(SignatureEnvelope.Part::algorithm)
                .containsExactly(SignatureAlgorithm.ED25519, SignatureAlgorithm.ML_DSA_65);
        assertThat(signer.verify(ROOT.getBytes(StandardCharsets.UTF_8), envelope)).isTrue();
    }

    @Test
    void submitRoot_chainsToPreviousReceipt() {
        AnchorReceiptEntity prev = new AnchorReceiptEntity(
                UUID.randomUUID(), TENANT, 5L, "b".repeat(64),
                SignedAnchorAdapter.GENESIS_HASH, "c".repeat(64), "sig", Instant.now(CLOCK), Instant.now(CLOCK));
        when(receipts.findTopByTenantIdOrderBySeqNoDesc(TENANT)).thenReturn(Optional.of(prev));

        adapter.submitRoot(TENANT, ROOT);

        ArgumentCaptor<AnchorReceiptEntity> captor = ArgumentCaptor.forClass(AnchorReceiptEntity.class);
        verify(receipts).save(captor.capture());
        AnchorReceiptEntity saved = captor.getValue();

        assertThat(saved.getSeqNo()).isEqualTo(6L);
        assertThat(saved.getPrevReceiptHash()).isEqualTo("c".repeat(64)); // = receiptHash du précédent
    }
}
