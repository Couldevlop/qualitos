package com.openlab.qualitos.quality.privacynotices.infrastructure;

import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNotice;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PrivacyNoticeMapperTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void roundtrip_emptyLinkedSet_csvNull() throws Exception {
        PrivacyNotice n = PrivacyNotice.draft(T, "PN-1", "1.0", "fr",
                "T", "s", "c", Set.of(), null, null, null, U, NOW);
        PrivacyNoticeJpaEntity e = invokeToEntity(n, null);
        assertThat(e.getLinkedProcessingActivityIdsCsv()).isNull();
        PrivacyNotice back = invokeToDomain(e);
        assertThat(back.getLinkedProcessingActivityIds()).isEmpty();
    }

    @Test
    void roundtrip_filledLinkedSet_csvPopulated() throws Exception {
        UUID linkA = UUID.randomUUID();
        UUID linkB = UUID.randomUUID();
        PrivacyNotice n = PrivacyNotice.draft(T, "PN-2", "1.0", "en",
                "T", "s", "c", Set.of(linkA, linkB), null, null, null, U, NOW);
        PrivacyNoticeJpaEntity e = invokeToEntity(n, null);
        assertThat(e.getLinkedProcessingActivityIdsCsv()).contains(linkA.toString());
        PrivacyNotice back = invokeToDomain(e);
        assertThat(back.getLinkedProcessingActivityIds()).containsExactlyInAnyOrder(linkA, linkB);
    }

    @Test
    void roundtrip_updatesExistingEntity_inPlace() throws Exception {
        PrivacyNoticeJpaEntity existing = new PrivacyNoticeJpaEntity();
        UUID existingId = UUID.randomUUID();
        existing.setId(existingId);
        PrivacyNotice n = PrivacyNotice.draft(T, "PN-3", "1.0", "fr",
                "T", "s", "c", Set.of(), null, null, null, U, NOW);
        n.assignId(existingId);
        PrivacyNoticeJpaEntity out = invokeToEntity(n, existing);
        assertThat(out).isSameAs(existing);
        assertThat(out.getId()).isEqualTo(existingId);
    }

    @Test
    void roundtrip_publishedNotice_metadataPreserved() throws Exception {
        PrivacyNotice n = PrivacyNotice.draft(T, "PN-4", "1.0", "fr",
                "T", "summary valid", "content valid", Set.of(),
                null, null, null, U, NOW);
        n.publish(U, NOW.plusSeconds(60));
        PrivacyNoticeJpaEntity e = invokeToEntity(n, null);
        assertThat(e.getStatus().name()).isEqualTo("PUBLISHED");
        assertThat(e.getPublishedAt()).isEqualTo(NOW.plusSeconds(60));
        PrivacyNotice back = invokeToDomain(e);
        assertThat(back.isPublished()).isTrue();
        assertThat(back.getPublishedByUserId()).isEqualTo(U);
    }

    private static PrivacyNoticeJpaEntity invokeToEntity(
            PrivacyNotice n, PrivacyNoticeJpaEntity target) throws Exception {
        Method m = PrivacyNoticeMapper.class.getDeclaredMethod(
                "toEntity", PrivacyNotice.class, PrivacyNoticeJpaEntity.class);
        m.setAccessible(true);
        return (PrivacyNoticeJpaEntity) m.invoke(null, n, target);
    }

    private static PrivacyNotice invokeToDomain(PrivacyNoticeJpaEntity e) throws Exception {
        Method m = PrivacyNoticeMapper.class.getDeclaredMethod(
                "toDomain", PrivacyNoticeJpaEntity.class);
        m.setAccessible(true);
        return (PrivacyNotice) m.invoke(null, e);
    }
}
