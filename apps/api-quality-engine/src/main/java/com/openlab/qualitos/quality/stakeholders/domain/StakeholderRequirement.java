package com.openlab.qualitos.quality.stakeholders.domain;

import java.util.UUID;

/**
 * Une exigence portée par une partie intéressée : ce qu'elle attend, d'où cela
 * vient, ce qu'on risque si on n'y répond pas, et comment on s'en assure.
 *
 * <p>C'est la ligne que l'auditeur lit en §4.2 : une attente sans exigence
 * nommée n'est pas auditable, et une exigence sans origine n'est pas opposable.
 * Les dix champs reprennent la trame du registre papier, sans en ajouter : tout
 * champ de plus serait un champ qu'on ne remplit pas.
 *
 * @param need         le besoin exprimé, en langage de la partie intéressée
 * @param requirement  l'exigence qui en découle, telle qu'on s'engage à la tenir
 * @param origin       d'où elle vient : contrat, texte, norme, référentiel
 * @param nature       ce qui la rend opposable
 * @param risk         ce qu'on risque à ne pas y répondre
 * @param riskLevel    la cotation de ce risque
 * @param control      la mesure qui la maîtrise
 * @param indicator    ce qui montre qu'elle est tenue
 * @param responsible  qui en répond, en clair (une fonction, pas un compte :
 *                     l'exigence survit au titulaire)
 * @param evidence     la preuve attendue par un auditeur
 * @param status       où en est son traitement
 * @param riskId       le risque du registre qui la porte, ou {@code null}
 * @param ncId         la non-conformité qui l'a mise en défaut, ou {@code null}
 * @param auditId      l'audit qui l'a examinée, ou {@code null}
 * @param documentId   le document qui la formalise, ou {@code null}
 * @param capaId       le dossier CAPA ouvert pour y répondre, ou {@code null}
 */
public record StakeholderRequirement(UUID id, String need, String requirement, String origin,
                                     MandatoryNature nature, String risk, ImpactLevel riskLevel,
                                     String control, String indicator, String responsible,
                                     String evidence, RequirementStatus status,
                                     UUID riskId, UUID ncId, UUID auditId, UUID documentId,
                                     UUID capaId) {

    public static final int SHORT_MAX = 500;

    public StakeholderRequirement {
        if (requirement == null || requirement.isBlank()) {
            // L'exigence est le SEUL champ obligatoire : c'est elle qu'on tient
            // ou qu'on ne tient pas. Le reste se complète au fil de l'analyse,
            // et exiger les dix champs d'un coup ferait saisir du remplissage.
            throw new StakeholderValidationException("A requirement says what is expected");
        }
        requirement = trim(requirement);
        need = trim(need);
        origin = trim(origin);
        risk = trim(risk);
        control = trim(control);
        indicator = trim(indicator);
        responsible = trim(responsible);
        evidence = trim(evidence);
        status = status == null ? RequirementStatus.TO_ANALYSE : status;
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String t = value.strip();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() > SHORT_MAX ? t.substring(0, SHORT_MAX) : t;
    }

    /** La même exigence, à un autre statut. */
    public StakeholderRequirement withStatus(RequirementStatus next) {
        return new StakeholderRequirement(id, need, requirement, origin, nature, risk, riskLevel,
                control, indicator, responsible, evidence, next, riskId, ncId, auditId, documentId,
                capaId);
    }

    /** La même exigence, rattachée à un objet d'un autre module. */
    public StakeholderRequirement withLink(RequirementLink link, UUID target) {
        return new StakeholderRequirement(id, need, requirement, origin, nature, risk, riskLevel,
                control, indicator, responsible, evidence, status,
                link == RequirementLink.RISK ? target : riskId,
                link == RequirementLink.NON_CONFORMITY ? target : ncId,
                link == RequirementLink.AUDIT ? target : auditId,
                link == RequirementLink.DOCUMENT ? target : documentId,
                capaId);
    }

    /** La même exigence, avec son dossier CAPA et le statut qui s'ensuit. */
    public StakeholderRequirement withCapa(UUID newCapaId) {
        // Ouvrir une action signifie qu'on a constaté un écart : l'exigence
        // repasse « à réévaluer », et ne peut plus être affichée maîtrisée.
        return new StakeholderRequirement(id, need, requirement, origin, nature, risk, riskLevel,
                control, indicator, responsible, evidence, RequirementStatus.TO_REASSESS,
                riskId, ncId, auditId, documentId, newCapaId);
    }

    /** La même exigence, dotée d'un identifiant. */
    public StakeholderRequirement withId(UUID newId) {
        return new StakeholderRequirement(newId, need, requirement, origin, nature, risk, riskLevel,
                control, indicator, responsible, evidence, status, riskId, ncId, auditId, documentId,
                capaId);
    }
}
