package com.openlab.qualitos.quality.docs;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentEntityCallbacksTest {

    @Test
    void docPrePersist_defaults() throws Exception {
        Document d = new Document();
        invoke(d, "prePersist");
        assertThat(d.getCreatedAt()).isNotNull();
        assertThat(d.getStatus()).isEqualTo(DocumentStatus.ACTIVE);
    }

    @Test
    void docPrePersist_preservesStatus() throws Exception {
        Document d = new Document();
        d.setStatus(DocumentStatus.ARCHIVED);
        invoke(d, "prePersist");
        assertThat(d.getStatus()).isEqualTo(DocumentStatus.ARCHIVED);
    }

    @Test
    void docPreUpdate_refreshes() throws Exception {
        Document d = new Document();
        d.setCreatedAt(Instant.now().minusSeconds(60));
        d.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = d.getUpdatedAt();
        Thread.sleep(5);
        invoke(d, "preUpdate");
        assertThat(d.getUpdatedAt()).isAfter(before);
    }

    @Test
    void versionPrePersist_defaults() throws Exception {
        DocumentVersion v = new DocumentVersion();
        invoke(v, "prePersist");
        assertThat(v.getStatus()).isEqualTo(VersionStatus.DRAFT);
        assertThat(v.getCreatedAt()).isNotNull();
    }

    @Test
    void versionPrePersist_preservesStatus() throws Exception {
        DocumentVersion v = new DocumentVersion();
        v.setStatus(VersionStatus.PUBLISHED);
        invoke(v, "prePersist");
        assertThat(v.getStatus()).isEqualTo(VersionStatus.PUBLISHED);
    }

    @Test
    void versionPreUpdate_refreshes() throws Exception {
        DocumentVersion v = new DocumentVersion();
        v.setCreatedAt(Instant.now().minusSeconds(60));
        v.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = v.getUpdatedAt();
        Thread.sleep(5);
        invoke(v, "preUpdate");
        assertThat(v.getUpdatedAt()).isAfter(before);
    }

    @Test
    void ackPrePersist_setsTimestamp() throws Exception {
        DocumentAcknowledgment a = new DocumentAcknowledgment();
        invoke(a, "prePersist");
        assertThat(a.getAcknowledgedAt()).isNotNull();
    }

    @Test
    void ackPrePersist_preservesExplicit() throws Exception {
        DocumentAcknowledgment a = new DocumentAcknowledgment();
        Instant fixed = Instant.now().minusSeconds(120);
        a.setAcknowledgedAt(fixed);
        invoke(a, "prePersist");
        assertThat(a.getAcknowledgedAt()).isEqualTo(fixed);
    }

    private static void invoke(Object t, String m) throws Exception {
        Method method = t.getClass().getDeclaredMethod(m);
        method.setAccessible(true);
        method.invoke(t);
    }
}
