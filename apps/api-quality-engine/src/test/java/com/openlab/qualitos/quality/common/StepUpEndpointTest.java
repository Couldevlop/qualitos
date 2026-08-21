package com.openlab.qualitos.quality.common;

import com.openlab.qualitos.quality.controlplan.application.ControlPlanDto;
import com.openlab.qualitos.quality.controlplan.application.ControlPlanService;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanPhase;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanStatus;
import com.openlab.qualitos.quality.controlplan.web.ControlPlanController;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La chaîne complète, avec la VRAIE garde et un vrai jeton : rôle habilité mais
 * jeton sans second facteur → 403 ; le même rôle avec {@code acr} relevé → 200.
 *
 * <p>Les autres bancs doublent la garde pour se concentrer sur ce qu'ils
 * vérifient. Celui-ci existe pour qu'un doublon trop complaisant ne cache jamais
 * une garde débranchée — le seul défaut qui compte vraiment ici.
 */
@Tag("web")
@WebMvcTest(controllers = ControlPlanController.class)
@Import({ MethodSecurityTestConfig.class, StepUpGuard.class, StepUpProperties.class })
class StepUpEndpointTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ControlPlanService service;

    static final UUID PRODUCT = UUID.randomUUID();
    static final UUID PLAN = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-08-19T08:00:00Z");
    static final String URL = "/api/v1/products/{p}/control-plans/{c}/approve";

    static final ControlPlanDto.View VIEW = new ControlPlanDto.View(
            PLAN, PRODUCT, ControlPlanPhase.PRODUCTION, "CP-4471", 1,
            ControlPlanStatus.ACTIVE, null, null, NOW, NOW, NOW,
            "0f5a", "tx-0001");

    @Test
    void aQualityDirectorWithoutASecondFactorCannotApprove() throws Exception {
        mockMvc.perform(post(URL, PRODUCT, PLAN).with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DIRECTOR_QUALITY"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://qualitos.io/errors/step-up-required"));

        verifyNoInteractions(service);
    }

    @Test
    void theSameDirectorApprovesOnceTheTokenCarriesTheSecondFactor() throws Exception {
        when(service.approve(PRODUCT, PLAN)).thenReturn(VIEW);

        mockMvc.perform(post(URL, PRODUCT, PLAN).with(csrf())
                        .with(jwt()
                                .jwt(token -> token.claim("acr", "2"))
                                .authorities(new SimpleGrantedAuthority("ROLE_DIRECTOR_QUALITY"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CP-4471"));
    }

    @Test
    void anAmrClaimWorksJustAsWellWhenTheRealmPublishesThatOne() throws Exception {
        when(service.approve(PRODUCT, PLAN)).thenReturn(VIEW);

        mockMvc.perform(post(URL, PRODUCT, PLAN).with(csrf())
                        .with(jwt()
                                .jwt(token -> token.claim("amr", java.util.List.of("pwd", "otp")))
                                .authorities(new SimpleGrantedAuthority("ROLE_DIRECTOR_QUALITY"))))
                .andExpect(status().isOk());
    }

    @Test
    void aSecondFactorDoesNotReplaceTheRole() throws Exception {
        // L'ordre des deux contrôles n'a pas d'importance pour l'utilisateur, mais
        // il en a pour nous : un OTP ne doit jamais valoir habilitation.
        mockMvc.perform(post(URL, PRODUCT, PLAN).with(csrf())
                        .with(jwt()
                                .jwt(token -> token.claim("acr", "2"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }
}
