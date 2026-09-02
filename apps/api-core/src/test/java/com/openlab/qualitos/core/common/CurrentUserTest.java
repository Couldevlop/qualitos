package com.openlab.qualitos.core.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Couvre les branches de {@link CurrentUser} qu'un test de contrôleur, où le
 * principal JWT est toujours présent et bien formé, n'exerce jamais :
 * contexte de sécurité absent, principal non authentifié, nom vide.
 */
@DisplayName("CurrentUser")
class CurrentUserTest {

    @AfterEach
    void clearSecurityContext() {
        // Le SecurityContextHolder est un ThreadLocal : un test qui le peuple
        // sans le nettoyer polluerait les tests suivants exécutés sur le même
        // thread (execution parallele par classe, voir la config JUnit5).
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("userId() est vide sans contexte de securite")
    void userIdEstVideSansContexte() {
        SecurityContextHolder.clearContext();

        assertThat(CurrentUser.userId()).isEmpty();
    }

    @Test
    @DisplayName("userId() est vide pour un principal non authentifie")
    void userIdEstVidePourUnPrincipalNonAuthentifie() {
        UUID sub = UUID.randomUUID();
        TestingAuthenticationToken nonAuthentifie =
                new TestingAuthenticationToken(sub.toString(), "credentials");
        nonAuthentifie.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(nonAuthentifie);

        assertThat(CurrentUser.userId()).isEmpty();
    }

    @Test
    @DisplayName("userId() est vide pour un principal authentifie au nom vide")
    void userIdEstVidePourUnNomVide() {
        // Troisieme branche du garde : authentifie, mais getName() ne rend rien
        // d'exploitable (ex. un jeton dont le sub est une chaine vide).
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("", null, java.util.List.of()));

        assertThat(CurrentUser.userId()).isEmpty();
    }

    @Test
    @DisplayName("userId() est vide quand le nom du principal est un UUID malforme")
    void userIdEstVidePourUnSubNonUuid() {
        // Cas exact du defaut corrige : un sub de jeton de compte de service,
        // ou tout principal non-JWT dont le nom n'est pas un UUID.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("service-account-mailer", null, java.util.List.of()));

        assertThat(CurrentUser.userId()).isEmpty();
    }

    @Test
    @DisplayName("userId() renvoie le sub type en UUID pour un principal authentifie valide")
    void userIdRenvoieLeSubValide() {
        UUID sub = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(sub.toString(), null, java.util.List.of()));

        assertThat(CurrentUser.userId()).contains(sub);
    }

    @Test
    @DisplayName("requireUserId() renvoie le sub quand il est exploitable")
    void requireUserIdRenvoieLeSubValide() {
        UUID sub = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(sub.toString(), null, java.util.List.of()));

        assertThat(CurrentUser.requireUserId()).isEqualTo(sub);
    }

    @Test
    @DisplayName("requireUserId() refuse proprement quand l'identite n'est pas exploitable")
    void requireUserIdRefuseQuandLIdentiteEstIllisible() {
        // C'est exactement le defaut corrige : sans ce garde-fou,
        // UUID.fromString() levait une IllegalArgumentException non geree qui
        // tombait dans le catch-all -> 500 generique.
        SecurityContextHolder.clearContext();

        assertThatThrownBy(CurrentUser::requireUserId)
                .isInstanceOf(UnresolvableActorException.class);
    }
}
