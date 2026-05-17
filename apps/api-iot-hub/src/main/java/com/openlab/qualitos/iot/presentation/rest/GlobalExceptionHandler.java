package com.openlab.qualitos.iot.presentation.rest;

import com.openlab.qualitos.iot.application.usecase.IngestTelemetryUseCase;
import com.openlab.qualitos.iot.application.usecase.RegisterDeviceUseCase;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * RFC 7807 ProblemDetail handlers — uniform error contract.
 *
 * <p>OWASP A05 — never leak stack traces or internal class names.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(RegisterDeviceUseCase.DeviceAlreadyExistsException.class)
  public ProblemDetail handleDuplicateDevice(RegisterDeviceUseCase.DeviceAlreadyExistsException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    pd.setType(URI.create("https://qualitos.local/errors/device-already-exists"));
    pd.setTitle("Device already exists");
    return pd;
  }

  @ExceptionHandler(IngestTelemetryUseCase.DeviceNotFoundException.class)
  public ProblemDetail handleDeviceMissing(IngestTelemetryUseCase.DeviceNotFoundException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    pd.setType(URI.create("https://qualitos.local/errors/device-not-found"));
    pd.setTitle("Device not found");
    return pd;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
    String detail = ex.getBindingResult().getFieldErrors().stream()
        .map(e -> e.getField() + ": " + e.getDefaultMessage())
        .reduce((a, b) -> a + "; " + b)
        .orElse("Validation failed");
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    pd.setType(URI.create("https://qualitos.local/errors/validation"));
    pd.setTitle("Validation failed");
    return pd;
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ProblemDetail handleConstraint(ConstraintViolationException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    pd.setType(URI.create("https://qualitos.local/errors/validation"));
    pd.setTitle("Validation failed");
    return pd;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArg(IllegalArgumentException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    pd.setType(URI.create("https://qualitos.local/errors/bad-request"));
    pd.setTitle("Bad request");
    return pd;
  }

  @ExceptionHandler(IllegalStateException.class)
  public ProblemDetail handleIllegalState(IllegalStateException ex) {
    // tenant context errors -> 401
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    pd.setType(URI.create("https://qualitos.local/errors/unauthorized"));
    pd.setTitle("Unauthorized");
    return pd;
  }
}
