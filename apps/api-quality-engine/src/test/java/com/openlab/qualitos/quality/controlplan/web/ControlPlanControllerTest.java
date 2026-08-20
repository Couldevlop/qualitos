package com.openlab.qualitos.quality.controlplan.web;

import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.common.StepUpGuard;
import com.openlab.qualitos.quality.common.StepUpRequiredException;
import com.openlab.qualitos.quality.controlplan.application.ControlPlanDto;
import com.openlab.qualitos.quality.controlplan.application.ControlPlanService;
import com.openlab.qualitos.quality.controlplan.domain.CharacteristicType;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanNotFoundException;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanPhase;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanStateException;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * L'approbation engage l'organisation devant l'auditeur : elle est réservée à la
 * direction qualité, alors que le reste de l'édition revient au manager qualité.
 * Le cas exigé par le plan — un {@code QUALITY_MANAGER} qui ajoute une ligne mais
 * reçoit 403 en approbation — est vérifié ci-dessous.
 */
@Tag("web")
@WebMvcTest(controllers = ControlPlanController.class)
@Import(MethodSecurityTestConfig.class)
class ControlPlanControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ControlPlanService service;
    /**
     * La garde est doublée ici : ce banc vérifie le partage des rôles et le
     * mapping HTTP. Ce que la garde lit dans le jeton est vérifié par
     * {@code StepUpAuthenticationTest} et prouvé bout en bout par
     * {@code StepUpEndpointTest}.
     */
    @MockitoBean StepUpGuard stepUp;

    static final UUID PRODUCT = UUID.randomUUID();
    static final UUID PLAN = UUID.randomUUID();
    static final UUID LINE = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-08-19T08:00:00Z");

    static final ControlPlanDto.View VIEW = new ControlPlanDto.View(
            PLAN, PRODUCT, ControlPlanPhase.PRODUCTION, "CP-4471", 1,
            ControlPlanStatus.DRAFT, null, null, null, NOW, NOW);
    static final ControlPlanDto.LineView LINE_VIEW = new ControlPlanDto.LineView(
            LINE, 10, null, "Tour CN 3", "12", "Diamètre", CharacteristicType.PRODUCT,
            null, "Ø 20", null, null, "mm", "Micromètre", 5, "1/h", "Carte X-R",
            "Tri à 100 %", null);

    private static final String LINE_BODY = """
            {"sequenceNo":10,"characteristicLabel":"Diamètre","characteristicType":"PRODUCT"}
            """;

    // ---------- le partage des rôles ----------

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void aQualityManagerCanAddALineButNotApproveThePlan() throws Exception {
        when(service.addLine(eq(PRODUCT), eq(PLAN), any())).thenReturn(LINE_VIEW);

        mockMvc.perform(post("/api/v1/products/{p}/control-plans/{c}/lines", PRODUCT, PLAN).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(LINE_BODY))
                .andExpect(status().isCreated())
                // On vérifie un champ sans accent : selon l'encodage de réponse retenu
                // par MockMvc, une comparaison accentuée échouerait pour une raison
                // qui n'a rien à voir avec ce que ce test prétend vérifier.
                .andExpect(jsonPath("$.sequenceNo").value(10));

        mockMvc.perform(post("/api/v1/products/{p}/control-plans/{c}/approve", PRODUCT, PLAN).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DIRECTOR_QUALITY")
    void theQualityDirectorApprovesThePlan() throws Exception {
        when(service.approve(PRODUCT, PLAN)).thenReturn(VIEW);

        mockMvc.perform(post("/api/v1/products/{p}/control-plans/{c}/approve", PRODUCT, PLAN).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CP-4471"));

        verify(stepUp).require("approuver un control plan");
    }

    @Test
    @WithMockUser(roles = "DIRECTOR_QUALITY")
    void approvingWithoutASecondFactorAnswers403AndSaysWhatIsMissing() throws Exception {
        // Le rôle suffit à ouvrir la porte, pas à signer : le jeton doit porter
        // la trace d'un second facteur (règle 18.2 §5).
        doThrow(new StepUpRequiredException("approuver un control plan"))
                .when(stepUp).require(any());

        mockMvc.perform(post("/api/v1/products/{p}/control-plans/{c}/approve", PRODUCT, PLAN).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://qualitos.io/errors/step-up-required"))
                .andExpect(jsonPath("$.action").value("approuver un control plan"));

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void editingALineNeverAsksForASecondFactor() throws Exception {
        // Exiger le second facteur sur chaque saisie le viderait de son sens :
        // c'est la signature qui engage, pas la frappe.
        when(service.addLine(eq(PRODUCT), eq(PLAN), any())).thenReturn(LINE_VIEW);

        mockMvc.perform(post("/api/v1/products/{p}/control-plans/{c}/lines", PRODUCT, PLAN).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(LINE_BODY))
                .andExpect(status().isCreated());

        verifyNoInteractions(stepUp);
    }

    @Test
    @WithMockUser(roles = "USER")
    void aSimpleUserCannotCreateAPlan() throws Exception {
        mockMvc.perform(post("/api/v1/products/{p}/control-plans", PRODUCT).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phase\":\"PRODUCTION\",\"code\":\"CP-1\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithAnonymousUser
    void anAnonymousVisitorReadsNothing() throws Exception {
        mockMvc.perform(get("/api/v1/products/{p}/control-plans", PRODUCT))
                .andExpect(status().is4xxClientError());
    }

    // ---------- lecture ----------

    @Test @WithMockUser(roles = "USER")
    void listingIsOpenToAnyAuthenticatedUser() throws Exception {
        when(service.listForProduct(PRODUCT)).thenReturn(List.of(VIEW));

        mockMvc.perform(get("/api/v1/products/{p}/control-plans", PRODUCT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].phase").value("PRODUCTION"));
    }

    @Test @WithMockUser(roles = "USER")
    void readingOnePlanReturnsItsLines() throws Exception {
        when(service.get(PRODUCT, PLAN))
                .thenReturn(new ControlPlanDto.Detail(VIEW, List.of(LINE_VIEW)));

        mockMvc.perform(get("/api/v1/products/{p}/control-plans/{c}", PRODUCT, PLAN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].machine").value("Tour CN 3"));

        mockMvc.perform(get("/api/v1/products/{p}/control-plans/{c}/lines", PRODUCT, PLAN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sampleFrequency").value("1/h"));
    }

    @Test @WithMockUser(roles = "USER")
    void anUnknownPlanAnswers404() throws Exception {
        when(service.get(PRODUCT, PLAN)).thenThrow(new ControlPlanNotFoundException(PLAN));

        mockMvc.perform(get("/api/v1/products/{p}/control-plans/{c}", PRODUCT, PLAN))
                .andExpect(status().isNotFound());
    }

    // ---------- écriture ----------

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void creatingADraftAnswers201() throws Exception {
        when(service.createDraft(eq(PRODUCT), any())).thenReturn(VIEW);

        mockMvc.perform(post("/api/v1/products/{p}/control-plans", PRODUCT).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phase\":\"PRODUCTION\",\"code\":\"CP-4471\"}"))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void anInvalidCodeIsRefusedBeforeReachingTheService() throws Exception {
        mockMvc.perform(post("/api/v1/products/{p}/control-plans", PRODUCT).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phase\":\"PRODUCTION\",\"code\":\"cp 4471/A\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void aLineWithoutACharacteristicIsRefusedBeforeReachingTheService() throws Exception {
        mockMvc.perform(post("/api/v1/products/{p}/control-plans/{c}/lines", PRODUCT, PLAN).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sequenceNo\":10,\"characteristicType\":\"PRODUCT\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void openingARevisionAnswers201() throws Exception {
        when(service.openRevision(PRODUCT, PLAN)).thenReturn(VIEW);

        mockMvc.perform(post("/api/v1/products/{p}/control-plans/{c}/revision", PRODUCT, PLAN).with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void writingOnAnApprovedPlanAnswers409() throws Exception {
        when(service.addLine(eq(PRODUCT), eq(PLAN), any()))
                .thenThrow(new ControlPlanStateException("Le plan CP-4471 est ACTIVE"));

        mockMvc.perform(post("/api/v1/products/{p}/control-plans/{c}/lines", PRODUCT, PLAN).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(LINE_BODY))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void updatingAndDeletingALineGoThrough() throws Exception {
        when(service.updateLine(eq(PRODUCT), eq(PLAN), eq(LINE), any())).thenReturn(LINE_VIEW);

        mockMvc.perform(put("/api/v1/products/{p}/control-plans/{c}/lines/{l}", PRODUCT, PLAN, LINE)
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(LINE_BODY))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/products/{p}/control-plans/{c}/lines/{l}", PRODUCT, PLAN, LINE)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void deletingALineOfAnotherPlanAnswers404() throws Exception {
        doThrow(new ControlPlanNotFoundException(LINE))
                .when(service).deleteLine(PRODUCT, PLAN, LINE);

        mockMvc.perform(delete("/api/v1/products/{p}/control-plans/{c}/lines/{l}", PRODUCT, PLAN, LINE)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}
