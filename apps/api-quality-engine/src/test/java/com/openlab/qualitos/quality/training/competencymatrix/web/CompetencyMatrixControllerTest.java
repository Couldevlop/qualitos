package com.openlab.qualitos.quality.training.competencymatrix.web;

import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.training.competencymatrix.application.CompetencyMatrixDto;
import com.openlab.qualitos.quality.training.competencymatrix.application.CompetencyMatrixService;
import com.openlab.qualitos.quality.training.competencymatrix.domain.CompetencyMatrixConflictException;
import com.openlab.qualitos.quality.training.competencymatrix.domain.CompetencyMatrixNotFoundException;
import com.openlab.qualitos.quality.training.competencymatrix.domain.CompetencyMatrixValidationException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le partage des rôles sur la matrice : tout authentifié lit, l'action
 * {@code training.manage} écrit, l'administrateur du client règle les
 * ateliers ; les corps sont validés avant le service et ne portent jamais
 * le client.
 */
@Tag("web")
@WebMvcTest(controllers = {CompetencyMatrixController.class, CompetencyWorkshopsController.class})
@Import(MethodSecurityTestConfig.class)
class CompetencyMatrixControllerTest {

    static final String BASE = "/api/v1/training/competencies/matrix";
    static final String WORKSHOPS = "/api/v1/training/competencies/workshops";
    static final UUID PERSONNE = UUID.fromString("6f1c2a4e-1f0b-4c47-9d0e-2b8f3a6c7d11");
    static final UUID FORMATEUR = UUID.fromString("0b9d6e1a-7c3f-4d2a-8e5b-1a2b3c4d5e6f");
    static final UUID COMPETENCE = UUID.fromString("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d");
    static final UUID INSCRIPTION = UUID.fromString("11111111-2222-4333-8444-555555555555");

    @Autowired MockMvc mockMvc;
    @MockitoBean CompetencyMatrixService service;

    static CompetencyMatrixDto.MatrixView vue() {
        return new CompetencyMatrixDto.MatrixView(List.of("Usinage"), true,
                List.of(new CompetencyMatrixDto.OperatorView(INSCRIPTION, PERSONNE, "Martin Dubois", "Usinage")),
                List.of(new CompetencyMatrixDto.SkillView(COMPETENCE, "Tournage CNC", "Usinage", FORMATEUR,
                        "Karim Benali", 1, true)),
                List.of(new CompetencyMatrixDto.CellView(PERSONNE, COMPETENCE, 3)));
    }

    static final String OPERATEUR = "{\"userId\":\"" + PERSONNE + "\",\"userName\":\"Martin Dubois\",\"workshop\":\"Usinage\"}";
    static final String COMPETENCE_CORPS = "{\"name\":\"Tournage CNC\",\"category\":\"Usinage\",\"trainerUserId\":\""
            + FORMATEUR + "\",\"trainerName\":\"Karim Benali\"}";
    static final String NIVEAU = "{\"userId\":\"" + PERSONNE + "\",\"skillId\":\"" + COMPETENCE + "\",\"level\":3}";

