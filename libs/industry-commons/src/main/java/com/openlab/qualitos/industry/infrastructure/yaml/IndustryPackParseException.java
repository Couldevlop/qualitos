package com.openlab.qualitos.industry.infrastructure.yaml;

/** Thrown when an Industry Pack YAML cannot be parsed or fails security checks. */
public class IndustryPackParseException extends RuntimeException {
  public IndustryPackParseException(String message) {
    super(message);
  }

  public IndustryPackParseException(String message, Throwable cause) {
    super(message, cause);
  }
}
