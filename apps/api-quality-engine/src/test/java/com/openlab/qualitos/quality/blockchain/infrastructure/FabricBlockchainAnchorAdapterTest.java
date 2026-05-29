package com.openlab.qualitos.quality.blockchain.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FabricBlockchainAnchorAdapterTest {

    @Mock FabricGatewayClient fabric;
    @Mock SignedAnchorAdapter signedFallback;
    @InjectMocks FabricBlockchainAnchorAdapter adapter;

    static final UUID TENANT = UUID.randomUUID();
    static final String ROOT = "a1b2c3";

    @Test
    void anchorsViaFabric_whenAvailable() {
        when(fabric.anchor(TENANT, ROOT)).thenReturn("txABC123");

        String ref = adapter.submitRoot(TENANT, ROOT);

        assertThat(ref).isEqualTo("fabric:txABC123");
        verifyNoInteractions(signedFallback);   // Fabric OK → pas de repli
    }

    @Test
    void fallsBackToSignedReceipt_whenFabricUnavailable() {
        when(fabric.anchor(TENANT, ROOT)).thenThrow(new FabricAnchorException("réseau indisponible"));
        when(signedFallback.submitRoot(TENANT, ROOT)).thenReturn("receipt-uuid-42");

        String ref = adapter.submitRoot(TENANT, ROOT);

        assertThat(ref).isEqualTo("receipt-uuid-42");  // ancrage jamais perdu
        verify(signedFallback).submitRoot(TENANT, ROOT);
    }
}
