package com.openlab.qualitos.quality.standards;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.docs.DocumentNotFoundException;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La création d'un référentiel MODIFIE le catalogue : elle est réservée aux rôles
 * qui pilotent le système qualité (OWASP A01), et chaque refus du service doit
 * arriver au client avec un statut qui dit ce qui s'est passé.
 */
@Tag("web")
@WebMvcTest(controllers = ProcedureStandardController.class)
@Import(MethodSecurityTestConfig.class)
class ProcedureStandardControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ProcedureStandardService service;
    ObjectMapper om;

    static final UUID DOC = UUID.randomUUID();
    static final UUID STD = UUID.randomUUID();
    /** Nœud de l'arborescence : section, clause ou exigence selon la route. */
    static final UUID CHILD = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper();
    }

    private String body() throws Exception {
        return om.writeValueAsString(Map.of("documentId", DOC));
    }

    private Standard created() {
        Standard s = new Standard();
        s.setId(STD);
        return s;
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void createsTheReferentialAndPointsToIt() throws Exception {
        when(service.createFromDocument(DOC)).thenReturn(created());

        mockMvc.perform(post("/api/v1/standards/from-document")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(STD.toString())));
    }

    @Test
    @WithMockUser(roles = "USER")
    void refusesAUserWhoDoesNotPilotTheQualitySystem() throws Exception {
        mockMvc.perform(post("/api/v1/standards/from-document")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isForbidden());

        verify(service, never()).createFromDocument(DOC);
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void reportsAnIneligibleDocumentAsUnprocessable() throws Exception {
        when(service.createFromDocument(DOC))
                .thenThrow(new ProcedureSourceException("Seule une procédure peut servir de référentiel d'audit"));

        mockMvc.perform(post("/api/v1/standards/from-document")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "DIRECTOR_QUALITY")
    void reportsAnExistingReferentialAsConflict() throws Exception {
        when(service.createFromDocument(DOC))
                .thenThrow(new StandardCodeConflictException("Un référentiel existe déjà pour cette procédure"));

        mockMvc.perform(post("/api/v1/standards/from-document")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isConflict());
    }

    /** Le document d'un autre tenant est traité comme absent, pas comme interdit. */
    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void reportsAnUnknownDocumentAsNotFound() throws Exception {
        when(service.createFromDocument(DOC)).thenThrow(new DocumentNotFoundException(DOC));

        mockMvc.perform(post("/api/v1/standards/from-document")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void rejectsARequestWithoutASourceDocument() throws Exception {
        mockMvc.perform(post("/api/v1/standards/from-document")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).createFromDocument(DOC);
    }

    // ---- Suppression du référentiel ----

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void deletesAReferential() throws Exception {
        mockMvc.perform(delete("/api/v1/standards/{id}", STD).with(csrf()))
                .andExpect(status().isNoContent());

        verify(service).deleteStandard(STD);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void reportsAFollowedReferentialAsConflict() throws Exception {
        doThrow(new AdoptionConflictException("suivi par un projet de conformité"))
                .when(service).deleteStandard(STD);

        mockMvc.perform(delete("/api/v1/standards/{id}", STD).with(csrf()))
                .andExpect(status().isConflict());
    }

    // ---- Arborescence ----

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void routesEveryTreeOperationToItsService() throws Exception {
        String section = om.writeValueAsString(Map.of("code", "1", "title", "Programmation"));
        String clause = om.writeValueAsString(Map.of("code", "1.1", "title", "Fréquence"));
        String requirement = om.writeValueAsString(Map.of(
                "code", "1.1.1", "text", "Le programme est revu chaque année", "obligation", "MUST"));

        perform(post("/api/v1/standards/{id}/sections", STD), section);
        perform(patch("/api/v1/standards/{id}/sections/{sid}", STD, CHILD), section);
        perform(delete("/api/v1/standards/{id}/sections/{sid}", STD, CHILD), null);
        perform(post("/api/v1/standards/{id}/sections/{sid}/clauses", STD, CHILD), clause);
        perform(patch("/api/v1/standards/{id}/clauses/{cid}", STD, CHILD), clause);
        perform(delete("/api/v1/standards/{id}/clauses/{cid}", STD, CHILD), null);
        perform(post("/api/v1/standards/{id}/clauses/{cid}/requirements", STD, CHILD), requirement);
        perform(patch("/api/v1/standards/{id}/requirements/{rid}", STD, CHILD), requirement);
        perform(delete("/api/v1/standards/{id}/requirements/{rid}", STD, CHILD), null);

        verify(service).addSection(eq(STD), any());
        verify(service).updateSection(eq(STD), eq(CHILD), any());
        verify(service).deleteSection(STD, CHILD);
        verify(service).addClause(eq(STD), eq(CHILD), any());
        verify(service).updateClause(eq(STD), eq(CHILD), any());
        verify(service).deleteClause(STD, CHILD);
        verify(service).addRequirement(eq(STD), eq(CHILD), any());
        verify(service).updateRequirement(eq(STD), eq(CHILD), any());
        verify(service).deleteRequirement(STD, CHILD);
    }

    private void perform(MockHttpServletRequestBuilder request, String body) throws Exception {
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        mockMvc.perform(request.with(csrf())).andExpect(status().isNoContent());
    }

    /**
     * La lecture du catalogue est ouverte à tout utilisateur authentifié ; son
     * ÉCRITURE ne l'est pas. Le contrôleur porte la restriction au niveau de la
     * classe : une route ajoutée plus tard en hérite, plutôt que de dépendre du
     * fait qu'on ait pensé à l'annoter.
     */
    @Test
    @WithMockUser(roles = "USER")
    void closesTheWholeTreeApiToAUserWhoDoesNotPilotTheQualitySystem() throws Exception {
        mockMvc.perform(post("/api/v1/standards/{id}/sections", STD).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("code", "1", "title", "X"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/standards/{id}/requirements/{rid}", STD, CHILD).with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void refusesToEditAPlatformStandard() throws Exception {
        doThrow(new PlatformStandardWriteException())
                .when(service).addSection(eq(STD), any());

        mockMvc.perform(post("/api/v1/standards/{id}/sections", STD).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("code", "1", "title", "X"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void reportsAnUnknownSectionAsNotFound() throws Exception {
        doThrow(new SectionNotFoundException(CHILD)).when(service).deleteSection(STD, CHILD);

        mockMvc.perform(delete("/api/v1/standards/{id}/sections/{sid}", STD, CHILD).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void reportsAnUnknownClauseAsNotFound() throws Exception {
        doThrow(new ClauseNotFoundException(CHILD)).when(service).deleteClause(STD, CHILD);

        mockMvc.perform(delete("/api/v1/standards/{id}/clauses/{cid}", STD, CHILD).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void rejectsASectionWithoutACode() throws Exception {
        mockMvc.perform(post("/api/v1/standards/{id}/sections", STD).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("title", "Sans code"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    /** Le code de section tient dans sa colonne : 20 caractères, pas un de plus. */
    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void rejectsASectionCodeLongerThanItsColumn() throws Exception {
        mockMvc.perform(post("/api/v1/standards/{id}/sections", STD).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "code", "1".repeat(21), "title", "Trop long"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
