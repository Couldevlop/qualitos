package com.openlab.qualitos.quality.training.competencymatrix.application;

import com.openlab.qualitos.quality.training.competencymatrix.domain.CompetencyGrid;

import java.util.List;
import java.util.UUID;

/**
 * Port — les evaluations du tenant. Elles forment les CELLULES et decident
 * des colonnes : seules les personnes evaluees apparaissent dans la matrice.
 */
public interface CompetencyAssessmentPort {

    List<CompetencyGrid.Assessment> findAll(UUID tenantId);
}
