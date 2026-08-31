package com.openlab.qualitos.quality.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * L'autorisation se décide AVANT que le corps de la requête ne soit lu.
 *
 * <p>Le défaut que ces bancs verrouillent est un défaut d'ORDRE, pas de règle :
 * la règle était juste, mais Spring MVC validait la charge utile avant de
 * l'appliquer, et un appelant sans habilitation recevait 400 au lieu de 403 —
 * une réponse qui varie selon ce qu'il envoie, donc un oracle de validation
 * offert sur un point d'entrée fermé (OWASP A01).
 *
 * <p>L'invariant capital est le dernier de ce fichier : cet intercepteur ne sait
 * que REFUSER. S'il se trompait, il fermerait un accès — jamais il n'en ouvre un.
 */
class MethodAuthorizationPreCheckInterceptorTest {

    private final MethodAuthorizationPreCheckInterceptor interceptor =
            new MethodAuthorizationPreCheckInterceptor();

    private final HttpServletRequest request = new MockHttpServletRequest();
    private final HttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // ---------- le refus, avant toute lecture du corps ----------

    @Test
    void aCallerWithoutTheRoleIsRefusedBeforeTheBodyIsEvenLookedAt() {
        authenticateWith("ROLE_QUALITY_MANAGER");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler("editorsOnly")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void aCallerWithTheRoleIsLetThrough() {
        authenticateWith("ROLE_DIRECTOR_QUALITY");

        assertThatNoException().isThrownBy(
                () -> interceptor.preHandle(request, response, handler("editorsOnly")));
    }

    @Test
    void anyOneOfTheAcceptedRolesSuffices() {
        authenticateWith("ROLE_SUPER_ADMIN");

        assertThatNoException().isThrownBy(
                () -> interceptor.preHandle(request, response, handler("editorsOnly")));
    }

    // ---------- ce sur quoi il ne se prononce pas ----------

    @Test
    void aMethodWithoutRuleIsNotItsBusiness() {
        authenticateWith("ROLE_USER");

        assertThat(interceptor.preHandle(request, response, handler("noRuleAtAll"))).isTrue();
    }

    @Test
    void anExpressionThatSpeaksOfMethodArgumentsIsLeftToPreAuthorize() {
        // Elle ne PEUT pas être pré-évaluée : ses arguments n'existent pas encore,
        // et les faire exister demanderait de lier le corps — précisément ce que
        // le pré-contrôle évite. @PreAuthorize l'applique comme avant.
        authenticateWith("ROLE_USER");

        assertThat(interceptor.preHandle(request, response, handler("boundToItsArgument"))).isTrue();
    }

    @Test
    void withoutAnyAuthenticationItStandsAsideSoTheChainCanAnswer401() {
        // « Je ne sais pas qui tu es » n'est pas « tu n'as pas le droit » : se
        // prononcer ici rendrait 403 là où la chaîne de filtres doit rendre 401.
        assertThat(interceptor.preHandle(request, response, handler("editorsOnly"))).isTrue();
    }

    @Test
    void aHandlerThatIsNotAControllerMethodIsIgnored() {
        authenticateWith("ROLE_USER");

        assertThat(interceptor.preHandle(request, response, "une ressource statique")).isTrue();
    }

    // ---------- l'héritage de la règle portée par la classe ----------

    @Test
    void theRuleCarriedByTheControllerAppliesToAMethodThatHasNone() {
        authenticateWith("ROLE_USER");

        assertThatThrownBy(() ->
                interceptor.preHandle(request, response, guardedHandler("inheritsTheClassRule")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void theRuleOnTheMethodWinsOverTheOneOnTheClass() {
        // Sans cette précédence, le pré-contrôle et @PreAuthorize jugeraient deux
        // règles différentes pour le même appel — et le verdict dépendrait de
        // lequel des deux s'exécute.
        authenticateWith("ROLE_USER");

        assertThatNoException().isThrownBy(() ->
                interceptor.preHandle(request, response, guardedHandler("overridesWithAnOpenRule")));
    }

    // ---------- l'invariant : il ne peut que refuser ----------

    @Test
    void itNeverGrantsWhatTheRuleRefuses() {
        // Balayage de toutes les portes du contrôleur d'essai : pour chacune, le
        // pré-contrôle laisse passer AU PLUS ce que la règle autorise. C'est ce
        // qui permet d'ajouter cet intercepteur à un système en service sans en
        // refaire l'audit : au pire il ferme, jamais il n'ouvre.
        authenticateWith("ROLE_USER");

        for (String door : List.of("editorsOnly", "adminsOnly")) {
            assertThatThrownBy(() -> interceptor.preHandle(request, response, handler(door)))
                    .describedAs("porte %s ouverte a un simple utilisateur", door)
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // ---------- montage ----------

    private void authenticateWith(String authority) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("someone", "n/a",
                        List.of(new SimpleGrantedAuthority(authority))));
    }

    private HandlerMethod handler(String methodName) {
        return handlerOn(new OpenController(), methodName);
    }

    private HandlerMethod guardedHandler(String methodName) {
        return handlerOn(new GuardedController(), methodName);
    }

    private static HandlerMethod handlerOn(Object controller, String methodName) {
        for (Method method : controller.getClass().getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return new HandlerMethod(controller, method);
            }
        }
        throw new IllegalArgumentException("Methode d'essai introuvable : " + methodName);
    }

    /** Contrôleur d'essai sans règle de classe : chaque méthode porte la sienne. */
    @SuppressWarnings("unused")
    static class OpenController {

        @PreAuthorize("hasAnyRole('DIRECTOR_QUALITY','QUALITY_DIRECTOR','ADMIN_TENANT','SUPER_ADMIN')")
        public void editorsOnly() {
        }

        @PreAuthorize("hasRole('SUPER_ADMIN')")
        public void adminsOnly() {
        }

        @PreAuthorize("hasRole('SUPER_ADMIN') and #id != null")
        public void boundToItsArgument(String id) {
        }

        public void noRuleAtAll() {
        }
    }

    /** Contrôleur d'essai dont la règle est portée par la CLASSE. */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @SuppressWarnings("unused")
    static class GuardedController {

        public void inheritsTheClassRule() {
        }

        @PreAuthorize("isAuthenticated()")
        public void overridesWithAnOpenRule() {
        }
    }
}
