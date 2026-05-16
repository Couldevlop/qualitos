package com.openlab.qualitos.quality.apikeys.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.apikeys.application.ApiKeyDto;
import com.openlab.qualitos.quality.apikeys.application.ApiKeyService;
import com.openlab.qualitos.quality.apikeys.domain.ApiKeyNotFoundException;
import com.openlab.qualitos.quality.apikeys.domain.ApiKeyStateException;
import com.openlab.qualitos.quality.apikeys.domain.ApiKeyStatus;
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
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Tag;

@Tag("web")
@WebMvcTest(controllers = ApiKeyController.class)
class ApiKeyControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ApiKeyService service;
    ObjectMapper om;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID ACTOR = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final Instant FUTURE = Instant.parse("2026-06-16T10:00:00Z");

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_200() throws Exception {
        when(service.list()).thenReturn(List.of(view()));
        mockMvc.perform(get("/api/v1/api-keys"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new ApiKeyNotFoundException(ID));
        mockMvc.perform(get("/api/v1/api-keys/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void create_201_andExposesPlaintextOnce() throws Exception {
        when(service.create(any())).thenReturn(new ApiKeyDto.IssuedKey(view(), "qos_pfx_secret"));
        ApiKeyWebDto.CreateRequest req = new ApiKeyWebDto.CreateRequest(
                "ci-bot", Set.of("audit.read"), FUTURE, ACTOR);
        mockMvc.perform(post("/api/v1/api-keys").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plaintext").value("qos_pfx_secret"))
                // OWASP : la vue exposée ne doit pas contenir de champ "hashedSecret"
                .andExpect(jsonPath("$.view.hashedSecret").doesNotExist());
    }

    @Test @WithMockUser
    void create_blankName_400() throws Exception {
        String body = "{\"name\":\"\",\"actor\":\"" + ACTOR + "\"}";
        mockMvc.perform(post("/api/v1/api-keys").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingActor_400() throws Exception {
        String body = "{\"name\":\"n\"}";
        mockMvc.perform(post("/api/v1/api-keys").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_invalidScope_409() throws Exception {
        when(service.create(any())).thenThrow(new ApiKeyStateException("Invalid scope"));
        ApiKeyWebDto.CreateRequest req = new ApiKeyWebDto.CreateRequest(
                "n", Set.of("WHATEVER"), null, ACTOR);
        mockMvc.perform(post("/api/v1/api-keys").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void rotate_200() throws Exception {
        when(service.rotate(eq(ID), any()))
                .thenReturn(new ApiKeyDto.IssuedKey(view(), "qos_new_secret"));
        mockMvc.perform(post("/api/v1/api-keys/{id}/rotate", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actor\":\"" + ACTOR + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plaintext").value("qos_new_secret"));
    }

    @Test @WithMockUser
    void revoke_200() throws Exception {
        when(service.revoke(eq(ID), any())).thenReturn(view());
        mockMvc.perform(post("/api/v1/api-keys/{id}/revoke", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actor\":\"" + ACTOR + "\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void revoke_invalidState_409() throws Exception {
        when(service.revoke(eq(ID), any())).thenThrow(new ApiKeyStateException("not active"));
        mockMvc.perform(post("/api/v1/api-keys/{id}/revoke", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actor\":\"" + ACTOR + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void expireDue_200() throws Exception {
        when(service.expireDue(200)).thenReturn(3);
        mockMvc.perform(post("/api/v1/api-keys/expire-due").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expired").value(3));
    }

    @Test @WithMockUser
    void expireDue_outOfRange_400() throws Exception {
        mockMvc.perform(post("/api/v1/api-keys/expire-due")
                        .param("limit", "0").with(csrf()))
                .andExpect(status().is4xxClientError());
    }

    private ApiKeyDto.View view() {
        return new ApiKeyDto.View(
                ID, TENANT, "ci-bot", "pfx12345",
                List.of("audit.read"), ApiKeyStatus.ACTIVE,
                Instant.now(), ACTOR, FUTURE, null, null, null);
    }
}
