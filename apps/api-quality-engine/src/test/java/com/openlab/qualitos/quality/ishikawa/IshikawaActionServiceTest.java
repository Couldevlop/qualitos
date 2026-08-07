package com.openlab.qualitos.quality.ishikawa;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Plan d'actions d'un diagramme Ishikawa.
 *
 * <p>Un diagramme sans suite ne sert à rien : identifier les causes est un moyen,
 * décider qui fait quoi et pour quand est la fin. Jusqu'ici l'écran s'arrêtait aux
 * causes, et les décisions prises en réunion vivaient ailleurs — dans un compte
 * rendu, un tableur, une mémoire.
 *
 * <p>Ces actions ne sont PAS des actions CAPA. Une CAPA est un dossier formel, avec
 * son instruction et sa preuve d'efficacité ; obliger à en ouvrir un pour noter
 * « refaire le réglage de la butée, Karim, vendredi » découragerait la saisie et
 * remplirait le registre CAPA de broutilles. L'escalade vers une CAPA reste
 * possible — c'est un lien, pas une contrainte (§3.6).
 */
@ExtendWith(MockitoExtension.class)
class IshikawaActionServiceTest {

    @Mock IshikawaActionRepository actionRepo;
    @Mock IshikawaDiagramRepository diagramRepo;
    @InjectMocks IshikawaActionService service;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final UUID DIAGRAM = UUID.randomUUID();

    @BeforeEach void ctx() { TenantContext.setTenantId(TENANT.toString()); }
    @AfterEach  void clr() { TenantContext.clear(); }

    private IshikawaDiagram diagram(UUID tenant) {
        IshikawaDiagram d = new IshikawaDiagram();
        d.setId(DIAGRAM);
        d.setTenantId(tenant);
        d.setProblemStatement("Rebuts en hausse sur la ligne 2");
        d.setMode(IshikawaMode.SIX_M);
        d.setStatus(IshikawaStatus.DRAFT);
        return d;
    }

    private IshikawaAction action(String label) {
        IshikawaAction a = new IshikawaAction();
        a.setId(UUID.randomUUID());
        a.setTenantId(TENANT);
        a.setDiagram(diagram(TENANT));
        a.setLabel(label);
        a.setStatus(IshikawaActionStatus.TODO);
        return a;
    }

    // ---- création ------------------------------------------------------------

    @Test
    @DisplayName("une action décidée est rattachée au diagramme, à faire par défaut")
    void createsAnActionOnTheDiagram() {
        when(diagramRepo.findByIdAndTenantId(DIAGRAM, TENANT))
                .thenReturn(Optional.of(diagram(TENANT)));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalDate decidedOn = LocalDate.of(2026, 8, 6);
        IshikawaDto.ActionResponse created = service.create(DIAGRAM,
                new IshikawaDto.CreateActionRequest(
                        "Refaire le réglage de la butée", "Karim", decidedOn, null));

        ArgumentCaptor<IshikawaAction> saved = ArgumentCaptor.forClass(IshikawaAction.class);
        verify(actionRepo).save(saved.capture());
        assertThat(saved.getValue().getLabel()).isEqualTo("Refaire le réglage de la butée");
        assertThat(saved.getValue().getResponsible()).isEqualTo("Karim");
        assertThat(saved.getValue().getDecidedOn()).isEqualTo(decidedOn);
        assertThat(saved.getValue().getTenantId()).isEqualTo(TENANT);
        assertThat(created.status()).isEqualTo(IshikawaActionStatus.TODO);
    }

