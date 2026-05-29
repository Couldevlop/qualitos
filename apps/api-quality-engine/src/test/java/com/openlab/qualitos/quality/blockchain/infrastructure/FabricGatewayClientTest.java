package com.openlab.qualitos.quality.blockchain.infrastructure;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teste le vrai {@link FabricGatewayClient} contre un serveur HTTP local (JDK) :
 * succès, txId absent, corps vide, erreur HTTP → {@link FabricAnchorException}.
 */
class FabricGatewayClientTest {

    private HttpServer server;
    private int port;
    private static final UUID TENANT = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private FabricGatewayClient clientReturning(int status, String body) {
        server.createContext("/", exchange -> {
            byte[] b = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, b.length == 0 ? -1 : b.length);
            if (b.length > 0) {
                exchange.getResponseBody().write(b);
            }
            exchange.close();
        });
        return new FabricGatewayClient("http://localhost:" + port, 2000, 5000);
    }

    @Test
    void anchor_success_returnsTxId() {
        FabricGatewayClient c = clientReturning(200, "{\"txId\":\"fabric-tx-42\",\"record\":\"{}\"}");
        assertThat(c.anchor(TENANT, "abcd1234")).isEqualTo("fabric-tx-42");
    }

    @Test
    void anchor_missingTxId_throws() {
        FabricGatewayClient c = clientReturning(200, "{\"record\":\"{}\"}");
        assertThatThrownBy(() -> c.anchor(TENANT, "abcd1234"))
                .isInstanceOf(FabricAnchorException.class);
    }

    @Test
    void anchor_emptyBody_throws() {
        FabricGatewayClient c = clientReturning(200, "");
        assertThatThrownBy(() -> c.anchor(TENANT, "abcd1234"))
                .isInstanceOf(FabricAnchorException.class);
    }

    @Test
    void anchor_serverError_throws() {
        FabricGatewayClient c = clientReturning(502, "bad gateway");
        assertThatThrownBy(() -> c.anchor(TENANT, "abcd1234"))
                .isInstanceOf(FabricAnchorException.class);
    }
}
