package com.openlab.qualitos.quality.smi.infrastructure;

import com.openlab.qualitos.quality.audit.AuditService;
import com.openlab.qualitos.quality.calibration.CalibrationEquipment;
import com.openlab.qualitos.quality.calibration.CalibrationEquipmentRepository;
import com.openlab.qualitos.quality.calibration.CalibrationPlan;
import com.openlab.qualitos.quality.calibration.CalibrationPlanRepository;
import com.openlab.qualitos.quality.capa.CapaAction;
import com.openlab.qualitos.quality.capa.CapaActionRepository;
import com.openlab.qualitos.quality.capa.CapaActionStatus;
import com.openlab.qualitos.quality.capa.CapaCriticity;
import com.openlab.qualitos.quality.capa.CapaStatus;
import com.openlab.qualitos.quality.change.ChangeRequest;
import com.openlab.qualitos.quality.change.ChangeRequestRepository;
import com.openlab.qualitos.quality.change.ChangeRequestStatus;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.riskregister.application.RiskRegisterService;
import com.openlab.qualitos.quality.riskregister.domain.RegisterRequirement;
import com.openlab.qualitos.quality.riskregister.domain.RiskStatus;
import com.openlab.qualitos.quality.smi.application.SmiPorts;
import com.openlab.qualitos.quality.smi.domain.Deadline;
import com.openlab.qualitos.quality.standards.AdoptionStatus;
import com.openlab.qualitos.quality.standards.StandardsDto;
import com.openlab.qualitos.quality.standards.StandardsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Les lectures du SMI dans chaque module.
 *
 * <p>Quand un module a un service de lecture, on passe par lui : il porte déjà
 * le cloisonnement par client et ses règles de visibilité. Les seuls accès
 * directs aux dépôts sont des COMPTES et des listes d'échéances qu'aucun
 * service n'offrait — toujours bornés au client du jeton.
 */
public final class SmiAdapters {

    private SmiAdapters() {}

    /** Les dossiers terminés n'ont plus d'action à mener. */
    static final Set<CapaStatus> CAPA_TERMINAL = EnumSet.of(CapaStatus.CLOSED, CapaStatus.REJECTED);
    /** Une norme retirée ou dont le certificat a expiré ne se pilote plus. */
    static final Set<AdoptionStatus> ADOPTION_ENDED = EnumSet.of(AdoptionStatus.WITHDRAWN, AdoptionStatus.EXPIRED);
    /** Un changement attend une validation tant qu'il est soumis ou en revue. */
    static final Set<ChangeRequestStatus> CHANGE_AWAITING =
            EnumSet.of(ChangeRequestStatus.SUBMITTED, ChangeRequestStatus.UNDER_REVIEW);

    static UUID tenant() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    // ---------- normes ----------

    public static final class Standards implements SmiPorts.Standards {

        private static final int MAX_ADOPTIONS = 50;
        private final StandardsService service;

        public Standards(StandardsService service) {
            this.service = service;
        }

        @Override
        public List<SmiPorts.AdoptedStandard> adopted() {
            return service.listAdoptions(null, PageRequest.of(0, MAX_ADOPTIONS)).stream()
                    .filter(a -> !ADOPTION_ENDED.contains(a.status()))
                    .map(a -> new SmiPorts.AdoptedStandard(a.id(), a.standardCode(), a.standardName()))
                    .toList();
        }

        @Override
        public Optional<SmiPorts.Alignment> alignment(UUID adoptionId) {
            StandardsDto.AlignmentReport r = service.computeAlignment(adoptionId);
            return Optional.of(new SmiPorts.Alignment(r.overallScore(), r.sections().stream()
                    .map(s -> new SmiPorts.SectionCoverage(s.sectionCode(), s.sectionTitle(),
                            s.coveredRequirements(), s.totalRequirements()))
                    .toList()));
        }
    }

    // ---------- CAPA ----------

    public static final class Actions implements SmiPorts.Actions {

        private final CapaActionRepository repository;

        public Actions(CapaActionRepository repository) {
            this.repository = repository;
        }

