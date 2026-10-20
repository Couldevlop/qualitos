package com.openlab.qualitos.quality.spc.capability.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.spc.capability.domain.CapabilityStudy;
import com.openlab.qualitos.quality.spc.capability.domain.CapabilityThresholds;
import com.openlab.qualitos.quality.spc.capability.domain.SpcFixtures;
import com.openlab.qualitos.quality.spc.capability.domain.SpcNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Les adaptateurs sur un vrai mapping JPA (contexte complet, H2 en mode
 * PostgreSQL) : la grille aller-retour en JSON, les seuils figés, et la
 * frontière entre clients.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
class SpcRepositoryAdaptersTest {

    @Autowired SpcStudyJpaRepository studiesJpa;
    @Autowired SpcSettingsJpaRepository settingsJpa;
    @Autowired ObjectMapper objectMapper;
    @Autowired Clock clock;

    SpcRepositoryAdapters.Studies studies;
    SpcRepositoryAdapters.Settings settings;
    UUID tenant;

    @BeforeEach
    void setup() {
        studies = new SpcRepositoryAdapters.Studies(studiesJpa, objectMapper);
        settings = new SpcRepositoryAdapters.Settings(settingsJpa, clock);
        tenant = UUID.randomUUID();
    }

    CapabilityStudy etude(UUID t) {
        List<List<Double>> grille = SpcFixtures.goldenGrid();
        grille.get(4).set(19, null);
        return CapabilityStudy.record(t, SpcFixtures.details(grille, 5), CapabilityThresholds.DEFAULT,
                UUID.randomUUID(), Instant.now().truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    void uneEtudeSEnregistreEtSeRelitAChampsEgaux() {
        CapabilityStudy enregistree = studies.save(etude(tenant));

        CapabilityStudy relue = studies.findByIdAndTenant(enregistree.getId(), tenant).orElseThrow();
        assertThat(relue.getPartReference()).isEqualTo("Shaft AX");
        assertThat(relue.getLeaderName()).isEqualTo("J. Martin");
        assertThat(relue.getLeaderUserId()).isNotNull();
        assertThat(relue.getMeasurements()).hasSize(5);
        assertThat(relue.getMeasurements().get(0).get(0)).isEqualTo(504.0);
        assertThat(relue.getMeasurements().get(4).get(19)).isNull();
        assertThat(relue.getCp()).isEqualTo(enregistree.getCp());
        assertThat(relue.getCpk()).isEqualTo(enregistree.getCpk());
        assertThat(relue.getThresholds().capable()).isEqualByComparingTo("1.67");
        assertThat(relue.calculation().completeSubgroups()).isEqualTo(19);
    }

    @Test
    void uneCorrectionReecritLaLigneSansChangerSesSeuils() {
        CapabilityStudy enregistree = studies.save(etude(tenant));
        enregistree.setActionPlan(true, Instant.now());

        CapabilityStudy corrigee = studies.save(enregistree);

        assertThat(corrigee.getId()).isEqualTo(enregistree.getId());
        assertThat(corrigee.isActionPlan()).isTrue();
        assertThat(studies.findByTenant(tenant)).hasSize(1);
    }

    @Test
    void lesEtudesDUnAutreClientNeSeVoientNiNeSEcrivent() {
        CapabilityStudy chezMoi = studies.save(etude(tenant));
        UUID autre = UUID.randomUUID();
        studies.save(etude(autre));

        assertThat(studies.findByTenant(tenant)).extracting(CapabilityStudy::getId).containsExactly(chezMoi.getId());
        assertThat(studies.findByIdAndTenant(chezMoi.getId(), autre)).isEmpty();

        CapabilityStudy usurpee = new CapabilityStudy(chezMoi.getId(), autre, "X", null, null, null, null, null,
                chezMoi.getStudyDate(), false, 475, 525, 5, chezMoi.getMeasurements(), 1, 1,
                CapabilityThresholds.DEFAULT, null, Instant.now(), Instant.now());
        assertThatThrownBy(() -> studies.save(usurpee)).isInstanceOf(SpcNotFoundException.class);
        studies.delete(usurpee);
        assertThat(studies.findByIdAndTenant(chezMoi.getId(), tenant)).isPresent();
    }

    @Test
    void supprimerRetireLEtude() {
        CapabilityStudy e = studies.save(etude(tenant));
        studies.delete(e);
        assertThat(studies.findByIdAndTenant(e.getId(), tenant)).isEmpty();
    }

    @Test
    void lesSeuilsSEcriventPuisSeRemplacent() {
        assertThat(settings.thresholds(tenant)).isEmpty();
        UUID admin = UUID.randomUUID();

        settings.saveThresholds(tenant, new CapabilityThresholds(new BigDecimal("2.00"), new BigDecimal("1.50")), admin);
        settings.saveThresholds(tenant, new CapabilityThresholds(new BigDecimal("1.50"), new BigDecimal("1.00")), admin);

        assertThat(settings.thresholds(tenant)).hasValueSatisfying(s -> {
            assertThat(s.capable()).isEqualByComparingTo("1.50");
            assertThat(s.watch()).isEqualByComparingTo("1.00");
        });
        assertThat(settingsJpa.findById(tenant)).hasValueSatisfying(e -> assertThat(e.getUpdatedBy()).isEqualTo(admin));
        assertThat(settings.thresholds(UUID.randomUUID())).isEmpty();
    }

    @Test
    void uneGrilleIllisibleEstUneErreurDeStockage() {
        CapabilityStudy e = studies.save(etude(tenant));
        SpcStudyJpaEntity ligne = studiesJpa.findById(e.getId()).orElseThrow();
        ligne.setMeasurements("pas du json");
        studiesJpa.save(ligne);

        assertThatThrownBy(() -> studies.findByIdAndTenant(e.getId(), tenant))
                .isInstanceOf(IllegalStateException.class);
    }
}
