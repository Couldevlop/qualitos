package com.openlab.qualitos.quality.industry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
@WebMvcTest(controllers = IndustryPackController.class)
class IndustryPackControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean IndustryPackService service;
    ObjectMapper om;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID ACT = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_returns200() throws Exception {
        when(service.listAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(packResp("manufacturing"))));
        mockMvc.perform(get("/api/v1/industry-packs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("manufacturing"));
    }

    @Test @WithMockUser
    void getOne_returns200() throws Exception {
        when(service.getByCode("it-itsm")).thenReturn(packResp("it-itsm"));
        mockMvc.perform(get("/api/v1/industry-packs/{code}", "it-itsm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("it-itsm"));
    }

    @Test @WithMockUser
    void getOne_notFound_returns404() throws Exception {
        when(service.getByCode("ghost")).thenThrow(new IndustryPackNotFoundException("ghost"));
        mockMvc.perform(get("/api/v1/industry-packs/{code}", "ghost"))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void activate_returns200() throws Exception {
        when(service.activate(eq("manufacturing"), any()))
                .thenReturn(actResp(ActivationStatus.ACTIVE));
        IndustryPackDto.ActivateRequest req = new IndustryPackDto.ActivateRequest(USER, null);
        mockMvc.perform(post("/api/v1/industry-packs/{code}/activate", "manufacturing").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test @WithMockUser
    void activate_missingActivatedBy_returns400() throws Exception {
        // @NotNull sur activatedBy : un body vide ⇒ 400
        mockMvc.perform(post("/api/v1/industry-packs/{code}/activate", "manufacturing").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void deactivate_returns200() throws Exception {
        when(service.deactivate(eq("manufacturing"), eq(USER)))
                .thenReturn(actResp(ActivationStatus.DEACTIVATED));
        mockMvc.perform(delete("/api/v1/industry-packs/{code}/activate", "manufacturing")
                        .param("deactivatedBy", USER.toString())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEACTIVATED"));
    }

    @Test @WithMockUser
    void mine_returnsList() throws Exception {
        when(service.myActiveActivations()).thenReturn(List.of(actResp(ActivationStatus.ACTIVE)));
        mockMvc.perform(get("/api/v1/industry-packs/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test @WithMockUser
    void history_returnsList() throws Exception {
        when(service.myActivationHistory()).thenReturn(List.of(
                actResp(ActivationStatus.ACTIVE), actResp(ActivationStatus.DEACTIVATED)));
        mockMvc.perform(get("/api/v1/industry-packs/my/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    private IndustryPackDto.PackResponse packResp(String code) {
        return new IndustryPackDto.PackResponse(
                UUID.randomUUID(), code, "Pack " + code, "desc", "1.0.0", "fr-FR",
                List.of("foo"), "{}", Instant.now(), Instant.now());
    }

    private IndustryPackDto.ActivationResponse actResp(ActivationStatus status) {
        return new IndustryPackDto.ActivationResponse(
                ACT, TENANT, "manufacturing", status, USER, Instant.now(),
                status == ActivationStatus.DEACTIVATED ? Instant.now() : null,
                status == ActivationStatus.DEACTIVATED ? USER : null);
    }
}
