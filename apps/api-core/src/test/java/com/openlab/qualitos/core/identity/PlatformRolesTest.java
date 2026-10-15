package com.openlab.qualitos.core.identity;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Une seule écriture des rôles : celle du realm (V9). */
class PlatformRolesTest {

    @Test
    void laSaisieEstRameneeALaFormeDuRealm() {
        assertThat(PlatformRoles.validated(List.of("ADMIN_TENANT", " Quality_Manager ", "admin", "DIRECTOR_QUALITY")))
                .containsExactly("admin_tenant", "quality_director", "quality_manager");
    }

    @Test
    void superAdminEstConnuMaisPasAttribuable() {
        assertThat(PlatformRoles.canonical("SUPER_ADMIN")).contains("super_admin");
        assertThatThrownBy(() -> PlatformRoles.validated(List.of("super_admin")))
                .isInstanceOf(InvalidRoleException.class);
        assertThatThrownBy(() -> PlatformRoles.validated(List.of("pilote")))
                .isInstanceOf(InvalidRoleException.class);
        assertThatThrownBy(() -> PlatformRoles.validated(List.of()))
                .isInstanceOf(InvalidRoleException.class);
        assertThatThrownBy(() -> PlatformRoles.validated(Arrays.asList("user", null)))
                .isInstanceOf(InvalidRoleException.class);
    }

    @Test
    void lesRolesDuJetonHorsPlateformeSontIgnores() {
        assertThat(PlatformRoles.fromToken(List.of("user", "offline_access", "uma_authorization",
                "default-roles-qualitos", "super_admin", "QUALITY_DIRECTOR")))
                .containsExactly("quality_director", "super_admin", "user");
        assertThat(PlatformRoles.fromToken(null)).isEmpty();
        assertThat(PlatformRoles.canonical(null)).isEmpty();
    }

    @Test
    void chaqueRoleConnuEstUnRoleDuRealm() {
        assertThat(PlatformRoles.KNOWN).containsExactlyInAnyOrderElementsOf(Set.of(
                "super_admin", "admin_tenant", "quality_director", "quality_manager", "auditor", "user",
                "external_auditor"));
    }
}
