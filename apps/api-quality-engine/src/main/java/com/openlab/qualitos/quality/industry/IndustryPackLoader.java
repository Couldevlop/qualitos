package com.openlab.qualitos.quality.industry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Charge les manifestes YAML embarqués sous {@code classpath:industry-packs/*.yml} et
 * upsert le catalogue en DB au démarrage de l'application. Idempotent.
 *
 * Politique d'erreur : un fichier malformé est loggé en WARN et le démarrage continue
 * (un pack défaillant ne doit pas bloquer toute la plateforme). Le compteur d'erreurs
 * est exposé via {@link #lastRunErrorCount()} pour les tests et la santé.
 */
@Component
public class IndustryPackLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IndustryPackLoader.class);

    static final String LOCATION_PATTERN = "classpath*:industry-packs/*.yml";

    private final IndustryPackRepository repo;
    private final ResourcePatternResolver resolver;
    private final ObjectMapper jsonMapper;
    private int lastRunErrorCount;
    private int lastRunLoadedCount;

    @org.springframework.beans.factory.annotation.Autowired
    public IndustryPackLoader(IndustryPackRepository repo) {
        this(repo, new PathMatchingResourcePatternResolver(), new ObjectMapper());
    }

    IndustryPackLoader(IndustryPackRepository repo, ResourcePatternResolver resolver,
                       ObjectMapper jsonMapper) {
        this.repo = repo;
        this.resolver = resolver;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        loadAll();
    }

    @Transactional
    public synchronized void loadAll() {
        lastRunErrorCount = 0;
        lastRunLoadedCount = 0;
        Resource[] resources;
        try {
            resources = resolver.getResources(LOCATION_PATTERN);
        } catch (Exception e) {
            log.warn("Cannot enumerate industry pack resources: {}", e.getMessage());
            lastRunErrorCount++;
            return;
        }
        for (Resource r : resources) {
            try {
                loadResource(r);
                lastRunLoadedCount++;
            } catch (RuntimeException ex) {
                lastRunErrorCount++;
                log.warn("Industry pack {} skipped: {}", r.getFilename(), ex.getMessage());
            }
        }
        log.info("Industry packs: {} loaded, {} errors", lastRunLoadedCount, lastRunErrorCount);
    }

    void loadResource(Resource r) {
        IndustryPackManifest manifest = parseManifest(r);
        validate(manifest, r.getFilename());

        String manifestJson;
        try {
            manifestJson = jsonMapper.writeValueAsString(manifest);
        } catch (Exception e) {
            throw new IndustryPackManifestException("Cannot serialise manifest to JSON", e);
        }

        IndustryPack entity = repo.findByCode(manifest.getCode()).orElseGet(IndustryPack::new);
        boolean isNew = entity.getId() == null;
        entity.setCode(manifest.getCode());
        entity.setName(manifest.getName());
        entity.setDescription(manifest.getDescription());
        entity.setVersion(manifest.getVersion());
        entity.setLocale(manifest.getLocale());
        entity.setTagsCsv(manifest.getTags() == null ? null : String.join(",", manifest.getTags()));
        entity.setManifestJson(manifestJson);
        repo.save(entity);
        log.debug("Industry pack {} v{} {}", manifest.getCode(), manifest.getVersion(),
                isNew ? "created" : "updated");
    }

    IndustryPackManifest parseManifest(Resource r) {
        try (InputStream in = r.getInputStream()) {
            // SnakeYAML 2.x : Constructor exige LoaderOptions ; sur 1.x on retombe sur l'API simple.
            // L'usage typed Constructor(class, options) protège contre les attaques YAML
            // (pas d'instanciation arbitraire).
            org.yaml.snakeyaml.LoaderOptions options = new org.yaml.snakeyaml.LoaderOptions();
            options.setAllowDuplicateKeys(false);
            Yaml yaml = new Yaml(new Constructor(IndustryPackManifest.class, options));
            IndustryPackManifest m = yaml.loadAs(
                    new String(in.readAllBytes(), StandardCharsets.UTF_8),
                    IndustryPackManifest.class);
            if (m == null) throw new IndustryPackManifestException("Empty manifest");
            return m;
        } catch (YAMLException | IndustryPackManifestException ex) {
            if (ex instanceof IndustryPackManifestException ipe) throw ipe;
            throw new IndustryPackManifestException("YAML parse error: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new IndustryPackManifestException("I/O error: " + ex.getMessage(), ex);
        }
    }

    void validate(IndustryPackManifest m, String fileName) {
        String fn = Optional.ofNullable(fileName).orElse("<unknown>");
        if (m.getCode() == null || m.getCode().isBlank()) {
            throw new IndustryPackManifestException(fn + ": missing 'code'");
        }
        if (!m.getCode().matches("[a-z0-9][a-z0-9_-]{1,62}")) {
            throw new IndustryPackManifestException(fn + ": invalid 'code' format (lowercase, [a-z0-9_-])");
        }
        if (m.getName() == null || m.getName().isBlank()) {
            throw new IndustryPackManifestException(fn + ": missing 'name'");
        }
        if (m.getVersion() == null || m.getVersion().isBlank()) {
            throw new IndustryPackManifestException(fn + ": missing 'version'");
        }
    }

    public int lastRunErrorCount() { return lastRunErrorCount; }
    public int lastRunLoadedCount() { return lastRunLoadedCount; }
}
