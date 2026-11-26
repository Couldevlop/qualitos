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

    /**
     * LA FONCTION D'UNE PERSONNE, EN TOUTES LETTRES.
     *
     * <p>Aucun champ « fonction » n'existe à la création d'un utilisateur :
     * l'invitation ne porte que le prénom, le nom et les rôles. Le rôle dit
     * donc la fonction, et c'est lui qu'on écrit dans un document remis au
     * client -- « quality_manager » n'y a pas sa place.
     */
    @Test
    void laFonctionSeLitDuRoleLePlusEleve() {
        assertThat(PlatformRoles.fonction(List.of("quality_manager")))
                .contains("Responsable qualité");
        assertThat(PlatformRoles.fonction(List.of("auditor"))).contains("Audit interne");
        // PLUSIEURS ROLES, UNE SEULE FONCTION : la plus haute. Une personne qui
        // dirige la qualite et siege en audit ne s'annonce pas « auditeur ».
        assertThat(PlatformRoles.fonction(List.of("user", "auditor", "quality_director")))
                .contains("Direction qualité");
        // Les formes du jeton sont normalisees comme ailleurs.
        assertThat(PlatformRoles.fonction(List.of("QUALITY_DIRECTOR"))).contains("Direction qualité");
    }

    /** Sans role connu, pas de fonction inventee : la colonne reste au role seul. */
    @Test
    void sansRoleConnuLaFonctionEstVide() {
        assertThat(PlatformRoles.fonction(null)).isEmpty();
        assertThat(PlatformRoles.fonction(List.of())).isEmpty();
        assertThat(PlatformRoles.fonction(List.of("offline_access", "uma_authorization"))).isEmpty();
    }

    @Test
    void chaqueRoleConnuEstUnRoleDuRealm() {
        assertThat(PlatformRoles.KNOWN).containsExactlyInAnyOrderElementsOf(Set.of(
                "super_admin", "admin_tenant", "quality_director", "quality_manager", "auditor", "user",
                "external_auditor",
                // Le cabinet partenaire (ADR 0106) : un consultant n'appartient a
                // aucun client et n'a pas de `tenant_id`. Le role existe dans
                // `infra/keycloak/realm-export.json` ; cette liste en est la copie,
                // et c'est tout l'objet du banc -- ajouter un role sans le declarer
                // au royaume le rendrait inattribuable.
                "consultant"));
    }
}
