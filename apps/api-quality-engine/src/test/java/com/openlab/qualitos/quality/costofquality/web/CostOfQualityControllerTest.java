package com.openlab.qualitos.quality.costofquality.web;

import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.costofquality.application.CoqDto;
import com.openlab.qualitos.quality.costofquality.application.CostOfQualityService;
import com.openlab.qualitos.quality.costofquality.domain.CoqCategory;
import com.openlab.qualitos.quality.costofquality.domain.CoqNotFoundException;
import com.openlab.qualitos.quality.costofquality.domain.CoqValidationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le partage des rôles sur le coût de la qualité : tout authentifié lit, seuls
 * les profils de pilotage saisissent.
 */
@Tag("web")
@WebMvcTest(controllers = CostOfQualityController.class)
@Import(MethodSecurityTestConfig.class)
class CostOfQualityControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CostOfQualityService service;

    static final UUID LIGNE = UUID.randomUUID();
    static final UUID LIBELLE = UUID.randomUUID();

    static final String CORPS = "{\"labelId\":\"" + LIBELLE + "\",\"amount\":1263.50,"
            + "\"responsible\":\"Mme Diallo\",\"imputationDate\":\"2026-09-15\","
            + "\"partReference\":\"P-4410\",\"partQuantity\":12,\"lot\":\"L-2609\","
            + "\"receivedOrMadeOn\":\"2026-09-12\"}";

    static CoqDto.LineView ligne() {
        return new CoqDto.LineView(LIGNE, LIBELLE, "INTERNAL_SCRAP", "Rebuts", true,
                new BigDecimal("1263.50"), 1, "Mme Diallo", LocalDate.of(2026, 9, 15), null,
                "P-4410", 12, "L-2609", LocalDate.of(2026, 9, 12));
    }

    static CoqDto.ReportView rapport(Integer mois) {
        return new CoqDto.ReportView(2026, mois, "EUR",
                List.of(new CoqDto.BlockView(CoqCategory.INTERNAL_FAILURE, List.of(ligne()),
                        new BigDecimal("1263.50"))),
                BigDecimal.ZERO, new BigDecimal("1263.50"), new BigDecimal("1263.50"), null, List.of());
    }

    @Test
    @WithMockUser(roles = "USER")
    void toutAuthentifieLitLeMois() throws Exception {
        when(service.report(2026, 9)).thenReturn(rapport(9));

        mockMvc.perform(get("/api/v1/cost-of-quality").param("year", "2026").param("month", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(9))
                .andExpect(jsonPath("$.blocks[0].category").value("INTERNAL_FAILURE"))
                .andExpect(jsonPath("$.blocks[0].lines[0].lot").value("L-2609"))
                .andExpect(jsonPath("$.nonConformanceTotal").value(1263.50));
    }

    @Test
    @WithMockUser(roles = "USER")
    void sansMoisCEstLAnnee() throws Exception {
        when(service.report(eq(2026), isNull())).thenReturn(rapport(null));

        mockMvc.perform(get("/api/v1/cost-of-quality").param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026));
    }

    @Test
    @WithMockUser(roles = "USER")
    void toutAuthentifieLitLeCatalogue() throws Exception {
        when(service.labels()).thenReturn(List.of(new CoqDto.LabelView(LIBELLE, CoqCategory.INTERNAL_FAILURE,
                "INTERNAL_SCRAP", "Rebuts", true, true)));

        mockMvc.perform(get("/api/v1/cost-of-quality/labels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].partControl").value(true))
                .andExpect(jsonPath("$[0].builtIn").value(true));
    }

    @Test
    @WithAnonymousUser
    void unAnonymeNeLitRien() throws Exception {
        mockMvc.perform(get("/api/v1/cost-of-quality").param("year", "2026"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"QUALITY_MANAGER", "DIRECTOR_QUALITY", "ADMIN_TENANT", "SUPER_ADMIN"})
    void lesProfilsDePilotageSaisissent(String role) throws Exception {
        when(service.record(any())).thenReturn(ligne());

        mockMvc.perform(post("/api/v1/cost-of-quality/entries")
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPS))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.entryId").value(LIGNE.toString()));
    }

    @Test
    @WithMockUser(roles = "USER")
    void unUtilisateurSimpleNeSaisitNiNeCorrigeNiNeSupprime() throws Exception {
        mockMvc.perform(post("/api/v1/cost-of-quality/entries").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/cost-of-quality/entries/" + LIGNE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/cost-of-quality/entries/" + LIGNE).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/cost-of-quality/labels").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"APPRAISAL\",\"name\":\"Tri\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/cost-of-quality/currency").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"currency\":\"USD\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void corrigerRendLaLigneCorrigee() throws Exception {
        when(service.revise(eq(LIGNE), any())).thenReturn(ligne());

        mockMvc.perform(put("/api/v1/cost-of-quality/entries/" + LIGNE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partQuantity").value(12));
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void supprimerRend204() throws Exception {
        mockMvc.perform(delete("/api/v1/cost-of-quality/entries/" + LIGNE).with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).delete(LIGNE);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void uneLigneIntrouvableRend404() throws Exception {
        doThrow(new CoqNotFoundException("Entry", LIGNE)).when(service).delete(LIGNE);

        mockMvc.perform(delete("/api/v1/cost-of-quality/entries/" + LIGNE).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void uneRegleDuDomaineRend422AvecLeChampFautif() throws Exception {
        when(service.record(any())).thenThrow(new CoqValidationException("lot", "Le lot est obligatoire."));

        mockMvc.perform(post("/api/v1/cost-of-quality/entries").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.field").value("lot"));
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void unCorpsSansResponsableNiMontantEstRefuseAvantLeService() throws Exception {
        mockMvc.perform(post("/api/v1/cost-of-quality/entries").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"labelId\":\"" + LIBELLE + "\",\"imputationDate\":\"2026-09-15\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/cost-of-quality/entries").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPS.replace("1263.50", "-5")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void ajouterUnLibelleEtFixerLaDevise() throws Exception {
        when(service.createLabel(any())).thenReturn(new CoqDto.LabelView(LIBELLE, CoqCategory.APPRAISAL,
                null, "Tri", true, false));
        when(service.setCurrency("usd")).thenReturn("USD");

        mockMvc.perform(post("/api/v1/cost-of-quality/labels").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"APPRAISAL\",\"name\":\"Tri\",\"partControl\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.builtIn").value(false));
        mockMvc.perform(put("/api/v1/cost-of-quality/currency").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"currency\":\"usd\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"));
        mockMvc.perform(put("/api/v1/cost-of-quality/currency").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"currency\":\"EURO\"}"))
                .andExpect(status().isBadRequest());
    }
}
