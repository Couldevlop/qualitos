package com.openlab.qualitos.quality.edition;

import com.openlab.qualitos.licensing.application.Licensing;
import com.openlab.qualitos.licensing.domain.LicenseStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** La lecture seule d'une installation on-premise sans licence valable (ADR 0082). */
class LicenseWriteGuardTest {

    final HandlerMethod handler = mock(HandlerMethod.class);

    @SuppressWarnings("unchecked")
    static LicenseWriteGuard guard(Licensing licensing) {
        ObjectProvider<Licensing> p = mock(ObjectProvider.class);
        when(p.getIfAvailable()).thenReturn(licensing);
        return new LicenseWriteGuard(p);
    }

    boolean passe(LicenseWriteGuard g, String method) {
        return g.preHandle(new MockHttpServletRequest(method, "/api/v1/nc"), new MockHttpServletResponse(), handler);
    }

    @Test
    void enSaasEtSansLicenceInjecteeRienNEstBloque() {
        assertThat(passe(guard(Licensing.saas(Clock.systemUTC())), "POST")).isTrue();
        assertThat(passe(guard(null), "DELETE")).isTrue();
    }

    @Test
    void uneLicenceValableOuEnGraceLaisseEcrire() {
        var l = TestLicenses.license(UUID.randomUUID(), "PRO", Set.of("*"), 0);
        assertThat(passe(guard(TestLicenses.onPrem(l, TestLicenses.ISSUED.plusSeconds(60))), "POST")).isTrue();
        assertThat(passe(guard(TestLicenses.onPrem(l, TestLicenses.EXPIRES.plus(Duration.ofDays(5)))), "PATCH"))
                .isTrue();
    }

    @Test
    void sansLicenceOuApresLaGraceLesEcrituresSontRefusees_jamaisLesLectures() {
        LicenseWriteGuard sans = guard(TestLicenses.unlicensed(TestLicenses.ISSUED));
        assertThatThrownBy(() -> passe(sans, "POST"))
                .isInstanceOf(LicenseReadOnlyException.class)
                .satisfies(e -> assertThat(((LicenseReadOnlyException) e).getStatus())
                        .isEqualTo(LicenseStatus.MISSING));
        assertThat(passe(sans, "GET")).isTrue();
        assertThat(passe(sans, "HEAD")).isTrue();

        var l = TestLicenses.license(UUID.randomUUID(), "PRO", Set.of("*"), 0);
        LicenseWriteGuard echue = guard(TestLicenses.onPrem(l, TestLicenses.EXPIRES.plus(Duration.ofDays(31))));
        assertThatThrownBy(() -> passe(echue, "PUT")).isInstanceOf(LicenseReadOnlyException.class)
                .hasMessageContaining("EXPIRED");
    }

    @Test
    void uneRequeteSansControleurNestPasConcernee() {
        LicenseWriteGuard sans = guard(TestLicenses.unlicensed(TestLicenses.ISSUED));
        assertThat(sans.preHandle(new MockHttpServletRequest("POST", "/x"), new MockHttpServletResponse(),
                new Object())).isTrue();
    }
}
