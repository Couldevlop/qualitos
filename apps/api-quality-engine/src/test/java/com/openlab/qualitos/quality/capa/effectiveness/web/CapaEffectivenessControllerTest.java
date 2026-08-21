package com.openlab.qualitos.quality.capa.effectiveness.web;

import com.openlab.qualitos.quality.capa.effectiveness.application.CapaEffectivenessDto;
import com.openlab.qualitos.quality.capa.effectiveness.application.CapaEffectivenessService;
import com.openlab.qualitos.quality.capa.effectiveness.domain.MeasurementStatus;
import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * L'API de l'efficacité CAPA.
 *
 * <p>Lecture ouverte à tout utilisateur authentifié du tenant : elle ne révèle
 * rien que la liste des CAPA ne révèle déjà. Ce que le banc protège, c'est la
 * borne de la fenêtre — un paramètre de requête finit toujours par recevoir une
 * valeur absurde — et le fait qu'un anonyme n'obtienne rien.
 */
@Tag("web")
@WebMvcTest(controllers = CapaEffectivenessController.class)
@Import(MethodSecurityTestConfig.class)
class CapaEffectivenessControllerTest {

    static final String URL = "/api/v1/capa/effectiveness";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CapaEffectivenessService service;

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void itRendersTheSummaryAndItsRows() throws Exception {
        when(service.measure(6)).thenReturn(summary());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.windowMonths").value(6))
                .andExpect(jsonPath("$.averageRatePercent").value(71))
                .andExpect(jsonPath("$.rows[0].title").value("Dérive dimensionnelle"))
                .andExpect(jsonPath("$.rows[0].ratePercent").value(92))
                .andExpect(jsonPath("$.rows[0].status").value("MEASURED"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void sixMonthsIsTheDefaultWindow() throws Exception {
        when(service.measure(6)).thenReturn(summary());

        mockMvc.perform(get(URL)).andExpect(status().isOk());

        verify(service).measure(6);
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void anotherWindowIsHonoured() throws Exception {
        when(service.measure(12)).thenReturn(summary());

        mockMvc.perform(get(URL).param("months", "12")).andExpect(status().isOk());

        verify(service).measure(12);
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void anAbsurdWindowIsRefusedWithoutReachingTheService() throws Exception {
        mockMvc.perform(get(URL).param("months", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL).param("months", "99"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @WithAnonymousUser
    void anAnonymousCallerGetsNothing() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void anEmptySummaryIsStillAValidAnswer() throws Exception {
        when(service.measure(anyInt())).thenReturn(
                new CapaEffectivenessDto.Summary(6, 0, 0, 0, null, 0, 0, List.of()));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows").isEmpty())
                .andExpect(jsonPath("$.averageRatePercent").doesNotExist());
    }

    private CapaEffectivenessDto.Summary summary() {
        CapaEffectivenessDto.Row row = new CapaEffectivenessDto.Row(
                UUID.randomUUID(), "Dérive dimensionnelle", "MAJOR",
                Instant.parse("2026-03-12T09:00:00Z"), MeasurementStatus.MEASURED,
                12, 1, 92, false, 180, 180, Boolean.TRUE, true);
        return new CapaEffectivenessDto.Summary(6, 1, 0, 0, 71, 0, 0, List.of(row));
    }
}
