package com.openlab.qualitos.quality.apqp;

import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le cycle APQP d'un client.
 *
 * <p>Deux exigences se croisent. Le cycle est AMORCÉ depuis le manuel AIAG mais
 * appartient ensuite au client : renommer une phase, en retirer une, en ajouter
 * une sixième. Et la forme en V doit tenir quel que soit le nombre de phases —
 * c'était la condition pour qu'on puisse en ajouter, et rien d'autre dans
 * l'écran ne le vérifierait.
 */
@ExtendWith(MockitoExtension.class)
class ApqpServiceTest {

    @Mock ApqpPhaseRepository repository;
    ApqpService service;

    static final UUID TENANT = UUID.randomUUID();

    @BeforeEach
    void poserLeTenant() {
        TenantContext.setTenantId(TENANT.toString());
        service = new ApqpService(repository);
    }

    @AfterEach
    void retirerLeTenant() {
        TenantContext.clear();
    }

    // ---------- la forme du V ----------

    @Test
    @DisplayName("le V garde sa forme quel que soit le nombre de phases")
    void niveau_dessineLeV() {
        // La règle qui rend l'ajout possible : on descend jusqu'au milieu, puis
        // on remonte. Sans elle, une sixième phase aurait cassé le schéma.
        assertThat(niveaux(5)).containsExactly(1, 2, 3, 2, 1);
        assertThat(niveaux(6)).containsExactly(1, 2, 3, 3, 2, 1);
        assertThat(niveaux(4)).containsExactly(1, 2, 2, 1);
        assertThat(niveaux(1)).containsExactly(1);
    }

    // ---------- amorçage ----------

    @Test
    @DisplayName("la première lecture amorce le cycle depuis le référentiel AIAG")
    void cycle_amorceALaPremiereLecture() {
        when(repository.existsByTenantId(TENANT)).thenReturn(false);
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(List.of());

        service.cycle();

        var capture = org.mockito.ArgumentCaptor.forClass(Iterable.class);
        verify(repository).saveAll(capture.capture());

        @SuppressWarnings("unchecked")
        List<ApqpPhase> amorcees = new ArrayList<>();
        ((Iterable<ApqpPhase>) capture.getValue()).forEach(amorcees::add);
        assertThat(amorcees).hasSize(5);
        assertThat(amorcees.get(0).getTitle()).isEqualTo("Planifier et définir");
        assertThat(amorcees.get(4).getTitle()).contains("Production série");
        // Les livrables viennent avec : un cycle sans eux n'aurait rien à dire.
        assertThat(amorcees.get(0).getDeliverables()).hasSize(12);
        // Le tenant est porté par CHAQUE livrable, pas seulement par la phase :
        // une ligne sans client échapperait au cloisonnement.
        assertThat(amorcees.get(0).getDeliverables().get(0).getTenantId()).isEqualTo(TENANT);
    }

    @Test
    @DisplayName("la deuxième lecture ne réamorce pas")
    void cycle_neReamorcePas() {
        when(repository.existsByTenantId(TENANT)).thenReturn(true);
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(cycleDe(5));

        List<ApqpDto.PhaseResponse> cycle = service.cycle();

        // Sans cette garde, chaque ouverture aurait rajouté cinq phases.
        verify(repository, never()).saveAll(anyList());
        assertThat(cycle).hasSize(5);
        assertThat(cycle.stream().map(ApqpDto.PhaseResponse::level))
                .containsExactly(1, 2, 3, 2, 1);
    }

    // ---------- phases ----------

    @Test
    @DisplayName("une phase ajoutée se place à la suite du cycle")
    void creerPhase_sePlaceALaSuite() {
        when(repository.existsByTenantId(TENANT)).thenReturn(true);
        when(repository.findFirstByTenantIdOrderByPositionDesc(TENANT))
                .thenReturn(Optional.of(phase(5, "Production série")));
        when(repository.save(any(ApqpPhase.class))).thenAnswer(i -> i.getArgument(0));

        ApqpDto.PhaseResponse ajoutee = service.creerPhase(
                new ApqpDto.CreatePhaseRequest("  Industrialisation  ", "  ", null));

        assertThat(ajoutee.position()).isEqualTo(6);
        assertThat(ajoutee.title()).isEqualTo("Industrialisation");
        // Un objet réduit à des espaces n'est pas un objet : on stocke l'absence,
        // pas une chaîne vide qui s'afficherait comme une ligne blanche.
        assertThat(ajoutee.purpose()).isNull();
    }

    @Test
    @DisplayName("créer sur un cycle vierge l'amorce d'abord")
    void creerPhase_amorceAvant() {
        when(repository.existsByTenantId(TENANT)).thenReturn(false);
        when(repository.findFirstByTenantIdOrderByPositionDesc(TENANT)).thenReturn(Optional.empty());
        when(repository.save(any(ApqpPhase.class))).thenAnswer(i -> i.getArgument(0));

        service.creerPhase(new ApqpDto.CreatePhaseRequest("Industrialisation", null, null));

        // Sinon la phase créée à la main aurait été suivie, à la lecture
        // suivante, des cinq phases d'amorçage posées par-dessus.
        verify(repository).saveAll(anyList());
    }

