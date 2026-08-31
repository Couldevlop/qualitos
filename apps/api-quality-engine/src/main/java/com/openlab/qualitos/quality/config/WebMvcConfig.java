package com.openlab.qualitos.quality.config;

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

    public WebMvcConfig(MethodAuthorizationPreCheckInterceptor authorizationPreCheck) {
        this.authorizationPreCheck = authorizationPreCheck;
    }

    /**
     * Exposé en bean pour être injectable dans un banc de test, et pour que son
     * cache d'expressions compilées soit unique dans le contexte.
     */
    @Bean
    public static MethodAuthorizationPreCheckInterceptor methodAuthorizationPreCheckInterceptor() {
        return new MethodAuthorizationPreCheckInterceptor();
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(authorizationPreCheck).addPathPatterns("/api/**");
    }
}
