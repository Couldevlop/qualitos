package com.openlab.qualitos.quality.dmaic.resolution.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.dmaic.DmaicPhaseGate;
import com.openlab.qualitos.quality.dmaic.DmaicProjectRepository;
import com.openlab.qualitos.quality.dmaic.resolution.application.ResolutionService;
import com.openlab.qualitos.quality.dmaic.resolution.domain.SheetSection;
import com.openlab.qualitos.quality.ishikawa.IshikawaDiagramRepository;
import com.openlab.qualitos.quality.nonconformity.storage.ObjectStorage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;

/**
 * Câblage Spring de la fiche de résolution DMAIC. Le {@code Clock} est celui de
 * {@code CommonBeansConfiguration} ; le stockage objet est optionnel (éteint, les
 * photos répondent 503 et la fiche reste lisible).
 */
@Configuration
public class ResolutionBeanConfiguration {

    @Bean
    @SuppressWarnings("java:S107")
    public ResolutionService dmaicResolutionService(DmaicProjectRepository projects,
                                                    ResolutionSheetJpaRepository sheets,
                                                    ResolutionSettingsJpaRepository settings,
                                                    ResolutionPhotoJpaRepository photos,
                                                    ObjectProvider<ObjectStorage> storage,
                                                    IshikawaDiagramRepository ishikawas,
                                                    AuditEventService auditEvents,
                                                    ObjectMapper objectMapper,
                                                    Clock clock) {
        ResolutionAdapters.Json json = new ResolutionAdapters.Json(objectMapper);
        return new ResolutionService(
                new ResolutionAdapters.JwtContext(),
                new ResolutionAdapters.Projects(projects),
                new ResolutionAdapters.Sheets(sheets, json),
                new ResolutionAdapters.Settings(settings, json),
                new ResolutionAdapters.Photos(photos),
                new ResolutionAdapters.Storage(storage),
                new ResolutionAdapters.Ishikawas(ishikawas),
                new AuditLogResolutionPublisher(auditEvents),
                new PdfBoxClaimReportRenderer(),
                clock);
    }

    /** Le verrou des phases du projet : la fiche dit ce qui manque à chacune. */
    @Bean
    public DmaicPhaseGate dmaicResolutionPhaseGate(ResolutionService resolution) {
        return (projectId, phase) -> resolution.missing(projectId, switch (phase) {
            case DEFINE -> List.of(SheetSection.DEFINE);
            case MEASURE -> List.of(SheetSection.MEASURE);
            case ANALYZE -> List.of(SheetSection.CONTAIN, SheetSection.ANALYZE);
            case IMPROVE -> List.of(SheetSection.IMPROVE);
            case CONTROL -> List.of(SheetSection.CONTROL);
        });
    }
}
