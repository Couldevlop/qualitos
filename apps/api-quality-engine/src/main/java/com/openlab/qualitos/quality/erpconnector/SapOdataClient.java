package com.openlab.qualitos.quality.erpconnector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client SAP S/4HANA via API OData (SAP Gateway). Auth Basic (compte technique).
 *
 * <p>Chemins par défaut visant des services OData standards ; surchargeables par property
 * sans toucher au code (CLAUDE.md règle 18.2 #11, industry-agnostic) :
 * <ul>
 *   <li>{@code qualitos.erp.sap.suppliers-path} (def. {@code /sap/opu/odata/sap/API_BUSINESS_PARTNER/A_Supplier})</li>
 *   <li>{@code qualitos.erp.sap.production-kpis-path} (def. {@code /sap/opu/odata/sap/API_PRODUCTION_KPI/KpiResults})</li>
 * </ul>
 * Le parsing tolère l'enveloppe OData v2 ({@code d.results}) et v4 ({@code value}).
 */
@Component
public class SapOdataClient extends AbstractErpRestClient {

    private final String suppliersPath;
    private final String productionKpisPath;

    @Autowired
    public SapOdataClient(
            @Value("${qualitos.erp.sap.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${qualitos.erp.sap.read-timeout-ms:30000}") int readTimeoutMs,
            @Value("${qualitos.erp.sap.suppliers-path:/sap/opu/odata/sap/API_BUSINESS_PARTNER/A_Supplier}")
            String suppliersPath,
            @Value("${qualitos.erp.sap.production-kpis-path:/sap/opu/odata/sap/API_PRODUCTION_KPI/KpiResults}")
            String productionKpisPath) {
        super(buildClient(connectTimeoutMs, readTimeoutMs));
        this.suppliersPath = suppliersPath;
        this.productionKpisPath = productionKpisPath;
    }

    /** Constructeur test : injecte un RestClient (cf. MockRestServiceServer). */
    SapOdataClient(RestClient client, String suppliersPath, String productionKpisPath) {
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

    @Override public ErpProvider provider() { return ErpProvider.SAP; }
    @Override protected String suppliersPath() { return suppliersPath; }
    @Override protected String productionKpisPath() { return productionKpisPath; }
    @Override protected AuthScheme authScheme() { return AuthScheme.BASIC; }
}
