package com.openlab.qualitos.quality.nonconformity.eightd.infrastructure;

import com.openlab.qualitos.quality.capa.CapaAction;
import com.openlab.qualitos.quality.capa.CapaCase;
import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.capa.CapaEvidenceRepository;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlan;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanRepository;
import com.openlab.qualitos.quality.fivewhys.FiveWhysAnalysis;
import com.openlab.qualitos.quality.fivewhys.FiveWhysAnalysisRepository;
import com.openlab.qualitos.quality.fivewhys.FiveWhysStep;
import com.openlab.qualitos.quality.fivewhys.FiveWhysStepRepository;
import com.openlab.qualitos.quality.ishikawa.IshikawaCause;
import com.openlab.qualitos.quality.ishikawa.IshikawaDiagram;
import com.openlab.qualitos.quality.ishikawa.IshikawaDiagramRepository;
import com.openlab.qualitos.quality.nonconformity.NcNotFoundException;
import com.openlab.qualitos.quality.nonconformity.NcPhotoRepository;
import com.openlab.qualitos.quality.nonconformity.NonConformity;
import com.openlab.qualitos.quality.nonconformity.NonConformityRepository;
import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDSourcePort;
import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDSources;
import com.openlab.qualitos.quality.risk.FmeaItem;
import com.openlab.qualitos.quality.risk.FmeaItemRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Rassemble, pour une non-conformité, ce que les autres modules savent déjà.
 *
 * <p>C'est l'adaptateur qui connaît les chemins de liaison — et ils ne se
 * ressemblent pas : l'Ishikawa porte un {@code ncId}, les 5 pourquoi une
 * association JPA, la CAPA est désignée par la NC, le mode de défaillance PFMEA
 * aussi, et les plans de surveillance se trouvent par le PRODUIT. Le cas d'usage
 * ignore tout cela, et c'est pour ça qu'il se teste sans base.
 *
 * <p>Lecture seule et dans une seule transaction : les collections paresseuses
 * (causes d'un Ishikawa, actions d'une CAPA) doivent être parcourues tant que la
 * session est ouverte.
 *
 * <p>Toutes les requêtes portent le tenant. Une non-conformité d'un autre tenant
 * est « introuvable », sans nuance : distinguer les deux cas ferait de ce point
 * d'entrée un oracle d'existence (OWASP A01).
 */
@Component
public class EightDSourceAdapter implements EightDSourcePort {

    private final NonConformityRepository ncs;
    private final NcPhotoRepository photos;
    private final IshikawaDiagramRepository ishikawas;
    private final FiveWhysAnalysisRepository fiveWhys;
    private final FiveWhysStepRepository fiveWhysSteps;
    private final CapaCaseRepository capas;
    private final CapaEvidenceRepository capaEvidences;
    private final FmeaItemRepository fmeaItems;
    private final ControlPlanRepository controlPlans;

    public EightDSourceAdapter(NonConformityRepository ncs,
                               NcPhotoRepository photos,
                               IshikawaDiagramRepository ishikawas,
                               FiveWhysAnalysisRepository fiveWhys,
                               FiveWhysStepRepository fiveWhysSteps,
                               CapaCaseRepository capas,
                               CapaEvidenceRepository capaEvidences,
                               FmeaItemRepository fmeaItems,
                               ControlPlanRepository controlPlans) {
        this.ncs = ncs;
        this.photos = photos;
        this.ishikawas = ishikawas;
        this.fiveWhys = fiveWhys;
        this.fiveWhysSteps = fiveWhysSteps;
        this.capas = capas;
        this.capaEvidences = capaEvidences;
        this.fmeaItems = fmeaItems;
        this.controlPlans = controlPlans;
    }

    @Override
    @Transactional(readOnly = true)
    public EightDSources collect(UUID tenantId, UUID ncId) {
        NonConformity nc = ncs.findByIdAndTenantId(ncId, tenantId)
                .orElseThrow(() -> new NcNotFoundException(ncId));

        return new EightDSources(
                enNc(tenantId, nc),
                enIshikawas(tenantId, ncId),
                enFiveWhys(tenantId, ncId),
                enCapa(tenantId, nc),
                enFmea(tenantId, nc),
                enSurveillance(tenantId, nc));
    }

    private EightDSources.Nc enNc(UUID tenantId, NonConformity nc) {
        int nbPhotos = photos.findByTenantIdAndNcIdOrderByCreatedAtAsc(tenantId, nc.getId()).size();
        return new EightDSources.Nc(
                nc.getReference(), nc.getTitle(), nc.getDescription(),
                nom(nc.getCategory()), nom(nc.getSeverity()), nom(nc.getOrigin()),
                nc.getStatus(), nc.getDetectedAt(), nc.getClosedAt(),
                nc.getZone(), nc.getReporterName(), nbPhotos,
                nc.getRootCause(), nc.getResolutionNote());
    }

