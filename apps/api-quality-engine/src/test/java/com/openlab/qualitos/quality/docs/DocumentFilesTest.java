package com.openlab.qualitos.quality.docs;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentFilesTest {

    static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    static final byte[] OLE = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1, 0, 0};

    // --- resolveType ---

    @Test
    void resolveType_typeDeclareAdmis_estRetenu() {
        assertThat(DocumentFiles.resolveType("application/pdf", "x.exe")).isEqualTo("application/pdf");
    }

    @Test
    void resolveType_parametresEtCasse_sontIgnores() {
        assertThat(DocumentFiles.resolveType(" Text/CSV ; charset=UTF-8", null)).isEqualTo("text/csv");
    }

    @Test
    void resolveType_typeDeclareNonAdmis_neRetombePasSurExtension() {
        assertThat(DocumentFiles.resolveType("application/x-msdownload", "x.pdf")).isNull();
    }

    @Test
    void resolveType_navigateurMuet_retombeSurExtension() {
        assertThat(DocumentFiles.resolveType(null, "process.BPMN")).isEqualTo("application/xml");
        assertThat(DocumentFiles.resolveType("", "notes.md")).isEqualTo("text/markdown");
        assertThat(DocumentFiles.resolveType("application/octet-stream", "old.doc")).isEqualTo("application/msword");
    }

    @Test
    void resolveType_navigateurMuet_sansExtensionExploitable_rendNull() {
        assertThat(DocumentFiles.resolveType("application/octet-stream", null)).isNull();
        assertThat(DocumentFiles.resolveType("", "sansextension")).isNull();
        assertThat(DocumentFiles.resolveType("", "point.")).isNull();
        assertThat(DocumentFiles.resolveType("", "virus.exe")).isNull();
    }

    // --- contentMatches ---

    @Test
    void contentMatches_videOuNull_faux() {
        assertThat(DocumentFiles.contentMatches("application/pdf", null)).isFalse();
        assertThat(DocumentFiles.contentMatches("application/pdf", new byte[0])).isFalse();
    }

    @Test
    void contentMatches_officeAncien_signatureOle() {
        assertThat(DocumentFiles.contentMatches("application/msword", OLE)).isTrue();
        assertThat(DocumentFiles.contentMatches("application/vnd.ms-excel", OLE)).isTrue();
        assertThat(DocumentFiles.contentMatches("application/vnd.ms-powerpoint", OLE)).isTrue();
        assertThat(DocumentFiles.contentMatches("application/msword", Arrays.copyOf(OLE, 4))).isFalse();
        byte[] faux = OLE.clone();
        faux[7] = 0;
        assertThat(DocumentFiles.contentMatches("application/msword", faux)).isFalse();
    }

    @Test
    void contentMatches_xml_bomEtEspacesTolérés() {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, ' ', '\n', '\t', '<', '?'};
        assertThat(DocumentFiles.contentMatches("application/xml", bom)).isTrue();
        assertThat(DocumentFiles.contentMatches("text/xml", "<a/>".getBytes(StandardCharsets.UTF_8))).isTrue();
    }

    @Test
    void contentMatches_xml_refuseTexteNonXmlEspacesSeulsEtOctetNul() {
        assertThat(DocumentFiles.contentMatches("application/xml", "abc".getBytes(StandardCharsets.UTF_8))).isFalse();
        assertThat(DocumentFiles.contentMatches("application/xml", "   ".getBytes(StandardCharsets.UTF_8))).isFalse();
        assertThat(DocumentFiles.contentMatches("application/xml", new byte[]{'<', 0, 'a'})).isFalse();
    }

    @Test
    void contentMatches_texte_octetNulDansLesPremiers8Ko_refuse() {
        assertThat(DocumentFiles.contentMatches("text/plain", "hello".getBytes(StandardCharsets.UTF_8))).isTrue();
        assertThat(DocumentFiles.contentMatches("text/csv", new byte[]{'a', 0})).isFalse();
        assertThat(DocumentFiles.contentMatches("text/markdown", "# T".getBytes(StandardCharsets.UTF_8))).isTrue();
        byte[] tardif = new byte[8193];
        Arrays.fill(tardif, (byte) 'a');
        tardif[8192] = 0;
        assertThat(DocumentFiles.contentMatches("text/plain", tardif)).isTrue();
    }

    @Test
    void contentMatches_autresTypes_delegueAuGarde() {
        assertThat(DocumentFiles.contentMatches("application/pdf", "%PDF-1.7".getBytes(StandardCharsets.US_ASCII))).isTrue();
        assertThat(DocumentFiles.contentMatches("application/pdf", "MZ...".getBytes(StandardCharsets.US_ASCII))).isFalse();
        assertThat(DocumentFiles.contentMatches(DOCX, new byte[]{0x50, 0x4B, 0x03, 0x04})).isTrue();
    }

    // --- sha256 ---

    @Test
    void sha256_vecteurConnu() {
        assertThat(DocumentFiles.sha256("abc".getBytes(StandardCharsets.US_ASCII)))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    // --- encodeForStorage ---

    @Test
    void encode_typeDejaCompresse_jamaisRecompresse() {
        byte[] repetitif = "a".repeat(10_000).getBytes(StandardCharsets.US_ASCII);
        for (String type : new String[]{DOCX, "image/jpeg", "image/png", "image/webp",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation"}) {
            DocumentFiles.Encoded e = DocumentFiles.encodeForStorage(type, repetitif);
            assertThat(e.contentEncoding()).isNull();
            assertThat(e.bytes()).isSameAs(repetitif);
        }
    }

    @Test
    void encode_texteCompressible_gzipEtRestituable() throws IOException {
        byte[] repetitif = "ligne de procédure\n".repeat(2_000).getBytes(StandardCharsets.UTF_8);
        DocumentFiles.Encoded e = DocumentFiles.encodeForStorage("text/plain", repetitif);
        assertThat(e.contentEncoding()).isEqualTo(DocumentFiles.GZIP);
        assertThat(e.bytes().length).isLessThan(repetitif.length / 2);
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(e.bytes()))) {
            assertThat(in.readAllBytes()).isEqualTo(repetitif);
        }
    }

    @Test
    void encode_gainInsuffisant_rendOriginal() {
        byte[] aleatoire = new byte[4096];
        new Random(42).nextBytes(aleatoire);
        DocumentFiles.Encoded e = DocumentFiles.encodeForStorage("application/pdf", aleatoire);
        assertThat(e.contentEncoding()).isNull();
        assertThat(e.bytes()).isSameAs(aleatoire);
    }

    @Test
    void isTextType() {
        assertThat(DocumentFiles.isTextType("text/plain")).isTrue();
        assertThat(DocumentFiles.isTextType("text/xml")).isTrue();
        assertThat(DocumentFiles.isTextType("application/pdf")).isFalse();
    }

    @Test
    void listeBlanche_ordreEtExtensions() {
        assertThat(DocumentFiles.ALLOWED_TYPES).containsEntry("application/pdf", "pdf")
                .containsEntry("text/xml", "xml").hasSize(15);
        assertThat(DocumentFiles.ALLOWED_TYPES.keySet().iterator().next()).isEqualTo("application/pdf");
    }
}
