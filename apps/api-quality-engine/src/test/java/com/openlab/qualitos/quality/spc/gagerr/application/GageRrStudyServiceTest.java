package com.openlab.qualitos.quality.spc.gagerr.application;

import com.openlab.qualitos.quality.spc.capability.application.SpcContext;
import com.openlab.qualitos.quality.spc.capability.domain.SpcNotFoundException;
import com.openlab.qualitos.quality.spc.capability.domain.SpcValidationException;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrAnova;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrDecision;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrFixtures;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrOperator;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrRepository;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrStudy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** Le service sur des doublures écrites à la main : persistance, jeton et audit sont des ports. */
class GageRrStudyServiceTest {

    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T08:00:00Z"), ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID AUTRE = UUID.randomUUID();
    static final UUID ACTEUR = UUID.randomUUID();

    FakeStudies studies;
    FakeContext context;
    FakeAudit audit;
    GageRrStudyService service;

    @BeforeEach
    void setup() {
        studies = new FakeStudies();
        context = new FakeContext(TENANT);
        audit = new FakeAudit();
        service = new GageRrStudyService(studies, context, audit, CLOCK);
    }

    static GageRrStudyDto.StudyCommand commande(String piece, LocalDate date, double multiplicateur) {
        return new GageRrStudyDto.StudyCommand(piece, "Diamètre extérieur (mm)", "Micromètre",
                GageRrFixtures.LSL, GageRrFixtures.USL, 3, 10, 3, multiplicateur, date,
                GageRrFixtures.RESPONSABLE, "Jeanne Leroy", GageRrFixtures.operators(),
                GageRrFixtures.workbookGrid());
    }

    static GageRrStudyDto.StudyCommand commande() {
        return commande("Arbre AX-210", LocalDate.of(2026, 10, 19), 5.15);
    }

