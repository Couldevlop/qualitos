package com.openlab.qualitos.quality.nonconformity.eightd.web;

import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDDto;
import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La vérification publique d'un rapport 8D émis.
 *
 * <p>Deux invariants de sécurité : un code inconnu répond {@code valid=false} et non
 * 404 — sinon la route permettrait d'énumérer les rapports existants — et la réponse
 * ne porte que des faits d'intégrité, jamais le contenu du rapport (OWASP A01).
 *
 * <p>L'ouverture du chemin lui-même (sans jeton) est vérifiée par
 * {@link EightDPublicPathTest}, qui charge la configuration de sécurité réelle.
 */
@Tag("web")
@WebMvcTest(controllers = EightDPublicController.class)
class EightDPublicControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean EightDService service;

    private static final String CODE = "ABCDEFGHIJKLMNOP";

    @Test
    @WithMockUser
    void un_code_connu_rend_les_faits_d_integrite() throws Exception {
        when(service.verifier(CODE)).thenReturn(new EightDDto.VerificationResult(
                true, CODE, "d".repeat(64), "tx-1", "NC-2026-0007",
                Instant.parse("2026-09-13T10:00:00Z")));

        mockMvc.perform(get("/api/v1/nc/public/8d/{code}/verify", CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.sha256Hex").value("d".repeat(64)))
                .andExpect(jsonPath("$.anchorTxRef").value("tx-1"))
                .andExpect(jsonPath("$.ncReference").value("NC-2026-0007"));
    }

    @Test
    @WithMockUser
    void un_code_inconnu_repond_invalide_et_non_404() throws Exception {
        when(service.verifier(CODE)).thenReturn(EightDDto.VerificationResult.unknown(CODE));

        mockMvc.perform(get("/api/v1/nc/public/8d/{code}/verify", CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.sha256Hex").doesNotExist())
                .andExpect(jsonPath("$.ncReference").doesNotExist());
    }

    @Test
    @WithMockUser
    void un_code_mal_forme_n_atteint_jamais_le_service() throws Exception {
        mockMvc.perform(get("/api/v1/nc/public/8d/{code}/verify", "bad code!"))
                .andExpect(status().is4xxClientError());
        verifyNoInteractions(service);
    }
}
