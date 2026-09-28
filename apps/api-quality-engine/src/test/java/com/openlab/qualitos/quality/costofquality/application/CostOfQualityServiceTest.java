package com.openlab.qualitos.quality.costofquality.application;

import com.openlab.qualitos.quality.costofquality.domain.CoqCategory;
import com.openlab.qualitos.quality.costofquality.domain.CoqEntry;
import com.openlab.qualitos.quality.costofquality.domain.CoqLabel;
import com.openlab.qualitos.quality.costofquality.domain.CoqNotFoundException;
import com.openlab.qualitos.quality.costofquality.domain.CoqRepositories;
import com.openlab.qualitos.quality.costofquality.domain.CoqValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le service sur des dépôts en mémoire : ce qui se vérifie ici, ce sont les
 * totaux, le ratio, le rangement des lignes et l'isolation entre clients.
 */
class CostOfQualityServiceTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID AUTRE = UUID.randomUUID();
    static final UUID MOI = UUID.randomUUID();
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-28T08:00:00Z"), ZoneOffset.UTC);

    static final CoqLabel FORMATION = livre(CoqCategory.PREVENTION, "PREVENTION_TRAINING", "Formation", false, 10);
    static final CoqLabel AMDEC = livre(CoqCategory.PREVENTION, "PREVENTION_DESIGN_REVIEW", "AMDEC", false, 40);
    static final CoqLabel RECEPTION = livre(CoqCategory.APPRAISAL, "APPRAISAL_INCOMING_INSPECTION", "Réception", true, 10);
    static final CoqLabel REBUTS = livre(CoqCategory.INTERNAL_FAILURE, "INTERNAL_SCRAP", "Rebuts", true, 10);
    static final CoqLabel RETOUCHES = livre(CoqCategory.INTERNAL_FAILURE, "INTERNAL_REWORK", "Retouches", true, 20);
    static final CoqLabel RECLAMATIONS = livre(CoqCategory.EXTERNAL_FAILURE, "EXTERNAL_COMPLAINTS", "Réclamations", false, 10);

    FakeEntries entries;
    FakeLabels labels;
    FakeSettings settings;
    FakeContext context;
    FakeAudit audit;
    CostOfQualityService service;

    static CoqLabel livre(CoqCategory c, String code, String nom, boolean pieces, int pos) {
        return new CoqLabel(UUID.randomUUID(), null, c, code, nom, pieces, pos);
    }

    @BeforeEach
    void setup() {
        entries = new FakeEntries();
        labels = new FakeLabels();
        settings = new FakeSettings();
        context = new FakeContext();
        audit = new FakeAudit();
        List.of(FORMATION, AMDEC, RECEPTION, REBUTS, RETOUCHES, RECLAMATIONS).forEach(labels::put);
        service = new CostOfQualityService(entries, labels, settings, context, audit, CLOCK);
    }

    CoqDto.EntryCommand simple(CoqLabel l, String montant, LocalDate date) {
        return new CoqDto.EntryCommand(l.getId(), new BigDecimal(montant), "M. Alaoui", date, null,
                null, null, null, null);
    }

    CoqDto.EntryCommand pieces(CoqLabel l, String montant, LocalDate date) {
        return new CoqDto.EntryCommand(l.getId(), new BigDecimal(montant), "Mme Diallo", date, null,
                "P-4410", 12, "L-2609", date.minusDays(3));
    }

    // ---------- rapport mensuel ----------

    @Test
    void leMoisAdditionneSesQuatreBlocsEtCalculeLeRatio() {
        service.record(simple(FORMATION, "170", LocalDate.of(2026, 9, 3)));
        service.record(pieces(RECEPTION, "1547", LocalDate.of(2026, 9, 5)));
        service.record(pieces(REBUTS, "1507", LocalDate.of(2026, 9, 8)));
        service.record(simple(RECLAMATIONS, "1371", LocalDate.of(2026, 9, 30)));
        // Hors période : août et octobre ne comptent pas en septembre.
        service.record(simple(FORMATION, "999", LocalDate.of(2026, 8, 31)));
        service.record(simple(FORMATION, "999", LocalDate.of(2026, 10, 1)));

        CoqDto.ReportView r = service.report(2026, 9);

        assertThat(r.month()).isEqualTo(9);
        assertThat(r.conformanceTotal()).isEqualByComparingTo("1717");
        assertThat(r.nonConformanceTotal()).isEqualByComparingTo("2878");
        assertThat(r.total()).isEqualByComparingTo("4595");
        assertThat(r.ratio()).isEqualByComparingTo("0.60");
        assertThat(r.blocks()).extracting(CoqDto.BlockView::category).containsExactly(CoqCategory.values());
        assertThat(r.blocks().get(0).total()).isEqualByComparingTo("170");
        assertThat(r.months()).isEmpty();
        assertThat(r.currency()).isEqualTo("EUR");
    }

    @Test
    void sansPerteLeRatioEstNulPlutotQueZeroOuInfini() {
        service.record(simple(FORMATION, "170", LocalDate.of(2026, 9, 3)));

        assertThat(service.report(2026, 9).ratio()).isNull();
    }

    @Test
    void lesLibellesLivresApparaissentAZeroEtDansLOrdreDuCatalogue() {
        service.record(simple(AMDEC, "40", LocalDate.of(2026, 9, 3)));

        List<CoqDto.LineView> prevention = service.report(2026, 9).blocks().get(0).lines();

        // La formation (rang 10) passe avant l'AMDEC (rang 40), bien qu'elle
        // n'ait aucune imputation : l'absence de prévention se voit.
        assertThat(prevention).extracting(CoqDto.LineView::labelCode)
                .containsExactly("PREVENTION_TRAINING", "PREVENTION_DESIGN_REVIEW");
        assertThat(prevention.get(0).entryId()).isNull();
        assertThat(prevention.get(0).amount()).isEqualByComparingTo("0");
        assertThat(prevention.get(0).entryCount()).isZero();
        assertThat(prevention.get(1).entryId()).isNotNull();
        assertThat(prevention.get(1).responsible()).isEqualTo("M. Alaoui");
    }

    @Test
    void deuxImputationsDuMemeLibelleFontDeuxLignesRangeesParDate() {
        service.record(pieces(REBUTS, "300", LocalDate.of(2026, 9, 20)));
        service.record(pieces(REBUTS, "100", LocalDate.of(2026, 9, 2)));

        List<CoqDto.LineView> internes = service.report(2026, 9).blocks().get(2).lines();

        assertThat(internes).filteredOn(l -> "INTERNAL_SCRAP".equals(l.labelCode()))
                .extracting(CoqDto.LineView::imputationDate)
                .containsExactly(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 20));
        assertThat(internes.get(0).lot()).isEqualTo("L-2609");
        assertThat(internes.get(0).partQuantity()).isEqualTo(12);
    }

    @Test
    void unLibelleSaisiInutiliseNApparaitPasDansLeRapport() {
        CoqDto.LabelView tri = service.createLabel(
                new CoqDto.LabelCommand(CoqCategory.APPRAISAL, "Tri 100 %", true));

        List<CoqDto.LineView> appreciation = service.report(2026, 9).blocks().get(1).lines();
        assertThat(appreciation).extracting(CoqDto.LineView::labelId).doesNotContain(tri.id());

        service.record(pieces(labels.byId(tri.id()), "80", LocalDate.of(2026, 9, 4)));
        appreciation = service.report(2026, 9).blocks().get(1).lines();
        // Et une fois servi, il se range APRÈS les libellés du catalogue.
        assertThat(appreciation).extracting(CoqDto.LineView::labelName).containsExactly("Réception", "Tri 100 %");
    }

    // ---------- rapport annuel ----------

    @Test
    void lAnneeCumuleParLibelleEtDonneDouzeMois() {
        service.record(simple(FORMATION, "100", LocalDate.of(2026, 1, 10)));
        service.record(simple(FORMATION, "50", LocalDate.of(2026, 6, 10)));
        service.record(pieces(REBUTS, "400", LocalDate.of(2026, 6, 11)));
        service.record(simple(FORMATION, "7", LocalDate.of(2025, 12, 31)));

        CoqDto.ReportView r = service.report(2026, null);

        assertThat(r.month()).isNull();
        CoqDto.LineView formation = r.blocks().get(0).lines().get(0);
        assertThat(formation.entryId()).isNull();
        assertThat(formation.amount()).isEqualByComparingTo("150");
        assertThat(formation.entryCount()).isEqualTo(2);
        assertThat(formation.responsible()).isNull();

        assertThat(r.months()).hasSize(12);
        assertThat(r.months().get(0).conformance()).isEqualByComparingTo("100");
        assertThat(r.months().get(5).conformance()).isEqualByComparingTo("50");
        assertThat(r.months().get(5).nonConformance()).isEqualByComparingTo("400");
        assertThat(r.months().get(11).conformance()).isEqualByComparingTo("0");
        assertThat(r.total()).isEqualByComparingTo("550");
    }

    @Test
    void lAnneeMontreAussiLesLibellesLivresSansImputation() {
        CoqDto.ReportView r = service.report(2026, null);

        assertThat(r.blocks().get(0).lines()).extracting(CoqDto.LineView::labelCode)
                .containsExactly("PREVENTION_TRAINING", "PREVENTION_DESIGN_REVIEW");
        assertThat(r.total()).isEqualByComparingTo("0");
    }

    @Test
    void uneAnneeOuUnMoisHorsBornesEstRefuse() {
        assertThatThrownBy(() -> service.report(1999, null)).isInstanceOf(CoqValidationException.class);
        assertThatThrownBy(() -> service.report(2101, null)).isInstanceOf(CoqValidationException.class);
        assertThatThrownBy(() -> service.report(2026, 0)).isInstanceOf(CoqValidationException.class);
        assertThatThrownBy(() -> service.report(2026, 13)).isInstanceOf(CoqValidationException.class);
    }

    // ---------- isolation ----------

    @Test
    void unClientNeVoitNiLesLignesNiLesLibellesDUnAutre() {
        context.tenant = AUTRE;
        CoqDto.LabelView sien = service.createLabel(new CoqDto.LabelCommand(CoqCategory.PREVENTION, "Kaizen", false));
        service.record(simple(labels.byId(sien.id()), "999", LocalDate.of(2026, 9, 1)));

        context.tenant = TENANT;
        assertThat(service.report(2026, 9).total()).isEqualByComparingTo("0");
        assertThat(service.labels()).extracting(CoqDto.LabelView::id).doesNotContain(sien.id());
        // Et il ne peut pas s'en servir en connaissant son identifiant.
        assertThatThrownBy(() -> service.record(simple(labels.byId(sien.id()), "1", LocalDate.of(2026, 9, 1))))
                .isInstanceOf(CoqNotFoundException.class);
    }

    @Test
    void uneLigneDUnAutreClientNeSeCorrigeNiNeSeSupprime() {
        context.tenant = AUTRE;
        UUID sienne = service.record(simple(FORMATION, "10", LocalDate.of(2026, 9, 1))).entryId();

        context.tenant = TENANT;
        assertThatThrownBy(() -> service.revise(sienne, simple(FORMATION, "0", LocalDate.of(2026, 9, 1))))
                .isInstanceOf(CoqNotFoundException.class);
        assertThatThrownBy(() -> service.delete(sienne)).isInstanceOf(CoqNotFoundException.class);
    }

    // ---------- écriture ----------

    @Test
    void saisirCorrigerEtSupprimerLaissentChacunUneTrace() {
        CoqDto.LineView ligne = service.record(simple(FORMATION, "10", LocalDate.of(2026, 9, 1)));
        service.revise(ligne.entryId(), simple(FORMATION, "20", LocalDate.of(2026, 9, 2)));
        service.delete(ligne.entryId());

        assertThat(audit.events).containsExactly("recorded", "revised", "deleted");
        assertThat(audit.actors).containsOnly(MOI);
        assertThat(entries.store).isEmpty();
    }

    @Test
    void corrigerLaDateDeplaceLaLigneDansUnAutreMois() {
        CoqDto.LineView ligne = service.record(simple(FORMATION, "10", LocalDate.of(2026, 9, 1)));

        service.revise(ligne.entryId(), simple(FORMATION, "10", LocalDate.of(2026, 10, 1)));

        assertThat(service.report(2026, 9).total()).isEqualByComparingTo("0");
        assertThat(service.report(2026, 10).total()).isEqualByComparingTo("10");
    }

    @Test
    void unLibelleAbsentOuInconnuEstRefuse() {
        assertThatThrownBy(() -> service.record(new CoqDto.EntryCommand(null, BigDecimal.ONE, "X",
                LocalDate.of(2026, 9, 1), null, null, null, null, null)))
                .isInstanceOf(CoqValidationException.class);
        assertThatThrownBy(() -> service.record(new CoqDto.EntryCommand(UUID.randomUUID(), BigDecimal.ONE,
                "X", LocalDate.of(2026, 9, 1), null, null, null, null, null)))
                .isInstanceOf(CoqNotFoundException.class);
        assertThat(audit.events).isEmpty();
    }

    @Test
    void uneLigneDePiecesIncompleteNEstNiEnregistreeNiTracee() {
        assertThatThrownBy(() -> service.record(simple(REBUTS, "10", LocalDate.of(2026, 9, 1))))
                .isInstanceOf(CoqValidationException.class);
        assertThat(entries.store).isEmpty();
        assertThat(audit.events).isEmpty();
    }

    @Test
    void unLibelleDejaPresentEstRenduPlutotQueDouble() {
        CoqDto.LabelView premier = service.createLabel(
                new CoqDto.LabelCommand(CoqCategory.APPRAISAL, "Tri  100 %", true));
        CoqDto.LabelView second = service.createLabel(
                new CoqDto.LabelCommand(CoqCategory.APPRAISAL, " tri 100 % ", false));
        CoqDto.LabelView livre = service.createLabel(
                new CoqDto.LabelCommand(CoqCategory.INTERNAL_FAILURE, "REBUTS", false));

        assertThat(second.id()).isEqualTo(premier.id());
        assertThat(livre.id()).isEqualTo(REBUTS.getId());
        assertThat(livre.builtIn()).isTrue();
    }

    @Test
    void leMemeNomDansUneAutreFamilleEstUnAutreLibelle() {
        CoqDto.LabelView a = service.createLabel(new CoqDto.LabelCommand(CoqCategory.APPRAISAL, "Tri", true));
        CoqDto.LabelView b = service.createLabel(new CoqDto.LabelCommand(CoqCategory.INTERNAL_FAILURE, "Tri", true));

        assertThat(a.id()).isNotEqualTo(b.id());
    }

    @Test
    void leCatalogueSeLitRangeParFamillePuisParRang() {
        assertThat(service.labels()).extracting(CoqDto.LabelView::code).containsExactly(
                "PREVENTION_TRAINING", "PREVENTION_DESIGN_REVIEW", "APPRAISAL_INCOMING_INSPECTION",
                "INTERNAL_SCRAP", "INTERNAL_REWORK", "EXTERNAL_COMPLAINTS");
    }

    @Test
    void laDeviseSeNormaliseEtSeRetrouveDansLeRapport() {
        assertThat(service.setCurrency(" usd ")).isEqualTo("USD");
        assertThat(service.report(2026, 9).currency()).isEqualTo("USD");
    }

    @Test
    void uneDeviseInconnueEstRefusee() {
        assertThatThrownBy(() -> service.setCurrency("ZZZ")).isInstanceOf(CoqValidationException.class);
        assertThatThrownBy(() -> service.setCurrency(null)).isInstanceOf(CoqValidationException.class);
    }

    // ---------- doublures ----------

    static final class FakeEntries implements CoqRepositories.Entries {
        final Map<UUID, CoqEntry> store = new LinkedHashMap<>();

        @Override
        public CoqEntry save(CoqEntry e) {
            UUID id = e.getId() == null ? UUID.randomUUID() : e.getId();
            CoqEntry copie = new CoqEntry(id, e.getTenantId(), e.getLabelId(), e.getCategory(), e.getAmount(),
                    e.getResponsible(), e.getImputationDate(), e.getComment(), e.isPartControl(),
                    e.getPartReference(), e.getPartQuantity(), e.getLot(), e.getReceivedOrMadeOn(),
                    e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
            store.put(id, copie);
            return copie;
        }

        @Override
        public Optional<CoqEntry> findByIdAndTenant(UUID id, UUID tenantId) {
            return Optional.ofNullable(store.get(id)).filter(e -> e.getTenantId().equals(tenantId));
        }

        @Override
        public List<CoqEntry> findByTenantBetween(UUID tenantId, LocalDate from, LocalDate to) {
            return store.values().stream()
                    .filter(e -> e.getTenantId().equals(tenantId))
                    .filter(e -> !e.getImputationDate().isBefore(from) && !e.getImputationDate().isAfter(to))
                    .toList();
        }

        @Override
        public void delete(CoqEntry entry) {
            store.remove(entry.getId());
        }
    }

    static final class FakeLabels implements CoqRepositories.Labels {
        final Map<UUID, CoqLabel> store = new LinkedHashMap<>();

        void put(CoqLabel l) { store.put(l.getId(), l); }

        CoqLabel byId(UUID id) { return store.get(id); }

        @Override
        public List<CoqLabel> findVisible(UUID tenantId) {
            // Ordre d'insertion volontairement quelconque : le tri est l'affaire du service.
            List<CoqLabel> visibles = new ArrayList<>(store.values().stream()
                    .filter(l -> l.getTenantId() == null || l.getTenantId().equals(tenantId)).toList());
            java.util.Collections.reverse(visibles);
            return visibles;
        }

        @Override
        public Optional<CoqLabel> findVisibleById(UUID id, UUID tenantId) {
            return findVisible(tenantId).stream().filter(l -> l.getId().equals(id)).findFirst();
        }

        @Override
        public CoqLabel save(CoqLabel l) {
            CoqLabel copie = new CoqLabel(UUID.randomUUID(), l.getTenantId(), l.getCategory(), l.getCode(),
                    l.getName(), l.isPartControl(), l.getPosition());
            put(copie);
            return copie;
        }
    }

    static final class FakeSettings implements CoqRepositories.Settings {
        final Map<UUID, String> store = new HashMap<>();

        @Override
        public Optional<String> currency(UUID tenantId) { return Optional.ofNullable(store.get(tenantId)); }

        @Override
        public void saveCurrency(UUID tenantId, String currency) { store.put(tenantId, currency); }
    }

    static final class FakeContext implements CoqContext {
        UUID tenant = TENANT;

        @Override
        public UUID requireTenantId() { return tenant; }

        @Override
        public UUID requireActorId() { return MOI; }
    }

    static final class FakeAudit implements CoqAuditPublisher {
        final List<String> events = new ArrayList<>();
        final List<UUID> actors = new ArrayList<>();

        @Override
        public void recorded(CoqEntry entry, UUID actor) { events.add("recorded"); actors.add(actor); }

        @Override
        public void revised(CoqEntry entry, UUID actor) { events.add("revised"); actors.add(actor); }

        @Override
        public void deleted(CoqEntry entry, UUID actor) { events.add("deleted"); actors.add(actor); }
    }
}
