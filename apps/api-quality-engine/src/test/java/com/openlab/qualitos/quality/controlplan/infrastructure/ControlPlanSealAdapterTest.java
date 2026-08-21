package com.openlab.qualitos.quality.controlplan.infrastructure;

import com.openlab.qualitos.crypto.application.HybridSignatureService;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;
import com.openlab.qualitos.crypto.domain.model.SignatureEnvelope;
import com.openlab.qualitos.quality.blockchain.domain.BlockchainAnchorPort;
import com.openlab.qualitos.quality.controlplan.application.ControlPlanSealPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le scellement d'un control plan : signature hybride puis ancrage.
 *
 * <p>Deux points tiennent la preuve. L'ordre — signer avant d'ancrer, sinon la
 * chaîne porterait la trace d'un document dont personne ne peut prouver
 * l'origine. Et le contexte de signature, qui empêche de présenter la signature
 * d'un control plan comme celle d'un autre document : les deux signent un
 * SHA-256 de 64 caractères et seraient autrement interchangeables.
 */
class ControlPlanSealAdapterTest {

    static final UUID TENANT = UUID.randomUUID();
    /**
     * Empreinte de banc. Le motif se répète à dessein : une suite hexadécimale
     * de 64 caractères d'apparence aléatoire fait sonner les détecteurs de
     * secrets, et une alerte qui se révèle fausse apprend à ignorer les alertes.
     */
    static final String SHA256 = "0f5a".repeat(16);

    HybridSignatureService signer;
    BlockchainAnchorPort blockchain;
    ControlPlanSealAdapter adapter;

    @BeforeEach
    void setUp() {
        signer = mock(HybridSignatureService.class);
        blockchain = mock(BlockchainAnchorPort.class);
        // Enveloppe RÉELLE et non simulée : `SignatureEnvelope` est un record,
        // donc final. En simuler une exigerait le moteur « inline » de Mockito —
        // une dépendance de banc pour contourner une classe qu'il est plus simple
        // de construire vraiment.
        when(signer.sign(any(), any())).thenReturn(envelope());
        when(blockchain.submitRoot(any(), any())).thenReturn("tx-0001");
        adapter = new ControlPlanSealAdapter(signer, blockchain);
    }

    @Test
    void itSignsTheFingerprintAndAnchorsIt() {
        ControlPlanSealPort.Seal seal = adapter.seal(TENANT, SHA256);

        assertThat(seal.signature()).isEqualTo(envelope().encode());
        assertThat(seal.anchorTxRef()).isEqualTo("tx-0001");
        verify(blockchain).submitRoot(TENANT, SHA256);
    }

    private static SignatureEnvelope envelope() {
        return new SignatureEnvelope(SignatureEnvelope.CURRENT_VERSION, "hybride-test",
                "cle-test", Instant.parse("2026-08-19T08:00:00Z"),
                List.of(new SignatureEnvelope.Part(SignatureAlgorithm.ED25519,
                        new byte[] {1, 2, 3}, new byte[] {4, 5, 6})));
    }

    @Test
    void theSignatureCarriesItsOwnContextSoItCannotBePassedOffAsAnother() {
        adapter.seal(TENANT, SHA256);

        ArgumentCaptor<String> context = ArgumentCaptor.forClass(String.class);
        verify(signer).sign(context.capture(), any());
        assertThat(context.getValue()).isEqualTo("control-plan");
    }

    @Test
    void whatIsSignedIsTheFingerprintItself() {
        adapter.seal(TENANT, SHA256);

        ArgumentCaptor<byte[]> message = ArgumentCaptor.forClass(byte[].class);
        verify(signer).sign(any(), message.capture());
        assertThat(new String(message.getValue(), StandardCharsets.UTF_8)).isEqualTo(SHA256);
    }

    @Test
    void nothingIsAnchoredWhenTheSignatureFails() {
        when(signer.sign(any(), any())).thenThrow(new IllegalStateException("clé indisponible"));

        assertThatThrownBy(() -> adapter.seal(TENANT, SHA256))
                .isInstanceOf(IllegalStateException.class);

        verify(blockchain, never()).submitRoot(any(), any());
    }

    @Test
    void theSignatureComesBeforeTheAnchoring() {
        adapter.seal(TENANT, SHA256);

        InOrder order = inOrder(signer, blockchain);
        order.verify(signer).sign(eq("control-plan"), any());
        order.verify(blockchain).submitRoot(TENANT, SHA256);
    }

    @Test
    void theFailureOfTheChainSurfacesRatherThanBeingSwallowed() {
        when(blockchain.submitRoot(any(), any()))
                .thenThrow(new IllegalStateException("pair injoignable"));

        assertThatThrownBy(() -> adapter.seal(TENANT, SHA256))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("injoignable");
    }
}
