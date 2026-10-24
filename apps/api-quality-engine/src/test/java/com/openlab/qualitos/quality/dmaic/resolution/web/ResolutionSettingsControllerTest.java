package com.openlab.qualitos.quality.dmaic.resolution.web;

import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.dmaic.resolution.application.ResolutionDto;
import com.openlab.qualitos.quality.dmaic.resolution.application.ResolutionService;
import com.openlab.qualitos.quality.dmaic.resolution.domain.AnswerType;
import com.openlab.qualitos.quality.dmaic.resolution.domain.DefineTopic;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionSettings;
import com.openlab.qualitos.quality.dmaic.resolution.domain.SheetSection;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le paramétrage se lit par tout authentifié et ne se change que par
 * l'administrateur du client — pas même par le directeur qualité.
 */
@Tag("web")
@WebMvcTest(controllers = ResolutionSettingsController.class)
@Import(MethodSecurityTestConfig.class)
class ResolutionSettingsControllerTest {

    static final String BASE = "/api/v1/dmaic/resolution-settings";

    @Autowired MockMvc mockMvc;
    @MockitoBean ResolutionService service;

    static ResolutionDto.SettingsView defauts() {
        ResolutionSettings d = ResolutionSettings.platformDefaults();
        return new ResolutionDto.SettingsView(d.questions(), d.failureTypes(), d.severities(), true);
    }

    static final String CORPS = "{\"questions\":[{\"code\":\"Q1\",\"section\":\"DEFINE\",\"topic\":\"WHO\","
            + "\"label\":\"Qui ?\",\"answerType\":\"TEXT\"}],"
            + "\"failureTypes\":[{\"code\":\"V1\",\"label\":\"Rayure\"}],"
            + "\"severities\":[{\"level\":1,\"label\":\"Grave\"}]}";

    @Test
    @WithMockUser(roles = "USER")
    void toutAuthentifieLitLeParametrage() throws Exception {
        when(service.settings()).thenReturn(defauts());
        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaults").value(true))
                .andExpect(jsonPath("$.failureTypes[0].code").value("V01"))
                .andExpect(jsonPath("$.severities[0].label").value("Sévère"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void lAdministrateurRemplaceEtRetablit() throws Exception {
        when(service.replaceSettings(anyList(), anyList(), anyList())).thenReturn(defauts());
        when(service.resetSettings()).thenReturn(defauts());

        mockMvc.perform(put(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isOk());
        mockMvc.perform(post(BASE + "/reset").with(csrf())).andExpect(status().isOk());

        verify(service).replaceSettings(
                argThat(q -> q.size() == 1 && q.get(0).code().equals("Q1") && q.get(0).topic() == DefineTopic.WHO
                        && q.get(0).section() == SheetSection.DEFINE && q.get(0).answerType() == AnswerType.TEXT),
                argThat(f -> f.size() == 1 && f.get(0).label().equals("Rayure")),
                argThat(s -> s.size() == 1 && s.get(0).level() == 1));
        verify(service).resetSettings();
    }

    @Test
    @WithMockUser(roles = {"QUALITY_DIRECTOR", "QUALITY_MANAGER"})
    void lePilotageNeParametrePas() throws Exception {
        mockMvc.perform(put(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/reset").with(csrf())).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void unParametrageMalFormeEstRefuseAvantLeService() throws Exception {
        for (String c : List.of(
                CORPS.replace("\"questions\":[{", "\"questions\":[],\"x\":[{"),
                CORPS.replace("\"severities\":[{\"level\":1,\"label\":\"Grave\"}]", "\"severities\":[]"),
                CORPS.replace("\"answerType\":\"TEXT\"", "\"answerType\":null"),
                CORPS.replace("\"level\":1", "\"level\":0"),
                CORPS.replace("\"label\":\"Rayure\"", "\"label\":\"\""))) {
            mockMvc.perform(put(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(c))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
}
