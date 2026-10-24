package com.openlab.qualitos.quality.dmaic.resolution.infrastructure;

import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionContent;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionSettings;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionSheet;
import com.openlab.qualitos.quality.dmaic.resolution.domain.SheetPhoto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** Chaque écriture laisse sa trace, sans texte libre ni nom dans la charge utile. */
class AuditLogResolutionPublisherTest {

    @Test
    void chaqueEcritureEstTracee() {
        AuditEventService events = mock(AuditEventService.class);
        AuditLogResolutionPublisher publisher = new AuditLogResolutionPublisher(events);
        UUID tenant = UUID.randomUUID();
        UUID projet = UUID.randomUUID();
        UUID acteur = UUID.randomUUID();
        ResolutionContent c = new ResolutionContent("Acme secret", null, null, null, null, null, 2, "V10",
                "Texte confidentiel", null, null, null, null, null, null, null, null, null);
        ResolutionContent sansCode = new ResolutionContent(null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null);
        SheetPhoto photo = new SheetPhoto(UUID.randomUUID(), tenant, projet, 4, "k", "image/png", 12, "remarque",
                acteur, Instant.EPOCH);

        publisher.sheetSaved(new ResolutionSheet(UUID.randomUUID(), tenant, projet, c, acteur, Instant.EPOCH,
                Instant.EPOCH), acteur);
        publisher.sheetSaved(new ResolutionSheet(UUID.randomUUID(), tenant, projet, sansCode, acteur, Instant.EPOCH,
                Instant.EPOCH), acteur);
        publisher.settingsReplaced(tenant, ResolutionSettings.platformDefaults(), acteur);
        publisher.settingsReset(tenant, acteur);
        publisher.photoAdded(photo, acteur);
        publisher.photoRemoved(photo, acteur);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> traces =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(events, times(6)).recordForTenant(eq(tenant), traces.capture());
        List<AuditEventDto.RecordEventRequest> t = traces.getAllValues();
        assertThat(t).extracting(AuditEventDto.RecordEventRequest::action).containsExactly(
                AuditLogResolutionPublisher.SAVED, AuditLogResolutionPublisher.SAVED,
                AuditLogResolutionPublisher.SETTINGS_REPLACED, AuditLogResolutionPublisher.SETTINGS_RESET,
                AuditLogResolutionPublisher.PHOTO_ADDED, AuditLogResolutionPublisher.PHOTO_REMOVED);
        assertThat(t.get(0).payloadJson()).contains("\"severity\":2", "\"failureType\":\"V10\"")
                .doesNotContain("Acme", "confidentiel");
        assertThat(t.get(1).payloadJson()).contains("\"failureType\":null");
        assertThat(t.get(2).payloadJson()).contains("\"questions\":48", "\"failureTypes\":56", "\"severities\":3");
        assertThat(t.get(4).payloadJson()).contains("\"slot\":4").doesNotContain("remarque");
        assertThat(t.get(4).resourceId()).isEqualTo(projet);
    }
}
