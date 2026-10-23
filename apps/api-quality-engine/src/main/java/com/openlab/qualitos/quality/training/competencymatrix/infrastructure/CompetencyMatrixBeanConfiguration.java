package com.openlab.qualitos.quality.training.competencymatrix.infrastructure;

import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.training.SkillRepository;
import com.openlab.qualitos.quality.training.UserSkillAssignmentRepository;
import com.openlab.qualitos.quality.training.competencymatrix.application.CompetencyMatrixService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Câblage Spring de la matrice de compétences.
 *
 * <p>Le {@code Clock} vient de {@code CommonBeansConfiguration}, unique et
 * {@code @Primary} ; en déclarer un second ici rendrait l'injection ambiguë.
 */
@Configuration
public class CompetencyMatrixBeanConfiguration {

    @Bean
    public CompetencyMatrixService competencyMatrixService(MatrixOperatorJpaRepository operators,
                                                           MatrixSkillJpaRepository matrixSkills,
                                                           SkillRepository catalogue,
                                                           UserSkillAssignmentRepository assessments,
                                                           CompetencyWorkshopJpaRepository workshops,
                                                           AuditEventService auditEvents,
                                                           Clock clock) {
        return new CompetencyMatrixService(
                new CompetencyMatrixAdapters.Operators(operators),
                new CompetencyMatrixAdapters.Skills(matrixSkills, catalogue),
                new CompetencyMatrixAdapters.Levels(assessments),
                new CompetencyMatrixAdapters.WorkshopSettings(workshops, clock),
                new JwtCompetencyMatrixContext(),
                new AuditLogCompetencyMatrixPublisher(auditEvents),
                clock);
    }
}
