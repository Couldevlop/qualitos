package com.openlab.qualitos.quality.ims.domain;

import com.openlab.qualitos.quality.ims.domain.model.ClauseMapping;
import com.openlab.qualitos.quality.ims.domain.model.ClauseRef;
import com.openlab.qualitos.quality.ims.domain.model.CoverageMatrix;
import com.openlab.qualitos.quality.ims.domain.model.CoverageMatrixCell;
import com.openlab.qualitos.quality.ims.domain.model.RelationType;
import com.openlab.qualitos.quality.ims.domain.service.CoverageMatrixDomainService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoverageMatrixDomainServiceTest {

    private final CoverageMatrixDomainService svc = new CoverageMatrixDomainService();

    @Test
    void build_emptyInputs() {
        CoverageMatrix m = svc.build("t1", List.of(), List.of());
        assertThat(m.cells()).isEmpty();
        assertThat(m.totalMappings()).isZero();
        assertThat(m.totalSourceClauses()).isZero();
        assertThat(m.tenantId()).isEqualTo("t1");
    }

    @Test
    void build_filtersOutMappingsNotInScope() {
        List<String> codes = List.of("iso-9001", "iso-14001");
        List<ClauseMapping> mappings = List.of(
                mapping("iso-9001", "4.1", "iso-14001", "4.1"),
                mapping("iso-9001", "5.1", "iso-27001", "5.1") // hors scope (27001 absent)
        );
        CoverageMatrix m = svc.build("t1", codes, mappings);
        assertThat(m.totalMappings()).isEqualTo(1);
        assertThat(m.cells()).hasSize(1);
        assertThat(m.cells().get(0).source().clauseCode()).isEqualTo("4.1");
        assertThat(m.cells().get(0).targetStandardCode()).isEqualTo("iso-14001");
    }

    @Test
    void build_groupsBySourceAndTargetStandard() {
        List<ClauseMapping> mappings = List.of(
                mapping("iso-9001", "4.1", "iso-14001", "4.1"),
                mapping("iso-9001", "4.1", "iso-14001", "4.2"), // même source, même target std
                mapping("iso-9001", "5.1", "iso-14001", "5.1")
        );
        CoverageMatrix m = svc.build("t1", List.of("iso-9001", "iso-14001"), mappings);
        assertThat(m.cells()).hasSize(2);
        CoverageMatrixCell forSource41 = m.cells().stream()
                .filter(c -> c.source().clauseCode().equals("4.1"))
                .findFirst().orElseThrow();
        assertThat(forSource41.coverages()).hasSize(2);
    }

    @Test
    void build_distinctSourceClausesCount() {
        List<ClauseMapping> mappings = List.of(
                mapping("iso-9001", "4.1", "iso-14001", "4.1"),
                mapping("iso-9001", "4.1", "iso-45001", "4.1"), // même source, ≠ target std
                mapping("iso-9001", "5.1", "iso-14001", "5.1")
        );
        CoverageMatrix m = svc.build("t1", List.of("iso-9001", "iso-14001", "iso-45001"), mappings);
        assertThat(m.totalSourceClauses()).isEqualTo(2);
        assertThat(m.totalMappings()).isEqualTo(3);
    }

    @Test
    void build_nullSafe() {
        CoverageMatrix m = svc.build("t1", null, null);
        assertThat(m.standardCodes()).isEmpty();
        assertThat(m.cells()).isEmpty();
    }

    @Test
    void build_requiresTenantId() {
        assertThatThrownBy(() -> svc.build(null, List.of(), List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    void clauseMapping_rejectsSelfLoop() {
        ClauseRef same = new ClauseRef("iso-9001", "4.1");
        assertThatThrownBy(() -> new ClauseMapping(same, same, RelationType.EQUIVALENT, 100, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("source and target must differ");
    }

    @Test
    void clauseMapping_rejectsBadConfidence() {
        assertThatThrownBy(() -> new ClauseMapping(
                new ClauseRef("a", "1"), new ClauseRef("b", "1"),
                RelationType.RELATED, 150, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClauseMapping(
                new ClauseRef("a", "1"), new ClauseRef("b", "1"),
                RelationType.RELATED, -5, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clauseRef_rejectsBlanks() {
        assertThatThrownBy(() -> new ClauseRef("", "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClauseRef("x", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void computeReuseRatio_zeroWhenSingleStandard() {
        CoverageMatrix m = svc.build("t1", List.of("iso-9001"), List.of());
        assertThat(svc.computeReuseRatio(m)).isZero();
    }

    @Test
    void computeReuseRatio_zeroWhenNoEquivalentMappings() {
        List<ClauseMapping> mappings = List.of(
                new ClauseMapping(
                        new ClauseRef("iso-9001", "10.2"),
                        new ClauseRef("iso-14001", "10.2"),
                        RelationType.RELATED, 50, null)
        );
        CoverageMatrix m = svc.build("t1", List.of("iso-9001", "iso-14001"), mappings);
        assertThat(svc.computeReuseRatio(m)).isZero();
    }

    @Test
    void computeReuseRatio_perfectWhenAllSourcesCoverAllTargetStandards() {
        // 2 sources × 1 norm cible = 2 paires possibles, 2 EQUIVALENT → 100%
        List<ClauseMapping> mappings = List.of(
                new ClauseMapping(
                        new ClauseRef("iso-9001", "4.1"),
                        new ClauseRef("iso-14001", "4.1"),
                        RelationType.EQUIVALENT, 100, null),
                new ClauseMapping(
                        new ClauseRef("iso-9001", "5.1"),
                        new ClauseRef("iso-14001", "5.1"),
                        RelationType.EQUIVALENT, 100, null)
        );
        CoverageMatrix m = svc.build("t1", List.of("iso-9001", "iso-14001"), mappings);
        assertThat(svc.computeReuseRatio(m)).isEqualTo(100.0);
    }

    @Test
    void computeReuseRatio_partial() {
        // 2 sources, scope 3 normes (iso-9001 + iso-14001 + iso-45001)
        // → 2 sources × 2 normes cible = 4 paires possibles.
        // 3 EQUIVALENT → 75%.
        List<ClauseMapping> mappings = List.of(
                new ClauseMapping(
                        new ClauseRef("iso-9001", "4.1"),
                        new ClauseRef("iso-14001", "4.1"),
                        RelationType.EQUIVALENT, 100, null),
                new ClauseMapping(
                        new ClauseRef("iso-9001", "4.1"),
                        new ClauseRef("iso-45001", "4.1"),
                        RelationType.EQUIVALENT, 100, null),
                new ClauseMapping(
                        new ClauseRef("iso-9001", "5.1"),
                        new ClauseRef("iso-14001", "5.1"),
                        RelationType.EQUIVALENT, 100, null)
        );
        CoverageMatrix m = svc.build("t1", List.of("iso-9001", "iso-14001", "iso-45001"), mappings);
        assertThat(svc.computeReuseRatio(m)).isEqualTo(75.0);
    }

    private static ClauseMapping mapping(String srcStd, String srcCl, String tgtStd, String tgtCl) {
        return new ClauseMapping(
                new ClauseRef(srcStd, srcCl),
                new ClauseRef(tgtStd, tgtCl),
                RelationType.EQUIVALENT, 100, null);
    }
}
