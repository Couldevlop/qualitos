package com.openlab.qualitos.quality.academy.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.academy.application.*;
import com.openlab.qualitos.quality.academy.domain.AcademyEnrollmentStatus;
import com.openlab.qualitos.quality.academy.domain.CourseStatus;
import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("web")
@WebMvcTest(controllers = {
        AcademyAuthoringController.class,
        AcademyLearningController.class,
        AcademyCertificateVerificationController.class})
@Import({MethodSecurityTestConfig.class, AcademyExceptionHandler.class})
class AcademyControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AcademyCourseService courseService;
    @MockitoBean AcademyExportService exportService;
    @MockitoBean AcademyLearningService learningService;
    @MockitoBean AcademyCertificateService certificateService;
    @MockitoBean AcademyLeaderboardService leaderboardService;
    ObjectMapper om;

    static final UUID COURSE = UUID.randomUUID();
    static final UUID ENR = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private AcademyDto.CourseResponse course() {
        return new AcademyDto.CourseResponse(COURSE, UUID.randomUUID(), "c1", "Cours 1", "d",
                "auditor", "manufacturing", 70, 50, 12, CourseStatus.DRAFT, UUID.randomUUID(),
                Instant.now(), Instant.now());
    }

    private AcademyDto.EnrollmentResponse enrollment() {
        return new AcademyDto.EnrollmentResponse(ENR, UUID.randomUUID(), UUID.randomUUID(), COURSE,
                AcademyEnrollmentStatus.ENROLLED, 0, null, Instant.now(), null, null, null,
                Instant.now(), Instant.now());
    }

    // ===== Authoring RBAC =====

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void createCourse_asQualityManager_201() throws Exception {
        when(courseService.createCourse(any())).thenReturn(course());
        mockMvc.perform(post("/api/v1/academy/courses").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new AcademyDto.CreateCourseRequest(
                                "c1", "Cours 1", null, null, null, null, null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("c1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createCourse_asPlainUser_403() throws Exception {
        mockMvc.perform(post("/api/v1/academy/courses").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new AcademyDto.CreateCourseRequest(
                                "c1", "Cours 1", null, null, null, null, null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void publishCourse_200() throws Exception {
        when(courseService.publishCourse(COURSE)).thenReturn(course());
        mockMvc.perform(post("/api/v1/academy/courses/" + COURSE + "/publish").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void exportScorm_returnsZip() throws Exception {
        when(exportService.exportScorm(COURSE)).thenReturn("PK".getBytes());
        mockMvc.perform(get("/api/v1/academy/courses/" + COURSE + "/export/scorm"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString(".zip")));
    }

    // ===== Learning (any authenticated user) =====

    @Test
    @WithMockUser(roles = "USER")
    void enroll_asUser_201() throws Exception {
        when(learningService.enroll(any())).thenReturn(enrollment());
        mockMvc.perform(post("/api/v1/academy/me/enrollments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new AcademyDto.EnrollRequest(COURSE))))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "USER")
    void submitQuiz_200() throws Exception {
        UUID quizId = UUID.randomUUID();
        when(learningService.submitQuiz(any(), any())).thenReturn(new AcademyDto.QuizResult(
                UUID.randomUUID(), quizId, 90, true, 9, 10, enrollment()));
        mockMvc.perform(post("/api/v1/academy/me/enrollments/" + ENR + "/submit-quiz").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new AcademyDto.SubmitQuizRequest(quizId, List.of(0, 1)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(90))
                .andExpect(jsonPath("$.passed").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void leaderboard_200() throws Exception {
        when(leaderboardService.leaderboard(20)).thenReturn(new AcademyDto.Leaderboard(List.of(), 0));
        mockMvc.perform(get("/api/v1/academy/me/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLearners").value(0));
    }

    // ===== Public verification (whitelisted) =====

    @Test
    @WithMockUser
    void verifyCertificate_public_200() throws Exception {
        when(certificateService.verify("CODE-1")).thenReturn(new AcademyDto.CertificateVerification(
                "CODE-1", true, "c1", "Cours 1", 90, Instant.now(), null, "a".repeat(64), "tx", true));
        mockMvc.perform(get("/api/v1/academy/public/certificates/CODE-1/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.signatureValid").value(true));
    }
}
