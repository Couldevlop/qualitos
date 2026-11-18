package com.openlab.qualitos.quality.ims.infrastructure.config;

import com.openlab.qualitos.quality.ims.application.usecase.GetCoverageMatrixUseCase;
import com.openlab.qualitos.quality.ims.domain.port.ClauseMappingRepository;
import com.openlab.qualitos.quality.ims.domain.port.TenantStandardCodesProvider;
import com.openlab.qualitos.quality.ims.domain.service.CoverageMatrixDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Déclare les objets du module qui ne portent aucune annotation Spring.
 *
 * <p>{@link CoverageMatrixDomainService} est un pur POJO de domaine, et
 * {@link GetCoverageMatrixUseCase} l'est devenu : il portait un {@code @Service},
 * seule raison pour laquelle la couche applicative de ce module nommait encore
 * Spring. Un cas d'usage déclaré ici se construit dans un banc d'un seul
 * {@code new}, sans contexte (ADR 0128).
 */
@Configuration
public class ImsConfig {

    @Bean
    public CoverageMatrixDomainService coverageMatrixDomainService() {
        return new CoverageMatrixDomainService();
    }

    @Bean
    public GetCoverageMatrixUseCase getCoverageMatrixUseCase(
            ClauseMappingRepository mappingRepo,
            TenantStandardCodesProvider tenantStandardCodes,
            CoverageMatrixDomainService domainService) {
        return new GetCoverageMatrixUseCase(mappingRepo, tenantStandardCodes, domainService);
    }
}
