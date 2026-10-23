package com.openlab.qualitos.quality.training.competencymatrix.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.training.competencymatrix.application.CompetencyMatrixPorts;
import com.openlab.qualitos.quality.training.competencymatrix.domain.LevelCell;
import com.openlab.qualitos.quality.training.competencymatrix.domain.MatrixOperator;
import com.openlab.qualitos.quality.training.competencymatrix.domain.MatrixSkill;

import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Route les écritures de la matrice vers le journal d'audit chaîné.
 *
 * <p>Aucune donnée personnelle dans la charge utile : ni le nom de l'opérateur
 * ni celui du formateur, seulement des identifiants de compte — de quoi
 * rapprocher une trace d'une case sans recopier l'annuaire. Un niveau est
 * tracé avec sa valeur précédente : « qui a fait passer X de débutant à
 * autonome, et quand » est exactement la question d'un auditeur.
 */
public class AuditLogCompetencyMatrixPublisher implements CompetencyMatrixPorts.Audit {

    static final String OPERATOR = "competency-matrix-operator";
    static final String SKILL = "competency-matrix-skill";
    static final String LEVEL = "competency-assessment";
    static final String WORKSHOPS = "competency-workshops";

    static final String OPERATOR_ADDED = "training.matrix.operator-added";
    static final String OPERATOR_REMOVED = "training.matrix.operator-removed";
    static final String SKILL_ADDED = "training.matrix.skill-added";
    static final String SKILL_REMOVED = "training.matrix.skill-removed";
    static final String LEVEL_SET = "training.matrix.level-set";
    static final String WORKSHOPS_CHANGED = "training.matrix.workshops-updated";

    private final AuditEventService auditEvents;

    public AuditLogCompetencyMatrixPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void operatorAdded(UUID tenantId, MatrixOperator operator, UUID actor) {
        record(tenantId, actor, OPERATOR_ADDED, OPERATOR, operator.id(), "Opérateur ajouté à la matrice de compétences",
                "{\"userId\":\"" + operator.userId() + "\"}");
    }

    @Override
    public void operatorRemoved(UUID tenantId, MatrixOperator operator, UUID actor) {
        record(tenantId, actor, OPERATOR_REMOVED, OPERATOR, operator.id(),
                "Opérateur retiré de la matrice de compétences", "{\"userId\":\"" + operator.userId() + "\"}");
    }

    @Override
    public void skillAdded(UUID tenantId, MatrixSkill skill, UUID actor) {
        record(tenantId, actor, SKILL_ADDED, SKILL, skill.skillId(), "Compétence ajoutée à la matrice",
                "{\"trainerUserId\":\"" + skill.trainerUserId() + "\"}");
    }

    @Override
    public void skillRemoved(UUID tenantId, MatrixSkill skill, UUID actor) {
        record(tenantId, actor, SKILL_REMOVED, SKILL, skill.skillId(), "Compétence retirée de la matrice", "{}");
    }

    @Override
    public void levelSet(UUID tenantId, LevelCell cell, OptionalInt previous, UUID actor) {
        String avant = previous.isPresent() ? String.valueOf(previous.getAsInt()) : "null";
        record(tenantId, actor, LEVEL_SET, LEVEL, cell.skillId(), "Niveau de compétence posé dans la matrice",
                "{\"userId\":\"" + cell.userId() + "\",\"previousLevel\":" + avant + ",\"level\":" + cell.level() + "}");
    }

    @Override
    public void workshopsChanged(UUID tenantId, List<String> names, UUID actor) {
        record(tenantId, actor, WORKSHOPS_CHANGED, WORKSHOPS, tenantId, "Ateliers de la matrice modifiés",
                "{\"count\":" + names.size() + "}");
    }

    private void record(UUID tenantId, UUID actor, String action, String type, UUID resourceId, String summary,
                        String payload) {
        auditEvents.recordForTenant(tenantId, new AuditEventDto.RecordEventRequest(
                null, ActorType.USER, actor, action, type, resourceId, summary, payload, null, null));
    }
}
