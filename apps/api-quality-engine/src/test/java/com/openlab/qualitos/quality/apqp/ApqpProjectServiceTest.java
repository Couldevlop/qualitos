package com.openlab.qualitos.quality.apqp;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Les projets APQP d'un client.
 *
 * <p>Ce que ce banc protège : le client vient du JETON et jamais du corps, un
 * projet naît avec son cycle, et la liste porte l'avancement de chaque programme
 * sans un aller-retour par ligne.
 */
@ExtendWith(MockitoExtension.class)
class ApqpProjectServiceTest {

    @Mock ApqpProjectRepository projets;
    @Mock ApqpPhaseRepository phases;
    @Mock ApqpCycleSeeder seeder;
    ApqpProjectService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID PROJET_ID = UUID.randomUUID();
    static final UUID ACTEUR = UUID.randomUUID();

    @BeforeEach
    void poserLeTenant() {
        TenantContext.setTenantId(TENANT.toString());
        service = new ApqpProjectService(projets, phases, seeder);
    }

    @AfterEach
    void rendreLeTenant() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("le client vient du jeton, l'acteur aussi — jamais du corps")
    void leClientEtLActeurViennentDuJeton() {
        when(projets.save(any())).thenAnswer(appel -> appel.getArgument(0));
        when(phases.avancementParProjet(TENANT)).thenReturn(List.of());

        service.creer(new ApqpDto.CreateProjectRequest(
                "  Support moteur 2027  ", ApqpProjectType.NPI, "  Client A  ", null, null),
                ACTEUR);

        ArgumentCaptor<ApqpProject> capture = ArgumentCaptor.forClass(ApqpProject.class);
        verify(projets).save(capture.capture());
        assertThat(capture.getValue().getTenantId()).isEqualTo(TENANT);
        assertThat(capture.getValue().getCreatedBy()).isEqualTo(ACTEUR);
        assertThat(capture.getValue().getName()).isEqualTo("Support moteur 2027");
        assertThat(capture.getValue().getCustomer()).isEqualTo("Client A");
    }

    @Test
    @DisplayName("un projet naît avec le cycle du référentiel")
    void unProjetNaitRempli() {
        ApqpProject enregistre = projet();
        when(projets.save(any())).thenReturn(enregistre);
        when(phases.avancementParProjet(TENANT)).thenReturn(List.of());

        service.creer(new ApqpDto.CreateProjectRequest(
                "Support moteur", ApqpProjectType.TOW, null, null, null), ACTEUR);

        verify(seeder).amorcer(enregistre);
    }

    @Test
    @DisplayName("la liste porte l'avancement de chaque projet, en une requête")
    void laListePorteLAvancement() {
        when(projets.findByTenantIdOrderByCreatedAtDesc(TENANT)).thenReturn(List.of(projet()));
        when(phases.avancementParProjet(TENANT)).thenReturn(
                List.<Object[]>of(new Object[] {PROJET_ID, 48L, 6L, 12L, 2L}));

        List<ApqpDto.ProjectResponse> liste = service.lister();

        assertThat(liste).singleElement().satisfies(p -> {
            assertThat(p.deliverablesTotal()).isEqualTo(48);
            assertThat(p.deliverablesDone()).isEqualTo(6);
            assertThat(p.ppapTotal()).isEqualTo(12);
            assertThat(p.ppapDone()).isEqualTo(2);
        });
    }

    @Test
    @DisplayName("un projet sans aucune phase affiche zéro, pas une erreur")
    void unProjetSansPhaseAfficheZero() {
        when(projets.findByTenantIdOrderByCreatedAtDesc(TENANT)).thenReturn(List.of(projet()));
        when(phases.avancementParProjet(TENANT)).thenReturn(List.of());

        assertThat(service.lister()).singleElement()
                .satisfies(p -> assertThat(p.deliverablesTotal()).isZero());
    }

    @Test
    @DisplayName("le projet d'un autre client est introuvable, et rien n'est supprimé")
    void leProjetDUnAutreClientEstIntrouvable() {
        when(projets.findByIdAndTenantId(PROJET_ID, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.supprimer(PROJET_ID))
                .isInstanceOf(ApqpProjectNotFoundException.class);
        verify(projets, never()).delete(any());
    }

    @Test
    @DisplayName("sans tenant au contexte, rien ne se liste")
    void sansTenantRienNeSeListe() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.lister())
                .isInstanceOf(MissingTenantContextException.class);
    }

    private ApqpProject projet() {
        ApqpProject projet = new ApqpProject();
        projet.setId(PROJET_ID);
        projet.setTenantId(TENANT);
        projet.setName("Support moteur 2027");
        projet.setType(ApqpProjectType.NPI);
        return projet;
    }
}
