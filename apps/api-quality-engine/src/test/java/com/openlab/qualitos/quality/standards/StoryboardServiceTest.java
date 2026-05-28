package com.openlab.qualitos.quality.standards;

import com.openlab.qualitos.quality.aigateway.AiCompletionResult;
import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoryboardServiceTest {

    @Mock StandardsService standards;
    @Mock AiGatewayClient ai;
    @InjectMocks StoryboardService service;

    static final UUID ADO = UUID.randomUUID();
    static final UUID STD = UUID.randomUUID();

    private void stubReports(String scope, List<StandardsDto.AuditFinding> findings) {
        when(standards.getAdoption(ADO)).thenReturn(new StandardsDto.AdoptionResponse(
                ADO, UUID.randomUUID(), STD, "iso-9001", "ISO 9001:2015",
                AdoptionStatus.IN_PROGRESS, scope, null, null, "AFNOR",
                null, null, Instant.now(), Instant.now()));
        when(standards.computeAlignment(ADO)).thenReturn(new StandardsDto.AlignmentReport(
                ADO, STD, "iso-9001", 88d, 50, 44, 40, 37, List.of()));
        when(standards.computeAuditBlanc(ADO)).thenReturn(new StandardsDto.AuditBlancReport(
                ADO, STD, "iso-9001", "ISO 9001:2015", Instant.now(),
                92d, 50, 44, 40, 37, 0, 2, 1, "QUASI PRÊT", findings));
        when(standards.getRoadmap(ADO)).thenReturn(new StandardsDto.RoadmapSummary(
                ADO, 19, 12, 1, 0, 63d, List.of()));
        when(ai.complete(any(), any(), anyInt()))
                .thenReturn(new AiCompletionResult("La certification progresse bien…", "ollama", 90, 1500));
    }

    @Test
    void generate_withFindingsAndScope_returnsNarrative() {
        stubReports("SMQ siège", List.of(
                new StandardsDto.AuditFinding(UUID.randomUUID(), "7", "7.4", "7.4", "Communication",
                        ObligationLevel.MUST, RiskLevel.LOW, "MAJOR", "Plan", "Couvrir 7.4", 2)));
        StandardsDto.StoryboardResponse r = service.generate(ADO);
        assertThat(r.narrative()).contains("certification");
        assertThat(r.provider()).isEqualTo("ollama");
        assertThat(r.standardCode()).isEqualTo("iso-9001");
    }

    @Test
    void generate_withoutFindingsAndNullScope_returnsNarrative() {
        stubReports(null, List.of());
        StandardsDto.StoryboardResponse r = service.generate(ADO);
        assertThat(r.narrative()).isNotBlank();
        assertThat(r.tenantStandardId()).isEqualTo(ADO);
    }
}
