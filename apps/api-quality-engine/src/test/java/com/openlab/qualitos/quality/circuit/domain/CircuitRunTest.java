package com.openlab.qualitos.quality.circuit.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CircuitRunTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID DOC = UUID.randomUUID();
    static final UUID AUTHOR = UUID.randomUUID();
    static final UUID ALICE = UUID.randomUUID();
    static final UUID BOB = UUID.randomUUID();
    static final UUID CHLOE = UUID.randomUUID();
    static final Instant T0 = Instant.parse("2026-10-08T08:00:00Z");

    static final Set<String> MANAGER = Set.of("QUALITY_MANAGER");
    static final Set<String> DIRECTOR = Set.of("QUALITY_DIRECTOR");

    /** Relecture par deux managers, puis signature de la direction. */
    static CircuitRun deuxEtapes() {
        return CircuitRun.start(TENANT, CircuitSubject.DOCUMENT_VERSION, DOC, AUTHOR, List.of(
                new CircuitStep("Relecture", "QUALITY_MANAGER", 2),
                new CircuitStep("Signature", "QUALITY_DIRECTOR", 1)), T0);
    }

    static CircuitRun.Decision approuve(CircuitRun run, UUID qui, Set<String> roles) {
        return run.decide(qui, roles, true, null, T0.plusSeconds(60));
    }

    @Nested
    class Depart {

        @Test
        void startsAtTheFirstStep_inProgress() {
            CircuitRun run = deuxEtapes();
            assertThat(run.status()).isEqualTo(CircuitRun.Status.IN_PROGRESS);
            assertThat(run.currentStep()).isZero();
            assertThat(run.decisions()).isEmpty();
            assertThat(run.endedAt()).isNull();
        }

        @Test
        void aCircuitWithoutStep_isRefused() {
            assertThatThrownBy(() -> CircuitRun.start(TENANT, CircuitSubject.DOCUMENT_VERSION, DOC, AUTHOR,
                    List.of(), T0))
                    .isInstanceOf(CircuitException.class)
                    .extracting(e -> ((CircuitException) e).getReason())
                    .isEqualTo(CircuitException.Reason.INVALID);
        }

        @Test
        void theStepsAreCopied_soLaterChangesDoNotAffectTheRun() {
            List<CircuitStep> reglage = new java.util.ArrayList<>(List.of(new CircuitStep("R", "QUALITY_MANAGER", 1)));
            CircuitRun run = CircuitRun.start(TENANT, CircuitSubject.DOCUMENT_VERSION, DOC, AUTHOR, reglage, T0);
            reglage.add(new CircuitStep("S", "QUALITY_DIRECTOR", 1));
            assertThat(run.steps()).hasSize(1);
        }
    }

    @Nested
    class Avancement {

        @Test
        void oneApprovalOutOfTwo_staysOnTheStep() {
            CircuitRun run = deuxEtapes();
            approuve(run, ALICE, MANAGER);
            assertThat(run.currentStep()).isZero();
            assertThat(run.approvalsAt(0)).isEqualTo(1);
        }

        @Test
        void enoughDistinctApprovals_moveToTheNextStep() {
            CircuitRun run = deuxEtapes();
            approuve(run, ALICE, MANAGER);
            approuve(run, BOB, MANAGER);
            assertThat(run.currentStep()).isEqualTo(1);
            assertThat(run.status()).isEqualTo(CircuitRun.Status.IN_PROGRESS);
        }

        @Test
        void theLastStep_approvesTheRun() {
            CircuitRun run = deuxEtapes();
            approuve(run, ALICE, MANAGER);
            approuve(run, BOB, MANAGER);
            approuve(run, CHLOE, DIRECTOR);
            assertThat(run.status()).isEqualTo(CircuitRun.Status.APPROVED);
            assertThat(run.endedAt()).isNotNull();
            assertThat(run.decisions()).hasSize(3);
        }

        @Test
        void theCommentIsKept_stripped() {
            CircuitRun run = deuxEtapes();
            CircuitRun.Decision d = run.decide(ALICE, MANAGER, true, "  RAS  ", T0);
            assertThat(d.comment()).isEqualTo("RAS");
            assertThat(d.stepIndex()).isZero();
        }
    }

    @Nested
    class Refus {

        @Test
        void aRejection_endsTheRun() {
            CircuitRun run = deuxEtapes();
            run.decide(ALICE, MANAGER, false, "Section 4 incomplète", T0);
            assertThat(run.status()).isEqualTo(CircuitRun.Status.REJECTED);
            assertThat(run.endedAt()).isEqualTo(T0);
        }

        @Test
        void aRejection_withoutReason_isRefused() {
            CircuitRun run = deuxEtapes();
            assertThatThrownBy(() -> run.decide(ALICE, MANAGER, false, "  ", T0))
                    .isInstanceOf(CircuitException.class)
                    .hasMessageContaining("motive");
            assertThat(run.decisions()).isEmpty();
        }

        @Test
        void aTooLongComment_isRefused() {
            CircuitRun run = deuxEtapes();
            String long_ = "x".repeat(CircuitRun.COMMENT_MAX + 1);
            assertThatThrownBy(() -> run.decide(ALICE, MANAGER, true, long_, T0))
                    .isInstanceOf(CircuitException.class)
                    .extracting(e -> ((CircuitException) e).getField())
                    .isEqualTo("comment");
        }
    }

    @Nested
    class QuatreYeux {

        @Test
        void theAuthorNeverDecides_evenWithTheRole() {
            CircuitRun run = deuxEtapes();
            assertThatThrownBy(() -> approuve(run, AUTHOR, MANAGER))
                    .isInstanceOf(CircuitException.class)
                    .extracting(e -> ((CircuitException) e).getReason())
                    .isEqualTo(CircuitException.Reason.AUTHOR_CANNOT_DECIDE);
        }

        @Test
        void withoutTheRoleOfTheStep_refused() {
            CircuitRun run = deuxEtapes();
            assertThatThrownBy(() -> approuve(run, CHLOE, DIRECTOR))
                    .isInstanceOf(CircuitException.class)
                    .extracting(e -> ((CircuitException) e).getReason())
                    .isEqualTo(CircuitException.Reason.NOT_YOUR_STEP);
        }

        @Test
        void thesamePerson_cannotCountTwice_onTheSameStep() {
            CircuitRun run = deuxEtapes();
            approuve(run, ALICE, MANAGER);
            assertThatThrownBy(() -> approuve(run, ALICE, MANAGER))
                    .isInstanceOf(CircuitException.class)
                    .extracting(e -> ((CircuitException) e).getReason())
                    .isEqualTo(CircuitException.Reason.ALREADY_DECIDED);
        }

        @Test
        void thesamePerson_cannotDecideAgain_onALaterStep() {
            CircuitRun run = deuxEtapes();
            Set<String> lesDeux = Set.of("QUALITY_MANAGER", "QUALITY_DIRECTOR");
            approuve(run, ALICE, lesDeux);
            approuve(run, BOB, MANAGER);
            assertThatThrownBy(() -> approuve(run, ALICE, lesDeux))
                    .extracting(e -> ((CircuitException) e).getReason())
                    .isEqualTo(CircuitException.Reason.ALREADY_DECIDED);
        }

        @Test
        void aFinishedRun_takesNoMoreDecision() {
            CircuitRun run = deuxEtapes();
            run.decide(ALICE, MANAGER, false, "non", T0);
            assertThatThrownBy(() -> approuve(run, BOB, MANAGER))
                    .extracting(e -> ((CircuitException) e).getReason())
                    .isEqualTo(CircuitException.Reason.NOT_IN_PROGRESS);
        }
    }

    @Nested
    class Etapes {

        @Test
        void aStep_needsAName() {
            assertThatThrownBy(() -> new CircuitStep("  ", "QUALITY_MANAGER", 1)).isInstanceOf(CircuitException.class);
        }

        @Test
        void aStep_needsAValidRoleCode() {
            assertThatThrownBy(() -> new CircuitStep("R", "manager", 1)).isInstanceOf(CircuitException.class);
            assertThatThrownBy(() -> new CircuitStep("R", null, 1)).isInstanceOf(CircuitException.class);
        }

        @Test
        void theSuperAdmin_neverApprovesInATenant() {
            assertThatThrownBy(() -> new CircuitStep("R", "SUPER_ADMIN", 1))
                    .isInstanceOf(CircuitException.class)
                    .hasMessageContaining("éditeur");
        }

        @Test
        void minApprovals_between1And10() {
            assertThatThrownBy(() -> new CircuitStep("R", "QUALITY_MANAGER", 0)).isInstanceOf(CircuitException.class);
            assertThatThrownBy(() -> new CircuitStep("R", "QUALITY_MANAGER", 11)).isInstanceOf(CircuitException.class);
            assertThat(new CircuitStep(" R ", "QUALITY_MANAGER", 10).name()).isEqualTo("R");
        }

        @Test
        void subjects_areFoundByCode() {
            assertThat(CircuitSubject.fromCode("document-version")).isEqualTo(CircuitSubject.DOCUMENT_VERSION);
            assertThatThrownBy(() -> CircuitSubject.fromCode("nc"))
                    .extracting(e -> ((CircuitException) e).getReason())
                    .isEqualTo(CircuitException.Reason.UNKNOWN_SUBJECT);
        }
    }
}
