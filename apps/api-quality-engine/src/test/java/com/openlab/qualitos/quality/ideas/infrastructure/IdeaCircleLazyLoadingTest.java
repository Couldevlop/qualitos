package com.openlab.qualitos.quality.ideas.infrastructure;

import com.openlab.qualitos.quality.circle.CircleStatus;
import com.openlab.qualitos.quality.circle.QualityCircle;
import com.openlab.qualitos.quality.circle.QualityCircleRepository;
import com.openlab.qualitos.quality.ideas.domain.Idea;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le trou que 5408 bancs Mockito n'ont pas vu.
 *
 * <p>{@code CircleProposal.circle} est {@code LAZY}, {@code open-in-view} est
 * désactivé (voir {@code application.yml}), et {@code IdeaRepositoryAdapter}
 * n'était couvert par aucune transaction : chaque appel Spring Data ouvre puis
 * referme sa propre session, si bien que le mapper lisait
 * {@code p.getCircle().getId()} sur une entité déjà détachée. Aucun banc
 * Mockito ne peut le voir — un dépôt simulé rend l'objet qu'on lui a donné, il
 * ne rejoue ni la session Hibernate ni la frontière transactionnelle.
 *
 * <p>Contexte Spring complet (pas de slice) : le mapping JPA réel, le
 * {@code open-in-view: false} de production, et une vraie séquence
 * enregistrer-puis-relire dans deux appels distincts — donc deux transactions
 * distinctes, exactement comme {@code GET /api/v1/ideas} le fait en
 * production. Un slice {@code @DataJpaTest} n'existe pas encore dans ce
 * module ; cette classe suit l'idiome du contexte complet déjà posé par
 * {@code QualityEngineContextLoadsTest} (H2 {@code MODE=PostgreSQL},
 * {@code application-test.yml}) plutôt que d'en inventer un second.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
class IdeaCircleLazyLoadingTest {

    @Autowired
    private IdeaRepositoryAdapter adapter;

    @Autowired
    private QualityCircleRepository circles;

    @Test
    void uneIdeeRattacheeAUnCercleRendSonCircleIdSansExploser() {
        UUID tenant = UUID.randomUUID();

        QualityCircle circle = new QualityCircle();
        circle.setTenantId(tenant);
        circle.setName("Cercle atelier");
        circle.setStatus(CircleStatus.ACTIVE);
        circle = circles.save(circle);
        UUID circleId = circle.getId();

        // Un appel Spring Data à part : sa transaction se referme avant de
        // rendre la main, exactement comme en production.
        Idea deposee = Idea.submitted(tenant, circleId, "Éclairage LED atelier",
                null, UUID.randomUUID(), "M. Alaoui", Instant.now());
        adapter.save(deposee);

        // Second appel, dans une SESSION NEUVE : la ligne relue porte un
        // proxy LAZY sur son cercle. Sans transaction couvrant le mapping,
        // et sans le graphe d'entité chargeant le cercle, ceci lève
        // LazyInitializationException — c'était le bug de GET /api/v1/ideas
        // sur tout tenant portant une idée héritée d'un cercle (V86).
        List<Idea> tableau = adapter.findByTenant(tenant);

        assertThat(tableau).hasSize(1);
        assertThat(tableau.get(0).getCircleId()).isEqualTo(circleId);
    }

    @Test
    void uneIdeeRattacheeAUnCercleSeRetrouveAussiParSonIdentifiant() {
        UUID tenant = UUID.randomUUID();

        QualityCircle circle = new QualityCircle();
        circle.setTenantId(tenant);
        circle.setName("Cercle logistique");
        circle.setStatus(CircleStatus.ACTIVE);
        circle = circles.save(circle);
        UUID circleId = circle.getId();

        Idea deposee = Idea.submitted(tenant, circleId, "Bac de tri recyclage",
                null, UUID.randomUUID(), "M. Alaoui", Instant.now());
        Idea sauvee = adapter.save(deposee);

        Idea relue = adapter.findByIdAndTenant(sauvee.getId(), tenant).orElseThrow();

        assertThat(relue.getCircleId()).isEqualTo(circleId);
    }
}
