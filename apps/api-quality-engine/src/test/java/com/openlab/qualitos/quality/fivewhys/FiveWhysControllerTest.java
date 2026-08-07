package com.openlab.qualitos.quality.fivewhys;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Façade HTTP des 5 Pourquoi (§3.5).
 *
 * <p>Le test porte deux choses que la couche service ne peut pas garantir seule.
 * D'abord l'aiguillage : la même URL rend une page d'analyses ou la liste ouverte
 * sur une non-conformité selon la présence du paramètre {@code ncId} — deux
 * mappings sur un même chemin, donc un vrai risque de collision. Ensuite la
 * traduction des refus : une règle de la méthode non tenue doit sortir en 409 et
 * une analyse absente en 404. Sans cela l'écran affiche « refusé » pendant que
 * l'API annonce une panne serveur, et le journal se remplit d'erreurs qui n'en
 * sont pas.
 */
@Tag("web")
@WebMvcTest(controllers = FiveWhysController.class)
class FiveWhysControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FiveWhysService service;

    private ObjectMapper objectMapper;

    private static final UUID ANALYSIS_ID = UUID.randomUUID();
    private static final UUID NC_ID = UUID.randomUUID();
    private static final UUID STEP_ID = UUID.randomUUID();

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // --- GET /five-whys ---------------------------------------------------------

    @Test
    @WithMockUser
    void list_returnsPagedAnalyses() throws Exception {
        when(service.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(analysis(null))));

        mockMvc.perform(get("/api/v1/five-whys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(ANALYSIS_ID.toString()))
                .andExpect(jsonPath("$.content[0].ncReference").value("NC-2026-014"));
    }

    @Test
    @WithMockUser
    void list_withNcId_returnsTheAnalysesOfThatNonConformity() throws Exception {
        // Même chemin, autre intention : sans cet aiguillage, ouvrir les analyses
        // d'une NC rendrait une page paginée de tout le tenant.
        when(service.findByNc(NC_ID)).thenReturn(List.of(analysis("Presse mal réglée")));

        mockMvc.perform(get("/api/v1/five-whys").param("ncId", NC_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ncId").value(NC_ID.toString()))
                .andExpect(jsonPath("$[0].rootCause").value("Presse mal réglée"));
    }

    @Test
    @WithMockUser
    void list_withMalformedNcId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/five-whys").param("ncId", "pas-un-uuid"))
                .andExpect(status().isBadRequest());
    }

    // --- GET /five-whys/{id} ----------------------------------------------------

    @Test
    @WithMockUser
    void get_returnsTheChainInOrder() throws Exception {
        when(service.get(ANALYSIS_ID)).thenReturn(new FiveWhysDto.AnalysisResponse(
                ANALYSIS_ID, NC_ID, "NC-2026-014", "Arrêt de ligne récurrent", null,
                List.of(step(1, "Le capteur s'est déclenché"), step(2, "Le convoyeur a dérivé")),
                Instant.parse("2026-08-06T10:00:00Z"), Instant.parse("2026-08-06T10:00:00Z")));

        mockMvc.perform(get("/api/v1/five-whys/{id}", ANALYSIS_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps[0].position").value(1))
                .andExpect(jsonPath("$.steps[1].answer").value("Le convoyeur a dérivé"));
    }

    @Test
    @WithMockUser
    void get_unknownAnalysis_returns404() throws Exception {
        when(service.get(ANALYSIS_ID)).thenThrow(new FiveWhysNotFoundException(ANALYSIS_ID));

        mockMvc.perform(get("/api/v1/five-whys/{id}", ANALYSIS_ID))
                .andExpect(status().isNotFound());
    }

    // --- POST /five-whys --------------------------------------------------------

    @Test
    @WithMockUser
    void create_returns201() throws Exception {
        when(service.create(any())).thenReturn(analysis(null));

        mockMvc.perform(post("/api/v1/five-whys")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FiveWhysDto.CreateRequest(NC_ID, "Arrêt de ligne récurrent"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ANALYSIS_ID.toString()));
    }

    @Test
    @WithMockUser
    void create_withoutNc_returns400() throws Exception {
        // L'analyse part d'une non-conformité : sans elle, elle ne part de rien.
        mockMvc.perform(post("/api/v1/five-whys")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problem\":\"Arrêt de ligne\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void create_onUnknownNc_returns404() throws Exception {
        when(service.create(any())).thenThrow(new FiveWhysNotFoundException(NC_ID));

        mockMvc.perform(post("/api/v1/five-whys")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FiveWhysDto.CreateRequest(NC_ID, null))))
                .andExpect(status().isNotFound());
    }

    // --- PATCH /five-whys/{id}/problem ------------------------------------------

    @Test
    @WithMockUser
    void updateProblem_returns200() throws Exception {
        when(service.updateProblem(eq(ANALYSIS_ID), any())).thenReturn(analysis(null));

        mockMvc.perform(patch("/api/v1/five-whys/{id}/problem", ANALYSIS_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FiveWhysDto.UpdateProblemRequest("Arrêt de ligne récurrent"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problem").value("Arrêt de ligne récurrent"));
    }

    @Test
    @WithMockUser
    void updateProblem_blank_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/five-whys/{id}/problem", ANALYSIS_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problem\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    // --- POST /five-whys/{id}/steps ---------------------------------------------

    @Test
    @WithMockUser
    void addStep_returns201_withItsRank() throws Exception {
        when(service.addStep(eq(ANALYSIS_ID), any())).thenReturn(step(3, "Le réglage n'était pas contrôlé"));

        mockMvc.perform(post("/api/v1/five-whys/{id}/steps", ANALYSIS_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FiveWhysDto.AddStepRequest("Le réglage n'était pas contrôlé"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(3));
    }

    @Test
    @WithMockUser
    void addStep_blankAnswer_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/five-whys/{id}/steps", ANALYSIS_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void addStep_beyondTheSeventhWhy_returns409() throws Exception {
        // Au-delà de sept, on n'remonte plus une cause : le refus est un conflit
        // d'état, pas une panne — et le message doit rester lisible côté écran.
        when(service.addStep(eq(ANALYSIS_ID), any())).thenThrow(new FiveWhysStateException(
                "Au-delà de 7 pourquoi, on n'remonte plus une cause : "
                        + "conclure la cause racine ou reformuler le problème"));

        mockMvc.perform(post("/api/v1/five-whys/{id}/steps", ANALYSIS_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FiveWhysDto.AddStepRequest("Un huitième pourquoi"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("7 pourquoi")));
    }

    // --- PATCH /five-whys/steps/{stepId} ----------------------------------------

    @Test
    @WithMockUser
    void updateStep_returns200() throws Exception {
        when(service.updateStep(eq(STEP_ID), any())).thenReturn(step(2, "Formulation corrigée"));

        mockMvc.perform(patch("/api/v1/five-whys/steps/{stepId}", STEP_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FiveWhysDto.AddStepRequest("Formulation corrigée"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Formulation corrigée"));
    }

    @Test
    @WithMockUser
    void updateStep_unknownStep_returns404() throws Exception {
        when(service.updateStep(eq(STEP_ID), any())).thenThrow(new FiveWhysNotFoundException(STEP_ID));

        mockMvc.perform(patch("/api/v1/five-whys/steps/{stepId}", STEP_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FiveWhysDto.AddStepRequest("Peu importe"))))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /five-whys/steps/{stepId} ---------------------------------------

    @Test
    @WithMockUser
    void deleteStep_returns204() throws Exception {
        doNothing().when(service).deleteStep(STEP_ID);

        mockMvc.perform(delete("/api/v1/five-whys/steps/{stepId}", STEP_ID).with(csrf()))
                .andExpect(status().isNoContent());

        verify(service).deleteStep(STEP_ID);
    }

    @Test
    @WithMockUser
    void deleteStep_whenNotTheLastLink_returns409() throws Exception {
        doThrow(new FiveWhysStateException(
                "Seul le dernier pourquoi peut être retiré : la chaîne se lit dans l'ordre"))
                .when(service).deleteStep(STEP_ID);

        mockMvc.perform(delete("/api/v1/five-whys/steps/{stepId}", STEP_ID).with(csrf()))
                .andExpect(status().isConflict());
    }

    // --- PUT /five-whys/{id}/root-cause -----------------------------------------

    @Test
    @WithMockUser
    void setRootCause_returns200() throws Exception {
        when(service.setRootCause(eq(ANALYSIS_ID), any())).thenReturn(analysis("Presse mal réglée"));

        mockMvc.perform(put("/api/v1/five-whys/{id}/root-cause", ANALYSIS_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FiveWhysDto.RootCauseRequest("Presse mal réglée"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rootCause").value("Presse mal réglée"));
    }

    @Test
    @WithMockUser
    void setRootCause_blank_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/five-whys/{id}/root-cause", ANALYSIS_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rootCause\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void setRootCause_tooEarlyInTheChain_returns409() throws Exception {
        when(service.setRootCause(eq(ANALYSIS_ID), any())).thenThrow(new FiveWhysStateException(
                "Conclure avant 3 pourquoi, c'est nommer un symptôme"));

        mockMvc.perform(put("/api/v1/five-whys/{id}/root-cause", ANALYSIS_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FiveWhysDto.RootCauseRequest("Un symptôme déguisé"))))
                .andExpect(status().isConflict());
    }

    // --- DELETE /five-whys/{id} -------------------------------------------------

    @Test
    @WithMockUser
    void delete_returns204() throws Exception {
        doNothing().when(service).delete(ANALYSIS_ID);

        mockMvc.perform(delete("/api/v1/five-whys/{id}", ANALYSIS_ID).with(csrf()))
                .andExpect(status().isNoContent());

        verify(service).delete(ANALYSIS_ID);
    }

    @Test
    @WithMockUser
    void delete_unknownAnalysis_returns404() throws Exception {
        doThrow(new FiveWhysNotFoundException(ANALYSIS_ID)).when(service).delete(ANALYSIS_ID);

        mockMvc.perform(delete("/api/v1/five-whys/{id}", ANALYSIS_ID).with(csrf()))
                .andExpect(status().isNotFound());
    }

    // --- fixtures ---------------------------------------------------------------

    private static FiveWhysDto.AnalysisResponse analysis(String rootCause) {
        return new FiveWhysDto.AnalysisResponse(
                ANALYSIS_ID, NC_ID, "NC-2026-014", "Arrêt de ligne récurrent", rootCause,
                List.of(), Instant.parse("2026-08-06T10:00:00Z"), Instant.parse("2026-08-06T10:00:00Z"));
    }

    private static FiveWhysDto.StepResponse step(int position, String answer) {
        return new FiveWhysDto.StepResponse(
                STEP_ID, position, answer,
                Instant.parse("2026-08-06T10:00:00Z"), Instant.parse("2026-08-06T10:00:00Z"));
    }
}
