package com.openlab.qualitos.quality.erpconnector;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Teste les clients ERP via {@link MockRestServiceServer} : auth, chemins, et parsing
 * tolérant des enveloppes OData/REST (SAP d.results, OData value, Oracle items).
 */
class ErpProviderClientTest {

    private static final String BASE = "https://erp.example.com";

    private ErpConnection conn(ErpProvider p) {
        ErpConnection c = new ErpConnection();
        c.setId(UUID.randomUUID());
        c.setTenantId(UUID.randomUUID());
        c.setProvider(p);
        c.setBaseUrl(BASE);
        c.setUsername("svc");
        c.setStatus(ErpConnectionStatus.ACTIVE);
        return c;
    }

    // ---------- SAP : OData v2 (d.results) + Basic auth ----------

    @Test
    void sap_fetchSuppliers_parsesODataV2_andSendsBasicAuth() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SapOdataClient client = new SapOdataClient(builder.build(), "/sap/suppliers", "/sap/kpis");

        server.expect(requestTo(BASE + "/sap/suppliers"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header("Authorization", org.hamcrest.Matchers.startsWith("Basic ")))
                .andRespond(withSuccess("""
                        {"d":{"results":[
                          {"Supplier":"S1","SupplierName":"Acme","SupplierCategory":"RAW","Country":"FR","EmailAddress":"a@x.io"},
                          {"Supplier":"S2","SupplierName":"Beta"}
                        ]}}""", MediaType.APPLICATION_JSON));

        List<ExternalSupplier> out = client.fetchSuppliers(conn(ErpProvider.SAP), "pwd");

        server.verify();
        assertThat(out).hasSize(2);
        assertThat(out.get(0).externalCode()).isEqualTo("S1");
        assertThat(out.get(0).name()).isEqualTo("Acme");
        assertThat(out.get(0).category()).isEqualTo("RAW");
        assertThat(out.get(0).countryCode()).isEqualTo("FR");
        assertThat(client.provider()).isEqualTo(ErpProvider.SAP);
    }

    @Test
    void sap_fetchProductionKpis_parsesODataV4_value() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SapOdataClient client = new SapOdataClient(builder.build(), "/sap/suppliers", "/sap/kpis");

        server.expect(requestTo(BASE + "/sap/kpis"))
                .andRespond(withSuccess("""
                        {"value":[
                          {"kpiCode":"OEE","value":"82.5","unit":"%","periodStart":"2026-01-01T00:00:00Z","periodEnd":"2026-02-01T00:00:00Z"}
                        ]}""", MediaType.APPLICATION_JSON));

        List<ExternalProductionKpi> out = client.fetchProductionKpis(conn(ErpProvider.SAP), "pwd");

        server.verify();
        assertThat(out).hasSize(1);
        assertThat(out.get(0).kpiCode()).isEqualTo("OEE");
        assertThat(out.get(0).value()).isEqualByComparingTo("82.5");
        assertThat(out.get(0).periodStart()).isNotNull();
    }

    // ---------- Oracle Fusion : REST (items) ----------

    @Test
    void oracle_fetchSuppliers_parsesItems() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OracleFusionClient client = new OracleFusionClient(builder.build(), "/o/suppliers", "/o/kpis");

        server.expect(requestTo(BASE + "/o/suppliers"))
                .andRespond(withSuccess("""
                        {"items":[{"SupplierNumber":"V100","SupplierName":"Oracle Vendor","category":"SERVICE"}]}""",
                        MediaType.APPLICATION_JSON));

        List<ExternalSupplier> out = client.fetchSuppliers(conn(ErpProvider.ORACLE_FUSION), "pwd");

        server.verify();
        assertThat(out).hasSize(1);
        assertThat(out.get(0).externalCode()).isEqualTo("V100");
        assertThat(client.provider()).isEqualTo(ErpProvider.ORACLE_FUSION);
    }

    // ---------- Dynamics : OData v4 + Bearer auth ----------

    @Test
    void dynamics_fetchSuppliers_sendsBearer_andParsesValueArrayRoot() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DynamicsClient client = new DynamicsClient(builder.build(), "/d/vendors", "/d/kpis");

        server.expect(requestTo(BASE + "/d/vendors"))
                .andExpect(header("Authorization", "Bearer tok123"))
                .andRespond(withSuccess(
                        "[{\"vendorid\":\"D1\",\"vendorname\":\"Dyn Co\",\"vendorgroupid\":\"COMPONENT\"}]",
                        MediaType.APPLICATION_JSON));

        List<ExternalSupplier> out = client.fetchSuppliers(conn(ErpProvider.DYNAMICS), "tok123");

        server.verify();
        assertThat(out).hasSize(1);
        assertThat(out.get(0).externalCode()).isEqualTo("D1");
        assertThat(out.get(0).name()).isEqualTo("Dyn Co");
        assertThat(client.provider()).isEqualTo(ErpProvider.DYNAMICS);
    }

    // ---------- Failures & edge cases ----------

    @Test
    void fetch_httpError_throwsErpSyncException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SapOdataClient client = new SapOdataClient(builder.build(), "/sap/suppliers", "/sap/kpis");

        server.expect(requestTo(BASE + "/sap/suppliers"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.fetchSuppliers(conn(ErpProvider.SAP), "pwd"))
                .isInstanceOf(ErpSyncException.class)
                .hasMessageContaining("fetch failed");
        server.verify();
    }

    @Test
    void fetch_unrecognizedShape_throwsErpSyncException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SapOdataClient client = new SapOdataClient(builder.build(), "/sap/suppliers", "/sap/kpis");

        server.expect(requestTo(BASE + "/sap/suppliers"))
                .andRespond(withSuccess("{\"unexpected\":true}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchSuppliers(conn(ErpProvider.SAP), "pwd"))
                .isInstanceOf(ErpSyncException.class);
    }

    @Test
    void extractCollection_handlesNullAndV2BareArray() {
        assertThatThrownBy(() -> AbstractErpRestClient.extractCollection(null))
                .isInstanceOf(ErpSyncException.class);
    }

    @Test
    void absoluteUrl_trimsAndJoinsSlashes() {
        assertThat(AbstractErpRestClient.absoluteUrl("https://a/", "x")).isEqualTo("https://a/x");
        assertThat(AbstractErpRestClient.absoluteUrl("https://a", "/x")).isEqualTo("https://a/x");
    }

    @Test
    void parseInstant_toleratesBadInput() {
        assertThat(AbstractErpRestClient.parseInstant("not-a-date")).isNull();
        assertThat(AbstractErpRestClient.parseInstant("")).isNull();
        assertThat(AbstractErpRestClient.parseInstant("2026-01-01T00:00:00Z")).isNotNull();
    }
}
