package com.openlab.qualitos.quality.change.domain;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static com.openlab.qualitos.quality.change.domain.ImpactDomain.*;
import static com.openlab.qualitos.quality.change.domain.ImpactLevel.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Le score d'impact, sa pondération et sa classification (ADR 0087). */
class MocScoringTest {

    /** La maquette : Moyen, Faible, Faible, Élevé, Moyen, Moyen, Aucun, Moyen → 13 / 24. */
    static Map<ImpactDomain, ImpactLevel> maquette() {
        Map<ImpactDomain, ImpactLevel> m = new EnumMap<>(ImpactDomain.class);
        m.put(SAFETY, MEDIUM);
        m.put(HEALTH_ENVIRONMENT, LOW);
        m.put(QUALITY, LOW);
        m.put(PRODUCTION, HIGH);
        m.put(MAINTENANCE, MEDIUM);
        m.put(COSTS, MEDIUM);
        m.put(REGULATORY, NONE);
        m.put(TRAINING, MEDIUM);
        return m;
    }

    @Test
    void laMaquetteDonne54EtCritique() {
        MocScoring.Assessment a = MocScoring.evaluate(maquette(), MocWeights.defaults(), MocThresholds.DEFAULT);
        assertThat(a.complete()).isTrue();
        assertThat(a.score()).isEqualTo(54);
        assertThat(a.classification()).isEqualTo(MocClassification.CRITICAL);
    }

    @Test
    void tantQuUnDomaineManqueRienNEstEvalue() {
        Map<ImpactDomain, ImpactLevel> m = maquette();
        m.remove(TRAINING);
        MocScoring.Assessment a = MocScoring.evaluate(m, MocWeights.defaults(), MocThresholds.DEFAULT);
        assertThat(a.complete()).isFalse();
        assertThat(a.score()).isNull();
        assertThat(a.classification()).isEqualTo(MocClassification.NOT_ASSESSED);
        assertThat(MocScoring.evaluate(null, MocWeights.defaults(), MocThresholds.DEFAULT).complete()).isFalse();
    }

    @Test
    void lesExtremesDonnent0Et100() {
        Map<ImpactDomain, ImpactLevel> rien = new EnumMap<>(ImpactDomain.class);
        Map<ImpactDomain, ImpactLevel> tout = new EnumMap<>(ImpactDomain.class);
        for (ImpactDomain d : ImpactDomain.values()) {
            rien.put(d, NONE);
            tout.put(d, HIGH);
        }
        assertThat(MocScoring.evaluate(rien, MocWeights.defaults(), MocThresholds.DEFAULT).classification())
                .isEqualTo(MocClassification.MINOR);
        assertThat(MocScoring.evaluate(rien, MocWeights.defaults(), MocThresholds.DEFAULT).score()).isZero();
        assertThat(MocScoring.evaluate(tout, MocWeights.defaults(), MocThresholds.DEFAULT).score()).isEqualTo(100);
    }

    @Test
    void lePoidsPondereLeScore() {
        // Sécurité pèse 5 : (2×5 + 1+1+3+2+2+0+2) / (3×5 + 3×7) = 21 / 36 = 58,3 → 58.
        Map<ImpactDomain, Integer> w = poids(1);
        w.put(SAFETY, 5);
        MocScoring.Assessment a = MocScoring.evaluate(maquette(), MocWeights.of(w), MocThresholds.DEFAULT);
        assertThat(a.score()).isEqualTo(58);
    }

    @Test
    void unDomaineDePoidsNulDoitEtreEvalueMaisNeComptePas() {
        Map<ImpactDomain, Integer> w = poids(1);
        w.put(PRODUCTION, 0);
        // Sans production (Élevé) : 10 / 21 = 47,6 → 48, majeur.
        MocScoring.Assessment a = MocScoring.evaluate(maquette(), MocWeights.of(w), MocThresholds.DEFAULT);
        assertThat(a.score()).isEqualTo(48);
        assertThat(a.classification()).isEqualTo(MocClassification.MAJOR);

        Map<ImpactDomain, ImpactLevel> incomplet = maquette();
        incomplet.remove(PRODUCTION);
        assertThat(MocScoring.evaluate(incomplet, MocWeights.of(w), MocThresholds.DEFAULT).complete()).isFalse();
    }

