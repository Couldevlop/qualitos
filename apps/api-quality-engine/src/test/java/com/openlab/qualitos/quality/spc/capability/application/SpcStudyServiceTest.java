package com.openlab.qualitos.quality.spc.capability.application;

import com.openlab.qualitos.quality.spc.capability.domain.CapabilityStatus;
import com.openlab.qualitos.quality.spc.capability.domain.CapabilityStudy;
import com.openlab.qualitos.quality.spc.capability.domain.CapabilityThresholds;
import com.openlab.qualitos.quality.spc.capability.domain.SpcFixtures;
import com.openlab.qualitos.quality.spc.capability.domain.SpcNotFoundException;
import com.openlab.qualitos.quality.spc.capability.domain.SpcRepositories;
import com.openlab.qualitos.quality.spc.capability.domain.SpcValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le service sur des doublures écrites à la main : la persistance, le contexte
 * du jeton et le journal d'audit sont des ports, rien de Spring.
 */
class SpcStudyServiceTest {

    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T08:00:00Z"), ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID AUTRE = UUID.randomUUID();
    static final UUID ACTEUR = UUID.randomUUID();

    FakeStudies studies;
    FakeSettings settings;
    FakeContext context;
    FakeAudit audit;
    SpcStudyService service;

    @BeforeEach
    void setup() {
        studies = new FakeStudies();
        settings = new FakeSettings();
        context = new FakeContext(TENANT);
        audit = new FakeAudit();
        service = new SpcStudyService(studies, settings, context, audit, CLOCK);
    }

    static SpcStudyDto.StudyCommand commande() {
        return commande(SpcFixtures.goldenGrid(), "Shaft AX", LocalDate.of(2026, 9, 14));
    }

    static SpcStudyDto.StudyCommand commande(List<List<Double>> grille, String piece, LocalDate date) {
        return new SpcStudyDto.StudyCommand(piece, "Weight", "500 ± 25 gram", "M-04",
                UUID.randomUUID(), "J. Martin", date, false, SpcFixtures.LSL, SpcFixtures.USL, 5, grille);
    }

    @Test
    @DisplayName("chemin doré : créer, lire, corriger, cocher le plan, supprimer — chaque écriture tracée")
    void cheminDore() {
        SpcStudyDto.StudySheet creee = service.create(commande());
        UUID id = creee.study().id();

        assertThat(creee.study().cp()).isEqualTo(1.840);
        assertThat(creee.study().cpk()).isEqualTo(1.769);
        assertThat(creee.study().status()).isEqualTo(CapabilityStatus.CAPABLE);
        assertThat(creee.study().capableThreshold()).isEqualByComparingTo("1.67");
        assertThat(creee.study().watchThreshold()).isEqualByComparingTo("1.33");
        assertThat(creee.lsl()).isEqualTo(475);
        assertThat(creee.subgroupSize()).isEqualTo(5);
        assertThat(creee.measurements()).hasSize(5);
        assertThat(creee.calculation().completeSubgroups()).isEqualTo(20);
        assertThat(creee.calculation().grandMean()).isEqualTo(499.03, org.assertj.core.api.Assertions.within(1e-9));
        assertThat(creee.calculation().d2()).isEqualTo(2.33);
        assertThat(creee.calculation().a2()).isEqualTo(0.58);
        assertThat(creee.calculation().d3()).isZero();
        assertThat(creee.calculation().d4()).isEqualTo(2.11);
        assertThat(creee.calculation().meanUcl()).isNotNull();
        assertThat(creee.calculation().rangeUcl()).isNotNull();
        assertThat(creee.calculation().rangeLcl()).isZero();
        assertThat(creee.calculation().sumOfMeans()).isNotNull();
        assertThat(creee.calculation().meanRange()).isNotNull();
        assertThat(creee.calculation().sigma()).isNotNull();
        assertThat(creee.calculation().cpu()).isNotNull();
        assertThat(creee.calculation().cpl()).isNotNull();
        assertThat(creee.calculation().meanLcl()).isNotNull();
        assertThat(creee.calculation().dispersionPercent()).isNotNull();
        assertThat(creee.calculation().centerOffset()).isNotNull();
        assertThat(creee.calculation().centeringPercent()).isNotNull();
        assertThat(creee.calculation().subgroupMeans()).hasSize(20);
        assertThat(creee.calculation().subgroupRanges()).hasSize(20);
        assertThat(creee.calculation().cp()).isNotNull();
        assertThat(creee.calculation().cpk()).isNotNull();

        assertThat(service.get(id).study().partReference()).isEqualTo("Shaft AX");

        List<List<Double>> serree = SpcFixtures.goldenGrid();
        serree.get(0).set(0, 530.0);
        SpcStudyDto.StudySheet corrigee = service.revise(id, commande(serree, "Shaft AX", LocalDate.of(2026, 9, 14)));
        assertThat(corrigee.study().cpk()).isLessThan(1.769);

        SpcStudyDto.StudySummary cochee = service.setActionPlan(id, true);
        assertThat(cochee.actionPlan()).isTrue();

        service.delete(id);
        assertThat(service.list()).isEmpty();
        assertThat(audit.events).containsExactly("recorded", "revised", "action-plan", "deleted");
        assertThat(audit.actors).containsOnly(ACTEUR);
    }

