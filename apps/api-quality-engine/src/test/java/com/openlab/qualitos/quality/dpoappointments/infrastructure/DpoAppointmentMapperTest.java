package com.openlab.qualitos.quality.dpoappointments.infrastructure;

import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointment;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DpoAppointmentMapperTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void roundtrip_emptyLinkedSet_csvNull() throws Exception {
        DpoAppointment a = DpoAppointment.propose(T, "DPO-1",
                "Jane", "j@x.com", null, DpoType.INTERNAL, null, null,
                "GROUP", Set.of(), U, NOW);
        DpoAppointmentJpaEntity e = invokeToEntity(a, null);
        assertThat(e.getLinkedProcessingActivityIdsCsv()).isNull();
        DpoAppointment back = invokeToDomain(e);
        assertThat(back.getLinkedProcessingActivityIds()).isEmpty();
    }

    @Test
    void roundtrip_filledLinkedSet_csvPopulated() throws Exception {
        UUID a1 = UUID.randomUUID();
        DpoAppointment a = DpoAppointment.propose(T, "DPO-2",
                "Jane", "j@x.com", null, DpoType.EXTERNAL, "DPO Inc", null,
                "GROUP", Set.of(a1), U, NOW);
        DpoAppointmentJpaEntity e = invokeToEntity(a, null);
        assertThat(e.getLinkedProcessingActivityIdsCsv()).contains(a1.toString());
        DpoAppointment back = invokeToDomain(e);
        assertThat(back.getLinkedProcessingActivityIds()).containsExactly(a1);
        assertThat(back.getExternalCompanyName()).isEqualTo("DPO Inc");
    }

    @Test
    void roundtrip_activeAppointment_preservesMetadata() throws Exception {
        DpoAppointment a = DpoAppointment.propose(T, "DPO-3",
                "Jane", "j@x.com", null, DpoType.INTERNAL, null, null,
                "GROUP", Set.of(), U, NOW);
        a.activate(NOW, NOW, "CNIL-1", NOW);
        DpoAppointmentJpaEntity e = invokeToEntity(a, null);
        assertThat(e.getStatus().name()).isEqualTo("ACTIVE");
        DpoAppointment back = invokeToDomain(e);
        assertThat(back.isActive()).isTrue();
        assertThat(back.getRegulatorNotificationReference()).isEqualTo("CNIL-1");
    }

    private static DpoAppointmentJpaEntity invokeToEntity(
            DpoAppointment a, DpoAppointmentJpaEntity target) throws Exception {
        Method m = DpoAppointmentMapper.class.getDeclaredMethod(
                "toEntity", DpoAppointment.class, DpoAppointmentJpaEntity.class);
        m.setAccessible(true);
        return (DpoAppointmentJpaEntity) m.invoke(null, a, target);
    }

    private static DpoAppointment invokeToDomain(DpoAppointmentJpaEntity e) throws Exception {
        Method m = DpoAppointmentMapper.class.getDeclaredMethod(
                "toDomain", DpoAppointmentJpaEntity.class);
        m.setAccessible(true);
        return (DpoAppointment) m.invoke(null, e);
    }
}
