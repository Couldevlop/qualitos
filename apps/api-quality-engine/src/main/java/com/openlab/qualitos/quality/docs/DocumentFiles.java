package com.openlab.qualitos.quality.docs;

import com.openlab.qualitos.quality.nonconformity.storage.UploadedBinaryGuard;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

/**
 * Ce que la GED admet comme fichier, et comment elle le conserve (ADR 0094).
 *
 * <p>Des fonctions pures : la liste blanche, la vérification des premiers octets,
 * l'empreinte, et la décision de compresser. Aucune dépendance au stockage.
 *
 * <p><b>Pourquoi compresser au cas par cas.</b> PDF, documents Office récents et
 * images sont déjà compressés : les recompresser coûte du processeur pour moins
 * de 5 % de gain, parfois un fichier plus gros. Le texte, le CSV, le XML (BPMN)
 * et les formats Office anciens se réduisent, eux, de 60 à 90 %. On tente donc
 * la compression hors des formats déjà compressés, et on ne garde la copie
 * compressée que si elle fait gagner au moins {@link #MIN_GAIN_PERCENT} %.
 * L'empreinte est TOUJOURS celle du fichier d'origine : c'est elle qu'on ancre et
 * qu'un auditeur recalcule sur le fichier téléchargé.
 */
public final class DocumentFiles {

    /** En deçà de ce gain, la copie compressée ne vaut pas sa décompression. */
    static final int MIN_GAIN_PERCENT = 10;

    static final String GZIP = "gzip";

    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    private static final String DOC = "application/msword";
    private static final String XLS = "application/vnd.ms-excel";
    private static final String PPT = "application/vnd.ms-powerpoint";

    /** Liste blanche type → extension ; l'ordre est celui qu'affiche l'écran. */
    static final Map<String, String> ALLOWED_TYPES = buildAllowedTypes();

    /** Les types déjà compressés : on ne tente pas de les recompresser. */
    private static final Set<String> ALREADY_COMPRESSED = Set.of(
            DOCX, XLSX, PPTX, "image/jpeg", "image/png", "image/webp");

    /** Les types textuels : leur contenu ne doit porter aucun octet nul. */
    private static final Set<String> TEXT_TYPES = Set.of(
            "text/plain", "text/csv", "text/markdown", "application/xml", "text/xml");

    /** Les extensions d'un navigateur muet sur le type (BPMN, CSV, Markdown…). */
    private static final Map<String, String> TYPE_BY_EXTENSION = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("docx", DOCX), Map.entry("xlsx", XLSX), Map.entry("pptx", PPTX),
            Map.entry("doc", DOC), Map.entry("xls", XLS), Map.entry("ppt", PPT),
            Map.entry("jpg", "image/jpeg"), Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"), Map.entry("webp", "image/webp"),
            Map.entry("txt", "text/plain"), Map.entry("csv", "text/csv"),
            Map.entry("md", "text/markdown"), Map.entry("xml", "application/xml"),
            Map.entry("bpmn", "application/xml"));

    private static final int TEXT_PROBE_BYTES = 8192;

    private DocumentFiles() {}

    private static Map<String, String> buildAllowedTypes() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("application/pdf", "pdf");
        types.put(DOCX, "docx");
        types.put(XLSX, "xlsx");
        types.put(PPTX, "pptx");
        types.put(DOC, "doc");
        types.put(XLS, "xls");
        types.put(PPT, "ppt");
        types.put("image/jpeg", "jpg");
        types.put("image/png", "png");
        types.put("image/webp", "webp");
        types.put("text/plain", "txt");
        types.put("text/csv", "csv");
        types.put("text/markdown", "md");
        types.put("application/xml", "xml");
        types.put("text/xml", "xml");
        return java.util.Collections.unmodifiableMap(types);
    }

    /**
     * Le type retenu pour un fichier : celui que déclare le navigateur s'il est
     * admis, sinon celui que désigne l'extension quand le navigateur n'a rien su
     * dire. {@code null} si rien n'est admis. Le contenu est vérifié ensuite :
     * l'extension ne fait qu'orienter la vérification, elle ne la remplace pas.
     */
    static String resolveType(String declaredType, String filename) {
        String declared = UploadedBinaryGuard.normalizeType(declaredType);
        int semicolon = declared.indexOf(';');
        if (semicolon >= 0) {
            declared = declared.substring(0, semicolon).trim();
        }
        if (ALLOWED_TYPES.containsKey(declared)) {
            return declared;
        }
        boolean silent = declared.isEmpty() || "application/octet-stream".equals(declared);
        if (!silent || filename == null) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        return TYPE_BY_EXTENSION.get(filename.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    /** Les premiers octets correspondent-ils au type retenu ? */
    static boolean contentMatches(String type, byte[] content) {
        if (content == null || content.length == 0) {
            return false;
        }
        return switch (type) {
            case DOC, XLS, PPT -> startsWith(content, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);
            case "application/xml", "text/xml" -> isText(content) && startsLikeXml(content);
            case "text/plain", "text/csv", "text/markdown" -> isText(content);
            default -> UploadedBinaryGuard.magicBytesMatch(type, content);
        };
    }

    /** SHA-256 hexadécimal du fichier d'origine. */
    static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** Ce qu'on confie au stockage : les octets et leur encodage ({@code null} = tels quels). */
    record Encoded(byte[] bytes, String contentEncoding) {}

    /** Compresse si le type s'y prête et si le gain le justifie ; rend l'original sinon. */
    static Encoded encodeForStorage(String type, byte[] content) {
        if (ALREADY_COMPRESSED.contains(type)) {
            return new Encoded(content, null);
        }
        byte[] gz = gzip(content);
        long threshold = (long) content.length * (100 - MIN_GAIN_PERCENT) / 100;
        return gz.length <= threshold ? new Encoded(gz, GZIP) : new Encoded(content, null);
    }

    static boolean isTextType(String type) {
        return TEXT_TYPES.contains(type);
    }

    private static byte[] gzip(byte[] content) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, content.length / 3));
        // Niveau par défaut : l'écart avec le niveau maximal tient en quelques pour
        // cent, pour un temps de compression deux à trois fois plus long.
        try (GZIPOutputStream gz = new GZIPOutputStream(out) {
            { def.setLevel(Deflater.DEFAULT_COMPRESSION); }
        }) {
            gz.write(content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static boolean isText(byte[] content) {
        int n = Math.min(content.length, TEXT_PROBE_BYTES);
        for (int i = 0; i < n; i++) {
            if (content[i] == 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean startsLikeXml(byte[] content) {
        int i = startsWith(content, 0xEF, 0xBB, 0xBF) ? 3 : 0;
        while (i < content.length && Character.isWhitespace(content[i])) {
            i++;
        }
        return i < content.length && content[i] == '<';
    }

    private static boolean startsWith(byte[] content, int... prefix) {
        if (content.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((content[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
