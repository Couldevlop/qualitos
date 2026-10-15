package com.openlab.qualitos.quality.smi.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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

    @Test
    void lesNormesSeRangentDansLOrdreNaturelDeLeursNumeros() {
        List<String> codes = new ArrayList<>(List.of(
                "iso-45001", "iso-14001", "iso-9001", "iatf-16949", "iso-20000-1", "iso-27001"));
        codes.sort(StandardOrder.BY_CODE);

        assertThat(codes).containsExactly(
                "iatf-16949", "iso-9001", "iso-14001", "iso-20000-1", "iso-27001", "iso-45001");
        assertThat(StandardOrder.BY_CODE.compare("ISO-9001", "iso-9001")).isZero();
        // Zéros de tête et nombres plus grands qu'un long : comparés sans conversion.
        assertThat(StandardOrder.BY_CODE.compare("iso-09001", "iso-9001")).isZero();
        assertThat(StandardOrder.BY_CODE.compare("x-123456789012345678901", "x-99")).isPositive();
        assertThat(StandardOrder.BY_CODE.compare("iso", "iso-9001")).isNegative();
        assertThat(StandardOrder.BY_CODE.compare(null, "iso-9001")).isPositive();
    }

    @Test
    void uneNormeIndisponibleDitPourquoi() {
        UUID id = UUID.randomUUID();
        assertThat(new StandardUnavailableException(id, StandardUnavailableException.Reason.ALREADY_ADOPTED))
                .hasMessageContaining("already adopted")
                .extracting(StandardUnavailableException::reason)
                .isEqualTo(StandardUnavailableException.Reason.ALREADY_ADOPTED);
        assertThat(new StandardUnavailableException(id, StandardUnavailableException.Reason.NOT_A_MANAGEMENT_SYSTEM))
                .hasMessageContaining("not a certifiable management system");
    }
}
