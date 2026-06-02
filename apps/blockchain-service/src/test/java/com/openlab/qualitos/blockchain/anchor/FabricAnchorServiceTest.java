package com.openlab.qualitos.blockchain.anchor;

import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.EndorseException;
import org.hyperledger.fabric.client.GatewayException;
import org.hyperledger.fabric.client.Proposal;
import org.hyperledger.fabric.client.Status;
import org.hyperledger.fabric.client.SubmitException;
import org.hyperledger.fabric.client.SubmittedTransaction;
import org.hyperledger.fabric.client.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de {@link FabricAnchorService} : le {@link Contract} Fabric et toute la
 * chaîne propose → endorse → submitAsync → status sont entièrement mockés (aucune I/O réseau/MSP).
 */
@ExtendWith(MockitoExtension.class)
class FabricAnchorServiceTest {

    private static final String TENANT = "tenant-42";
    private static final String ROOT = "abc123deadbeef";
    private static final String TS = "2026-05-29T10:00:00Z";
    private static final int EVENT_COUNT = 7;

    @Mock
    Contract contract;
    @Mock
    Proposal.Builder proposalBuilder;
    @Mock
    Proposal proposal;
    @Mock
    Transaction transaction;
    @Mock
    SubmittedTransaction submitted;
    @Mock
    Status status;

    FabricAnchorService service;

    @BeforeEach
    void setUp() {
        service = new FabricAnchorService(contract);
    }

    /** Configure la chaîne newProposal → addArguments → build → endorse → submitAsync. */
    private void stubAnchorChain() throws Exception {
        when(contract.newProposal("AnchorAudit")).thenReturn(proposalBuilder);
        when(proposalBuilder.addArguments(
                TENANT, ROOT, TS, Integer.toString(EVENT_COUNT))).thenReturn(proposalBuilder);
        when(proposalBuilder.build()).thenReturn(proposal);
        when(proposal.endorse()).thenReturn(transaction);
        when(transaction.submitAsync()).thenReturn(submitted);
    }

    @Test
    void anchor_success_returnsTxIdAndRecord() throws Exception {
        stubAnchorChain();
        when(submitted.getTransactionId()).thenReturn("tx-0001");
        when(submitted.getResult()).thenReturn("{\"root\":\"abc123deadbeef\"}".getBytes(StandardCharsets.UTF_8));
        when(submitted.getStatus()).thenReturn(status);
        when(status.isSuccessful()).thenReturn(true);

        FabricAnchorService.AnchorResult result = service.anchor(TENANT, ROOT, TS, EVENT_COUNT);

        assertThat(result.txId()).isEqualTo("tx-0001");
        assertThat(result.recordJson()).isEqualTo("{\"root\":\"abc123deadbeef\"}");
        // l'argument eventCount est bien sérialisé en chaîne
        verify(proposalBuilder).addArguments(TENANT, ROOT, TS, "7");
        verify(submitted).getStatus();
    }

    @Test
    void anchor_commitNotSuccessful_throwsFabricInvocationException() throws Exception {
        stubAnchorChain();
        lenient().when(submitted.getTransactionId()).thenReturn("tx-0002");
        lenient().when(submitted.getResult()).thenReturn(new byte[0]);
        when(submitted.getStatus()).thenReturn(status);
        when(status.isSuccessful()).thenReturn(false);
        // getCode() n'est pas stubbé (retourne null) : on évite de charger l'enum protobuf
        // TxValidationCode, qui dépend d'une version de runtime protobuf absente du classpath
        // de test. Le service se contente de concaténer la valeur dans le message d'erreur.

        assertThatThrownBy(() -> service.anchor(TENANT, ROOT, TS, EVENT_COUNT))
                .isInstanceOf(FabricInvocationException.class)
                .hasMessageContaining("commit rejeté")
                .hasMessageContaining("tx-0002")
                .hasNoCause();
    }

    @Test
    void anchor_endorseGatewayException_isWrapped() throws Exception {
        when(contract.newProposal("AnchorAudit")).thenReturn(proposalBuilder);
        when(proposalBuilder.addArguments(
                TENANT, ROOT, TS, Integer.toString(EVENT_COUNT))).thenReturn(proposalBuilder);
        when(proposalBuilder.build()).thenReturn(proposal);

        EndorseException endorseEx = mock(EndorseException.class);
        when(endorseEx.getMessage()).thenReturn("peer endorsement refusé");
        when(proposal.endorse()).thenThrow(endorseEx);

        assertThatThrownBy(() -> service.anchor(TENANT, ROOT, TS, EVENT_COUNT))
                .isInstanceOf(FabricInvocationException.class)
                .hasMessageContaining("ancrage Fabric échoué")
                .hasMessageContaining("peer endorsement refusé")
                .cause().isSameAs(endorseEx);
    }

    @Test
    void anchor_submitGatewayException_isWrapped() throws Exception {
        when(contract.newProposal("AnchorAudit")).thenReturn(proposalBuilder);
        when(proposalBuilder.addArguments(
                TENANT, ROOT, TS, Integer.toString(EVENT_COUNT))).thenReturn(proposalBuilder);
        when(proposalBuilder.build()).thenReturn(proposal);
        when(proposal.endorse()).thenReturn(transaction);

        SubmitException submitEx = mock(SubmitException.class);
        when(submitEx.getMessage()).thenReturn("ordering service indisponible");
        when(transaction.submitAsync()).thenThrow(submitEx);

        assertThatThrownBy(() -> service.anchor(TENANT, ROOT, TS, EVENT_COUNT))
                .isInstanceOf(FabricInvocationException.class)
                .hasMessageContaining("ancrage Fabric échoué")
                .hasMessageContaining("ordering service indisponible")
                .cause().isSameAs(submitEx);
    }

    @Test
    void verify_success_returnsRecord() throws Exception {
        when(contract.evaluateTransaction("VerifyEvidence", TENANT, ROOT))
                .thenReturn("{\"found\":true}".getBytes(StandardCharsets.UTF_8));

        String record = service.verify(TENANT, ROOT);

        assertThat(record).isEqualTo("{\"found\":true}");
        verify(contract).evaluateTransaction("VerifyEvidence", TENANT, ROOT);
    }

    @Test
    void verify_gatewayException_isWrapped() throws Exception {
        GatewayException gwEx = mock(GatewayException.class);
        when(gwEx.getMessage()).thenReturn("ledger inaccessible");
        when(contract.evaluateTransaction(eq("VerifyEvidence"), any(String[].class))).thenThrow(gwEx);

        assertThatThrownBy(() -> service.verify(TENANT, ROOT))
                .isInstanceOf(FabricInvocationException.class)
                .hasMessageContaining("vérification Fabric échouée")
                .hasMessageContaining("ledger inaccessible")
                .cause().isSameAs(gwEx);
    }

    @Test
    void anchorResult_record_exposesComponents() {
        FabricAnchorService.AnchorResult r = new FabricAnchorService.AnchorResult("tx-9", "{}");
        assertThat(r.txId()).isEqualTo("tx-9");
        assertThat(r.recordJson()).isEqualTo("{}");
    }
}
