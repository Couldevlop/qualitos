package com.openlab.qualitos.quality.spc.capability.domain;

import java.math.BigDecimal;

/**
 * Les deux seuils de Cpk d'un client : maîtrise et surveillance.
 *
 * <p>Paramétrés par l'administrateur du client, jamais codés en dur dans
 * l'écran. Une étude recopie les seuils en vigueur à sa création et les garde :
 * relever le seuil plus tard ne change pas après coup le verdict d'une étude
 * déjà présentée en revue.
 *
 * @param capable Cpk à partir duquel le procédé est maîtrisé
 * @param watch   Cpk à partir duquel un procédé non maîtrisé est seulement « à surveiller »
 */
public record CapabilityThresholds(BigDecimal capable, BigDecimal watch) {

    /** NUMERIC(4,2) : deux chiffres avant la virgule. Déclaré avant DEFAULT, qui le lit. */
    private static final BigDecimal MAX = new BigDecimal("10.00");

    public static final CapabilityThresholds DEFAULT =
            new CapabilityThresholds(new BigDecimal("1.67"), new BigDecimal("1.33"));

    public CapabilityThresholds {
        if (capable == null) {
            throw new SpcValidationException("capableThreshold", "Le seuil de maîtrise est obligatoire.");
        }
        if (watch == null) {
            throw new SpcValidationException("watchThreshold", "Le seuil de surveillance est obligatoire.");
        }
        if (capable.scale() > 2 || watch.scale() > 2) {
            throw new SpcValidationException("capableThreshold", "Un seuil a au plus deux décimales.");
        }
        if (watch.signum() <= 0) {
            throw new SpcValidationException("watchThreshold", "Le seuil de surveillance doit être positif.");
        }
        if (capable.compareTo(MAX) > 0) {
            throw new SpcValidationException("capableThreshold", "Le seuil de maîtrise ne dépasse pas 10.");
        }
        if (watch.compareTo(capable) >= 0) {
            throw new SpcValidationException("watchThreshold",
                    "Le seuil de surveillance doit être inférieur au seuil de maîtrise.");
        }
    }

    public CapabilityStatus classify(double cpk) {
        if (cpk >= capable.doubleValue()) {
            return CapabilityStatus.CAPABLE;
        }
        return cpk >= watch.doubleValue() ? CapabilityStatus.WATCH : CapabilityStatus.NOT_CAPABLE;
    }
}
