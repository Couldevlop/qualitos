package com.openlab.qualitos.quality.auditlog;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditEventHasherTest {

    @Test
    void hash_isDeterministicForIdenticalInput() {
        AuditEvent a = sample();
        AuditEvent b = sample();
        assertThat(AuditEventHasher.hash(a)).isEqualTo(AuditEventHasher.hash(b));
    }

    @Test
    void hash_changesWhenFieldChanges() {
        AuditEvent a = sample();
        AuditEvent b = sample();
        b.setAction("other.action");
        assertThat(AuditEventHasher.hash(a)).isNotEqualTo(AuditEventHasher.hash(b));
    }

    @Test
    void hash_changesWhenSequenceChanges() {
        AuditEvent a = sample();
        AuditEvent b = sample();
        b.setSequenceNo(99L);
        assertThat(AuditEventHasher.hash(a)).isNotEqualTo(AuditEventHasher.hash(b));
    }

    @Test
    void hash_changesWhenPreviousHashChanges() {
        AuditEvent a = sample();
        AuditEvent b = sample();
        b.setPreviousHash("ffff");
        assertThat(AuditEventHasher.hash(a)).isNotEqualTo(AuditEventHasher.hash(b));
    }

    @Test
    void hash_nullFields_areSafe() {
        AuditEvent e = new AuditEvent();
        e.setTenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        e.setSequenceNo(1L);
        e.setOccurredAt(Instant.parse("2026-05-15T10:00:00Z"));
        e.setActorType(ActorType.SYSTEM);
        e.setAction("test.event");
        e.setResourceType("foo");
        // tous les autres champs nuls
        String h = AuditEventHasher.hash(e);
        assertThat(h).hasSize(64);
    }

    @Test
    void hash_isHex64Chars() {
        assertThat(AuditEventHasher.hash(sample())).matches("^[0-9a-f]{64}$");
    }

    @Test
    void hash_separator_preventsInjection() {
        // Si on concaténait sans séparateur, action="A" + resourceType="B" et
        // action="AB" + resourceType="" produiraient le même hash.
        AuditEvent a = sample();
        a.setAction("A");
        a.setResourceType("foo-b");
        AuditEvent b = sample();
        b.setAction("Afoo-b");
        b.setResourceType("");
        // Avec séparateur, les deux hashes diffèrent.
        assertThat(AuditEventHasher.hash(a)).isNotEqualTo(AuditEventHasher.hash(b));
    }

    private AuditEvent sample() {
        AuditEvent e = new AuditEvent();
        e.setTenantId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        e.setSequenceNo(1L);
        e.setOccurredAt(Instant.parse("2026-05-15T10:00:00Z"));
        e.setActorType(ActorType.USER);
        e.setActorUserId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        e.setAction("pdca.cycle.created");
        e.setResourceType("pdca-cycle");
        e.setResourceId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        e.setSummary("Cycle created");
        e.setPayloadJson("{\"x\":1}");
        e.setPreviousHash(null);
        return e;
    }
}
