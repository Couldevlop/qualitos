package com.openlab.qualitos.quality.product.web;

import com.openlab.qualitos.quality.common.ProblemDetails;
import com.openlab.qualitos.quality.product.domain.ProductCodeConflictException;
import com.openlab.qualitos.quality.product.domain.ProductNotFoundException;
import com.openlab.qualitos.quality.product.domain.ProductStateException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Les refus du module product, dits par le module lui-meme.
 *
 * <p>Ces gestionnaires vivaient dans {@code common/GlobalExceptionHandler}, qui
 * devait importer l exception de chaque module pour savoir quel statut lui
 * repondre. Le socle dependait ainsi de soixante-six modules, et tout module
 * neuf devait MODIFIER un fichier partage pour que ses refus sortent avec le bon
 * statut (ADR 0128).
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
public class ProductExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "product-not-found", "Product Not Found");
    }

    @ExceptionHandler(ProductStateException.class)
    public ProblemDetail handleProductState(ProductStateException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(),
                "product-invalid-state", "Invalid Product State");
    }

    @ExceptionHandler(ProductCodeConflictException.class)
    public ProblemDetail handleProductCodeConflict(ProductCodeConflictException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(),
                "product-code-conflict", "Product Code Conflict");
    }
}