    @Test
    @DisplayName("supprimer une phase resserre les rangs")
    void supprimerPhase_resserreLesRangs() {
        List<ApqpPhase> cycle = cycleDe(5);
        ApqpPhase troisieme = cycle.get(2);
        when(repository.findByIdAndTenantId(troisieme.getId(), TENANT))
                .thenReturn(Optional.of(troisieme));

        List<ApqpPhase> restantes = new ArrayList<>(cycle);
        restantes.remove(troisieme);
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(restantes);

        service.supprimerPhase(troisieme.getId());

        verify(repository).delete(troisieme);
        // Sans resserrement on lirait 1, 2, 4, 5 : le V se dessinerait avec un
        // trou, et la contrainte d'unicité refuserait la prochaine insertion.
        assertThat(restantes.stream().map(ApqpPhase::getPosition))
                .containsExactly(1, 2, 3, 4);
    }

    @Test
    @DisplayName("une phase d'un autre client reste introuvable")
    void phaseDUnAutreClient_estIntrouvable() {
        UUID etrangere = UUID.randomUUID();
        when(repository.findByIdAndTenantId(etrangere, TENANT)).thenReturn(Optional.empty());

        // Le filtrage par client n'est pas une commodité d'affichage : sans lui,
        // un identifiant deviné suffirait à effacer la méthode d'un voisin.
        assertThatThrownBy(() -> service.supprimerPhase(etrangere))
                .isInstanceOf(ApqpPhaseNotFoundException.class);
        verify(repository, never()).delete(any());
    }

    // ---------- réorganisation ----------

