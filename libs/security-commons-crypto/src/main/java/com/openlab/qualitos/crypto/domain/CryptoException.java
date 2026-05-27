package com.openlab.qualitos.crypto.domain;

/** Unchecked wrapper for low-level cryptographic failures (key parsing, provider errors). */
public class CryptoException extends RuntimeException {
  public CryptoException(String message, Throwable cause) {
    super(message, cause);
  }

  public CryptoException(String message) {
    super(message);
  }
}
