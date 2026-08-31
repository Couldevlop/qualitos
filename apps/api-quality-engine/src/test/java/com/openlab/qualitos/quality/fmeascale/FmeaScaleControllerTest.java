package com.openlab.qualitos.quality.fmeascale;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le partage des rôles sur le référentiel de cotation.
 *
 * <p>LIRE est ouvert à tout utilisateur authentifié : on ne peut pas demander à
 * quelqu'un de coter de 1 à 10 en lui cachant ce que valent les chiffres.
 *
 * <p>ÉCRIRE ne l'est pas. Redéfinir un barème rend incomparables les RPN cotés
 * avant et après — c'est une décision de politique qualité. Le manager qualité
 * en est volontairement exclu : il cote, il ne redéfinit pas l'échelle sur
 * laquelle il cote.
 */
@Tag("web")
@WebMvcTest(controllers = FmeaScaleController.class)
@Import(MethodSecurityTestConfig.class)
class FmeaScaleControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean FmeaScaleService service;

    private static final String BODY = """
            {"rows":[
              {"score":10,"label":"Arrêt de ligne client"},
              {"score":9,"label":"N9"},{"score":8,"label":"N8"},{"score":7,"label":"N7"},
              {"score":6,"label":"N6"},{"score":5,"label":"N5"},{"score":4,"label":"N4"},
              {"score":3,"label":"N3"},{"score":2,"label":"N2"},{"score":1,"label":"N1"}
            ]}""";

    private static FmeaScaleDto.ScaleView view(boolean custom) {
        return new FmeaScaleDto.ScaleView(FmeaScaleKind.SEVERITY, custom,
                List.of(new FmeaScaleDto.RowView(10, "Arrêt de ligne client", null, null, null)),
                null, null);
    }

    // ---------- lecture ----------

    @Test
    @WithMockUser(roles = "USER")
    void anyAuthenticatedUserMayReadTheScales() throws Exception {
        when(service.findAll()).thenReturn(new FmeaScaleDto.ReferenceView(List.of(view(false))));

        mockMvc.perform(get("/api/v1/fmea/rating-scales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scales[0].kind").value("SEVERITY"))
                .andExpect(jsonPath("$.scales[0].custom").value(false));
    }

    @Test
    @WithAnonymousUser
    void anAnonymousVisitorReadsNothing() throws Exception {
        mockMvc.perform(get("/api/v1/fmea/rating-scales"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    // ---------- écriture ----------

    @Test
    @WithMockUser(roles = "DIRECTOR_QUALITY")
    void theQualityDirectorMayRedefineAScale() throws Exception {
        when(service.replace(eq(FmeaScaleKind.SEVERITY), any())).thenReturn(view(true));

        mockMvc.perform(put("/api/v1/fmea/rating-scales/SEVERITY")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.custom").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void theTenantAdministratorMayToo() throws Exception {
        when(service.replace(eq(FmeaScaleKind.SEVERITY), any())).thenReturn(view(true));

        mockMvc.perform(put("/api/v1/fmea/rating-scales/SEVERITY")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void theQualityManagerCotesButDoesNotRedefineTheScale() throws Exception {
        mockMvc.perform(put("/api/v1/fmea/rating-scales/SEVERITY")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void norMayHeRestoreTheReference() throws Exception {
        mockMvc.perform(delete("/api/v1/fmea/rating-scales/SEVERITY").with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "DIRECTOR_QUALITY")
    void restoringTheReferenceRendersTheScaleThatTakesEffect() throws Exception {
        when(service.revertToReference(FmeaScaleKind.SEVERITY)).thenReturn(view(false));

        mockMvc.perform(delete("/api/v1/fmea/rating-scales/SEVERITY").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.custom").value(false));
    }

    // ---------- validation de surface ----------

    @Test
    @WithMockUser(roles = "DIRECTOR_QUALITY")
    void anEmptyScaleIsRefusedAtTheBoundary() throws Exception {
        mockMvc.perform(put("/api/v1/fmea/rating-scales/SEVERITY")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rows\":[]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "DIRECTOR_QUALITY")
    void aScoreOutsideOneToTenIsRefusedAtTheBoundary() throws Exception {
        // Le RPN est le produit des trois cotations : un 11 le ferait sortir de
        // la plage attendue sans qu'aucun écran ne le signale.
        mockMvc.perform(put("/api/v1/fmea/rating-scales/SEVERITY")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rows\":[{\"score\":11,\"label\":\"Trop\"}]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "DIRECTOR_QUALITY")
    void anUnknownScaleNameIsNotAScale() throws Exception {
        mockMvc.perform(get("/api/v1/fmea/rating-scales/CRITICITY"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
