package com.openlab.qualitos.quality.iot;

/**
 * Cycle de vie d'un équipement IoT (CLAUDE.md §9.6).
 * PROVISIONED → ACTIVE → SUSPENDED ↔ ACTIVE → DECOMMISSIONED (terminal).
 */
public enum IotDeviceStatus {
    PROVISIONED,
    ACTIVE,
    SUSPENDED,
    DECOMMISSIONED
}
