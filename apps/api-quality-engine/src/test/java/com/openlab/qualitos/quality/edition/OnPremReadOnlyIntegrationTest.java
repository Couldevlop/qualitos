package com.openlab.qualitos.quality.edition;

import com.openlab.qualitos.licensing.application.Licensing;
import com.openlab.qualitos.licensing.domain.LicenseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le moteur démarré en édition on-premise SANS licence (ADR 0082) : il démarre,
 * se lit, et refuse toute écriture en disant pourquoi. Câblage complet : réglage,
 * fabrique, garde, traduction de l'erreur.
 */
@SpringBootTest(properties = {"qualitos.edition=onprem", "qualitos.license.file="})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("web")
class OnPremReadOnlyIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired Licensing licensing;
    @MockitoBean JwtDecoder jwtDecoder;

    final UUID tenant = UUID.randomUUID();

    @BeforeEach
    void jeton() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(UUID.randomUUID().toString())
                .claim("tenant_id", tenant.toString())
                .claim("realm_access", Map.of("roles", List.of("QUALITY_MANAGER")))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
        when(jwtDecoder.decode(anyString())).thenReturn(jwt);
    }

    @Test
    void sansLicenceLesEcrituresSontRefuseesEtLesLecturesPassent() throws Exception {
        assertThat(licensing.isOnPrem()).isTrue();
        assertThat(licensing.state().status()).isEqualTo(LicenseStatus.MISSING);

        mvc.perform(post("/api/v1/nc").header("Authorization", "Bearer t")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://qualitos.io/errors/license-read-only"))
                .andExpect(jsonPath("$.licenseStatus").value("MISSING"));

        mvc.perform(get("/api/v1/nc").header("Authorization", "Bearer t"))
                .andExpect(status().isOk());
    }
}
