package com.openlab.qualitos.licensing.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.openlab.qualitos.licensing.domain.LicenseException;

import java.io.IOException;
import java.util.Base64;

/**
 * Le fichier de licence : le contenu signé et sa signature, en JSON.
 *
 * <pre>
 * { "format": "qualitos-license/1", "payload": "&lt;base64url&gt;", "signature": "&lt;enveloppe&gt;" }
 * </pre>
 *
 * <p>Un fichier texte, lisible à l'œil, qui se copie sur une clé USB ou se colle
 * dans un secret Kubernetes.
 */
public record LicenseFile(byte[] payload, String signature) {

    public static final String FORMAT = "qualitos-license/1";
    /** Bien au-delà d'une licence réelle (≈ 8 Ko avec ML-DSA-65) : borne la lecture. */
    public static final int MAX_BYTES = 64 * 1024;

    public LicenseFile {
        if (payload == null || payload.length == 0 || signature == null || signature.isBlank()) {
            throw new LicenseException("Fichier de licence incomplet.");
        }
    }

    public String write() {
        return "{\n  \"format\": \"" + FORMAT + "\",\n  \"payload\": \""
                + Base64.getUrlEncoder().withoutPadding().encodeToString(payload) + "\",\n  \"signature\": \""
                + signature + "\"\n}\n";
    }

    public static LicenseFile parse(String content) {
        if (content == null || content.isBlank()) {
            throw new LicenseException("Fichier de licence vide.");
        }
        if (content.length() > MAX_BYTES) {
            throw new LicenseException("Fichier de licence trop volumineux.");
        }
        JsonNode node;
        try {
            node = LicenseCodec.json().readTree(content);
        } catch (IOException e) {
            throw new LicenseException("Fichier de licence illisible.", e);
        }
        if (node == null || !FORMAT.equals(text(node, "format"))) {
            throw new LicenseException("Ce fichier n'est pas une licence QualitOS.");
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(text(node, "payload"));
            return new LicenseFile(payload, text(node, "signature"));
        } catch (IllegalArgumentException e) {
            throw new LicenseException("Contenu de licence mal encodé.", e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || !v.isTextual() ? "" : v.asText();
    }
}
