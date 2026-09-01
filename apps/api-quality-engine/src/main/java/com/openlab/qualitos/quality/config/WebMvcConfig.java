package com.openlab.qualitos.quality.config;

import com.openlab.qualitos.quality.tenantmodules.application.ModuleActivationService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration de la couche web du moteur qualité.
 *
 * <p>Le seul intercepteur enregistré ici décide l'autorisation d'une méthode de
 * contrôleur avant que le corps de la requête ne soit lu et validé — voir
 * {@link MethodAuthorizationPreCheckInterceptor} pour le défaut qu'il ferme.
 *
 * <p>Il s'applique à {@code /api/**} et à rien d'autre : la sonde de vivacité et
 * la documentation OpenAPI n'ont pas de règle de rôle à pré-évaluer, et un
 * intercepteur sur {@code /**} ferait payer une recherche d'annotation à chaque
 * appel de {@code /actuator/health} — plusieurs fois par minute et par pod.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final MethodAuthorizationPreCheckInterceptor authorizationPreCheck;
    private final ModuleEnabledInterceptor moduleEnabled;

    public WebMvcConfig(MethodAuthorizationPreCheckInterceptor authorizationPreCheck,
                        ModuleEnabledInterceptor moduleEnabled) {
        this.authorizationPreCheck = authorizationPreCheck;
        this.moduleEnabled = moduleEnabled;
    }

    /**
     * Exposé en bean pour être injectable dans un banc de test, et pour que son
     * cache d'expressions compilées soit unique dans le contexte.
     */
    @Bean
    public static MethodAuthorizationPreCheckInterceptor methodAuthorizationPreCheckInterceptor() {
        return new MethodAuthorizationPreCheckInterceptor();
    }

    /**
     * Bean séparé pour rester injectable en test et n'avoir qu'un cache
     * d'annotations dans le contexte.
     */
    @Bean
    public static ModuleEnabledInterceptor moduleEnabledInterceptor(
            ObjectProvider<ModuleActivationService> modules) {
        return new ModuleEnabledInterceptor(modules);
    }

    /**
     * L'ORDRE compte. Le rôle se décide avant le module : « tu n'as pas le droit
     * de faire cela » prime sur « ton organisation n'a pas souscrit cela ».
     * L'inverse révélerait à un utilisateur sans droits quels modules le tenant
     * a souscrits — une information sur l'abonnement, obtenue en tapant sur une
     * porte qui lui est de toute façon fermée.
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(authorizationPreCheck).addPathPatterns("/api/**");
        registry.addInterceptor(moduleEnabled).addPathPatterns("/api/**");
    }
}
