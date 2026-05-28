package com.openlab.qualitos.quality.nlq;

import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NlqServiceTest {

    @Mock AiGatewayClient ai;
    @InjectMocks NlqService service;

    @Test
    void ask_mapsFullGatewayResponse() {
        Map<String, Object> gateway = Map.of(
                "sql", Map.of(
                        "sql", "SELECT status, COUNT(*) FROM capa_cases WHERE tenant_id = %(tenant_id)s GROUP BY status",
                        "tenant_filter_applied", true,
                        "tables_used", List.of("capa_cases"),
                        "functions_used", List.of("count")),
                "rows", List.of(Map.of("status", "OPEN", "count", 3)),
                "row_count", 1,
                "confidence", 0.85,
                "chart", Map.of("chart_type", "bar", "title", "CAPA par statut"),
                "narrative", "1 ligne.");
        when(ai.askNlq("Combien de CAPA par statut ?", 50)).thenReturn(gateway);

        NlqDto.AskResponse r = service.ask(new NlqDto.AskRequest("Combien de CAPA par statut ?", 50));

        assertThat(r.question()).isEqualTo("Combien de CAPA par statut ?");
        assertThat(r.sql()).contains("capa_cases");
        assertThat(r.tenantFilterApplied()).isTrue();
        assertThat(r.tablesUsed()).containsExactly("capa_cases");
        assertThat(r.functionsUsed()).containsExactly("count");
        assertThat(r.rows()).hasSize(1);
        assertThat(r.rows().get(0)).containsEntry("status", "OPEN");
        assertThat(r.rowCount()).isEqualTo(1);
        assertThat(r.confidence()).isEqualTo(0.85);
        assertThat(r.chart()).containsEntry("chart_type", "bar");
        assertThat(r.narrative()).isEqualTo("1 ligne.");
    }

    @Test
    void ask_defaultsMaxRows_andToleratesMissingOrMistypedFields() {
        // Réponse dégradée : champs absents ou mal typés -> valeurs sûres par défaut.
        when(ai.askNlq(eq("x"), eq(100))).thenReturn(Map.of(
                "sql", "not-a-map",          // mauvais type -> sqlNode vide
                "rows", "not-a-list",        // mauvais type -> []
                "confidence", "high"));       // mauvais type -> 0.0

        NlqDto.AskResponse r = service.ask(new NlqDto.AskRequest("x", null));

        assertThat(r.sql()).isEmpty();
        assertThat(r.tenantFilterApplied()).isFalse();
        assertThat(r.tablesUsed()).isEmpty();
        assertThat(r.functionsUsed()).isEmpty();
        assertThat(r.rows()).isEmpty();
        assertThat(r.rowCount()).isZero();
        assertThat(r.confidence()).isZero();
        assertThat(r.chart()).isEmpty();
        assertThat(r.narrative()).isEmpty();
    }

    @Test
    void ask_passesDefaultMaxRowsToGateway() {
        when(ai.askNlq(eq("q"), eq(100))).thenReturn(Map.of());
        service.ask(new NlqDto.AskRequest("q", null));
        ArgumentCaptor<Integer> rows = ArgumentCaptor.forClass(Integer.class);
        verify(ai).askNlq(eq("q"), rows.capture());
        assertThat(rows.getValue()).isEqualTo(100);
    }
}
