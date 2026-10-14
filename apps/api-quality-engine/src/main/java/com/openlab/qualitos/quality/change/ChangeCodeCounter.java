package com.openlab.qualitos.quality.change;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Le dernier rang attribué à un code {@code MOC-AAAA-NNN}, par client et par
 * année. Lu sous verrou : deux créations simultanées n'obtiennent jamais le
 * même rang, et un brouillon supprimé ne rend pas son numéro.
 */
@Entity
@Table(name = "change_code_counters")
@IdClass(ChangeCodeCounter.Key.class)
public class ChangeCodeCounter {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Id
    @Column(name = "counter_year", nullable = false)
    private int year;

    @Column(name = "last_value", nullable = false)
    private int lastValue;

    protected ChangeCodeCounter() {}

    public ChangeCodeCounter(UUID tenantId, int year, int lastValue) {
        this.tenantId = tenantId;
        this.year = year;
        this.lastValue = lastValue;
    }

    public UUID getTenantId() { return tenantId; }
    public int getYear() { return year; }
    public int getLastValue() { return lastValue; }
    public void setLastValue(int lastValue) { this.lastValue = lastValue; }

    /** Clé composite (client, année). */
    public static class Key implements Serializable {
        private UUID tenantId;
        private int year;

        public Key() {}

        public Key(UUID tenantId, int year) {
            this.tenantId = tenantId;
            this.year = year;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && k.year == year && Objects.equals(k.tenantId, tenantId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tenantId, year);
        }
    }
}
