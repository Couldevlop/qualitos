package com.openlab.qualitos.quality.webhooks;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
class WebhookDispatcherTest {

    @Test
    void dispatch_success_2xx() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(202);
        when(resp.body()).thenReturn("accepted");
        doReturn(resp).when(client).send(any(HttpRequest.class), any());

        WebhookDispatcher d = new WebhookDispatcher(new HmacSigner(), client);
        WebhookDispatcher.DispatchResult r = d.dispatch(
                sub("https://example.com/hook", "supersecret-1234"),
                EventType.TEST_PING, UUID.randomUUID().toString(), "{}");

        assertThat(r.success()).isTrue();
        assertThat(r.statusCode()).isEqualTo(202);
        assertThat(r.responseBody()).isEqualTo("accepted");
        assertThat(r.errorMessage()).isNull();
    }

    @Test
    void dispatch_4xxOr5xx_marksFailure() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(503);
        when(resp.body()).thenReturn("server unavailable");
        doReturn(resp).when(client).send(any(HttpRequest.class), any());

        WebhookDispatcher d = new WebhookDispatcher(new HmacSigner(), client);
        WebhookDispatcher.DispatchResult r = d.dispatch(
                sub("https://example.com/hook", "supersecret-1234"),
                EventType.TEST_PING, "evt-1", "{}");

        assertThat(r.success()).isFalse();
        assertThat(r.statusCode()).isEqualTo(503);
    }

    @Test
    void dispatch_ioException_capturesError() throws Exception {
        HttpClient client = mock(HttpClient.class);
        when(client.send(any(HttpRequest.class), any()))
                .thenThrow(new java.io.IOException("connection refused"));

        WebhookDispatcher d = new WebhookDispatcher(new HmacSigner(), client);
        WebhookDispatcher.DispatchResult r = d.dispatch(
                sub("https://example.com/hook", "supersecret-1234"),
                EventType.TEST_PING, "evt-2", "{}");

        assertThat(r.success()).isFalse();
        assertThat(r.errorMessage()).contains("IOException").contains("connection refused");
    }

    @Test
    void dispatch_attachesHmacAndMetaHeaders() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(200);
        when(resp.body()).thenReturn("");
        doReturn(resp).when(client).send(any(HttpRequest.class), any());

        WebhookDispatcher d = new WebhookDispatcher(new HmacSigner(), client);
        WebhookSubscription s = sub("https://example.com/hook", "supersecret-1234");
        d.dispatch(s, EventType.PDCA_CYCLE_ADVANCED, "ev-99", "{\"x\":1}");

        ArgumentCaptor<HttpRequest> cap = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(cap.capture(), any());
        HttpRequest req = cap.getValue();

        assertThat(req.uri().toString()).isEqualTo("https://example.com/hook");
        assertThat(req.method()).isEqualTo("POST");
        assertThat(req.headers().firstValue("Content-Type")).hasValue("application/json");
        assertThat(req.headers().firstValue("X-QualitOS-Event")).hasValue("pdca.cycle.advanced");
        assertThat(req.headers().firstValue("X-QualitOS-Event-Id")).hasValue("ev-99");
        assertThat(req.headers().firstValue("X-QualitOS-Timestamp")).isPresent();
        assertThat(req.headers().firstValue("X-QualitOS-Signature")).hasValueSatisfying(
                v -> assertThat(v).startsWith("sha256="));
    }

    @Test
    void truncate_helper() {
        assertThat(WebhookDispatcher.truncate(null, 10)).isNull();
        assertThat(WebhookDispatcher.truncate("short", 10)).isEqualTo("short");
        assertThat(WebhookDispatcher.truncate("12345678901", 5)).startsWith("12345").contains("truncated");
    }

    private WebhookSubscription sub(String url, String secret) {
        WebhookSubscription s = new WebhookSubscription();
        s.setId(UUID.randomUUID());
        s.setEndpointUrl(url);
        s.setSecret(secret);
        return s;
    }
}