    @Test
    @DisplayName("réorganiser exige le cycle entier, pas un ordre partiel")
    void reorganiser_refuseUnOrdrePartiel() {
        List<ApqpPhase> cycle = cycleDe(5);
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(cycle);
        List<UUID> partiel = List.of(cycle.get(0).getId(), cycle.get(1).getId());

        // Un ordre partiel laisserait des phases sans rang, donc hors du V.
        // Deviner où ranger les absentes serait pire que refuser.
        assertThatThrownBy(() -> service.reorganiser(new ApqpDto.ReorderRequest(partiel)))
                .isInstanceOf(ApqpReorderException.class);
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("réorganiser refuse un identifiant étranger au cycle")
    void reorganiser_refuseUnIntrus() {
        List<ApqpPhase> cycle = cycleDe(3);
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(cycle);
        List<UUID> avecIntrus = List.of(
                cycle.get(0).getId(), cycle.get(1).getId(), UUID.randomUUID());

        assertThatThrownBy(() -> service.reorganiser(new ApqpDto.ReorderRequest(avecIntrus)))
                .isInstanceOf(ApqpReorderException.class);
    }

    @Test
    @DisplayName("réorganiser renumérote de 1 à n")
    void reorganiser_renumerote() {
        List<ApqpPhase> cycle = cycleDe(5);
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(cycle);

        List<UUID> inverse = new ArrayList<>(cycle.stream().map(ApqpPhase::getId).toList());
        java.util.Collections.reverse(inverse);

        service.reorganiser(new ApqpDto.ReorderRequest(inverse));

        assertThat(cycle.get(4).getPosition()).isEqualTo(1);
        assertThat(cycle.get(0).getPosition()).isEqualTo(5);
    }

    // ---------- livrables ----------

    @Test
    @DisplayName("un livrable s'ajoute au bout de la liste")
    void ajouterLivrable_seMetAuBout() {
        ApqpPhase phase = avecLivrables(phase(1, "Planifier"), "A", "B");
        when(repository.findByIdAndTenantId(phase.getId(), TENANT)).thenReturn(Optional.of(phase));
        lenient().when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(List.of(phase));

        ApqpDto.PhaseResponse apres = service.ajouterLivrable(
                phase.getId(), new ApqpDto.DeliverableRequest("  C  "));

        assertThat(apres.deliverables()).hasSize(3);
        assertThat(apres.deliverables().get(2).label()).isEqualTo("C");
        assertThat(apres.deliverables().get(2).position()).isEqualTo(3);
    }

    @Test
    @DisplayName("retirer un livrable resserre les rangs des suivants")
    void supprimerLivrable_resserreLesRangs() {
        ApqpPhase phase = avecLivrables(phase(1, "Planifier"), "A", "B", "C");
        UUID premier = phase.getDeliverables().get(0).getId();
        when(repository.findByIdAndTenantId(phase.getId(), TENANT)).thenReturn(Optional.of(phase));
        lenient().when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(List.of(phase));

        ApqpDto.PhaseResponse apres = service.supprimerLivrable(phase.getId(), premier);

        assertThat(apres.deliverables().stream().map(ApqpDto.DeliverableResponse::label))
                .containsExactly("B", "C");
        assertThat(apres.deliverables().stream().map(ApqpDto.DeliverableResponse::position))
                .containsExactly(1, 2);
    }

    @Test
    @DisplayName("un livrable inconnu est refusé, pas ignoré en silence")
    void livrableInconnu_estRefuse() {
        ApqpPhase phase = avecLivrables(phase(1, "Planifier"), "A");
        when(repository.findByIdAndTenantId(phase.getId(), TENANT)).thenReturn(Optional.of(phase));

        // Ignorer laisserait croire à une suppression qui n'a pas eu lieu.
        assertThatThrownBy(() -> service.supprimerLivrable(phase.getId(), UUID.randomUUID()))
                .isInstanceOf(ApqpDeliverableNotFoundException.class);
    }

    @Test
    @DisplayName("reformuler un livrable ne touche pas les autres")
    void modifierLivrable_neTouchePasLesAutres() {
        ApqpPhase phase = avecLivrables(phase(1, "Planifier"), "A", "B");
        UUID second = phase.getDeliverables().get(1).getId();
        when(repository.findByIdAndTenantId(phase.getId(), TENANT)).thenReturn(Optional.of(phase));
        lenient().when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(List.of(phase));

        ApqpDto.PhaseResponse apres = service.modifierLivrable(
                phase.getId(), second, new ApqpDto.DeliverableRequest("B modifié"));

        assertThat(apres.deliverables().stream().map(ApqpDto.DeliverableResponse::label))
                .containsExactly("A", "B modifié");
    }

    @Test
    @DisplayName("renommer une phase la garde à son rang dans le V")
    void modifierPhase_gardeLeRang() {
        List<ApqpPhase> cycle = cycleDe(5);
        ApqpPhase troisieme = cycle.get(2);
        when(repository.findByIdAndTenantId(troisieme.getId(), TENANT)).thenReturn(Optional.of(troisieme));
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(cycle);

        ApqpDto.PhaseResponse apres = service.modifierPhase(
                troisieme.getId(),
                new ApqpDto.UpdatePhaseRequest("Validation", "Prouver", "Sait-on tenir ?"));

        assertThat(apres.title()).isEqualTo("Validation");
        assertThat(apres.position()).isEqualTo(3);
        // Le point bas du V pour un cycle de cinq : renommer ne déplace pas.
        assertThat(apres.level()).isEqualTo(3);
    }

    @Test
    @DisplayName("un objet laissé en blanc devient une absence, pas une chaîne vide")
    void champsBlancs_deviennentNull() {
        // Sans cela, l'écran afficherait une ligne vide sous l'intitulé, qu'on ne
        // pourrait distinguer d'une phase dont l'objet reste à écrire.
        when(repository.existsByTenantId(TENANT)).thenReturn(true);
        when(repository.findFirstByTenantIdOrderByPositionDesc(TENANT)).thenReturn(Optional.empty());
        when(repository.save(any(ApqpPhase.class))).thenAnswer(i -> i.getArgument(0));

        ApqpDto.PhaseResponse creee = service.creerPhase(
                new ApqpDto.CreatePhaseRequest("  Lancement  ", "   ", null));

        assertThat(creee.title()).isEqualTo("Lancement");
        assertThat(creee.purpose()).isNull();
        assertThat(creee.question()).isNull();
    }

    @Test
    @DisplayName("sans client dans le contexte, on ne lit aucun cycle")
    void sansTenant_refuse() {
        // Le tenant vient du jeton, jamais du corps : privé de contexte, le
        // service doit s'arrêter plutôt que de rendre le cycle de quelqu'un.
        TenantContext.clear();

        assertThatThrownBy(() -> service.cycle())
                .isInstanceOf(com.openlab.qualitos.quality.common.MissingTenantContextException.class);
        verify(repository, never()).findByTenantIdOrderByPositionAsc(any());
    }

    // ---------- fabriques ----------

    private List<Integer> niveaux(int total) {
        return java.util.stream.IntStream.rangeClosed(1, total)
                .map(i -> ApqpService.niveau(i, total))
                .boxed()
                .toList();
    }

    private ApqpPhase phase(int position, String titre) {
        ApqpPhase p = new ApqpPhase();
        p.setId(UUID.randomUUID());
        p.setTenantId(TENANT);
        p.setPosition(position);
        p.setTitle(titre);
        return p;
    }

    private ApqpPhase avecLivrables(ApqpPhase phase, String... libelles) {
        int rang = 1;
        for (String libelle : libelles) {
            ApqpDeliverable d = new ApqpDeliverable();
            d.setId(UUID.randomUUID());
            d.setLabel(libelle);
            d.setPosition(rang++);
            phase.addDeliverable(d);
        }
        return phase;
    }

    private List<ApqpPhase> cycleDe(int taille) {
        List<ApqpPhase> phases = new ArrayList<>();
        for (int i = 1; i <= taille; i++) {
            phases.add(phase(i, "Phase " + i));
        }
        return phases;
    }
}
