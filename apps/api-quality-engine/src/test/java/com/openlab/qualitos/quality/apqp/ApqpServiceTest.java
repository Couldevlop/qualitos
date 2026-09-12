package com.openlab.qualitos.quality.apqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
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
    @Mock ApqpDeliverableEvidenceRepository evidences;
    @Mock ApqpLinkResolver linkResolver;
    ApqpService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID PHASE_ID = UUID.randomUUID();
    static final UUID LIVRABLE_ID = UUID.randomUUID();
    static final UUID ACTEUR = UUID.randomUUID();
    static final UUID CIBLE = UUID.randomUUID();
    static final java.time.Instant AMORCAGE = java.time.Instant.parse("2026-09-13T08:00:00Z");

    @BeforeEach
    void poserLeTenant() {
        TenantContext.setTenantId(TENANT.toString());
        // Le validateur est une fonction pure : la vraie instance dit la vérité
        // là où une doublure dirait ce qu'on lui souffle.
        service = new ApqpService(repository, evidences, new ApqpDeliverableDataValidator(),
                linkResolver, new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    @AfterEach
    void retirerLeTenant() {
        TenantContext.clear();
        // Le contexte de langue est un ThreadLocal : le laisser en anglais ferait
        // echouer le banc suivant, et pour une raison invisible dans son code.
        LocaleContextHolder.resetLocaleContext();
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
    @DisplayName("la première lecture amorce le cycle depuis le référentiel du document")
    void cycle_amorceALaPremiereLecture() {
        when(repository.existsByTenantId(TENANT)).thenReturn(false);
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(List.of());

        service.cycle();

        List<ApqpPhase> amorcees = amorcage();
        assertThat(amorcees).hasSize(5);
        // Les TITRES sont en français : ce sont des mots ordinaires, et le V est
        // l'élément le plus visible de l'écran. Les LIVRABLES, eux, gardent la
        // langue du document — « Control plan » désigne un document précis, pas
        // un plan de contrôle quelconque (cf. le banc des genres ci-dessous).
        assertThat(amorcees).extracting(ApqpPhase::getTitle)
                .containsExactly("Planification",
                                 "Conception du produit et développement",
                                 "Conception du processus et développement",
                                 "Validation du produit et du processus",
                                 "Production série et retour d'expérience");
        // Les livrables viennent avec : un cycle sans eux n'aurait rien à dire.
        assertThat(amorcees.get(0).getDeliverables()).hasSize(8);
        assertThat(amorcees.get(1).getDeliverables()).hasSize(9);
        assertThat(amorcees.get(2).getDeliverables()).hasSize(13);
        assertThat(amorcees.get(3).getDeliverables()).hasSize(9);
        assertThat(amorcees.get(4).getDeliverables()).hasSize(9);
        // Le tenant est porté par CHAQUE livrable, pas seulement par la phase :
        // une ligne sans client échapperait au cloisonnement.
        assertThat(amorcees.get(0).getDeliverables().get(0).getTenantId()).isEqualTo(TENANT);
    }

    @Test
    @DisplayName("l'astérisque du document marque les livrables du dossier PPAP, et eux seuls")
    void amorcage_marqueLesLivrablesPpap() {
        when(repository.existsByTenantId(TENANT)).thenReturn(false);
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(List.of());

        service.cycle();

        List<ApqpPhase> amorcees = amorcage();
        List<ApqpDeliverable> tous = amorcees.stream()
                .flatMap(p -> p.getDeliverables().stream()).toList();

        // Le document compte douze éléments PPAP : deux en phase 2, quatre en
        // phase 3, six en phase 4. Ce chiffre est la raison d'être de la section
        // PPAP : elle n'est juste que s'il l'est.
        assertThat(tous).filteredOn(ApqpDeliverable::isPpap).hasSize(12);
        // Aucun en phase 1 : le dossier ne se constitue qu'à partir de la
        // conception produit.
        assertThat(amorcees.get(0).getDeliverables()).noneMatch(ApqpDeliverable::isPpap);
        assertThat(tous).filteredOn(ApqpDeliverable::isPpap)
                .extracting(ApqpDeliverable::getLabel)
                .contains("AMDEC processus (PFMEA)", "Plan de surveillance",
                          "Analyse des systèmes de mesure (MSA)",
                          "Rapport de contrôle du premier article (FAIR)",
                          "Dossier PPAP et formulaire d'approbation",
                          "Exigences spécifiques du client");
    }

    @Test
    @DisplayName("chaque livrable reçoit le genre qui dit ce qu'il produit")
    void amorcage_donneLeGenre() {
        when(repository.existsByTenantId(TENANT)).thenReturn(false);
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(List.of());

        service.cycle();

        Map<String, ApqpDeliverable> parLibelle = amorcage().stream()
                .flatMap(p -> p.getDeliverables().stream())
                .collect(Collectors.toMap(ApqpDeliverable::getLabel, d -> d, (a, b) -> a));

        // Une AMDEC et un plan de surveillance sont déjà tenus ailleurs dans
        // QualitOS : on y renvoie, au lieu d'en demander une copie qui
        // vieillirait à part.
        assertThat(parLibelle.get("AMDEC processus (PFMEA)").getKind())
                .isEqualTo(ApqpDeliverableKind.MODULE_LINK);
        assertThat(parLibelle.get("Plan de surveillance").getKind())
                .isEqualTo(ApqpDeliverableKind.MODULE_LINK);
        assertThat(parLibelle.get("Nomenclature préliminaire (BOM)").getKind())
                .isEqualTo(ApqpDeliverableKind.ATTACHMENT);
        assertThat(parLibelle.get("Études de capabilité initiale du processus").getKind())
                .isEqualTo(ApqpDeliverableKind.DATA_ENTRY);
        assertThat(parLibelle.get("Approbations de manutention, d'emballage,"
                                 + " d'étiquetage et de marquage des pièces").getKind())
                .isEqualTo(ApqpDeliverableKind.CHECKLIST);
    }

    @Test
    @DisplayName("les sous-points et les mesures du document sont amorcés, vides")
    void amorcage_poseLesSousPointsEtLesMesures() {
        when(repository.existsByTenantId(TENANT)).thenReturn(false);
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(List.of());

        service.cycle();

        List<ApqpPhase> amorcees = amorcage();
        ApqpDeliverable cibles = amorcees.get(0).getDeliverables().get(1);
        assertThat(cibles.getKind()).isEqualTo(ApqpDeliverableKind.CHECKLIST);
        assertThat(cibles.getData())
                .contains("\"label\":\"sécurité\"")
                .contains("\"checked\":false")
                .contains("maintenabilité");

        ApqpDeliverable capabilite = amorcees.get(3).getDeliverables().stream()
                .filter(d -> "Études de capabilité initiale du processus".equals(d.getLabel()))
                .findFirst().orElseThrow();
        // Les intitulés sont posés, les valeurs restent à mesurer : c'est ce qui
        // distingue un livrable amorcé d'un livrable renseigné.
        assertThat(capabilite.getData())
                .contains("\"label\":\"Cpk\"")
                .contains("\"value\":\"\"")
                .contains("\"measuredAt\":null");
    }

    @Test
    @DisplayName("un livrable documentaire n'emporte aucun contenu d'amorçage")
    void amorcage_laissePiecesJointesSansContenu() {
        when(repository.existsByTenantId(TENANT)).thenReturn(false);
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(List.of());

        service.cycle();

        // Une chaîne vide aurait été lue comme un tableau vide par l'écran, donc
        // comme une liste de points qu'on aurait effacée.
        assertThat(amorcage().stream()
                .flatMap(p -> p.getDeliverables().stream())
                .filter(d -> d.getKind() == ApqpDeliverableKind.ATTACHMENT))
                .isNotEmpty()
                .allMatch(d -> d.getData() == null);
    }

    @Test
    @DisplayName("la deuxième lecture ne réamorce pas")
    void cycle_neReamorcePas() {
        when(repository.existsByTenantId(TENANT)).thenReturn(true);
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(cycleDe(5));

        ApqpDto.CycleResponse cycle = service.cycle();

        // Sans cette garde, chaque ouverture aurait rajouté cinq phases.
        verify(repository, never()).saveAll(anyList());
        assertThat(cycle.phases()).hasSize(5);
        assertThat(cycle.phases().stream().map(ApqpDto.PhaseResponse::level))
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
                phase.getId(),
                new ApqpDto.DeliverableRequest("  C  ", false, ApqpDeliverableKind.ATTACHMENT));

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
                phase.getId(), second,
                new ApqpDto.DeliverableRequest("B modifié", false, ApqpDeliverableKind.ATTACHMENT));

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

    // ---------- achèvement d'un livrable ----------

    @Test
    @DisplayName("cocher un livrable prend l'acteur du jeton et l'heure du serveur")
    void completer_prendActeurEtHeureDuServeur() {
        ApqpPhase phase = phaseAvecLivrable(ApqpDeliverableKind.ATTACHMENT);
        when(repository.findByIdAndTenantId(PHASE_ID, TENANT)).thenReturn(Optional.of(phase));
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(List.of(phase));

        service.completerLivrable(PHASE_ID, LIVRABLE_ID,
                new ApqpDto.CompletionRequest(true, "  reçu par courriel  ", null, null, null),
                ACTEUR);

        ApqpDeliverable livrable = phase.getDeliverables().get(0);
        assertThat(livrable.isDone()).isTrue();
        assertThat(livrable.getDoneAt()).isNotNull();
        assertThat(livrable.getDoneBy()).isEqualTo(ACTEUR);
        assertThat(livrable.getComment()).isEqualTo("reçu par courriel");
    }

    @Test
    @DisplayName("décocher efface qui et quand, plutôt que de laisser une trace fausse")
    void decocher_effaceQuiEtQuand() {
        ApqpPhase phase = phaseAvecLivrable(ApqpDeliverableKind.ATTACHMENT);
        ApqpDeliverable livrable = phase.getDeliverables().get(0);
        livrable.setDone(true);
        livrable.setDoneAt(Instant.parse("2026-09-01T10:00:00Z"));
        livrable.setDoneBy(ACTEUR);
        when(repository.findByIdAndTenantId(PHASE_ID, TENANT)).thenReturn(Optional.of(phase));
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(List.of(phase));

        service.completerLivrable(PHASE_ID, LIVRABLE_ID,
                new ApqpDto.CompletionRequest(false, null, null, null, null), ACTEUR);

        assertThat(livrable.isDone()).isFalse();
        assertThat(livrable.getDoneAt()).isNull();
        assertThat(livrable.getDoneBy()).isNull();
    }

    @Test
    @DisplayName("un renvoi vers un enregistrement absent du client est refusé")
    void renvoiMort_estRefuse() {
        ApqpPhase phase = phaseAvecLivrable(ApqpDeliverableKind.MODULE_LINK);
        when(repository.findByIdAndTenantId(PHASE_ID, TENANT)).thenReturn(Optional.of(phase));
        doThrow(new ApqpDeliverableValidationException("No FMEA record"))
                .when(linkResolver).verifier(ApqpLinkedKind.FMEA, CIBLE, TENANT);

        assertThatThrownBy(() -> service.completerLivrable(PHASE_ID, LIVRABLE_ID,
                new ApqpDto.CompletionRequest(true, null, null, ApqpLinkedKind.FMEA, CIBLE),
                ACTEUR))
                .isInstanceOf(ApqpDeliverableValidationException.class);

        // Rien n'a bougé : un lien mort ne doit pas laisser un livrable coché à
        // moitié, qui affirmerait qu'une preuve existe.
        assertThat(phase.getDeliverables().get(0).isDone()).isFalse();
        assertThat(phase.getDeliverables().get(0).getLinkedId()).isNull();
    }

    @Test
    @DisplayName("un renvoi validé est enregistré avec le livrable")
    void renvoiValide_estEnregistre() {
        ApqpPhase phase = phaseAvecLivrable(ApqpDeliverableKind.MODULE_LINK);
        when(repository.findByIdAndTenantId(PHASE_ID, TENANT)).thenReturn(Optional.of(phase));
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(List.of(phase));

        service.completerLivrable(PHASE_ID, LIVRABLE_ID,
                new ApqpDto.CompletionRequest(true, null, null, ApqpLinkedKind.CONTROL_PLAN, CIBLE),
                ACTEUR);

        verify(linkResolver).verifier(ApqpLinkedKind.CONTROL_PLAN, CIBLE, TENANT);
        assertThat(phase.getDeliverables().get(0).getLinkedKind())
                .isEqualTo(ApqpLinkedKind.CONTROL_PLAN);
        assertThat(phase.getDeliverables().get(0).getLinkedId()).isEqualTo(CIBLE);
    }

    @Test
    @DisplayName("un livrable à renvoi ne se coche pas sans son enregistrement")
    void renvoiAbsent_empecheDeCocher() {
        ApqpPhase phase = phaseAvecLivrable(ApqpDeliverableKind.MODULE_LINK);
        when(repository.findByIdAndTenantId(PHASE_ID, TENANT)).thenReturn(Optional.of(phase));

        // Coché sans enregistrement, il affirmerait qu'une AMDEC existe sans dire
        // laquelle — la pire des deux situations.
        assertThatThrownBy(() -> service.completerLivrable(PHASE_ID, LIVRABLE_ID,
                new ApqpDto.CompletionRequest(true, null, null, null, null), ACTEUR))
                .isInstanceOf(ApqpDeliverableValidationException.class)
                .hasMessageContaining("MODULE_LINK");
    }

    @Test
    @DisplayName("un renvoi sur un genre qui n'en porte pas est refusé")
    void renvoiSurMauvaisGenre_estRefuse() {
        ApqpPhase phase = phaseAvecLivrable(ApqpDeliverableKind.ATTACHMENT);
        when(repository.findByIdAndTenantId(PHASE_ID, TENANT)).thenReturn(Optional.of(phase));

        assertThatThrownBy(() -> service.completerLivrable(PHASE_ID, LIVRABLE_ID,
                new ApqpDto.CompletionRequest(false, null, null, ApqpLinkedKind.PDCA, CIBLE),
                ACTEUR))
                .isInstanceOf(ApqpDeliverableValidationException.class);
        verify(linkResolver, never()).verifier(any(), any(), any());
    }

    @Test
    @DisplayName("le contenu d'une checklist revient tel qu'il a été coché")
    void contenu_allerRetour() {
        ApqpPhase phase = phaseAvecLivrable(ApqpDeliverableKind.CHECKLIST);
        when(repository.findByIdAndTenantId(PHASE_ID, TENANT)).thenReturn(Optional.of(phase));
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(List.of(phase));

        ApqpDto.CycleResponse apres = service.completerLivrable(PHASE_ID, LIVRABLE_ID,
                new ApqpDto.CompletionRequest(false, null, List.of(
                        new ApqpDto.DataRow("safety", null, null, null, true),
                        new ApqpDto.DataRow("cost", null, null, null, false)), null, null),
                ACTEUR);

        List<ApqpDto.DataRow> lu = apres.phases().get(0).deliverables().get(0).data();
        assertThat(lu).extracting(ApqpDto.DataRow::label).containsExactly("safety", "cost");
        assertThat(lu).extracting(ApqpDto.DataRow::checked).containsExactly(true, false);
    }

    @Test
    @DisplayName("un contenu illisible rend une liste vide au lieu de casser l'écran")
    void contenuIllisible_neCassePasLEcran() {
        ApqpPhase phase = phaseAvecLivrable(ApqpDeliverableKind.CHECKLIST);
        // Une donnée d'avant ce lot, ou touchée à la main en base.
        phase.getDeliverables().get(0).setData("ceci n'est pas du JSON");
        when(repository.existsByTenantId(TENANT)).thenReturn(true);
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(List.of(phase));

        ApqpDto.CycleResponse cycle = service.cycle();

        assertThat(cycle.phases().get(0).deliverables().get(0).data()).isEmpty();
    }

    @Test
    @DisplayName("changer le genre d'un livrable vide son contenu et son renvoi")
    void changerDeGenre_videLeContenu() {
        ApqpPhase phase = phaseAvecLivrable(ApqpDeliverableKind.CHECKLIST);
        ApqpDeliverable livrable = phase.getDeliverables().get(0);
        livrable.setData("[{\"label\":\"safety\",\"checked\":true}]");
        when(repository.findByIdAndTenantId(PHASE_ID, TENANT)).thenReturn(Optional.of(phase));
        lenient().when(repository.findByTenantIdOrderByPositionAsc(TENANT))
                .thenReturn(List.of(phase));

        service.modifierLivrable(PHASE_ID, LIVRABLE_ID, new ApqpDto.DeliverableRequest(
                "Project plan", true, ApqpDeliverableKind.ATTACHMENT));

        // Une liste de points lue comme une table de mesures ne veut rien dire :
        // on vide, plutôt que de garder un état qu'aucun formulaire ne rend.
        assertThat(livrable.getData()).isNull();
        assertThat(livrable.getKind()).isEqualTo(ApqpDeliverableKind.ATTACHMENT);
        assertThat(livrable.isPpap()).isTrue();
    }

    @Test
    @DisplayName("un livrable créé à la main peut être marqué PPAP")
    void ajouterLivrable_peutEtrePpap() {
        ApqpPhase phase = phaseAvecLivrable(ApqpDeliverableKind.ATTACHMENT);
        when(repository.findByIdAndTenantId(PHASE_ID, TENANT)).thenReturn(Optional.of(phase));
        lenient().when(repository.findByTenantIdOrderByPositionAsc(TENANT))
                .thenReturn(List.of(phase));

        service.ajouterLivrable(PHASE_ID, new ApqpDto.DeliverableRequest(
                "Customer sign-off", true, ApqpDeliverableKind.ATTACHMENT));

        ApqpDeliverable ajoute = phase.getDeliverables().get(phase.getDeliverables().size() - 1);
        assertThat(ajoute.isPpap()).isTrue();
        assertThat(ajoute.getLabel()).isEqualTo("Customer sign-off");
    }

    // ---------- dossier PPAP ----------

    @Test
    @DisplayName("le cycle dit combien de livrables PPAP sont acquis")
    void cycle_compteLeDossierPpap() {
        ApqpPhase phase = phaseAvecLivrable(ApqpDeliverableKind.ATTACHMENT);
        ApqpDeliverable etoile = phase.getDeliverables().get(0);
        etoile.setPpap(true);
        etoile.setDone(true);

        ApqpDeliverable autre = new ApqpDeliverable();
        autre.setId(UUID.randomUUID());
        autre.setPosition(2);
        autre.setLabel("MSA");
        autre.setPpap(true);
        phase.addDeliverable(autre);

        ApqpDeliverable ordinaire = new ApqpDeliverable();
        ordinaire.setId(UUID.randomUUID());
        ordinaire.setPosition(3);
        ordinaire.setLabel("Floor plan layout");
        phase.addDeliverable(ordinaire);

        when(repository.existsByTenantId(TENANT)).thenReturn(true);
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(List.of(phase));

        ApqpDto.CycleResponse cycle = service.cycle();

        // Calculé par le SERVEUR : deux vues du même cycle doivent afficher le même
        // chiffre, et la règle changera le jour où « acquis » voudra dire « coché ET
        // prouvé ».
        assertThat(cycle.ppapTotal()).isEqualTo(2);
        assertThat(cycle.ppapDone()).isEqualTo(1);
    }

    @Test
    @DisplayName("chaque livrable annonce combien de pièces le prouvent")
    void cycle_compteLesPieces() {
        ApqpPhase phase = phaseAvecLivrable(ApqpDeliverableKind.ATTACHMENT);
        when(repository.existsByTenantId(TENANT)).thenReturn(true);
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(List.of(phase));
        List<Object[]> comptes = new ArrayList<>();
        comptes.add(new Object[] { LIVRABLE_ID, 3L });
        when(evidences.countByDeliverableForTenant(TENANT)).thenReturn(comptes);

        ApqpDto.CycleResponse cycle = service.cycle();

        assertThat(cycle.phases().get(0).deliverables().get(0).evidenceCount()).isEqualTo(3);
    }

    // ---------- réinitialisation ----------

    @Test
    @DisplayName("la réinitialisation efface le cycle du client avant de le réamorcer")
    void reinitialiser_effacePuisAmorce() {
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(List.of());

        service.reinitialiser();

        // L'ordre compte : effacer, VIDER le cache de persistance, puis insérer.
        // Sans le vidage, l'insertion bute sur l'unicité (client, rang).
        InOrder ordre = inOrder(repository);
        ordre.verify(repository).deleteByTenantId(TENANT);
        ordre.verify(repository).flush();
        ordre.verify(repository).saveAll(anyList());
    }

    // ---------- le référentiel suit la langue ----------

    @Test
    @DisplayName("le cycle se lit dans la langue demandée tant qu'il n'est pas retouché")
    void referentiel_suitLaLangue() {
        ApqpPhase phase = phaseDuReferentiel();
        when(repository.existsByTenantId(TENANT)).thenReturn(true);
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(List.of(phase));

        LocaleContextHolder.setLocale(Locale.ENGLISH);
        ApqpDto.CycleResponse anglais = service.cycle();
        LocaleContextHolder.setLocale(Locale.forLanguageTag("es"));
        ApqpDto.CycleResponse espagnol = service.cycle();

        assertThat(anglais.phases().get(0).title()).isEqualTo("Process Design & Development");
        assertThat(anglais.phases().get(0).deliverables().get(0).label()).isEqualTo("PFMEA");
        assertThat(espagnol.phases().get(0).title())
                .isEqualTo("Diseño y desarrollo del proceso");
        assertThat(espagnol.phases().get(0).deliverables().get(0).label())
                .isEqualTo("AMFE de proceso (PFMEA)");
    }

    @Test
    @DisplayName("une langue inconnue retombe sur le français, langue source")
    void langueInconnue_retombeSurLeFrancais() {
        ApqpPhase phase = phaseDuReferentiel();
        when(repository.existsByTenantId(TENANT)).thenReturn(true);
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(List.of(phase));

        LocaleContextHolder.setLocale(Locale.forLanguageTag("it"));

        assertThat(service.cycle().phases().get(0).title())
                .isEqualTo("Conception du processus et développement");
    }

    @Test
    @DisplayName("une ligne que le client a réécrite n'est plus traduite")
    void ligneRetouchee_nEstPlusTraduite() {
        ApqpPhase phase = phaseDuReferentiel();
        // Le client a renommé : `updatedAt` se décale, et c'est SON texte qui doit
        // sortir — dans SA langue, quelle que soit celle de l'interface.
        phase.setTitle("Notre conception process");
        phase.setUpdatedAt(phase.getCreatedAt().plusSeconds(1));
        ApqpDeliverable livrable = phase.getDeliverables().get(0);
        livrable.setLabel("AMDEC maison");
        livrable.setUpdatedAt(livrable.getCreatedAt().plusSeconds(1));

        when(repository.existsByTenantId(TENANT)).thenReturn(true);
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(List.of(phase));
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        ApqpDto.CycleResponse cycle = service.cycle();

        assertThat(cycle.phases().get(0).title()).isEqualTo("Notre conception process");
        assertThat(cycle.phases().get(0).deliverables().get(0).label()).isEqualTo("AMDEC maison");
    }

    @Test
    @DisplayName("une ligne que le client a ajoutée n'a pas de clé, donc pas de traduction")
    void ligneDuClient_nEstJamaisTraduite() {
        ApqpPhase phase = phaseDuReferentiel();
        ApqpDeliverable sien = new ApqpDeliverable();
        sien.setId(UUID.randomUUID());
        sien.setPosition(2);
        sien.setLabel("Revue de contrat client");
        sien.setCreatedAt(phase.getCreatedAt());
        sien.setUpdatedAt(phase.getCreatedAt());
        phase.addDeliverable(sien);

        when(repository.existsByTenantId(TENANT)).thenReturn(true);
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(List.of(phase));
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        assertThat(service.cycle().phases().get(0).deliverables().get(1).label())
                .isEqualTo("Revue de contrat client");
    }

    @Test
    @DisplayName("les sous-points amorcés suivent la langue, eux aussi")
    void sousPoints_suiventLaLangue() {
        ApqpPhase phase = new ApqpPhase();
        phase.setId(PHASE_ID);
        phase.setTenantId(TENANT);
        phase.setPosition(1);
        phase.setTitle("Planification");
        phase.setReferenceKey("phase.planning");
        phase.setCreatedAt(AMORCAGE);
        phase.setUpdatedAt(AMORCAGE);

        ApqpDeliverable cibles = new ApqpDeliverable();
        cibles.setId(LIVRABLE_ID);
        cibles.setPosition(1);
        cibles.setReferenceKey("deliv.project-targets");
        cibles.setKind(ApqpDeliverableKind.CHECKLIST);
        cibles.setLabel("Objectifs du projet");
        cibles.setData("[{\"label\":\"sécurité\",\"checked\":false}]");
        cibles.setCreatedAt(AMORCAGE);
        cibles.setUpdatedAt(AMORCAGE);
        phase.addDeliverable(cibles);

        when(repository.existsByTenantId(TENANT)).thenReturn(true);
        when(repository.findByTenantIdOrderByPositionAsc(TENANT)).thenReturn(List.of(phase));
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        List<ApqpDto.DataRow> lignes =
                service.cycle().phases().get(0).deliverables().get(0).data();

        assertThat(lignes).extracting(ApqpDto.DataRow::label)
                .containsExactly("safety", "quality/manufacturability", "service life",
                                 "reliability", "durability", "maintainability",
                                 "schedule", "cost");
    }

    // ---------- fabriques ----------

    /**
     * Une phase telle que l'amorçage l'écrit : clé posée, dates égales.
     *
     * <p>L'égalité des dates EST la condition de traduction : c'est elle qui dit
     * que personne n'a retouché la ligne.
     */
    private ApqpPhase phaseDuReferentiel() {
        ApqpPhase phase = new ApqpPhase();
        phase.setId(PHASE_ID);
        phase.setTenantId(TENANT);
        phase.setPosition(1);
        phase.setReferenceKey("phase.process-design");
        phase.setTitle("Conception du processus et développement");
        phase.setPurpose("Définir le processus de fabrication et ce qui le surveillera.");
        phase.setQuestion("Comment fabrique-t-on, et comment saura-t-on que c'est conforme ?");
        phase.setCreatedAt(AMORCAGE);
        phase.setUpdatedAt(AMORCAGE);

        ApqpDeliverable livrable = new ApqpDeliverable();
        livrable.setId(LIVRABLE_ID);
        livrable.setPosition(1);
        livrable.setReferenceKey("deliv.pfmea");
        livrable.setLabel("AMDEC processus (PFMEA)");
        livrable.setKind(ApqpDeliverableKind.MODULE_LINK);
        livrable.setCreatedAt(AMORCAGE);
        livrable.setUpdatedAt(AMORCAGE);
        phase.addDeliverable(livrable);
        return phase;
    }

    /** Une phase d'un seul livrable, du genre demandé. */
    private ApqpPhase phaseAvecLivrable(ApqpDeliverableKind genre) {
        ApqpPhase phase = new ApqpPhase();
        phase.setId(PHASE_ID);
        phase.setTenantId(TENANT);
        phase.setPosition(1);
        phase.setTitle("Planning");

        ApqpDeliverable livrable = new ApqpDeliverable();
        livrable.setId(LIVRABLE_ID);
        livrable.setPosition(1);
        livrable.setLabel("Project plan");
        livrable.setKind(genre);
        phase.addDeliverable(livrable);
        return phase;
    }

    /**
     * Les phases que l'amorçage vient d'écrire.
     *
     * <p>Capturées sur {@code saveAll} plutôt que relues du dépôt : c'est bien ce
     * que le service a composé qu'on éprouve, pas ce qu'une doublure rendrait.
     */
    private List<ApqpPhase> amorcage() {
        var capture = org.mockito.ArgumentCaptor.forClass(Iterable.class);
        verify(repository).saveAll(capture.capture());
        List<ApqpPhase> amorcees = new ArrayList<>();
        @SuppressWarnings("unchecked")
        Iterable<ApqpPhase> capturees = (Iterable<ApqpPhase>) capture.getValue();
        capturees.forEach(amorcees::add);
        return amorcees;
    }


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
