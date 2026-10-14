package com.openlab.qualitos.quality.change.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Le poids de chacun des huit domaines dans le score, réglé par client.
 *
 * <p>Entier de 0 à 10, 1 par défaut partout (ce qui redonne la moyenne simple).
 * Un domaine de poids 0 reste à évaluer — la règle « huit domaines évalués
 * avant soumission » ne change pas — mais ne compte pas dans le score. Il en
 * faut au moins un de poids non nul, sans quoi le score n'aurait pas de
 * dénominateur.
 */
public final class MocWeights {

    public static final int MIN = 0;
    public static final int MAX = 10;

    private final Map<ImpactDomain, Integer> weights;

    private MocWeights(Map<ImpactDomain, Integer> weights) {
        this.weights = Collections.unmodifiableMap(weights);
    }

    /** Un poids de 1 pour chaque domaine. */
    public static MocWeights defaults() {
        EnumMap<ImpactDomain, Integer> m = new EnumMap<>(ImpactDomain.class);
        for (ImpactDomain d : ImpactDomain.values()) {
            m.put(d, 1);
        }
        return new MocWeights(m);
    }

    /**
     * Des poids complets et bornés ; un domaine absent est une erreur, pas un
     * poids implicite : l'administrateur doit voir ce qu'il enregistre.
     */
    public static MocWeights of(Map<ImpactDomain, Integer> source) {
        if (source == null) {
            throw MocException.invalid("moc.settings.weights", "Les poids des huit domaines sont obligatoires.");
        }
        EnumMap<ImpactDomain, Integer> m = new EnumMap<>(ImpactDomain.class);
        int total = 0;
        for (ImpactDomain d : ImpactDomain.values()) {
            Integer w = source.get(d);
            if (w == null || w < MIN || w > MAX) {
                throw MocException.invalid("moc.settings.weights",
                        "Le poids du domaine " + d + " doit être un entier de 0 à 10.");
            }
            m.put(d, w);
            total += w;
        }
        if (total == 0) {
            throw MocException.invalid("moc.settings.weights-zero",
                    "Au moins un domaine doit avoir un poids supérieur à 0.");
        }
        return new MocWeights(m);
    }

    public int weightOf(ImpactDomain domain) {
        return weights.get(domain);
    }

    public Map<ImpactDomain, Integer> asMap() {
        return weights;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MocWeights other && weights.equals(other.weights);
    }

    @Override
    public int hashCode() {
        return weights.hashCode();
    }
}
