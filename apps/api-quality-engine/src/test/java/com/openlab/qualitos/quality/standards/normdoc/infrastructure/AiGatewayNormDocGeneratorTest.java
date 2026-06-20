package com.openlab.qualitos.quality.standards.normdoc.infrastructure;

import com.openlab.qualitos.quality.aigateway.AiCompletionResult;
import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.standards.normdoc.domain.GeneratedNormDoc;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocGenerationCommand;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiGatewayNormDocGeneratorTest {

    @Mock AiGatewayClient ai;

    private NormDocGenerationCommand command(NormDocKind kind, boolean withProcesses,
                                             boolean withClauses, boolean withGuidance) {
        return new NormDocGenerationCommand(kind, "iso-9001", "ISO 9001:2015",
                "ACME", "manufacturing", "PME", "fr",
                withProcesses ? List.of("achats", "production") : List.of(),
                List.of(new NormDocGenerationCommand.SectionRequest("ctx", "Contexte",
                        withClauses ? List.of("4.1") : List.of(),
                        withGuidance ? "cadrer" : "")));
    }

    @Test
    void generate_callsGatewayPerSection_buildsTitleAndProvider() {
        when(ai.complete(anyString(), anyString(), anyInt()))
                .thenReturn(new AiCompletionResult("## corps\nrédigé", "ollama", 120, 2000));

        AiGatewayNormDocGenerator gen = new AiGatewayNormDocGenerator(ai);
        GeneratedNormDoc out = gen.generate(command(NormDocKind.MANUAL, true, true, true));

        assertThat(out.title()).isEqualTo("Manuel Qualité — ACME (iso-9001)");
        assertThat(out.provider()).isEqualTo("ollama");
        assertThat(out.sections()).hasSize(1);
        assertThat(out.sections().get(0).getBodyMarkdown()).isEqualTo("## corps\nrédigé");

        ArgumentCaptor<String> sys = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> user = ArgumentCaptor.forClass(String.class);
        verify(ai, times(1)).complete(sys.capture(), user.capture(), anyInt());
        assertThat(sys.getValue()).contains("Manuel Qualité").contains("ISO 9001:2015 (iso-9001)");
        assertThat(user.getValue())
                .contains("Organisation : ACME")
                .contains("Processus connus : achats, production")
                .contains("Clauses couvertes : 4.1.")
                .contains("Consigne : cadrer");
    }

    @Test
    void generate_withoutProcessesClausesGuidance_omitsThem() {
        when(ai.complete(anyString(), anyString(), anyInt()))
                .thenReturn(new AiCompletionResult("corps", "ollama", 10, 500));
        AiGatewayNormDocGenerator gen = new AiGatewayNormDocGenerator(ai);
        gen.generate(command(NormDocKind.POLICY, false, false, false));

        ArgumentCaptor<String> sys = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> user = ArgumentCaptor.forClass(String.class);
        verify(ai).complete(sys.capture(), user.capture(), anyInt());
        assertThat(sys.getValue()).contains("Politique Qualité");
        assertThat(user.getValue())
                .doesNotContain("Processus connus")
                .doesNotContain("Clauses couvertes")
                .doesNotContain("Consigne :");
    }

    @Test
    void generate_nullTextBecomesEmptyBody() {
        when(ai.complete(anyString(), anyString(), anyInt()))
                .thenReturn(new AiCompletionResult(null, "ollama", 0, 0));
        AiGatewayNormDocGenerator gen = new AiGatewayNormDocGenerator(ai);
        GeneratedNormDoc out = gen.generate(command(NormDocKind.PROCEDURE, false, false, false));
        assertThat(out.title()).startsWith("Procédure documentée — ACME");
        assertThat(out.sections().get(0).getBodyMarkdown()).isEmpty();
    }
}
