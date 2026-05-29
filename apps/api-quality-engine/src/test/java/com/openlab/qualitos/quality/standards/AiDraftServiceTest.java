package com.openlab.qualitos.quality.standards;

import com.openlab.qualitos.quality.aigateway.AiCompletionResult;
import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiDraftServiceTest {

    @Mock StandardDocumentTemplateRepository templates;
    @Mock AiGatewayClient ai;
    @InjectMocks AiDraftService service;

    static final UUID SID = UUID.randomUUID();
    static final UUID TID = UUID.randomUUID();

    private StandardDocumentTemplate template(String cat, String clauses, String desc) {
        Standard std = new Standard();
        std.setCode("iso-9001");
        std.setFullName("ISO 9001:2015");
        StandardDocumentTemplate t = new StandardDocumentTemplate();
        t.setStandard(std);
        t.setCode("ISO9001-MQ");
        t.setName("Manuel Qualité");
        t.setCategory(cat);
        t.setMapsToClauses(clauses);
        t.setDescription(desc);
        return t;
    }

    @Test
    void generate_withAllFields_buildsDraftFromTemplateContext() {
        when(templates.findByIdAndStandardId(TID, SID))
                .thenReturn(Optional.of(template("MANUAL", "4,5", "Manuel structurant le SMQ")));
        when(ai.complete(any(), any(), anyInt()))
                .thenReturn(new AiCompletionResult("# Manuel Qualité\n…", "ollama", 120, 2000));

        StandardsDto.AiDraftResponse r = service.generate(SID, TID);

        assertThat(r.templateCode()).isEqualTo("ISO9001-MQ");
        assertThat(r.draft()).contains("Manuel");
        assertThat(r.provider()).isEqualTo("ollama");
        assertThat(r.latencyMs()).isEqualTo(2000);
    }

    @Test
    void generate_withNullOptionalFields_stillWorks() {
        when(templates.findByIdAndStandardId(TID, SID))
                .thenReturn(Optional.of(template(null, null, null)));
        when(ai.complete(any(), any(), anyInt()))
                .thenReturn(new AiCompletionResult("brouillon", "ollama", 10, 500));

        StandardsDto.AiDraftResponse r = service.generate(SID, TID);

        assertThat(r.draft()).isEqualTo("brouillon");
        assertThat(r.templateName()).isEqualTo("Manuel Qualité");
    }

    @Test
    void generate_templateNotFound_throws() {
        when(templates.findByIdAndStandardId(TID, SID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.generate(SID, TID))
                .isInstanceOf(DocumentTemplateNotFoundException.class);
    }
}
