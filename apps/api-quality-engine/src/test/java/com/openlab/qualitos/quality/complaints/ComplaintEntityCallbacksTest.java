package com.openlab.qualitos.quality.complaints;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ComplaintEntityCallbacksTest {

    @Test
    void complaintPrePersist_defaults() throws Exception {
        Complaint c = new Complaint();
        invoke(c, "prePersist");
        assertThat(c.getStatus()).isEqualTo(ComplaintStatus.RECEIVED);
        assertThat(c.getSeverity()).isEqualTo(ComplaintSeverity.MEDIUM);
        assertThat(c.getCategory()).isEqualTo(ComplaintCategory.OTHER);
        assertThat(c.getReceivedAt()).isNotNull();
        assertThat(c.getCreatedAt()).isNotNull();
    }

    @Test
    void complaintPrePersist_preservesValues() throws Exception {
        Complaint c = new Complaint();
        c.setStatus(ComplaintStatus.UNDER_INVESTIGATION);
        c.setSeverity(ComplaintSeverity.CRITICAL);
        c.setCategory(ComplaintCategory.QUALITY);
        Instant ts = Instant.parse("2026-04-04T04:04:04Z");
        c.setReceivedAt(ts);
        invoke(c, "prePersist");
        assertThat(c.getStatus()).isEqualTo(ComplaintStatus.UNDER_INVESTIGATION);
        assertThat(c.getSeverity()).isEqualTo(ComplaintSeverity.CRITICAL);
        assertThat(c.getCategory()).isEqualTo(ComplaintCategory.QUALITY);
        assertThat(c.getReceivedAt()).isEqualTo(ts);
    }

    @Test
    void complaintPreUpdate_refreshes() throws Exception {
        Complaint c = new Complaint();
        c.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = c.getUpdatedAt();
        Thread.sleep(5);
        invoke(c, "preUpdate");
        assertThat(c.getUpdatedAt()).isAfter(before);
    }

    @Test
    void responsePrePersist_stampsSentAndCreated() throws Exception {
        ComplaintResponse r = new ComplaintResponse();
        invoke(r, "prePersist");
        assertThat(r.getCreatedAt()).isNotNull();
        assertThat(r.getSentAt()).isNotNull();
    }

    @Test
    void responsePrePersist_preservesSentAt() throws Exception {
        ComplaintResponse r = new ComplaintResponse();
        Instant ts = Instant.parse("2026-02-02T02:02:02Z");
        r.setSentAt(ts);
        invoke(r, "prePersist");
        assertThat(r.getSentAt()).isEqualTo(ts);
    }

    private static void invoke(Object t, String m) throws Exception {
        Method method = t.getClass().getDeclaredMethod(m);
        method.setAccessible(true);
        method.invoke(t);
    }
}
