package com.openlab.qualitos.quality.circuit.application;

import com.openlab.qualitos.quality.circuit.domain.CircuitException;
import com.openlab.qualitos.quality.circuit.domain.CircuitRun;
import com.openlab.qualitos.quality.circuit.domain.CircuitStep;
import com.openlab.qualitos.quality.circuit.domain.CircuitSubject;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Les circuits de validation paramétrables (ADR 0080).
 *
 * <p>Le client règle, pour chaque type d'objet, les étapes et le rôle qui
 * approuve chacune. Quand un objet entre en revue, son passage commence avec
 * les étapes du moment ; chaque approbateur décide à l'étape en cours.
 */
public class CircuitService implements ApprovalCircuits {

    static final int MAX_STEPS = 10;

    private final CircuitPorts.Circuits circuits;
    private final CircuitPorts.Runs runs;
    private final CircuitPorts.Context context;
    private final CircuitPorts.Audit audit;
    private final Clock clock;

    public CircuitService(CircuitPorts.Circuits circuits, CircuitPorts.Runs runs, CircuitPorts.Context context,
                          CircuitPorts.Audit audit, Clock clock) {
        this.circuits = circuits;
        this.runs = runs;
        this.context = context;
        this.audit = audit;
        this.clock = clock;
    }

    // ---------- réglage ----------

    public CircuitDto.CircuitView circuit(CircuitSubject subject) {
        return view(subject, circuits.find(context.requireTenantId(), subject));
    }

    /**
     * Remplace le circuit d'un type d'objet. Une liste vide le retire : l'objet
     * revient à l'approbation simple. Les passages déjà commencés gardent leurs
     * étapes.
     */
    public CircuitDto.CircuitView replace(CircuitSubject subject, List<CircuitDto.StepView> steps) {
        UUID tenant = context.requireTenantId();
        List<CircuitDto.StepView> demandees = steps == null ? List.of() : steps;
        if (demandees.size() > MAX_STEPS) {
            throw new CircuitException(CircuitException.Reason.INVALID, "steps",
                    "Un circuit compte au plus " + MAX_STEPS + " étapes.");
        }
        Set<String> connus = context.tenantRoles();
        List<CircuitStep> etapes = demandees.stream().map(s -> {
            CircuitStep e = new CircuitStep(s.name(), s.roleCode(), s.minApprovals());
            if (!connus.contains(e.roleCode())) {
                throw new CircuitException(CircuitException.Reason.INVALID, "steps",
                        "Rôle inconnu dans votre organisation : " + e.roleCode());
            }
            // Sans le droit de décider, les porteurs du rôle ne verraient jamais le
            // bouton : le circuit resterait bloqué à cette étape.
            if (!context.roleGrants(e.roleCode(), subject.decidePermission())) {
                throw new CircuitException(CircuitException.Reason.INVALID, "steps",
                        "Le rôle " + e.roleCode() + " n'a pas le droit " + subject.decidePermission().code() + ".");
            }
            return e;
        }).toList();
        UUID acteur = context.requireActorId();
        circuits.replace(tenant, subject, etapes, acteur);
        audit.circuitReplaced(tenant, subject, etapes, acteur);
        return view(subject, etapes);
    }

    // ---------- passages ----------

    @Override
    public Optional<CircuitDto.RunView> start(CircuitSubject subject, UUID subjectId, UUID authorId) {
        UUID tenant = context.requireTenantId();
        Optional<CircuitRun> ouvert = runs.findOpen(tenant, subject, subjectId);
        if (ouvert.isPresent()) {
            return ouvert.map(CircuitService::view);
        }
        List<CircuitStep> etapes = circuits.find(tenant, subject);
        if (etapes.isEmpty()) {
            return Optional.empty();
        }
        CircuitRun run = CircuitRun.start(tenant, subject, subjectId, authorId, etapes, clock.instant());
        runs.save(run);
        return Optional.of(view(run));
    }

    @Override
    public Optional<CircuitDto.RunView> decide(CircuitSubject subject, UUID subjectId, boolean approve,
                                               String comment) {
        UUID tenant = context.requireTenantId();
        Optional<CircuitRun> ouvert = runs.findOpen(tenant, subject, subjectId);
        if (ouvert.isEmpty()) {
            return Optional.empty();
        }
        CircuitRun run = ouvert.get();
        CircuitRun.Decision d = run.decide(context.requireActorId(), context.actorRoles(), approve, comment,
                clock.instant());
        runs.save(run);
        audit.decided(run, d);
        return Optional.of(view(run));
    }

    /** Le dernier passage de l'objet — pour afficher où il en est. */
    public Optional<CircuitDto.RunView> run(CircuitSubject subject, UUID subjectId) {
        return runs.findLatest(context.requireTenantId(), subject, subjectId).map(CircuitService::view);
    }

    // ---------- vues ----------

    private static CircuitDto.CircuitView view(CircuitSubject subject, List<CircuitStep> etapes) {
        return new CircuitDto.CircuitView(subject.code(), etapes.stream().map(CircuitService::view).toList());
    }

    private static CircuitDto.StepView view(CircuitStep s) {
        return new CircuitDto.StepView(s.name(), s.roleCode(), s.minApprovals());
    }

    static CircuitDto.RunView view(CircuitRun r) {
        return new CircuitDto.RunView(r.id(), r.subject().code(), r.subjectId(), r.status().name(), r.currentStep(),
                r.steps().stream().map(CircuitService::view).toList(),
                r.decisions().stream().map(d -> new CircuitDto.DecisionView(d.stepIndex(), d.actorId(), d.approved(),
                        d.comment(), d.at())).toList(),
                r.startedAt(), r.endedAt());
    }
}
