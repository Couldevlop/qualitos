package com.openlab.qualitos.quality.authz.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthzDomainTest {

    @Test
    void lesRolesDuJetonSeLisentSousTousLeursNoms() {
        assertThat(SystemRole.fromTokenRole("quality_manager")).contains(SystemRole.QUALITY_MANAGER);
        assertThat(SystemRole.fromTokenRole("ROLE_DIRECTOR_QUALITY")).contains(SystemRole.QUALITY_DIRECTOR);
        assertThat(SystemRole.fromTokenRole("ROLE_ADMIN")).contains(SystemRole.ADMIN_TENANT);
        assertThat(SystemRole.fromTokenRole(" tenant_admin ")).contains(SystemRole.ADMIN_TENANT);
        assertThat(SystemRole.fromTokenRole("offline_access")).isEmpty();
        assertThat(SystemRole.fromTokenRole(null)).isEmpty();
        assertThat(SystemRole.SUPER_ADMIN.tenantManaged()).isFalse();
        assertThat(SystemRole.USER.tenantManaged()).isTrue();
    }

    @Test
    void lesDroitsLivresSuiventLeTableauDesRoles() {
        // L'utilisateur terrain déclare et fait avancer ce qu'on lui confie.
        TenantRole user = TenantRole.defaultsOf(SystemRole.USER);
        assertThat(user.grants(Permission.NC_CREATE)).isTrue();
        assertThat(user.grants(Permission.CAPA_ACTION_UPDATE)).isTrue();
        assertThat(user.grants(Permission.CAPA_CREATE)).isFalse();
        assertThat(user.grants(Permission.NC_CLOSE)).isFalse();
        // L'auditeur constate et vérifie, ne pilote pas.
        TenantRole auditeur = TenantRole.defaultsOf(SystemRole.AUDITOR);
        assertThat(auditeur.grants(Permission.CAPA_VERIFY)).isTrue();
        assertThat(auditeur.grants(Permission.DOCUMENT_APPROVE)).isFalse();
        // L'administrateur du client administre les droits, le manager non.
        assertThat(TenantRole.defaultsOf(SystemRole.ADMIN_TENANT).grants(Permission.AUTHZ_MANAGE)).isTrue();
        assertThat(TenantRole.defaultsOf(SystemRole.QUALITY_MANAGER).grants(Permission.AUTHZ_MANAGE)).isFalse();
        // L'éditeur a tout.
        assertThat(TenantRole.defaultsOf(SystemRole.SUPER_ADMIN).permissions())
                .containsExactlyInAnyOrder(Permission.values());
        // L'auditeur externe acquitte ses lectures ; comme chacun, il voit tout le
        // registre de chaque module tant que le client ne le restreint pas (ADR 0081).
        assertThat(TenantRole.defaultsOf(SystemRole.EXTERNAL_AUDITOR).permissions())
                .containsExactlyInAnyOrder(Permission.DOCUMENT_ACKNOWLEDGE, Permission.NC_VIEW_ALL,
                        Permission.CAPA_VIEW_ALL, Permission.RISK_VIEW_ALL);
    }

    @Test
    void chaqueActionAUnCodeStableEtUnModule() {
        assertThat(Arrays.stream(Permission.values()).map(Permission::code))
                .doesNotHaveDuplicates()
                .allMatch(c -> c.matches("^[a-z][a-z0-9.]{1,79}$"));
        assertThat(Permission.fromCode("capa.close")).isEmpty();
        assertThat(Permission.fromCode("nc.close")).contains(Permission.NC_CLOSE);
        assertThat(Permission.CAPA_CREATE.module()).isEqualTo("capa");
    }

    @Test
    void unRoleSurMesureAUnCodeLibreEtUnNom() {
        TenantRole r = TenantRole.custom(" pilote_site ", " Pilote de site ", null,
                List.of(Permission.NC_CLOSE));
        assertThat(r.code()).isEqualTo("PILOTE_SITE");
        assertThat(r.name()).isEqualTo("Pilote de site");
        assertThat(r.system()).isFalse();

        assertThatThrownBy(() -> TenantRole.custom("x", "X", null, List.of()))
                .extracting("field").isEqualTo("code");
        assertThatThrownBy(() -> TenantRole.custom("9AB", "X", null, List.of()))
                .extracting("field").isEqualTo("code");
        assertThatThrownBy(() -> TenantRole.custom("ADMIN", "X", null, List.of()))
                .extracting("field").isEqualTo("code");
        assertThatThrownBy(() -> TenantRole.custom("PILOTE", "  ", null, List.of()))
                .extracting("field").isEqualTo("name");
        assertThatThrownBy(() -> TenantRole.custom("PILOTE", "x".repeat(TenantRole.NAME_MAX + 1), null, List.of()))
                .extracting("field").isEqualTo("name");
        assertThatThrownBy(() -> TenantRole.custom("PILOTE", "P", "d".repeat(TenantRole.DESCRIPTION_MAX + 1),
                List.of())).extracting("field").isEqualTo("description");
    }

    @Test
    void lAdministrateurGardeToujoursLaMainSurLesDroits() {
        assertThatThrownBy(() -> TenantRole.system(SystemRole.ADMIN_TENANT, null, null, List.of(Permission.NC_CREATE)))
                .isInstanceOf(AuthzValidationException.class)
                .extracting("field").isEqualTo("permissions");
        TenantRole admin = TenantRole.defaultsOf(SystemRole.ADMIN_TENANT);
        assertThatThrownBy(() -> admin.withPermissions(EnumSet.noneOf(Permission.class)))
                .isInstanceOf(AuthzValidationException.class);
        // Un autre rôle peut tout perdre.
        assertThat(TenantRole.defaultsOf(SystemRole.USER).withPermissions(List.of()).permissions()).isEmpty();
    }

    @Test
    void lesReglagesDuClientSePosentSurLesDefauts() {
        TenantRole userRestreint = TenantRole.system(SystemRole.USER, "Opérateur", null, List.of(Permission.NC_PHOTO));
        TenantRole pilote = TenantRole.custom("PILOTE_SITE", "Pilote de site", null, List.of(Permission.NC_CLOSE));
        TenantRole superAdminForge = TenantRole.custom("ZZZ", "x", null, List.of());

        TenantRoles roles = TenantRoles.of(List.of(pilote, userRestreint, superAdminForge));

        assertThat(roles.find("USER").orElseThrow().name()).isEqualTo("Opérateur");
        assertThat(roles.effective(Set.of("USER"))).containsExactly(Permission.NC_PHOTO);
        assertThat(roles.effective(Set.of("USER", "PILOTE_SITE")))
                .containsExactlyInAnyOrder(Permission.NC_PHOTO, Permission.NC_CLOSE);
        // Un rôle système jamais réglé garde ses droits livrés.
        assertThat(roles.effective(Set.of("AUDITOR"))).contains(Permission.CAPA_VERIFY);
        // Un code inconnu n'accorde rien.
        assertThat(roles.effective(Set.of("FANTOME"))).isEmpty();
        // Le super administrateur n'est pas administrable par le client.
        assertThat(roles.manageable()).extracting(TenantRole::code).doesNotContain("SUPER_ADMIN")
                .startsWith("ADMIN_TENANT").contains("PILOTE_SITE", "ZZZ");
        assertThat(roles.find(null)).isEmpty();
    }

    @Test
    void unRoleSeCompareParSonContenu() {
        TenantRole a = TenantRole.custom("PILOTE", "P", null, List.of(Permission.NC_CLOSE));
        TenantRole b = TenantRole.custom("PILOTE", "P", null, List.of(Permission.NC_CLOSE));
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(b.withPermissions(List.of()));
    }
}
