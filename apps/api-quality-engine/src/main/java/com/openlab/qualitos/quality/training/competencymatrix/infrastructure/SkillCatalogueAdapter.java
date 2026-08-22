package com.openlab.qualitos.quality.training.competencymatrix.infrastructure;

import com.openlab.qualitos.quality.training.SkillRepository;
import com.openlab.qualitos.quality.training.competencymatrix.application.SkillCataloguePort;
import com.openlab.qualitos.quality.training.competencymatrix.domain.CompetencyGrid;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Le catalogue des competences du tenant.
 *
 * <p>Borne a {@value #MAX_SKILLS} lignes. Une matrice est une figure qu'on lit
 * d'un coup d'oeil : au-dela, ce n'est plus une matrice mais un export, et le
 * navigateur peinerait autant que l'oeil. La borne protege aussi le serveur
 * d'un catalogue anormalement gonfle.
 */
@Component
public class SkillCatalogueAdapter implements SkillCataloguePort {

    static final int MAX_SKILLS = 500;

    private final SkillRepository skills;

    public SkillCatalogueAdapter(SkillRepository skills) {
        this.skills = skills;
    }

    @Override
    public List<CompetencyGrid.SkillEntry> findAll(UUID tenantId) {
        return skills.findByTenantId(tenantId, PageRequest.of(0, MAX_SKILLS))
                .map(skill -> new CompetencyGrid.SkillEntry(
                        skill.getId(), skill.getCode(), skill.getName(), skill.getCategory()))
                .toList();
    }
}
