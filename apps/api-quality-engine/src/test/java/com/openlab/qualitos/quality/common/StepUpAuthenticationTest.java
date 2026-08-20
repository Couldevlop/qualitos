package com.openlab.qualitos.quality.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La preuve qu'un second facteur a été présenté ne se déduit pas d'un rôle : un
 * directeur qualité authentifié par mot de passe seul porte le même rôle qu'un
 * directeur qualité authentifié par mot de passe ET code TOTP. Seul le jeton fait
 * la différence, et il la fait par ses revendications {@code acr} et {@code amr}.
 *
 * <p>Les deux sont lues, parce qu'aucune n'est garantie : Keycloak publie
 * {@code acr} quand une carte de niveaux d'authentification est configurée, et
 * {@code amr} quand un protocol mapper le pose. Accepter les deux évite de rendre
 * le contrôle dépendant d'un détail de configuration du realm.
 */
class StepUpAuthenticationTest {

    private static final StepUpProperties PROPERTIES = properties();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aTokenWithoutAnyStepUpClaimDoesNotSatisfyTheRequirement() {
        givenJwt(Map.of("sub", "u"));

        assertThat(StepUpAuthentication.satisfied(PROPERTIES)).isFalse();
    }

    @Test
    void theConfiguredAcrLevelSatisfiesTheRequirement() {
        givenJwt(Map.of("sub", "u", "acr", "2"));

        assertThat(StepUpAuthentication.satisfied(PROPERTIES)).isTrue();
    }

    @Test
    void aLowerAcrLevelDoesNot() {
        // « 1 » est le niveau d'une authentification par mot de passe seul :
        // l'accepter viderait le contrôle de sa substance.
        givenJwt(Map.of("sub", "u", "acr", "1"));

        assertThat(StepUpAuthentication.satisfied(PROPERTIES)).isFalse();
    }

    @Test
    void anAcrNameSatisfiesItTooBecauseARealmMayMapLevelsToNames() {
        givenJwt(Map.of("sub", "u", "acr", "gold"));

        assertThat(StepUpAuthentication.satisfied(PROPERTIES)).isTrue();
    }

    @Test
    void theAcrComparisonIgnoresCaseAndSurroundingSpaces() {
        givenJwt(Map.of("sub", "u", "acr", "  GOLD  "));

        assertThat(StepUpAuthentication.satisfied(PROPERTIES)).isTrue();
    }

    @Test
    void anAmrEntrySatisfiesItWhenTheRealmPublishesThatClaimInstead() {
        givenJwt(Map.of("sub", "u", "amr", List.of("pwd", "otp")));

        assertThat(StepUpAuthentication.satisfied(PROPERTIES)).isTrue();
    }

    @Test
    void anAmrThatOnlyMentionsAPasswordDoesNot() {
        givenJwt(Map.of("sub", "u", "amr", List.of("pwd")));

        assertThat(StepUpAuthentication.satisfied(PROPERTIES)).isFalse();
    }

    @Test
    void anAmrPublishedAsASingleStringIsReadJustTheSame() {
        // Certains fournisseurs sérialisent une revendication à valeur unique en
        // chaîne plutôt qu'en tableau. Le contrôle ne doit pas dépendre de ce choix.
        givenJwt(Map.of("sub", "u", "amr", "otp"));

        assertThat(StepUpAuthentication.satisfied(PROPERTIES)).isTrue();
    }

    @Test
    void anAuthenticationThatIsNotABearerTokenNeverSatisfiesIt() {
        // Un test qui monterait une authentification maison ne doit pas ouvrir la
        // porte : sans jeton, il n'y a aucune preuve à lire.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u", "p", List.of()));

        assertThat(StepUpAuthentication.satisfied(PROPERTIES)).isFalse();
    }

    @Test
    void anEmptySecurityContextNeverSatisfiesIt() {
        SecurityContextHolder.clearContext();

        assertThat(StepUpAuthentication.satisfied(PROPERTIES)).isFalse();
    }

    @Test
    void disablingEnforcementSatisfiesEverythingAndSaysSoLoudly() {
        // L'interrupteur existe pour un environnement dont le realm n'expose pas
        // encore la revendication. Il ouvre tout : c'est précisément pour cela
        // qu'il vaut `true` par défaut.
        StepUpProperties open = new StepUpProperties();
        open.setEnforced(false);
        givenJwt(Map.of("sub", "u"));

        assertThat(StepUpAuthentication.satisfied(open)).isTrue();
    }

    @Test
    void theDefaultsEnforceAndAcceptTheUsualSecondFactors() {
        StepUpProperties defaults = new StepUpProperties();

        assertThat(defaults.isEnforced()).isTrue();
        assertThat(defaults.getAcceptedAcr()).contains("2");
        assertThat(defaults.getAcceptedAmr()).contains("otp");
    }

    private static void givenJwt(Map<String, Object> claims) {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("alg", "none"), claims);
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of()));
    }

    private static StepUpProperties properties() {
        StepUpProperties props = new StepUpProperties();
        props.setEnforced(true);
        props.setAcceptedAcr(List.of("2", "3", "gold"));
        props.setAcceptedAmr(List.of("otp", "mfa", "hwk"));
        return props;
    }
}
