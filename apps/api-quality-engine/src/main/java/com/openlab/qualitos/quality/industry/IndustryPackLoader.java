package com.openlab.qualitos.quality.industry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.standards.StandardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Charge les manifestes YAML embarqués sous {@code classpath*:industry-packs/*.yml} ET
 * {@code *.yaml}, puis upsert le catalogue en DB au démarrage. Idempotent (ADR 0019).
 *
 * <p>Deux schémas sont supportés et convergent vers {@link IndustryPackManifest} :
 * <ul>
 *   <li><b>schéma plat</b> historique (clé {@code code}) — rétro-compatibilité totale ;</li>
 *   <li><b>schéma riche canonique</b> (clé {@code pack_id}) — normalisé via
 *       {@link IndustryPackRichManifest#toManifest()} ; {@code code} DB = {@code pack_id}.</li>
 * </ul>
 * La détection se fait par une pré-lecture {@link SafeConstructor} en map (présence de
 * {@code pack_id}). Le parsing métier reste typé (durcissement anti-désérialisation conservé).
 *
 * <p><b>Validation référentielle</b> (ADR §4) : chaque code de {@code standards}/{@code norms}
 * est vérifié contre le catalogue Standards Hub. Une référence inconnue produit un WARN
 * structuré (code pack + code norme) — le pack reste chargé (tolérance contenu). Le nombre
 * total de références inconnues du dernier run est exposé via {@link #lastRunUnknownNormCount()}.
 *
 * <p>Politique d'erreur : un fichier malformé est loggé en WARN et le démarrage continue.
 */
@Component
public class IndustryPackLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IndustryPackLoader.class);

    // *.yml ET *.yaml : le schéma riche canonique utilise l'extension .yaml.
    static final String[] LOCATION_PATTERNS = {
            "classpath*:industry-packs/*.yml",
            "classpath*:industry-packs/*.yaml"
    };
    // Conservé pour la rétro-compatibilité des tests existants.
    static final String LOCATION_PATTERN = LOCATION_PATTERNS[0];

    private final IndustryPackRepository repo;
    @Nullable private final StandardRepository standardRepo;
    private final ResourcePatternResolver resolver;
    private final ObjectMapper jsonMapper;
    private int lastRunErrorCount;
    private int lastRunLoadedCount;
    private int lastRunUnknownNormCount;

    @org.springframework.beans.factory.annotation.Autowired
    public IndustryPackLoader(IndustryPackRepository repo, StandardRepository standardRepo) {
        this(repo, standardRepo, new PathMatchingResourcePatternResolver(), new ObjectMapper());
    }

    /** Constructeur sans repo standards : validation référentielle désactivée (tests legacy). */
    IndustryPackLoader(IndustryPackRepository repo) {
        this(repo, null, new PathMatchingResourcePatternResolver(), new ObjectMapper());
    }

    IndustryPackLoader(IndustryPackRepository repo, ResourcePatternResolver resolver,
                       ObjectMapper jsonMapper) {
        this(repo, null, resolver, jsonMapper);
    }

    IndustryPackLoader(IndustryPackRepository repo, @Nullable StandardRepository standardRepo,
                       ResourcePatternResolver resolver, ObjectMapper jsonMapper) {
        this.repo = repo;
        this.standardRepo = standardRepo;
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
        lastRunUnknownNormCount = 0;
        java.util.LinkedHashMap<String, Resource> resources = new java.util.LinkedHashMap<>();
        try {
            for (String pattern : LOCATION_PATTERNS) {
                Resource[] matched = resolver.getResources(pattern);
                if (matched == null) continue;
                for (Resource r : matched) {
                    // Déduplication par URI : un même fichier ne matche qu'un pattern,
                    // mais on se protège des chevauchements de classpath.
                    resources.putIfAbsent(resourceKey(r), r);
                }
            }
        } catch (Exception e) {
            log.warn("Cannot enumerate industry pack resources: {}", e.getMessage());
            lastRunErrorCount++;
            return;
        }
        for (Resource r : resources.values()) {
            try {
                loadResource(r);
                lastRunLoadedCount++;
            } catch (RuntimeException ex) {
                lastRunErrorCount++;
                log.warn("Industry pack {} skipped: {}", r.getFilename(), ex.getMessage());
            }
        }
        log.info("Industry packs: {} loaded, {} errors, {} unknown norm references",
                lastRunLoadedCount, lastRunErrorCount, lastRunUnknownNormCount);
    }

    private static String resourceKey(Resource r) {
        try {
            return r.getURI().toString();
        } catch (Exception e) {
            return Optional.ofNullable(r.getFilename()).orElse(r.getDescription());
        }
    }

    void loadResource(Resource r) {
        IndustryPackManifest manifest = parseManifest(r);
        validate(manifest, r.getFilename());
        validateNormReferences(manifest);

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
        entity.setTagsCsv(tagsCsv(manifest));
        entity.setManifestJson(manifestJson);
        repo.save(entity);
        log.debug("Industry pack {} v{} {}", manifest.getCode(), manifest.getVersion(),
                isNew ? "created" : "updated");
    }

    /** Tags : schéma plat = {@code tags} ; schéma riche = repli sur {@code sectors}. */
    private static String tagsCsv(IndustryPackManifest m) {
        List<String> tags = m.getTags();
        if ((tags == null || tags.isEmpty()) && m.getSectors() != null) {
            tags = m.getSectors();
        }
        return (tags == null || tags.isEmpty()) ? null : String.join(",", tags);
    }

    IndustryPackManifest parseManifest(Resource r) {
        try (InputStream in = r.getInputStream()) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return isRichSchema(content) ? parseRich(content) : parseFlat(content);
        } catch (YAMLException | IndustryPackManifestException ex) {
            if (ex instanceof IndustryPackManifestException ipe) throw ipe;
            throw new IndustryPackManifestException("YAML parse error: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new IndustryPackManifestException("I/O error: " + ex.getMessage(), ex);
        }
    }

    /**
     * Détection du schéma : pré-lecture en map via {@link SafeConstructor} (aucune
     * instanciation d'objet arbitraire) ; présence de {@code pack_id} ⇒ schéma riche.
     */
    @SuppressWarnings("unchecked")
    private static boolean isRichSchema(String content) {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Object root = new Yaml(new SafeConstructor(options)).load(content);
        if (root == null) throw new IndustryPackManifestException("Empty manifest");
        if (!(root instanceof Map)) {
            throw new IndustryPackManifestException("Manifest root must be a mapping");
        }
        return ((Map<String, Object>) root).containsKey("pack_id");
    }

    /** Schéma plat historique — Constructor typé inchangé (rétro-compat). */
    private static IndustryPackManifest parseFlat(String content) {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new Constructor(IndustryPackManifest.class, options));
        IndustryPackManifest m = yaml.loadAs(content, IndustryPackManifest.class);
        if (m == null) throw new IndustryPackManifestException("Empty manifest");
        return m;
    }

    /** Schéma riche canonique — Constructor typé + PropertyUtils snake_case→camelCase. */
    private static IndustryPackManifest parseRich(String content) {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Constructor constructor = new Constructor(IndustryPackRichManifest.class, options);
        SnakeCasePropertyUtils props = new SnakeCasePropertyUtils();
        // Tolérance : une clé riche additionnelle (ajoutée par le contenu) ne fait pas
        // échouer le pack ; le test de contrat P4 garantit la complétude séparément.
        props.setSkipMissingProperties(true);
        constructor.setPropertyUtils(props);
        IndustryPackRichManifest rich = new Yaml(constructor).loadAs(content, IndustryPackRichManifest.class);
        if (rich == null) throw new IndustryPackManifestException("Empty manifest");
        return rich.toManifest();
    }

    void validate(IndustryPackManifest m, String fileName) {
        String fn = Optional.ofNullable(fileName).orElse("<unknown>");
        if (m.getCode() == null || m.getCode().isBlank()) {
            throw new IndustryPackManifestException(fn + ": missing 'code'/'pack_id'");
        }
        if (!m.getCode().matches("[a-z0-9][a-z0-9_-]{1,62}")) {
            throw new IndustryPackManifestException(fn + ": invalid 'code'/'pack_id' format (lowercase, [a-z0-9_-])");
        }
        if (m.getName() == null || m.getName().isBlank()) {
            throw new IndustryPackManifestException(fn + ": missing 'name'");
        }
        if (m.getVersion() == null || m.getVersion().isBlank()) {
            throw new IndustryPackManifestException(fn + ": missing 'version'");
        }
    }

    /**
     * Validation référentielle (ADR §4) : chaque norme référencée doit exister dans le
     * catalogue Standards Hub. Référence inconnue ⇒ WARN structuré, pack chargé quand même.
     */
    void validateNormReferences(IndustryPackManifest m) {
        if (standardRepo == null || m.getStandards() == null) return;
        for (String normCode : m.getStandards()) {
            if (normCode == null || normCode.isBlank()) continue;
            if (standardRepo.findByCode(normCode).isEmpty()) {
                lastRunUnknownNormCount++;
                log.warn("Industry pack '{}' references unknown standard '{}' (not in Standards Hub catalogue) — pack loaded anyway",
                        m.getCode(), normCode);
            }
        }
    }

    public int lastRunErrorCount() { return lastRunErrorCount; }
    public int lastRunLoadedCount() { return lastRunLoadedCount; }
    public int lastRunUnknownNormCount() { return lastRunUnknownNormCount; }

    /**
     * {@link PropertyUtils} qui mappe les clés YAML snake_case ({@code pack_id}, {@code kpi_id},
     * {@code poka_yoke_library}…) vers les propriétés JavaBean camelCase correspondantes.
     */
    static final class SnakeCasePropertyUtils extends PropertyUtils {
        @Override
        public Property getProperty(Class<?> type, String name) {
            return super.getProperty(type, toCamelCase(name));
        }

        private static String toCamelCase(String name) {
            if (name == null || name.indexOf('_') < 0) return name;
            StringBuilder sb = new StringBuilder(name.length());
            boolean upper = false;
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (c == '_') {
                    upper = true;
                } else if (upper) {
                    sb.append(Character.toUpperCase(c));
                    upper = false;
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
    }
}
