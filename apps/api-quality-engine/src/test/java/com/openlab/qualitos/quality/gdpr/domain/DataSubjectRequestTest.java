package com.openlab.qualitos.quality.gdpr.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class DataSubjectRequestTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID H = UUID.randomUUID();
    static final String HASH = "a".repeat(64);
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void receive_setsDefaultDeadlineAt30Days() {
        DataSubjectRequest r = DataSubjectRequest.receive(
                T, SubjectRequestType.ACCESS, HASH, "label", U, NOW);
        assertThat(r.getStatus()).isEqualTo(SubjectRequestStatus.RECEIVED);
        assertThat(r.getReceivedAt()).isEqualTo(NOW);
        assertThat(r.getDeadlineAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
        assertThat(r.isExtended()).isFalse();
        assertThat(r.isTerminal()).isFalse();
        assertThat(r.getSubjectIdentifierHash()).isEqualTo(HASH);
    }

    @Test
    void receive_blankHash_throws() {
        assertThatThrownBy(() -> DataSubjectRequest.receive(
                T, SubjectRequestType.ACCESS, "", "x", U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void startProcessing_fromReceived_movesToInProgress() {
        DataSubjectRequest r = fresh();
        r.startProcessing(H, NOW.plusSeconds(60));
        assertThat(r.getStatus()).isEqualTo(SubjectRequestStatus.IN_PROGRESS);
        assertThat(r.getHandledByUserId()).isEqualTo(H);
        assertThat(r.getInProgressAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void startProcessing_fromTerminal_rejected() {
        DataSubjectRequest r = fresh();
        r.reject("denied", H, NOW);
        assertThatThrownBy(() -> r.startProcessing(H, NOW.plusSeconds(60)))
                .isInstanceOf(SubjectRequestStateException.class);
    }

    @Test
    void complete_requiresResolutionNotes() {
        DataSubjectRequest r = fresh();
        r.startProcessing(H, NOW.plusSeconds(60));
        assertThatThrownBy(() -> r.complete("  ", null, H, NOW.plusSeconds(120)))
                .isInstanceOf(SubjectRequestStateException.class);
    }

    @Test
    void complete_fromInProgress_movesToCompleted() {
        DataSubjectRequest r = fresh();
        r.startProcessing(H, NOW.plusSeconds(60));
        r.complete("Done", "https://evidence", H, NOW.plusSeconds(120));
        assertThat(r.getStatus()).isEqualTo(SubjectRequestStatus.COMPLETED);
        assertThat(r.getResolutionNotes()).isEqualTo("Done");
        assertThat(r.getEvidenceUrl()).isEqualTo("https://evidence");
        assertThat(r.isTerminal()).isTrue();
    }

    @Test
    void reject_fromReceived_movesToRejected() {
        DataSubjectRequest r = fresh();
        r.reject("Out of scope", H, NOW.plusSeconds(60));
        assertThat(r.getStatus()).isEqualTo(SubjectRequestStatus.REJECTED);
        assertThat(r.getRejectionReason()).isEqualTo("Out of scope");
        assertThat(r.isTerminal()).isTrue();
    }

    @Test
    void reject_blankReason_throws() {
        DataSubjectRequest r = fresh();
        assertThatThrownBy(() -> r.reject(" ", H, NOW))
                .isInstanceOf(SubjectRequestStateException.class);
    }

    @Test
    void extendDeadline_oncePermitted_withinPlus60() {
        DataSubjectRequest r = fresh();
        Instant newDeadline = NOW.plus(Duration.ofDays(60));
        r.extendDeadline(newDeadline, NOW);
        assertThat(r.getDeadlineAt()).isEqualTo(newDeadline);
        assertThat(r.isExtended()).isTrue();
    }

    @Test
    void extendDeadline_secondAttempt_rejected() {
        DataSubjectRequest r = fresh();
        r.extendDeadline(NOW.plus(Duration.ofDays(60)), NOW);
        assertThatThrownBy(() -> r.extendDeadline(NOW.plus(Duration.ofDays(80)), NOW))
                .isInstanceOf(SubjectRequestStateException.class)
                .hasMessageContaining("already extended");
    }

    @Test
    void extendDeadline_beyond90Days_rejected() {
        DataSubjectRequest r = fresh();
        assertThatThrownBy(() -> r.extendDeadline(NOW.plus(Duration.ofDays(91)), NOW))
                .isInstanceOf(SubjectRequestStateException.class);
    }

    @Test
    void extendDeadline_notAfterCurrent_rejected() {
        DataSubjectRequest r = fresh();
        assertThatThrownBy(() -> r.extendDeadline(r.getDeadlineAt(), NOW))
                .isInstanceOf(SubjectRequestStateException.class);
    }

    @Test
    void extendDeadline_onTerminal_rejected() {
        DataSubjectRequest r = fresh();
        r.reject("nope", H, NOW);
        assertThatThrownBy(() -> r.extendDeadline(NOW.plus(Duration.ofDays(60)), NOW))
                .isInstanceOf(SubjectRequestStateException.class);
    }

    @Test
    void isOverdue_trueAfterDeadlineWhenNonTerminal() {
        DataSubjectRequest r = fresh();
        assertThat(r.isOverdue(NOW.plus(Duration.ofDays(31)))).isTrue();
        assertThat(r.isOverdue(NOW.plus(Duration.ofDays(15)))).isFalse();
    }

    @Test
    void isOverdue_falseWhenTerminal() {
        DataSubjectRequest r = fresh();
        r.reject("done", H, NOW);
        assertThat(r.isOverdue(NOW.plus(Duration.ofDays(31)))).isFalse();
    }

    @Test
    void completed_thenTransition_rejected() {
        DataSubjectRequest r = fresh();
        r.startProcessing(H, NOW);
        r.complete("ok", null, H, NOW);
        assertThatThrownBy(() -> r.startProcessing(H, NOW))
                .isInstanceOf(SubjectRequestStateException.class);
        assertThatThrownBy(() -> r.reject("x", H, NOW))
                .isInstanceOf(SubjectRequestStateException.class);
    }

    private DataSubjectRequest fresh() {
        return DataSubjectRequest.receive(T, SubjectRequestType.ACCESS, HASH, "label", U, NOW);
    }
}
