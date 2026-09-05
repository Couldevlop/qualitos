package com.openlab.qualitos.quality.product.export;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La plomberie du telechargement : ce qui distingue un fichier qui s'ouvre dans
 * un tableur d'une page d'octets illisibles.
 */
@Tag("web")
@DisplayName("ProductExportController")
@WebMvcTest(controllers = ProductExportController.class)
class ProductExportControllerTest {

    private static final UUID PRODUIT = UUID.randomUUID();
    private static final String XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired MockMvc mockMvc;
    @MockitoBean ProductExportService service;

    @Test
    @WithMockUser
    void rendLeClasseurAvecSonTypeEtSonNom() throws Exception {
        // Sans `attachment` et sans nom de fichier, le classeur arrive chez
        // l'utilisateur sans extension - et Excel refuse de l'ouvrir.
        byte[] octets = "PK-classeur".getBytes(StandardCharsets.UTF_8);
        when(service.export(PRODUIT))
                .thenReturn(new ProductExportService.Export("p-001-pfmea.xlsx", octets));

        mockMvc.perform(get("/api/v1/products/{id}/export/xlsx", PRODUIT))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(XLSX))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("p-001-pfmea.xlsx")))
                .andExpect(content().bytes(octets));

        verify(service).export(PRODUIT);
    }

    @Test
    void refuseUnAppelAnonyme() throws Exception {
        // Le dossier d'un produit n'est pas public : sans jeton, on ne descend
        // meme pas jusqu'au service.
        mockMvc.perform(get("/api/v1/products/{id}/export/xlsx", PRODUIT))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser
    void unIdentifiantMalFormeNeDescendPasJusquAuService() throws Exception {
        // Un chemin bricole a la main ne doit pas atteindre la couche metier :
        // la conversion echoue avant, et rend une erreur de requete.
        mockMvc.perform(get("/api/v1/products/{id}/export/xlsx", "pas-un-uuid"))
                .andExpect(status().is4xxClientError());

        verify(service, org.mockito.Mockito.never()).export(any());
    }
}
