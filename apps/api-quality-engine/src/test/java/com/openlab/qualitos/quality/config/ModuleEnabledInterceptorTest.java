package com.openlab.qualitos.quality.config;

import com.openlab.qualitos.quality.tenantmodules.application.ModuleActivationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La frontière commerciale des modules, appliquée au serveur (§10.4).
 *
 * <p>Elle n'existait nulle part avant : les modules ne conditionnaient que deux
 * entrées de navigation sur trente-six, et rien n'empêchait un tenant sans
 * abonnement d'appeler l'API. Une porte qu'on se contente de ne pas afficher
 * n'est pas fermée.
 *
 * <p>Deux invariants tiennent ce banc, et le second compte autant que le premier :
 * l'écriture est refusée sans le module, et la LECTURE ne l'est jamais — un
 * enregistrement qualité doit rester opposable après une résiliation, sans quoi
 * une décision commerciale se transforme en perte de conformité.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ModuleEnabledInterceptorTest {

    @Mock ModuleActivationService service;
    @Mock ObjectProvider<ModuleActivationService> provider;

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    private ModuleEnabledInterceptor interceptor() {
        when(provider.getIfAvailable()).thenReturn(service);
        return new ModuleEnabledInterceptor(provider);
    }

    private MockHttpServletRequest requete(String methode) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        r.setMethod(methode);
        return r;
    }

    // ---------- l'écriture ----------

    @Test
    void uneEcritureEstRefuseeSansLeModule() {
        when(service.enabledModuleCodes()).thenReturn(List.of("pdca", "capa"));

        assertThatThrownBy(() -> interceptor()
                .preHandle(requete("POST"), response, handler(Garde.class, "ecrire")))
                .isInstanceOf(ModuleNotEnabledException.class)
                .hasMessageContaining("controlplan");
    }

    @Test
    void laMemeEcriturePasseAvecLeModule() {
        when(service.enabledModuleCodes()).thenReturn(List.of("pdca", "controlplan"));

        assertThatNoException().isThrownBy(() -> interceptor()
                .preHandle(requete("POST"), response, handler(Garde.class, "ecrire")));
    }

    @Test
    void toutesLesEcrituresSontConcernees() {
        when(service.enabledModuleCodes()).thenReturn(List.of("pdca"));

        for (String methode : List.of("POST", "PUT", "PATCH", "DELETE")) {
            assertThatThrownBy(() -> interceptor()
                    .preHandle(requete(methode), response, handler(Garde.class, "ecrire")))
                    .describedAs("methode %s", methode)
                    .isInstanceOf(ModuleNotEnabledException.class);
        }
    }

    // ---------- la lecture, jamais refusée ----------

    @Test
    void laLectureResteOuverteMemeSansLeModule() {
        // L'invariant capital. Un control plan approuvé est un enregistrement
        // opposable : si résilier le module le rendait illisible, le client ne
        // pourrait plus répondre à son auditeur sur un audit déjà passé.
        when(service.enabledModuleCodes()).thenReturn(List.of());

        for (String methode : List.of("GET", "HEAD", "OPTIONS")) {
            assertThatNoException()
                    .describedAs("methode %s", methode)
                    .isThrownBy(() -> interceptor()
                            .preHandle(requete(methode), response, handler(Garde.class, "lire")));
        }
    }

    @Test
    void uneLectureNInterrogeMemePasLaBase() {
        interceptor().preHandle(requete("GET"), response, handler(Garde.class, "lire"));

        verify(service, never()).enabledModuleCodes();
    }

    // ---------- la portée de l'annotation ----------

    @Test
    void unPointDEntreeSansAnnotationNEstPasConcerne() {
        assertThat(interceptor()
                .preHandle(requete("POST"), response, handler(SansGarde.class, "ecrire"))).isTrue();
        verify(service, never()).enabledModuleCodes();
    }

    @Test
    void lAnnotationDeLaMethodeLEmporteSurCelleDeLaClasse() {
        when(service.enabledModuleCodes()).thenReturn(List.of("risk"));

        // La classe exige `controlplan`, la méthode `risk` : c'est `risk` qui compte.
        assertThatNoException().isThrownBy(() -> interceptor()
                .preHandle(requete("POST"), response, handler(Garde.class, "ecrireAutreModule")));
    }

    @Test
    void unHandlerQuiNEstPasUneMethodeDeControleurEstIgnore() {
        assertThat(interceptor().preHandle(requete("POST"), response, "une ressource statique"))
                .isTrue();
    }

    // ---------- le coût ----------

    @Test
    void laListeDesModulesNEstLueQuUneFoisParRequete() {
        // Une annotation de classe ET de méthode ne doit pas payer deux fois un
        // aller-retour en base sur un chemin chaud (SLO §20).
        when(service.enabledModuleCodes()).thenReturn(List.of("controlplan", "risk"));
        ModuleEnabledInterceptor i = interceptor();
        MockHttpServletRequest requete = requete("POST");

        i.preHandle(requete, response, handler(Garde.class, "ecrire"));
        i.preHandle(requete, response, handler(Garde.class, "ecrireAutreModule"));

        verify(service, times(1)).enabledModuleCodes();
    }

    // ---------- l'absence de service ----------

    @Test
    void sansServiceLaGardeSAbstientEtLeDitFranchement() {
        // Cas des tranches @WebMvcTest, qui n'instancient aucun service. La garde
        // s'abstient — mais `isActive()` permet au banc de câblage de constater
        // que le contexte RÉEL, lui, la porte : une absence silencieuse
        // désactiverait la frontière commerciale sans que rien ne le signale.
        when(provider.getIfAvailable()).thenReturn(null);
        ModuleEnabledInterceptor i = new ModuleEnabledInterceptor(provider);

        assertThat(i.isActive()).isFalse();
        assertThat(i.preHandle(requete("POST"), response, handler(Garde.class, "ecrire"))).isTrue();
    }

    @Test
    void avecServiceLaGardeSeDeclareActive() {
        assertThat(interceptor().isActive()).isTrue();
    }

    // ---------- montage ----------

    private static HandlerMethod handler(Class<?> type, String nom) {
        try {
            Object instance = type.getDeclaredConstructor().newInstance();
            for (Method m : type.getDeclaredMethods()) {
                if (m.getName().equals(nom)) {
                    return new HandlerMethod(instance, m);
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        throw new IllegalArgumentException("Methode d'essai introuvable : " + nom);
    }

    @RequiresModule("controlplan")
    @SuppressWarnings("unused")
    public static class Garde {
        public void ecrire() { }
        public void lire() { }
        @RequiresModule("risk")
        public void ecrireAutreModule() { }
    }

    @SuppressWarnings("unused")
    public static class SansGarde {
        public void ecrire() { }
    }
}
