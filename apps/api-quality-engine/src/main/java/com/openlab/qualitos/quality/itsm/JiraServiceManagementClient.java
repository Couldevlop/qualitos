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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Client Jira Service Management (REST /rest/api/3/search avec JQL).
 *
 * Auth : Basic email + API token (Atlassian Cloud convention).
 *
 * JQL incrémentale : project = <scope> AND updated >= "<since>" ORDER BY updated ASC
 */
@Component
public class JiraServiceManagementClient implements ItsmProviderClient {

    private static final Logger log = LoggerFactory.getLogger(JiraServiceManagementClient.class);

    /** Jira accepte des dates au format "yyyy-MM-dd HH:mm" pour JQL "updated >=". */
    private static final DateTimeFormatter JQL_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(java.time.ZoneOffset.UTC);

    private final HttpClient http;
    private final ObjectMapper mapper;

    public JiraServiceManagementClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(), new ObjectMapper());
    }

    JiraServiceManagementClient(HttpClient http, ObjectMapper mapper) {
        this.http = http;
        this.mapper = mapper;
    }

    @Override
    public ItsmProvider provider() { return ItsmProvider.JIRA_SM; }

    @Override
    public List<ExternalIncident> fetchIncidents(ItsmConnection conn, String secret, Instant since) {
        String base = conn.getBaseUrl().endsWith("/") ? conn.getBaseUrl().substring(0, conn.getBaseUrl().length() - 1)
                : conn.getBaseUrl();
        String project = conn.getExternalScope() == null ? null : conn.getExternalScope();
        StringBuilder jql = new StringBuilder();
        if (project != null && !project.isBlank()) jql.append("project = ").append(project);
        if (since != null) {
            if (!jql.isEmpty()) jql.append(" AND ");
            jql.append("updated >= \"").append(JQL_DATE.format(since)).append("\"");
        }
        if (jql.isEmpty()) jql.append("issuetype is not EMPTY");
        jql.append(" ORDER BY updated ASC");

        String url = base + "/rest/api/3/search?maxResults=200&fields=summary,description,status,priority,created,updated"
                + "&jql=" + URLEncoder.encode(jql.toString(), StandardCharsets.UTF_8);

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
                throw new ItsmSyncException("Jira returned HTTP " + resp.statusCode());
            }
            JsonNode root = mapper.readTree(resp.body());
            JsonNode issues = root.get("issues");
            if (issues == null || !issues.isArray()) return List.of();
            List<ExternalIncident> out = new ArrayList<>(issues.size());
            for (JsonNode issue : issues) {
                JsonNode fields = issue.get("fields");
                JsonNode status = fields == null ? null : fields.get("status");
                JsonNode prio = fields == null ? null : fields.get("priority");
                String key = textOr(issue, "key");
                out.add(new ExternalIncident(
                        key,
                        textOf(fields, "summary"),
                        textOf(fields, "description"),
                        status == null ? null : textOr(status, "name"),
                        prio == null ? null : textOr(prio, "name"),
                        parseInstant(textOf(fields, "created")),
                        parseInstant(textOf(fields, "updated")),
                        base + "/browse/" + key
                ));
            }
            return out;
        } catch (java.io.IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("Jira fetch failed: {}", e.getMessage());
            throw new ItsmSyncException("Jira fetch failed", e);
        }
    }

    private static String textOr(JsonNode n, String f) {
        if (n == null) return null;
        JsonNode v = n.get(f);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private static String textOf(JsonNode parent, String f) {
        return parent == null ? null : textOr(parent, f);
    }

    /** Jira Cloud renvoie "yyyy-MM-dd'T'HH:mm:ss.SSSZ" (offset compact, ex "+0000").
     *  Le {@code DateTimeFormatter.ISO_OFFSET_DATE_TIME} attend "+00:00", on tente
     *  d'abord ce format puis on retombe sur un parseur custom. */
    private static final DateTimeFormatter JIRA_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private static Instant parseInstant(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return java.time.OffsetDateTime.parse(iso).toInstant();
        } catch (Exception ignore) {
            try {
                return java.time.OffsetDateTime.parse(iso, JIRA_TS).toInstant();
            } catch (Exception e) {
                return null;
            }
        }
    }
}
