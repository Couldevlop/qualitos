package com.openlab.qualitos.quality.tenantmodules.application;

import com.openlab.qualitos.quality.edition.TestLicenses;
import com.openlab.qualitos.quality.tenantmodules.domain.BillingTier;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivation;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivationRepository;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivationStateException;
import com.openlab.qualitos.quality.tenantmodules.infrastructure.LicensedModules;
import com.openlab.qualitos.licensing.application.Licensing;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** En on-premise, la licence ouvre les modules ; le client peut en fermer, jamais en ouvrir d'autres (ADR 0082). */
class ModuleActivationLicenseTest {

    static final UUID TENANT = UUID.randomUUID();
    static final Instant NOW = TestLicenses.ISSUED.plusSeconds(3600);

    final ModuleActivationRepository repo = mock(ModuleActivationRepository.class);

    ModuleActivationService service(Licensing licensing) {
        LicensedModules modules = new LicensedModules(licensing);
        TenantProvider tenant = () -> TENANT;
        ActorProvider actor = () -> UUID.randomUUID();
        return new ModuleActivationService(repo, tenant, modules, actor, new ModuleActivationEventPublisher.NoOp(),
                Clock.fixed(NOW, ZoneOffset.UTC), modules);
    }

    @Test
    void laLicenceOuvreDOfficeSesModulesEnPlusDuSocle() {
        when(repo.findAllByTenantId(TENANT)).thenReturn(List.of());
        ModuleActivationService s = service(TestLicenses.onPrem(
                TestLicenses.license(TENANT, "PRO", Set.of("dmaic", "standards"), 0), NOW));

        assertThat(s.enabledModuleCodes()).contains("pdca", "capa", "dmaic", "standards").doesNotContain("iot");
        assertThat(s.isEnabled("dmaic")).isTrue();
        assertThat(s.isEnabled("iot")).isFalse();
        assertThat(s.summary().tenantTier()).isEqualTo(BillingTier.PRO);
    }

    @Test
    void uneLicenceTousModulesOuvreToutLeCatalogue() {
        when(repo.findAllByTenantId(TENANT)).thenReturn(List.of());
        ModuleActivationService s = service(TestLicenses.onPrem(
                TestLicenses.license(TENANT, "ENTERPRISE", Set.of("*"), 0), NOW));
        assertThat(s.enabledModuleCodes()).contains("iot", "blockchain", "itsm");
    }

    @Test
    void leClientPeutFermerUnModuleDeSaLicence() {
        ModuleActivation fermee = ModuleActivation.activateNow(TENANT, "dmaic", BillingTier.STANDARD, null,
                UUID.randomUUID(), NOW.minusSeconds(60));
        fermee.disable(UUID.randomUUID(), NOW.minusSeconds(30));
        when(repo.findAllByTenantId(TENANT)).thenReturn(List.of(fermee));
        ModuleActivationService s = service(TestLicenses.onPrem(
                TestLicenses.license(TENANT, "PRO", Set.of("dmaic"), 0), NOW));

        assertThat(s.enabledModuleCodes()).doesNotContain("dmaic");
    }

    @Test
    void uneActivationHorsLicenceNeRouvreRien_etOnNePeutPasEnCreer() {
        ModuleActivation ancienne = ModuleActivation.activateNow(TENANT, "iot", BillingTier.PRO, null,
                UUID.randomUUID(), NOW.minusSeconds(60));
        when(repo.findAllByTenantId(TENANT)).thenReturn(List.of(ancienne));
        when(repo.findOpenByTenantIdAndCode(any(), any())).thenReturn(Optional.empty());
        ModuleActivationService s = service(TestLicenses.onPrem(
                TestLicenses.license(TENANT, "ENTERPRISE", Set.of("dmaic"), 0), NOW));

        assertThat(s.enabledModuleCodes()).doesNotContain("iot");
        assertThatThrownBy(() -> s.activate(new ModuleActivationDto.ActivateRequest("iot", null)))
                .isInstanceOf(ModuleActivationStateException.class)
                .hasMessageContaining("license");
        verify(repo, never()).save(any());
    }

    @Test
    void sansLicenceSeulLeSocleResteOuvert_auPalierGratuit() {
        when(repo.findAllByTenantId(TENANT)).thenReturn(List.of());
        ModuleActivationService s = service(TestLicenses.unlicensed(NOW));

        assertThat(s.enabledModuleCodes()).contains("pdca", "capa").doesNotContain("dmaic");
        assertThat(s.summary().tenantTier()).isEqualTo(BillingTier.FREE);
    }

    @Test
    void enSaasLaLicenceNeGouverneRien() {
        LicensedModules saas = new LicensedModules(Licensing.saas(Clock.systemUTC()));
        assertThat(saas.governs()).isFalse();
        assertThat(saas.allows("iot")).isTrue();
        assertThat(ModuleLicense.NONE.governs()).isFalse();
        assertThat(ModuleLicense.NONE.allows("x")).isTrue();
    }
}
