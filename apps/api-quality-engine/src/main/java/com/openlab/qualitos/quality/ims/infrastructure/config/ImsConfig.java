package com.openlab.qualitos.quality.ims.infrastructure.config;

import com.openlab.qualitos.quality.ims.domain.service.CoverageMatrixDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring config — expose le service domain {@link CoverageMatrixDomainService}
 * comme bean. Ce service est un pur POJO, sans dépendances Spring.
 */
@Configuration
public class ImsConfig {

    @Bean
    public CoverageMatrixDomainService coverageMatrixDomainService() {
        return new CoverageMatrixDomainService();
    }
}
