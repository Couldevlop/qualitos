package com.openlab.qualitos.quality.airouting.web;

import com.openlab.qualitos.quality.airouting.application.AiPolicyAdminService;
import com.openlab.qualitos.quality.airouting.application.AiPolicyDto;
import com.openlab.qualitos.quality.airouting.domain.AiProvider;
import com.openlab.qualitos.quality.airouting.domain.AiProviderNotConfiguredException;
import com.openlab.qualitos.quality.airouting.domain.InvalidAiProviderException;
import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Où partent les prompts du tenant est un paramétrage : seul son administrateur le
 * lit et le change, et le corps ne porte rien d'autre que le fournisseur.
 */
@Tag("web")
@WebMvcTest(controllers = AiPolicyController.class)
@Import(MethodSecurityTestConfig.class)
class AiPolicyControllerTest {

    private static final String BASE = "/api/v1/admin/ai-policy";
    private static final UUID ADMIN = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired MockMvc mockMvc;
    @MockitoBean AiPolicyAdminService service;

    private static AiPolicyDto.View pinnedOllama() {
        return new AiPolicyDto.View(true, "ollama", "ollama", "mistral", true, List.of(
                new AiPolicyDto.ProviderOption("ollama", true, true),
                new AiPolicyDto.ProviderOption("anthropic", false, false),
                new AiPolicyDto.ProviderOption("mistral", false, true)),
                Instant.parse("2026-10-25T10:00:00Z"), ADMIN);
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void l_administrateur_lit_epingle_et_revient_au_defaut() throws Exception {
        when(service.view()).thenReturn(pinnedOllama());
        when(service.pin(any())).thenReturn(pinnedOllama());
        when(service.release()).thenReturn(new AiPolicyDto.View(false, null, "mistral", "mistral", true,
                List.of(), null, null));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("ollama"))
                .andExpect(jsonPath("$.providers[0].onPremise").value(true))
                .andExpect(jsonPath("$.providers[1].configured").value(false));
        mockMvc.perform(put(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"ollama\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveProvider").value("ollama"));
        mockMvc.perform(delete(BASE).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pinned").value(false));

        verify(service).pin(argThat(c -> "ollama".equals(c.provider())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "QUALITY_MANAGER", "QUALITY_DIRECTOR", "AUDITOR"})
    void les_autres_profils_ne_lisent_ni_ne_changent_rien(String role) throws Exception {
        mockMvc.perform(get(BASE).with(user("u").roles(role))).andExpect(status().isForbidden());
        mockMvc.perform(put(BASE).with(csrf()).with(user("u").roles(role))
                .contentType(MediaType.APPLICATION_JSON).content("{\"provider\":\"mistral\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(BASE).with(csrf()).with(user("u").roles(role))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithAnonymousUser
    void un_anonyme_n_obtient_rien() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"provider\":\"\"}", "{\"provider\":\"openai\"}",
            "{\"provider\":\"http://evil.example\"}"})
    @WithMockUser(roles = "ADMIN_TENANT")
    void un_fournisseur_hors_liste_est_refuse_a_la_frontiere(String body) throws Exception {
        mockMvc.perform(put(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void un_fournisseur_non_configure_rend_422_et_le_nomme() throws Exception {
        when(service.pin(any())).thenThrow(new AiProviderNotConfiguredException(AiProvider.ANTHROPIC));

        mockMvc.perform(put(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"anthropic\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.provider").value("anthropic"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void le_domaine_refuse_aussi_un_fournisseur_inconnu() throws Exception {
        when(service.pin(any())).thenThrow(new InvalidAiProviderException("x"));

        mockMvc.perform(put(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"mistral\"}"))
                .andExpect(status().isBadRequest());
    }
}
