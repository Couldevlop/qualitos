package com.openlab.qualitos.quality.audit;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.nonconformity.NcCategory;
import com.openlab.qualitos.quality.nonconformity.NcDto;
import com.openlab.qualitos.quality.nonconformity.NcOrigin;
import com.openlab.qualitos.quality.nonconformity.NcService;
import com.openlab.qualitos.quality.nonconformity.NcSeverity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Instruit un constat d'audit non conforme comme non-conformité au registre.
 *
 * <p>Un écart relevé pendant un audit n'est pas une ligne de rapport : c'est un
 * écart à traiter, qui doit compter dans le registre, porter une analyse de
 * cause et se clore sur une preuve. Jusqu'ici il restait dans la fiche d'audit,
 * et l'on ouvrait la non-conformité à la main en recopiant le constat — avec
 * les écarts de libellé et les oublis que cela suppose.
 *
 * <p>La CAPA et la non-conformité ne se remplacent pas, et le constat peut
 * porter les deux : la CAPA est le plan d'action, l'écart est le fait constaté.
 * Un rapport d'audit externe cite le second ; une revue de direction compte le
 * second. Ouvrir l'une n'exempte donc pas de déclarer l'autre.
 *
 * <p>Service à part, comme {@link AuditFindingCapaService} : le cycle de
 * l'audit n'a pas à connaître le registre des écarts, et ce pont est la seule
 * chose qui les relie.
 */
@Service
@Transactional
public class AuditFindingNcService {

    /**
     * Seuls les écarts deviennent des non-conformités. Une conformité est un
     * constat favorable ; une observation et une piste de progrès n'affirment
     * aucun manquement — les compter comme écarts gonflerait le registre de
     * choses qu'aucun auditeur n'a relevées comme telles.
     */
    static final Set<FindingType> NON_CONFORMING = Set.of(FindingType.MINOR_NC, FindingType.MAJOR_NC);

    /**
     * Les audits dont l'écart vient du DEHORS.
     *
     * <p>Un écart relevé par un organisme certificateur, par un client ou chez
     * un fournisseur n'a ni les mêmes délais ni les mêmes destinataires qu'un
     * écart trouvé par l'équipe elle-même, et une revue de direction les compte
     * séparément. L'origine se déduit donc du type d'audit, sans la demander.
     */
    static final Set<AuditType> EXTERNAL_AUDITS =
            Set.of(AuditType.EXTERNAL, AuditType.CERTIFICATION, AuditType.SURVEILLANCE, AuditType.SUPPLIER);

    private final AuditPlanRepository planRepository;
    private final AuditFindingRepository findingRepository;
    private final NcService ncService;

    public AuditFindingNcService(AuditPlanRepository planRepository,
                                 AuditFindingRepository findingRepository,
                                 NcService ncService) {
        this.planRepository = planRepository;
        this.findingRepository = findingRepository;
        this.ncService = ncService;
    }

    /** Ce que l'écran d'audit a besoin de savoir après la déclaration. */
    public record FindingNc(UUID findingId, UUID ncId, String ncReference, String sourceRef) {}

    /**
     * Ouvre l'écart du constat.
     *
     * <p>Le corps est volontairement court : le titre, la catégorie et la
     * nature fine sont les seules choses que l'écran demande. Tout le reste est
     * {@code déduit} du constat — sa description, sa date de relevé, sa clause,
     * sa gravité, l'origine de l'audit — parce que le recopier serait rouvrir
     * la porte aux divergences que ce pont existe pour fermer.
     */
    public FindingNc open(UUID planId, UUID findingId, AuditDto.DeclareNcRequest request) {
        UUID tenantId = requireTenantId();
        AuditPlan plan = planRepository.findByIdAndTenantId(planId, tenantId)
                .orElseThrow(() -> new AuditPlanNotFoundException(planId));
        if (plan.getStatus() == AuditStatus.CANCELLED) {
            throw new AuditStateException("A cancelled audit raises no non-conformity");
        }
        AuditFinding finding = findingRepository.findForUpdateByIdAndPlanId(findingId, planId)
                .orElseThrow(() -> new AuditFindingNotFoundException(findingId));
        if (!NON_CONFORMING.contains(finding.getType())) {
            throw new AuditStateException("Only a non-conforming finding raises a non-conformity");
        }
        if (finding.getNcId() != null) {
            throw new AuditStateException("This finding already has its non-conformity " + finding.getNcId());
        }

        String sourceRef = AuditFindingCapaService.sourceRef(plan, finding);
        NcDto.Response nc = ncService.create(new NcDto.CreateRequest(
                request.title(),
                description(sourceRef, finding),
                request.category(),
                request.subcategoryCode(),
                severityOf(finding.getType()),
                finding.getRaisedAt(),
                // La clause auditée tient lieu de localisation : c'est l'endroit
                // du système de management où l'écart a été constaté, et c'est
                // ce que cite le rapport.
                zone(finding.getClauseRef()),
                null, null,
                finding.getPhotoUrl(),
                null,
                EXTERNAL_AUDITS.contains(plan.getType()) ? NcOrigin.EXTERNAL : NcOrigin.INTERNAL,
                null, null,
                // Aucune équipe : un écart né d'un constat d'audit n'a pas encore
                // de personnes désignées pour le traiter. L'obligation de nommer
                // une équipe pèse sur l'humain qui OUVRE une fiche, pas sur
                // l'écran qui la déduit d'un constat — inventer des noms ici
                // mettrait dans le D1 du 8D des gens qui n'ont rien accepté. La
                // fiche se complète ensuite, et D1 dit « à saisir » entre-temps.
                List.of()));

        finding.setNcId(nc.id());
        findingRepository.save(finding);
        return new FindingNc(finding.getId(), nc.id(), nc.reference(), sourceRef);
    }

    /**
     * Un écart majeur est une non-conformité majeure, un écart mineur une
     * mineure. {@code CRITICAL} n'est jamais attribué d'office : la criticité la
     * plus haute du registre engage des délais et des escalades, et c'est une
     * décision humaine prise sur la fiche, pas une conversion mécanique.
     */
    static NcSeverity severityOf(FindingType type) {
        return type == FindingType.MAJOR_NC ? NcSeverity.MAJOR : NcSeverity.MINOR;
    }

    /**
     * La description de l'écart EST celle du constat, précédée de sa citation.
     *
     * <p>Six mois plus tard, personne ne se souvient de quel audit vient un
     * écart ; la citation reste lisible dans le registre même quand le plan
     * d'audit a été archivé, et elle est ce qu'un auditeur externe demande.
     */
    static String description(String sourceRef, AuditFinding finding) {
        String clause = finding.getClauseRef() != null && !finding.getClauseRef().isBlank()
                ? " (clause " + finding.getClauseRef().strip() + ")" : "";
        return "Constat d'audit " + sourceRef + clause + " — " + finding.getDescription();
    }

    /** La zone est bornée à 255 par la colonne : on ne tronque pas en silence ailleurs. */
    private static String zone(String clauseRef) {
        if (clauseRef == null || clauseRef.isBlank()) return null;
        String zone = "Clause " + clauseRef.strip();
        return zone.length() > 255 ? zone.substring(0, 255) : zone;
    }

    private static UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }
}
