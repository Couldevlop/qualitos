package com.openlab.qualitos.quality.industry;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    @Mock ResourcePatternResolver resolver;
    IndustryPackLoader loader;

    @BeforeEach
    void setup() {
        loader = new IndustryPackLoader(repo, resolver, new ObjectMapper());
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
                .hasMessageContaining("invalid 'code' format");
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
        new IndustryPackLoader(repo);
    }
}
