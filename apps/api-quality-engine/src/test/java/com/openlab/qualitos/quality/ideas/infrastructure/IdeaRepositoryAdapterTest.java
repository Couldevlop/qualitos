package com.openlab.qualitos.quality.ideas.infrastructure;

import com.openlab.qualitos.quality.circle.CircleProposal;
import com.openlab.qualitos.quality.circle.QualityCircle;
import com.openlab.qualitos.quality.circle.QualityCircleRepository;
import com.openlab.qualitos.quality.ideas.domain.Idea;
import com.openlab.qualitos.quality.ideas.domain.IdeaNotFoundException;
import com.openlab.qualitos.quality.ideas.domain.IdeaStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L'adaptateur, sans base réelle — pour les règles qui ne dépendent pas
 * d'une session Hibernate. Le comportement face à `circle` LAZY (C1) est,
 * lui, vérifié sur un vrai moteur par {@link IdeaCircleLazyLoadingTest} : un
 * dépôt simulé rend l'objet qu'on lui a donné et ne peut pas voir un proxy
 * qui explose.
 */
@ExtendWith(MockitoExtension.class)
class IdeaRepositoryAdapterTest {

    @Mock IdeaJpaRepository jpa;
    @Mock QualityCircleRepository circles;

    IdeaRepositoryAdapter adapter;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID AUTEUR = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-09-09T08:00:00Z");

    @BeforeEach
    void setup() {
        adapter = new IdeaRepositoryAdapter(jpa, circles);
    }

    private Idea idee() {
        return Idea.submitted(TENANT, null, "Bac de tri", null, AUTEUR, "M. Alaoui", NOW);
    }

    /**
     * C4 — une transition sur une idée disparue entre-temps (identifiant
     * fourni, ligne introuvable) doit rendre 404, pas ressusciter une ligne
     * neuve sous l'ancien identifiant.
     */
    @Test
    void save_idFourniIntrouvable_leveIdeaNotFound() {
        Idea idee = idee();
        idee.assignId(UUID.randomUUID());
        when(jpa.findByIdAndTenantId(idee.getId(), TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.save(idee))
                .isInstanceOf(IdeaNotFoundException.class);

        verify(jpa, never()).save(any());
    }

    @Test
    void save_ideeNeuve_neCherchePasDeLigneExistante() {
        Idea idee = idee();
        when(jpa.save(any())).thenAnswer(inv -> {
            CircleProposal e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        Idea sauvee = adapter.save(idee);

        assertThat(sauvee.getId()).isNotNull();
        verify(jpa, never()).findByIdAndTenantId(any(), any());
    }

    @Test
    void save_ideeExistante_reutiliseLaLigne() {
        Idea idee = idee();
        idee.assignId(UUID.randomUUID());
        CircleProposal ligne = new CircleProposal();
        ligne.setId(idee.getId());
        ligne.setTenantId(TENANT);
        when(jpa.findByIdAndTenantId(idee.getId(), TENANT)).thenReturn(Optional.of(ligne));
        when(jpa.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Idea sauvee = adapter.save(idee);

        assertThat(sauvee.getId()).isEqualTo(idee.getId());
        verify(jpa).findByIdAndTenantId(idee.getId(), TENANT);
    }

    @Test
    void save_cercleDUnAutreTenant_refuse() {
        UUID circleId = UUID.randomUUID();
        Idea idee = Idea.submitted(TENANT, circleId, "Bac de tri", null, AUTEUR, "M. Alaoui", NOW);
        QualityCircle circleAutreTenant = new QualityCircle();
        circleAutreTenant.setId(circleId);
        circleAutreTenant.setTenantId(UUID.randomUUID());
        when(circles.findById(circleId)).thenReturn(Optional.of(circleAutreTenant));

        assertThatThrownBy(() -> adapter.save(idee))
                .isInstanceOf(IdeaStateException.class);

        verify(jpa, never()).save(any());
    }
}
