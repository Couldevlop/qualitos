package com.openlab.qualitos.quality.itsm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Client ServiceNow (Table API : /api/now/table/incident).
 *
 * Auth : Basic (username + password/token). Pour OAuth2 (V2), prévoir un decorator.
 *
 * Format date ServiceNow : "yyyy-MM-dd HH:mm:ss" (UTC implicite).
 * Query incrémentale : sysparm_query=sys_updated_on>=<since>.
 */
@Component
public class ServiceNowClient implements ItsmProviderClient {

    private static final Logger log = LoggerFactory.getLogger(ServiceNowClient.class);

    /** Format date ServiceNow (UTC). */
    private static final DateTimeFormatter SN_DATE = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .toFormatter(Locale.ROOT);

    private final HttpClient http;
    private final ObjectMapper mapper;

    @org.springframework.beans.factory.annotation.Autowired
    public ServiceNowClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(), new ObjectMapper());
    }

    ServiceNowClient(HttpClient http, ObjectMapper mapper) {
        this.http = http;
        this.mapper = mapper;
    }

    @Override
    public ItsmProvider provider() { return ItsmProvider.SERVICENOW; }

    @Override
    public List<ExternalIncident> fetchIncidents(ItsmConnection conn, String secret, Instant since) {
        String base = conn.getBaseUrl().endsWith("/") ? conn.getBaseUrl().substring(0, conn.getBaseUrl().length() - 1)
                : conn.getBaseUrl();
        String query = since != null
                ? "sysparm_query=" + URLEncoder.encode(
                        "sys_updated_on>=" + SN_DATE.format(since.atZone(java.time.ZoneOffset.UTC)),
                        StandardCharsets.UTF_8)
                : "";
        String url = base + "/api/now/table/incident?sysparm_limit=200" + (query.isBlank() ? "" : "&" + query);

        String auth = Base64.getEncoder().encodeToString(
                ((conn.getUsername() == null ? "" : conn.getUsername()) + ":" + secret).getBytes(StandardCharsets.UTF_8));

        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("Authorization", "Basic " + auth)
                .GET()
                .build();

        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new ItsmSyncException("ServiceNow returned HTTP " + resp.statusCode());
            }
            JsonNode root = mapper.readTree(resp.body());
            JsonNode result = root.get("result");
            if (result == null || !result.isArray()) return List.of();
            List<ExternalIncident> out = new ArrayList<>(result.size());
            for (JsonNode n : result) {
                out.add(new ExternalIncident(
                        textOr(n, "sys_id"),
                        textOr(n, "short_description"),
                        textOr(n, "description"),
                        textOr(n, "state"),
                        textOr(n, "priority"),
                        parseInstant(textOr(n, "sys_created_on")),
                        parseInstant(textOr(n, "sys_updated_on")),
                        base + "/nav_to.do?uri=incident.do?sys_id=" + textOr(n, "sys_id")
                ));
            }
            return out;
        } catch (java.io.IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("ServiceNow fetch failed: {}", e.getMessage());
            throw new ItsmSyncException("ServiceNow fetch failed", e);
        }
    }

    private static String textOr(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private static Instant parseInstant(String snDate) {
        if (snDate == null || snDate.isBlank()) return null;
        try {
            return SN_DATE.parse(snDate, java.time.LocalDateTime::from).atZone(java.time.ZoneOffset.UTC).toInstant();
        } catch (Exception e) {
            return null;
        }
    }
}