    @Test
    @DisplayName("le registre range les études les plus récentes d'abord, sans celles des autres clients")
    void registre() {
        service.create(commande(SpcFixtures.goldenGrid(), "Ancienne", LocalDate.of(2026, 5, 19)));
        service.create(commande(SpcFixtures.goldenGrid(), "Recente", LocalDate.of(2026, 10, 2)));
        service.create(commande(SpcFixtures.goldenGrid(), "Moyenne", LocalDate.of(2026, 7, 3)));
        context.tenant = AUTRE;
        service.create(commande(SpcFixtures.goldenGrid(), "Ailleurs", LocalDate.of(2026, 10, 3)));
        context.tenant = TENANT;

        assertThat(service.list()).extracting(SpcStudyDto.StudySummary::partReference)
                .containsExactly("Recente", "Moyenne", "Ancienne");
    }

    @Test
    @DisplayName("à date égale, l'étude retouchée en dernier passe devant")
    void registreADateEgale() {
        LocalDate jour = LocalDate.of(2026, 10, 2);
        UUID premiere = service.create(commande(SpcFixtures.goldenGrid(), "Premiere", jour)).study().id();
        service.create(commande(SpcFixtures.goldenGrid(), "Seconde", jour));
        SpcStudyService plusTard = new SpcStudyService(studies, settings, context, audit,
                Clock.offset(CLOCK, java.time.Duration.ofHours(1)));
        plusTard.setActionPlan(premiere, true);

        assertThat(service.list()).extracting(SpcStudyDto.StudySummary::partReference)
                .containsExactly("Premiere", "Seconde");
    }

    @Test
    @DisplayName("une étude neuve fige les seuils du client ; les changer ensuite ne réécrit pas son verdict")
    void seuilsFiges() {
        service.updateSettings(new SpcStudyDto.SettingsCommand(new BigDecimal("2.00"), new BigDecimal("1.50")));
        SpcStudyDto.StudySheet stricte = service.create(commande());
        assertThat(stricte.study().status()).isEqualTo(CapabilityStatus.WATCH);

        service.updateSettings(new SpcStudyDto.SettingsCommand(new BigDecimal("1.67"), new BigDecimal("1.33")));
        assertThat(service.get(stricte.study().id()).study().status()).isEqualTo(CapabilityStatus.WATCH);
        assertThat(service.revise(stricte.study().id(), commande()).study().capableThreshold())
                .isEqualByComparingTo("2.00");
        assertThat(service.create(commande()).study().status()).isEqualTo(CapabilityStatus.CAPABLE);
    }

    @Test
    void lesSeuilsParDefautSontSignalesCommeTels() {
        SpcStudyDto.SettingsView defaut = service.settings();
        assertThat(defaut.defaults()).isTrue();
        assertThat(defaut.capableThreshold()).isEqualByComparingTo("1.67");

        SpcStudyDto.SettingsView regle = service.updateSettings(
                new SpcStudyDto.SettingsCommand(new BigDecimal("1.50"), new BigDecimal("1.00")));
        assertThat(regle.defaults()).isFalse();
        assertThat(service.settings()).isEqualTo(regle);
        assertThat(audit.events).containsExactly("thresholds");
        assertThat(settings.actor).isEqualTo(ACTEUR);
    }

