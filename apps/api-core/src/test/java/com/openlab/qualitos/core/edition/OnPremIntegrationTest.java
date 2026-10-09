package com.openlab.qualitos.core.edition;

import com.openlab.qualitos.core.security.TenantContext;
import com.openlab.qualitos.core.tenant.Tenant;
import com.openlab.qualitos.core.tenant.TenantRepository;
import com.openlab.qualitos.licensing.application.Licensing;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * api-core démarré en on-premise avec une licence (ADR 0082) : le client de la
 * licence existe dès le démarrage, avec l'identifiant qu'elle fixe, et
 * {@code /api/v1/edition} dit ce que l'écran doit savoir.
 */
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("test")
@Tag("web")
class OnPremIntegrationTest {

    static final UUID TENANT = UUID.fromString("0d6e7f80-91a2-4b3c-8d4e-5f6a7b8c9d0e");

    @TestConfiguration
    static class Licence {
        @Bean
        @Primary
        Licensing licensing() {
            return TestLicenses.onPrem(TestLicenses.license(TENANT, "ENTERPRISE", Set.of("pdca", "capa"), 40),
                    TestLicenses.ISSUED.plus(Duration.ofDays(30)));
        }
    }

    @Autowired TenantRepository tenants;
    @Autowired EditionController edition;

    @AfterEach
    void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void leClientDeLaLicenceExisteDesLeDemarrage_etLEditionSeLit() {
        Tenant t = tenants.findById(TENANT).orElseThrow();
        assertThat(t.getName()).isEqualTo("Client de test");
        assertThat(t.getPlan()).isEqualTo(Tenant.Plan.ENTERPRISE);

        TenantContext.setTenantId(TENANT.toString());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                UUID.randomUUID().toString(), "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        EditionController.EditionView v = edition.edition();
        assertThat(v.edition()).isEqualTo("ONPREM");
        assertThat(v.licenseStatus()).isEqualTo("VALID");
        assertThat(v.writable()).isTrue();
        assertThat(v.modules()).containsExactly("capa", "pdca");
        assertThat(v.maxUsers()).isEqualTo(40);
        assertThat(v.activeUsers()).isZero();
        assertThat(v.daysLeft()).isEqualTo(335);
    }
}
