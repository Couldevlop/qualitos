package com.openlab.qualitos.quality.ropa.domain;

/**
 * Bases légales du traitement — RGPD Art. 6§1 (et Art. 9§2 pour les
 * catégories particulières).
 */
public enum LawfulBasis {
    CONSENT,              // Art. 6.1.a
    CONTRACT,             // Art. 6.1.b
    LEGAL_OBLIGATION,     // Art. 6.1.c
    VITAL_INTERESTS,      // Art. 6.1.d
    PUBLIC_TASK,          // Art. 6.1.e
    LEGITIMATE_INTERESTS  // Art. 6.1.f — requiert documentation LIA
}
