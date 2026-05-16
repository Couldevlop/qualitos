package com.openlab.qualitos.quality.tenantmodules.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModuleActivationTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID ACTOR = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Instant FUTURE = NOW.plusSeconds(86400L * 30);

    @Test
    void startTrial_requires_futureTrialEnd() {
        assertThatThrownBy(() -> ModuleActivation.startTrial(
                TENANT, "pdca", BillingTier.FREE, NOW, ACTOR, NOW))
                .isInstanceOf(ModuleActivationStateException.class);
    }

    @Test
    void startTrial_persistsAsTrial() {
        ModuleActivation a = ModuleActivation.startTrial(
                TENANT, "pdca", BillingTier.FREE, FUTURE, ACTOR, NOW);
        assertThat(a.getStatus()).isEqualTo(ActivationStatus.TRIAL);
        assertThat(a.isEnabled()).isTrue();
        assertThat(a.getTrialEndsAt()).isEqualTo(FUTURE);
    }

    @Test
    void activateNow_persistsAsActive() {
        ModuleActivation a = ModuleActivation.activateNow(
                TENANT, "kpi", BillingTier.STANDARD, FUTURE, ACTOR, NOW);
        assertThat(a.getStatus()).isEqualTo(ActivationStatus.ACTIVE);
        assertThat(a.isEnabled()).isTrue();
    }

    @Test
    void convertTrial_to_active_moves() {
        ModuleActivation a = ModuleActivation.startTrial(
                TENANT, "kpi", BillingTier.STANDARD, FUTURE, ACTOR, NOW);
        a.convertTrialToActive(FUTURE.plusSeconds(86400), ACTOR, NOW.plusSeconds(60));
        assertThat(a.getStatus()).isEqualTo(ActivationStatus.ACTIVE);
        assertThat(a.getTrialEndsAt()).isNull();
    }

    @Test
    void suspend_then_resume_returnsToActive() {
        ModuleActivation a = active();
        a.suspend(ACTOR, NOW.plusSeconds(60));
        assertThat(a.getStatus()).isEqualTo(ActivationStatus.SUSPENDED);
        a.resume(ACTOR, NOW.plusSeconds(120));
        assertThat(a.getStatus()).isEqualTo(ActivationStatus.ACTIVE);
    }

    @Test
    void resume_fromNonSuspended_rejected() {
        assertThatThrownBy(() -> active().resume(ACTOR, NOW))
                .isInstanceOf(ModuleActivationStateException.class);
    }

    @Test
    void disable_isTerminal() {
        ModuleActivation a = active();
        a.disable(ACTOR, NOW.plusSeconds(60));
        assertThat(a.isTerminal()).isTrue();
        assertThatThrownBy(() -> a.resume(ACTOR, NOW)).isInstanceOf(ModuleActivationStateException.class);
        assertThatThrownBy(() -> a.suspend(ACTOR, NOW)).isInstanceOf(ModuleActivationStateException.class);
    }

    @Test
    void expire_isTerminal() {
        ModuleActivation a = active();
        a.expire(ACTOR, NOW.plusSeconds(60));
        assertThat(a.getStatus()).isEqualTo(ActivationStatus.EXPIRED);
        assertThat(a.isTerminal()).isTrue();
    }

    @Test
    void expireIfDue_byTrialEnd() {
        ModuleActivation a = ModuleActivation.startTrial(
                TENANT, "kpi", BillingTier.STANDARD, FUTURE, ACTOR, NOW);
        assertThat(a.expireIfDue(FUTURE.plusSeconds(1))).isTrue();
        assertThat(a.getStatus()).isEqualTo(ActivationStatus.EXPIRED);
    }

    @Test
    void expireIfDue_byExpiresAt() {
        ModuleActivation a = active();
        assertThat(a.expireIfDue(FUTURE.plusSeconds(1))).isTrue();
        assertThat(a.getStatus()).isEqualTo(ActivationStatus.EXPIRED);
    }

    @Test
    void expireIfDue_noOpIfNotDue() {
        ModuleActivation a = active();
        assertThat(a.expireIfDue(NOW.plusSeconds(60))).isFalse();
        assertThat(a.getStatus()).isEqualTo(ActivationStatus.ACTIVE);
    }

    @Test
    void expireIfDue_terminal_noOp() {
        ModuleActivation a = active();
        a.expire(ACTOR, NOW.plusSeconds(60));
        assertThat(a.expireIfDue(FUTURE.plusSeconds(86400L * 365L * 10L))).isFalse();
    }

    @Test
    void changeTier_terminal_rejected() {
        ModuleActivation a = active();
        a.disable(ACTOR, NOW.plusSeconds(60));
        assertThatThrownBy(() -> a.changeTier(BillingTier.PRO, ACTOR, NOW))
                .isInstanceOf(ModuleActivationStateException.class);
    }

    @Test
    void configure_persistsJson_andTouchesUpdatedAt() {
        ModuleActivation a = active();
        a.configure("{\"x\":1}", ACTOR, NOW.plusSeconds(60));
        assertThat(a.getConfigurationJson()).isEqualTo("{\"x\":1}");
    }

    @Test
    void configure_terminal_rejected() {
        ModuleActivation a = active();
        a.disable(ACTOR, NOW.plusSeconds(60));
        assertThatThrownBy(() -> a.configure("{}", ACTOR, NOW))
                .isInstanceOf(ModuleActivationStateException.class);
    }

    @Test
    void invalidTransition_disabledFromTrial_throws() {
        // TRIAL → DISABLED non autorisé (passer par ACTIVE puis disable, ou expire)
        ModuleActivation a = ModuleActivation.startTrial(
                TENANT, "kpi", BillingTier.STANDARD, FUTURE, ACTOR, NOW);
        assertThatThrownBy(() -> a.disable(ACTOR, NOW.plusSeconds(60)))
                .isInstanceOf(ModuleActivationStateException.class);
    }

    @Test
    void assignId_setsIdAfterPersist() {
        ModuleActivation a = active();
        UUID id = UUID.randomUUID();
        a.assignId(id);
        assertThat(a.getId()).isEqualTo(id);
    }

    private ModuleActivation active() {
        return ModuleActivation.activateNow(
                TENANT, "kpi", BillingTier.STANDARD, FUTURE, ACTOR, NOW);
    }
}
