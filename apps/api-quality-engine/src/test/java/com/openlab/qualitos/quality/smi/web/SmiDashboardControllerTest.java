package com.openlab.qualitos.quality.smi.web;

import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.smi.application.SmiDashboardService;
import com.openlab.qualitos.quality.smi.application.SmiDto;
import com.openlab.qualitos.quality.smi.domain.CoverageStatus;
import com.openlab.qualitos.quality.smi.domain.Deadline;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Le tableau de bord SMI : lecture pour tout authentifié, filtre de norme borné. */
@Tag("web")
@WebMvcTest(controllers = SmiDashboardController.class)
@Import(MethodSecurityTestConfig.class)
class SmiDashboardControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SmiDashboardService service;

    static final List<List<Integer>> VIDE = List.of(List.of(0, 0, 0, 0, 0), List.of(0, 0, 0, 0, 0),
            List.of(0, 0, 0, 0, 0), List.of(0, 0, 0, 0, 0), List.of(0, 0, 0, 0, 0));

    static SmiDto.Dashboard tableau() {
        return new SmiDto.Dashboard(
                List.of(new SmiDto.StandardRef(UUID.randomUUID(), "iso-9001", "ISO 9001")),
                "iso-9001",
                new SmiDto.Compliance(91, List.of(new SmiDto.StandardScore("iso-9001", "ISO 9001", 91))),
                new SmiDto.OverdueActions(12, 3),
                new SmiDto.MajorRisks(9, 3, 6),
                new SmiDto.RiskMatrix(57, VIDE, VIDE),
                null,
                List.of(new SmiDto.Upcoming(Deadline.Kind.CALIBRATION, UUID.randomUUID(), "PC-118",
                        "Pied à coulisse", LocalDate.of(2026, 10, 10), 2)));
    }

    @Test
    @WithMockUser(roles = "USER")
    void toutAuthentifieLitLeTableauDeBordFiltre() throws Exception {
        when(service.dashboard("iso-9001")).thenReturn(tableau());

        mockMvc.perform(get("/api/v1/smi/dashboard").param("standard", "iso-9001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selected").value("iso-9001"))
                .andExpect(jsonPath("$.compliance.global").value(91))
                .andExpect(jsonPath("$.overdueActions.critical").value(3))
                .andExpect(jsonPath("$.riskMatrix.open").value(57))
                .andExpect(jsonPath("$.thisWeek[0].kind").value("CALIBRATION"))
                .andExpect(jsonPath("$.thisWeek[0].daysLeft").value(2));
    }

    @Test
    @WithMockUser(roles = "USER")
    void laMatriceDesExigencesSeLit() throws Exception {
        when(service.requirementsMatrix()).thenReturn(new SmiDto.RequirementsMatrix(
                List.of(new SmiDto.StandardRef(UUID.randomUUID(), "iso-9001", "ISO 9001")),
                List.of(new SmiDto.ChapterRow("10", List.of("NC", "CAPA"),
                        List.of(new SmiDto.Cell("iso-9001", CoverageStatus.PARTIAL, 2, 5, "Amélioration"))))));

        mockMvc.perform(get("/api/v1/smi/requirements-matrix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].chapter").value("10"))
                .andExpect(jsonPath("$.rows[0].modules[1]").value("CAPA"))
                .andExpect(jsonPath("$.rows[0].cells[0].status").value("PARTIAL"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void unFiltreQuiNEstPasUnCodeDeNormeEstRefuse() throws Exception {
        mockMvc.perform(get("/api/v1/smi/dashboard").param("standard", "iso'; drop"))
                .andExpect(status().isBadRequest());
        verify(service, never()).dashboard(any());
    }

    @Test
    @WithAnonymousUser
    void anonymeRefuse() throws Exception {
        mockMvc.perform(get("/api/v1/smi/dashboard")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/smi/requirements-matrix")).andExpect(status().isUnauthorized());
    }
}
