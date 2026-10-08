package com.openlab.qualitos.quality.smi.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SmiDomainTest {

    @Test
    void lEtatDUneCaseSeDeduitDesExigencesProuvees() {
        assertThat(CoverageStatus.of(5, 5)).isEqualTo(CoverageStatus.COVERED);
        assertThat(CoverageStatus.of(6, 5)).isEqualTo(CoverageStatus.COVERED);
        assertThat(CoverageStatus.of(2, 5)).isEqualTo(CoverageStatus.PARTIAL);
        assertThat(CoverageStatus.of(0, 5)).isEqualTo(CoverageStatus.GAP);
        // Sans exigence sous ce chapitre, la case ne prétend pas un écart.
        assertThat(CoverageStatus.of(0, 0)).isEqualTo(CoverageStatus.NOT_APPLICABLE);
    }

    @Test
    void lesSeptChapitresCommunsVontDe4A10EtNommentLeursModules() {
        assertThat(Arrays.stream(HlsChapter.values()).map(HlsChapter::code))
                .containsExactly("4", "5", "6", "7", "8", "9", "10");
        assertThat(HlsChapter.IMPROVEMENT.modules()).containsExactly("NC", "CAPA");
        assertThat(HlsChapter.ofCode("6")).contains(HlsChapter.PLANNING);
        assertThat(HlsChapter.ofCode("11")).isEmpty();
    }

    @Test
    void uneNormeSeLitDansLesExigencesDuRegistreEtDansLeTexteDUnAudit() {
        StandardScope iso45001 = StandardScope.of(" ISO-45001 ");

        assertThat(iso45001.code()).isEqualTo("iso-45001");
        assertThat(iso45001.coversAny(List.of("ISO_9001_6_1", "ISO_45001_6_1"))).isTrue();
        assertThat(iso45001.coversAny(List.of("ISO_9001_6_1"))).isFalse();
        assertThat(iso45001.coversAny(null)).isFalse();
        assertThat(iso45001.namedIn("ISO 45001:2018 — site de Lyon")).isTrue();
        assertThat(iso45001.namedIn("ISO 14001")).isFalse();
        assertThat(iso45001.namedIn(null)).isFalse();
        assertThat(StandardScope.of("iatf-16949").coversAny(List.of("IATF_16949_6_1_2"))).isTrue();
        assertThat(StandardScope.of("  ")).isNull();
        assertThat(StandardScope.of(null)).isNull();
    }

    @Test
    void laGrilleCompteParGraviteEtProbabiliteEtIgnoreLesNotesAbsentes() {
        RiskGrid g = RiskGrid.of(List.of(
                new RiskGrid.Rating(4, 3), new RiskGrid.Rating(4, 3), new RiskGrid.Rating(1, 1),
                new RiskGrid.Rating(null, null), new RiskGrid.Rating(6, 1), new RiskGrid.Rating(5, 0)));

        assertThat(g.at(4, 3)).isEqualTo(2);
        assertThat(g.at(1, 1)).isEqualTo(1);
        assertThat(g.at(5, 5)).isZero();
        assertThat(g.total()).isEqualTo(3);
        assertThat(g.counts()).hasSize(5).allSatisfy(l -> assertThat(l).hasSize(5));
        assertThat(RiskGrid.of(Arrays.asList((RiskGrid.Rating) null)).total()).isZero();
    }
}
