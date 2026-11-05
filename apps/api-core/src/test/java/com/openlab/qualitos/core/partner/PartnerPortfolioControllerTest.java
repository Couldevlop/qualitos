package com.openlab.qualitos.core.partner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le rattachement d'un compte à sa fiche de consultant.
 *
 * <p>Ce que ces bancs tiennent : <b>le courriel qui désigne la fiche doit être
 * vérifié par le fournisseur d'identité</b>. C'est le courriel, et lui seul, qui
 * décide de quelle fiche un compte hérite — donc de quel portefeuille de clients.
 * Un courriel simplement déclaré laisserait quiconque connaît l'adresse d'un
 * consultant s'inscrire sous cette adresse et hériter de tous ses accès.
 *
 * <p>On échoue fermé : sans {@code email_verified} à vrai, rien n'est rattaché, et
 * la personne est dans le même cas que celle qu'aucune fiche n'attend.
 */
class PartnerPortfolioControllerTest {

    private PartnerAccessService service;
    private PartnerPortfolioController controller;

    @BeforeEach
    void setUp() {
        service = mock(PartnerAccessService.class);
        controller = new PartnerPortfolioController(service);
    }

    private static Jwt jeton(Map<String, Object> claims) {
        return new Jwt("jeton", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("alg", "none"), claims);
    }

    @Test
    @DisplayName("un courriel vérifié rattache la fiche qui l'attend")
    void courriel_verifie_rattache() {
        PartnerConsultant fiche = PartnerConsultant.builder()
                .displayName("Claire Martin").email("claire@qualiconseil.fr").active(true).build();
        when(service.linkAccount(any(), eq("claire@qualiconseil.fr"))).thenReturn(Optional.of(fiche));

        ResponseEntity<PartnerDto.ConsultantResponse> reponse = controller.link(jeton(Map.of(
                "sub", "u-1", "email", "claire@qualiconseil.fr", "email_verified", true)));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody()).isNotNull();
        assertThat(reponse.getBody().email()).isEqualTo("claire@qualiconseil.fr");
    }

    @Test
    @DisplayName("un courriel NON vérifié ne rattache rien — le service ne le voit même pas")
    void courriel_non_verifie_ne_rattache_rien() {
        when(service.linkAccount(any(), isNull())).thenReturn(Optional.empty());

        ResponseEntity<PartnerDto.ConsultantResponse> reponse = controller.link(jeton(Map.of(
                "sub", "u-1", "email", "claire@qualiconseil.fr", "email_verified", false)));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        // Le courriel n'est PAS transmis : le contrôleur le retient, plutôt que
        // de laisser le service décider sur une donnée non attestée.
        verify(service).linkAccount(any(), isNull());
    }

    @Test
    @DisplayName("un jeton sans la revendication de vérification ne rattache rien non plus")
    void revendication_absente_ne_rattache_rien() {
        when(service.linkAccount(any(), isNull())).thenReturn(Optional.empty());

        // Absence n'est pas consentement : un fournisseur d'identité qui ne dit
        // rien ne dit pas « vérifié ».
        ResponseEntity<PartnerDto.ConsultantResponse> reponse = controller.link(jeton(Map.of(
                "sub", "u-1", "email", "claire@qualiconseil.fr")));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(service).linkAccount(any(), isNull());
    }

    @Test
    @DisplayName("aucun jeton : rien n'est rattaché, et rien n'explose")
    void sans_jeton() {
        when(service.linkAccount(any(), isNull())).thenReturn(Optional.empty());

        assertThat(controller.link(null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("le portefeuille est celui du compte du contexte, jamais d'un identifiant reçu")
    void portefeuille_du_compte_du_contexte() {
        when(service.portfolioOf(any())).thenReturn(java.util.List.of());

        ResponseEntity<java.util.List<PartnerDto.EngagementResponse>> reponse = controller.portfolio();

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody()).isEmpty();
    }
}
