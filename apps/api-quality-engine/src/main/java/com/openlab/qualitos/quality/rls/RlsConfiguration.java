package com.openlab.qualitos.quality.rls;

import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Câblage de l'isolation par la base (ADR 0086) : après chaque migration, sous
 * le rôle propriétaire, le rôle applicatif et les politiques ; à chaque emprunt
 * d'une connexion, le client courant passé à PostgreSQL.
 */
@Configuration(proxyBeanMethods = false)
public class RlsConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RlsConfiguration.class);

    @Bean
    FlywayConfigurationCustomizer rlsFlywayCallback(RlsProperties properties) {
        return configuration -> configuration.callbacks(new RlsCallback(properties));
    }

    @Bean
    static BeanPostProcessor rlsTenantDataSource(org.springframework.core.env.Environment env) {
        boolean enabled = env.getProperty("qualitos.rls.enabled", Boolean.class, false);
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (enabled && bean instanceof DataSource ds && !(bean instanceof TenantAwareDataSource)) {
                    return new TenantAwareDataSource(ds);
                }
                return bean;
            }
        };
    }

    static final class RlsCallback implements Callback {
        private final RlsProperties properties;

        RlsCallback(RlsProperties properties) {
            this.properties = properties;
        }

        @Override
        public boolean supports(Event event, Context context) {
            return event == Event.AFTER_MIGRATE;
        }

        @Override
        public boolean canHandleInTransaction(Event event, Context context) {
            return true;
        }

        @Override
        public void handle(Event event, Context context) {
            try {
                apply(context.getConnection(), properties);
            } catch (SQLException e) {
                throw new IllegalStateException("isolation par la base : préparation impossible", e);
            }
        }

        @Override
        public String getCallbackName() {
            return "qualitos-rls";
        }
    }

    static void apply(Connection owner, RlsProperties properties) throws SQLException {
        if (!"PostgreSQL".equals(owner.getMetaData().getDatabaseProductName())) {
            return; // bancs H2 : rien à poser
        }
        if (properties.hasAppUser()) {
            RlsSchema.applyRole(owner, properties.getAppUser(), properties.getAppPassword());
        }
        List<String> tables = RlsSchema.applyPolicies(owner, properties.isEnabled());
        log.info("isolation par la base {} sur {} tables (rôle applicatif : {})",
                properties.isEnabled() ? "ACTIVE" : "inactive", tables.size(),
                properties.hasAppUser() ? properties.getAppUser() : "propriétaire — non contraint");
    }
}
