package com.openlab.qualitos.quality.academy.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScormManifestBuilderTest {

    @Test
    void buildsValidScorm2004Manifest() {
        String manifest = ScormManifestBuilder.build("iso-9001-basics", "ISO 9001 — Bases",
                List.of(new ScormManifestBuilder.Item("L1", "Introduction", "content/lesson-1.html"),
                        new ScormManifestBuilder.Item("L2", "Clause 4", "content/lesson-2.html")));

        assertThat(manifest).contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        assertThat(manifest).contains("<schemaversion>2004 4th Edition</schemaversion>");
        assertThat(manifest).contains("identifier=\"iso-9001-basics\"");
        assertThat(manifest).contains("<title>ISO 9001 — Bases</title>");
        assertThat(manifest).contains("identifierref=\"RES-L1\"");
        assertThat(manifest).contains("href=\"content/lesson-1.html\"");
        assertThat(manifest).contains("adlcp:scormType=\"sco\"");
        assertThat(manifest).contains("<title>Clause 4</title>");
    }

    @Test
    void escapesXmlSpecialCharacters() {
        String manifest = ScormManifestBuilder.build("c1", "Q&A <b>course</b>",
                List.of(new ScormManifestBuilder.Item("L1", "A & B", "content/lesson-1.html")));
        assertThat(manifest).contains("Q&amp;A &lt;b&gt;course&lt;/b&gt;");
        assertThat(manifest).contains("<title>A &amp; B</title>");
        assertThat(manifest).doesNotContain("<b>course</b>");
    }

    @Test
    void handlesEmptyItemList() {
        String manifest = ScormManifestBuilder.build("c1", "Empty", List.of());
        assertThat(manifest).contains("<organizations");
        assertThat(manifest).contains("<resources>");
        assertThat(manifest).doesNotContain("identifierref");
    }
}
