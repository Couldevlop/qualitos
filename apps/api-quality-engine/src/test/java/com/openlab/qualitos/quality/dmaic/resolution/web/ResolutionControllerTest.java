package com.openlab.qualitos.quality.dmaic.resolution.web;

import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.dmaic.resolution.application.ResolutionDto;
import com.openlab.qualitos.quality.dmaic.resolution.application.ResolutionService;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ActionStatus;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionContent;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionNotFoundException;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionSettings;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionValidationException;
import com.openlab.qualitos.quality.dmaic.resolution.domain.WhyBranch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le partage des rôles sur la fiche : tout authentifié lit, l'action
 * {@code dmaic.manage} écrit ; le corps ne porte pas de tenant ; une requête
 * mal formée est refusée avant le service, une règle métier en 422.
 */
@Tag("web")
@WebMvcTest(controllers = ResolutionController.class)
@Import(MethodSecurityTestConfig.class)
class ResolutionControllerTest {

    static final UUID PROJET = UUID.randomUUID();
    static final String BASE = "/api/v1/dmaic/projects/" + PROJET + "/resolution";
    static final UUID ALICE = UUID.fromString("0b6f5c1e-1a2b-4c3d-8e9f-000000000001");

    @Autowired MockMvc mockMvc;
    @MockitoBean ResolutionService service;

    static ResolutionDto.SheetView vue() {
        ResolutionSettings d = ResolutionSettings.platformDefaults();
        return new ResolutionDto.SheetView(PROJET, "Fissures carter", "Carters fissurés", true,
                ResolutionContent.empty(), new ResolutionDto.SettingsView(d.questions(), d.failureTypes(),
                d.severities(), true),
                List.of(new ResolutionDto.PhotoView(1, "image/png", 10, "Vue", "https://s3.test/k")), Instant.EPOCH);
    }

    static String corps() {
        return "{\"clientName\":\"Acme\",\"claimNumber\":\"CL-7\","
                + "\"problemOwner\":{\"accountId\":\"" + ALICE + "\",\"name\":\"Alice Martin\"},"
                + "\"team\":[{\"accountId\":\"" + ALICE + "\",\"name\":\"Alice Martin\"}],"
                + "\"openedOn\":\"2026-10-01\",\"severity\":1,\"failureTypeCode\":\"V10\","
                + "\"answers\":{\"DEF-04\":\"YES\"},"
                + "\"whys\":[{\"branch\":\"DETECTION\",\"cause\":\"Contrôle absent\",\"whys\":[\"Pas prévu\"],"
                + "\"rootCause\":true,\"action\":\"Ajouter\"}],"
                + "\"actions\":[{\"action\":\"Changer l'outil\",\"status\":\"IN_PROGRESS\",\"percent\":40,"
                + "\"responsible\":{\"accountId\":\"" + ALICE + "\",\"name\":\"Alice Martin\"}}]}";
    }

    @Test
    @WithMockUser(roles = "USER")
    void toutAuthentifieLitLaFicheEtLeRapport() throws Exception {
        when(service.sheet(PROJET)).thenReturn(vue());
        when(service.claimReport(PROJET)).thenReturn(new byte[]{'%', 'P', 'D', 'F'});

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectTitle").value("Fissures carter"))
                .andExpect(jsonPath("$.saved").value(true))
                .andExpect(jsonPath("$.settings.questions[0].code").value("DEF-01"))
                .andExpect(jsonPath("$.settings.questions[0].topic").value("WHAT"))
                .andExpect(jsonPath("$.photos[0].url").value("https://s3.test/k"));
        mockMvc.perform(get(BASE + "/claim-report"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("rapport-client-dmaic.pdf")));
    }

