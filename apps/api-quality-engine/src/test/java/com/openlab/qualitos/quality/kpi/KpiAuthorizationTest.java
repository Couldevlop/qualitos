package com.openlab.qualitos.quality.kpi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.config.SecurityConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Qui a le droit de toucher à un indicateur.
 *
 * <p>Avant ces bancs, les routes des indicateurs ne demandaient rien : la
 * configuration de sécurité ne nomme que les suppressions et quelques chemins
 * précis, tout le reste tombant sur {@code anyRequest().authenticated()}. Un
 * simple utilisateur pouvait donc <b>déplacer la cible</b> d'un indicateur qu'on
 * n'atteint pas, et le faire passer au vert sans que rien n'ait changé sur le
 * terrain — exactement ce qu'un auditeur vient chercher au §6.2 et au §9.3.
 *
 * <p>La frontière retenue sépare la DÉCISION du FAIT :
 * <ul>
 *   <li>définir l'indicateur, sa cible, ses seuils, son cycle de vie, et
 *       retirer un relevé : {@code kpi.manage}, donc le pilotage ;</li>
 *   <li>consigner un relevé : {@code kpi.measure}, ouvert à l'utilisateur, parce
 *       qu'un relevé est consigné par celui qui a mesuré ;</li>
 *   <li>LIRE : ouvert à tout authentifié — chacun doit pouvoir voir où en est ce
 *       sur quoi on le juge.</li>
 * </ul>
 *
 * <p>Les droits se règlent ensuite client par client (ADR 0078) : ce banc vérifie
 * le catalogue livré, pas une liste de rôles écrite en dur dans le contrôleur.
 *
 * <p>Monté sur la VRAIE chaîne de filtres ({@link SecurityConfig}), que
 * {@code KpiControllerTest} ne charge pas — comme {@code SpcDeleteSecurityTest} :
 * c'est la seule façon de voir l'alias de rôle du directeur qualité, et de
 * distinguer un 401 d'un 403. Accessoirement, cela donne à ce banc un contexte
 * Spring distinct : deux {@code @WebMvcTest} du même contrôleur avec les mêmes
 * simulacres partagent leur contexte, et les stubs de l'un filent entre les
 * doigts de l'autre.
 */
@Tag("web")
@WebMvcTest(controllers = KpiController.class)
@Import(SecurityConfig.class)
class KpiAuthorizationTest {

    static final String BASE = "/api/v1/kpis";
    static final UUID KPI = UUID.randomUUID();
    static final UUID MESURE = UUID.randomUUID();

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean KpiService service;

    private String corpsDeMesure() throws Exception {
        ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());
        return om.writeValueAsString(new KpiDto.RecordMeasurementRequest(
                Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-31T00:00:00Z"),
                new BigDecimal("720"), "ppm", null, null, null));
    }

    // ---------- la décision : réservée au pilotage ----------

    @Test @WithMockUser(roles = "USER")
    void unUtilisateurNeDefinitPasUnIndicateur() throws Exception {
        mockMvc.perform(post(BASE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verify(service, never()).create(any());
    }

    /**
     * Le cas qui motive tout le banc : la cible vit dans la mise à jour.
     */
    @Test @WithMockUser(roles = "USER")
    void unUtilisateurNeDeplacePasLaCible() throws Exception {
        mockMvc.perform(patch(BASE + "/" + KPI).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetValue\":9999}"))
                .andExpect(status().isForbidden());
        verify(service, never()).update(any(), any());
    }

    @Test @WithMockUser(roles = "AUDITOR")
    void unAuditeurConstate_maisNeDefinitRien() throws Exception {
        // L'auditeur a la lecture la plus large de la plateforme, et aucune main
        // sur ce qu'il audite : fixer la cible qu'il viendra vérifier serait
        // juger son propre travail.
        mockMvc.perform(patch(BASE + "/" + KPI).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/" + KPI + "/archive").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "USER")
    void unUtilisateurNArchiveNiNeRouvreUnIndicateur() throws Exception {
        mockMvc.perform(post(BASE + "/" + KPI + "/activate").with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/" + KPI + "/reopen").with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/" + KPI + "/archive").with(csrf()))
                .andExpect(status().isForbidden());
        verify(service, never()).activate(any());
        verify(service, never()).reopen(any());
        verify(service, never()).archive(any());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void leManagerQualiteDefinitLIndicateur() throws Exception {
        mockMvc.perform(post(BASE + "/" + KPI + "/archive").with(csrf()))
                .andExpect(status().isOk());
        verify(service).archive(KPI);
    }

    @Test @WithMockUser(roles = "QUALITY_DIRECTOR")
    void leDirecteurQualiteAussi() throws Exception {
        // Le realm nomme ce rôle « quality_director » et le code « DIRECTOR_QUALITY » :
        // sans l'alias posé dans SecurityConfig, un vrai directeur serait refusé.
        mockMvc.perform(post(BASE + "/" + KPI + "/archive").with(csrf()))
                .andExpect(status().isOk());
        verify(service).archive(KPI);
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void lAdministrateurDuClientAussi() throws Exception {
        mockMvc.perform(post(BASE + "/" + KPI + "/archive").with(csrf()))
                .andExpect(status().isOk());
        verify(service).archive(KPI);
    }

    // ---------- le fait : consigner un relevé ----------

    @Test @WithMockUser(roles = "USER")
    void unUtilisateurConsigneUnReleve() throws Exception {
        mockMvc.perform(post(BASE + "/" + KPI + "/measurements").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpsDeMesure()))
                .andExpect(status().isCreated());
        verify(service).record(eq(KPI), any());
    }

    /**
     * Mais il ne l'efface pas : effacer un relevé gênant change la courbe sans
     * laisser de trace à l'écran. C'est pourquoi le retrait relève de
     * {@code kpi.manage} et non de {@code kpi.measure}.
     */
    @Test @WithMockUser(roles = "USER")
    void unUtilisateurNEffacePasUnReleveGenant() throws Exception {
        mockMvc.perform(delete(BASE + "/" + KPI + "/measurements/" + MESURE).with(csrf()))
                .andExpect(status().isForbidden());
        verify(service, never()).deleteMeasurement(any(), any());
    }

    @Test
    void sansJeton_401() throws Exception {
        // Un 401 et un 403 ne disent pas la meme chose : « identifie-toi » n'est
        // pas « tu n'as pas le droit ».
        mockMvc.perform(post(BASE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        verify(service, never()).create(any());
    }

    // ---------- la lecture reste ouverte ----------

    @Test @WithMockUser(roles = "USER")
    void toutAuthentifieLitLesIndicateurs() throws Exception {
        mockMvc.perform(get(BASE + "/" + KPI)).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/" + KPI + "/status")).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/" + KPI + "/trend")).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/" + KPI + "/measurements")).andExpect(status().isOk());
    }
}
