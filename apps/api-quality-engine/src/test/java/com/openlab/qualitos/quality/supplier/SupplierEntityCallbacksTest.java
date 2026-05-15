package com.openlab.qualitos.quality.supplier;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SupplierEntityCallbacksTest {

    @Test
    void supplierPrePersist_defaults() throws Exception {
        Supplier s = new Supplier();
        invoke(s, "prePersist");
        assertThat(s.getStatus()).isEqualTo(SupplierStatus.PROSPECT);
        assertThat(s.getScore()).isEqualTo(100);
        assertThat(s.getCreatedAt()).isNotNull();
    }

    @Test
    void supplierPrePersist_preservesValues() throws Exception {
        Supplier s = new Supplier();
        s.setStatus(SupplierStatus.APPROVED);
        s.setScore(75);
        invoke(s, "prePersist");
        assertThat(s.getStatus()).isEqualTo(SupplierStatus.APPROVED);
        assertThat(s.getScore()).isEqualTo(75);
    }

    @Test
    void supplierPreUpdate_refreshes() throws Exception {
        Supplier s = new Supplier();
        s.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = s.getUpdatedAt();
        Thread.sleep(5);
        invoke(s, "preUpdate");
        assertThat(s.getUpdatedAt()).isAfter(before);
    }

    @Test
    void auditPrePersist_setsCreatedAt() throws Exception {
        SupplierAuditRecord a = new SupplierAuditRecord();
        invoke(a, "prePersist");
        assertThat(a.getCreatedAt()).isNotNull();
    }

    @Test
    void ncPrePersist_defaultsStatusOpen() throws Exception {
        SupplierNonConformity nc = new SupplierNonConformity();
        invoke(nc, "prePersist");
        assertThat(nc.getStatus()).isEqualTo(NonConformityStatus.OPEN);
    }

    @Test
    void ncPrePersist_preservesStatus() throws Exception {
        SupplierNonConformity nc = new SupplierNonConformity();
        nc.setStatus(NonConformityStatus.RESOLVED);
        invoke(nc, "prePersist");
        assertThat(nc.getStatus()).isEqualTo(NonConformityStatus.RESOLVED);
    }

    @Test
    void certPrePersist_setsTimestamps() throws Exception {
        SupplierCertificate c = new SupplierCertificate();
        invoke(c, "prePersist");
        assertThat(c.getCreatedAt()).isNotNull();
        assertThat(c.getUpdatedAt()).isNotNull();
    }

    @Test
    void certIsExpired_byRefDate() {
        SupplierCertificate c = new SupplierCertificate();
        c.setExpiresOn(LocalDate.parse("2026-01-01"));
        assertThat(c.isExpiredAt(LocalDate.parse("2026-06-01"))).isTrue();
        assertThat(c.isExpiredAt(LocalDate.parse("2025-12-01"))).isFalse();
    }

    private static void invoke(Object t, String m) throws Exception {
        Method method = t.getClass().getDeclaredMethod(m);
        method.setAccessible(true);
        method.invoke(t);
    }
}
