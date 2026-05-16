package com.openlab.qualitos.quality.ratelimit.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.ratelimit.application.RateLimitDto;
import com.openlab.qualitos.quality.ratelimit.application.RateLimitService;
import com.openlab.qualitos.quality.ratelimit.domain.RateLimitDecision;
import com.openlab.qualitos.quality.ratelimit.domain.RateLimitPolicyException;
import com.openlab.qualitos.quality.ratelimit.domain.RateLimitPolicyNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RateLimitController.class)
class RateLimitControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean RateLimitService service;
    ObjectMapper om;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test @WithMockUser
    void list_200() throws Exception {
        when(service.list()).thenReturn(List.of(view()));
        mockMvc.perform(get("/api/v1/rate-limits/policies"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new RateLimitPolicyNotFoundException(ID));
        mockMvc.perform(get("/api/v1/rate-limits/policies/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void upsert_200() throws Exception {
        when(service.upsert(any())).thenReturn(view());
        RateLimitWebDto.UpsertRequest req =
                new RateLimitWebDto.UpsertRequest("api.public", 60, 100, true);
        mockMvc.perform(put("/api/v1/rate-limits/policies").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void upsert_invalidScope_400() throws Exception {
        String body = "{\"scope\":\"BAD\",\"windowSeconds\":60,\"maxRequests\":100,\"enabled\":true}";
        mockMvc.perform(put("/api/v1/rate-limits/policies").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void upsert_invalidWindow_400() throws Exception {
        String body = "{\"scope\":\"x\",\"windowSeconds\":0,\"maxRequests\":100,\"enabled\":true}";
        mockMvc.perform(put("/api/v1/rate-limits/policies").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void upsert_domainException_400() throws Exception {
        when(service.upsert(any())).thenThrow(new RateLimitPolicyException("nope"));
        RateLimitWebDto.UpsertRequest req =
                new RateLimitWebDto.UpsertRequest("x", 60, 100, true);
        mockMvc.perform(put("/api/v1/rate-limits/policies").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void delete_204() throws Exception {
        mockMvc.perform(delete("/api/v1/rate-limits/policies/{id}", ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser
    void check_200() throws Exception {
        when(service.peek(eq(TENANT), eq("x")))
                .thenReturn(RateLimitDecision.allow(100, 42, 60));
        mockMvc.perform(get("/api/v1/rate-limits/check/{scope}", "x"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.remaining").value(42));
    }

    @Test @WithMockUser
    void check_noTenant_403() throws Exception {
        TenantContext.clear();
        mockMvc.perform(get("/api/v1/rate-limits/check/{scope}", "x"))
                .andExpect(status().isForbidden());
    }

    private RateLimitDto.PolicyView view() {
        return new RateLimitDto.PolicyView(
                ID, TENANT, "api.public", 60, 100, true,
                Instant.now(), Instant.now());
    }
}
