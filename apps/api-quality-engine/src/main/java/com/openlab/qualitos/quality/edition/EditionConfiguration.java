package com.openlab.qualitos.quality.edition;

import com.openlab.qualitos.licensing.application.Licensing;
import com.openlab.qualitos.licensing.infrastructure.LicensingFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
