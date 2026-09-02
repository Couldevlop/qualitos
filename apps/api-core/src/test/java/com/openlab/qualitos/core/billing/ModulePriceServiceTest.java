package com.openlab.qualitos.core.billing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.openlab.qualitos.core.billing.BillingPeriod.ANNUAL;
import static com.openlab.qualitos.core.billing.BillingPeriod.MONTHLY;
import static com.openlab.qualitos.core.billing.BillingTier.STANDARD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModulePriceServiceTest {

    @Mock ModulePriceRepository repo;
    ModulePriceService service;

    static final UUID ACTOR = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        service = new ModulePriceService(repo, CLOCK);
    }

    // ---------- priceOf : les deux tests du brief ----------

    @Test
    void leTarifAnnuelNEstPasDouzeFoisLeMensuel() {
        // Une remise annuelle est une decision commerciale. La deduire par calcul
        // interdirait de la fixer.
        when(repo.find("controlplan", STANDARD, MONTHLY))
                .thenReturn(Optional.of(prix(9900)));
        when(repo.find("controlplan", STANDARD, ANNUAL))
                .thenReturn(Optional.of(prix(99000)));   // dix mois, pas douze

        // Les deux stubs sont exerces : le mensuel ET l'annuel sont lus tels
        // quels depuis leurs lignes respectives, l'un n'est jamais deduit de
        // l'autre par un facteur douze.
        assertThat(service.priceOf("controlplan", STANDARD, MONTHLY))
                .contains(Money.of(9900, "EUR"));
        assertThat(service.priceOf("controlplan", STANDARD, ANNUAL))
                .contains(Money.of(99000, "EUR"));
    }

    @Test
    void unModuleSansTarifNeSeFacturePas_etLeDit() {
        // Rendre zero silencieusement ferait passer un tarif oublie pour un module
        // gratuit, et la perte ne se verrait qu'a la lecture de la facture.
        when(repo.find("nouveau", STANDARD, MONTHLY)).thenReturn(Optional.empty());

        assertThat(service.priceOf("nouveau", STANDARD, MONTHLY)).isEmpty();
    }

    // ---------- findAll ----------

    @Test
    void leCatalogueEstTrieParModulePuisPalierPuisPeriode() {
        when(repo.findAll(org.springframework.data.domain.Sort.by(
                "moduleCode", "billingTier", "period")))
                .thenReturn(List.of(prix(9900), prix(99000)));

        List<ModulePriceDto.View> vues = service.findAll();

        assertThat(vues).hasSize(2);
        assertThat(vues.get(0).moduleCode()).isEqualTo("controlplan");
    }

    @Test
    void leCatalogueVideEstUneListeVide() {
        when(repo.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of());

        assertThat(service.findAll()).isEmpty();
    }

    // ---------- setPrice ----------

    @Test
    void fixerUnTarifInexistantEnCreeUn() {
        // Symetrique de "un premier enregistrement cree un profil" dans
        // BillingProfileServiceTest : sans ligne existante, setPrice() en
        // cree une nouvelle, avec son propre id et l'horodatage fige.
        when(repo.find("controlplan", STANDARD, MONTHLY)).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        ModulePriceDto.View vue = service.setPrice(commandeValide(), ACTOR);

        ArgumentCaptor<ModulePrice> saved = ArgumentCaptor.forClass(ModulePrice.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getId()).isNotNull();
        assertThat(saved.getValue().getModuleCode()).isEqualTo("controlplan");
        assertThat(saved.getValue().getBillingTier()).isEqualTo(STANDARD);
        assertThat(saved.getValue().getPeriod()).isEqualTo(MONTHLY);
        assertThat(saved.getValue().getUpdatedAt()).isEqualTo(NOW);
        assertThat(saved.getValue().getUpdatedBy()).isEqualTo(ACTOR);
        assertThat(vue.amountCents()).isEqualTo(9900);
    }

    @Test
    void fixerUnTarifExistantLeRemplace_pasDeSecondeLigne() {
        // Un second appel sur le meme triplet (module, palier, periode) met a
        // jour la ligne existante : deux lignes pour le meme triplet
        // rendraient priceOf() ambigu, et la contrainte uk_module_price le
        // refuserait de toute facon en base.
        ModulePrice existant = prix(5000);
        when(repo.find("controlplan", STANDARD, MONTHLY)).thenReturn(Optional.of(existant));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        ModulePriceDto.View vue = service.setPrice(commandeValide(), ACTOR);

        ArgumentCaptor<ModulePrice> saved = ArgumentCaptor.forClass(ModulePrice.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(existant.getId());
        assertThat(saved.getValue().getAmountCents()).isEqualTo(9900);
        assertThat(vue.updatedBy()).isEqualTo(ACTOR);
    }

    // --- Fixtures -----------------------------------------------------------

    private ModulePriceDto.SaveCommand commandeValide() {
        return new ModulePriceDto.SaveCommand("controlplan", STANDARD, MONTHLY, 9900, "EUR");
    }

    // Un seul prix "special" (99000, dix mois) sert au test du prix annuel ;
    // tous les autres sont mensuels a 9900 (99,00 EUR) — la fixture prend
    // directement le montant en parametre plutot que de le deviner.
    private ModulePrice prix(long amountCents) {
        return ModulePrice.builder()
                .id(UUID.randomUUID())
                .moduleCode("controlplan")
                .billingTier(STANDARD)
                .period(amountCents == 99000 ? ANNUAL : MONTHLY)
                .amountCents(amountCents)
                .currency("EUR")
                .updatedAt(NOW)
                .updatedBy(ACTOR)
                .build();
    }
}
