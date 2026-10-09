package com.openlab.qualitos.quality.authz.web;

import com.openlab.qualitos.quality.authz.application.AuthorizationService;
import com.openlab.qualitos.quality.authz.application.AuthzDto;
import com.openlab.qualitos.quality.authz.domain.AuthzNotFoundException;
import com.openlab.qualitos.quality.authz.domain.AuthzValidationException;
import com.openlab.qualitos.quality.authz.domain.Permission;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** L'administration des droits : chacun lit les siens, seul « authz.manage » administre. */
@Tag("web")
@WebMvcTest(controllers = AuthzController.class)
@Import(MethodSecurityTestConfig.class)
class AuthzControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AuthorizationService service;

    static final String BASE = "/api/v1/authz";
    static final String CORPS_ROLE = "{\"code\":\"PILOTE_SITE\",\"name\":\"Pilote\",\"permissions\":[\"nc.close\"]}";

    @Test
    @WithMockUser(roles = "USER")
    void chacunLitSesDroitsMaisPasCeuxDesAutres() throws Exception {
        when(service.me()).thenReturn(new AuthzDto.Me(UUID.randomUUID(), List.of("USER"), List.of("nc.create")));

        mockMvc.perform(get(BASE + "/me")).andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions[0]").value("nc.create"));
        mockMvc.perform(get(BASE + "/roles")).andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/catalog")).andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/members")).andExpect(status().isForbidden());
        // Refusé AVANT la lecture du corps : un corps invalide ne change rien au verdict.
        mockMvc.perform(post(BASE + "/roles").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(BASE + "/members/" + UUID.randomUUID() + "/roles").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"ADMIN_TENANT\"]}"))
                .andExpect(status().isForbidden());
        verify(service, never()).replaceMemberRoles(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void lAdministrateurRegleRolesEtMembres() throws Exception {
        when(service.has(Permission.AUTHZ_MANAGE)).thenReturn(true);
        AuthzDto.RoleView pilote = new AuthzDto.RoleView("PILOTE_SITE", "Pilote", null, false, true, List.of("nc.close"));
        when(service.roles()).thenReturn(List.of(pilote));
        when(service.catalog()).thenReturn(List.of(new AuthzDto.CatalogEntry("nc.close", "nc")));
        when(service.createRole(any())).thenReturn(pilote);
        when(service.updateRole(eq("PILOTE_SITE"), any())).thenReturn(pilote);
        UUID marie = UUID.randomUUID();
        when(service.replaceMemberRoles(eq(marie), any())).thenReturn(new AuthzDto.MemberView(marie, List.of("PILOTE_SITE")));
        when(service.members()).thenReturn(List.of(new AuthzDto.MemberView(marie, List.of("PILOTE_SITE"))));

        mockMvc.perform(get(BASE + "/roles")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customized").value(true));
        mockMvc.perform(get(BASE + "/catalog")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].module").value("nc"));
        mockMvc.perform(post(BASE + "/roles").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CORPS_ROLE))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("PILOTE_SITE"));
        mockMvc.perform(put(BASE + "/roles/PILOTE_SITE").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(CORPS_ROLE)).andExpect(status().isOk());
        mockMvc.perform(delete(BASE + "/roles/PILOTE_SITE").with(csrf())).andExpect(status().isNoContent());
        verify(service).deleteRole("PILOTE_SITE");
        mockMvc.perform(put(BASE + "/members/" + marie + "/roles").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"PILOTE_SITE\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").value("PILOTE_SITE"));
        mockMvc.perform(get(BASE + "/members")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(marie.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void lesRefusDuDomaineDeviennent422Ou404() throws Exception {
        when(service.has(Permission.AUTHZ_MANAGE)).thenReturn(true);
        when(service.createRole(any())).thenThrow(new AuthzValidationException("code", "Un rôle porte déjà ce code."));
        doThrow(new AuthzNotFoundException("Rôle introuvable.")).when(service).deleteRole("FANTOME");

        mockMvc.perform(post(BASE + "/roles").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CORPS_ROLE))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.field").value("code"));
        mockMvc.perform(delete(BASE + "/roles/FANTOME").with(csrf())).andExpect(status().isNotFound());
        // Un code de rôle mal formé dans l'adresse ne va pas jusqu'au service.
        mockMvc.perform(delete(BASE + "/roles/x;drop").with(csrf())).andExpect(status().isBadRequest());
        // Un corps sans liste de droits est refusé à la frontière.
        mockMvc.perform(post(BASE + "/roles").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"P\",\"name\":\"P\"}")).andExpect(status().isBadRequest());
    }

    @Test
    @WithAnonymousUser
    void anonymeRefuse() throws Exception {
        mockMvc.perform(get(BASE + "/me")).andExpect(status().isUnauthorized());
    }
}
