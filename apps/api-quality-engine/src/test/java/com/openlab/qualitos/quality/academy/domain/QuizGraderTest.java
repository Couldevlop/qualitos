package com.openlab.qualitos.quality.academy.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class QuizGraderTest {

    private QuizQuestion q(int correctIndex, int points, int order) {
        QuizQuestion q = new QuizQuestion();
        q.setId(UUID.randomUUID());
        q.setStem("Question " + order);
        q.setOptions(List.of("A", "B", "C", "D"));
        q.setCorrectIndex(correctIndex);
        q.setPoints(points);
        q.setOrderIndex(order);
        return q;
    }

    @Test
    void allCorrect_scores100_andPasses() {
        List<QuizQuestion> qs = List.of(q(0, 1, 0), q(1, 1, 1), q(2, 1, 2));
        QuizGrader.Result r = QuizGrader.grade(qs, List.of(0, 1, 2), 70);
        assertThat(r.score()).isEqualTo(100);
        assertThat(r.passed()).isTrue();
        assertThat(r.earnedPoints()).isEqualTo(3);
        assertThat(r.totalPoints()).isEqualTo(3);
    }

    @Test
    void allWrong_scores0_andFails() {
        List<QuizQuestion> qs = List.of(q(0, 1, 0), q(1, 1, 1));
        QuizGrader.Result r = QuizGrader.grade(qs, List.of(3, 3), 70);
        assertThat(r.score()).isZero();
        assertThat(r.passed()).isFalse();
        assertThat(r.earnedPoints()).isZero();
    }

    @Test
    void weightedScore_roundsToNearest() {
        // Q1 vaut 3 points (juste), Q2 vaut 1 point (faux) → 3/4 = 75 %.
        List<QuizQuestion> qs = List.of(q(0, 3, 0), q(0, 1, 1));
        QuizGrader.Result r = QuizGrader.grade(qs, List.of(0, 2), 70);
        assertThat(r.score()).isEqualTo(75);
        assertThat(r.passed()).isTrue();
    }

    @Test
    void exactlyAtThreshold_passes() {
        List<QuizQuestion> qs = List.of(q(0, 1, 0), q(0, 1, 1), q(0, 1, 2), q(0, 1, 3));
        // 3/4 = 75 % ; seuil 75 → réussite (>=).
        QuizGrader.Result r = QuizGrader.grade(qs, List.of(0, 0, 0, 1), 75);
        assertThat(r.score()).isEqualTo(75);
        assertThat(r.passed()).isTrue();
    }

    @Test
    void missingAnswers_areCountedWrong() {
        List<QuizQuestion> qs = List.of(q(0, 1, 0), q(1, 1, 1), q(2, 1, 2));
        // Une seule réponse fournie (la bonne) → 1/3 ≈ 33 %.
        QuizGrader.Result r = QuizGrader.grade(qs, List.of(0), 70);
        assertThat(r.score()).isEqualTo(33);
        assertThat(r.passed()).isFalse();
    }

    @Test
    void outOfBoundsAnswerIndex_isWrong_notError() {
        List<QuizQuestion> qs = List.of(q(0, 1, 0));
        QuizGrader.Result r = QuizGrader.grade(qs, List.of(99), 50);
        assertThat(r.score()).isZero();
    }

    @Test
    void emptyQuiz_isTrivialPass() {
        QuizGrader.Result r = QuizGrader.grade(List.of(), List.of(), 70);
        assertThat(r.score()).isEqualTo(100);
        assertThat(r.passed()).isTrue();
        assertThat(r.totalPoints()).isZero();
    }

    @Test
    void nullAnswers_handledAsAllWrong() {
        List<QuizQuestion> qs = List.of(q(0, 1, 0));
        QuizGrader.Result r = QuizGrader.grade(qs, null, 1);
        assertThat(r.score()).isZero();
        assertThat(r.passed()).isFalse();
    }
}
