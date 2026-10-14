package com.openlab.qualitos.quality.change.domain;

import java.util.Map;

/**
 * Un refus de la gestion des changements, avec ce qu'il faut pour l'expliquer.
 *
 * <p>{@code code} est stable ({@code moc.analysis-incomplete}…) : l'écran le
 * traduit dans la langue de l'utilisateur, et ne montre jamais le message brut
 * du serveur (OWASP A09). {@code details} porte ce que la phrase nomme — le
 * rôle sans titulaire, le champ fautif.
 */
public class MocException extends RuntimeException {

    /** La nature du refus, qui décide du statut HTTP. */
    public enum Kind {
        /** La demande est mal formée (400). */
        INVALID,
        /** L'utilisateur n'a pas le droit de faire cela, ici (403). */
        FORBIDDEN,
        /** L'état de la demande ne le permet pas (409). */
        CONFLICT,
        /** L'objet n'existe pas pour ce client (404). */
        NOT_FOUND
    }

    private final Kind kind;
    private final String code;
    private final transient Map<String, String> details;

    public MocException(Kind kind, String code, String message) {
        this(kind, code, message, Map.of());
    }

    public MocException(Kind kind, String code, String message, Map<String, String> details) {
        super(message);
        this.kind = kind;
        this.code = code;
        this.details = Map.copyOf(details);
    }

    public static MocException invalid(String code, String message) {
        return new MocException(Kind.INVALID, code, message);
    }

    public static MocException conflict(String code, String message) {
        return new MocException(Kind.CONFLICT, code, message);
    }

    public Kind kind() {
        return kind;
    }

    public String code() {
        return code;
    }

    public Map<String, String> details() {
        return details;
    }
}
