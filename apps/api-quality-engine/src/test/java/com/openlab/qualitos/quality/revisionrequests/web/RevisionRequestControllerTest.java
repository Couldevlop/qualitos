package com.openlab.qualitos.quality.revisionrequests.web;

import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.common.StepUpGuard;
import com.openlab.qualitos.quality.common.StepUpRequiredException;
import com.openlab.qualitos.quality.revisionrequests.application.RevisionRequestDto;
import com.openlab.qualitos.quality.revisionrequests.application.RevisionRequestService;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequestNotFoundException;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequestStateException;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequestStatus;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionTargetType;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionTriggerType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Lire les propositions est ouvert ; trancher ne l'est pas. Et refuser sans note
 * est refusé à la frontière : le domaine lèverait bien une exception, mais elle
 * ressortirait en 500 au lieu d'un 400 qui dit ce qui manque.
 */
@Tag("web")
@WebMvcTest(controllers = RevisionRequestController.class)
@Import(MethodSecurityTestConfig.class)
class RevisionRequestControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean RevisionRequestService service;
    /** Doublée ici : ce que la garde lit du jeton est vérifié par ses propres tests. */
    @MockitoBean StepUpGuard stepUp;

    static final UUID PRODUCT = UUID.randomUUID();
    static final UUID REQUEST = UUID.randomUUID();
    static final UUID TRIGGER = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-08-19T08:00:00Z");

    static final RevisionRequestDto.View VIEW = new RevisionRequestDto.View(
            REQUEST, PRODUCT, RevisionTargetType.PFMEA_ITEM, UUID.randomUUID(),
            RevisionTriggerType.NC_CREATED, TRIGGER, "NC-2026-0143",
            "3 NC en 12 mois", "occurrence", "4", "6", null,
            RevisionRequestStatus.PENDING, null, null, null, NOW, NOW);

    @Test @WithMockUser(roles = "USER")
    void anyAuthenticatedUserReadsTheProposalsOfAProduct() throws Exception {
        when(service.pendingForProduct(PRODUCT)).thenReturn(List.of(VIEW));

        mockMvc.perform(get("/api/v1/products/{p}/revision-requests", PRODUCT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].field").value("occurrence"))
                .andExpect(jsonPath("$[0].to").value("6"));
    }

    @Test @WithMockUser(roles = "USER")
    void theProposalsOfOneTriggerAreReadableToo() throws Exception {
        when(service.forTrigger(TRIGGER)).thenReturn(List.of(VIEW));

        mockMvc.perform(get("/api/v1/revision-requests").param("triggerRefId", TRIGGER.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].triggerRefLabel").value("NC-2026-0143"));
    }

    @Test @WithMockUser(roles = "USER")
    void aMissingTriggerParameterAnswers400() throws Exception {
        mockMvc.perform(get("/api/v1/revision-requests"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "USER")
    void aSimpleUserCannotDecide() throws Exception {
        mockMvc.perform(post("/api/v1/revision-requests/{id}/accept", REQUEST).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void theQualityManagerAccepts() throws Exception {
        when(service.accept(REQUEST)).thenReturn(VIEW);

        mockMvc.perform(post("/api/v1/revision-requests/{id}/accept", REQUEST).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(REQUEST.toString()));

        verify(stepUp).require("accepter une proposition de révision");
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void rejectingWithoutANoteAnswers400() throws Exception {
        mockMvc.perform(post("/api/v1/revision-requests/{id}/reject", REQUEST).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"   \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void acceptingWithoutASecondFactorAnswers403() throws Exception {
        // Accepter écrit dans un document approuvé : le rôle ne suffit pas.
        doThrow(new StepUpRequiredException("accepter une proposition de révision"))
                .when(stepUp).require(any());

        mockMvc.perform(post("/api/v1/revision-requests/{id}/accept", REQUEST).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://qualitos.io/errors/step-up-required"));

        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void rejectingNeverAsksForASecondFactor() throws Exception {
        // Un refus ne modifie aucun document : il consigne une décision. Lui
        // imposer un second facteur découragerait de motiver les refus.
        when(service.reject(eq(REQUEST), any())).thenReturn(VIEW);

        mockMvc.perform(post("/api/v1/revision-requests/{id}/reject", REQUEST).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Cotation revue le 12/08\"}"))
                .andExpect(status().isOk());

        verifyNoInteractions(stepUp);
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void rejectingWithANoteGoesThrough() throws Exception {
        when(service.reject(eq(REQUEST), any())).thenReturn(VIEW);

        mockMvc.perform(post("/api/v1/revision-requests/{id}/reject", REQUEST).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Cotation revue le 12/08\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void anUnknownProposalAnswers404() throws Exception {
        when(service.accept(REQUEST)).thenThrow(new RevisionRequestNotFoundException(REQUEST));

        mockMvc.perform(post("/api/v1/revision-requests/{id}/accept", REQUEST).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void anAlreadyDecidedProposalAnswers409() throws Exception {
        when(service.accept(REQUEST))
                .thenThrow(new RevisionRequestStateException("Cette demande est déjà ACCEPTED"));

        mockMvc.perform(post("/api/v1/revision-requests/{id}/accept", REQUEST).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithAnonymousUser
    void anAnonymousVisitorReadsNothing() throws Exception {
        mockMvc.perform(get("/api/v1/products/{p}/revision-requests", PRODUCT))
                .andExpect(status().is4xxClientError());
    }
}
