package com.openlab.qualitos.core.edition;

import com.openlab.qualitos.core.tenant.Tenant;
import com.openlab.qualitos.core.tenant.TenantController;
import com.openlab.qualitos.core.tenant.TenantRepository;
import com.openlab.qualitos.core.user.UserController;
import com.openlab.qualitos.licensing.application.Licensing;
import com.openlab.qualitos.licensing.domain.License;
import com.openlab.qualitos.licensing.domain.LicenseStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** L'édition dans api-core : console éditeur absente, lecture seule, plafond, client unique (ADR 0082). */
class EditionUnitTest {

    static final UUID TENANT = UUID.fromString("5e1f0c3a-2b4d-4e6f-8a9b-0c1d2e3f4a5b");
    static final Instant NOW = TestLicenses.ISSUED.plus(Duration.ofDays(10));

    static License licence(int maxUsers) {
        return TestLicenses.license(TENANT, "PRO", Set.of("*"), maxUsers);
    }

    @Nested
    class Garde {

        @SuppressWarnings("unchecked")
        EditionGuard garde(Licensing l) {
            ObjectProvider<Licensing> p = mock(ObjectProvider.class);
            when(p.getIfAvailable()).thenReturn(l);
            return new EditionGuard(p);
        }

        HandlerMethod methode(Class<?> type, String nom) throws NoSuchMethodException {
            for (var m : type.getDeclaredMethods()) {
                if (m.getName().equals(nom)) {
                    return new HandlerMethod(mock(type), m);
                }
            }
            throw new NoSuchMethodException(nom);
        }

        boolean passe(EditionGuard g, String verbe, HandlerMethod h) {
            return g.preHandle(new MockHttpServletRequest(verbe, "/api/v1/x"), new MockHttpServletResponse(), h);
        }

        @Test
        void enSaasEtSansLicenceInjecteeTOutPasse() throws Exception {
            HandlerMethod onboard = methode(TenantController.class, "onboardTenant");
            assertThat(passe(garde(Licensing.saas(Clock.systemUTC())), "POST", onboard)).isTrue();
            assertThat(passe(garde(null), "POST", onboard)).isTrue();
            assertThat(garde(TestLicenses.unlicensed(NOW)).preHandle(new MockHttpServletRequest("POST", "/x"),
                    new MockHttpServletResponse(), new Object())).isTrue();
        }

        @Test
        void enOnPremLaConsoleEditeurNExistePas_memeEnLecture() throws Exception {
            EditionGuard g = garde(TestLicenses.onPrem(licence(0), NOW));
            assertThatThrownBy(() -> passe(g, "GET", methode(TenantController.class, "listTenants")))
                    .isInstanceOf(EditionExceptions.NotInThisEdition.class);
            assertThat(passe(g, "POST", methode(UserController.class, "inviteUser"))).isTrue();
        }

        @Test
        void sansLicenceValableLesEcrituresSontRefusees() throws Exception {
            EditionGuard g = garde(TestLicenses.unlicensed(NOW));
            HandlerMethod invite = methode(UserController.class, "inviteUser");
            assertThatThrownBy(() -> passe(g, "POST", invite))
                    .isInstanceOf(EditionExceptions.LicenseReadOnly.class)
                    .satisfies(e -> assertThat(((EditionExceptions.LicenseReadOnly) e).getStatus())
                            .isEqualTo(LicenseStatus.MISSING));
            assertThat(passe(g, "GET", invite)).isTrue();
            assertThat(new EditionExceptions.LicenseReadOnly(LicenseStatus.EXPIRED, null).getMessage())
                    .contains("EXPIRED");
        }
    }

    @Nested
    class Plafond {

        @Test
        void sansPlafondRienNEstRefuse() {
            MemberLimit.NONE.ensureRoomForOneMore(1_000_000);
            MemberLimit.of(Licensing.saas(Clock.systemUTC())).ensureRoomForOneMore(1_000_000);
            MemberLimit.of(TestLicenses.onPrem(licence(0), NOW)).ensureRoomForOneMore(1_000_000);
            MemberLimit.of(TestLicenses.unlicensed(NOW)).ensureRoomForOneMore(1_000_000);
        }

