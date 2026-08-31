package com.openlab.qualitos.quality.controlplan.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le control plan est un document approuvé : il est affiché au poste et montré à
 * l'auditeur. Ce qui est en vigueur ne se modifie pas — on en ouvre une révision.
 * L'invariant est ici, dans le domaine, et pas seulement dans le service : c'est
 * lui qui rend le document opposable.
 */
class ControlPlanTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID PRODUCT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-08-19T08:00:00Z");

    private ControlPlan draft() {
        return ControlPlan.create(TENANT, PRODUCT, ControlPlanPhase.PRODUCTION, "CP-4471", USER, NOW);
    }

    @Test
    void aNewPlanIsADraftAtRevisionOne() {
        ControlPlan plan = draft();

        assertThat(plan.getStatus()).isEqualTo(ControlPlanStatus.DRAFT);
        assertThat(plan.getRevision()).isEqualTo(1);
        assertThat(plan.getApprovedAt()).isNull();
    }

    @Test
    void approvingMakesItActiveAndStampsTheApprover() {
        ControlPlan plan = draft();

        plan.approve(USER, NOW);

        assertThat(plan.getStatus()).isEqualTo(ControlPlanStatus.ACTIVE);
        assertThat(plan.getApprovedBy()).isEqualTo(USER);
        assertThat(plan.getApprovedAt()).isEqualTo(NOW);
    }

    @Test
    void anActivePlanRefusesEveryModification() {
        ControlPlan plan = draft();
        plan.approve(USER, NOW);

        assertThatThrownBy(plan::requireDraft)
                .isInstanceOf(ControlPlanStateException.class)
                .hasMessageContaining("révision");
    }

    @Test
    void approvingTwiceIsRefused() {
        ControlPlan plan = draft();
        plan.approve(USER, NOW);

        assertThatThrownBy(() -> plan.approve(USER, NOW))
                .isInstanceOf(ControlPlanStateException.class);
    }

    @Test
    void aRevisionCarriesTheNextIndexAndStartsAsADraft() {
        ControlPlan active = draft();
        active.approve(USER, NOW);

        ControlPlan next = active.nextRevision(USER, NOW.plusSeconds(3600));

        assertThat(next.getRevision()).isEqualTo(2);
        assertThat(next.getStatus()).isEqualTo(ControlPlanStatus.DRAFT);
        assertThat(next.getProductId()).isEqualTo(PRODUCT);
        assertThat(next.getPhase()).isEqualTo(ControlPlanPhase.PRODUCTION);
    }

    @Test
    void openingARevisionOfADraftIsRefusedThereIsAlreadyOne() {
        ControlPlan plan = draft();

        assertThatThrownBy(() -> plan.nextRevision(USER, NOW))
                .isInstanceOf(ControlPlanStateException.class);
    }

    @Test
    void anArchivedPlanIsFrozenForGood() {
        ControlPlan plan = draft();
        plan.approve(USER, NOW);
        plan.archive();

        assertThatThrownBy(plan::requireDraft).isInstanceOf(ControlPlanStateException.class);
        assertThatThrownBy(() -> plan.approve(USER, NOW)).isInstanceOf(ControlPlanStateException.class);
        assertThatThrownBy(() -> plan.nextRevision(USER, NOW)).isInstanceOf(ControlPlanStateException.class);
    }

    @Test
    void aPlanWithoutACodeIsRefused() {
        assertThatThrownBy(() -> ControlPlan.create(TENANT, PRODUCT, ControlPlanPhase.PRODUCTION,
                "   ", USER, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void renamingAndAssigningAnOwnerNeedADraft() {
        ControlPlan plan = draft();
        plan.rename("CP-4471-B");
        plan.assignOwner(USER);
        assertThat(plan.getCode()).isEqualTo("CP-4471-B");
        assertThat(plan.getOwnerUserId()).isEqualTo(USER);

        plan.approve(USER, NOW);

        assertThatThrownBy(() -> plan.rename("CP-X")).isInstanceOf(ControlPlanStateException.class);
        assertThatThrownBy(() -> plan.assignOwner(USER)).isInstanceOf(ControlPlanStateException.class);
    }

    @Test
    void aDraftCannotBeSealed() {
        ControlPlan draft = ControlPlan.create(TENANT, PRODUCT, ControlPlanPhase.PRODUCTION,
                "CP-1", USER, NOW);

        assertThatThrownBy(() -> draft.seal("0f5a", "sig", "tx-1"))
                .isInstanceOf(ControlPlanStateException.class)
                .hasMessageContaining("en vigueur");
    }

    @Test
    void anApprovedPlanCarriesItsProof() {
        ControlPlan plan = ControlPlan.create(TENANT, PRODUCT, ControlPlanPhase.PRODUCTION,
                "CP-1", USER, NOW);
        plan.approve(USER, NOW);

        plan.seal("0f5a", "sig", "tx-1");

        assertThat(plan.isSealed()).isTrue();
        assertThat(plan.getSealSha256()).isEqualTo("0f5a");
        assertThat(plan.getSealSignature()).isEqualTo("sig");
        assertThat(plan.getAnchorTxRef()).isEqualTo("tx-1");
    }

    @Test
    void aSealedPlanRefusesASecondSeal() {
        ControlPlan plan = ControlPlan.create(TENANT, PRODUCT, ControlPlanPhase.PRODUCTION,
                "CP-1", USER, NOW);
        plan.approve(USER, NOW);
        plan.seal("0f5a", "sig", "tx-1");

        assertThatThrownBy(() -> plan.seal("autre", "sig", "tx-2"))
                .isInstanceOf(ControlPlanStateException.class)
                .hasMessageContaining("déjà scellé");
    }

    @Test
    void anIncompleteSealIsRefusedRatherThanStoredHalfWay() {
        ControlPlan plan = ControlPlan.create(TENANT, PRODUCT, ControlPlanPhase.PRODUCTION,
                "CP-1", USER, NOW);
        plan.approve(USER, NOW);

        assertThatThrownBy(() -> plan.seal("0f5a", "  ", "tx-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signature");
        assertThat(plan.isSealed()).isFalse();
    }

    @Test
    void aRehydratedPlanKeepsItsStateWithoutReplayingAnyTransition() {
        ControlPlan plan = ControlPlan.rehydrate(UUID.randomUUID(), TENANT, PRODUCT,
                ControlPlanPhase.PRE_LAUNCH, "CP-9", 3, ControlPlanStatus.ACTIVE, USER,
                USER, NOW, USER, NOW, NOW, null, null, null, 0);

        assertThat(plan.getStatus()).isEqualTo(ControlPlanStatus.ACTIVE);
        assertThat(plan.getRevision()).isEqualTo(3);
        assertThat(plan.getApprovedBy()).isEqualTo(USER);
        assertThat(plan.getCreatedBy()).isEqualTo(USER);
        assertThat(plan.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void aRehydratedPlanWithoutStatusFallsBackToDraft() {
        ControlPlan plan = ControlPlan.rehydrate(UUID.randomUUID(), TENANT, PRODUCT,
                ControlPlanPhase.PROTOTYPE, "CP-9", 1, null, null, null, null, USER, NOW, NOW,
                null, null, null, 0);

        assertThat(plan.getStatus()).isEqualTo(ControlPlanStatus.DRAFT);
    }

    @Test
    void aLineNeedsAtLeastACharacteristicAndItsType() {
        assertThatThrownBy(() -> ControlPlanLine.create(TENANT, UUID.randomUUID(), 10, "  ",
                CharacteristicType.PRODUCT))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