    @Test
    @DisplayName("chemin doré : créer, lire, corriger, supprimer — chaque écriture tracée, tout recalculé")
    void cheminDore() {
        GageRrStudyDto.StudySheet creee = service.create(commande());
        UUID id = creee.study().id();

        assertThat(creee.study().grrPercent()).isEqualTo(21.0);
        assertThat(creee.study().grrTolerancePercent()).isEqualTo(24.74);
        assertThat(creee.study().ndc()).isEqualTo(6);
        assertThat(creee.study().decision()).isEqualTo(GageRrDecision.CONDITIONAL);
        assertThat(creee.study().anovaGrrPercent()).isEqualTo(21.16);
        assertThat(creee.study().anovaNdc()).isEqualTo(6);
        assertThat(creee.study().operators()).extracting(GageRrOperator::name)
                .containsExactly("Alice Martin", "Bruno Petit", "Chloé Durand");
        assertThat(creee.study().operatorCount()).isEqualTo(3);
        assertThat(creee.study().partCount()).isEqualTo(10);
        assertThat(creee.study().trialCount()).isEqualTo(3);
        assertThat(creee.study().sigmaMultiplier()).isEqualTo(5.15);
        assertThat(creee.study().responsibleName()).isEqualTo("Jeanne Leroy");
        assertThat(creee.study().updatedAt()).isEqualTo(CLOCK.instant());
        assertThat(creee.lsl()).isEqualTo(9.9);
        assertThat(creee.usl()).isEqualTo(10.1);
        assertThat(creee.measurements()).hasSize(3);

        GageRrStudyDto.CalculationView c = creee.calculation();
        assertThat(c.ev().value()).isCloseTo(0.04137833333333388, within(1e-9));
        assertThat(c.av().value()).isCloseTo(0.02713810391961042, within(1e-9));
        assertThat(c.grr().value()).isCloseTo(0.049483766568401644, within(1e-9));
        assertThat(c.pv().value()).isCloseTo(0.23040000000000152, within(1e-9));
        assertThat(c.tv().value()).isCloseTo(0.2356539903201233, within(1e-9));
        assertThat(c.k1()).isCloseTo(3.05, within(1e-12));
        assertThat(c.k2()).isCloseTo(2.70, within(1e-12));
        assertThat(c.k3()).isCloseTo(1.62, within(1e-12));
        assertThat(c.d4()).isEqualTo(2.58);
        assertThat(c.multiplier()).isEqualTo(5.15);
        assertThat(c.tolerance()).isCloseTo(0.2, within(1e-12));
        assertThat(c.ndc()).isEqualTo(6);
        assertThat(c.ndcSufficient()).isTrue();
        assertThat(c.decision()).isEqualTo(GageRrDecision.CONDITIONAL);
        assertThat(c.repeatabilityDominant()).isTrue();
        assertThat(c.rangesBeyondLimit()).isZero();
        assertThat(c.rangeUcl()).isCloseTo(0.035002, within(1e-9));
        assertThat(c.maxRange()).isCloseTo(0.028, within(1e-9));
        assertThat(c.xDiff()).isCloseTo(0.0104333333, within(1e-9));
        assertThat(c.meanRange()).isCloseTo(0.0135666667, within(1e-9));
        assertThat(c.partRange()).isCloseTo(0.1422222222, within(1e-9));
        assertThat(c.grandMean()).isCloseTo(10.0122, within(1e-9));
        assertThat(c.cellMeans()).hasSize(3);
        assertThat(c.cellRanges()).hasSize(3);
        assertThat(c.trialMeans()).hasSize(3);
        assertThat(c.operatorMeans()).hasSize(3);
        assertThat(c.operatorRanges()).hasSize(3);
        assertThat(c.partMeans()).hasSize(10);

        GageRrStudyDto.AnovaView a = c.anova();
        assertThat(a.interactionPooled()).isTrue();
        assertThat(a.fullTable()).extracting(GageRrAnova.AnovaRow::source).containsExactly(
                GageRrAnova.Source.PART, GageRrAnova.Source.OPERATOR, GageRrAnova.Source.INTERACTION,
                GageRrAnova.Source.REPEATABILITY, GageRrAnova.Source.TOTAL);
        assertThat(a.pooledTable()).hasSize(4);
        assertThat(a.variance().total()).isCloseTo(0.001946933449403829, within(1e-12));
        assertThat(a.ev().value()).isCloseTo(0.03948438590197881, within(1e-9));
        assertThat(a.av().value()).isCloseTo(0.027423486803149443, within(1e-9));
        assertThat(a.operator().value()).isCloseTo(0.027423486803149443, within(1e-9));
        assertThat(a.interaction().value()).isZero();
        assertThat(a.grr().percentOfTotal()).isCloseTo(0.21155497246110322, within(1e-9));
        assertThat(a.pv().value()).isCloseTo(0.22209565068527154, within(1e-9));
        assertThat(a.tv().value()).isCloseTo(0.22723895443302203, within(1e-9));
        assertThat(a.ndc()).isEqualTo(6);
        assertThat(a.ndcSufficient()).isTrue();
        assertThat(a.decision()).isEqualTo(GageRrDecision.CONDITIONAL);

        assertThat(service.get(id).study().partReference()).isEqualTo("Arbre AX-210");

        GageRrStudyDto.StudySheet corrigee = service.revise(id,
                commande("Arbre AX-210", LocalDate.of(2026, 10, 19), 6));
        assertThat(corrigee.study().sigmaMultiplier()).isEqualTo(6.0);
        assertThat(corrigee.study().grrTolerancePercent()).isEqualTo(28.83);

        service.delete(id);
        assertThat(service.list()).isEmpty();
        assertThat(audit.events).containsExactly("recorded", "revised", "deleted");
        assertThat(audit.actors).containsOnly(ACTEUR);
    }

    @Test
    @DisplayName("le registre range les plus récentes d'abord, sans les études des autres clients")
    void registre() {
        service.create(commande("Ancienne", LocalDate.of(2026, 5, 19), 5.15));
        service.create(commande("Récente", LocalDate.of(2026, 10, 1), 5.15));
        context.tenant = AUTRE;
        service.create(commande("Ailleurs", LocalDate.of(2026, 10, 2), 5.15));
        context.tenant = TENANT;

        assertThat(service.list()).extracting(GageRrStudyDto.StudySummary::partReference)
                .containsExactly("Récente", "Ancienne");
    }

