package com.openlab.qualitos.quality.controlplan.application;

import com.openlab.qualitos.quality.controlplan.domain.CharacteristicType;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlan;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanLine;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanNotFoundException;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanPhase;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanRepository;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanStateException;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanStatus;
import com.openlab.qualitos.quality.controlplan.domain.FmeaItemLookup;
import com.openlab.qualitos.quality.product.domain.Product;
import com.openlab.qualitos.quality.product.domain.ProductLookup;
import com.openlab.qualitos.quality.product.domain.ProductNotFoundException;
import com.openlab.qualitos.quality.product.domain.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ControlPlanServiceTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID OTHER_TENANT = UUID.randomUUID();
    static final UUID PRODUCT = UUID.randomUUID();
    static final UUID OTHER_PRODUCT = UUID.randomUUID();
    static final UUID PLAN = UUID.randomUUID();
    static final UUID LINE = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-08-19T08:00:00Z");

    ControlPlanRepository repo;
    ProductLookup products;
    FmeaItemLookup fmeaItems;
    TenantProvider tenants;
    ActorProvider actors;
    ControlPlanService service;

    @BeforeEach
    void setUp() {
        repo = mock(ControlPlanRepository.class);
        products = mock(ProductLookup.class);
        fmeaItems = mock(FmeaItemLookup.class);
        tenants = mock(TenantProvider.class);
        actors = mock(ActorProvider.class);
        when(tenants.requireTenantId()).thenReturn(TENANT);
        when(actors.currentUserId()).thenReturn(USER);
        when(products.findById(PRODUCT)).thenReturn(Optional.of(product(TENANT, PRODUCT)));
        when(repo.save(any())).thenAnswer(inv -> {
            ControlPlan p = inv.getArgument(0);
            if (p.getId() == null) p.assignId(UUID.randomUUID());
            return p;
        });
        when(repo.saveLine(any())).thenAnswer(inv -> {
            ControlPlanLine l = inv.getArgument(0);
            if (l.getId() == null) l.assignId(UUID.randomUUID());
            return l;
        });
        service = new ControlPlanService(repo, products, fmeaItems, tenants, actors,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void listingAndReadingGoThroughTheProduct() {
        ControlPlan plan = activePlan();
        when(repo.findByProduct(TENANT, PRODUCT)).thenReturn(List.of(plan));
        when(repo.findById(PLAN)).thenReturn(Optional.of(plan));
        when(repo.linesOf(PLAN)).thenReturn(List.of(line(PLAN)));

        assertThat(service.listForProduct(PRODUCT)).singleElement()
                .extracting(ControlPlanDto.View::code).isEqualTo("CP-4471");
        ControlPlanDto.Detail detail = service.get(PRODUCT, PLAN);
        assertThat(detail.plan().status()).isEqualTo(ControlPlanStatus.ACTIVE);
        assertThat(detail.lines()).singleElement()
                .extracting(ControlPlanDto.LineView::characteristicLabel).isEqualTo("Diamètre");
    }

    @Test
    void anUnknownProductIsRefusedBeforeAnythingElse() {
        when(products.findById(OTHER_PRODUCT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listForProduct(OTHER_PRODUCT))
                .isInstanceOf(ProductNotFoundException.class);
        verifyNoInteractions(repo);
    }

    @Test
    void aProductOfAnotherTenantIsNotFoundNeverForbidden() {
        when(products.findById(OTHER_PRODUCT))
                .thenReturn(Optional.of(product(OTHER_TENANT, OTHER_PRODUCT)));

        assertThatThrownBy(() -> service.listForProduct(OTHER_PRODUCT))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void creatingADraftRefusesASecondOneOnTheSamePhase() {
        when(repo.findDraft(TENANT, PRODUCT, ControlPlanPhase.PRODUCTION))
                .thenReturn(Optional.of(draftPlan()));

        assertThatThrownBy(() -> service.createDraft(PRODUCT,
                new ControlPlanDto.CreateCommand(ControlPlanPhase.PRODUCTION, "CP-2", null)))
                .isInstanceOf(ControlPlanStateException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void aFreshDraftCarriesTheTenantOfTheContextAndTheActorOfTheJwt() {
        when(repo.findDraft(TENANT, PRODUCT, ControlPlanPhase.PRE_LAUNCH)).thenReturn(Optional.empty());

        ControlPlanDto.View view = service.createDraft(PRODUCT,
                new ControlPlanDto.CreateCommand(ControlPlanPhase.PRE_LAUNCH, "CP-2", USER));

        assertThat(view.status()).isEqualTo(ControlPlanStatus.DRAFT);
        assertThat(view.revision()).isEqualTo(1);
        assertThat(view.ownerUserId()).isEqualTo(USER);
        assertThat(view.productId()).isEqualTo(PRODUCT);
    }

    @Test
    void addingALineToAnApprovedPlanIsRefused() {
        // 409. Le document en vigueur est affiché au poste : on n'y touche pas.
        when(repo.findById(PLAN)).thenReturn(Optional.of(activePlan()));

        assertThatThrownBy(() -> service.addLine(PRODUCT, PLAN, lineCommand(null)))
                .isInstanceOf(ControlPlanStateException.class);
        verify(repo, never()).saveLine(any());
    }

    @Test
    void openingARevisionCopiesEveryLineOfTheActivePlan() {
        // Une révision qui repartirait d'un plan vide ferait perdre le travail :
        // l'utilisateur corrigerait UNE ligne et perdrait les quarante autres.
        ControlPlan active = activePlan();
        when(repo.findById(PLAN)).thenReturn(Optional.of(active));
        when(repo.findDraft(TENANT, PRODUCT, ControlPlanPhase.PRODUCTION)).thenReturn(Optional.empty());
        when(repo.linesOf(PLAN)).thenReturn(List.of(line(PLAN), line(PLAN)));

        ControlPlanDto.View next = service.openRevision(PRODUCT, PLAN);

        assertThat(next.revision()).isEqualTo(2);
        assertThat(next.status()).isEqualTo(ControlPlanStatus.DRAFT);
        verify(repo, times(2)).saveLine(argThat(l -> l.getPlanId().equals(next.id())
                && l.getCharacteristicLabel().equals("Diamètre")
                && l.getMeasurementTechnique().equals("Micromètre")));
    }

    @Test
    void openingARevisionOfADraftIsRefused() {
        when(repo.findById(PLAN)).thenReturn(Optional.of(draftPlan()));

        assertThatThrownBy(() -> service.openRevision(PRODUCT, PLAN))
                .isInstanceOf(ControlPlanStateException.class);
    }

    @Test
    void openingASecondRevisionOnTheSamePhaseIsRefused() {
        when(repo.findById(PLAN)).thenReturn(Optional.of(activePlan()));
        when(repo.findDraft(TENANT, PRODUCT, ControlPlanPhase.PRODUCTION))
                .thenReturn(Optional.of(draftPlan()));

        assertThatThrownBy(() -> service.openRevision(PRODUCT, PLAN))
                .isInstanceOf(ControlPlanStateException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void approvingARevisionArchivesThePreviousActivePlanOfTheSamePhase() {
        // Sinon deux plans actifs coexistent, l'index partiel rejette l'écriture,
        // et l'utilisateur reçoit une erreur de base de données incompréhensible.
        ControlPlan draft = draftPlan();
        // La révision précédente est un AUTRE enregistrement : c'est justement parce
        // qu'elle coexiste avec le brouillon qu'il faut l'archiver au moment d'approuver.
        ControlPlan previous = activePlan(UUID.randomUUID());
        when(repo.findById(PLAN)).thenReturn(Optional.of(draft));
        when(repo.findActive(TENANT, PRODUCT, ControlPlanPhase.PRODUCTION))
                .thenReturn(Optional.of(previous));

        ControlPlanDto.View approved = service.approve(PRODUCT, PLAN);

        assertThat(approved.status()).isEqualTo(ControlPlanStatus.ACTIVE);
        assertThat(approved.approvedBy()).isEqualTo(USER);
        assertThat(approved.approvedAt()).isEqualTo(NOW);
        assertThat(previous.getStatus()).isEqualTo(ControlPlanStatus.ARCHIVED);
        verify(repo).save(previous);
    }

    @Test
    void approvingARevisionLeavesTheOtherPhaseAlone() {
        // Un produit en pré-série ET en série a légitimement deux plans actifs.
        ControlPlan draft = draftPlan();
        when(repo.findById(PLAN)).thenReturn(Optional.of(draft));
        when(repo.findActive(TENANT, PRODUCT, ControlPlanPhase.PRODUCTION)).thenReturn(Optional.empty());

        service.approve(PRODUCT, PLAN);

        verify(repo, never()).findActive(TENANT, PRODUCT, ControlPlanPhase.PRE_LAUNCH);
        verify(repo, times(1)).save(any());
    }

    @Test
    void approvingAnAlreadyActivePlanIsRefused() {
        when(repo.findById(PLAN)).thenReturn(Optional.of(activePlan()));

        assertThatThrownBy(() -> service.approve(PRODUCT, PLAN))
                .isInstanceOf(ControlPlanStateException.class);
    }

    @Test
    void aPlanOfAnotherTenantIsNotFoundNeverForbidden() {
        ControlPlan foreign = ControlPlan.rehydrate(PLAN, OTHER_TENANT, PRODUCT,
                ControlPlanPhase.PRODUCTION, "CP-X", 1, ControlPlanStatus.DRAFT,
                null, null, null, USER, NOW, NOW);
        when(repo.findById(PLAN)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.get(PRODUCT, PLAN))
                .isInstanceOf(ControlPlanNotFoundException.class);
    }

    @Test
    void aPlanOfAnotherProductIsNotFoundEvenWithTheRightTenant() {
        ControlPlan elsewhere = ControlPlan.rehydrate(PLAN, TENANT, OTHER_PRODUCT,
                ControlPlanPhase.PRODUCTION, "CP-X", 1, ControlPlanStatus.DRAFT,
                null, null, null, USER, NOW, NOW);
        when(repo.findById(PLAN)).thenReturn(Optional.of(elsewhere));

        assertThatThrownBy(() -> service.get(PRODUCT, PLAN))
                .isInstanceOf(ControlPlanNotFoundException.class);
    }

    @Test
    void anUnknownPlanIsNotFound() {
        when(repo.findById(PLAN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(PRODUCT, PLAN))
                .isInstanceOf(ControlPlanNotFoundException.class);
    }

    @Test
    void aLineOfAnotherPlanIsNotFoundEvenWithTheRightProduct() {
        // IDOR : la route est imbriquée, la chaîne complète produit → plan → ligne
        // se revérifie, pas seulement la feuille.
        when(repo.findById(PLAN)).thenReturn(Optional.of(draftPlan()));
        when(repo.findLine(LINE)).thenReturn(Optional.of(line(UUID.randomUUID())));

        assertThatThrownBy(() -> service.updateLine(PRODUCT, PLAN, LINE, lineCommand(null)))
                .isInstanceOf(ControlPlanNotFoundException.class);
        assertThatThrownBy(() -> service.deleteLine(PRODUCT, PLAN, LINE))
                .isInstanceOf(ControlPlanNotFoundException.class);
    }

    @Test
    void anUnknownLineIsNotFound() {
        when(repo.findById(PLAN)).thenReturn(Optional.of(draftPlan()));
        when(repo.findLine(LINE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteLine(PRODUCT, PLAN, LINE))
                .isInstanceOf(ControlPlanNotFoundException.class);
    }

    @Test
    void aLineWhoseFmeaItemBelongsToAnotherProductIsRefused() {
        // Le lien PFMEA justifie le contrôle. Pointer la ligne d'un autre produit
        // produirait une justification fausse — pire que pas de justification.
        UUID fmeaItem = UUID.randomUUID();
        when(repo.findById(PLAN)).thenReturn(Optional.of(draftPlan()));
        when(fmeaItems.productCoveredBy(fmeaItem)).thenReturn(Optional.of(OTHER_PRODUCT));

        assertThatThrownBy(() -> service.addLine(PRODUCT, PLAN, lineCommand(fmeaItem)))
                .isInstanceOf(ControlPlanStateException.class);
        verify(repo, never()).saveLine(any());
    }

    @Test
    void aLineWhoseFmeaItemDoesNotExistIsRefused() {
        UUID fmeaItem = UUID.randomUUID();
        when(repo.findById(PLAN)).thenReturn(Optional.of(draftPlan()));
        when(fmeaItems.productCoveredBy(fmeaItem)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addLine(PRODUCT, PLAN, lineCommand(fmeaItem)))
                .isInstanceOf(ControlPlanStateException.class);
    }

    @Test
    void aLineJustifiedByTheRightFmeaItemIsAccepted() {
        UUID fmeaItem = UUID.randomUUID();
        when(repo.findById(PLAN)).thenReturn(Optional.of(draftPlan()));
        when(fmeaItems.productCoveredBy(fmeaItem)).thenReturn(Optional.of(PRODUCT));

        ControlPlanDto.LineView view = service.addLine(PRODUCT, PLAN, lineCommand(fmeaItem));

        assertThat(view.fmeaItemId()).isEqualTo(fmeaItem);
        assertThat(view.characteristicLabel()).isEqualTo("Diamètre");
    }

    @Test
    void aLineWithoutAnyFmeaItemIsAcceptedAndNeverAsksTheFmea() {
        when(repo.findById(PLAN)).thenReturn(Optional.of(draftPlan()));

        ControlPlanDto.LineView view = service.addLine(PRODUCT, PLAN, lineCommand(null));

        assertThat(view.fmeaItemId()).isNull();
        verifyNoInteractions(fmeaItems);
    }

    @Test
    void updatingALineRewritesItsCharacteristic() {
        when(repo.findById(PLAN)).thenReturn(Optional.of(draftPlan()));
        when(repo.findLine(LINE)).thenReturn(Optional.of(line(PLAN)));

        ControlPlanDto.LineView view = service.updateLine(PRODUCT, PLAN, LINE,
                new ControlPlanDto.LineCommand(20, null, null, null, "Rugosité",
                        CharacteristicType.PROCESS, null, null, null, null, null, null,
                        null, null, null, null, null));

        assertThat(view.characteristicLabel()).isEqualTo("Rugosité");
        assertThat(view.characteristicType()).isEqualTo(CharacteristicType.PROCESS);
        assertThat(view.sequenceNo()).isEqualTo(20);
    }

    @Test
    void deletingALineOfADraftRemovesIt() {
        when(repo.findById(PLAN)).thenReturn(Optional.of(draftPlan()));
        when(repo.findLine(LINE)).thenReturn(Optional.of(line(PLAN)));

        service.deleteLine(PRODUCT, PLAN, LINE);

        verify(repo).deleteLine(LINE);
    }

    @Test
    void aLineOfAnotherTenantIsNotFound() {
        when(repo.findById(PLAN)).thenReturn(Optional.of(draftPlan()));
        ControlPlanLine foreign = ControlPlanLine.rehydrate(LINE, OTHER_TENANT, PLAN, 10,
                "Diamètre", CharacteristicType.PRODUCT);
        when(repo.findLine(LINE)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.deleteLine(PRODUCT, PLAN, LINE))
                .isInstanceOf(ControlPlanNotFoundException.class);
    }

    // ---------- montage ----------

    private Product product(UUID tenant, UUID id) {
        return Product.rehydrate(id, tenant, "REF-4471", "Support", null, null,
                ProductStatus.ACTIVE, null, null, null, USER, NOW, NOW);
    }

    private ControlPlan draftPlan() {
        return ControlPlan.rehydrate(PLAN, TENANT, PRODUCT, ControlPlanPhase.PRODUCTION,
                "CP-4471", 1, ControlPlanStatus.DRAFT, null, null, null, USER, NOW, NOW);
    }

    private ControlPlan activePlan() {
        return activePlan(PLAN);
    }

    private ControlPlan activePlan(UUID id) {
        return ControlPlan.rehydrate(id, TENANT, PRODUCT, ControlPlanPhase.PRODUCTION,
                "CP-4471", 1, ControlPlanStatus.ACTIVE, USER, USER, NOW, USER, NOW, NOW);
    }

    private ControlPlanLine line(UUID planId) {
        ControlPlanLine l = ControlPlanLine.rehydrate(LINE, TENANT, planId, 10, "Diamètre",
                CharacteristicType.PRODUCT);
        l.describe(null, "Tour CN 3", "12", null, "Ø 20", null, null, "mm",
                "Micromètre", 5, "1/h", "Carte X-R", "Tri à 100 %");
        return l;
    }

    private ControlPlanDto.LineCommand lineCommand(UUID fmeaItemId) {
        return new ControlPlanDto.LineCommand(10, null, "Tour CN 3", "12", "Diamètre",
                CharacteristicType.PRODUCT, null, "Ø 20", null, null, "mm", "Micromètre",
                5, "1/h", "Carte X-R", "Tri à 100 %", fmeaItemId);
    }
}
