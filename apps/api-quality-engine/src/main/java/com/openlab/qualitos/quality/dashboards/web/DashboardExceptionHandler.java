package com.openlab.qualitos.quality.dashboards.web;

import com.openlab.qualitos.quality.common.ProblemDetails;
import com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotationForbiddenException;
import com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotationNotFoundException;
import com.openlab.qualitos.quality.dashboards.domain.DashboardLayoutNotFoundException;
import com.openlab.qualitos.quality.dashboards.domain.DashboardLayoutStateException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Les refus du module dashboards, dits par le module lui-meme.
 *
 * <p>Ces gestionnaires vivaient dans {@code common/GlobalExceptionHandler}, qui
 * devait importer l exception de chaque module pour savoir quel statut lui
 * repondre. Le socle dependait ainsi de soixante-six modules, et tout module
 * neuf devait MODIFIER un fichier partage pour que ses refus sortent avec le bon
 * statut.
 *
 * <p>{@code @Order(HIGHEST_PRECEDENCE)} : Spring consulte les conseils dans
 * l ordre et retient le PREMIER qui possede un gestionnaire applicable. Le socle
 * garde un filet {@code @ExceptionHandler(Exception.class)} qui accepte tout ;
 * sans cette priorite, il repondrait 500 a la place de ce module.
 *
 * <p>La portee reste GLOBALE, sans {@code assignableTypes} : une exception de ce
 * module peut etre levee pendant une requete adressee a un autre module -- un
 * ecart qui ouvre un dossier CAPA, par exemple -- et elle doit garder son statut.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class DashboardExceptionHandler {


    // ----- P5: dashboards & marketplace -----

    
    @ExceptionHandler(DashboardLayoutNotFoundException.class)
    public ProblemDetail handleDashboardNotFound(DashboardLayoutNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "dashboard-not-found", "Dashboard Not Found");
    }

    @ExceptionHandler(DashboardLayoutStateException.class)
    public ProblemDetail handleDashboardState(DashboardLayoutStateException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(),
                "dashboard-invalid-state", "Invalid Dashboard State");
    }

    @ExceptionHandler(com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotationNotFoundException.class)
    public ProblemDetail handleAnnotationNotFound(com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotationNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "dashboard-annotation-not-found", "Dashboard Annotation Not Found");
    }

    @ExceptionHandler(com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotationForbiddenException.class)
    public ProblemDetail handleAnnotationForbidden(com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotationForbiddenException ex) {
        return ProblemDetails.of(HttpStatus.FORBIDDEN, ex.getMessage(),
                "dashboard-annotation-forbidden", "Dashboard Annotation Forbidden");
    }
}
