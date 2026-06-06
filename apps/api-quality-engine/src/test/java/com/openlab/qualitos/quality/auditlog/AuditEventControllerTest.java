package com.openlab.qualitos.quality.auditlog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Tag;

@Tag("web")
@WebMvcTest(controllers = AuditEventController.class)
@Import(MethodSecurityTestConfig.class)
class AuditEventControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AuditEventService service;
    ObjectMapper om;

    static final UUID EVT = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID RES = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_200() throws Exception {
        when(service.list(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(eventResp())));
        mockMvc.perform(get("/api/v1/audit/events"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void list_byResource() throws Exception {
        when(service.list(any(), eq("capa"), eq(RES), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(eventResp())));
        mockMvc.perform(get("/api/v1/audit/events")
                        .param("resourceType", "capa")
                        .param("resourceId", RES.toString()))
                .andExpect(status().isOk());
    }

    // --- C1 : forge du journal d'audit ---

    static final UUID JWT_SUB = UUID.randomUUID();

    @Test @WithMockUser(roles = "USER")
    void create_nonAdmin_403() throws Exception {
        // Un utilisateur standard ne peut PAS écrire dans la chaîne d'audit.
        AuditEventDto.RecordEventRequest req = new AuditEventDto.RecordEventRequest(
                null, ActorType.USER, USER, "pdca.cycle.created", "pdca-cycle",
                RES, "Cycle X", "{}", "1.2.3.4", "forged-agent");
        mockMvc.perform(post("/api/v1/audit/events").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void create_admin_201_overridesServerControlledFields() throws Exception {
        when(service.record(any())).thenReturn(eventResp());
        // Le client tente de forger l'acteur, l'IP, l'UA et l'horodatage : tout est ignoré.
        AuditEventDto.RecordEventRequest req = new AuditEventDto.RecordEventRequest(
                Instant.parse("2000-01-01T00:00:00Z"), ActorType.USER, USER,
                "pdca.cycle.created", "pdca-cycle", RES, "Cycle X", "{}",
                "9.9.9.9", "forged-agent");

        mockMvc.perform(post("/api/v1/audit/events").with(csrf())
                        .with(jwt().jwt(j -> j.subject(JWT_SUB.toString()))
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")))
                        .header("User-Agent", "real-agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());

        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(service).record(cap.capture());
        AuditEventDto.RecordEventRequest passed = cap.getValue();
        // Acteur = sub du JWT, pas la valeur forgée du body.
        assertThat(passed.actorUserId()).isEqualTo(JWT_SUB);
        // occurredAt forcé à null → horloge serveur côté service.
        assertThat(passed.occurredAt()).isNull();
        // IP / UA issues de la requête, pas du body.
        assertThat(passed.ipAddress()).isNotEqualTo("9.9.9.9");
        assertThat(passed.userAgent()).isEqualTo("real-agent");
        // La description métier reste celle du body.
        assertThat(passed.action()).isEqualTo("pdca.cycle.created");
        assertThat(passed.summary()).isEqualTo("Cycle X");
    }

    @Test @WithMockUser(roles = "ADMIN")
    void create_invalidAction_400() throws Exception {
        String body = "{\"actorType\":\"USER\",\"action\":\"BAD ACTION!\",\"resourceType\":\"foo\"}";
        mockMvc.perform(post("/api/v1/audit/events").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void create_invalidResourceType_400() throws Exception {
        String body = "{\"actorType\":\"USER\",\"action\":\"x.y\",\"resourceType\":\"BAD-Type\"}";
        mockMvc.perform(post("/api/v1/audit/events").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void create_missingActorType_400() throws Exception {
        String body = "{\"action\":\"x.y\",\"resourceType\":\"foo\"}";
        mockMvc.perform(post("/api/v1/audit/events").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(EVT)).thenThrow(new AuditEventNotFoundException(EVT));
        mockMvc.perform(get("/api/v1/audit/events/{id}", EVT))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void verify_200_valid() throws Exception {
        when(service.verifyChain(1L, 5L)).thenReturn(new AuditEventDto.ChainVerification(
                TENANT, 1L, 5L, 5L, true, List.of()));
        mockMvc.perform(get("/api/v1/audit/events/verify")
                        .param("fromSeq", "1")
                        .param("toSeq", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test @WithMockUser
    void verify_invalidRange_409() throws Exception {
        when(service.verifyChain(5L, 1L)).thenThrow(new AuditEventStateException("toSeq < fromSeq"));
        mockMvc.perform(get("/api/v1/audit/events/verify")
                        .param("fromSeq", "5")
                        .param("toSeq", "1"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void anchor_200() throws Exception {
        when(service.anchor(eq(EVT), any())).thenReturn(eventResp());
        mockMvc.perform(post("/api/v1/audit/events/{id}/anchor", EVT).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"blockchainTxRef\":\"fabric-tx-abc\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void anchor_alreadyAnchored_409() throws Exception {
        when(service.anchor(eq(EVT), any()))
                .thenThrow(new AuditEventStateException("already anchored"));
        mockMvc.perform(post("/api/v1/audit/events/{id}/anchor", EVT).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"blockchainTxRef\":\"x\"}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void anchor_missingTxRef_400() throws Exception {
        mockMvc.perform(post("/api/v1/audit/events/{id}/anchor", EVT).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    private AuditEventDto.EventResponse eventResp() {
        return new AuditEventDto.EventResponse(
                EVT, TENANT, 1L,
                Instant.parse("2026-05-15T10:00:00Z"),
                Instant.parse("2026-05-15T10:00:01Z"),
                ActorType.USER, USER,
                "pdca.cycle.created", "pdca-cycle", RES,
                "Cycle X", "{}", "127.0.0.1", "agent",
                "a".repeat(64), null, null);
    }
}