    @Test
    @DisplayName("une étude d'un autre client est introuvable, en lecture comme en écriture")
    void isolation() {
        UUID id = service.create(commande()).study().id();
        context.tenant = AUTRE;
        assertThatThrownBy(() -> service.get(id)).isInstanceOf(SpcNotFoundException.class);
        assertThatThrownBy(() -> service.revise(id, commande())).isInstanceOf(SpcNotFoundException.class);
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(SpcNotFoundException.class);
        assertThat(audit.events).containsExactly("recorded");
    }

    @Test
    @DisplayName("une saisie refusée ne laisse ni étude ni trace")
    void saisieRefusee() {
        GageRrStudyDto.StudyCommand c = commande();
        GageRrStudyDto.StudyCommand fausse = new GageRrStudyDto.StudyCommand(c.partReference(),
                c.characteristic(), c.instrument(), 10.1, 9.9, 3, 10, 3, 5.15, c.studyDate(),
                c.responsibleUserId(), c.responsibleName(), c.operators(), c.measurements());
        assertThatThrownBy(() -> service.create(fausse)).isInstanceOf(SpcValidationException.class);
        assertThat(studies.rows).isEmpty();
        assertThat(audit.events).isEmpty();
    }

    // ---------- doublures ----------

    static final class FakeStudies implements GageRrRepository {
        final Map<UUID, GageRrStudy> rows = new LinkedHashMap<>();

        @Override
        public GageRrStudy save(GageRrStudy s) {
            UUID id = s.getId() == null ? UUID.randomUUID() : s.getId();
            GageRrStudy copie = new GageRrStudy(id, s.getTenantId(), s.getPartReference(), s.getCharacteristic(),
                    s.getInstrument(), s.getLsl(), s.getUsl(), s.getOperatorCount(), s.getPartCount(),
                    s.getTrialCount(), s.getSigmaMultiplier(), s.getStudyDate(), s.getResponsibleUserId(),
                    s.getResponsibleName(), s.getOperators(), s.getMeasurements(), s.getGrrPercent(),
                    s.getGrrTolerancePercent(), s.getNdc(), s.getDecision(), s.getAnovaGrrPercent(),
                    s.getAnovaNdc(), s.getCreatedBy(), s.getCreatedAt(), s.getUpdatedAt());
            rows.put(id, copie);
            return copie;
        }

        @Override
        public Optional<GageRrStudy> findByIdAndTenant(UUID id, UUID tenantId) {
            return Optional.ofNullable(rows.get(id)).filter(s -> s.getTenantId().equals(tenantId));
        }

        @Override
        public List<GageRrStudy> findByTenant(UUID tenantId) {
            return rows.values().stream().filter(s -> s.getTenantId().equals(tenantId)).toList();
        }

        @Override
        public void delete(GageRrStudy study) {
            rows.remove(study.getId());
        }
    }

    static final class FakeContext implements SpcContext {
        UUID tenant;

        FakeContext(UUID tenant) {
            this.tenant = tenant;
        }

        @Override
        public UUID requireTenantId() {
            return tenant;
        }

        @Override
        public UUID requireActorId() {
            return ACTEUR;
        }
    }

    static final class FakeAudit implements GageRrAuditPublisher {
        final List<String> events = new ArrayList<>();
        final List<UUID> actors = new ArrayList<>();

        @Override
        public void recorded(GageRrStudy study, UUID actor) {
            events.add("recorded");
            actors.add(actor);
        }

        @Override
        public void revised(GageRrStudy study, UUID actor) {
            events.add("revised");
            actors.add(actor);
        }

        @Override
        public void deleted(GageRrStudy study, UUID actor) {
            events.add("deleted");
            actors.add(actor);
        }
    }
}