        @Test
        void laLicenceFixeLeNombreDUtilisateursActifs() {
            MemberLimit vingtCinq = MemberLimit.of(TestLicenses.onPrem(licence(25), NOW));
            vingtCinq.ensureRoomForOneMore(24);
            assertThatThrownBy(() -> vingtCinq.ensureRoomForOneMore(25))
                    .isInstanceOf(EditionExceptions.MemberLimitReached.class)
                    .hasMessageContaining("25")
                    .satisfies(e -> assertThat(((EditionExceptions.MemberLimitReached) e).getLimit()).isEqualTo(25));
        }
    }

    @Nested
    class ClientUnique {

        final TenantRepository tenants = mock(TenantRepository.class);
        final Clock horloge = Clock.fixed(NOW, ZoneOffset.UTC);

        @Test
        void leClientDeLaLicenceNaitAvecSonIdentifiant() {
            when(tenants.findById(TENANT)).thenReturn(Optional.empty(), Optional.of(new Tenant()));
            when(tenants.existsBySlug("client-de-test")).thenReturn(false);
            new OnPremTenantBootstrap(TestLicenses.onPrem(licence(0), NOW), tenants, horloge).onReady();
            verify(tenants).insertWithId(TENANT, "client-de-test", "Client de test", "PRO", NOW);
        }

        @Test
        void unSlugDejaPrisEstRenduUnique() {
            when(tenants.findById(TENANT)).thenReturn(Optional.empty());
            when(tenants.existsBySlug("client-de-test")).thenReturn(true);
            new OnPremTenantBootstrap(TestLicenses.onPrem(licence(0), NOW), tenants, horloge).ensureTenant();
            verify(tenants).insertWithId(eq(TENANT), eq("client-de-test-5e1f0c3a"), any(), any(), any());
        }

        @Test
        void leNomEtLePlanSuiventLaLicence_sansRecreer() {
            Tenant t = Tenant.builder().id(TENANT).slug("x-y-z").name("Ancien nom").plan(Tenant.Plan.STARTER).build();
            when(tenants.findById(TENANT)).thenReturn(Optional.of(t));
            new OnPremTenantBootstrap(TestLicenses.onPrem(licence(0), NOW), tenants, horloge).ensureTenant();
            assertThat(t.getName()).isEqualTo("Client de test");
            assertThat(t.getPlan()).isEqualTo(Tenant.Plan.PRO);
            verify(tenants).save(t);
            verify(tenants, never()).insertWithId(any(), any(), any(), any(), any());
        }

        @Test
        void enSaasOuSansLicenceRienNEstCree() {
            assertThat(new OnPremTenantBootstrap(Licensing.saas(horloge), tenants, horloge).ensureTenant()).isEmpty();
            assertThat(new OnPremTenantBootstrap(TestLicenses.unlicensed(NOW), tenants, horloge).ensureTenant())
                    .isEmpty();
            verify(tenants, never()).insertWithId(any(), any(), any(), any(), any());
        }

        @Test
        void slugsEtPlans() {
            assertThat(OnPremTenantBootstrap.slug("Hôpital Saint-Louis")).isEqualTo("hopital-saint-louis");
            assertThat(OnPremTenantBootstrap.slug("  É ")).isEqualTo("client-e");
            assertThat(OnPremTenantBootstrap.slug("@@")).isEqualTo("client-local");
            assertThat(OnPremTenantBootstrap.slug("a".repeat(80))).hasSize(63);
            assertThat(OnPremTenantBootstrap.planOf(TestLicenses.license(TENANT, "FREE", Set.of("*"), 0)))
                    .isEqualTo(Tenant.Plan.STARTER);
            assertThat(OnPremTenantBootstrap.planOf(TestLicenses.license(TENANT, "ENTERPRISE", Set.of("*"), 0)))
                    .isEqualTo(Tenant.Plan.ENTERPRISE);
        }
    }
}
