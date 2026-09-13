package com.openlab.qualitos.quality.nonconformity.eightd.infrastructure;

import com.openlab.qualitos.crypto.application.HybridSignatureService;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;
import com.openlab.qualitos.crypto.domain.model.SignatureEnvelope;
import com.openlab.qualitos.quality.blockchain.domain.BlockchainAnchorPort;
import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDSealPort;
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
 * Le scellement d'un rapport 8D : signature hybride, puis ancrage.
 *
 * <p>Mêmes deux points que pour le control plan. L'ordre : signer avant d'ancrer,
 * sinon la chaîne porterait la trace d'un document dont personne ne peut prouver
 * l'origine. Et le contexte de signature, qui empêche de présenter la signature
 * d'un 8D comme celle d'un control plan — les deux signent un SHA-256 de
 * 64 caractères et seraient autrement interchangeables.
 */
class EightDSealAdapterTest {

    private static final UUID TENANT = UUID.randomUUID();

    /**
     * Empreinte de banc. Le motif se répète à dessein : une suite hexadécimale
     * d'apparence aléatoire fait sonner les détecteurs de secrets, et une alerte qui
     * se révèle fausse apprend à ignorer les alertes.
     */
    private static final String SHA256 = "0f5a".repeat(16);

    private HybridSignatureService signer;
    private BlockchainAnchorPort blockchain;
    private EightDSealAdapter adapter;

    @BeforeEach
    void setUp() {
        signer = mock(HybridSignatureService.class);
        blockchain = mock(BlockchainAnchorPort.class);
        when(signer.sign(any(), any())).thenReturn(enveloppe());
        when(blockchain.submitRoot(any(), any())).thenReturn("tx-8d-1");
        adapter = new EightDSealAdapter(signer, blockchain);
    }

    @Test
    void l_empreinte_est_signee_puis_ancree() {
        EightDSealPort.Seal seal = adapter.seal(TENANT, SHA256);

        assertThat(seal.signature()).isEqualTo(enveloppe().encode());
        assertThat(seal.anchorTxRef()).isEqualTo("tx-8d-1");
        verify(blockchain).submitRoot(TENANT, SHA256);
    }

    @Test
    void la_signature_porte_son_propre_contexte() {
        adapter.seal(TENANT, SHA256);

        ArgumentCaptor<String> contexte = ArgumentCaptor.forClass(String.class);
        verify(signer).sign(contexte.capture(), any());
        assertThat(contexte.getValue()).isEqualTo("eightd-report");
    }

    @Test
    void ce_qui_est_signe_est_l_empreinte_elle_meme() {
        adapter.seal(TENANT, SHA256);

        ArgumentCaptor<byte[]> message = ArgumentCaptor.forClass(byte[].class);
        verify(signer).sign(any(), message.capture());
        assertThat(new String(message.getValue(), StandardCharsets.UTF_8)).isEqualTo(SHA256);
    }

    @Test
    void la_signature_precede_l_ancrage() {
        adapter.seal(TENANT, SHA256);

        InOrder ordre = inOrder(signer, blockchain);
        ordre.verify(signer).sign(eq("eightd-report"), any());
        ordre.verify(blockchain).submitRoot(TENANT, SHA256);
    }

    @Test
    void rien_n_est_ancre_quand_la_signature_echoue() {
        when(signer.sign(any(), any())).thenThrow(new IllegalStateException("clé indisponible"));

        assertThatThrownBy(() -> adapter.seal(TENANT, SHA256))
                .isInstanceOf(IllegalStateException.class);

        verify(blockchain, never()).submitRoot(any(), any());
    }

    @Test
    void l_echec_de_la_chaine_remonte_au_lieu_d_etre_avale() {
        when(blockchain.submitRoot(any(), any()))
                .thenThrow(new IllegalStateException("pair injoignable"));

        assertThatThrownBy(() -> adapter.seal(TENANT, SHA256))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("injoignable");
    }

    @Test
    void une_enveloppe_illisible_rend_invalide_et_non_une_exception() {
        // La route publique de vérification ne doit pas distinguer « altéré » de
        // « inconnu » : les deux répondent simplement « non valide ».
        assertThat(adapter.verify("ceci-n-est-pas-une-enveloppe", SHA256)).isFalse();
        assertThat(adapter.verify(null, SHA256)).isFalse();
        assertThat(adapter.verify(enveloppe().encode(), null)).isFalse();
    }

    @Test
    void une_enveloppe_valide_est_revalidee_sur_l_empreinte_stockee() {
        when(signer.verify(any(), any())).thenReturn(true);

        assertThat(adapter.verify(enveloppe().encode(), SHA256)).isTrue();

        ArgumentCaptor<byte[]> message = ArgumentCaptor.forClass(byte[].class);
        verify(signer).verify(message.capture(), any());
        assertThat(new String(message.getValue(), StandardCharsets.UTF_8)).isEqualTo(SHA256);
    }

    private static SignatureEnvelope enveloppe() {
        return new SignatureEnvelope(SignatureEnvelope.CURRENT_VERSION, "hybride-test",
                "cle-test", Instant.parse("2026-09-13T10:00:00Z"),
                List.of(new SignatureEnvelope.Part(SignatureAlgorithm.ED25519,
                        new byte[] {1, 2, 3}, new byte[] {4, 5, 6})));
    }
}
