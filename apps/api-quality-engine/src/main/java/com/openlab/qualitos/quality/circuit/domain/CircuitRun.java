package com.openlab.qualitos.quality.circuit.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Le passage d'un objet dans un circuit : les étapes, figées au départ, et les
 * décisions prises.
 *
 * <p><b>Figées au départ</b> : changer le circuit du client ne change pas les
 * étapes d'un document déjà soumis — sans quoi un réglage pourrait faire
 * approuver un document par moins de monde que prévu.
 *
 * <p><b>Règles</b> : seul un porteur du rôle de l'étape décide ; l'auteur ne
 * décide jamais ; une même personne ne décide qu'une fois dans le passage
 * (quatre yeux, même sur plusieurs étapes) ; un refus clôt le passage ; une
 * étape est franchie quand elle a ses approbations distinctes.
 */
public final class CircuitRun {

    public enum Status { IN_PROGRESS, APPROVED, REJECTED }

    /** Une décision : à quelle étape, par qui, laquelle. */
    public record Decision(int stepIndex, UUID actorId, boolean approved, String comment, Instant at) {}

    public static final int COMMENT_MAX = 1000;

    private final UUID id;
    private final UUID tenantId;
    private final CircuitSubject subject;
    private final UUID subjectId;
    private final UUID authorId;
    private final List<CircuitStep> steps;
    private final List<Decision> decisions;
    private int currentStep;
    private Status status;
    private final Instant startedAt;
    private Instant endedAt;

    private CircuitRun(UUID id, UUID tenantId, CircuitSubject subject, UUID subjectId, UUID authorId,
                       List<CircuitStep> steps, List<Decision> decisions, int currentStep, Status status,
                       Instant startedAt, Instant endedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId);
        this.subject = Objects.requireNonNull(subject);
        this.subjectId = Objects.requireNonNull(subjectId);
        this.authorId = authorId;
        this.steps = List.copyOf(steps);
        this.decisions = new ArrayList<>(decisions);
        this.currentStep = currentStep;
        this.status = status;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    public static CircuitRun start(UUID tenantId, CircuitSubject subject, UUID subjectId, UUID authorId,
                                   List<CircuitStep> steps, Instant now) {
        if (steps == null || steps.isEmpty()) {
            throw new CircuitException(CircuitException.Reason.INVALID, "steps", "Un circuit compte au moins une étape.");
        }
        return new CircuitRun(UUID.randomUUID(), tenantId, subject, subjectId, authorId, steps, List.of(), 0,
                Status.IN_PROGRESS, now, null);
    }

    public static CircuitRun restore(UUID id, UUID tenantId, CircuitSubject subject, UUID subjectId, UUID authorId,
                                     List<CircuitStep> steps, List<Decision> decisions, int currentStep,
                                     Status status, Instant startedAt, Instant endedAt) {
        return new CircuitRun(id, tenantId, subject, subjectId, authorId, steps, decisions, currentStep, status,
                startedAt, endedAt);
    }

    /**
     * Enregistre la décision de {@code actor}, qui porte {@code actorRoles}.
     *
     * @return la décision enregistrée
     */
    public Decision decide(UUID actor, Set<String> actorRoles, boolean approve, String comment, Instant now) {
        if (status != Status.IN_PROGRESS) {
            throw new CircuitException(CircuitException.Reason.NOT_IN_PROGRESS, "Le circuit est terminé.");
        }
        if (actor.equals(authorId)) {
            throw new CircuitException(CircuitException.Reason.AUTHOR_CANNOT_DECIDE,
                    "L'auteur ne valide pas son propre travail.");
        }
        CircuitStep etape = steps.get(currentStep);
        if (!actorRoles.contains(etape.roleCode())) {
            throw new CircuitException(CircuitException.Reason.NOT_YOUR_STEP,
                    "Cette étape revient au rôle " + etape.roleCode() + ".");
        }
        if (decisions.stream().anyMatch(d -> d.actorId().equals(actor))) {
            throw new CircuitException(CircuitException.Reason.ALREADY_DECIDED,
                    "Vous avez déjà décidé dans ce circuit : une autre personne doit valider.");
        }
        String note = comment == null || comment.isBlank() ? null : comment.strip();
        if (note != null && note.length() > COMMENT_MAX) {
            throw new CircuitException(CircuitException.Reason.INVALID, "comment",
                    "Le commentaire dépasse " + COMMENT_MAX + " caractères.");
        }
        if (!approve && note == null) {
            throw new CircuitException(CircuitException.Reason.INVALID, "comment", "Un refus se motive.");
        }
        Decision d = new Decision(currentStep, actor, approve, note, now);
        decisions.add(d);
        if (!approve) {
            status = Status.REJECTED;
            endedAt = now;
        } else if (approvalsAt(currentStep) >= etape.minApprovals()) {
            if (currentStep == steps.size() - 1) {
                status = Status.APPROVED;
                endedAt = now;
            } else {
                currentStep++;
            }
        }
        return d;
    }

    public long approvalsAt(int stepIndex) {
        return decisions.stream().filter(d -> d.stepIndex() == stepIndex && d.approved()).count();
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public CircuitSubject subject() { return subject; }
    public UUID subjectId() { return subjectId; }
    public UUID authorId() { return authorId; }
    public List<CircuitStep> steps() { return steps; }
    public List<Decision> decisions() { return List.copyOf(decisions); }
    public int currentStep() { return currentStep; }
    public Status status() { return status; }
    public Instant startedAt() { return startedAt; }
    public Instant endedAt() { return endedAt; }
}
