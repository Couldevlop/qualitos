package com.openlab.qualitos.quality.training.competencymatrix.web;

import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.training.competencymatrix.application.CompetencyMatrixService;
import com.openlab.qualitos.quality.training.competencymatrix.domain.CompetencyGrid;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * L'API de la matrice de compétences.
 *
 * <p>Le point qui compte ici : une case jamais évaluée doit sortir en
 * {@code null} dans le JSON, et non en zéro. La sérialisation est le dernier
 * endroit où la distinction peut se perdre — et la perdre transformerait « on ne
 * sait pas » en « niveau nul, constaté ».
 */
@Tag("web")
@WebMvcTest(controllers = CompetencyMatrixController.class)
@Import(MethodSecurityTestConfig.class)
class CompetencyMatrixControllerTest {

    static final String URL = "/api/v1/training/competencies/matrix";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CompetencyMatrixService service;

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void itRendersTheGroupsItsPeopleAndItsLevels() throws Exception {
        when(service.grid()).thenReturn(grid());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.people[0].label").value("Anna"))
                .andExpect(jsonPath("$.people[1].label").value("Boris"))
                .andExpect(jsonPath("$.groups[0].category").value("Gestion de projet"))
                .andExpect(jsonPath("$.groups[0].rows[0].name").value("Planification"))
                .andExpect(jsonPath("$.groups[0].rows[0].levels[0]").value(4));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void aCellNeverAssessedIsRenderedAsNullAndNotAsZero() throws Exception {
        when(service.grid()).thenReturn(grid());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups[0].rows[0].levels[1]").doesNotExist())
                .andExpect(jsonPath("$.groups[0].rows[0].singlePointOfKnowledge").value(true));
    }

    @Test
    @WithAnonymousUser
    void anAnonymousCallerGetsNothing() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void anEmptyGridIsAValidAnswer() throws Exception {
        when(service.grid()).thenReturn(new CompetencyGrid(List.of(), List.of()));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups").isEmpty())
                .andExpect(jsonPath("$.people").isEmpty());
    }

    private CompetencyGrid grid() {
        CompetencyGrid.Row row = new CompetencyGrid.Row(UUID.randomUUID(), "PLAN", "Planification",
                Arrays.asList(4, null), 1, true);
        return new CompetencyGrid(
                List.of(new CompetencyGrid.Person(UUID.randomUUID(), "Anna"),
                        new CompetencyGrid.Person(UUID.randomUUID(), "Boris")),
                List.of(new CompetencyGrid.Group("Gestion de projet", List.of(row))));
    }
}
