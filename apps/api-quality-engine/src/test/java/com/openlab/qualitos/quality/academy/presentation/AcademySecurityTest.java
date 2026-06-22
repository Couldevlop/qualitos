package com.openlab.qualitos.quality.academy.presentation;

import com.openlab.qualitos.quality.academy.application.*;
import com.openlab.qualitos.quality.academy.domain.CourseStatus;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.config.SecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vérifie les invariants de sécurité réels de la {@link SecurityConfig} pour
 * l'Academy (OWASP A01) : la vérification publique d'un certificat est ouverte
 * sans authentification ; l'autoring est réservé aux rôles de pilotage qualité.
 */
@Tag("web")
@WebMvcTest(controllers = {
        AcademyAuthoringController.class,
        AcademyCertificateVerificationController.class})
@Import({SecurityConfig.class, AcademyExceptionHandler.class})
class AcademySecurityTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean AcademyCourseService courseService;
    @MockitoBean AcademyExportService exportService;
    @MockitoBean AcademyCertificateService certificateService;

    static final UUID COURSE = UUID.randomUUID();

    @BeforeEach
    void ctx() { TenantContext.setTenantId(UUID.randomUUID().toString()); }

    @AfterEach
    void clr() { TenantContext.clear(); }

    @Test
    void publicCertificateVerify_isReachableWithoutAuth() throws Exception {
        when(certificateService.verify("CODE-1")).thenReturn(new AcademyDto.CertificateVerification(
                "CODE-1", true, "c1", "Cours 1", 90, Instant.now(), null, "a".repeat(64), "tx", true));
        // Aucun @WithMockUser : l'endpoint doit être permitAll (sinon 401).
        mockMvc.perform(get("/api/v1/academy/public/certificates/CODE-1/verify"))
                .andExpect(status().isOk());
    }

    @Test
    void authoring_withoutAuth_is401() throws Exception {
        mockMvc.perform(get("/api/v1/academy/courses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void authoring_asPlainUser_is403() throws Exception {
        mockMvc.perform(get("/api/v1/academy/courses"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void authoring_asQualityManager_isAllowed() throws Exception {
        when(courseService.createCourse(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AcademyDto.CourseResponse(COURSE, UUID.randomUUID(), "c1", "Cours 1",
                        null, null, null, 70, 50, null, CourseStatus.DRAFT, UUID.randomUUID(),
                        Instant.now(), Instant.now()));
        mockMvc.perform(post("/api/v1/academy/courses").with(csrf())
                        .contentType("application/json")
                        .content("{\"code\":\"c1\",\"title\":\"Cours 1\"}"))
                .andExpect(status().isCreated());
    }
}
