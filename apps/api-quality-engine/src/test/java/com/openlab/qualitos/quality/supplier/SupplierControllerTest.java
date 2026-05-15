package com.openlab.qualitos.quality.supplier;

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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SupplierController.class)
class SupplierControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SupplierService service;
    ObjectMapper om;

    static final UUID SUP = UUID.randomUUID();
    static final UUID CHILD = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_returns200() throws Exception {
        when(service.list(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(resp())));
        mockMvc.perform(get("/api/v1/suppliers"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void create_returns201() throws Exception {
        when(service.create(any())).thenReturn(resp());
        SupplierDto.CreateSupplierRequest req = new SupplierDto.CreateSupplierRequest(
                "acme", "Acme", "FR", "ops@acme.test", SupplierType.COMPONENT, USER);
        mockMvc.perform(post("/api/v1/suppliers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void create_invalidCountryCode_returns400() throws Exception {
        String body = "{\"code\":\"x\",\"name\":\"n\",\"countryCode\":\"FRA\","
                + "\"supplierType\":\"COMPONENT\",\"createdBy\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/suppliers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_invalidEmail_returns400() throws Exception {
        String body = "{\"code\":\"x\",\"name\":\"n\",\"contactEmail\":\"not-an-email\","
                + "\"supplierType\":\"COMPONENT\",\"createdBy\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/suppliers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_invalidCode_returns400() throws Exception {
        String body = "{\"code\":\"bad code\",\"name\":\"n\",\"supplierType\":\"COMPONENT\","
                + "\"createdBy\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/suppliers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void get_notFound_returns404() throws Exception {
        when(service.get(SUP)).thenThrow(new SupplierNotFoundException(SUP));
        mockMvc.perform(get("/api/v1/suppliers/{id}", SUP))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void update_patches() throws Exception {
        when(service.update(any(), any())).thenReturn(resp());
        mockMvc.perform(patch("/api/v1/suppliers/{id}", SUP).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/suppliers/{id}", SUP).with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).delete(SUP);
    }

    @Test @WithMockUser
    void changeStatus_returns200() throws Exception {
        when(service.changeStatus(eq(SUP), eq(SupplierStatus.APPROVED), any())).thenReturn(resp());
        SupplierDto.StatusChangeRequest req = new SupplierDto.StatusChangeRequest(USER, "ok");
        mockMvc.perform(post("/api/v1/suppliers/{id}/status/{target}", SUP, "APPROVED").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void changeStatus_invalidTransition_returns409() throws Exception {
        when(service.changeStatus(eq(SUP), eq(SupplierStatus.APPROVED), any()))
                .thenThrow(new SupplierStateException("not allowed"));
        SupplierDto.StatusChangeRequest req = new SupplierDto.StatusChangeRequest(USER, null);
        mockMvc.perform(post("/api/v1/suppliers/{id}/status/{target}", SUP, "APPROVED").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void changeStatus_missingActor_returns400() throws Exception {
        // actorUserId est @NotNull
        mockMvc.perform(post("/api/v1/suppliers/{id}/status/{target}", SUP, "APPROVED").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void statistics_returns200() throws Exception {
        when(service.statistics(SUP)).thenReturn(new SupplierDto.SupplierStatistics(
                SUP, 80, SupplierStatus.APPROVED, 2L, 5L, 1L, LocalDate.parse("2026-02-01")));
        mockMvc.perform(get("/api/v1/suppliers/{id}/statistics", SUP))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(80))
                .andExpect(jsonPath("$.openNonConformities").value(2));
    }

    // ----- audits, NC, certs -----

    @Test @WithMockUser
    void listAudits_returns200() throws Exception {
        when(service.listAudits(eq(SUP), any())).thenReturn(new PageImpl<>(List.of(audit())));
        mockMvc.perform(get("/api/v1/suppliers/{id}/audits", SUP))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void addAudit_returns201() throws Exception {
        when(service.addAudit(eq(SUP), any())).thenReturn(audit());
        SupplierDto.CreateAuditRequest req = new SupplierDto.CreateAuditRequest(
                LocalDate.parse("2026-04-01"), 88, USER, "ok", 0, 1, 2);
        mockMvc.perform(post("/api/v1/suppliers/{id}/audits", SUP).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void addAudit_scoreOutOfRange_returns400() throws Exception {
        String body = "{\"auditedOn\":\"2026-04-01\",\"score\":150}";
        mockMvc.perform(post("/api/v1/suppliers/{id}/audits", SUP).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void addNc_returns201() throws Exception {
        when(service.addNonConformity(eq(SUP), any())).thenReturn(ncResp());
        SupplierDto.CreateNonConformityRequest req = new SupplierDto.CreateNonConformityRequest(
                "LOT-1", "issue", NonConformitySeverity.MAJOR, LocalDate.parse("2026-05-01"));
        mockMvc.perform(post("/api/v1/suppliers/{id}/non-conformities", SUP).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void updateNc_childNotFound_returns404() throws Exception {
        when(service.updateNonConformity(eq(SUP), eq(CHILD), any()))
                .thenThrow(new SupplierChildNotFoundException("Non-conformity", CHILD));
        mockMvc.perform(patch("/api/v1/suppliers/{id}/non-conformities/{ncId}", SUP, CHILD)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void addCert_returns201() throws Exception {
        when(service.addCertificate(eq(SUP), any())).thenReturn(certResp());
        SupplierDto.CreateCertificateRequest req = new SupplierDto.CreateCertificateRequest(
                "iso-9001", "REF", LocalDate.parse("2024-01-01"),
                LocalDate.parse("2027-01-01"), null);
        mockMvc.perform(post("/api/v1/suppliers/{id}/certificates", SUP).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void addCert_invalidStandardCode_returns400() throws Exception {
        String body = "{\"standardCode\":\"BAD-CODE\",\"issuedOn\":\"2024-01-01\","
                + "\"expiresOn\":\"2027-01-01\"}";
        mockMvc.perform(post("/api/v1/suppliers/{id}/certificates", SUP).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void deleteCert_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/suppliers/{id}/certificates/{certId}", SUP, CHILD)
                        .with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).deleteCertificate(SUP, CHILD);
    }

    // --- factories ---

    private SupplierDto.SupplierResponse resp() {
        return new SupplierDto.SupplierResponse(
                SUP, TENANT, "acme", "Acme", "FR", "ops@acme.test",
                SupplierType.COMPONENT, SupplierStatus.PROSPECT, 100, null,
                null, null, USER, Instant.now(), Instant.now());
    }

    private SupplierDto.AuditResponse audit() {
        return new SupplierDto.AuditResponse(
                CHILD, TENANT, SUP, LocalDate.parse("2026-04-01"),
                88, USER, "summary", 0, 1, 2, Instant.now());
    }

    private SupplierDto.NonConformityResponse ncResp() {
        return new SupplierDto.NonConformityResponse(
                CHILD, TENANT, SUP, "LOT-1", "issue",
                NonConformitySeverity.MAJOR, NonConformityStatus.OPEN,
                LocalDate.parse("2026-05-01"), null, null,
                Instant.now(), Instant.now());
    }

    private SupplierDto.CertificateResponse certResp() {
        return new SupplierDto.CertificateResponse(
                CHILD, TENANT, SUP, "iso-9001", "REF",
                LocalDate.parse("2024-01-01"), LocalDate.parse("2027-01-01"),
                null, false, Instant.now(), Instant.now());
    }
}
