package com.openlab.qualitos.quality.industry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.standards.Standard;
import com.openlab.qualitos.quality.standards.StandardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndustryPackLoaderTest {

    @Mock IndustryPackRepository repo;
    @Mock(strictness = Mock.Strictness.LENIENT) StandardRepository standardRepo;
    @Mock ResourcePatternResolver resolver;
    IndustryPackLoader loader;

    @BeforeEach
    void setup() {
        loader = new IndustryPackLoader(repo, standardRepo, resolver, new ObjectMapper());
    }

    private static Resource yamlResource(String yaml, String filename) {
        return new ByteArrayResource(yaml.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            @Override public String getFilename() { return filename; }
        };
    }

    @Test
    void loadAll_parsesAndUpsertsValidManifest() throws Exception {
        String yaml = """
                code: test-pack
                name: Test Pack
                version: '1.0.0'
                locale: fr-FR
                tags: [foo, bar]
                standards: [iso-9001]
                """;
        Resource res = new ByteArrayResource(yaml.getBytes()) {
            @Override public String getFilename() { return "test-pack.yml"; }
        };
        when(resolver.getResources(IndustryPackLoader.LOCATION_PATTERN)).thenReturn(new Resource[]{ res });
        when(repo.findByCode("test-pack")).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        loader.loadAll();

        ArgumentCaptor<IndustryPack> cap = ArgumentCaptor.forClass(IndustryPack.class);
        verify(repo).save(cap.capture());
        IndustryPack saved = cap.getValue();
        assertThat(saved.getCode()).isEqualTo("test-pack");
        assertThat(saved.getName()).isEqualTo("Test Pack");
        assertThat(saved.getVersion()).isEqualTo("1.0.0");
        assertThat(saved.getLocale()).isEqualTo("fr-FR");
        assertThat(saved.getTagsCsv()).isEqualTo("foo,bar");
        assertThat(saved.getManifestJson()).contains("\"code\":\"test-pack\"");
        assertThat(loader.lastRunLoadedCount()).isOne();
        assertThat(loader.lastRunErrorCount()).isZero();
    }

    @Test
    void loadAll_existingPack_updatesInPlace() throws Exception {
        String yaml = """
                code: existing
                name: Existing
                version: '2.0.0'
                """;
        Resource res = new ByteArrayResource(yaml.getBytes()) {
            @Override public String getFilename() { return "existing.yml"; }
        };
        IndustryPack existing = new IndustryPack();
        existing.setId(java.util.UUID.randomUUID());
        existing.setCode("existing");
        existing.setVersion("1.0.0");

        when(resolver.getResources(any())).thenReturn(new Resource[]{ res });
        when(repo.findByCode("existing")).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        loader.loadAll();

        ArgumentCaptor<IndustryPack> cap = ArgumentCaptor.forClass(IndustryPack.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getId()).isEqualTo(existing.getId());
        assertThat(cap.getValue().getVersion()).isEqualTo("2.0.0");
    }

    @Test
    void loadAll_invalidCode_skipsAndCounts() throws Exception {
        String yaml = """
                code: 'BAD CODE!'
                name: x
                version: '1'
                """;
        Resource res = new ByteArrayResource(yaml.getBytes()) {
            @Override public String getFilename() { return "bad.yml"; }
        };
        when(resolver.getResources(any())).thenReturn(new Resource[]{ res });

        loader.loadAll();

        verify(repo, never()).save(any());
        assertThat(loader.lastRunErrorCount()).isOne();
        assertThat(loader.lastRunLoadedCount()).isZero();
    }

    @Test
    void loadAll_missingCode_skipsWithError() throws Exception {
        String yaml = """
                name: NoCode
                version: '1.0'
                """;
        Resource res = new ByteArrayResource(yaml.getBytes()) {
            @Override public String getFilename() { return "no-code.yml"; }
        };
        when(resolver.getResources(any())).thenReturn(new Resource[]{ res });

        loader.loadAll();
        verify(repo, never()).save(any());
        assertThat(loader.lastRunErrorCount()).isOne();
    }

    @Test
    void loadAll_missingName_skipsWithError() throws Exception {
        String yaml = """
                code: a-b
                version: '1.0'
                """;
        Resource res = new ByteArrayResource(yaml.getBytes()) {
            @Override public String getFilename() { return "no-name.yml"; }
        };
        when(resolver.getResources(any())).thenReturn(new Resource[]{ res });
        loader.loadAll();
        verify(repo, never()).save(any());
        assertThat(loader.lastRunErrorCount()).isOne();
    }

    @Test
    void loadAll_missingVersion_skipsWithError() throws Exception {
        String yaml = """
                code: a-b
                name: x
                """;
        Resource res = new ByteArrayResource(yaml.getBytes()) {
            @Override public String getFilename() { return "no-ver.yml"; }
        };
        when(resolver.getResources(any())).thenReturn(new Resource[]{ res });
        loader.loadAll();
        verify(repo, never()).save(any());
        assertThat(loader.lastRunErrorCount()).isOne();
    }

    @Test
    void loadAll_yamlParseError_skipsWithError() throws Exception {
        String yaml = "code: [unclosed array";
        Resource res = new ByteArrayResource(yaml.getBytes()) {
            @Override public String getFilename() { return "broken.yml"; }
        };
        when(resolver.getResources(any())).thenReturn(new Resource[]{ res });
        loader.loadAll();
        assertThat(loader.lastRunErrorCount()).isOne();
    }

    @Test
    void loadAll_emptyDir_noEntries() throws Exception {
        when(resolver.getResources(any())).thenReturn(new Resource[]{});
        loader.loadAll();
        verify(repo, never()).save(any());
        assertThat(loader.lastRunLoadedCount()).isZero();
    }

    @Test
    void loadAll_resolverThrows_recordsError() throws Exception {
        when(resolver.getResources(any())).thenThrow(new java.io.IOException("disk error"));
        loader.loadAll();
        assertThat(loader.lastRunErrorCount()).isOne();
    }

    @Test
    void validate_codeMustBeLowerKebab() {
        IndustryPackManifest m = new IndustryPackManifest();
        m.setCode("UPPER");
        m.setName("n");
        m.setVersion("1");
        assertThatThrownBy(() -> loader.validate(m, "test.yml"))
                .isInstanceOf(IndustryPackManifestException.class)
                .hasMessageContaining("invalid 'code'");
    }

    @Test
    void run_delegatesToLoadAll() throws Exception {
        when(resolver.getResources(any())).thenReturn(new Resource[]{});
        loader.run(null);
        assertThat(loader.lastRunErrorCount()).isZero();
    }

    @Test
    void defaultConstructor_isInvokable() {
        // Sanity check : le constructeur public (utilisé par Spring) ne lève pas.
        new IndustryPackLoader(repo, standardRepo);
    }

    // ---------------------------------------------------------------------
    // Schéma riche canonique (ADR 0019)
    // ---------------------------------------------------------------------

    private static final String RICH_YAML = """
            pack_id: banking
            version: 1.0.0
            name: Banking & Finance (Regulated)
            sectors: [banking, finance, insurance]
            norms:
              - dora
              - iso-27001
            kpis:
              - kpi_id: bank_dora_incident_notification_sla
                name: DORA Incident Notification SLA
                category: compliance-regulatory
                formula: "COUNT(a) / COUNT(b) * 100"
                unit: "%"
                target: "100"
                threshold_warning: "below 100"
                threshold_critical: "any miss"
                data_source: incidents
                refresh_frequency: per-event
                owner: compliance_officer
                applicable_industries: [banking, finance]
                related_kpis: []
                explainability: "DORA Art. 19."
            glossary:
              - {term: DORA, definition: "Digital Operational Resilience Act"}
              - {term: RCSA, definition: "Risk and Control Self-Assessment"}
            connectors_required:
              - sap-erp
              - servicenow-itsm
            ishikawa_templates:
              - problem_archetype: "Incident DORA majeur non notifié dans les 4h"
                branches:
                  man: ["Compliance officer absent"]
                  machine: ["Outil notification down"]
                  material: []
                  method: ["Procédure escalade ambiguë"]
                  measurement: ["Classification tardive"]
                  environment: ["Incident hors heures"]
            poka_yoke_library:
              - id: poka-yoke-bank-001
                name: 4-eyes obligatoire transactions > seuil
                description: "Workflow validation 2 approbateurs"
                sector_fit: [banking, finance]
            training_paths:
              - role: compliance_officer
                modules: [dora-fundamentals, lcb-ft]
            documents_templates:
              - /standards/templates/dora/cadre.md
            processes_templates:
              - /industry-packs/processes/banking/incident.bpmn
            """;

    @Test
    void loadAll_richSchema_normalizesPackIdToCodeAndKeepsRichSections() throws Exception {
        Resource res = yamlResource(RICH_YAML, "banking.yaml");
        when(resolver.getResources(any())).thenReturn(new Resource[]{ res });
        when(repo.findByCode("banking")).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(standardRepo.findByCode(any())).thenReturn(Optional.of(new Standard()));

        loader.loadAll();

        ArgumentCaptor<IndustryPack> cap = ArgumentCaptor.forClass(IndustryPack.class);
        verify(repo).save(cap.capture());
        IndustryPack saved = cap.getValue();
        // code DB = pack_id
        assertThat(saved.getCode()).isEqualTo("banking");
        assertThat(saved.getName()).isEqualTo("Banking & Finance (Regulated)");
        assertThat(saved.getVersion()).isEqualTo("1.0.0");
        // tags repliés sur sectors quand absents
        assertThat(saved.getTagsCsv()).isEqualTo("banking,finance,insurance");
        String json = saved.getManifestJson();
        // norms → standards
        assertThat(json).contains("\"standards\":[\"dora\",\"iso-27001\"]");
        // KPIs riches §6.6 conservés (kpiId, formula, owner…)
        assertThat(json).contains("\"kpiId\":\"bank_dora_incident_notification_sla\"");
        assertThat(json).contains("\"formula\":\"COUNT(a) / COUNT(b) * 100\"");
        assertThat(json).contains("\"owner\":\"compliance_officer\"");
        // glossaire liste→map
        assertThat(json).contains("\"DORA\":\"Digital Operational Resilience Act\"");
        // Ishikawa 6M branches
        assertThat(json).contains("\"ishikawaTemplates\"");
        assertThat(json).contains("\"machine\":[\"Outil notification down\"]");
        // Poka-Yoke
        assertThat(json).contains("\"pokaYokeLibrary\"");
        assertThat(json).contains("\"sectorFit\":[\"banking\",\"finance\"]");
        // connecteurs (connectors_required → connectors)
        assertThat(json).contains("\"connectors\":[\"sap-erp\",\"servicenow-itsm\"]");
        assertThat(loader.lastRunLoadedCount()).isOne();
        assertThat(loader.lastRunErrorCount()).isZero();
        assertThat(loader.lastRunUnknownNormCount()).isZero();
    }

    @Test
    void loadAll_richSchema_unknownNorm_warnsButLoads() throws Exception {
        Resource res = yamlResource(RICH_YAML, "banking.yaml");
        when(resolver.getResources(any())).thenReturn(new Resource[]{ res });
        when(repo.findByCode("banking")).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // 'dora' connu, 'iso-27001' inconnu du catalogue.
        when(standardRepo.findByCode("dora")).thenReturn(Optional.of(new Standard()));
        when(standardRepo.findByCode("iso-27001")).thenReturn(Optional.empty());

        loader.loadAll();

        verify(repo).save(any());                          // pack chargé malgré la réf inconnue
        assertThat(loader.lastRunLoadedCount()).isOne();
        assertThat(loader.lastRunErrorCount()).isZero();
        assertThat(loader.lastRunUnknownNormCount()).isOne();
    }

    @Test
    void loadAll_flatSchema_noStandardRepo_skipsReferentialValidation() throws Exception {
        // Régression : loader sans repo standards (overload legacy) ne valide pas les normes.
        IndustryPackLoader legacy = new IndustryPackLoader(repo, resolver, new ObjectMapper());
        String yaml = """
                code: flat-pack
                name: Flat Pack
                version: '1.0.0'
                standards: [made-up-norm]
                kpis: [oee, fpy]
                """;
        Resource res = yamlResource(yaml, "flat-pack.yml");
        when(resolver.getResources(any())).thenReturn(new Resource[]{ res });
        when(repo.findByCode("flat-pack")).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        legacy.loadAll();

        verify(repo).save(any());
        assertThat(legacy.lastRunLoadedCount()).isOne();
        assertThat(legacy.lastRunUnknownNormCount()).isZero();   // pas de validation sans repo
        verifyNoInteractions(standardRepo);
    }

    @Test
    void loadAll_richSchema_invalidPackId_skipsWithError() throws Exception {
        String yaml = """
                pack_id: 'BAD ID!'
                name: x
                version: '1.0.0'
                """;
        Resource res = yamlResource(yaml, "bad-rich.yaml");
        when(resolver.getResources(any())).thenReturn(new Resource[]{ res });

        loader.loadAll();

        verify(repo, never()).save(any());
        assertThat(loader.lastRunErrorCount()).isOne();
        assertThat(loader.lastRunLoadedCount()).isZero();
    }
}
