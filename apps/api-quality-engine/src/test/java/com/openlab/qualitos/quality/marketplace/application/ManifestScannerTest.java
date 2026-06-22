package com.openlab.qualitos.quality.marketplace.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.marketplace.domain.ManifestScanResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ManifestScannerTest {

    private final ManifestScanner scanner = new ManifestScanner(new ObjectMapper());

    @Test
    void validManifest_passes() {
        String json = """
            {"name":"Pack ISO 13485","version":"1.0","description":"d",
             "norms":["iso-13485","iso-14971"],
             "documents":[{"path":"docs/manual.md"}],
             "templates":[{"file":"templates/policy.docx"}]}
            """;
        ManifestScanResult r = scanner.scan(json);
        assertThat(r.ok()).isTrue();
        assertThat(r.errors()).isEmpty();
    }

    @Test
    void empty_isRejected() {
        assertThat(scanner.scan("   ").ok()).isFalse();
        assertThat(scanner.scan(null).ok()).isFalse();
    }

    @Test
    void notJson_isRejected() {
        ManifestScanResult r = scanner.scan("not-json");
        assertThat(r.ok()).isFalse();
        assertThat(r.errors()).anyMatch(e -> e.contains("valid JSON"));
    }

    @Test
    void nonObject_isRejected() {
        assertThat(scanner.scan("[1,2,3]").ok()).isFalse();
    }

    @Test
    void missingRequiredFields_isRejected() {
        ManifestScanResult r = scanner.scan("{\"description\":\"x\"}");
        assertThat(r.ok()).isFalse();
        assertThat(r.errors()).anyMatch(e -> e.contains("'name'"));
        assertThat(r.errors()).anyMatch(e -> e.contains("'version'"));
    }

    @Test
    void pathTraversal_isRejected() {
        String json = """
            {"name":"x","version":"1.0","documents":[{"path":"../../etc/passwd"}]}
            """;
        ManifestScanResult r = scanner.scan(json);
        assertThat(r.ok()).isFalse();
        assertThat(r.errors()).anyMatch(e -> e.contains("unsafe file reference"));
    }

    @Test
    void absoluteAndSchemePaths_areRejected() {
        assertThat(ManifestScanner.isSafeRelativePath("/etc/passwd")).isFalse();
        assertThat(ManifestScanner.isSafeRelativePath("C:\\windows")).isFalse();
        assertThat(ManifestScanner.isSafeRelativePath("file:///etc")).isFalse();
        assertThat(ManifestScanner.isSafeRelativePath("classpath:secret")).isFalse();
        assertThat(ManifestScanner.isSafeRelativePath("https://evil/x")).isFalse();
        assertThat(ManifestScanner.isSafeRelativePath("a/b..c")).isFalse();
        assertThat(ManifestScanner.isSafeRelativePath("docs/manual.md")).isTrue();
    }

    @Test
    void invalidNormSlug_isRejected() {
        String json = "{\"name\":\"x\",\"version\":\"1.0\",\"norms\":[\"ISO 13485!\"]}";
        ManifestScanResult r = scanner.scan(json);
        assertThat(r.ok()).isFalse();
        assertThat(r.errors()).anyMatch(e -> e.contains("invalid norm slug"));
    }

    @Test
    void noNormsNoDocs_warnsButPasses() {
        ManifestScanResult r = scanner.scan("{\"name\":\"x\",\"version\":\"1.0\"}");
        assertThat(r.ok()).isTrue();
        assertThat(r.warnings()).anyMatch(w -> w.contains("norms"));
        assertThat(r.warnings()).anyMatch(w -> w.contains("documents/templates"));
    }

    @Test
    void oversizeManifest_isRejected() {
        StringBuilder sb = new StringBuilder("{\"name\":\"x\",\"version\":\"1.0\",\"description\":\"");
        sb.append("a".repeat(ManifestScanner.MAX_MANIFEST_BYTES + 10));
        sb.append("\"}");
        ManifestScanResult r = scanner.scan(sb.toString());
        assertThat(r.ok()).isFalse();
        assertThat(r.errors()).anyMatch(e -> e.contains("exceeds"));
    }
}
