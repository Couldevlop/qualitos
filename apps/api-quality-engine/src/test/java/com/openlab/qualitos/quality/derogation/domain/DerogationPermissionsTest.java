package com.openlab.qualitos.quality.derogation.domain;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.domain.SystemRole;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La cohérence du jeu de droits de la dérogation.
 *
 * <p>Ce banc existe pour une classe de défaut précise, qu'aucun test de domaine
 * ne verrait : un droit accordé pour <i>ouvrir</i> un dossier mais refusé pour le
 * geste suivant produit un <b>cul-de-sac</b>. L'agrégat n'accepte que
 * l'initiateur ; si le rôle de l'initiateur n'a pas le droit d'envoyer, le
 * dossier n'est envoyable par <i>personne</i> — ni par lui, faute de droit, ni
 * par le pilotage, que l'agrégat refuse. Rien ne lève à la compilation, rien ne
 * lève aux tests unitaires : on ne s'en aperçoit qu'en recette.
 */
class DerogationPermissionsTest {

    /** Tous les gestes qu'un initiateur doit pouvoir enchaîner sur SON dossier. */
    private static final Set<Permission> LA_SUITE_DE_LINITIATEUR = Set.of(
            Permission.DEROGATION_EDIT,
            Permission.DEROGATION_REQUEST_APPROVAL,
            Permission.DEROGATION_SEND,
            Permission.DEROGATION_DECIDE,
            Permission.DEROGATION_REOPEN);

    @Test
    void quiPeutOuvrirUnDossierPeutLeMenerAuBout() {
        for (SystemRole role : SystemRole.values()) {
            if (!Permission.DEROGATION_CREATE.grantedByDefaultTo(role)) {
                continue;
            }
            for (Permission suite : LA_SUITE_DE_LINITIATEUR) {
                assertThat(suite.grantedByDefaultTo(role))
                        .as("%s peut ouvrir un dossier : il doit pouvoir %s,"
                                + " sinon son dossier est bloque pour tout le monde"
                                + " (l'agregat n'accepte que l'initiateur)",
                                role, suite.code())
                        .isTrue();
            }
        }
    }

    @Test
    void quiPeutSignerPeutLireLeDossierQuilSigne() {
        // « Mes validations » montre le compteur et la liste a qui peut signer :
        // se faire refuser la FICHE ensuite serait une impasse a l'ecran.
        for (SystemRole role : SystemRole.values()) {
            if (!Permission.DEROGATION_APPROVE.grantedByDefaultTo(role)) {
                continue;
            }
            assertThat(Permission.DEROGATION_VIEW.grantedByDefaultTo(role))
                    .as("%s peut signer : il doit pouvoir lire la fiche", role)
                    .isTrue();
        }
    }

    @Test
    void lesHuitDroitsSontRangesDansLeMemeModule() {
        // Le module nomme l'onglet de la console des droits : un intrus s'y
        // afficherait sous une autre rubrique.
        assertThat(java.util.Arrays.stream(Permission.values())
                .filter(p -> p.name().startsWith("DEROGATION_"))
                .map(Permission::module)
                .distinct())
                .containsExactly("derogation");
    }

    @Test
    void lesCodesSontStablesEtPrefixes() {
        // Le code est ce que la base enregistre : le renommer demanderait une
        // migration des droits deja accordes.
        assertThat(Permission.DEROGATION_VIEW.code()).isEqualTo("derogation.view");
        assertThat(Permission.DEROGATION_VIEW_ALL.code()).isEqualTo("derogation.view.all");
        assertThat(Permission.DEROGATION_CREATE.code()).isEqualTo("derogation.create");
        assertThat(Permission.DEROGATION_EDIT.code()).isEqualTo("derogation.edit");
        assertThat(Permission.DEROGATION_REQUEST_APPROVAL.code())
                .isEqualTo("derogation.request.approval");
        assertThat(Permission.DEROGATION_APPROVE.code()).isEqualTo("derogation.approve");
        assertThat(Permission.DEROGATION_REOPEN.code()).isEqualTo("derogation.reopen");
        assertThat(Permission.DEROGATION_SEND.code()).isEqualTo("derogation.send");
        assertThat(Permission.DEROGATION_DECIDE.code()).isEqualTo("derogation.decide");
    }
}
