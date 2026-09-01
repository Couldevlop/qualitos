package com.openlab.qualitos.core.common;

import com.openlab.qualitos.core.billing.ModuleActivationFailedException;
import com.openlab.qualitos.core.billing.SubscriptionNotFoundException;
import com.openlab.qualitos.core.tenant.TenantAlreadyExistsException;
import com.openlab.qualitos.core.tenant.TenantNotFoundException;
import com.openlab.qualitos.core.user.UserAlreadyExistsException;
import com.openlab.qualitos.core.user.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TenantNotFoundException.class)
    public ProblemDetail handleTenantNotFound(TenantNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/tenant-not-found"));
        problem.setTitle("Tenant Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(TenantAlreadyExistsException.class)
    public ProblemDetail handleTenantAlreadyExists(TenantAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/tenant-conflict"));
        problem.setTitle("Tenant Already Exists");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/user-not-found"));
        problem.setTitle("User Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/user-conflict"));
        problem.setTitle("User Already Exists");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setType(URI.create("https://qualitos.io/errors/validation-failed"));
        problem.setTitle("Validation Failed");
        problem.setProperty("errors", errors);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler({
            org.springframework.security.access.AccessDeniedException.class,
            org.springframework.security.authorization.AuthorizationDeniedException.class})
    public ProblemDetail handleAccessDenied(RuntimeException ex) {
        // Un refus d'autorisation par @PreAuthorize (sécurité de méthode, comme sur
        // BillingProfileController) n'est PAS intercepté par la chaîne de filtres
        // Spring Security : l'exception surgit à l'intérieur du DispatcherServlet,
        // après que le filtre a déjà laissé passer la requête (l'utilisateur est
        // authentifié, seul son rôle est refusé). Sans ce handler explicite, le
        // catch-all Exception -> 500 ci-dessous la capturait en premier et masquait
        // le refus derrière une erreur serveur générique — un 403 devenait un 500,
        // ce qu'aucun test de rôle bien écrit ne devait laisser passer silencieusement.
        // Même correctif que le moteur de qualité (GlobalExceptionHandler, H1/C1).
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "Access denied");
        problem.setType(URI.create("https://qualitos.io/errors/access-denied"));
        problem.setTitle("Access Denied");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MissingTenantContextException.class)
    public ProblemDetail handleMissingTenant(MissingTenantContextException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/missing-tenant"));
        problem.setTitle("Missing Tenant Context");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(UnresolvableActorException.class)
    public ProblemDetail handleUnresolvableActor(UnresolvableActorException ex) {
        // 401, pas 500 : un sub absent ou non-UUID (jeton de compte de service,
        // principal non-JWT, claim personnalise) n'est pas une panne serveur —
        // c'est l'identite du principal qui n'est pas exploitable. Sans ce
        // handler, l'IllegalArgumentException de UUID.fromString() tombait dans
        // le catch-all Exception -> 500 ci-dessous, et une action d'administration
        // refusee pour cause d'identite illisible ressemblait a une panne.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/unresolvable-actor"));
        problem.setTitle("Unresolvable Actor");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ProblemDetail handleSubscriptionNotFound(SubscriptionNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/subscription-not-found"));
        problem.setTitle("Subscription Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ModuleActivationFailedException.class)
    public ProblemDetail handleModuleActivationFailed(ModuleActivationFailedException ex) {
        // 502 et non 500 : la panne n'est pas ici. api-core a fait son travail —
        // c'est le moteur de qualité, en aval, qui n'a pas appliqué la décision.
        // La distinction compte pour l'exploitation : un 500 enverrait chercher
        // le défaut dans le mauvais service, et un 503 laisserait croire que la
        // facturation elle-même est indisponible, ce qu'elle n'est pas.
        //
        // L'abonnement N'A PAS été enregistré (voir SubscriptionService) : rien
        // à défaire côté appelant, l'appel peut être rejoué tel quel.
        log.warn("quality engine refused a billing decision: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/module-activation-failed"));
        problem.setTitle("Module Activation Failed");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        // 409 : l'état du système s'oppose à l'action (module déjà souscrit,
        // abonnement déjà résilié, module sans tarif au catalogue). La requête
        // est bien formée — la refuser en 400 laisserait croire à une erreur de
        // saisie — et le serveur va bien — la refuser en 500 enverrait chercher
        // une panne inexistante. Rejouer l'appel à l'identique ne changera rien
        // tant que l'état n'aura pas changé, et c'est exactement ce que dit 409.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/conflicting-state"));
        problem.setTitle("Conflicting State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        // 400 : l'argument est refusé par une règle que les annotations de
        // validation ne peuvent pas exprimer, parce qu'elle porte sur DEUX
        // champs à la fois — « une exemption de facturation doit indiquer un
        // motif » (BillingProfileService). Sans ce handler, ce refus tombait
        // dans le catch-all ci-dessous : le client recevait un 500 « erreur
        // inattendue » pour une saisie qu'il pouvait corriger lui-même, et le
        // message expliquant quoi corriger était perdu en route.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/invalid-argument"));
        problem.setTitle("Invalid Argument");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setType(URI.create("https://qualitos.io/errors/internal-error"));
        problem.setTitle("Internal Server Error");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