    @Test
    @DisplayName("le statut demandé à la création est respecté")
    void honoursTheRequestedStatus() {
        when(diagramRepo.findByIdAndTenantId(DIAGRAM, TENANT))
                .thenReturn(Optional.of(diagram(TENANT)));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IshikawaDto.ActionResponse created = service.create(DIAGRAM,
                new IshikawaDto.CreateActionRequest("Contrôler le lot", null, null,
                        IshikawaActionStatus.IN_PROGRESS));

        assertThat(created.status()).isEqualTo(IshikawaActionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("on n'ajoute pas d'action au diagramme d'un autre tenant")
    void refusesAnotherTenantsDiagram() {
        when(diagramRepo.findByIdAndTenantId(DIAGRAM, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(DIAGRAM,
                new IshikawaDto.CreateActionRequest("x", null, null, null)))
                .isInstanceOf(IshikawaDiagramNotFoundException.class);
        verifyNoInteractions(actionRepo);
    }

    @Test
    @DisplayName("sans tenant, rien n'est écrit")
    void refusesWithoutTenant() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.create(DIAGRAM,
                new IshikawaDto.CreateActionRequest("x", null, null, null)))
                .isInstanceOf(MissingTenantContextException.class);
        verifyNoInteractions(actionRepo);
    }

    // ---- modification en ligne ------------------------------------------------

    @Test
    @DisplayName("le libellé se modifie sans toucher au reste")
    void renamesTheAction() {
        IshikawaAction existing = action("Ancien libellé");
        existing.setResponsible("Karim");
        when(actionRepo.findByIdAndTenantId(existing.getId(), TENANT))
                .thenReturn(Optional.of(existing));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(existing.getId(),
                new IshikawaDto.UpdateActionRequest("Nouveau libellé", null, null, null));

        assertThat(existing.getLabel()).isEqualTo("Nouveau libellé");
        // Ce qui n'est pas transmis ne doit pas être effacé : l'édition se fait
        // cellule par cellule, un champ absent signifie « inchangé ».
        assertThat(existing.getResponsible()).isEqualTo("Karim");
    }

    @Test
    @DisplayName("le statut se change seul, depuis la liste déroulante")
    void changesOnlyTheStatus() {
        IshikawaAction existing = action("Contrôler le lot");
        when(actionRepo.findByIdAndTenantId(existing.getId(), TENANT))
                .thenReturn(Optional.of(existing));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(existing.getId(), new IshikawaDto.UpdateActionRequest(
                null, null, null, IshikawaActionStatus.DONE));

        assertThat(existing.getStatus()).isEqualTo(IshikawaActionStatus.DONE);
        assertThat(existing.getLabel()).isEqualTo("Contrôler le lot");
    }

    @Test
    @DisplayName("un libellé vide est refusé")
    void refusesAnEmptyLabel() {
        IshikawaAction existing = action("Contrôler le lot");
        when(actionRepo.findByIdAndTenantId(existing.getId(), TENANT))
                .thenReturn(Optional.of(existing));

        // Une cellule vidée par mégarde ne doit pas effacer l'action de la liste
        // en n'y laissant qu'une ligne muette.
        assertThatThrownBy(() -> service.update(existing.getId(),
                new IshikawaDto.UpdateActionRequest("   ", null, null, null)))
                .isInstanceOf(IshikawaStateException.class);
    }

    @Test
    @DisplayName("on ne modifie pas l'action d'un autre tenant")
    void refusesToUpdateAnotherTenantsAction() {
        UUID id = UUID.randomUUID();
        when(actionRepo.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id,
                new IshikawaDto.UpdateActionRequest("x", null, null, null)))
                .isInstanceOf(IshikawaActionNotFoundException.class);
    }

    // ---- lecture & suppression -------------------------------------------------

    @Test
    @DisplayName("les actions d'un diagramme sont listées dans l'ordre de décision")
    void listsTheDiagramActions() {
        when(diagramRepo.findByIdAndTenantId(DIAGRAM, TENANT))
                .thenReturn(Optional.of(diagram(TENANT)));
        when(actionRepo.findByDiagramIdAndTenantIdOrderByCreatedAtAsc(DIAGRAM, TENANT))
                .thenReturn(List.of(action("A"), action("B")));

        List<IshikawaDto.ActionResponse> actions = service.list(DIAGRAM);

        assertThat(actions).extracting(IshikawaDto.ActionResponse::label)
                .containsExactly("A", "B");
    }

    @Test
    @DisplayName("supprimer une action ne touche pas au diagramme")
    void deletesAnAction() {
        IshikawaAction existing = action("À supprimer");
        when(actionRepo.findByIdAndTenantId(existing.getId(), TENANT))
                .thenReturn(Optional.of(existing));

        service.delete(existing.getId());

        verify(actionRepo).delete(existing);
        verifyNoInteractions(diagramRepo);
    }
}
