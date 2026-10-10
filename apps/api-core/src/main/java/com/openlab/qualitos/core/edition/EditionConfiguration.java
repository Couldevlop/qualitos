package com.openlab.qualitos.core.edition;

import com.openlab.qualitos.licensing.application.Licensing;
import com.openlab.qualitos.licensing.infrastructure.LicensingFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;

/**
 * L'édition de l'installation et sa licence (ADR 0082).
 *
 * <p>{@code QUALITOS_EDITION} : {@code saas} (défaut) ou {@code onprem}.
 * {@code QUALITOS_LICENSE_FILE} : le fichier de licence monté, en on-premise.
 */
@Configuration
public class EditionConfiguration {

    @Bean
    public Licensing licensing(@Value("${qualitos.edition:saas}") String edition,
                               @Value("${qualitos.license.file:}") String licenseFile,
                               Clock clock) {
        return LicensingFactory.create(edition, licenseFile, clock);
    }

    @Bean
    public MemberLimit memberLimit(Licensing licensing) {
        return MemberLimit.of(licensing);
    }

    /**
     * La garde d'édition, avant la lecture du corps, sur toute l'API. La licence
     * est optionnelle : les tranches {@code @WebMvcTest} chargent cette
     * configuration sans les beans de service — la garde ne fait alors rien.
     */
    @Configuration
    static class Web implements WebMvcConfigurer {

        private final ObjectProvider<Licensing> licensing;

        Web(ObjectProvider<Licensing> licensing) {
            this.licensing = licensing;
        }

        @Override
        public void addInterceptors(@NonNull InterceptorRegistry registry) {
            registry.addInterceptor(new EditionGuard(licensing)).addPathPatterns("/api/**");
        }
    }
}
