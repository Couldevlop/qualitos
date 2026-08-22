package com.openlab.qualitos.quality.training.competencymatrix.application;

import com.openlab.qualitos.quality.training.competencymatrix.domain.CompetencyGrid;

import java.util.List;
import java.util.UUID;

/** Port — le catalogue des competences du tenant, qui forme les LIGNES. */
public interface SkillCataloguePort {

    List<CompetencyGrid.SkillEntry> findAll(UUID tenantId);
}
