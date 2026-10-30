package com.openlab.qualitos.quality.dmaic.resolution.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * La fiche de résolution d'un projet DMAIC : une par projet.
 *
 * <p><b>Elle se vérifie contre le paramétrage du client</b> au moment où on
 * l'écrit : une réponse « Peut-être » à une question Oui/Non, une sévérité ou un
 * type de défaillance absents du catalogue sont refusés. Ce qui a été accepté
 * hier reste lisible demain, même si l'administrateur a changé le catalogue
 * entre-temps — on ne réécrit pas l'histoire d'une réclamation.
 */
public final class ResolutionSheet {

    public static final int SHORT_MAX = 200;
    public static final int CLAIM_MAX = 60;
    public static final int SUMMARY_MAX = 4000;
    public static final int TEAM_MAX = 15;
    public static final int WHYS_MAX = 30;
    public static final int WHY_TEXT_MAX = 500;
    public static final int ACTIONS_MAX = 100;
    public static final int ACTION_TEXT_MAX = 1000;

    private final UUID id;
    private final UUID tenantId;
    private final UUID projectId;
    private ResolutionContent content;
    private final UUID createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    public ResolutionSheet(UUID id, UUID tenantId, UUID projectId, ResolutionContent content, UUID createdBy,
                           Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.projectId = Objects.requireNonNull(projectId, "projectId");
        this.content = Objects.requireNonNull(content, "content");
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** La fiche vierge d'un projet, pas encore enregistrée. */
    public static ResolutionSheet blank(UUID tenantId, UUID projectId, UUID actor, Instant now) {
        return new ResolutionSheet(null, tenantId, projectId, ResolutionContent.empty(), actor, now, now);
    }

    /**
     * Remplace le contenu par la saisie, nettoyée et vérifiée contre le
     * paramétrage. Les réponses aux questions que le catalogue ne pose plus sont
     * conservées telles qu'elles étaient.
     */
    public void revise(ResolutionContent saisie, ResolutionSettings settings, Instant now) {
        Objects.requireNonNull(saisie, "content");
        if (saisie.openedOn() != null && saisie.closedOn() != null && saisie.closedOn().isBefore(saisie.openedOn())) {
            throw new ResolutionValidationException("closedOn", "The closing date is before the creation date");
        }
        Integer severity = saisie.severity();
        if (severity != null && !settings.knowsSeverity(severity)) {
            throw new ResolutionValidationException("severity", "Unknown severity level: " + severity);
        }
        String failureType = trim(saisie.failureTypeCode(), 20, "failureTypeCode");
        if (failureType != null) {
            failureType = failureType.toUpperCase(Locale.ROOT);
        }
        if (failureType != null && !settings.knowsFailureType(failureType)) {
            throw new ResolutionValidationException("failureTypeCode", "Unknown failure type: " + failureType);
        }
        this.content = new ResolutionContent(
                trim(saisie.clientName(), SHORT_MAX, "clientName"),
                trim(saisie.claimNumber(), CLAIM_MAX, "claimNumber"),
                person(saisie.problemOwner(), "problemOwner"),
                team(saisie.team()),
                saisie.openedOn(), saisie.closedOn(), severity, failureType,
                trim(saisie.customerSymptoms(), SUMMARY_MAX, "customerSymptoms"),
                answers(saisie.answers(), settings),
                trim(saisie.measureSummary(), SUMMARY_MAX, "measureSummary"),
                trim(saisie.containmentMeasures(), SUMMARY_MAX, "containmentMeasures"),
                trim(saisie.rootCause(), SUMMARY_MAX, "rootCause"),
                trim(saisie.retainedSolutions(), SUMMARY_MAX, "retainedSolutions"),
                trim(saisie.surveillancePlan(), SUMMARY_MAX, "surveillancePlan"),
                whys(saisie.whys()),
                actions(saisie.actions()),
                saisie.ishikawaId(), saisie.measuresEnabled(), saisie.capabilityStudy(), saisie.gageRrStudy());
        this.updatedAt = now;
    }

    private Map<String, String> answers(Map<String, String> saisies, ResolutionSettings settings) {
        Map<String, String> result = new HashMap<>();
        // Les réponses que le catalogue ne pose plus : gardées, l'écran ne les voit pas.
        content.answers().forEach((code, value) -> {
            if (settings.question(code).isEmpty()) {
                result.put(code, value);
            }
        });
        for (Map.Entry<String, String> e : saisies.entrySet()) {
            SheetQuestion question = settings.question(e.getKey()).orElseThrow(() ->
                    new ResolutionValidationException("answers", "Unknown question: " + e.getKey()));
            String value = e.getValue() == null ? "" : e.getValue().strip();
            if (value.isEmpty()) {
                continue;
            }
            if (!question.answerType().accepts(value)) {
                throw new ResolutionValidationException("answers." + question.code(),
                        "Answer not allowed for " + question.code() + " (" + question.answerType() + ")");
            }
            result.put(question.code(), value);
        }
        return result;
    }

    private static List<Person> team(List<Person> raw) {
        if (raw.size() > TEAM_MAX) {
            throw new ResolutionValidationException("team", "At most " + TEAM_MAX + " team members");
        }
        List<Person> clean = new ArrayList<>(raw.size());
        Set<UUID> seen = new HashSet<>();
        for (Person p : raw) {
            Person member = person(p, "team");
            if (member == null) {
                throw new ResolutionValidationException("team", "A team member is chosen from the directory");
            }
            if (!seen.add(member.accountId())) {
                throw new ResolutionValidationException("team", "A team member appears twice: " + member.name());
            }
            clean.add(member);
        }
        return clean;
    }

    private static List<WhyChain> whys(List<WhyChain> raw) {
        if (raw.size() > WHYS_MAX) {
            throw new ResolutionValidationException("whys", "At most " + WHYS_MAX + " cause chains");
        }
        List<WhyChain> clean = new ArrayList<>(raw.size());
        for (WhyChain w : raw) {
            if (w == null || w.branch() == null) {
                throw new ResolutionValidationException("whys", "A cause chain belongs to a branch");
            }
            String cause = trim(w.cause(), WHY_TEXT_MAX, "whys");
            if (cause == null) {
                throw new ResolutionValidationException("whys", "A cause chain starts with a cause");
            }
            List<String> pourquoi = new ArrayList<>();
            for (String why : w.whys()) {
                String t = trim(why, WHY_TEXT_MAX, "whys");
                if (t != null) {
                    pourquoi.add(t);
                }
            }
            if (pourquoi.size() > WhyChain.MAX_WHYS) {
                throw new ResolutionValidationException("whys", "At most " + WhyChain.MAX_WHYS + " whys per cause");
            }
            clean.add(new WhyChain(w.branch(), cause, pourquoi, w.rootCause(), trim(w.action(), WHY_TEXT_MAX, "whys")));
        }
        return clean;
    }

    private static List<ActionLine> actions(List<ActionLine> raw) {
        if (raw.size() > ACTIONS_MAX) {
            throw new ResolutionValidationException("actions", "At most " + ACTIONS_MAX + " actions");
        }
        List<ActionLine> clean = new ArrayList<>(raw.size());
        for (ActionLine a : raw) {
            if (a == null || a.status() == null) {
                throw new ResolutionValidationException("actions", "An action has a status");
            }
            String action = trim(a.action(), ACTION_TEXT_MAX, "actions");
            if (action == null) {
                throw new ResolutionValidationException("actions", "An action says what is to be done");
            }
            if (a.percent() < 0 || a.percent() > 100) {
                throw new ResolutionValidationException("actions", "Progress is between 0 and 100 %");
            }
            clean.add(new ActionLine(a.createdOn(), action, person(a.responsible(), "actions"), a.status(),
                    a.plannedOn(), a.doneOn(), a.percent(), trim(a.notes(), ACTION_TEXT_MAX, "actions"),
                    a.capaId(), a.containment()));
        }
        return clean;
    }

    /** Une personne de l'annuaire, ou personne : jamais un nom sans compte. */
    private static Person person(Person p, String field) {
        if (p == null || (p.accountId() == null && (p.name() == null || p.name().isBlank()))) {
            return null;
        }
        if (p.accountId() == null) {
            throw new ResolutionValidationException(field, "A person is chosen from the directory");
        }
        String name = trim(p.name(), Person.NAME_MAX, field);
        if (name == null) {
            throw new ResolutionValidationException(field, "A person has a name");
        }
        return new Person(p.accountId(), name);
    }

    private static String trim(String raw, int max, String field) {
        if (raw == null) {
            return null;
        }
        String t = raw.strip();
        if (t.isEmpty()) {
            return null;
        }
        if (t.length() > max) {
            throw new ResolutionValidationException(field, "At most " + max + " characters");
        }
        return t;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public ResolutionContent getContent() {
        return content;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
