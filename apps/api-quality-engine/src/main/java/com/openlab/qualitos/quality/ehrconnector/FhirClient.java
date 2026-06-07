package com.openlab.qualitos.quality.ehrconnector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Client REST LÉGER pour interroger un serveur HL7 FHIR (R4/R5), sans dépendance
 * HAPI FHIR (§13.3). Parsing tolérant du {@code Bundle} via Jackson : on lit
 * {@code entry[].resource} et on n'extrait que des champs techniques / de
 * classification (cf. {@link FhirResource}, aucune PII).
 *
 * <p>Ressources interrogées : {@code Observation} et {@code DiagnosticReport},
 * filtrées sur les éléments « adverse event / abnormal / patient-safety » :
 * <ul>
 *   <li>si {@link EhrConnection#getResourceCategory()} est défini → on l'utilise
 *       comme filtre {@code category=...} côté serveur ;</li>
 *   <li>sinon → filtre serveur {@code Observation?interpretation=A} (abnormal) ;</li>
 *   <li>dans tous les cas, filtrage défensif côté client : on ne conserve que les
 *       ressources à interprétation anormale/critique.</li>
 * </ul>
 *
 * <p>Auth : Basic (username:secret) ou Bearer (secret), selon {@link EhrAuthMode}.
 * Le secret est passé en argument (déchiffré en mémoire par le service) et n'est
 * jamais loggé.</p>
 */
@Component
public class FhirClient {

    private static final Logger log = LoggerFactory.getLogger(FhirClient.class);

    /** Types de ressources FHIR scrutés pour la sécurité patient. */
    static final List<String> RESOURCE_TYPES = List.of("Observation", "DiagnosticReport");

    /**
     * Codes d'interprétation FHIR (HL7 v3 ObservationInterpretation) considérés
     * comme anormaux / signaux de sécurité patient.
     */
    private static final Set<String> ABNORMAL_INTERPRETATIONS = Set.of(
            "A",   // Abnormal
            "AA",  // Critically abnormal
            "H",   // High
            "HH",  // Critically high
            "L",   // Low
            "LL",  // Critically low
            "HU",  // Significantly high
            "LU",  // Significantly low
            "POS", // Positive
            "U",   // Significant change up
            "D",   // Significant change down
            "W"    // Worse
    );

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE_TIME;

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper mapper;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    /**
     * Quand {@code true}, on applique nos timeouts via un {@link SimpleClientHttpRequestFactory}.
     * Les tests fournissent un builder déjà branché sur {@code MockRestServiceServer} et
     * passent {@code false} pour ne pas écraser l'intercepteur du mock.
     */
    private final boolean applyTimeouts;

    public FhirClient() {
        this(RestClient.builder(), new ObjectMapper(), 5000, 15000, true);
    }

    FhirClient(RestClient.Builder restClientBuilder, ObjectMapper mapper,
               int connectTimeoutMs, int readTimeoutMs, boolean applyTimeouts) {
        this.restClientBuilder = restClientBuilder;
        this.mapper = mapper;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.applyTimeouts = applyTimeouts;
    }

    /**
     * Récupère les ressources FHIR pertinentes (anormales) modifiées depuis
     * {@code since}.
     *
     * @param conn   connexion EHR (base url, auth mode, catégorie de filtrage)
     * @param secret secret déjà déchiffré (mot de passe Basic ou jeton Bearer)
     * @param since  borne basse de modification ({@code _lastUpdated=gt...}), ou null
     * @return liste de ressources non-PII (peut être vide), jamais null
     * @throws EhrSyncException sur erreur réseau / HTTP non-2xx / réponse illisible
     */
    public List<FhirResource> fetchAdverseResources(EhrConnection conn, String secret, Instant since) {
        RestClient client = buildClient(conn, secret);
        List<FhirResource> out = new ArrayList<>();
        for (String type : RESOURCE_TYPES) {
            out.addAll(fetchType(client, conn, type, since));
        }
        return out;
    }

    private List<FhirResource> fetchType(RestClient client, EhrConnection conn, String type, Instant since) {
        String path = buildPath(conn, type, since);
        String bundleJson;
        try {
            bundleJson = client.get()
                    .uri(path)
                    .accept(MediaType.valueOf("application/fhir+json"), MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            // Pas de PII ni de secret dans le message.
            log.warn("FHIR fetch failed for {} on connection {}: {}", type, conn.getId(), e.getMessage());
            throw new EhrSyncException("FHIR " + type + " fetch failed: " + e.getMessage(), e);
        }
        return parseBundle(bundleJson, type);
    }

    /** Construit le chemin relatif {@code /<Type>?...} (filtre catégorie/abnormal + _lastUpdated). */
    private String buildPath(EhrConnection conn, String type, Instant since) {
        UriComponentsBuilder b = UriComponentsBuilder.fromPath("/" + type).queryParam("_count", "200");
        if (conn.getResourceCategory() != null && !conn.getResourceCategory().isBlank()) {
            b.queryParam("category", conn.getResourceCategory());
        } else if ("Observation".equals(type)) {
            // Filtre serveur « anormal » disponible sur Observation.
            b.queryParam("interpretation", "A");
        }
        if (since != null) {
            b.queryParam("_lastUpdated", "gt" + ISO.format(OffsetDateTime.ofInstant(since, java.time.ZoneOffset.UTC)));
        }
        return b.build().toUriString();
    }

    /**
     * Parse tolérant d'un Bundle FHIR : itère {@code entry[].resource}, garde celles
     * du bon type avec une interprétation anormale, dédoublonne par id.
     */
    List<FhirResource> parseBundle(String json, String requestedType) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (Exception e) {
            throw new EhrSyncException("FHIR bundle parse error: " + e.getMessage(), e);
        }
        JsonNode entries = root.path("entry");
        if (!entries.isArray()) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<FhirResource> out = new ArrayList<>();
        for (JsonNode entry : entries) {
            JsonNode res = entry.path("resource");
            if (res.isMissingNode() || res.isNull()) {
                continue;
            }
            String resourceType = text(res, "resourceType");
            if (resourceType == null) {
                resourceType = requestedType;
            }
            String id = text(res, "id");
            if (id == null || id.isBlank()) {
                continue;
            }
            String interpretation = extractInterpretation(res);
            if (!isAbnormal(interpretation)) {
                continue; // signal faible / normal → on ignore (pas une NC patient-safety)
            }
            if (!seen.add(resourceType + "/" + id)) {
                continue; // dédoublonnage intra-bundle
            }
            out.add(new FhirResource(
                    resourceType,
                    id,
                    extractCode(res),
                    extractCodeDisplay(res),
                    text(res, "status"),
                    interpretation,
                    extractEffective(res)
            ));
        }
        return out;
    }

    private boolean isAbnormal(String interpretation) {
        return interpretation != null && ABNORMAL_INTERPRETATIONS.contains(interpretation.toUpperCase());
    }

    /** interpretation[0].coding[0].code (FHIR R4/R5). */
    private String extractInterpretation(JsonNode res) {
        JsonNode interp = res.path("interpretation");
        if (interp.isArray() && !interp.isEmpty()) {
            return firstCodingCode(interp.get(0));
        }
        return null;
    }

    /** code.coding[0].code */
    private String extractCode(JsonNode res) {
        return firstCodingCode(res.path("code"));
    }

    /** code.coding[0].display (libellé terminologique, pas une donnée patient). */
    private String extractCodeDisplay(JsonNode res) {
        JsonNode coding = res.path("code").path("coding");
        if (coding.isArray() && !coding.isEmpty()) {
            String display = text(coding.get(0), "display");
            if (display != null) {
                return display;
            }
        }
        return text(res.path("code"), "text");
    }

    /** effectiveDateTime (Observation) ou effectivePeriod.start, sinon issued. */
    private Instant extractEffective(JsonNode res) {
        Instant dt = parseInstant(text(res, "effectiveDateTime"));
        if (dt != null) return dt;
        dt = parseInstant(text(res.path("effectivePeriod"), "start"));
        if (dt != null) return dt;
        return parseInstant(text(res, "issued"));
    }

    private static String firstCodingCode(JsonNode codeableConcept) {
        JsonNode coding = codeableConcept.path("coding");
        if (coding.isArray() && !coding.isEmpty()) {
            return text(coding.get(0), "code");
        }
        return null;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return (v.isMissingNode() || v.isNull()) ? null : v.asText();
    }

    private static Instant parseInstant(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return OffsetDateTime.parse(iso).toInstant();
        } catch (Exception e) {
            // FHIR date sans offset/heure (ex. "2026-05-10") → non bloquant.
            return null;
        }
    }

    private RestClient buildClient(EhrConnection conn, String secret) {
        String base = conn.getFhirBaseUrl();
        if (base != null && base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        RestClient.Builder builder = restClientBuilder.clone()
                .baseUrl(base)
                .defaultHeader("Authorization", authHeader(conn, secret));
        if (applyTimeouts) {
            SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
            rf.setConnectTimeout(connectTimeoutMs);
            rf.setReadTimeout(readTimeoutMs);
            builder.requestFactory(rf);
        }
        return builder.build();
    }

    private static String authHeader(EhrConnection conn, String secret) {
        if (conn.getAuthMode() == EhrAuthMode.BEARER) {
            return "Bearer " + (secret == null ? "" : secret);
        }
        String user = conn.getUsername() == null ? "" : conn.getUsername();
        String creds = user + ":" + (secret == null ? "" : secret);
        return "Basic " + java.util.Base64.getEncoder()
                .encodeToString(creds.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
