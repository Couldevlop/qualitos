package com.openlab.qualitos.quality.nonconformity.subcategory;

import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.nonconformity.NcCategory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le catalogue des natures d'écart se lit par tout authentifié — chaque
 * formulaire de saisie en a besoin — et ne se change que par l'administrateur
 * du client.
 */
@Tag("web")
@WebMvcTest(controllers = NcSubcategoryController.class)
@Import(MethodSecurityTestConfig.class)
class NcSubcategoryControllerTest {

    static final String BASE = "/api/v1/nc/subcategories";

    static final String CORPS = "{\"entries\":[{\"code\":\"MIEN-1\",\"category\":\"PROCESS\","
            + "\"label\":\"Ma nature\"}]}";

    @Autowired MockMvc mockMvc;
    @MockitoBean NcSubcategoryCatalog catalog;

    private static NcSubcategoryCatalog.View defauts() {
        return new NcSubcategoryCatalog.View(NcSubcategoryDefaults.CATALOGUE, true);
    }

    @Test
    @WithMockUser(roles = "USER")
    void toutAuthentifieLitLeCatalogue() throws Exception {
        when(catalog.view()).thenReturn(defauts());

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaults").value(true))
                .andExpect(jsonPath("$.entries[0].code").value("PRD-DIM"))
                .andExpect(jsonPath("$.entries[0].category").value("PRODUCT"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void lAdministrateurRemplaceEtRetablit() throws Exception {
        when(catalog.replace(anyList())).thenReturn(
                new NcSubcategoryCatalog.View(
                        List.of(new NcSubcategory("MIEN-1", NcCategory.PROCESS, "Ma nature")), false));
        when(catalog.reset()).thenReturn(defauts());

        mockMvc.perform(put(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaults").value(false));
        mockMvc.perform(post(BASE + "/reset").with(csrf())).andExpect(status().isOk());

        verify(catalog).replace(argThat(e -> e.size() == 1 && e.get(0).code().equals("MIEN-1")
                && e.get(0).category() == NcCategory.PROCESS && e.get(0).label().equals("Ma nature")));
        verify(catalog).reset();
    }

    @Test
    @WithMockUser(roles = {"QUALITY_DIRECTOR", "QUALITY_MANAGER"})
    void lePilotageNeParametrePas() throws Exception {
        mockMvc.perform(put(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/reset").with(csrf())).andExpect(status().isForbidden());

        verifyNoInteractions(catalog);
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void unCatalogueMalFormeEstRefuseAvantLeService() throws Exception {
        for (String c : List.of(
                "{\"entries\":[]}",
                CORPS.replace("\"category\":\"PROCESS\"", "\"category\":null"),
                CORPS.replace("\"label\":\"Ma nature\"", "\"label\":\"\""),
                CORPS.replace("\"code\":\"MIEN-1\"", "\"code\":\"   \""))) {
            mockMvc.perform(put(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(c))
                    .andExpect(status().isBadRequest());
        }

        verifyNoInteractions(catalog);
    }
}
