package com.openlab.qualitos.core.common;

import com.openlab.qualitos.core.tenant.TenantAlreadyExistsException;
import com.openlab.qualitos.core.tenant.TenantNotFoundException;
import com.openlab.qualitos.core.user.UserAlreadyExistsException;
import com.openlab.qualitos.core.user.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Returns 404 for TenantNotFoundException")
    void handlesTenantNotFound() {
        TenantNotFoundException ex = new TenantNotFoundException(UUID.randomUUID());
        ProblemDetail result = handler.handleTenantNotFound(ex);
        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getTitle()).isEqualTo("Tenant Not Found");
    }

    @Test
    @DisplayName("Returns 409 for TenantAlreadyExistsException")
    void handlesTenantConflict() {
        TenantAlreadyExistsException ex = new TenantAlreadyExistsException("slug");
        ProblemDetail result = handler.handleTenantAlreadyExists(ex);
        assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(result.getTitle()).isEqualTo("Tenant Already Exists");
    }

    @Test
    @DisplayName("Returns 404 for UserNotFoundException")
    void handlesUserNotFound() {
        UserNotFoundException ex = new UserNotFoundException(UUID.randomUUID());
        ProblemDetail result = handler.handleUserNotFound(ex);
        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getTitle()).isEqualTo("User Not Found");
    }

    @Test
    @DisplayName("Returns 409 for UserAlreadyExistsException")
    void handlesUserConflict() {
        UserAlreadyExistsException ex = new UserAlreadyExistsException("kc-id");
        ProblemDetail result = handler.handleUserAlreadyExists(ex);
        assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(result.getTitle()).isEqualTo("User Already Exists");
    }

    @Test
    @DisplayName("Returns 400 with field errors for MethodArgumentNotValidException")
    void handlesValidation() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "slug", "must match pattern");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ProblemDetail result = handler.handleValidation(ex);
        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getTitle()).isEqualTo("Validation Failed");
    }

    @Test
    @DisplayName("Returns 400 with field error having null message — uses fallback")
    void handlesValidationWithNullMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "slug", null, false, null, null, null);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ProblemDetail result = handler.handleValidation(ex);
        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Returns 401 for MissingTenantContextException")
    void handlesMissingTenant() {
        MissingTenantContextException ex = new MissingTenantContextException();
        ProblemDetail result = handler.handleMissingTenant(ex);
        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(result.getTitle()).isEqualTo("Missing Tenant Context");
    }

    @Test
    @DisplayName("Returns 500 for unhandled Exception")
    void handlesGeneric() {
        Exception ex = new RuntimeException("unexpected");
        ProblemDetail result = handler.handleGeneric(ex);
        assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getTitle()).isEqualTo("Internal Server Error");
    }
}
