package com.openlab.qualitos.quality.webhooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Tag;

@Tag("web")
@WebMvcTest(controllers = WebhookController.class)
class WebhookControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean WebhookService service;
    ObjectMapper om;

    static final UUID SUB = UUID.randomUUID();
    static final UUID DELIV = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_returns200() throws Exception {
        when(service.listSubscriptions(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(subResp())));
        mockMvc.perform(get("/api/v1/webhooks/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(SUB.toString()));
    }

    @Test @WithMockUser
    void create_returns201() throws Exception {
        when(service.createSubscription(any())).thenReturn(
                new WebhookDto.CreatedSubscriptionResponse(subResp(), "supersecret-1234"));
        WebhookDto.CreateSubscriptionRequest req = new WebhookDto.CreateSubscriptionRequest(
                "n", "https://x.example.com/h", List.of(EventType.PDCA_CYCLE_CREATED),
                "supersecret-12345", 5, USER);
        mockMvc.perform(post("/api/v1/webhooks/subscriptions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.secret").value("supersecret-1234"));
    }

    @Test @WithMockUser
    void create_invalidUrl_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/subscriptions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"n\",\"endpointUrl\":\"not-a-url\",\"eventTypes\":[\"pdca.cycle.created\"],\"secret\":\"supersecret-12345\",\"createdBy\":\"" + USER + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_shortSecret_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/subscriptions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"n\",\"endpointUrl\":\"https://x\",\"eventTypes\":[\"pdca.cycle.created\"],\"secret\":\"short\",\"createdBy\":\"" + USER + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_emptyEventTypes_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/subscriptions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"n\",\"endpointUrl\":\"https://x\",\"eventTypes\":[],\"secret\":\"supersecret-12345\",\"createdBy\":\"" + USER + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void get_found() throws Exception {
        when(service.getSubscription(SUB)).thenReturn(subResp());
        mockMvc.perform(get("/api/v1/webhooks/subscriptions/{id}", SUB)).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound() throws Exception {
        when(service.getSubscription(SUB)).thenThrow(new WebhookSubscriptionNotFoundException(SUB));
        mockMvc.perform(get("/api/v1/webhooks/subscriptions/{id}", SUB)).andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void update_success() throws Exception {
        when(service.updateSubscription(eq(SUB), any())).thenReturn(subResp());
        mockMvc.perform(patch("/api/v1/webhooks/subscriptions/{id}", SUB).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAUSED\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_success() throws Exception {
        doNothing().when(service).deleteSubscription(SUB);
        mockMvc.perform(delete("/api/v1/webhooks/subscriptions/{id}", SUB).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser
    void testPing_returnsResult() throws Exception {
        when(service.testPing(SUB)).thenReturn(new WebhookDto.TestPingResponse(
                DELIV, DeliveryStatus.SUCCESS, 200, null));
        mockMvc.perform(post("/api/v1/webhooks/subscriptions/{id}/test", SUB).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test @WithMockUser
    void deliveries_returns200() throws Exception {
        when(service.listDeliveries(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(deliveryResp())));
        mockMvc.perform(get("/api/v1/webhooks/deliveries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(DELIV.toString()));
    }

    @Test @WithMockUser
    void deliveries_withFilters() throws Exception {
        when(service.listDeliveries(eq(SUB), eq(DeliveryStatus.DEAD_LETTER), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/api/v1/webhooks/deliveries")
                        .param("subscriptionId", SUB.toString())
                        .param("status", "DEAD_LETTER"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void getDelivery_notFound() throws Exception {
        when(service.getDelivery(DELIV)).thenThrow(new WebhookDeliveryNotFoundException(DELIV));
        mockMvc.perform(get("/api/v1/webhooks/deliveries/{id}", DELIV))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void retry_success() throws Exception {
        when(service.retryDelivery(DELIV)).thenReturn(deliveryResp());
        mockMvc.perform(post("/api/v1/webhooks/deliveries/{id}/retry", DELIV).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void retry_invalidState_returns409() throws Exception {
        when(service.retryDelivery(DELIV)).thenThrow(new WebhookStateException("succeeded"));
        mockMvc.perform(post("/api/v1/webhooks/deliveries/{id}/retry", DELIV).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void create_missingTenant_returns403() throws Exception {
        when(service.createSubscription(any())).thenThrow(new MissingTenantContextException());
        WebhookDto.CreateSubscriptionRequest req = new WebhookDto.CreateSubscriptionRequest(
                "n", "https://x.example.com/h", List.of(EventType.PDCA_CYCLE_CREATED),
                "supersecret-12345", 5, USER);
        mockMvc.perform(post("/api/v1/webhooks/subscriptions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // helpers
    private WebhookDto.SubscriptionResponse subResp() {
        return new WebhookDto.SubscriptionResponse(
                SUB, TENANT, "sub", "https://x.example.com/h",
                List.of(EventType.PDCA_CYCLE_CREATED.wire()),
                SubscriptionStatus.ACTIVE, 5, 0, null, null, USER,
                Instant.now(), Instant.now());
    }

    private WebhookDto.DeliveryResponse deliveryResp() {
        return new WebhookDto.DeliveryResponse(
                DELIV, TENANT, SUB, "evt-1", EventType.PDCA_CYCLE_CREATED, "{}",
                DeliveryStatus.SUCCESS, 1, Instant.now(), null, 200, "ok", null,
                Instant.now(), Instant.now());
    }
}
