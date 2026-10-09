package com.openlab.qualitos.quality.circuit.web;

import com.openlab.qualitos.quality.authz.application.AuthorizationService;
import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.circuit.application.CircuitDto;
import com.openlab.qualitos.quality.circuit.application.CircuitService;
import com.openlab.qualitos.quality.circuit.domain.CircuitException;
import com.openlab.qualitos.quality.circuit.domain.CircuitSubject;
import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Les circuits : seul « authz.manage » les règle, tout membre voit où en est un objet. */
@Tag("web")
@WebMvcTest(controllers = CircuitController.class)
@Import(MethodSecurityTestConfig.class)
class CircuitControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CircuitService service;
    @MockitoBean AuthorizationService authorization;

    static final String BASE = "/api/v1/circuits/document-version";
    static final String CORPS = "{\"steps\":[{\"name\":\"Relecture\",\"roleCode\":\"QUALITY_MANAGER\","
            + "\"minApprovals\":2},{\"name\":\"Signature\",\"roleCode\":\"QUALITY_DIRECTOR\",\"minApprovals\":1}]}";

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void lAdministrateurLitEtRegleLeCircuit() throws Exception {
        when(authorization.has(Permission.AUTHZ_MANAGE)).thenReturn(true);
        CircuitDto.CircuitView vue = new CircuitDto.CircuitView("document-version", List.of(
                new CircuitDto.StepView("Relecture", "QUALITY_MANAGER", 2),
                new CircuitDto.StepView("Signature", "QUALITY_DIRECTOR", 1)));
        when(service.circuit(CircuitSubject.DOCUMENT_VERSION)).thenReturn(vue);
        when(service.replace(eq(CircuitSubject.DOCUMENT_VERSION), any())).thenReturn(vue);

        mockMvc.perform(get(BASE)).andExpect(status().isOk())
                .andExpect(jsonPath("$.steps[1].roleCode").value("QUALITY_DIRECTOR"));
        mockMvc.perform(put(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps[0].minApprovals").value(2));
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void sansLeDroitDAdministrer_refuseAvantLeCorps() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isForbidden());
        mockMvc.perform(put(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isForbidden());
        verify(service, never()).replace(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void unCircuitMalForme_400ou422() throws Exception {
        when(authorization.has(Permission.AUTHZ_MANAGE)).thenReturn(true);
        // Validation du corps : rôle mal écrit, approbations hors bornes.
        mockMvc.perform(put(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"steps\":[{\"name\":\"R\",\"roleCode\":\"manager\",\"minApprovals\":1}]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"steps\":[{\"name\":\"R\",\"roleCode\":\"AUDITOR\",\"minApprovals\":11}]}"))
                .andExpect(status().isBadRequest());
        // Règle du domaine : le rôle n'a pas le droit d'approuver.
        when(service.replace(eq(CircuitSubject.DOCUMENT_VERSION), any())).thenThrow(
                new CircuitException(CircuitException.Reason.INVALID, "steps", "Le rôle AUDITOR n'a pas le droit."));
        mockMvc.perform(put(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"steps\":[{\"name\":\"R\",\"roleCode\":\"AUDITOR\",\"minApprovals\":1}]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.reason").value("INVALID"))
                .andExpect(jsonPath("$.field").value("steps"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void unTypeDObjetInconnu_404() throws Exception {
        when(authorization.has(Permission.AUTHZ_MANAGE)).thenReturn(true);
        mockMvc.perform(get("/api/v1/circuits/inconnu")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void toutMembreVoitOuEnEstUnObjet() throws Exception {
        UUID version = UUID.randomUUID();
        when(service.run(CircuitSubject.DOCUMENT_VERSION, version)).thenReturn(Optional.of(
                new CircuitDto.RunView(UUID.randomUUID(), "document-version", version, "IN_PROGRESS", 1,
                        List.of(new CircuitDto.StepView("Relecture", "QUALITY_MANAGER", 1),
                                new CircuitDto.StepView("Signature", "QUALITY_DIRECTOR", 1)),
                        List.of(new CircuitDto.DecisionView(0, UUID.randomUUID(), true, null, Instant.now())),
                        Instant.now(), null)));

        mockMvc.perform(get(BASE + "/runs/" + version)).andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStep").value(1))
                .andExpect(jsonPath("$.decisions.length()").value(1));
        mockMvc.perform(get(BASE + "/runs/" + UUID.randomUUID())).andExpect(status().isNoContent());
    }

    @Test
    @WithAnonymousUser
    void anonyme_401() throws Exception {
        mockMvc.perform(get(BASE + "/runs/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }
}
