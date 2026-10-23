package com.openlab.qualitos.quality.training.competencymatrix.web;

import com.openlab.qualitos.quality.training.competencymatrix.domain.CompetencyMatrixConflictException;
import com.openlab.qualitos.quality.training.competencymatrix.domain.CompetencyMatrixNotFoundException;
import com.openlab.qualitos.quality.training.competencymatrix.domain.CompetencyMatrixValidationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Les refus de la matrice de compétences, en RFC 7807. Limité à ses deux
 * contrôleurs : le gestionnaire global n'a pas à la connaître.
 *
 * <p>404 : la personne ou la compétence n'est pas dans la matrice de CE client.
 * 409 : elle y est déjà. 422 : la saisie est bien formée mais refusée par la
 * règle (atelier inconnu, ateliers en double). {@code field} dit lequel, pour
 * que l'écran l'accroche au bon contrôle.
 */
@RestControllerAdvice(assignableTypes = {CompetencyMatrixController.class, CompetencyWorkshopsController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CompetencyMatrixExceptionHandler {

    @ExceptionHandler(CompetencyMatrixNotFoundException.class)
    public ProblemDetail handleNotFound(CompetencyMatrixNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), "competency-matrix-not-found",
                "Not In The Competency Matrix", null);
    }

    @ExceptionHandler(CompetencyMatrixConflictException.class)
    public ProblemDetail handleConflict(CompetencyMatrixConflictException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), "competency-matrix-conflict",
                "Already In The Competency Matrix", ex.getField());
    }

    @ExceptionHandler(CompetencyMatrixValidationException.class)
    public ProblemDetail handleValidation(CompetencyMatrixValidationException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), "competency-matrix-invalid",
                "Invalid Competency Matrix Entry", ex.getField());
    }

    private static ProblemDetail problem(HttpStatus status, String detail, String type, String title, String field) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("https://qualitos.io/errors/" + type));
        problem.setTitle(title);
        if (field != null) {
            problem.setProperty("field", field);
        }
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
