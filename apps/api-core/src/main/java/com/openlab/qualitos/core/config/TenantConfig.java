package com.openlab.qualitos.core.config;

import com.openlab.qualitos.core.security.TenantContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configuration Hibernate pour le multi-tenancy.
 *
 * <p>Stratégie retenue : DISCRIMINATOR (Row-Level Security PostgreSQL).
 * Hibernate injecte automatiquement le filtre {@code tenantFilter} sur toutes
 * les entités annotées {@code @Filter(name = "tenantFilter")}.
 *
 * <p>Le tenant_id courant est résolu via {@link TenantContext} (ThreadLocal
 * peuplé par {@link com.openlab.qualitos.core.security.TenantJwtFilter}).
 */
@Configuration
public class TenantConfig {

    /**
     * Résout l'identifiant du tenant courant depuis le ThreadLocal.
     * Utilisé par Hibernate pour appliquer le filtre de tenant sur les requêtes.
     */
    @Bean
    public CurrentTenantIdentifierResolver<String> currentTenantIdentifierResolver() {
        return new CurrentTenantIdentifierResolver<>() {

            @Override
            public String resolveCurrentTenantIdentifier() {
                String tenantId = TenantContext.getTenantId();
                // Retourne "NONE" si pas de tenant (endpoints publics comme /actuator)
                // Le filtre Hibernate sera activé uniquement pour les entités annotées
                return tenantId != null ? tenantId : "NONE";
            }

            @Override
            public boolean validateExistingCurrentSessions() {
                return false;
            }
        };
    }

    /**
     * Active les filtres Hibernate nécessaires au multi-tenancy.
     * Le filtre "tenantFilter" est appliqué automatiquement sur chaque session
     * Hibernate via un intercepteur (voir TenantFilterInterceptor).
     */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(
            CurrentTenantIdentifierResolver<String> resolver) {
        return properties -> properties.put(
                AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    }

    /**
     * Noms des filtres Hibernate déclarés sur les entités multi-tenant.
     */
    public static final class FilterNames {
        public static final String TENANT_FILTER = "tenantFilter";
        public static final String TENANT_FILTER_PARAM = "tenantId";

        private FilterNames() {}
    }

    /**
     * Paramètre de filtre Hibernate utilisé dans les annotations @Filter.
     */
    public static Map<String, Object> tenantFilterParams(String tenantId) {
        return Map.of(FilterNames.TENANT_FILTER_PARAM, tenantId);
    }
}
