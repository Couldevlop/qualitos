package com.openlab.qualitos.quality.controlplan.application;

import com.openlab.qualitos.quality.controlplan.domain.CharacteristicType;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlan;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanLine;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanNotFoundException;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanPhase;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanRepository;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanStateException;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanFingerprint;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanStatus;
import com.openlab.qualitos.quality.controlplan.domain.InputOutput;
import com.openlab.qualitos.quality.controlplan.domain.FmeaItemLookup;
import com.openlab.qualitos.quality.product.domain.Product;
import com.openlab.qualitos.quality.product.domain.ProductLookup;
import com.openlab.qualitos.quality.product.domain.ProductNotFoundException;
import com.openlab.qualitos.quality.product.domain.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
    ControlPlanAuditPort audit;
    ControlPlanSealPort seals;
    TenantProvider tenants;
    ActorProvider actors;
    ControlPlanService service;

    @BeforeEach
    void setUp() {
        repo = mock(ControlPlanRepository.class);
        products = mock(ProductLookup.class);
        fmeaItems = mock(FmeaItemLookup.class);
        audit = mock(ControlPlanAuditPort.class);
        seals = mock(ControlPlanSealPort.class);
        when(seals.seal(any(), any()))
                .thenReturn(new ControlPlanSealPort.Seal("sig-hybride", "tx-0001"));
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
        service = new ControlPlanService(repo, products, fmeaItems, audit, seals, tenants, actors,
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
                null, null, null, USER, NOW, NOW, null, null, null);
        when(repo.findById(PLAN)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.get(PRODUCT, PLAN))
                .isInstanceOf(ControlPlanNotFoundException.class);
    }

    @Test
    void aPlanOfAnotherProductIsNotFoundEvenWithTheRightTenant() {
        ControlPlan elsewhere = ControlPlan.rehydrate(PLAN, TENANT, OTHER_PRODUCT,
                ControlPlanPhase.PRODUCTION, "CP-X", 1, ControlPlanStatus.DRAFT,
                null, null, null, USER, NOW, NOW, null, null, null);
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
                        null, null, null, null, null, null, null, null, null));

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

    @Test
    void approvingIsWrittenToTheChainedJournalWithTheActorOfTheToken() {
        // Sans cette inscription, rien n'ancre l'approbation : le journal est ancré
        // par lots, le document ne l'est pas séparément. Un plan approuvé qui ne
        // laisserait aucune trace serait un écart en audit de certification.
        ControlPlan draft = draftPlan();
        when(repo.findById(PLAN)).thenReturn(Optional.of(draft));
        when(repo.findActive(TENANT, PRODUCT, ControlPlanPhase.PRODUCTION)).thenReturn(Optional.empty());

        service.approve(PRODUCT, PLAN);

        ArgumentCaptor<String> details = ArgumentCaptor.forClass(String.class);
        verify(audit).record(eq(TENANT), eq(USER), eq("controlplan.plan.approved"),
                eq(PLAN), eq("Control plan approuvé"), details.capture());
        assertThat(details.getValue())
                .contains("\"productId\":\"" + PRODUCT + "\"")
                .contains("\"phase\":\"PRODUCTION\"")
                .contains("\"status\":\"ACTIVE\"");
    }

    @Test
    void openingARevisionIsWrittenToTheJournalToo() {
        // Ouvrir une révision dégèle le document : c'est un fait aussi important
        // que l'approbation qui l'a figé.
        ControlPlan active = activePlan();
        when(repo.findById(PLAN)).thenReturn(Optional.of(active));
        when(repo.findDraft(TENANT, PRODUCT, ControlPlanPhase.PRODUCTION)).thenReturn(Optional.empty());
        when(repo.linesOf(PLAN)).thenReturn(List.of());

        service.openRevision(PRODUCT, PLAN);

        verify(audit).record(eq(TENANT), eq(USER), eq("controlplan.plan.revision-opened"),
                any(), eq("Révision de control plan ouverte"), any());
    }

    @Test
    void aRefusedApprovalLeavesNoTraceInTheJournal() {
        // Le journal consigne des faits, pas des tentatives : y inscrire un refus
        // technique noierait les décisions réelles.
        when(repo.findById(PLAN)).thenReturn(Optional.of(activePlan()));

        assertThatThrownBy(() -> service.approve(PRODUCT, PLAN))
                .isInstanceOf(ControlPlanStateException.class);

        verifyNoInteractions(audit);
    }

    @Test
    void aCodeCarryingAQuoteDoesNotBreakTheJournalLine() {
        ControlPlan draft = ControlPlan.rehydrate(PLAN, TENANT, PRODUCT, ControlPlanPhase.PRODUCTION,
                "CP \"A\"", 1, ControlPlanStatus.DRAFT, null, null, null, USER, NOW, NOW, null, null, null);
        when(repo.findById(PLAN)).thenReturn(Optional.of(draft));
        when(repo.findActive(TENANT, PRODUCT, ControlPlanPhase.PRODUCTION)).thenReturn(Optional.empty());

        service.approve(PRODUCT, PLAN);

        ArgumentCaptor<String> details = ArgumentCaptor.forClass(String.class);
        verify(audit).record(any(), any(), any(), any(), any(), details.capture());
        assertThat(details.getValue()).contains("CP \\\"A\\\"");
    }

    @Test
    void approvingSealsTheDocumentItselfAndNotJustTheJournalEntry() {
        ControlPlan draft = draftPlan();
        when(repo.findById(PLAN)).thenReturn(Optional.of(draft));
        when(repo.findActive(TENANT, PRODUCT, ControlPlanPhase.PRODUCTION)).thenReturn(Optional.empty());
        when(repo.linesOf(PLAN)).thenReturn(List.of(line(PLAN)));

        ControlPlanDto.View view = service.approve(PRODUCT, PLAN);

        assertThat(view.sealSha256()).hasSize(64);
        assertThat(view.anchorTxRef()).isEqualTo("tx-0001");
        verify(seals).seal(eq(TENANT), argThat(hash -> hash.length() == 64));
    }

    @Test
    void whatIsSealedIsTheFingerprintOfThePlanAndItsLines() {
        ControlPlan draft = draftPlan();
        when(repo.findById(PLAN)).thenReturn(Optional.of(draft));
        when(repo.findActive(TENANT, PRODUCT, ControlPlanPhase.PRODUCTION)).thenReturn(Optional.empty());
        List<ControlPlanLine> lines = List.of(line(PLAN));
        when(repo.linesOf(PLAN)).thenReturn(lines);

        ControlPlanDto.View view = service.approve(PRODUCT, PLAN);

        // Rejouer le calcul sur le plan rendu doit retrouver l'empreinte signée :
        // c'est très exactement le geste de l'auditeur qui vérifie lui-même.
        assertThat(view.sealSha256())
                .isEqualTo(ControlPlanFingerprint.of(draft, lines));
    }

    /**
     * Le banc précédent rejoue le calcul sur l'objet EN MÉMOIRE, celui-là même qui
     * vient d'être scellé : il ne peut pas voir un champ que la base abîme.
     *
     * <p>Or l'empreinte porte l'horodatage d'approbation, et
     * {@code TIMESTAMP WITH TIME ZONE} arrondit à la microseconde ce que
     * {@link Instant} exprime à la nanoseconde. Sceller un instant plus fin que
     * cela produit une preuve que plus personne ne peut recalculer à partir du
     * plan relu — l'auditeur, lui, ne travaille jamais sur autre chose.
     *
     * <p>Le geste vérifié ici est donc le sien : approuver avec une horloge à la
     * nanoseconde, puis rejouer le calcul sur le plan tel que la base le rendra.
     */
    @Test
    void theSealSurvivesTheRoundTripThroughTheDatabase() {
        service = new ControlPlanService(repo, products, fmeaItems, audit, seals, tenants, actors,
                Clock.fixed(Instant.parse("2026-08-19T08:00:00.123456789Z"), ZoneOffset.UTC));
        ControlPlan draft = draftPlan();
        when(repo.findById(PLAN)).thenReturn(Optional.of(draft));
        when(repo.findActive(TENANT, PRODUCT, ControlPlanPhase.PRODUCTION)).thenReturn(Optional.empty());
        List<ControlPlanLine> lines = List.of(line(PLAN));
        when(repo.linesOf(PLAN)).thenReturn(lines);

        ControlPlanDto.View view = service.approve(PRODUCT, PLAN);

        ControlPlan asReadBack = ControlPlan.rehydrate(
                draft.getId(), draft.getTenantId(), draft.getProductId(), draft.getPhase(),
                draft.getCode(), draft.getRevision(), draft.getStatus(),
                draft.getOwnerUserId(), draft.getApprovedBy(),
                draft.getApprovedAt().truncatedTo(ChronoUnit.MICROS),
                draft.getCreatedBy(), draft.getCreatedAt(), draft.getUpdatedAt(),
                draft.getSealSha256(), draft.getSealSignature(), draft.getAnchorTxRef());

        assertThat(ControlPlanFingerprint.of(asReadBack, lines)).isEqualTo(view.sealSha256());
    }

    /**
     * « Aucune action critique sans ancrage » (CLAUDE.md §18.2 #5). Un plan
     * approuvé mais non scellé serait affiché au poste avec une preuve manquante
     * que rien ne signalerait : mieux vaut refuser l'approbation.
     */
    @Test
    void anApprovalWhoseAnchoringFailsDoesNotGoThrough() {
        ControlPlan draft = draftPlan();
        when(repo.findById(PLAN)).thenReturn(Optional.of(draft));
        when(repo.findActive(TENANT, PRODUCT, ControlPlanPhase.PRODUCTION)).thenReturn(Optional.empty());
        when(repo.linesOf(PLAN)).thenReturn(List.of());
        when(seals.seal(any(), any())).thenThrow(new IllegalStateException("chaîne injoignable"));

        assertThatThrownBy(() -> service.approve(PRODUCT, PLAN))
                .isInstanceOf(IllegalStateException.class);

        verify(audit, never()).record(any(), any(), eq("controlplan.plan.approved"),
                any(), any(), any());
    }

    @Test
    void openingARevisionSealsNothing() {
        when(repo.findById(PLAN)).thenReturn(Optional.of(activePlan()));
        when(repo.findDraft(TENANT, PRODUCT, ControlPlanPhase.PRODUCTION)).thenReturn(Optional.empty());
        when(repo.linesOf(PLAN)).thenReturn(List.of());

        ControlPlanDto.View next = service.openRevision(PRODUCT, PLAN);

        assertThat(next.sealSha256()).isNull();
        verify(seals, never()).seal(any(), any());
    }

    // ---------- montage ----------

    private Product product(UUID tenant, UUID id) {
        return Product.rehydrate(id, tenant, "REF-4471", "Support", null, null,
                ProductStatus.ACTIVE, null, null, null, USER, NOW, NOW);
    }

    private ControlPlan draftPlan() {
        return ControlPlan.rehydrate(PLAN, TENANT, PRODUCT, ControlPlanPhase.PRODUCTION,
                "CP-4471", 1, ControlPlanStatus.DRAFT, null, null, null, USER, NOW, NOW, null, null, null);
    }

    private ControlPlan activePlan() {
        return activePlan(PLAN);
    }

    private ControlPlan activePlan(UUID id) {
        return ControlPlan.rehydrate(id, TENANT, PRODUCT, ControlPlanPhase.PRODUCTION,
                "CP-4471", 1, ControlPlanStatus.ACTIVE, USER, USER, NOW, USER, NOW, NOW, null, null, null);
    }

    private ControlPlanLine line(UUID planId) {
        ControlPlanLine l = ControlPlanLine.rehydrate(LINE, TENANT, planId, 10, "Diamètre",
                CharacteristicType.PRODUCT);
        l.describe(new ControlPlanLine.Details(null, "Tour CN 3", "12", null, "Ø 20",
                null, null, "mm", "Micromètre", "5", "1/h", "Carte X-R", "Tri à 100 %",
                "SOP-103", InputOutput.OUTPUT, "Opérateur", "Journal qualité"));
        return l;
    }

    private ControlPlanDto.LineCommand lineCommand(UUID fmeaItemId) {
        return new ControlPlanDto.LineCommand(10, null, "Tour CN 3", "12", "Diamètre",
                CharacteristicType.PRODUCT, null, "Ø 20", null, null, "mm", "Micromètre",
                "100 % (automatisé)", "1/h", "Carte X-R", "Tri à 100 %", fmeaItemId,
                "SOP-103", InputOutput.OUTPUT, "Opérateur de ligne", "Journal qualité");
    }
}
