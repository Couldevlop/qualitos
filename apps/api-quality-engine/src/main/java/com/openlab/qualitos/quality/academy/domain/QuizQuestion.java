package com.openlab.qualitos.quality.academy.domain;

import com.openlab.qualitos.quality.academy.infrastructure.StringListJsonConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Question de quiz à choix unique (QCM auto-corrigé).
 *
 * <p>Les options de réponse sont stockées en JSON texte ({@code options_json}) via
 * {@link StringListJsonConverter} — pattern projet {@code @Column(columnDefinition="TEXT")}
 * + {@code @JdbcTypeCode(LONGVARCHAR)}. La bonne réponse est l'index
 * {@code correctIndex} (0-based) dans la liste d'options ; elle n'est JAMAIS
 * exposée à l'apprenant (filtrée côté DTO).</p>
 */
@Entity
@Table(name = "academy_quiz_questions",
        uniqueConstraints = @UniqueConstraint(name = "uk_academy_question_quiz_order",
                columnNames = {"quiz_id", "order_index"}),
        indexes = @Index(name = "idx_academy_question_quiz", columnList = "quiz_id"))
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    @Column(nullable = false, length = 1000)
    private String stem;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "options_json", columnDefinition = "TEXT", nullable = false)
    @JdbcTypeCode(Types.LONGVARCHAR)
    private List<String> options = List.of();

    @Column(name = "correct_index", nullable = false)
    private int correctIndex;

    @Column(nullable = false)
    private int points;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (points == 0) points = 1;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getQuizId() { return quizId; }
    public void setQuizId(UUID quizId) { this.quizId = quizId; }
    public String getStem() { return stem; }
    public void setStem(String stem) { this.stem = stem; }
    public List<String> getOptions() { return options == null ? List.of() : options; }
    public void setOptions(List<String> options) { this.options = options == null ? List.of() : List.copyOf(options); }
    public int getCorrectIndex() { return correctIndex; }
    public void setCorrectIndex(int correctIndex) { this.correctIndex = correctIndex; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
