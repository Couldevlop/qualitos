package com.openlab.qualitos.quality.commconnector;

import com.openlab.qualitos.quality.itsm.ConnectionStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CommConnectionEntityTest {

    @Test
    void prePersist_setsDefaults() throws Exception {
        CommConnection c = new CommConnection();
        c.setTenantId(UUID.randomUUID());
        c.setName("n");
        c.setProvider(CommProvider.SLACK);
        c.setWebhookUrlCipher("x");
        c.setCreatedBy(UUID.randomUUID());

        invoke(c, "prePersist");

        assertThat(c.getCreatedAt()).isNotNull();
        assertThat(c.getUpdatedAt()).isNotNull();
        assertThat(c.getStatus()).isEqualTo(ConnectionStatus.ACTIVE);
    }

    @Test
    void preUpdate_touchesUpdatedAt() throws Exception {
        CommConnection c = new CommConnection();
        invoke(c, "preUpdate");
        assertThat(c.getUpdatedAt()).isNotNull();
    }

    @Test
    void gettersSetters_roundTrip() {
        CommConnection c = new CommConnection();
        UUID id = UUID.randomUUID();
        c.setId(id);
        c.setChannel("#q");
        c.setConsecutiveFailures(3);
        assertThat(c.getId()).isEqualTo(id);
        assertThat(c.getChannel()).isEqualTo("#q");
        assertThat(c.getConsecutiveFailures()).isEqualTo(3);
    }

    @Test
    void severity_hasHexAndEmoji() {
        assertThat(CommSeverity.CRITICAL.hex()).isEqualTo("D0021B");
        assertThat(CommSeverity.WARNING.emoji()).isNotEmpty();
        assertThat(CommSeverity.INFO.emoji()).isNotEmpty();
    }

    @Test
    void kind_fromWire_resolvesAndDefaults() {
        assertThat(CommEvent.Kind.fromWire("non-conformity.detected")).isEqualTo(CommEvent.Kind.NC_DETECTED);
        assertThat(CommEvent.Kind.fromWire("unknown.action")).isNull();
        assertThat(CommEvent.Kind.fromWire(null)).isNull();
        assertThat(CommEvent.Kind.CAPA_OVERDUE.defaultTitle()).isEqualTo("CAPA en retard");
        assertThat(CommEvent.Kind.CAPA_OVERDUE.wire()).isEqualTo("capa.case.overdue");
    }

    @Test
    void message_normalizesNulls() {
        CommMessage m = new CommMessage("t", null, null, null, null, null);
        assertThat(m.severity()).isEqualTo(CommSeverity.INFO);
        assertThat(m.facts()).isEmpty();
    }

    private static void invoke(Object target, String method) throws Exception {
        Method m = target.getClass().getDeclaredMethod(method);
        m.setAccessible(true);
        m.invoke(target);
    }
}
