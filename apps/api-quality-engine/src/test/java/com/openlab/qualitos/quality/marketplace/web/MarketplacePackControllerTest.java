package com.openlab.qualitos.quality.marketplace.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.marketplace.application.MarketplacePackDto;
import com.openlab.qualitos.quality.marketplace.application.MarketplacePackService;
import com.openlab.qualitos.quality.marketplace.domain.InstallationStatus;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePackStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("web")
@WebMvcTest(controllers = MarketplacePackController.class)
@Import(MethodSecurityTestConfig.class)
class MarketplacePackControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean MarketplacePackService service;
    ObjectMapper om;

    static final UUID ID = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private MarketplacePackDto.View view(MarketplacePackStatus status) {
        return new MarketplacePackDto.View(ID, "iso", "1.0", "Pub", "Title", "d",
                "healthcare", List.of("iso-9001"), 0, "EUR", status,
                null, null, null, null, null, null, "https://x.com/y.zip",
                0.0, 0, Instant.now(), Instant.now());
    }

    private MarketplacePackDto.InstallationView install() {
        return new MarketplacePackDto.InstallationView(UUID.randomUUID(), UUID.randomUUID(), ID,
                "iso", "1.0", InstallationStatus.INSTALLED, UUID.randomUUID(), Instant.now(), null, null);
    }

    private String submitBody() throws Exception {
        return om.writeValueAsString(new MarketplacePackWebDto.SubmitRequest(
                "iso", "1.0", "Pub", "Title", "desc", "healthcare", List.of("iso-9001"),
                0, "EUR", "https://x.com/y.zip",
                "{\"name\":\"x\",\"version\":\"1.0\"}", "deadbeef".repeat(8)));
    }

    // ---------- lecture catalogue (authentifié) ----------

    @Test @WithMockUser
    void list_200() throws Exception {
        when(service.listPublished(any())).thenReturn(List.of(view(MarketplacePackStatus.PUBLISHED)));
        mockMvc.perform(get("/api/v1/marketplace/packs")).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_200() throws Exception {
        when(service.getPublished(ID)).thenReturn(view(MarketplacePackStatus.PUBLISHED));
        mockMvc.perform(get("/api/v1/marketplace/packs/{id}", ID)).andExpect(status().isOk());
    }

    // ---------- soumission (PARTNER) ----------

    @Test @WithMockUser(roles = "PARTNER")
    void submit_partner_201() throws Exception {
        when(service.submit(any())).thenReturn(view(MarketplacePackStatus.SUBMITTED));
        mockMvc.perform(post("/api/v1/marketplace/packs").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(submitBody()))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser(roles = "USER")
    void submit_simpleUser_403() throws Exception {
        mockMvc.perform(post("/api/v1/marketplace/packs").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(submitBody()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "PARTNER")
    void submit_invalidBody_400() throws Exception {
        // signatureHash trop court → rejet Jakarta (400) avant le service.
        String bad = om.writeValueAsString(new MarketplacePackWebDto.SubmitRequest(
                "iso", "1.0", "Pub", "Title", "desc", "healthcare", List.of("iso-9001"),
                0, "EUR", "https://x.com/y.zip", "{\"name\":\"x\"}", "short"));
        mockMvc.perform(post("/api/v1/marketplace/packs").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest());
    }

    // ---------- modération (SUPER_ADMIN) ----------

    @Test @WithMockUser(roles = "SUPER_ADMIN")
    void publish_superAdmin_200() throws Exception {
        when(service.publish(ID)).thenReturn(view(MarketplacePackStatus.PUBLISHED));
        mockMvc.perform(post("/api/v1/marketplace/packs/{id}/publish", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void publish_adminTenant_403() throws Exception {
        mockMvc.perform(post("/api/v1/marketplace/packs/{id}/publish", ID).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "SUPER_ADMIN")
    void takeForReview_superAdmin_200() throws Exception {
        when(service.takeForReview(ID)).thenReturn(view(MarketplacePackStatus.IN_REVIEW));
        mockMvc.perform(post("/api/v1/marketplace/packs/{id}/take-review", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "SUPER_ADMIN")
    void reject_superAdmin_200() throws Exception {
        when(service.reject(any(), any())).thenReturn(view(MarketplacePackStatus.REJECTED));
        mockMvc.perform(post("/api/v1/marketplace/packs/{id}/reject", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"manifeste incomplet\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "SUPER_ADMIN")
    void deprecate_superAdmin_200() throws Exception {
        when(service.deprecate(ID)).thenReturn(view(MarketplacePackStatus.DEPRECATED));
        mockMvc.perform(post("/api/v1/marketplace/packs/{id}/deprecate", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "SUPER_ADMIN")
    void moderationQueue_superAdmin_200() throws Exception {
        when(service.moderationQueue()).thenReturn(List.of(view(MarketplacePackStatus.SUBMITTED)));
        mockMvc.perform(get("/api/v1/marketplace/packs/moderation/queue")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "USER")
    void moderationQueue_user_403() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/packs/moderation/queue"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    // ---------- installation (ADMIN_TENANT) ----------

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void install_adminTenant_201() throws Exception {
        when(service.install(ID)).thenReturn(install());
        mockMvc.perform(post("/api/v1/marketplace/packs/{id}/install", ID).with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser(roles = "USER")
    void install_user_403() throws Exception {
        mockMvc.perform(post("/api/v1/marketplace/packs/{id}/install", ID).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void uninstall_adminTenant_200() throws Exception {
        when(service.uninstall(any())).thenReturn(install());
        mockMvc.perform(delete("/api/v1/marketplace/packs/installations/{id}", UUID.randomUUID()).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void myInstallations_200() throws Exception {
        when(service.myInstallations()).thenReturn(List.of(install()));
        mockMvc.perform(get("/api/v1/marketplace/packs/installations/my")).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void myInstallationHistory_200() throws Exception {
        when(service.myInstallationHistory()).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/marketplace/packs/installations/my/history")).andExpect(status().isOk());
    }

    // ---------- notation ----------

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void rate_adminTenant_200() throws Exception {
        when(service.rate(any(), any())).thenReturn(view(MarketplacePackStatus.PUBLISHED));
        mockMvc.perform(post("/api/v1/marketplace/packs/{id}/rate", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stars\":4}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void rate_invalidStars_400() throws Exception {
        mockMvc.perform(post("/api/v1/marketplace/packs/{id}/rate", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stars\":9}"))
                .andExpect(status().isBadRequest());
    }
}
