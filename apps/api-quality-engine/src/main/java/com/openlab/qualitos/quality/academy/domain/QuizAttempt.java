package com.openlab.qualitos.quality.academy.domain;

import com.openlab.qualitos.quality.academy.infrastructure.IntListJsonConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Tentative de quiz notée (auto-correction). Plusieurs tentatives sont permises ;
 * le meilleur score retenu pour la complétion est calculé côté service.
 *
 * <p>{@code answers} stocke les index choisis par l'apprenant, en JSON texte
 * (pattern projet TEXT + LONGVARCHAR).</p>
 */
@Entity
@Table(name = "academy_quiz_attempts",
        indexes = {
                @Index(name = "idx_academy_attempt_enrollment", columnList = "tenant_id, enrollment_id"),
                @Index(name = "idx_academy_attempt_quiz", columnList = "quiz_id")
        })
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private boolean passed;

    @Convert(converter = IntListJsonConverter.class)
    @Column(name = "answers_json", columnDefinition = "TEXT", nullable = false)
    @JdbcTypeCode(Types.LONGVARCHAR)
    private List<Integer> answers = List.of();

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    @PrePersist
    void prePersist() {
        if (attemptedAt == null) attemptedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(UUID enrollmentId) { this.enrollmentId = enrollmentId; }
    public UUID getQuizId() { return quizId; }
    public void setQuizId(UUID quizId) { this.quizId = quizId; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }
    public List<Integer> getAnswers() { return answers == null ? List.of() : answers; }
    public void setAnswers(List<Integer> answers) { this.answers = answers == null ? List.of() : List.copyOf(answers); }
    public Instant getAttemptedAt() { return attemptedAt; }
    public void setAttemptedAt(Instant attemptedAt) { this.attemptedAt = attemptedAt; }
}
