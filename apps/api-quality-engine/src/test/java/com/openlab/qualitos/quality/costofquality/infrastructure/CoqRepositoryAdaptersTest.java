package com.openlab.qualitos.quality.costofquality.infrastructure;

import com.openlab.qualitos.quality.costofquality.domain.CoqCategory;
import com.openlab.qualitos.quality.costofquality.domain.CoqEntry;
import com.openlab.qualitos.quality.costofquality.domain.CoqEntryDetails;
import com.openlab.qualitos.quality.costofquality.domain.CoqLabel;
import com.openlab.qualitos.quality.costofquality.domain.CoqNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Les adaptateurs sur un vrai mapping JPA (contexte complet, H2 en mode
 * PostgreSQL) : les requêtes JPQL, la conversion aller-retour et la
 * frontière entre clients se vérifient contre un moteur, pas contre une
 * doublure qui rendrait l'objet qu'on lui a donné.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
class CoqRepositoryAdaptersTest {

    @Autowired CoqEntryJpaRepository entriesJpa;
    @Autowired CoqLabelJpaRepository labelsJpa;
    @Autowired CoqSettingsJpaRepository settingsJpa;
    @Autowired Clock clock;

    CoqRepositoryAdapters.Entries entries;
    CoqRepositoryAdapters.Labels labels;
    CoqRepositoryAdapters.Settings settings;

    UUID tenant;
    CoqLabel rebuts;

    @BeforeEach
    void setup() {
        entries = new CoqRepositoryAdapters.Entries(entriesJpa);
        labels = new CoqRepositoryAdapters.Labels(labelsJpa, clock);
        settings = new CoqRepositoryAdapters.Settings(settingsJpa, clock);
        tenant = UUID.randomUUID();
        // Le catalogue livré vient de la migration, absente de ce profil : on
        // pose un libellé sans tenant, comme elle le ferait.
        rebuts = labels.save(new CoqLabel(null, null, CoqCategory.INTERNAL_FAILURE,
                "SCRAP_" + UUID.randomUUID(), "Rebuts", true, 10));
    }

    CoqEntry ligne(UUID t, LocalDate date) {
        return CoqEntry.record(t, rebuts, new CoqEntryDetails(new BigDecimal("1263.50"), "Mme Diallo",
                date, "note", "P-4410", 12, "L-2609", date.minusDays(3)), UUID.randomUUID(), Instant.now());
    }

    @Test
    void uneLigneSEnregistreEtSeRelitAChampsEgaux() {
        CoqEntry enregistree = entries.save(ligne(tenant, LocalDate.of(2026, 9, 15)));

        CoqEntry relue = entries.findByIdAndTenant(enregistree.getId(), tenant).orElseThrow();
        assertThat(relue.getAmount()).isEqualByComparingTo("1263.50");
        assertThat(relue.getCategory()).isEqualTo(CoqCategory.INTERNAL_FAILURE);
        assertThat(relue.getLot()).isEqualTo("L-2609");
        assertThat(relue.getPartQuantity()).isEqualTo(12);
        assertThat(relue.getReceivedOrMadeOn()).isEqualTo(LocalDate.of(2026, 9, 12));
        assertThat(relue.getComment()).isEqualTo("note");
    }

    @Test
    void laPeriodeInclutSesDeuxBornesEtExclutLeReste() {
        entries.save(ligne(tenant, LocalDate.of(2026, 9, 1)));
        entries.save(ligne(tenant, LocalDate.of(2026, 9, 30)));
        entries.save(ligne(tenant, LocalDate.of(2026, 10, 1)));
        entries.save(ligne(UUID.randomUUID(), LocalDate.of(2026, 9, 15)));

        List<CoqEntry> septembre = entries.findByTenantBetween(tenant,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(septembre).hasSize(2);
    }

    @Test
    void uneCorrectionSurUneLigneDisparueRend404AuLieuDeLaRessusciter() {
        CoqEntry enregistree = entries.save(ligne(tenant, LocalDate.of(2026, 9, 15)));
        entries.delete(enregistree);

        assertThat(entries.findByIdAndTenant(enregistree.getId(), tenant)).isEmpty();
        assertThatThrownBy(() -> entries.save(enregistree)).isInstanceOf(CoqNotFoundException.class);
    }

    @Test
    void uneLigneDUnAutreClientEstIntrouvable() {
        CoqEntry enregistree = entries.save(ligne(tenant, LocalDate.of(2026, 9, 15)));

        assertThat(entries.findByIdAndTenant(enregistree.getId(), UUID.randomUUID())).isEmpty();
    }

    @Test
    void leCatalogueVisibleEstLeLivrePlusLeSienJamaisCeluiDUnAutre() {
        CoqLabel mien = labels.save(CoqLabel.custom(tenant, CoqCategory.APPRAISAL, "Tri", true));
        CoqLabel sien = labels.save(CoqLabel.custom(UUID.randomUUID(), CoqCategory.APPRAISAL, "Tri", true));

        List<UUID> visibles = labels.findVisible(tenant).stream().map(CoqLabel::getId).toList();

        assertThat(visibles).contains(rebuts.getId(), mien.getId()).doesNotContain(sien.getId());
        assertThat(labels.findVisibleById(sien.getId(), tenant)).isEmpty();
        assertThat(labels.findVisibleById(rebuts.getId(), tenant)).isPresent();
        assertThat(labels.findVisibleById(mien.getId(), tenant).orElseThrow().isPartControl()).isTrue();
    }

    @Test
    void laDeviseSEcritPuisSeRemplace() {
        assertThat(settings.currency(tenant)).isEmpty();

        settings.saveCurrency(tenant, "USD");
        settings.saveCurrency(tenant, "CHF");

        assertThat(settings.currency(tenant)).contains("CHF");
    }
}
