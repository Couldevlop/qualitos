package com.openlab.qualitos.quality.training.competencymatrix.infrastructure;

import com.openlab.qualitos.quality.training.competencymatrix.application.CompetencyAssessmentPort;
import com.openlab.qualitos.quality.training.competencymatrix.application.CompetencyMatrixService;
import com.openlab.qualitos.quality.training.competencymatrix.application.SkillCataloguePort;
import com.openlab.qualitos.quality.training.competencymatrix.application.TenantProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CompetencyMatrixBeanConfiguration {

    @Bean
    public CompetencyMatrixService competencyMatrixService(
            SkillCataloguePort skills,
            CompetencyAssessmentPort assessments,
            @Qualifier("competencyMatrixTenantContextProvider") TenantProvider tenantProvider) {
        return new CompetencyMatrixService(skills, assessments, tenantProvider);
    }
}
