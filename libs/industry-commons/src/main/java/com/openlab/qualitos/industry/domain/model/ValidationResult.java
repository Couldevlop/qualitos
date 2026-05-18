package com.openlab.qualitos.industry.domain.model;

import java.util.List;
import java.util.Objects;

/** Result of {@code IndustryPackProvider.validate()}. */
public record ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
  public ValidationResult {
    Objects.requireNonNull(errors);
    Objects.requireNonNull(warnings);
    errors = List.copyOf(errors);
    warnings = List.copyOf(warnings);
  }

  public static ValidationResult ok() {
    return new ValidationResult(true, List.of(), List.of());
  }

  public static ValidationResult ok(List<String> warnings) {
    return new ValidationResult(true, List.of(), warnings);
  }

  public static ValidationResult invalid(List<String> errors) {
    return new ValidationResult(false, errors, List.of());
  }
}
