package com.openlab.qualitos.quality.ehrconnector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.itsm.ConnectionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Teste le {@link FhirClient} avec {@link MockRestServiceServer} servant un Bundle FHIR.
 */
class FhirClientTest {

    private static final String BASE = "https://fhir.example.org/r5";

    /** Bundle avec une Observation ANORMALE (interpretation=H) et une NORMALE (N). */
    private static final String OBSERVATION_BUNDLE = """
            {
              "resourceType": "Bundle",
              "type": "searchset",
              "entry": [
                {
                  "resource": {
                    "resourceType": "Observation",
                    "id": "obs-abnormal-1",
                    "status": "final",
                    "code": { "coding": [ { "code": "8867-4", "display": "Heart rate" } ] },
                    "interpretation": [ { "coding": [ { "code": "H" } ] } ],
                    "effectiveDateTime": "2026-05-10T08:00:00Z",
                    "subject": { "reference": "Patient/SECRET-123", "display": "John Doe" },
                    "valueQuantity": { "value": 180, "unit": "bpm" }
                  }
                },
                {
                  "resource": {
                    "resourceType": "Observation",
                    "id": "obs-normal-1",
                    "status": "final",
                    "code": { "coding": [ { "code": "8480-6", "display": "Systolic BP" } ] },
                    "interpretation": [ { "coding": [ { "code": "N" } ] } ]
                  }
                }
              ]
            }
            """;

    private static final String EMPTY_BUNDLE = """
            { "resourceType": "Bundle", "type": "searchset" }
            """;

    private EhrConnection conn(EhrAuthMode mode, String category) {
        EhrConnection c = new EhrConnection();
        c.setId(UUID.randomUUID());
        c.setTenantId(UUID.randomUUID());
        c.setName("fhir");
        c.setProvider(EhrProvider.FHIR_R5);
        c.setFhirBaseUrl(BASE);
        c.setAuthMode(mode);
        c.setUsername("svc");
        c.setResourceCategory(category);
        c.setStatus(ConnectionStatus.ACTIVE);
        return c;
    }

    @Test
    void fetch_parsesBundle_keepsOnlyAbnormal_andNoPii() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        // Observation : filtre serveur interpretation=A (catégorie absente).
        server.expect(requestTo(BASE + "/Observation?_count=200&interpretation=A"))
                .andExpect(header("Authorization", org.hamcrest.Matchers.startsWith("Bearer ")))
                .andRespond(withSuccess(OBSERVATION_BUNDLE, MediaType.valueOf("application/fhir+json")));
        // DiagnosticReport : pas de filtre interpretation, bundle vide.
        server.expect(requestTo(BASE + "/DiagnosticReport?_count=200"))
                .andRespond(withSuccess(EMPTY_BUNDLE, MediaType.APPLICATION_JSON));

        FhirClient client = new FhirClient(builder, new ObjectMapper(), 2000, 5000, false);
        List<FhirResource> out = client.fetchAdverseResources(conn(EhrAuthMode.BEARER, null), "token-xyz", null);

        server.verify();
        assertThat(out).hasSize(1);
        FhirResource r = out.get(0);
        assertThat(r.resourceType()).isEqualTo("Observation");
        assertThat(r.id()).isEqualTo("obs-abnormal-1");
        assertThat(r.code()).isEqualTo("8867-4");
        assertThat(r.codeDisplay()).isEqualTo("Heart rate");
        assertThat(r.interpretation()).isEqualTo("H");
        assertThat(r.status()).isEqualTo("final");
        assertThat(r.effective()).isEqualTo(Instant.parse("2026-05-10T08:00:00Z"));
        assertThat(r.reference()).isEqualTo("Observation/obs-abnormal-1");
        // PRIVACY : aucune donnée patient ne doit transiter dans le modèle extrait.
        assertThat(out.toString())
                .doesNotContain("SECRET-123")
                .doesNotContain("John Doe")
                .doesNotContain("180");
    }

    @Test
    void fetch_withCategory_usesCategoryFilter_andBasicAuth() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE + "/Observation?_count=200&category=patient-safety"))
                .andExpect(header("Authorization", org.hamcrest.Matchers.startsWith("Basic ")))
                .andRespond(withSuccess(EMPTY_BUNDLE, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/DiagnosticReport?_count=200&category=patient-safety"))
                .andRespond(withSuccess(EMPTY_BUNDLE, MediaType.APPLICATION_JSON));

        FhirClient client = new FhirClient(builder, new ObjectMapper(), 2000, 5000, false);
        List<FhirResource> out = client.fetchAdverseResources(
                conn(EhrAuthMode.BASIC, "patient-safety"), "pw", null);

        server.verify();
        assertThat(out).isEmpty();
    }

    @Test
    void fetch_withSince_addsLastUpdatedFilter() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        Instant since = Instant.parse("2026-05-01T00:00:00Z");
        server.expect(requestTo(org.hamcrest.Matchers.containsString("_lastUpdated=gt2026-05-01")))
                .andRespond(withSuccess(EMPTY_BUNDLE, MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("DiagnosticReport")))
                .andRespond(withSuccess(EMPTY_BUNDLE, MediaType.APPLICATION_JSON));

        FhirClient client = new FhirClient(builder, new ObjectMapper(), 2000, 5000, false);
        client.fetchAdverseResources(conn(EhrAuthMode.BEARER, null), "t", since);
        server.verify();
    }

    @Test
    void fetch_httpError_throwsSyncException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/Observation")))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        FhirClient client = new FhirClient(builder, new ObjectMapper(), 2000, 5000, false);
        assertThatThrownBy(() -> client.fetchAdverseResources(conn(EhrAuthMode.BEARER, null), "t", null))
                .isInstanceOf(EhrSyncException.class)
                .hasMessageContaining("Observation");
    }

    @Test
    void parseBundle_emptyOrBlank_returnsEmpty() {
        FhirClient client = new FhirClient(RestClient.builder(), new ObjectMapper(), 1, 1, false);
        assertThat(client.parseBundle(null, "Observation")).isEmpty();
        assertThat(client.parseBundle("", "Observation")).isEmpty();
        assertThat(client.parseBundle("{}", "Observation")).isEmpty();
    }

    @Test
    void parseBundle_malformedJson_throws() {
        FhirClient client = new FhirClient(RestClient.builder(), new ObjectMapper(), 1, 1, false);
        assertThatThrownBy(() -> client.parseBundle("{not-json", "Observation"))
                .isInstanceOf(EhrSyncException.class);
    }

    @Test
    void parseBundle_dedupesAndSkipsMissingId() {
        String bundle = """
                {
                  "entry": [
                    { "resource": { "resourceType": "Observation", "id": "x",
                        "interpretation": [ { "coding": [ { "code": "AA" } ] } ] } },
                    { "resource": { "resourceType": "Observation", "id": "x",
                        "interpretation": [ { "coding": [ { "code": "AA" } ] } ] } },
                    { "resource": { "resourceType": "Observation",
                        "interpretation": [ { "coding": [ { "code": "AA" } ] } ] } }
                  ]
                }
                """;
        FhirClient client = new FhirClient(RestClient.builder(), new ObjectMapper(), 1, 1, false);
        List<FhirResource> out = client.parseBundle(bundle, "Observation");
        assertThat(out).hasSize(1);
        assertThat(out.get(0).interpretation()).isEqualTo("AA");
    }
}