    @Test
    void desSeuilsIncoherentsNeSontNiEnregistresNiTraces() {
        assertThatThrownBy(() -> service.updateSettings(
                new SpcStudyDto.SettingsCommand(new BigDecimal("1.00"), new BigDecimal("1.50"))))
                .isInstanceOf(SpcValidationException.class);
        assertThat(settings.thresholds(TENANT)).isEmpty();
        assertThat(audit.events).isEmpty();
    }

    @Test
    void uneSaisieInvalideNEstNiEnregistreeNiTracee() {
        assertThatThrownBy(() -> service.create(commande(SpcFixtures.emptyGrid(5), "Vide", LocalDate.now(CLOCK))))
                .isInstanceOf(SpcValidationException.class);
        assertThat(studies.rows).isEmpty();
        assertThat(audit.events).isEmpty();
    }

    @Test
    @DisplayName("l'étude d'un autre client est introuvable, en lecture comme en écriture")
    void autreClient() {
        UUID id = service.create(commande()).study().id();
        context.tenant = AUTRE;

        assertThatThrownBy(() -> service.get(id)).isInstanceOf(SpcNotFoundException.class);
        assertThatThrownBy(() -> service.revise(id, commande())).isInstanceOf(SpcNotFoundException.class);
        assertThatThrownBy(() -> service.setActionPlan(id, true)).isInstanceOf(SpcNotFoundException.class);
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(SpcNotFoundException.class);
        assertThat(audit.events).containsExactly("recorded");
    }

    // ---------- doublures ----------

    static final class FakeStudies implements SpcRepositories.Studies {
        final Map<UUID, CapabilityStudy> rows = new HashMap<>();

        @Override
        public CapabilityStudy save(CapabilityStudy s) {
            UUID id = s.getId() == null ? UUID.randomUUID() : s.getId();
            CapabilityStudy copie = new CapabilityStudy(id, s.getTenantId(), s.getPartReference(),
                    s.getCharacteristic(), s.getSpecification(), s.getMachine(), s.getLeaderUserId(),
                    s.getLeaderName(), s.getStudyDate(), s.isActionPlan(), s.getLsl(), s.getUsl(),
                    s.getSubgroupSize(), s.getMeasurements(), s.getCp(), s.getCpk(), s.getThresholds(),
                    s.getCreatedBy(), s.getCreatedAt(), s.getUpdatedAt());
            rows.put(id, copie);
            return copie;
        }

        @Override
        public Optional<CapabilityStudy> findByIdAndTenant(UUID id, UUID tenantId) {
            return Optional.ofNullable(rows.get(id)).filter(s -> s.getTenantId().equals(tenantId));
        }

        @Override
        public List<CapabilityStudy> findByTenant(UUID tenantId) {
            return rows.values().stream().filter(s -> s.getTenantId().equals(tenantId)).toList();
        }

        @Override
        public void delete(CapabilityStudy s) {
            rows.remove(s.getId());
        }
    }

    static final class FakeSettings implements SpcRepositories.Settings {
        final Map<UUID, CapabilityThresholds> rows = new HashMap<>();
        UUID actor;

        @Override
        public Optional<CapabilityThresholds> thresholds(UUID tenantId) {
            return Optional.ofNullable(rows.get(tenantId));
        }

        @Override
        public void saveThresholds(UUID tenantId, CapabilityThresholds thresholds, UUID actor) {
            rows.put(tenantId, thresholds);
            this.actor = actor;
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

    static final class FakeAudit implements SpcAuditPublisher {
        final List<String> events = new ArrayList<>();
        final List<UUID> actors = new ArrayList<>();

        @Override
        public void recorded(CapabilityStudy study, UUID actor) {
            note("recorded", actor);
        }

        @Override
        public void revised(CapabilityStudy study, UUID actor) {
            note("revised", actor);
        }

        @Override
        public void actionPlanChanged(CapabilityStudy study, UUID actor) {
            note("action-plan", actor);
        }

        @Override
        public void deleted(CapabilityStudy study, UUID actor) {
            note("deleted", actor);
        }

        @Override
        public void thresholdsChanged(UUID tenantId, CapabilityThresholds thresholds, UUID actor) {
            note("thresholds", actor);
        }

        private void note(String event, UUID actor) {
            events.add(event);
            actors.add(actor);
        }
    }
}