    @Test
    @WithMockUser(roles = "USER")
    void toutAuthentifieLitLaMatriceEtLesAteliers() throws Exception {
        when(service.view()).thenReturn(vue());
        when(service.workshops()).thenReturn(new CompetencyMatrixDto.WorkshopsView(List.of("Usinage"), true));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workshops[0]").value("Usinage"))
                .andExpect(jsonPath("$.operators[0].name").value("Martin Dubois"))
                .andExpect(jsonPath("$.skills[0].trainerName").value("Karim Benali"))
                .andExpect(jsonPath("$.skills[0].atRisk").value(true))
                .andExpect(jsonPath("$.cells[0].level").value(3));
        mockMvc.perform(get(WORKSHOPS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaults").value(true));
    }

    @Test
    @WithAnonymousUser
    void unAnonymeNeLitRien() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(WORKSHOPS)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"QUALITY_MANAGER", "QUALITY_DIRECTOR", "ADMIN_TENANT", "SUPER_ADMIN"})
    void lesProfilsDePilotageTiennentLaMatrice(String role) throws Exception {
        when(service.addOperator(any())).thenReturn(vue());
        when(service.removeOperator(INSCRIPTION)).thenReturn(vue());
        when(service.addSkill(any())).thenReturn(vue());
        when(service.removeSkill(COMPETENCE)).thenReturn(vue());
        when(service.setLevel(any())).thenReturn(new CompetencyMatrixDto.CellView(PERSONNE, COMPETENCE, 3));

        mockMvc.perform(post(BASE + "/operators").with(csrf()).with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON).content(OPERATEUR))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.operators[0].userId").value(PERSONNE.toString()));
        mockMvc.perform(delete(BASE + "/operators/" + INSCRIPTION).with(csrf()).with(user("u").roles(role)))
                .andExpect(status().isOk());
        mockMvc.perform(post(BASE + "/skills").with(csrf()).with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON).content(COMPETENCE_CORPS))
                .andExpect(status().isCreated());
        mockMvc.perform(delete(BASE + "/skills/" + COMPETENCE).with(csrf()).with(user("u").roles(role)))
                .andExpect(status().isOk());
        mockMvc.perform(put(BASE + "/levels").with(csrf()).with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON).content(NIVEAU))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value(3));

        verify(service).addOperator(new CompetencyMatrixDto.AddOperatorCommand(PERSONNE, "Martin Dubois", "Usinage"));
        verify(service).addSkill(new CompetencyMatrixDto.AddSkillCommand("Tournage CNC", "Usinage", FORMATEUR,
                "Karim Benali"));
        verify(service).setLevel(new CompetencyMatrixDto.SetLevelCommand(PERSONNE, COMPETENCE, 3));
    }

    @Test
    @WithMockUser(roles = {"USER", "AUDITOR"})
    void niUnUtilisateurNiUnAuditeurNEcrivent() throws Exception {
        mockMvc.perform(post(BASE + "/operators").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(OPERATEUR)).andExpect(status().isForbidden());
        mockMvc.perform(delete(BASE + "/operators/" + INSCRIPTION).with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/skills").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(COMPETENCE_CORPS)).andExpect(status().isForbidden());
        mockMvc.perform(delete(BASE + "/skills/" + COMPETENCE).with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(put(BASE + "/levels").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(NIVEAU)).andExpect(status().isForbidden());
        mockMvc.perform(put(WORKSHOPS).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"names\":[\"A\"]}")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void seulLAdministrateurDuClientRegleLesAteliers() throws Exception {
        mockMvc.perform(put(WORKSHOPS).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"names\":[\"A\"]}")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void lAdministrateurRegleLesAteliers() throws Exception {
        when(service.replaceWorkshops(any())).thenReturn(
                new CompetencyMatrixDto.WorkshopsView(List.of("Peinture", "Montage"), false));

        mockMvc.perform(put(WORKSHOPS).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"names\":[\"Peinture\",\"Montage\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names[1]").value("Montage"))
                .andExpect(jsonPath("$.defaults").value(false));
        verify(service).replaceWorkshops(List.of("Peinture", "Montage"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void desCorpsMalFormesSontRefusesAvantLeService() throws Exception {
        for (String corps : List.of(
                "{\"userName\":\"Martin\",\"workshop\":\"Usinage\"}",
                "{\"userId\":\"" + PERSONNE + "\",\"userName\":\"\",\"workshop\":\"Usinage\"}",
                "{\"userId\":\"" + PERSONNE + "\",\"userName\":\"Martin\",\"workshop\":\"" + "x".repeat(61) + "\"}",
                "{\"userId\":\"pas-un-uuid\",\"userName\":\"Martin\",\"workshop\":\"Usinage\"}")) {
            mockMvc.perform(post(BASE + "/operators").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content(corps)).andExpect(status().isBadRequest());
        }
        for (String corps : List.of(
                "{\"name\":\"Tournage\",\"category\":\"Usinage\",\"trainerName\":\"Karim\"}",
                "{\"name\":\"\",\"category\":\"Usinage\",\"trainerUserId\":\"" + FORMATEUR + "\",\"trainerName\":\"K\"}",
                "{\"name\":\"T\",\"category\":\"\",\"trainerUserId\":\"" + FORMATEUR + "\",\"trainerName\":\"K\"}",
                "{\"name\":\"T\",\"category\":\"U\",\"trainerUserId\":\"" + FORMATEUR + "\",\"trainerName\":\"\"}")) {
            mockMvc.perform(post(BASE + "/skills").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content(corps)).andExpect(status().isBadRequest());
        }
        for (String corps : List.of(
                NIVEAU.replace("\"level\":3", "\"level\":5"),
                NIVEAU.replace("\"level\":3", "\"level\":-1"),
                "{\"userId\":\"" + PERSONNE + "\",\"level\":2}")) {
            mockMvc.perform(put(BASE + "/levels").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content(corps)).andExpect(status().isBadRequest());
        }
        for (String corps : List.of("{\"names\":[]}", "{\"names\":[\"\"]}", "{}")) {
            mockMvc.perform(put(WORKSHOPS).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content(corps)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void unClientGlisseDansLeCorpsEstIgnore() throws Exception {
        when(service.addOperator(any())).thenReturn(vue());
        String avecClient = OPERATEUR.replace("{", "{\"tenantId\":\"" + UUID.randomUUID() + "\",");

        mockMvc.perform(post(BASE + "/operators").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(avecClient)).andExpect(status().isCreated());

        // La commande n'a pas de champ client : le service prend celui du jeton.
        verify(service).addOperator(argThat(c -> c.userId().equals(PERSONNE) && c.workshop().equals("Usinage")));
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void lesRefusDuServiceDeviennent404_409_422AvecLeChamp() throws Exception {
        when(service.removeOperator(INSCRIPTION)).thenThrow(new CompetencyMatrixNotFoundException("absent"));
        when(service.addOperator(any())).thenThrow(new CompetencyMatrixConflictException("userId", "déjà là"));
        when(service.addSkill(any())).thenThrow(new CompetencyMatrixValidationException("workshop", "inconnu"));

        mockMvc.perform(delete(BASE + "/operators/" + INSCRIPTION).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.field").doesNotExist());
        mockMvc.perform(post(BASE + "/operators").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(OPERATEUR))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.field").value("userId"));
        mockMvc.perform(post(BASE + "/skills").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(COMPETENCE_CORPS))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.field").value("workshop"));
    }
}
