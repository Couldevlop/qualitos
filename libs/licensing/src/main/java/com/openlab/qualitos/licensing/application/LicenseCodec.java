package com.openlab.qualitos.licensing.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.licensing.domain.License;
import com.openlab.qualitos.licensing.domain.LicenseException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Le contenu d'une licence en JSON — ce que l'éditeur signe.
 *
 * <p>La signature porte sur les OCTETS exacts du contenu, transportés tels quels
 * dans le fichier : aucune forme canonique à recalculer, donc aucune ambiguïté
 * entre ce qui a été signé et ce qui est lu. La lecture est stricte : un champ
 * inconnu est refusé plutôt qu'ignoré.
 */
public final class LicenseCodec {

    /** La forme JSON d'une licence ; les noms sont ceux du fichier. */
    record Payload(int version, String licenseId, String customer, UUID tenantId, String tier,
                   List<String> modules, int maxUsers, Instant issuedAt, Instant notBefore, Instant expiresAt,
                   int graceDays) {}

    static final int VERSION = 1;

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES);

    private LicenseCodec() {}

    public static byte[] toBytes(License l) {
        Payload p = new Payload(VERSION, l.licenseId(), l.customer(), l.tenantId(), l.tier(),
                List.copyOf(new TreeSet<>(l.modules())), l.maxUsers(), l.issuedAt(), l.notBefore(), l.expiresAt(),
                l.graceDays());
        try {
            return JSON.writeValueAsBytes(p);
        } catch (JsonProcessingException e) {
            throw new LicenseException("Licence impossible à écrire.", e);
        }
    }

    public static License fromBytes(byte[] bytes) {
        Payload p;
        try {
            p = JSON.readValue(bytes, Payload.class);
        } catch (java.io.IOException e) {
            throw new LicenseException("Contenu de licence illisible.", e);
        }
        if (p.version() != VERSION) {
            throw new LicenseException("Version de licence non prise en charge : " + p.version());
        }
        return new License(p.licenseId(), p.customer(), p.tenantId(), p.tier(),
                p.modules() == null ? null : new TreeSet<>(p.modules()), p.maxUsers(), p.issuedAt(),
                p.notBefore(), p.expiresAt(), p.graceDays());
    }

    /** Lit une demande de licence écrite à la main par l'éditeur (même forme, version facultative). */
    public static License fromRequest(String json) {
        try {
            com.fasterxml.jackson.databind.JsonNode node = JSON.readTree(json);
            if (!(node instanceof com.fasterxml.jackson.databind.node.ObjectNode obj)) {
                throw new LicenseException("La demande de licence doit être un objet JSON.");
            }
            if (!obj.has("version")) {
                obj.put("version", VERSION);
            }
            return fromBytes(JSON.writeValueAsString(obj).getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            throw new LicenseException("Demande de licence illisible.", e);
        }
    }

    static ObjectMapper json() {
        return JSON;
    }
}
