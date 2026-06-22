package com.openlab.qualitos.quality.academy.infrastructure;

import com.openlab.qualitos.quality.academy.domain.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {

    List<QuizQuestion> findByQuizIdOrderByOrderIndexAsc(UUID quizId);

    boolean existsByQuizIdAndOrderIndex(UUID quizId, int orderIndex);

    long countByQuizId(UUID quizId);
}
