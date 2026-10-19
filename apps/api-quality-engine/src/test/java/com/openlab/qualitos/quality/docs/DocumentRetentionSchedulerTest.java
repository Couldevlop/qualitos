package com.openlab.qualitos.quality.docs;

import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentRetentionSchedulerTest {

    @Mock DocumentSettingsRowRepository settings;
    @Mock DocumentFileService files;

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    static DocumentSettingsRow row(UUID tenant, int years) {
        DocumentSettingsRow r = DocumentSettingsRow.defaults(tenant);
        r.setObsoleteRetentionYears(years);
        return r;
    }

    @Test
    void unClientEnEchec_nArretePasLesSuivants_etLeTenantEstPosepuisRetire() {
        UUID t1 = UUID.randomUUID();
        UUID t2 = UUID.randomUUID();
        when(settings.findWithRetention()).thenReturn(List.of(row(t1, 3), row(t2, 10)));
        List<String> seen = new ArrayList<>();
        when(files.purgeExpired(t1, 3)).thenAnswer(inv -> {
            seen.add(TenantContext.getTenantId());
            throw new IllegalStateException("boom");
        });
        when(files.purgeExpired(t2, 10)).thenAnswer(inv -> {
            seen.add(TenantContext.getTenantId());
            return 4;
        });

        new DocumentRetentionScheduler(settings, files).run();

        assertThat(seen).containsExactly(t1.toString(), t2.toString());
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    void rienARetirer_neJournaliseRien() {
        UUID t = UUID.randomUUID();
        when(settings.findWithRetention()).thenReturn(List.of(row(t, 1)));
        when(files.purgeExpired(t, 1)).thenReturn(0);
        new DocumentRetentionScheduler(settings, files).run();
        verify(files).purgeExpired(t, 1);
    }

    @Test
    void echecDeLaLecture_absorbe() {
        when(settings.findWithRetention()).thenThrow(new IllegalStateException("db down"));
        assertThatCode(() -> new DocumentRetentionScheduler(settings, files).run()).doesNotThrowAnyException();
        verifyNoInteractions(files);
    }
}
