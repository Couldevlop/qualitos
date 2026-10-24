package com.openlab.qualitos.quality.dmaic.resolution.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Le paramétrage de la fiche de résolution d'un client : son questionnaire, son
 * catalogue de types de défaillance, son échelle de sévérité.
 *
 * <p>Tenu par l'administrateur du client. Sans réglage, la plateforme propose
 * les valeurs du classeur ({@link ResolutionDefaults}) : la fiche s'utilise dès
 * le premier jour.
 *
 * <p>Retirer une question ne supprime pas les réponses déjà données : elles
 * restent enregistrées, l'écran ne les pose plus. Remettre la question (même
 * code) les fait réapparaître.
 *
 * @param defaults vrai quand ce sont les valeurs de la plateforme, faute de réglage
 */
public record ResolutionSettings(List<SheetQuestion> questions, List<FailureType> failureTypes,
                                 List<SeverityLevel> severities, boolean defaults) {

    public static final int MAX_QUESTIONS = 150;
    public static final int MAX_FAILURE_TYPES = 300;
    public static final int MAX_SEVERITIES = 10;
    public static final int LABEL_MAX = 500;
    public static final int FAILURE_LABEL_MAX = 200;
    public static final int SEVERITY_LABEL_MAX = 80;

    /** Un code se tape et se relit : lettres capitales, chiffres, tiret, point ou soulignement. */
    private static final Pattern CODE = Pattern.compile("[A-Z0-9][A-Z0-9._-]{0,19}");

    public ResolutionSettings {
        questions = List.copyOf(questions);
        failureTypes = List.copyOf(failureTypes);
        severities = List.copyOf(severities);
    }

    public static ResolutionSettings platformDefaults() {
        return new ResolutionSettings(ResolutionDefaults.QUESTIONS, ResolutionDefaults.FAILURE_TYPES,
                ResolutionDefaults.SEVERITIES, true);
    }

    /**
     * Le réglage de l'administrateur, nettoyé (espaces de bord retirés, codes en
     * capitales) et vérifié : au moins une question, des codes uniques et
     * lisibles, une rubrique de définition seulement pour la phase « Définir ».
     */
    public static ResolutionSettings configured(List<SheetQuestion> questions, List<FailureType> failureTypes,
                                                List<SeverityLevel> severities) {
        return new ResolutionSettings(questions(questions), failureTypes(failureTypes),
                severities(severities), false);
    }

    public Optional<SheetQuestion> question(String code) {
        return questions.stream().filter(q -> q.code().equals(code)).findFirst();
    }

    public boolean knowsFailureType(String code) {
        return failureTypes.stream().anyMatch(f -> f.code().equals(code));
    }

    public boolean knowsSeverity(int level) {
        return severities.stream().anyMatch(s -> s.level() == level);
    }

    private static List<SheetQuestion> questions(List<SheetQuestion> raw) {
        if (raw == null || raw.isEmpty()) {
            throw new ResolutionValidationException("questions", "At least one question is required");
        }
        if (raw.size() > MAX_QUESTIONS) {
            throw new ResolutionValidationException("questions", "At most " + MAX_QUESTIONS + " questions");
        }
        List<SheetQuestion> clean = new ArrayList<>(raw.size());
        Set<String> seen = new HashSet<>();
        for (SheetQuestion q : raw) {
            if (q == null || q.section() == null || q.answerType() == null) {
                throw new ResolutionValidationException("questions", "A question needs a section and an answer type");
            }
            String code = code(q.code(), "questions");
            if (!seen.add(code)) {
                throw new ResolutionValidationException("questions", "Duplicate question code: " + code);
            }
            if (q.topic() != null && q.section() != SheetSection.DEFINE) {
                throw new ResolutionValidationException("questions",
                        "Only a question of the Define phase has a topic: " + code);
            }
            clean.add(new SheetQuestion(code, q.section(), q.topic(), label(q.label(), LABEL_MAX, "questions"),
                    q.answerType()));
        }
        return clean;
    }

    private static List<FailureType> failureTypes(List<FailureType> raw) {
        List<FailureType> source = raw == null ? List.of() : raw;
        if (source.size() > MAX_FAILURE_TYPES) {
            throw new ResolutionValidationException("failureTypes",
                    "At most " + MAX_FAILURE_TYPES + " failure types");
        }
        List<FailureType> clean = new ArrayList<>(source.size());
        Set<String> seen = new HashSet<>();
        for (FailureType f : source) {
            if (f == null) {
                throw new ResolutionValidationException("failureTypes", "Empty failure type");
            }
            String code = code(f.code(), "failureTypes");
            if (!seen.add(code)) {
                throw new ResolutionValidationException("failureTypes", "Duplicate failure type: " + code);
            }
            clean.add(new FailureType(code, label(f.label(), FAILURE_LABEL_MAX, "failureTypes")));
        }
        return clean;
    }

    private static List<SeverityLevel> severities(List<SeverityLevel> raw) {
        if (raw == null || raw.isEmpty()) {
            throw new ResolutionValidationException("severities", "At least one severity level is required");
        }
        if (raw.size() > MAX_SEVERITIES) {
            throw new ResolutionValidationException("severities", "At most " + MAX_SEVERITIES + " severity levels");
        }
        List<SeverityLevel> clean = new ArrayList<>(raw.size());
        Set<Integer> seen = new HashSet<>();
        for (SeverityLevel s : raw) {
            if (s == null || s.level() < 1 || s.level() > MAX_SEVERITIES) {
                throw new ResolutionValidationException("severities",
                        "A severity level is between 1 and " + MAX_SEVERITIES);
            }
            if (!seen.add(s.level())) {
                throw new ResolutionValidationException("severities", "Duplicate severity level: " + s.level());
            }
            clean.add(new SeverityLevel(s.level(), label(s.label(), SEVERITY_LABEL_MAX, "severities")));
        }
        return clean;
    }

    private static String code(String raw, String field) {
        String code = raw == null ? "" : raw.strip().toUpperCase(java.util.Locale.ROOT);
        if (!CODE.matcher(code).matches()) {
            throw new ResolutionValidationException(field,
                    "A code is 1 to 20 capital letters, digits, dots, dashes or underscores: " + raw);
        }
        return code;
    }

    private static String label(String raw, int max, String field) {
        String label = raw == null ? "" : raw.strip();
        if (label.isEmpty() || label.length() > max) {
            throw new ResolutionValidationException(field, "A label is 1 to " + max + " characters");
        }
        return label;
    }
}
