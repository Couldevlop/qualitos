package com.openlab.qualitos.quality.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Décide l'autorisation d'une méthode de contrôleur AVANT que le corps de la
 * requête ne soit lié et validé (OWASP A01 — « fail securely », ASVS V4).
 *
 * <h2>Le défaut corrigé</h2>
 *
 * <p>Spring MVC résout et valide les arguments — dont {@code @Valid @RequestBody} —
 * dans l'adaptateur de handler, c'est-à-dire AVANT d'invoquer la méthode, donc
 * avant l'advice qui porte {@code @PreAuthorize}. Un appelant sans habilitation
 * qui envoyait un corps malformé recevait donc <b>400</b> et non 403 : la
 * plateforme lui répondait sur la forme de sa charge utile alors qu'elle aurait
 * dû refuser de le lire. La réponse variait selon le contenu envoyé, ce qui
 * transforme un endpoint fermé en oracle de validation — et fait porter à
 * l'analyse d'un corps hostile un travail qui n'aurait jamais dû commencer.
 *
 * <h2>Pourquoi ici, et pas une règle d'URL de plus</h2>
 *
 * <p>La chaîne de filtres ({@link SecurityConfig}) s'exécute bien avant MVC et
 * corrige l'ordre — mais au prix d'une <b>seconde source de vérité</b> : il
 * faudrait y recopier les soixante-dix règles de rôle portées par les
 * contrôleurs, et les deux copies divergeraient au premier endpoint ajouté. Un
 * écart entre elles est précisément la faille qu'on prétend fermer. La chaîne
 * garde donc les règles <i>transverses</i> (méthode HTTP, familles de chemins) ;
 * cet intercepteur, lui, ne fait que <b>lire l'annotation existante plus tôt</b>.
 * Une seule déclaration, deux moments d'application.
 *
 * <h2>Il ne peut que refuser plus tôt</h2>
 *
 * <p>Invariant tenu par construction : {@code @PreAuthorize} reste en place et
 * s'exécute ensuite. Cet intercepteur n'autorise rien — il ne sait que jeter
 * {@link AccessDeniedException}. Une erreur de son évaluation ne peut donc pas
 * ouvrir un accès ; au pire elle en ferme un, et le banc de test le voit. C'est
 * ce qui permet de l'ajouter à un système en service sans en refaire l'audit.
 *
 * <p>L'évaluation est déléguée à {@link WebExpressionAuthorizationManager}, le
 * moteur d'expressions de Spring Security lui-même : réécrire {@code hasAnyRole}
 * à la main aurait rendu possible un désaccord sur le préfixe {@code ROLE_} ou
 * sur les alias de rôle, c'est-à-dire deux verdicts d'autorisation différents
 * pour un même jeton.
 *
 * <p><b>Expressions ignorées.</b> Celles qui parlent des ARGUMENTS de la méthode
 * ({@code #id}, {@code returnObject}, {@code filterObject}) ne peuvent pas être
 * évaluées avant que ces arguments n'existent — les pré-évaluer demanderait de
 * lier le corps, c'est-à-dire exactement ce qu'on veut éviter. Elles sont
 * laissées à {@code @PreAuthorize}, qui les applique comme avant. Le moteur
 * qualité n'en compte aucune aujourd'hui ; la garde est là pour le jour où l'une
 * apparaîtra, afin qu'elle échoue en « pas de pré-contrôle » et jamais en
 * « autorisé ».
 */
public class MethodAuthorizationPreCheckInterceptor implements HandlerInterceptor {

    /**
     * Marqueurs d'une expression qui dépend des arguments de la méthode.
     * {@code #} couvre {@code #id}, {@code #request}, {@code #this}.
     */
    private static final String[] ARGUMENT_BOUND = {"#", "returnObject", "filterObject"};

    /**
     * Une expression compilée par méthode de contrôleur.
     *
     * <p>Compiler la même SpEL à chaque requête coûterait sur un chemin chaud
     * (SLO §20 : p95 &lt; 300 ms) ce que l'annotation, elle, ne change jamais.
     * {@link Optional#empty()} mémorise aussi les méthodes SANS règle
     * pré-évaluable, pour ne pas refaire la recherche d'annotation à chaque appel.
     */
    private final Map<HandlerMethod, Optional<WebExpressionAuthorizationManager>> compiled =
            new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        Optional<WebExpressionAuthorizationManager> manager =
                compiled.computeIfAbsent(method, MethodAuthorizationPreCheckInterceptor::compile);
        if (manager.isEmpty()) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            // Aucune authentification établie : c'est à la chaîne de filtres de
            // répondre 401. Se prononcer ici rendrait 403 là où il faut 401 —
            // « je ne sais pas qui tu es » n'est pas « tu n'as pas le droit ».
            return true;
        }

        Supplier<Authentication> supplier = () -> authentication;
        AuthorizationDecision decision = manager.get()
                .check(supplier, new RequestAuthorizationContext(request));

        // `null` = le gestionnaire s'abstient. Une abstention n'est pas un refus :
        // on laisse @PreAuthorize trancher, comme avant cet intercepteur.
        if (decision != null && !decision.isGranted()) {
            throw new AccessDeniedException("Access denied");
        }
        return true;
    }

    /**
     * Rend l'expression pré-évaluable de la méthode, ou {@link Optional#empty()}.
     *
     * <p>L'annotation est cherchée sur la méthode PUIS sur sa classe : un
     * contrôleur peut porter la sienne pour tous ses points d'entrée, et celle de
     * la méthode l'emporte — même précédence que Spring Security, sans quoi le
     * pré-contrôle et le contrôle final ne jugeraient pas la même règle.
     */
    private static Optional<WebExpressionAuthorizationManager> compile(HandlerMethod method) {
        PreAuthorize annotation = AnnotatedElementUtils.findMergedAnnotation(
                method.getMethod(), PreAuthorize.class);
        if (annotation == null) {
            annotation = AnnotatedElementUtils.findMergedAnnotation(
                    method.getBeanType(), PreAuthorize.class);
        }
        if (annotation == null) {
            return Optional.empty();
        }
        String expression = annotation.value();
        for (String marker : ARGUMENT_BOUND) {
            if (expression.contains(marker)) {
                return Optional.empty();
            }
        }
        try {
            return Optional.of(new WebExpressionAuthorizationManager(expression));
        } catch (RuntimeException notWebEvaluable) {
            // Une expression que le moteur web ne sait pas compiler reste
            // intégralement traitée par @PreAuthorize. Échouer ici ouvrirait un
            // accès ; s'abstenir n'en ouvre aucun.
            return Optional.empty();
        }
    }
}
