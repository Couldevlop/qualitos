package com.openlab.qualitos.quality.itsm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
class ServiceNowClientTest {

    @Test
    void fetch_parsesResultArray() throws Exception {
        String body = """
                {
                  "result": [
                    {
                      "sys_id": "abc123",
                      "short_description": "Mon incident",
                      "description": "Détail",
                      "state": "2",
                      "priority": "3",
                      "sys_created_on": "2026-05-10 12:00:00",
                      "sys_updated_on": "2026-05-12 10:30:00"
                    },
                    { "sys_id": "def456", "short_description": "Autre", "state": "1", "priority": "4" }
                  ]
                }
                """;
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(200);
        when(resp.body()).thenReturn(body);
        doReturn(resp).when(http).send(any(HttpRequest.class), any());

        ServiceNowClient c = new ServiceNowClient(http, new ObjectMapper());
        List<ExternalIncident> out = c.fetchIncidents(conn("https://x.service-now.com"), "tok",
                Instant.parse("2026-05-10T00:00:00Z"));

        assertThat(out).hasSize(2);
        assertThat(out.get(0).externalId()).isEqualTo("abc123");
        assertThat(out.get(0).title()).isEqualTo("Mon incident");
        assertThat(out.get(0).status()).isEqualTo("2");
        assertThat(out.get(0).createdAt()).isNotNull();
        assertThat(out.get(0).updatedAt()).isNotNull();
        assertThat(out.get(0).url()).contains("/nav_to.do");
        assertThat(out.get(1).externalId()).isEqualTo("def456");
        assertThat(out.get(1).createdAt()).isNull(); // pas de date dans le 2e
    }

    @Test
    void fetch_emptyResult_returnsEmpty() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(200);
        when(resp.body()).thenReturn("{\"result\":[]}");
        doReturn(resp).when(http).send(any(HttpRequest.class), any());
        ServiceNowClient c = new ServiceNowClient(http, new ObjectMapper());
        assertThat(c.fetchIncidents(conn("https://x.service-now.com"), "t", null)).isEmpty();
    }

    @Test
    void fetch_noResultField_returnsEmpty() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(200);
        when(resp.body()).thenReturn("{}");
        doReturn(resp).when(http).send(any(HttpRequest.class), any());
        assertThat(new ServiceNowClient(http, new ObjectMapper())
                .fetchIncidents(conn("https://x.service-now.com"), "t", null)).isEmpty();
    }

    @Test
    void fetch_http5xx_throwsSyncException() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(503);
        when(resp.body()).thenReturn("");
        doReturn(resp).when(http).send(any(HttpRequest.class), any());
        ServiceNowClient c = new ServiceNowClient(http, new ObjectMapper());
        assertThatThrownBy(() -> c.fetchIncidents(conn("https://x.service-now.com"), "t", null))
                .isInstanceOf(ItsmSyncException.class)
                .hasMessageContaining("503");
    }

    @Test
    void fetch_ioException_wrapped() throws Exception {
        HttpClient http = mock(HttpClient.class);
        doThrow(new java.io.IOException("connection refused"))
                .when(http).send(any(HttpRequest.class), any());
        ServiceNowClient c = new ServiceNowClient(http, new ObjectMapper());
        assertThatThrownBy(() -> c.fetchIncidents(conn("https://x.service-now.com"), "t", null))
                .isInstanceOf(ItsmSyncException.class)
                .hasMessageContaining("Service");
    }

    @Test
    void fetch_attachesBasicAuthAndAcceptHeaders() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(200);
        when(resp.body()).thenReturn("{\"result\":[]}");
        doReturn(resp).when(http).send(any(HttpRequest.class), any());

        ServiceNowClient c = new ServiceNowClient(http, new ObjectMapper());
        ItsmConnection conn = conn("https://x.service-now.com/");
        conn.setUsername("admin");
        c.fetchIncidents(conn, "secret-token", null);

        ArgumentCaptor<HttpRequest> cap = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(cap.capture(), any());
        HttpRequest req = cap.getValue();
        assertThat(req.method()).isEqualTo("GET");
        assertThat(req.headers().firstValue("Accept")).hasValue("application/json");
        assertThat(req.headers().firstValue("Authorization")).hasValueSatisfying(
                v -> assertThat(v).startsWith("Basic "));
        assertThat(req.uri().toString())
                .contains("/api/now/table/incident")
                .contains("sysparm_limit=200")
                .doesNotEndWith("//api/now/table/incident"); // trailing slash handled
    }

    @Test
    void provider_isServiceNow() {
        assertThat(new ServiceNowClient().provider()).isEqualTo(ItsmProvider.SERVICENOW);
    }

    private ItsmConnection conn(String url) {
        ItsmConnection c = new ItsmConnection();
        c.setId(UUID.randomUUID());
        c.setTenantId(UUID.randomUUID());
        c.setProvider(ItsmProvider.SERVICENOW);
        c.setBaseUrl(url);
        c.setName("sn");
        return c;
    }
}
