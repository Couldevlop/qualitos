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

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
}
