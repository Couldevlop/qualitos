package com.openlab.qualitos.quality.gdpr.domain;

/**
 * Types de demande RGPD :
 *   ACCESS        — Article 15
 *   ERASURE       — Article 17 (droit à l'oubli)
 *   PORTABILITY   — Article 20
 *   RECTIFICATION — Article 16
 *   RESTRICTION   — Article 18
 *   OBJECTION     — Article 21
 */
public enum SubjectRequestType {
    ACCESS, ERASURE, PORTABILITY, RECTIFICATION, RESTRICTION, OBJECTION
}
