package com.openlab.qualitos.quality.smi.application;

import com.openlab.qualitos.quality.smi.domain.CoverageStatus;
import com.openlab.qualitos.quality.smi.domain.Deadline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SmiDashboardServiceTest {

    static final LocalDate AUJOURDHUI = LocalDate.of(2026, 10, 8);
    static final UUID A9001 = UUID.randomUUID();
    static final UUID A45001 = UUID.randomUUID();

    FakeStandards standards;
    FakeActions actions;
    List<SmiPorts.OpenRisk> risques;
    List<SmiPorts.PlannedAudit> audits;
    List<Deadline> echeances;
    SmiDashboardService service;

    @BeforeEach
    void setUp() {
        standards = new FakeStandards();
        actions = new FakeActions();
        risques = new ArrayList<>();
        audits = new ArrayList<>();
        echeances = new ArrayList<>();
        service = new SmiDashboardService(standards, actions, () -> risques, h -> audits,
                (until, limit) -> echeances,
                Clock.fixed(Instant.parse("2026-10-08T09:00:00Z"), ZoneOffset.UTC));

        standards.adoptees.add(new SmiPorts.AdoptedStandard(A45001, "iso-45001", "ISO 45001"));
        standards.adoptees.add(new SmiPorts.AdoptedStandard(A9001, "iso-9001", "ISO 9001"));
        standards.alignements.put(A9001, new SmiPorts.Alignment(91.4, List.of(
                new SmiPorts.SectionCoverage("4", "Contexte", 3, 3),
                new SmiPorts.SectionCoverage("6", "Planification", 1, 4))));
        standards.alignements.put(A45001, new SmiPorts.Alignment(78.6, List.of(
                new SmiPorts.SectionCoverage("6", "Planification", 0, 5))));
    }

    static SmiPorts.OpenRisk risque(String ref, int g, int p, Integer rg, Integer rp, String niveau,
                                    List<String> exigences, LocalDate revue) {
        return new SmiPorts.OpenRisk(UUID.randomUUID(), ref, "Risque " + ref, g, p, rg, rp, niveau, exigences, revue);
    }

    @Test
    void sansFiltreLaConformiteGlobaleEstLaMoyenneDesNormesRangeesDansLOrdreNaturel() {
        SmiDto.Dashboard d = service.dashboard(null);

        assertThat(d.selected()).isNull();
        // ISO 9001 avant ISO 45001 : les numéros se comparent par leur valeur, pas comme du texte.
        assertThat(d.standards()).extracting(SmiDto.StandardRef::code).containsExactly("iso-9001", "iso-45001");
        assertThat(d.compliance().perStandard()).extracting(SmiDto.StandardScore::score).containsExactly(91, 79);
        assertThat(d.compliance().global()).isEqualTo(85);
    }

    @Test
    void filtrerUneNormeRestreintConformiteRisquesEtAuditsMaisPasLesActions() {
        risques.add(risque("R-1", 4, 3, 4, 2, "HIGH", List.of("ISO_45001_6_1"), null));
        risques.add(risque("R-2", 5, 4, null, null, "CRITICAL", List.of("ISO_9001_6_1"), null));
        audits.add(new SmiPorts.PlannedAudit(UUID.randomUUID(), "AUD-1", "Interne 9001", "INTERNAL",
                "ISO 9001:2015", AUJOURDHUI.plusDays(20)));
        audits.add(new SmiPorts.PlannedAudit(UUID.randomUUID(), "AUD-2", "Interne 45001", "INTERNAL",
                "ISO 45001", AUJOURDHUI.plusDays(40)));
        actions.retard = new SmiPorts.OverdueActions(12, 3);

        SmiDto.Dashboard d = service.dashboard("iso-45001");

        assertThat(d.selected()).isEqualTo("iso-45001");
        assertThat(d.compliance().global()).isEqualTo(79);
        assertThat(d.riskMatrix().open()).isEqualTo(1);
        assertThat(d.riskMatrix().gross().get(3).get(2)).isEqualTo(1);
        assertThat(d.riskMatrix().residual().get(3).get(1)).isEqualTo(1);
        assertThat(d.majorRisks()).isEqualTo(new SmiDto.MajorRisks(1, 0, 1));
        assertThat(d.nextAudit().reference()).isEqualTo("AUD-2");
        assertThat(d.nextAudit().daysUntil()).isEqualTo(40);
        // Une action n'appartient à aucune norme : elle reste comptée.
        assertThat(d.overdueActions()).isEqualTo(new SmiDto.OverdueActions(12, 3));
    }

    @Test
    void uneNormeNonAdopteeNeFiltreRien() {
        risques.add(risque("R-1", 4, 3, null, null, "HIGH", List.of("ISO_9001_6_1"), null));

        SmiDto.Dashboard d = service.dashboard("iso-27001");

        assertThat(d.selected()).isNull();
        assertThat(d.riskMatrix().open()).isEqualTo(1);
        assertThat(d.compliance().global()).isEqualTo(85);
    }

    @Test
    void lesRisquesMajeursSontCeuxDeNiveauDixOuPlus() {
        risques.add(risque("R-1", 3, 3, null, null, "MEDIUM", List.of(), null));
        risques.add(risque("R-2", 5, 2, null, null, "HIGH", List.of(), null));
        risques.add(risque("R-3", 5, 3, null, null, "CRITICAL", List.of(), null));
        risques.add(risque("R-4", 5, 5, null, null, "CRITICAL", List.of(), null));

        SmiDto.Dashboard d = service.dashboard("");

        assertThat(d.majorRisks()).isEqualTo(new SmiDto.MajorRisks(3, 2, 1));
        assertThat(d.riskMatrix().open()).isEqualTo(4);
        // La résiduelle non fixée ne compte dans aucune case.
        assertThat(d.riskMatrix().residual().stream().flatMap(List::stream).mapToInt(Integer::intValue).sum()).isZero();
    }

    @Test
    void laSemaineReunitLesEcheancesDeChaqueModuleRetardsComprisLaPlusPresseeDAbord() {
        UUID capa = UUID.randomUUID();
        actions.dues.add(new Deadline(Deadline.Kind.CAPA_ACTION, capa, "R-014", "Clôture CAPA", AUJOURDHUI.plusDays(3)));
        echeances.add(new Deadline(Deadline.Kind.CALIBRATION, UUID.randomUUID(), "PC-118", "Pied à coulisse",
                AUJOURDHUI.plusDays(2)));
        echeances.add(new Deadline(Deadline.Kind.CHANGE, UUID.randomUUID(), "MOC-7", "Changement de gamme",
                AUJOURDHUI.minusDays(1)));
        risques.add(risque("R-9", 2, 2, null, null, "LOW", List.of(), AUJOURDHUI.plusDays(5)));
        risques.add(risque("R-10", 2, 2, null, null, "LOW", List.of(), AUJOURDHUI.plusDays(30)));
        audits.add(new SmiPorts.PlannedAudit(UUID.randomUUID(), "AUD-1", "Audit", "INTERNAL", "ISO 9001",
                AUJOURDHUI.plusDays(7)));
        audits.add(new SmiPorts.PlannedAudit(UUID.randomUUID(), "AUD-0", "Audit en retard", "INTERNAL", "ISO 9001",
                AUJOURDHUI.minusDays(3)));

        List<SmiDto.Upcoming> semaine = service.dashboard(null).thisWeek();

        assertThat(semaine).extracting(SmiDto.Upcoming::reference)
                .containsExactly("AUD-0", "MOC-7", "PC-118", "R-014", "R-9", "AUD-1");
        assertThat(semaine.get(1).daysLeft()).isEqualTo(-1);
        assertThat(semaine.get(3).targetId()).isEqualTo(capa);
        assertThat(semaine.get(3).kind()).isEqualTo(Deadline.Kind.CAPA_ACTION);
    }

    @Test
    void laSemaineSArreteAHuitLignes() {
        for (int i = 0; i < 12; i++) {
            echeances.add(new Deadline(Deadline.Kind.CHANGE, UUID.randomUUID(), "MOC-" + i, "x", AUJOURDHUI));
        }
        assertThat(service.dashboard(null).thisWeek()).hasSize(SmiDashboardService.MAX_UPCOMING);
    }

    @Test
    void leProchainAuditEstLePremierAVenirEtAucunSansPlanning() {
        assertThat(service.dashboard(null).nextAudit()).isNull();

        audits.add(new SmiPorts.PlannedAudit(UUID.randomUUID(), "AUD-0", "Passé", "INTERNAL", null,
                AUJOURDHUI.minusDays(1)));
        audits.add(new SmiPorts.PlannedAudit(UUID.randomUUID(), "AUD-2", "Plus tard", "EXTERNAL", null,
                AUJOURDHUI.plusDays(60)));
        audits.add(new SmiPorts.PlannedAudit(UUID.randomUUID(), "AUD-1", "Bientôt", "INTERNAL", null,
                AUJOURDHUI));
        audits.add(new SmiPorts.PlannedAudit(UUID.randomUUID(), "AUD-X", "Sans date", "INTERNAL", null, null));

        SmiDto.NextAudit prochain = service.dashboard(null).nextAudit();
        assertThat(prochain.reference()).isEqualTo("AUD-1");
        assertThat(prochain.daysUntil()).isZero();
    }

    @Test
    void sansNormeAdopteeLaConformiteEstAbsente() {
        standards.adoptees.clear();
        SmiDto.Dashboard d = service.dashboard("iso-9001");
        assertThat(d.compliance()).isNull();
        assertThat(d.standards()).isEmpty();
        assertThat(service.requirementsMatrix().rows()).allSatisfy(r -> assertThat(r.cells()).isEmpty());
    }

    @Test
    void uneNormeSansAlignementNAPasDeBarreEtLaMoyenneSeFaitSurLesAutres() {
        standards.alignements.remove(A45001);
        SmiDto.Dashboard d = service.dashboard(null);
        assertThat(d.compliance().perStandard()).extracting(SmiDto.StandardScore::code).containsExactly("iso-9001");
        assertThat(d.compliance().global()).isEqualTo(91);

        standards.alignements.clear();
        assertThat(service.dashboard(null).compliance().global()).isNull();
    }

    @Test
    void laMatriceDesExigencesCroiseLesChapitresCommunsEtLesNormes() {
        SmiDto.RequirementsMatrix m = service.requirementsMatrix();

        assertThat(m.standards()).extracting(SmiDto.StandardRef::code).containsExactly("iso-9001", "iso-45001");
        assertThat(m.rows()).extracting(SmiDto.ChapterRow::chapter)
                .containsExactly("4", "5", "6", "7", "8", "9", "10");

        SmiDto.ChapterRow contexte = m.rows().get(0);
        assertThat(contexte.modules()).isNotEmpty();
        assertThat(contexte.cells()).extracting(SmiDto.Cell::status)
                .containsExactly(CoverageStatus.COVERED, CoverageStatus.NOT_APPLICABLE);

        SmiDto.ChapterRow planification = m.rows().get(2);
        assertThat(planification.cells()).extracting(SmiDto.Cell::status)
                .containsExactly(CoverageStatus.PARTIAL, CoverageStatus.GAP);
        assertThat(planification.cells().get(0)).satisfies(c -> {
            assertThat(c.covered()).isEqualTo(1);
            assertThat(c.total()).isEqualTo(4);
            assertThat(c.sectionTitle()).isEqualTo("Planification");
        });
    }

    /**
     * Un SMI 9001 + 14001, avec les chapitres tels que le catalogue les livre
     * (V10/V57 pour la 9001, V14/V59 pour la 14001 : sections « 4 » à « 10 »).
     * Les deux normes se lisent côte à côte, chacune avec SES couvertures.
     */
    @Test
    void neufMilleEtQuatorzeMilleSePilotentCoteACote() {
        UUID a14001 = UUID.randomUUID();
        standards.adoptees.clear();
        standards.alignements.clear();
        standards.adoptees.add(new SmiPorts.AdoptedStandard(a14001, "iso-14001", "ISO 14001:2015 — SME"));
        standards.adoptees.add(new SmiPorts.AdoptedStandard(A9001, "iso-9001", "ISO 9001:2015 — SMQ"));
        standards.alignements.put(A9001, new SmiPorts.Alignment(57.1, List.of(
                new SmiPorts.SectionCoverage("4", "Contexte de l'organisme", 5, 5),
                new SmiPorts.SectionCoverage("5", "Leadership", 4, 4),
                new SmiPorts.SectionCoverage("6", "Planification", 2, 5),
                new SmiPorts.SectionCoverage("7", "Support", 5, 9),
                new SmiPorts.SectionCoverage("8", "Réalisation des activités opérationnelles", 4, 11),
                new SmiPorts.SectionCoverage("9", "Évaluation des performances", 3, 5),
                new SmiPorts.SectionCoverage("10", "Amélioration", 1, 3))));
        standards.alignements.put(a14001, new SmiPorts.Alignment(9.5, List.of(
                new SmiPorts.SectionCoverage("4", "Contexte de l'organisme", 0, 2),
                new SmiPorts.SectionCoverage("5", "Leadership", 0, 2),
                new SmiPorts.SectionCoverage("6", "Planification", 2, 4),
                new SmiPorts.SectionCoverage("7", "Support", 0, 3),
                new SmiPorts.SectionCoverage("8", "Réalisation des activités opérationnelles", 0, 2),
                new SmiPorts.SectionCoverage("9", "Évaluation des performances", 0, 4),
                new SmiPorts.SectionCoverage("10", "Amélioration", 0, 2))));
        risques.add(risque("R-ENV", 4, 4, null, null, "CRITICAL", List.of("ISO_14001_6_1"), null));
        risques.add(risque("R-QUA", 3, 4, null, null, "HIGH", List.of("ISO_9001_6_1"), null));
        audits.add(new SmiPorts.PlannedAudit(UUID.randomUUID(), "AUD-E", "Audit SME", "INTERNAL",
                "ISO 14001:2015", AUJOURDHUI.plusDays(15)));
        audits.add(new SmiPorts.PlannedAudit(UUID.randomUUID(), "AUD-Q", "Audit SMQ", "INTERNAL",
                "ISO 9001:2015", AUJOURDHUI.plusDays(5)));

        // Tous : les deux normes, la 9001 d'abord, la conformité globale est leur moyenne.
        SmiDto.Dashboard tous = service.dashboard(null);
        assertThat(tous.standards()).extracting(SmiDto.StandardRef::code).containsExactly("iso-9001", "iso-14001");
        assertThat(tous.compliance().perStandard()).extracting(SmiDto.StandardScore::code, SmiDto.StandardScore::score)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("iso-9001", 57),
                        org.assertj.core.groups.Tuple.tuple("iso-14001", 10));
        assertThat(tous.compliance().global()).isEqualTo(34);
        assertThat(tous.riskMatrix().open()).isEqualTo(2);
        assertThat(tous.nextAudit().reference()).isEqualTo("AUD-Q");

        // Filtre 14001 : sa conformité, ses risques, son audit.
        SmiDto.Dashboard env = service.dashboard("iso-14001");
        assertThat(env.selected()).isEqualTo("iso-14001");
        assertThat(env.compliance().global()).isEqualTo(10);
        assertThat(env.compliance().perStandard()).hasSize(2);
        assertThat(env.riskMatrix().open()).isEqualTo(1);
        assertThat(env.majorRisks()).isEqualTo(new SmiDto.MajorRisks(1, 1, 0));
        assertThat(env.nextAudit().reference()).isEqualTo("AUD-E");

        // Matrice : chaque chapitre commun porte une case par norme, avec la couverture de CETTE norme.
        SmiDto.RequirementsMatrix m = service.requirementsMatrix();
        assertThat(m.standards()).extracting(SmiDto.StandardRef::code).containsExactly("iso-9001", "iso-14001");
        assertThat(m.rows()).allSatisfy(r -> {
            assertThat(r.cells()).extracting(SmiDto.Cell::standardCode).containsExactly("iso-9001", "iso-14001");
            assertThat(r.cells()).noneMatch(c -> c.status() == CoverageStatus.NOT_APPLICABLE);
        });
        assertThat(m.rows()).extracting(r -> r.cells().get(1).status()).containsExactly(
                CoverageStatus.GAP, CoverageStatus.GAP, CoverageStatus.PARTIAL, CoverageStatus.GAP,
                CoverageStatus.GAP, CoverageStatus.GAP, CoverageStatus.GAP);
        assertThat(m.rows()).extracting(r -> r.cells().get(0).status()).containsExactly(
                CoverageStatus.COVERED, CoverageStatus.COVERED, CoverageStatus.PARTIAL, CoverageStatus.PARTIAL,
                CoverageStatus.PARTIAL, CoverageStatus.PARTIAL, CoverageStatus.PARTIAL);
        SmiDto.Cell planif14001 = m.rows().get(2).cells().get(1);
        assertThat(planif14001.covered()).isEqualTo(2);
        assertThat(planif14001.total()).isEqualTo(4);
    }

    // ---------- doublures ----------

    static final class FakeStandards implements SmiPorts.Standards {
        final List<SmiPorts.AdoptedStandard> adoptees = new ArrayList<>();
        final Map<UUID, SmiPorts.Alignment> alignements = new HashMap<>();

        @Override
        public List<SmiPorts.AdoptedStandard> adopted() {
            return adoptees;
        }

        @Override
        public Optional<SmiPorts.Alignment> alignment(UUID adoptionId) {
            return Optional.ofNullable(alignements.get(adoptionId));
        }
    }

    static final class FakeActions implements SmiPorts.Actions {
        SmiPorts.OverdueActions retard = new SmiPorts.OverdueActions(0, 0);
        final List<Deadline> dues = new ArrayList<>();

        @Override
        public SmiPorts.OverdueActions overdue(LocalDate today) {
            return retard;
        }

        @Override
        public List<Deadline> dueBy(LocalDate until, int limit) {
            return dues;
        }
    }
}
