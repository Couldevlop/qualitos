package com.openlab.qualitos.quality.blockchain.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.UUID;

/**
 * Client serveur-à-serveur vers {@code blockchain-service} (ADR 0012 Phase B), qui
 * porte le <b>Fabric Gateway SDK</b> et invoque le chaincode {@code qualitos-anchor}
 * ({@code AnchorAudit} / {@code VerifyEvidence}). On isole ainsi les dépendances
 * Hyperledger Fabric (gRPC, protobuf, MSP) hors de l'engine : ici, simple appel HTTP.
 *
 * <p>RGPD (§11.3) : seul le <b>Merkle root</b> (un hash) transite — jamais de donnée
 * personnelle. En prod : mTLS + OIDC client_credentials ; en dev : appel interne direct.
 */
@Component
@Profile("fabric")
public class FabricGatewayClient {

    private final RestClient client;

    public FabricGatewayClient(
            @Value("${qualitos.blockchain.service-url:http://localhost:8090}") String baseUrl) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(5_000);
        rf.setReadTimeout(30_000);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(rf).build();
    }

    /**
     * Soumet le Merkle root au chaincode via {@code blockchain-service}.
     *
     * @return l'identifiant de transaction Fabric (txId).
     * @throws FabricAnchorException si le service / réseau Fabric est indisponible.
     */
    @SuppressWarnings("unchecked")
    public String anchor(UUID tenantId, String merkleRootHex) {
        Map<String, Object> body = Map.of(
                "tenantId", tenantId.toString(),
                "merkleRoot", merkleRootHex);
        try {
            Map<String, Object> resp = client.post()
                    .uri("/internal/v1/anchor")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (resp == null || resp.get("txId") == null) {
                throw new FabricAnchorException("Réponse invalide de blockchain-service (txId absent)");
            }
            return String.valueOf(resp.get("txId"));
        } catch (RestClientException e) {
            throw new FabricAnchorException("blockchain-service indisponible : " + e.getMessage(), e);
        }
    }
}