    @Test
    void lesSeuilsDuClientDecidentDeLaClasse() {
        MocThresholds t = new MocThresholds(40, 60);
        assertThat(MocScoring.evaluate(maquette(), MocWeights.defaults(), t).classification())
                .isEqualTo(MocClassification.MAJOR);
        assertThat(t.classify(39)).isEqualTo(MocClassification.MINOR);
        assertThat(t.classify(40)).isEqualTo(MocClassification.MAJOR);
        assertThat(t.classify(59)).isEqualTo(MocClassification.MAJOR);
        assertThat(t.classify(60)).isEqualTo(MocClassification.CRITICAL);
    }

    @Test
    void lesSeuilsParDefautSont25Et50() {
        assertThat(MocThresholds.DEFAULT.classify(24)).isEqualTo(MocClassification.MINOR);
        assertThat(MocThresholds.DEFAULT.classify(25)).isEqualTo(MocClassification.MAJOR);
        assertThat(MocThresholds.DEFAULT.classify(49)).isEqualTo(MocClassification.MAJOR);
        assertThat(MocThresholds.DEFAULT.classify(50)).isEqualTo(MocClassification.CRITICAL);
    }

    @Test
    void desSeuilsIncoherentsSontRefuses() {
        assertThatThrownBy(() -> new MocThresholds(0, 50)).isInstanceOf(MocException.class)
                .extracting(e -> ((MocException) e).code()).isEqualTo("moc.settings.thresholds");
        assertThatThrownBy(() -> new MocThresholds(50, 50)).isInstanceOf(MocException.class);
        assertThatThrownBy(() -> new MocThresholds(60, 50)).isInstanceOf(MocException.class);
        assertThatThrownBy(() -> new MocThresholds(25, 101)).isInstanceOf(MocException.class);
        assertThat(new MocThresholds(1, 100).critical()).isEqualTo(100);
    }

    @Test
    void desPoidsIncompletsHorsBornesOuTousNulsSontRefuses() {
        assertThatThrownBy(() -> MocWeights.of(null)).isInstanceOf(MocException.class);
        Map<ImpactDomain, Integer> manque = poids(1);
        manque.remove(COSTS);
        assertThatThrownBy(() -> MocWeights.of(manque)).isInstanceOf(MocException.class);
        Map<ImpactDomain, Integer> trop = poids(1);
        trop.put(COSTS, 11);
        assertThatThrownBy(() -> MocWeights.of(trop)).isInstanceOf(MocException.class);
        Map<ImpactDomain, Integer> negatif = poids(1);
        negatif.put(COSTS, -1);
        assertThatThrownBy(() -> MocWeights.of(negatif)).isInstanceOf(MocException.class);
        assertThatThrownBy(() -> MocWeights.of(poids(0))).isInstanceOf(MocException.class)
                .extracting(e -> ((MocException) e).code()).isEqualTo("moc.settings.weights-zero");
    }

    @Test
    void lesPoidsSeComparentParValeur() {
        assertThat(MocWeights.of(poids(1))).isEqualTo(MocWeights.defaults());
        assertThat(MocWeights.of(poids(1)).hashCode()).isEqualTo(MocWeights.defaults().hashCode());
        assertThat(MocWeights.of(poids(2))).isNotEqualTo(MocWeights.defaults());
        assertThat(MocWeights.defaults().asMap()).hasSize(8);
    }

    @Test
    void lesNiveauxValentDeZeroATrois() {
        assertThat(NONE.points()).isZero();
        assertThat(LOW.points()).isEqualTo(1);
        assertThat(MEDIUM.points()).isEqualTo(2);
        assertThat(HIGH.points()).isEqualTo(ImpactLevel.MAX_POINTS);
        assertThat(MocClassification.NOT_ASSESSED.assessed()).isFalse();
        assertThat(MocClassification.MINOR.assessed()).isTrue();
    }

    static Map<ImpactDomain, Integer> poids(int w) {
        Map<ImpactDomain, Integer> m = new EnumMap<>(ImpactDomain.class);
        for (ImpactDomain d : ImpactDomain.values()) {
            m.put(d, w);
        }
        return m;
    }
}
