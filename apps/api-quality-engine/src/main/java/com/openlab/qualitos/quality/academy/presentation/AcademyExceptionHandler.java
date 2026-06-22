package com.openlab.qualitos.quality.academy.presentation;

import com.openlab.qualitos.quality.academy.domain.AcademyConflictException;
import com.openlab.qualitos.quality.academy.domain.AcademyNotFoundException;
import com.openlab.qualitos.quality.academy.domain.AcademyStateException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Traduit les exceptions du module Academy en réponses RFC 7807 (ProblemDetail),
 * comme les autres modules à architecture propre (cf. MockAuditExceptionHandler).
 */
@RestControllerAdvice(basePackages = "com.openlab.qualitos.quality.academy")
public class AcademyExceptionHandler {

    @ExceptionHandler(AcademyNotFoundException.class)
    public ProblemDetail handleNotFound(AcademyNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/academy-not-found"));
        problem.setTitle("Academy Resource Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AcademyStateException.class)
    public ProblemDetail handleState(AcademyStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/academy-invalid-state"));
        problem.setTitle("Invalid Academy State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AcademyConflictException.class)
    public ProblemDetail handleConflict(AcademyConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/academy-conflict"));
        problem.setTitle("Academy Conflict");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
