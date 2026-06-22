package com.openlab.qualitos.quality.dashboards.export.application;

/**
 * Port — builds the absolute public verification URL embedded in the QR code,
 * given a verification code. The base URL is configured per-environment in the
 * infrastructure adapter (never hard-coded in the domain/application).
 */
public interface VerifyUrlBuilder {
    String verifyUrl(String verificationCode);
}
