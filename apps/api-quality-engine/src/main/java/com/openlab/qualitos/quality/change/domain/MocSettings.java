package com.openlab.qualitos.quality.change.domain;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Le paramétrage de la gestion des changements d'un client (ADR 0087) :
 * seuils de classification, poids des domaines, délai de rappel des
 * validateurs, rôles de validation, leurs titulaires et les classifications
 * qui les exigent.
 *
 * <p>Un client qui n'a rien réglé reçoit {@link #defaults()} : seuils 25 / 50,
 * poids 1, rappel à 3 jours, et les quatre rôles de la maquette sans titulaire — une demande ne
 * peut alors pas partir en validation tant que l'administrateur n'a nommé
 * personne (refus explicite, qui nomme le rôle).
 */
public record MocSettings(MocThresholds thresholds, MocWeights weights, int reminderDays,
                          List<ValidationRole> roles) {

    public static final int MAX_ROLES = 20;
    public static final int MAX_HOLDERS = 20;
    public static final int DEFAULT_REMINDER_DAYS = 3;
    public static final int MIN_REMINDER_DAYS = 1;
    public static final int MAX_REMINDER_DAYS = 30;

    public MocSettings {
        roles = roles == null ? List.of() : List.copyOf(roles);
        if (reminderDays < MIN_REMINDER_DAYS || reminderDays > MAX_REMINDER_DAYS) {
            throw MocException.invalid("moc.settings.reminder-days",
                    "Le délai de rappel est un nombre de jours de 1 à 30.");
        }
    }

    /** Le paramétrage livré : celui de la maquette, sans titulaire. */
    public static MocSettings defaults() {
        Set<MocClassification> tous = EnumSet.of(MocClassification.MINOR, MocClassification.MAJOR,
                MocClassification.CRITICAL);
        return new MocSettings(MocThresholds.DEFAULT, MocWeights.defaults(), DEFAULT_REMINDER_DAYS, List.of(
                new ValidationRole(null, "Responsable de service", tous, List.of()),
                new ValidationRole(null, "HSE", tous, List.of()),
                new ValidationRole(null, "Directeur technique",
                        EnumSet.of(MocClassification.MAJOR, MocClassification.CRITICAL), List.of()),
                new ValidationRole(null, "Directeur de site", EnumSet.of(MocClassification.CRITICAL), List.of())));
    }

    /**
     * Vérifie ce que l'administrateur enregistre. Les seuils et les poids se
     * vérifient à leur construction ; ici, les rôles.
     */
    public MocSettings validated() {
        if (roles.isEmpty() || roles.size() > MAX_ROLES) {
            throw MocException.invalid("moc.settings.roles-count",
                    "Il faut entre 1 et " + MAX_ROLES + " rôles de validation.");
        }
        Set<String> libelles = new HashSet<>();
        Set<UUID> ids = new HashSet<>();
        for (ValidationRole r : roles) {
            String label = r.label() == null ? "" : r.label().strip();
            if (label.isEmpty() || label.length() > ValidationRole.LABEL_MAX) {
                throw MocException.invalid("moc.settings.role-label",
                        "Chaque rôle porte un libellé de 1 à " + ValidationRole.LABEL_MAX + " caractères.");
            }
            if (!libelles.add(label.toLowerCase(Locale.ROOT))) {
                throw new MocException(MocException.Kind.INVALID, "moc.settings.role-duplicate",
                        "Deux rôles portent le même libellé : " + label + ".", Map.of("role", label));
            }
            if (r.requiredFor().contains(MocClassification.NOT_ASSESSED)) {
                throw MocException.invalid("moc.settings.role-required",
                        "Un rôle n'est requis que pour une classification évaluée.");
            }
            if (r.id() != null && !ids.add(r.id())) {
                throw MocException.invalid("moc.settings.role-id", "Un même rôle apparaît deux fois.");
            }
            verifierTitulaires(r, label);
        }
        for (MocClassification c : List.of(MocClassification.MINOR, MocClassification.MAJOR,
                MocClassification.CRITICAL)) {
            if (requiredRoles(c).isEmpty()) {
                throw new MocException(MocException.Kind.INVALID, "moc.settings.required-empty",
                        "Chaque classification exige au moins un rôle de validation (" + c + ").",
                        Map.of("classification", c.name()));
            }
        }
        return this;
    }

    private static void verifierTitulaires(ValidationRole r, String label) {
        if (r.holders().size() > MAX_HOLDERS) {
            throw MocException.invalid("moc.settings.holders-count",
                    "Un rôle compte au plus " + MAX_HOLDERS + " titulaires.");
        }
        Set<UUID> vus = new HashSet<>();
        for (ValidationRole.RoleHolder h : r.holders()) {
            if (h == null || h.userId() == null) {
                throw MocException.invalid("moc.settings.holder", "Un titulaire doit être un membre de l'annuaire.");
            }
            String l = h.label() == null ? "" : h.label().strip();
            if (l.isEmpty() || l.length() > ValidationRole.RoleHolder.LABEL_MAX) {
                throw MocException.invalid("moc.settings.holder", "Un titulaire doit être un membre de l'annuaire.");
            }
            if (h.email() != null
                    && (h.email().length() > ValidationRole.RoleHolder.EMAIL_MAX || !h.email().contains("@"))) {
                throw MocException.invalid("moc.settings.holder", "Un titulaire doit être un membre de l'annuaire.");
            }
            if (!vus.add(h.userId())) {
                throw new MocException(MocException.Kind.INVALID, "moc.settings.holder-duplicate",
                        "Une même personne est nommée deux fois au rôle " + label + ".", Map.of("role", label));
            }
        }
    }

    /** Les rôles qu'exige une classification, dans l'ordre du paramétrage. */
    public List<ValidationRole> requiredRoles(MocClassification classification) {
        return roles.stream().filter(r -> r.requiredFor(classification)).toList();
    }
}
