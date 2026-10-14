package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.change.domain.ImpactDomain;
import com.openlab.qualitos.quality.change.domain.ImpactLevel;
import com.openlab.qualitos.quality.change.domain.MocScoring;
import com.openlab.qualitos.quality.change.domain.MocSettings;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reporte sur une demande ce que son analyse d'impact donne : score,
 * classification, et la synthèse texte reprise par le registre des risques.
 *
 * <p>Appelée tant que la demande est ouverte (brouillon, analyse) — après chaque
 * évaluation et après chaque changement de paramétrage — et JAMAIS au-delà : à
 * la soumission, score et classification sont figés.
 */
final class MocAnalysis {

    static final int SUMMARY_MAX = 2000;

    private MocAnalysis() {}

    static MocScoring.Assessment apply(ChangeRequest c, List<ChangeImpactAssessment> assessments,
                                       MocSettings settings) {
        MocScoring.Assessment a = MocScoring.evaluate(levels(assessments), settings.weights(),
                settings.thresholds());
        c.setImpactScore(a.score());
        c.setClassification(a.classification());
        String synthese = summary(assessments);
        if (synthese != null) {
            c.setImpactSummary(synthese);
        }
        return a;
    }

    static Map<ImpactDomain, ImpactLevel> levels(List<ChangeImpactAssessment> assessments) {
        Map<ImpactDomain, ImpactLevel> m = new EnumMap<>(ImpactDomain.class);
        for (ChangeImpactAssessment a : assessments) {
            m.put(a.getDomain(), a.getLevel());
        }
        return m;
    }

    /**
     * Les natures d'impact des domaines touchés, du plus fort au plus faible :
     * c'est « l'effet » d'un risque inscrit au registre depuis la demande. Rien
     * quand aucun domaine touché n'est décrit — on garde alors la synthèse d'avant
     * (reprise d'une demande antérieure au MOC).
     */
    static String summary(List<ChangeImpactAssessment> assessments) {
        String s = assessments.stream()
                .filter(a -> a.getLevel() != ImpactLevel.NONE)
                .filter(a -> a.getNotes() != null && !a.getNotes().isBlank())
                .sorted(Comparator.comparing((ChangeImpactAssessment a) -> a.getLevel()).reversed()
                        .thenComparing(ChangeImpactAssessment::getDomain))
                .map(a -> a.getNotes().strip())
                .collect(Collectors.joining(" ; "));
        if (s.isEmpty()) {
            return null;
        }
        return s.length() <= SUMMARY_MAX ? s : s.substring(0, SUMMARY_MAX);
    }
}
