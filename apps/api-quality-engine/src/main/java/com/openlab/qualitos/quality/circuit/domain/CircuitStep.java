package com.openlab.qualitos.quality.circuit.domain;

import java.util.regex.Pattern;

/**
 * Une étape d'un circuit : qui doit approuver (un rôle du client, système ou sur
 * mesure — ADR 0078) et combien de ses porteurs.
 *
 * @param minApprovals approbations distinctes exigées à cette étape (1 à 10)
 */
public record CircuitStep(String name, String roleCode, int minApprovals) {

    public static final int NAME_MAX = 120;
    public static final int MAX_APPROVALS = 10;
    private static final Pattern ROLE = Pattern.compile("^[A-Z][A-Z0-9_]{1,63}$");

    public CircuitStep {
        name = name == null ? "" : name.strip();
        if (name.isEmpty() || name.length() > NAME_MAX) {
            throw new CircuitException(CircuitException.Reason.INVALID, "steps",
                    "Chaque étape porte un nom de 1 à " + NAME_MAX + " caractères.");
        }
        if (roleCode == null || !ROLE.matcher(roleCode).matches()) {
            throw new CircuitException(CircuitException.Reason.INVALID, "steps",
                    "Chaque étape désigne le rôle qui approuve.");
        }
        if ("SUPER_ADMIN".equals(roleCode)) {
            throw new CircuitException(CircuitException.Reason.INVALID, "steps",
                    "Le super administrateur appartient à l'éditeur : il n'approuve pas dans un client.");
        }
        if (minApprovals < 1 || minApprovals > MAX_APPROVALS) {
            throw new CircuitException(CircuitException.Reason.INVALID, "steps",
                    "Une étape exige de 1 à " + MAX_APPROVALS + " approbations.");
        }
    }
}
