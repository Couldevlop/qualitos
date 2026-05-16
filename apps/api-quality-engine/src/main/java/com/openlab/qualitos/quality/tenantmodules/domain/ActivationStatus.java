package com.openlab.qualitos.quality.tenantmodules.domain;

/**
 * TRIAL → ACTIVE                             (paid)
 * TRIAL → EXPIRED                            (no payment after trial)
 * ACTIVE → SUSPENDED ↔ ACTIVE                (admin pause)
 * ACTIVE | SUSPENDED → DISABLED              (terminal-ish, can re-enable as new row)
 * ANY → EXPIRED                              (billing-driven, terminal)
 */
public enum ActivationStatus {
    TRIAL, ACTIVE, SUSPENDED, EXPIRED, DISABLED
}
