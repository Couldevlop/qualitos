package com.openlab.qualitos.quality.nonconformity.eightd.web;

import com.openlab.qualitos.quality.config.SecurityConfig;
import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDDto;
import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le chemin de vérification est-il réellement ouvert ?
 *
 * <p>La question mérite son propre banc, avec la {@link SecurityConfig} réelle : un QR
 * code imprimé sur un document remis au client ne peut pas demander de s'authentifier.
 * Si la règle {@code permitAll} disparaissait d'un refactoring, tous les PDF déjà émis
 * deviendraient invérifiables — et rien, dans les autres tests, ne le signalerait.
 */
@Tag("web")
@WebMvcTest(controllers = EightDPublicController.class)
@Import(SecurityConfig.class)
class EightDPublicPathTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean EightDService service;
    @MockitoBean JwtDecoder jwtDecoder;

    private static final String CODE = "ABCDEFGHIJKLMNOP";

    @Test
    @WithAnonymousUser
    void la_verification_repond_sans_jeton() throws Exception {
        org.mockito.Mockito.when(service.verifier(CODE)).thenReturn(
                new EightDDto.VerificationResult(true, CODE, "d".repeat(64), "tx-1",
                        "NC-2026-0007", Instant.parse("2026-09-13T10:00:00Z")));

        mockMvc.perform(get("/api/v1/nc/public/8d/{code}/verify", CODE))
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void seule_la_lecture_est_ouverte_sur_ce_chemin() throws Exception {
        // Le chemin public est ouvert en GET et en GET seulement : une écriture y
        // serait refusée avant d'atteindre quoi que ce soit.
        mockMvc.perform(post("/api/v1/nc/public/8d/{code}/verify", CODE))
                .andExpect(status().is4xxClientError());
    }
}
