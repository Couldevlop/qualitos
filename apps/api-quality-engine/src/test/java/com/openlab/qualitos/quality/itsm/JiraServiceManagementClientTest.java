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
class JiraServiceManagementClientTest {

    @Test
    void fetch_parsesIssuesArray() throws Exception {
        String body = """
                {
                  "issues": [
                    {
                      "key": "SUP-12",
                      "fields": {
                        "summary": "Reset password",
                        "description": "long desc",
                        "status": { "name": "In Progress" },
                        "priority": { "name": "High" },
                        "created": "2026-05-01T10:00:00.000+0000",
                        "updated": "2026-05-12T11:30:00.000+0000"
                      }
                    },
                    { "key": "SUP-13", "fields": { "summary": "Other" } }
                  ]
                }
                """;
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(200);
        when(resp.body()).thenReturn(body);
        doReturn(resp).when(http).send(any(HttpRequest.class), any());

        JiraServiceManagementClient c = new JiraServiceManagementClient(http, new ObjectMapper());
        List<ExternalIncident> out = c.fetchIncidents(conn("https://x.atlassian.net", "SUP"), "tok",
                Instant.parse("2026-04-01T00:00:00Z"));

        assertThat(out).hasSize(2);
        assertThat(out.get(0).externalId()).isEqualTo("SUP-12");
        assertThat(out.get(0).title()).isEqualTo("Reset password");
        assertThat(out.get(0).status()).isEqualTo("In Progress");
        assertThat(out.get(0).priority()).isEqualTo("High");
        assertThat(out.get(0).url()).endsWith("/browse/SUP-12");
        assertThat(out.get(0).createdAt()).isNotNull();
        assertThat(out.get(0).updatedAt()).isNotNull();
        assertThat(out.get(1).externalId()).isEqualTo("SUP-13");
        assertThat(out.get(1).priority()).isNull();
    }

    @Test
    void fetch_noScope_useFallbackJql() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(200);
        when(resp.body()).thenReturn("{\"issues\":[]}");
        doReturn(resp).when(http).send(any(HttpRequest.class), any());

        JiraServiceManagementClient c = new JiraServiceManagementClient(http, new ObjectMapper());
        c.fetchIncidents(conn("https://x.atlassian.net/", null), "t", null);

        ArgumentCaptor<HttpRequest> cap = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(cap.capture(), any());
        assertThat(cap.getValue().uri().toString())
                .contains("/rest/api/3/search")
                .contains("jql=")
                .doesNotEndWith("//rest/api/3/search"); // trailing slash handled
    }

    @Test
    void fetch_http401_throwsSyncException() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(401);
        when(resp.body()).thenReturn("");
        doReturn(resp).when(http).send(any(HttpRequest.class), any());
        JiraServiceManagementClient c = new JiraServiceManagementClient(http, new ObjectMapper());
        assertThatThrownBy(() -> c.fetchIncidents(conn("https://x.atlassian.net", "P"), "t", null))
                .isInstanceOf(ItsmSyncException.class)
                .hasMessageContaining("401");
    }

    @Test
    void fetch_ioException_wrapped() throws Exception {
        HttpClient http = mock(HttpClient.class);
        doThrow(new java.io.IOException("network")).when(http).send(any(HttpRequest.class), any());
        JiraServiceManagementClient c = new JiraServiceManagementClient(http, new ObjectMapper());
        assertThatThrownBy(() -> c.fetchIncidents(conn("https://x.atlassian.net", "P"), "t", null))
                .isInstanceOf(ItsmSyncException.class);
    }

    @Test
    void fetch_noIssuesField_returnsEmpty() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(200);
        when(resp.body()).thenReturn("{}");
        doReturn(resp).when(http).send(any(HttpRequest.class), any());
        assertThat(new JiraServiceManagementClient(http, new ObjectMapper())
                .fetchIncidents(conn("https://x.atlassian.net", "P"), "t", null)).isEmpty();
    }

    @Test
    void provider_isJiraSm() {
        assertThat(new JiraServiceManagementClient().provider()).isEqualTo(ItsmProvider.JIRA_SM);
    }

    private ItsmConnection conn(String url, String scope) {
        ItsmConnection c = new ItsmConnection();
        c.setId(UUID.randomUUID());
        c.setTenantId(UUID.randomUUID());
        c.setProvider(ItsmProvider.JIRA_SM);
        c.setBaseUrl(url);
        c.setExternalScope(scope);
        c.setUsername("user@example.com");
        c.setName("jira");
        return c;
    }
}
