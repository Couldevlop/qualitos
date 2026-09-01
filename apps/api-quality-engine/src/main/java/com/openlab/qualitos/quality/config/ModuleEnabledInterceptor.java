package com.openlab.qualitos.quality.config;

import com.openlab.qualitos.quality.tenantmodules.application.ModuleActivationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Refuse les ÉCRITURES d'un point d'entrée dont le tenant n'a pas souscrit le
 * module (§10.4). Voir {@link RequiresModule} pour la règle et ses raisons.
 *
 * <h2>Pourquoi un intercepteur et pas une règle d'URL</h2>
 *
 * <p>Même raison que pour l'autorisation (ADR 0065) : la chaîne de filtres
 * ignore quel module porte quel chemin, et l'y recopier créerait une seconde
 * source de vérité que le premier endpoint ajouté ferait diverger. La
 * déclaration reste sur le contrôleur, à côté du code qu'elle protège ; elle est
 * seulement <b>lue plus tôt</b>, avant que le corps ne soit lié — inutile de
 * désérialiser une charge utile qu'on va refuser.
 *
 * <h2>Ce qui est lu, et combien de fois</h2>
 *
 * <p>La liste des modules du tenant vient de la base. Elle est mémorisée pour la
 * DURÉE DE LA REQUÊTE : un contrôleur peut porter l'annotation sur sa classe et
 * sur sa méthode, et interroger deux fois la base pour la même réponse serait
 * payer deux fois un chemin chaud (SLO §20 : p95 &lt; 300 ms). Elle n'est PAS
 * mémorisée au-delà : une activation doit prendre effet immédiatement, c'est la
 * promesse de la « désactivation à chaud » (§10.4).
 */
public class ModuleEnabledInterceptor implements HandlerInterceptor {

    /** Clé de mémorisation pour la durée d'une requête. */
    private static final String CACHE_ATTRIBUTE =
            ModuleEnabledInterceptor.class.getName() + ".modules";

    /**
     * Méthodes qui ne modifient rien.
     *
     * <p>Elles passent toujours : un enregistrement qualité doit rester lisible
     * après la résiliation du module qui l'a produit.
     */
    private static final Set<String> READ_ONLY = Set.of("GET", "HEAD", "OPTIONS");

    /** Le module déclaré par méthode de contrôleur, cherché une fois. */
    private final Map<HandlerMethod, Optional<String>> declared = new ConcurrentHashMap<>();

    /**
     * Le service est OPTIONNEL, et il faut dire pourquoi.
     *
     * <p>Cette configuration web est chargée par les quatre-vingt-cinq tranches
     * {@code @WebMvcTest} du moteur, qui n'instancient aucun bean de service :
     * l'exiger empêcherait leur contexte de démarrer. Une tranche teste un
     * contrôleur, pas la licence du tenant — s'y abstenir est correct.
     *
     * <p>Le contexte complet, lui, le porte toujours ({@code @Service}), et le
     * banc {@code ModuleEnabledInterceptorWiringTest} le vérifie sur le contexte
     * réel : sans lui, une absence silencieuse désactiverait la frontière
     * commerciale sans que rien ne le signale.
     */
    private final ObjectProvider<ModuleActivationService> modules;

    public ModuleEnabledInterceptor(ObjectProvider<ModuleActivationService> modules) {
        this.modules = modules;
    }

    /** Vrai quand la garde est réellement en mesure de refuser. */
    public boolean isActive() {
        return modules.getIfAvailable() != null;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        if (READ_ONLY.contains(request.getMethod())) {
            return true;
        }
        Optional<String> code = declared.computeIfAbsent(method, ModuleEnabledInterceptor::declaredOn);
        if (code.isEmpty()) {
            return true;
        }
        ModuleActivationService service = modules.getIfAvailable();
        if (service == null) {
            return true;
        }
        if (!enabledFor(request, service).contains(code.get())) {
            throw new ModuleNotEnabledException(code.get());
        }
        return true;
    }

    /**
     * Le module déclaré sur la méthode, sinon sur sa classe.
     *
     * <p>Même précédence que {@code @PreAuthorize} : un contrôleur annonce le
     * module de tous ses points d'entrée, une méthode peut en exiger un autre.
     */
    private static Optional<String> declaredOn(HandlerMethod method) {
        RequiresModule annotation = AnnotatedElementUtils.findMergedAnnotation(
                method.getMethod(), RequiresModule.class);
        if (annotation == null) {
            annotation = AnnotatedElementUtils.findMergedAnnotation(
                    method.getBeanType(), RequiresModule.class);
        }
        return annotation == null ? Optional.empty() : Optional.of(annotation.value());
    }

    @SuppressWarnings("unchecked")
    private List<String> enabledFor(HttpServletRequest request, ModuleActivationService service) {
        Object cached = request.getAttribute(CACHE_ATTRIBUTE);
        if (cached != null) {
            return (List<String>) cached;
        }
        List<String> codes = service.enabledModuleCodes();
        request.setAttribute(CACHE_ATTRIBUTE, codes);
        return codes;
    }
}
