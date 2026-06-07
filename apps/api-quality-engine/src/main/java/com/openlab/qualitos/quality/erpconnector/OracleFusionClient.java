package com.openlab.qualitos.quality.erpconnector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client Oracle Fusion Cloud via REST API. Auth Basic par défaut (compte d'intégration) —
 * Oracle Fusion accepte aussi OAuth2 Bearer (V2). Réponses au format Oracle REST
 * ({@code {"items":[...]}}), gérées par le parsing tolérant.
 *
 * <p>Chemins par défaut surchargeables par property :
 * <ul>
 *   <li>{@code qualitos.erp.oracle.suppliers-path} (def. {@code /fscmRestApi/resources/11.13.18.05/suppliers})</li>
 *   <li>{@code qualitos.erp.oracle.production-kpis-path} (def. {@code /fscmRestApi/resources/11.13.18.05/productionKpis})</li>
 * </ul>
 */
@Component
public class OracleFusionClient extends AbstractErpRestClient {

    private final String suppliersPath;
    private final String productionKpisPath;

    @Autowired
    public OracleFusionClient(
            @Value("${qualitos.erp.oracle.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${qualitos.erp.oracle.read-timeout-ms:30000}") int readTimeoutMs,
            @Value("${qualitos.erp.oracle.suppliers-path:/fscmRestApi/resources/11.13.18.05/suppliers}")
            String suppliersPath,
            @Value("${qualitos.erp.oracle.production-kpis-path:/fscmRestApi/resources/11.13.18.05/productionKpis}")
            String productionKpisPath) {
        super(buildClient(connectTimeoutMs, readTimeoutMs));
        this.suppliersPath = suppliersPath;
        this.productionKpisPath = productionKpisPath;
    }

    /** Constructeur test : injecte un RestClient (cf. MockRestServiceServer). */
    OracleFusionClient(RestClient client, String suppliersPath, String productionKpisPath) {
        super(client);
        this.suppliersPath = suppliersPath;
        this.productionKpisPath = productionKpisPath;
    }

    private static RestClient buildClient(int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(connectTimeoutMs);
        rf.setReadTimeout(readTimeoutMs);
        return RestClient.builder().requestFactory(rf).build();
    }

    @Override public ErpProvider provider() { return ErpProvider.ORACLE_FUSION; }
    @Override protected String suppliersPath() { return suppliersPath; }
    @Override protected String productionKpisPath() { return productionKpisPath; }
    @Override protected AuthScheme authScheme() { return AuthScheme.BASIC; }
}
