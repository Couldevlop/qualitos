package com.openlab.qualitos.quality.training;

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
import org.junit.jupiter.api.Tag;

@Tag("web")
@WebMvcTest(controllers = TrainingController.class)
class TrainingControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SkillService skillService;
    @MockitoBean TrainingPathService pathService;
    @MockitoBean TrainingEnrollmentService enrollmentService;
    ObjectMapper om;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID SKILL = UUID.randomUUID();
    static final UUID PATH = UUID.randomUUID();
    static final UUID ENR = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // --- skills ---

    @Test @WithMockUser
    void listSkills_200() throws Exception {
        when(skillService.list(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(skillResp())));
        mockMvc.perform(get("/api/v1/training/skills"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void createSkill_201() throws Exception {
        when(skillService.create(any())).thenReturn(skillResp());
        TrainingDto.CreateSkillRequest req = new TrainingDto.CreateSkillRequest(
                "iso-9001", "ISO 9001", "desc", "quality");
        mockMvc.perform(post("/api/v1/training/skills").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void createSkill_invalidCode_400() throws Exception {
        String body = "{\"code\":\"BAD CODE\",\"name\":\"n\"}";
        mockMvc.perform(post("/api/v1/training/skills").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void getSkill_notFound_404() throws Exception {
        when(skillService.get(SKILL)).thenThrow(new SkillNotFoundException(SKILL));
        mockMvc.perform(get("/api/v1/training/skills/{id}", SKILL))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void deleteSkill_blocked_409() throws Exception {
        doThrow(new TrainingStateException("referenced")).when(skillService).delete(SKILL);
        mockMvc.perform(delete("/api/v1/training/skills/{id}", SKILL).with(csrf()))
                .andExpect(status().isConflict());
    }

    // --- competencies ---

    @Test @WithMockUser
    void assess_returnsCompetency() throws Exception {
        when(skillService.assess(any())).thenReturn(competencyResp());
        TrainingDto.AssessCompetencyRequest req = new TrainingDto.AssessCompetencyRequest(
                USER, SKILL, 3, CompetencySource.TRAINING, null, null);
        mockMvc.perform(post("/api/v1/training/competencies/assess").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void assess_levelOutOfRange_400() throws Exception {
        String body = "{\"userId\":\"" + USER + "\",\"skillId\":\"" + SKILL
                + "\",\"level\":7,\"source\":\"SELF\"}";
        mockMvc.perform(post("/api/v1/training/competencies/assess").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void matrix_returns200() throws Exception {
        when(skillService.matrix(USER)).thenReturn(
                new TrainingDto.CompetencyMatrix(USER, List.of()));
        mockMvc.perform(get("/api/v1/training/competencies/users/{id}", USER))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void gap_returns200() throws Exception {
        when(pathService.analyzeGap(USER, PATH)).thenReturn(
                new TrainingDto.RoleGapAnalysis(USER, PATH, "p", 0, 0, List.of()));
        mockMvc.perform(get("/api/v1/training/competencies/users/{id}/gap", USER)
                        .param("pathId", PATH.toString()))
                .andExpect(status().isOk());
    }

    // --- paths ---

    @Test @WithMockUser
    void createPath_201() throws Exception {
        when(pathService.create(any())).thenReturn(pathResp());
        TrainingDto.CreatePathRequest req = new TrainingDto.CreatePathRequest(
                "p1", "Path", null, "auditor", 16, 80, 24, USER);
        mockMvc.perform(post("/api/v1/training/paths").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void createPath_zeroDuration_400() throws Exception {
        String body = "{\"code\":\"p\",\"name\":\"n\",\"durationHours\":0,\"createdBy\":\""
                + USER + "\"}";
        mockMvc.perform(post("/api/v1/training/paths").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void activatePath_409whenNoRequirements() throws Exception {
        when(pathService.activate(PATH))
                .thenThrow(new TrainingStateException("at least one skill requirement"));
        mockMvc.perform(post("/api/v1/training/paths/{id}/activate", PATH).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void attachRequirement_201() throws Exception {
        when(pathService.attachSkill(eq(PATH), any())).thenReturn(
                new TrainingDto.SkillRequirementResponse(
                        UUID.randomUUID(), PATH, SKILL, 3, Instant.now()));
        TrainingDto.AttachSkillRequirementRequest req =
                new TrainingDto.AttachSkillRequirementRequest(SKILL, 3);
        mockMvc.perform(post("/api/v1/training/paths/{id}/requirements", PATH).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void detachRequirement_204() throws Exception {
        mockMvc.perform(delete("/api/v1/training/paths/{id}/requirements/{skillId}", PATH, SKILL)
                        .with(csrf()))
                .andExpect(status().isNoContent());
        verify(pathService).detachSkill(PATH, SKILL);
    }

    @Test @WithMockUser
    void listRequirements_returns200() throws Exception {
        when(pathService.listRequirements(PATH)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/training/paths/{id}/requirements", PATH))
                .andExpect(status().isOk());
    }

    // --- enrollments ---

    @Test @WithMockUser
    void enroll_201() throws Exception {
        when(enrollmentService.enroll(any())).thenReturn(enrollmentResp());
        TrainingDto.EnrollRequest req = new TrainingDto.EnrollRequest(USER, PATH);
        mockMvc.perform(post("/api/v1/training/enrollments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void enroll_missingPath_400() throws Exception {
        String body = "{\"userId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/training/enrollments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void listEnrollments_filtered_byUser() throws Exception {
        when(enrollmentService.listByUser(eq(USER), any()))
                .thenReturn(new PageImpl<>(List.of(enrollmentResp())));
        mockMvc.perform(get("/api/v1/training/enrollments")
                        .param("userId", USER.toString()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void listEnrollments_filtered_byPath() throws Exception {
        when(enrollmentService.listByPath(eq(PATH), any()))
                .thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/api/v1/training/enrollments")
                        .param("pathId", PATH.toString()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void listEnrollments_noFilter_returnsEmptyPage() throws Exception {
        mockMvc.perform(get("/api/v1/training/enrollments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test @WithMockUser
    void start_returns200() throws Exception {
        when(enrollmentService.start(ENR)).thenReturn(enrollmentResp());
        mockMvc.perform(post("/api/v1/training/enrollments/{id}/start", ENR).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void progress_returns200() throws Exception {
        when(enrollmentService.updateProgress(eq(ENR), any())).thenReturn(enrollmentResp());
        mockMvc.perform(post("/api/v1/training/enrollments/{id}/progress", ENR).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"progressPct\":50}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void progress_invalidPct_400() throws Exception {
        mockMvc.perform(post("/api/v1/training/enrollments/{id}/progress", ENR).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"progressPct\":150}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void complete_returns200() throws Exception {
        when(enrollmentService.complete(eq(ENR), any())).thenReturn(enrollmentResp());
        mockMvc.perform(post("/api/v1/training/enrollments/{id}/complete", ENR).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"finalScore\":85}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void cancel_returns200() throws Exception {
        when(enrollmentService.cancel(ENR)).thenReturn(enrollmentResp());
        mockMvc.perform(post("/api/v1/training/enrollments/{id}/cancel", ENR).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void cancel_terminal_409() throws Exception {
        when(enrollmentService.cancel(ENR))
                .thenThrow(new TrainingStateException("terminal"));
        mockMvc.perform(post("/api/v1/training/enrollments/{id}/cancel", ENR).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void verifyCertificate_returns200() throws Exception {
        when(enrollmentService.verifyCertificate("XYZ")).thenReturn(
                new TrainingDto.CertificateVerification(
                        "XYZ", ENR, TENANT, USER, PATH, "p", "Path",
                        88, LocalDate.parse("2026-04-01"),
                        LocalDate.parse("2027-04-01"), true));
        mockMvc.perform(get("/api/v1/training/certificates/{code}", "XYZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test @WithMockUser
    void verifyCertificate_unknown_404() throws Exception {
        when(enrollmentService.verifyCertificate("NOPE"))
                .thenThrow(new EnrollmentNotFoundException("NOPE"));
        mockMvc.perform(get("/api/v1/training/certificates/{code}", "NOPE"))
                .andExpect(status().isNotFound());
    }

    // --- factories ---

    private TrainingDto.SkillResponse skillResp() {
        return new TrainingDto.SkillResponse(
                SKILL, TENANT, "iso-9001", "ISO 9001", "desc", "quality",
                Instant.now(), Instant.now());
    }

    private TrainingDto.CompetencyResponse competencyResp() {
        return new TrainingDto.CompetencyResponse(
                UUID.randomUUID(), TENANT, USER, SKILL,
                3, CompetencyLevel.COMPETENT,
                CompetencySource.TRAINING, null,
                LocalDate.parse("2026-05-15"), null, false,
                Instant.now(), Instant.now());
    }

    private TrainingDto.PathResponse pathResp() {
        return new TrainingDto.PathResponse(
                PATH, TENANT, "p", "Path", null, "auditor",
                16, 80, 24, TrainingPathStatus.DRAFT, USER,
                Instant.now(), Instant.now());
    }

    private TrainingDto.EnrollmentResponse enrollmentResp() {
        return new TrainingDto.EnrollmentResponse(
                ENR, TENANT, USER, PATH, EnrollmentStatus.ENROLLED, 0, null,
                LocalDate.parse("2026-05-15"), null, null, null, null,
                Instant.now(), Instant.now());
    }
}
