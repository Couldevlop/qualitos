package com.openlab.qualitos.quality.config;

import com.openlab.qualitos.quality.authz.application.AuthorizationService;
import com.openlab.qualitos.licensing.application.Licensing;
import com.openlab.qualitos.quality.authz.web.PermissionInterceptor;
import com.openlab.qualitos.quality.edition.LicenseWriteGuard;
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
    private final PermissionInterceptor permissions;
    private final ModuleEnabledInterceptor moduleEnabled;
    private final LicenseWriteGuard licenseWriteGuard;

    public WebMvcConfig(MethodAuthorizationPreCheckInterceptor authorizationPreCheck,
                        PermissionInterceptor permissions,
                        ModuleEnabledInterceptor moduleEnabled,
                        LicenseWriteGuard licenseWriteGuard) {
        this.authorizationPreCheck = authorizationPreCheck;
        this.permissions = permissions;
        this.moduleEnabled = moduleEnabled;
        this.licenseWriteGuard = licenseWriteGuard;
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
     * Les actions réglables par client (ADR 0078). Service optionnel, pour la
     * même raison que les modules : les tranches {@code @WebMvcTest} ne
     * l'instancient pas — l'intercepteur applique alors les droits livrés.
     */
    @Bean
    public static PermissionInterceptor permissionInterceptor(ObjectProvider<AuthorizationService> service) {
        return new PermissionInterceptor(service);
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
     * La lecture seule d'une installation on-premise sans licence valable (ADR 0082).
     * Licence optionnelle, comme les autres services : les tranches de test ne la
     * portent pas, et la garde ne fait alors rien.
     */
    @Bean
    public static LicenseWriteGuard licenseWriteGuard(ObjectProvider<Licensing> licensing) {
        return new LicenseWriteGuard(licensing);
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
        registry.addInterceptor(permissions).addPathPatterns("/api/**");
        // Après les droits, comme le module : un utilisateur sans droit n'apprend
        // rien de l'état de la licence en tapant sur une porte qui lui est fermée.
        registry.addInterceptor(licenseWriteGuard).addPathPatterns("/api/**");
        registry.addInterceptor(moduleEnabled).addPathPatterns("/api/**");
    }
}
