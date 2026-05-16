package com.openlab.qualitos.quality.calibration;

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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Tag;

@Tag("web")
@WebMvcTest(controllers = CalibrationController.class)
class CalibrationControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CalibrationService service;
    ObjectMapper om;

    static final UUID EQ = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_200() throws Exception {
        when(service.listEquipment(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(equipmentResp())));
        mockMvc.perform(get("/api/v1/calibration/equipment"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void create_201() throws Exception {
        when(service.createEquipment(any())).thenReturn(equipmentResp());
        CalibrationDto.CreateEquipmentRequest req = new CalibrationDto.CreateEquipmentRequest(
                "EQ-1", "Caliper", "Mitutoyo", null, "SN-1", "Lab",
                true, null, null, USER);
        mockMvc.perform(post("/api/v1/calibration/equipment").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void create_invalidCode_400() throws Exception {
        String body = "{\"code\":\"bad code!\",\"name\":\"n\",\"createdBy\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/calibration/equipment").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingCreatedBy_400() throws Exception {
        String body = "{\"code\":\"EQ-1\",\"name\":\"n\"}";
        mockMvc.perform(post("/api/v1/calibration/equipment").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.getEquipment(EQ))
                .thenThrow(new CalibrationEquipmentNotFoundException(EQ));
        mockMvc.perform(get("/api/v1/calibration/equipment/{id}", EQ))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void update_200() throws Exception {
        when(service.updateEquipment(any(), any())).thenReturn(equipmentResp());
        mockMvc.perform(patch("/api/v1/calibration/equipment/{id}", EQ).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void setStatus_200() throws Exception {
        when(service.setEquipmentStatus(eq(EQ), eq(EquipmentStatus.OUT_OF_SERVICE)))
                .thenReturn(equipmentResp());
        mockMvc.perform(post("/api/v1/calibration/equipment/{id}/status/{t}", EQ, "OUT_OF_SERVICE")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void setStatus_blockedByCritical_409() throws Exception {
        when(service.setEquipmentStatus(eq(EQ), eq(EquipmentStatus.ACTIVE)))
                .thenThrow(new CalibrationStateException("needs PASS"));
        mockMvc.perform(post("/api/v1/calibration/equipment/{id}/status/{t}", EQ, "ACTIVE")
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void delete_204() throws Exception {
        mockMvc.perform(delete("/api/v1/calibration/equipment/{id}", EQ).with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).deleteEquipment(EQ);
    }

    @Test @WithMockUser
    void summary_200() throws Exception {
        when(service.summary(EQ)).thenReturn(new CalibrationDto.EquipmentSummary(
                EQ, EquipmentStatus.ACTIVE, true, LocalDate.parse("2026-04-01"),
                LocalDate.parse("2027-04-01"), false, CalibrationResult.PASS,
                5L, 1L, 2L, 3L, 0L));
        mockMvc.perform(get("/api/v1/calibration/equipment/{id}/summary", EQ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passRecords").value(5));
    }

    // ---- Plan ----

    @Test @WithMockUser
    void upsertPlan_200() throws Exception {
        when(service.upsertPlan(eq(EQ), any())).thenReturn(planResp());
        CalibrationDto.UpsertPlanRequest req = new CalibrationDto.UpsertPlanRequest(
                12, "PROC", "tol", "COFRAC", LocalDate.parse("2026-09-01"));
        mockMvc.perform(put("/api/v1/calibration/equipment/{id}/plan", EQ).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void upsertPlan_invalidFrequency_400() throws Exception {
        String body = "{\"frequencyMonths\":0,\"firstDueOn\":\"2026-09-01\"}";
        mockMvc.perform(put("/api/v1/calibration/equipment/{id}/plan", EQ).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void upsertPlan_missingFirstDueOn_400() throws Exception {
        String body = "{\"frequencyMonths\":12}";
        mockMvc.perform(put("/api/v1/calibration/equipment/{id}/plan", EQ).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void getPlan_present_200() throws Exception {
        when(service.getPlan(EQ)).thenReturn(Optional.of(planResp()));
        mockMvc.perform(get("/api/v1/calibration/equipment/{id}/plan", EQ))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void getPlan_absent_404() throws Exception {
        when(service.getPlan(EQ)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/calibration/equipment/{id}/plan", EQ))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void deletePlan_critical_409() throws Exception {
        doThrow(new CalibrationStateException("critical")).when(service).deletePlan(EQ);
        mockMvc.perform(delete("/api/v1/calibration/equipment/{id}/plan", EQ).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void deletePlan_missing_404() throws Exception {
        doThrow(new CalibrationChildNotFoundException("Plan", EQ))
                .when(service).deletePlan(EQ);
        mockMvc.perform(delete("/api/v1/calibration/equipment/{id}/plan", EQ).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void overdue_200() throws Exception {
        when(service.overdue(any(), any())).thenReturn(new PageImpl<>(List.of(planResp())));
        mockMvc.perform(get("/api/v1/calibration/plans/overdue"))
                .andExpect(status().isOk());
    }

    // ---- Records ----

    @Test @WithMockUser
    void addRecord_201() throws Exception {
        when(service.addRecord(eq(EQ), any())).thenReturn(recordResp());
        CalibrationDto.CreateRecordRequest req = new CalibrationDto.CreateRecordRequest(
                LocalDate.parse("2026-05-01"), USER, "AcmeLab",
                CalibrationResult.PASS, "values", "CERT-1", null);
        mockMvc.perform(post("/api/v1/calibration/equipment/{id}/records", EQ).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void addRecord_missingResult_400() throws Exception {
        String body = "{\"performedOn\":\"2026-05-01\"}";
        mockMvc.perform(post("/api/v1/calibration/equipment/{id}/records", EQ).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void listRecords_200() throws Exception {
        when(service.listRecords(eq(EQ), any())).thenReturn(new PageImpl<>(List.of(recordResp())));
        mockMvc.perform(get("/api/v1/calibration/equipment/{id}/records", EQ))
                .andExpect(status().isOk());
    }

    // ---- MSA ----

    @Test @WithMockUser
    void addMsa_201() throws Exception {
        when(service.addMsa(eq(EQ), any())).thenReturn(msaResp());
        CalibrationDto.CreateMsaRequest req = new CalibrationDto.CreateMsaRequest(
                MsaType.GAGE_R_R, LocalDate.parse("2026-04-01"),
                new BigDecimal("8.50"), new BigDecimal("10.00"),
                MsaResult.PASS, "notes", USER);
        mockMvc.perform(post("/api/v1/calibration/equipment/{id}/msa", EQ).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void addMsa_missingType_400() throws Exception {
        String body = "{\"performedOn\":\"2026-04-01\",\"studyValue\":8.5,\"result\":\"PASS\","
                + "\"createdBy\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/calibration/equipment/{id}/msa", EQ).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void listMsa_200() throws Exception {
        when(service.listMsa(eq(EQ), any())).thenReturn(new PageImpl<>(List.of(msaResp())));
        mockMvc.perform(get("/api/v1/calibration/equipment/{id}/msa", EQ))
                .andExpect(status().isOk());
    }

    // ---- factories ----

    private CalibrationDto.EquipmentResponse equipmentResp() {
        return new CalibrationDto.EquipmentResponse(
                EQ, TENANT, "EQ-1", "Caliper", "Mitutoyo", "CD-15CPX", "SN-1",
                "Lab", EquipmentStatus.ACTIVE, true,
                null, USER, USER, Instant.now(), Instant.now());
    }

    private CalibrationDto.PlanResponse planResp() {
        return new CalibrationDto.PlanResponse(
                UUID.randomUUID(), TENANT, EQ, 12, "PROC", "tol", "COFRAC",
                LocalDate.parse("2026-04-01"), LocalDate.parse("2027-04-01"),
                false, Instant.now(), Instant.now());
    }

    private CalibrationDto.RecordResponse recordResp() {
        return new CalibrationDto.RecordResponse(
                UUID.randomUUID(), TENANT, EQ,
                LocalDate.parse("2026-05-01"), USER, "AcmeLab",
                CalibrationResult.PASS, "values", "CERT-1", null,
                Instant.now());
    }

    private CalibrationDto.MsaResponse msaResp() {
        return new CalibrationDto.MsaResponse(
                UUID.randomUUID(), TENANT, EQ,
                MsaType.GAGE_R_R, LocalDate.parse("2026-04-01"),
                new BigDecimal("8.50"), new BigDecimal("10.00"),
                MsaResult.PASS, "notes", USER, Instant.now());
    }
}
