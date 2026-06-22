package com.openlab.qualitos.quality.standards.normdoc.dossier.infrastructure;

import com.openlab.qualitos.crypto.application.HybridSignatureService;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;
import com.openlab.qualitos.crypto.domain.model.SignatureEnvelope;
import com.openlab.qualitos.quality.blockchain.domain.BlockchainAnchorPort;
import com.openlab.qualitos.quality.standards.normdoc.dossier.application.DossierIntegrityPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignedAnchorDossierIntegrityAdapterTest {

    @Mock HybridSignatureService signer;
    @Mock BlockchainAnchorPort blockchain;

    SignedAnchorDossierIntegrityAdapter adapter;

    static final UUID TENANT = UUID.randomUUID();

    @BeforeEach
    void setup() {
        adapter = new SignedAnchorDossierIntegrityAdapter(signer, blockchain);
    }

    /** Enveloppe réelle (record final, non mockable) — une part ED25519 factice. */
    private static SignatureEnvelope envelope() {
        return new SignatureEnvelope(
                SignatureEnvelope.CURRENT_VERSION, "qualitos-hybrid", "key-1",
                Instant.parse("2026-06-22T09:00:00Z"),
                List.of(new SignatureEnvelope.Part(
                        SignatureAlgorithm.ED25519, new byte[]{1, 2, 3}, new byte[]{4, 5, 6})));
    }

    @Test
    void seal_computesSha256_signsAndAnchors() {
        when(signer.sign(eq("audit-report"), any(byte[].class))).thenReturn(envelope());
        when(blockchain.submitRoot(eq(TENANT), any())).thenReturn("tx-ref-42");

        DossierIntegrityPort.Sealed sealed = adapter.seal(TENANT, "contenu du dossier");

        assertThat(sealed.sha256()).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(sealed.signature()).isNotBlank();
        assertThat(sealed.anchorTxRef()).isEqualTo("tx-ref-42");

        // l'empreinte signée == l'empreinte ancrée (même SHA-256)
        ArgumentCaptor<String> anchored = ArgumentCaptor.forClass(String.class);
        verify(blockchain).submitRoot(eq(TENANT), anchored.capture());
        assertThat(anchored.getValue()).isEqualTo(sealed.sha256());
    }

    @Test
    void seal_isDeterministicForSameContent() {
        when(signer.sign(any(), any())).thenReturn(envelope());
        when(blockchain.submitRoot(any(), any())).thenReturn("tx");

        String h1 = adapter.seal(TENANT, "même contenu").sha256();
        String h2 = adapter.seal(TENANT, "même contenu").sha256();
        assertThat(h1).isEqualTo(h2);
    }
}