    private List<EightDSources.CauseTree> enIshikawas(UUID tenantId, UUID ncId) {
        List<EightDSources.CauseTree> arbres = new ArrayList<>();
        for (IshikawaDiagram diagram : ishikawas.findByTenantIdAndNcIdOrderByCreatedAtDesc(tenantId, ncId)) {
            List<EightDSources.Cause> causes = new ArrayList<>();
            for (IshikawaCause cause : diagram.getCauses()) {
                causes.add(new EightDSources.Cause(
                        nom(cause.getCategory()), cause.getLabel(), cause.getDescription(),
                        cause.getRootCauseScore()));
            }
            arbres.add(new EightDSources.CauseTree(
                    diagram.getProblemStatement(), nom(diagram.getStatus()), causes));
        }
        return arbres;
    }

    private List<EightDSources.WhysChain> enFiveWhys(UUID tenantId, UUID ncId) {
        List<EightDSources.WhysChain> chaines = new ArrayList<>();
        for (FiveWhysAnalysis analyse
                : fiveWhys.findByNonConformityIdAndTenantIdOrderByCreatedAtDesc(ncId, tenantId)) {
            List<String> reponses = new ArrayList<>();
            for (FiveWhysStep etape
                    : fiveWhysSteps.findByAnalysisIdAndTenantIdOrderByPositionAsc(analyse.getId(), tenantId)) {
                reponses.add(etape.getAnswer());
            }
            chaines.add(new EightDSources.WhysChain(
                    analyse.getProblem(), analyse.getRootCause(), reponses));
        }
        return chaines;
    }

    private EightDSources.Capa enCapa(UUID tenantId, NonConformity nc) {
        if (nc.getCapaCaseId() == null) {
            return null;
        }
        CapaCase capa = capas.findByIdAndTenantId(nc.getCapaCaseId(), tenantId).orElse(null);
        if (capa == null) {
            // Lien rompu (CAPA supprimée) : on le traite comme une absence de source
            // plutôt qu'en erreur — le rapport doit rester émissible, et il le dira.
            return null;
        }
        List<EightDSources.Action> actions = new ArrayList<>();
        for (CapaAction action : capa.getActions()) {
            actions.add(new EightDSources.Action(
                    action.getTitle(), action.getDescription(),
                    nom(action.getActionType()), nom(action.getStatus()),
                    action.getAssigneeName(), action.getDueDate(), action.getCompletedAt(),
                    capaEvidences.countByTenantIdAndActionId(tenantId, action.getId())));
        }
        return new EightDSources.Capa(
                capa.getTitle(), nom(capa.getType()), nom(capa.getCriticity()), nom(capa.getStatus()),
                capa.getDueDate(), capa.getClosedAt(),
                capa.getEffectivenessVerified(), capa.getEffectivenessVerifiedAt(),
                capaEvidences.countCaseLevel(tenantId, capa.getId()),
                actions);
    }

    private EightDSources.Fmea enFmea(UUID tenantId, NonConformity nc) {
        if (nc.getFmeaItemId() == null) {
            return null;
        }
        FmeaItem item = fmeaItems.findByIdAndTenantId(nc.getFmeaItemId(), tenantId).orElse(null);
        if (item == null) {
            return null;
        }
        return new EightDSources.Fmea(
                item.getFailureMode(), item.getFailureEffect(), item.getFailureCause(),
                item.getCurrentControls(), item.getRpn(), item.getRpnAfter(),
                nom(item.getActionPriority()), item.getRecommendedAction(), item.getActionsTaken());
    }

    private List<EightDSources.Surveillance> enSurveillance(UUID tenantId, NonConformity nc) {
        if (nc.getProductId() == null) {
            return List.of();
        }
        List<EightDSources.Surveillance> plans = new ArrayList<>();
        for (ControlPlan plan : controlPlans.findByProduct(tenantId, nc.getProductId())) {
            plans.add(new EightDSources.Surveillance(
                    plan.getCode(), plan.getRevision(), nom(plan.getPhase()), nom(plan.getStatus()),
                    controlPlans.linesOf(plan.getId()).size(), plan.getSealSha256()));
        }
        return plans;
    }

    private static String nom(Enum<?> valeur) {
        return valeur == null ? null : valeur.name();
    }
}
