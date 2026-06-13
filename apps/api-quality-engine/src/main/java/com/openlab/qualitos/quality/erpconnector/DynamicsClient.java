package com.openlab.qualitos.quality.erpconnector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client Microsoft Dynamics 365 (Finance & Operations) via OData v4. Auth Bearer
 * (jeton AAD obtenu en amont ; ici on transporte le token stocké comme secret).
 * Réponses OData v4 ({@code {"value":[...]}}), gérées par le parsing tolérant.
 *
 * <p>Chemins par défaut surchargeables par property :
 * <ul>
 *   <li>{@code qualitos.erp.dynamics.suppliers-path} (def. {@code /data/Vendors})</li>
 *   <li>{@code qualitos.erp.dynamics.production-kpis-path} (def. {@code /data/ProductionKpis})</li>
 * </ul>
 */
@Component
public class DynamicsClient extends AbstractErpRestClient {

    private final String suppliersPath;
    private final String productionKpisPath;

    @Autowired
    public DynamicsClient(
            @Value("${qualitos.erp.dynamics.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${qualitos.erp.dynamics.read-timeout-ms:30000}") int readTimeoutMs,
            @Value("${qualitos.erp.dynamics.suppliers-path:/data/Vendors}")
            String suppliersPath,
            @Value("${qualitos.erp.dynamics.production-kpis-path:/data/ProductionKpis}")
            String productionKpisPath) {
        super(buildClient(connectTimeoutMs, readTimeoutMs));
        this.suppliersPath = suppliersPath;
        this.productionKpisPath = productionKpisPath;
    }

    /** Constructeur test : injecte un RestClient (cf. MockRestServiceServer). */
    DynamicsClient(RestClient client, String suppliersPath, String productionKpisPath) {
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

    @Override public ErpProvider provider() { return ErpProvider.DYNAMICS; }
    @Override protected String suppliersPath() { return suppliersPath; }
    @Override protected String productionKpisPath() { return productionKpisPath; }
    @Override protected AuthScheme authScheme() { return AuthScheme.BEARER; }
}