        @Override
        public SmiPorts.OverdueActions overdue(LocalDate today) {
            UUID t = tenant();
            return new SmiPorts.OverdueActions(
                    repository.countOverdue(t, CapaActionStatus.DONE, today, CAPA_TERMINAL),
                    repository.countOverdueWithCriticity(t, CapaActionStatus.DONE, today, CAPA_TERMINAL,
                            CapaCriticity.CRITICAL));
        }

        @Override
        public List<Deadline> dueBy(LocalDate until, int limit) {
            return repository.findDueBy(tenant(), CapaActionStatus.DONE, until, CAPA_TERMINAL,
                            PageRequest.of(0, limit)).stream()
                    .map(Actions::toDeadline)
                    .toList();
        }

        static Deadline toDeadline(CapaAction a) {
            return new Deadline(Deadline.Kind.CAPA_ACTION, a.getCapa().getId(), a.getCapa().getSourceRef(),
                    a.getTitle(), a.getDueDate());
        }
    }

    // ---------- registre des risques ----------

    public static final class Risks implements SmiPorts.Risks {

        private final RiskRegisterService service;

        public Risks(RiskRegisterService service) {
            this.service = service;
        }

        /** Ouvert = pas clos. Un risque accepté reste au registre, et compte. */
        @Override
        public List<SmiPorts.OpenRisk> open() {
            return service.risks().stream()
                    .filter(r -> r.status() != RiskStatus.CLOSED)
                    .map(r -> new SmiPorts.OpenRisk(r.id(), r.reference(), r.title(), r.grossSeverity(),
                            r.grossProbability(), r.residualSeverity(), r.residualProbability(),
                            r.grossLevel().name(),
                            r.requirements().stream().map(RegisterRequirement::name).toList(),
                            r.nextReviewOn()))
                    .toList();
        }
    }

    // ---------- audits ----------

    public static final class Audits implements SmiPorts.Audits {

        private final AuditService service;

        public Audits(AuditService service) {
            this.service = service;
        }

        @Override
        public List<SmiPorts.PlannedAudit> planned(int horizonDays) {
            return service.planning(null, horizonDays).stream()
                    .map(p -> new SmiPorts.PlannedAudit(p.id(), p.reference(), p.title(),
                            p.type() == null ? null : p.type().name(), p.standard(), p.scheduledDate()))
                    .toList();
        }
    }

    // ---------- étalonnages et changements ----------

    public static final class Deadlines implements SmiPorts.Deadlines {

        private final CalibrationPlanRepository plans;
        private final CalibrationEquipmentRepository equipments;
        private final ChangeRequestRepository changes;

        public Deadlines(CalibrationPlanRepository plans, CalibrationEquipmentRepository equipments,
                         ChangeRequestRepository changes) {
            this.plans = plans;
            this.equipments = equipments;
            this.changes = changes;
        }

        @Override
        public List<Deadline> dueBy(LocalDate until, int limit) {
            UUID t = tenant();
            List<Deadline> echeances = new ArrayList<>();

            List<CalibrationPlan> dus = plans.findByTenantIdAndNextDueOnBefore(t, until.plusDays(1),
                    PageRequest.of(0, limit, Sort.by("nextDueOn"))).getContent();
            Map<UUID, CalibrationEquipment> parId = equipments
                    .findAllById(dus.stream().map(CalibrationPlan::getEquipmentId).toList()).stream()
                    // Défense en profondeur : l'équipement d'un plan est du même client,
                    // mais un nom d'un autre client ne doit jamais s'afficher ici.
                    .filter(e -> t.equals(e.getTenantId()))
                    .collect(Collectors.toMap(CalibrationEquipment::getId, Function.identity()));
            for (CalibrationPlan p : dus) {
                CalibrationEquipment e = parId.get(p.getEquipmentId());
                if (e != null) {
                    echeances.add(new Deadline(Deadline.Kind.CALIBRATION, e.getId(), e.getCode(), e.getName(),
                            p.getNextDueOn()));
                }
            }

            for (ChangeRequest c : changes.findByTenantIdAndStatusInAndPlannedForLessThanEqualOrderByPlannedForAsc(
                    t, CHANGE_AWAITING, until, PageRequest.of(0, limit))) {
                echeances.add(new Deadline(Deadline.Kind.CHANGE, c.getId(), c.getCode(), c.getTitle(),
                        c.getPlannedFor()));
            }
            return echeances;
        }
    }
}