    @Test
    @WithAnonymousUser
    void unAnonymeNeLitRien() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"QUALITY_MANAGER", "QUALITY_DIRECTOR", "ADMIN_TENANT", "SUPER_ADMIN"})
    void lesProfilsDePilotageEcrivent(String role) throws Exception {
        when(service.save(eq(PROJET), any())).thenReturn(vue());
        when(service.uploadPhoto(eq(PROJET), eq(2), eq("image/png"), any(), eq("Vue"), eq("p.png")))
                .thenReturn(new ResolutionDto.PhotoView(2, "image/png", 3, "Vue", "https://s3.test/k"));
        when(service.updateRemark(PROJET, 2, "Autre"))
                .thenReturn(new ResolutionDto.PhotoView(2, "image/png", 3, "Autre", "https://s3.test/k"));

        mockMvc.perform(put(BASE).with(csrf()).with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON).content(corps()))
                .andExpect(status().isOk());
        mockMvc.perform(multipart(BASE + "/photos/2")
                        .file(new MockMultipartFile("file", "p.png", "image/png", new byte[]{1, 2, 3}))
                        .param("remark", "Vue").with(csrf()).with(user("u").roles(role)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slot").value(2));
        mockMvc.perform(patch(BASE + "/photos/2").with(csrf()).with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"remark\":\"Autre\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remark").value("Autre"));
        mockMvc.perform(delete(BASE + "/photos/2").with(csrf()).with(user("u").roles(role)))
                .andExpect(status().isNoContent());
        verify(service).deletePhoto(PROJET, 2);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void laSaisieArriveTelleQuelleAuService() throws Exception {
        when(service.save(eq(PROJET), any())).thenReturn(vue());
        String avecTenant = corps().replaceFirst("\\{", "{\"tenantId\":\"" + UUID.randomUUID() + "\",");

        mockMvc.perform(put(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(avecTenant))
                .andExpect(status().isOk());

        verify(service).save(eq(PROJET), argThat(c -> c.clientName().equals("Acme")
                && c.problemOwner().accountId().equals(ALICE) && c.team().size() == 1
                && c.openedOn().equals(LocalDate.of(2026, 10, 1)) && c.severity() == 1
                && c.failureTypeCode().equals("V10") && c.answers().get("DEF-04").equals("YES")
                && c.whys().get(0).branch() == WhyBranch.DETECTION && c.whys().get(0).rootCause()
                && c.actions().get(0).status() == ActionStatus.IN_PROGRESS && c.actions().get(0).percent() == 40
                && c.actions().get(0).responsible().name().equals("Alice Martin")));
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void unCorpsMinimalSuffit() throws Exception {
        when(service.save(eq(PROJET), any())).thenReturn(vue());
        mockMvc.perform(put(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(service).save(eq(PROJET), argThat(c -> c.team().isEmpty() && c.whys().isEmpty()
                && c.actions().isEmpty() && c.problemOwner() == null));
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void unePhotoSansRemarqueEstAdmise() throws Exception {
        when(service.uploadPhoto(eq(PROJET), eq(1), eq("image/jpeg"), any(), isNull()))
                .thenReturn(new ResolutionDto.PhotoView(1, "image/jpeg", 3, null, "https://s3.test/k"));
        mockMvc.perform(multipart(BASE + "/photos/1")
                        .file(new MockMultipartFile("file", "p.jpg", "image/jpeg", new byte[]{1, 2, 3})).with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = {"USER", "AUDITOR"})
    void niUnUtilisateurNiUnAuditeurNEcrivent() throws Exception {
        mockMvc.perform(put(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corps()))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart(BASE + "/photos/1")
                        .file(new MockMultipartFile("file", "p.png", "image/png", new byte[]{1})).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(BASE + "/photos/1").with(csrf())).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void uneRequeteMalFormeeEstRefuseeAvantLeService() throws Exception {
        List<String> fautifs = List.of(
                corps().replace("\"severity\":1", "\"severity\":11"),
                corps().replace("\"percent\":40", "\"percent\":140"),
                corps().replace("\"status\":\"IN_PROGRESS\",", ""),
                corps().replace("\"branch\":\"DETECTION\",", ""),
                corps().replace("\"name\":\"Alice Martin\"}]", "\"name\":\"\"}]"),
                corps().replace("\"clientName\":\"Acme\"", "\"clientName\":\"" + "x".repeat(201) + "\""));
        for (String c : fautifs) {
            mockMvc.perform(put(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(c))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void lesRefusDuDomaineSontEnRfc7807() throws Exception {
        when(service.save(eq(PROJET), any())).thenThrow(new ResolutionValidationException("answers.DEF-04", "no"));
        when(service.sheet(PROJET)).thenThrow(new ResolutionNotFoundException("absent"));

        mockMvc.perform(put(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corps()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.field").value("answers.DEF-04"))
                .andExpect(jsonPath("$.type").value("https://qualitos.io/errors/dmaic-resolution-invalid"));
        mockMvc.perform(get(BASE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.field").doesNotExist());
    }
}
