package com.openlab.qualitos.quality.erpconnector;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

/**
 * Base commune des clients ERP REST/OData. Mutualise :
 * <ul>
 *   <li>la construction d'un {@link RestClient} (timeouts via {@code RestClient.Builder}) ;</li>
 *   <li>l'auth (Basic ou Bearer) injectée par header ;</li>
 *   <li>un parsing JSON/OData TOLÉRANT : sait extraire la collection que la racine soit
 *       un tableau nu, {@code {"value":[...]}} (OData v4 / Dynamics / SAP), {@code {"d":{"results":[...]}}}
 *       (OData v2 / SAP Gateway) ou {@code {"items":[...]}} (Oracle Fusion REST).</li>
 * </ul>
 *
 * <p>Pas de SDK propriétaire : un RestClient générique paramétrable par baseUrl + chemins.
 * Le secret n'est jamais loggé.
 */
abstract class AbstractErpRestClient implements ErpProviderClient {

    private static final Logger log = LoggerFactory.getLogger(AbstractErpRestClient.class);

    /** Schéma d'authentification HTTP supporté. */
    enum AuthScheme { BASIC, BEARER }

    protected final RestClient client;

    AbstractErpRestClient(RestClient client) {
        this.client = client;
    }

    /** Chemin (relatif à baseUrl) de la collection fournisseurs. */
    protected abstract String suppliersPath();

    /** Chemin (relatif à baseUrl) de la collection d'indicateurs de production. */
    protected abstract String productionKpisPath();

    /** Schéma d'auth du provider. */
    protected abstract AuthScheme authScheme();

    @Override
    public List<ExternalSupplier> fetchSuppliers(ErpConnection conn, String secret) {
        JsonNode collection = getCollection(conn, secret, suppliersPath());
        List<ExternalSupplier> out = new ArrayList<>(collection.size());
        for (JsonNode n : collection) {
            String code = firstText(n, "code", "Code", "Supplier", "SupplierNumber", "BusinessPartner",
                    "vendorid", "VendorAccountNumber", "id");
            if (code == null || code.isBlank()) continue;
            out.add(new ExternalSupplier(
                    code,
                    firstText(n, "name", "Name", "SupplierName", "BusinessPartnerName",
                            "OrganizationBPName1", "vendorname"),
                    firstText(n, "category", "Category", "SupplierCategory", "vendorgroupid",
                            "type"),
                    firstText(n, "countryCode", "country", "Country", "CountryRegionCode"),
                    firstText(n, "email", "Email", "EmailAddress", "primaryemailaddress")));
        }
        return out;
    }

    @Override
    public List<ExternalProductionKpi> fetchProductionKpis(ErpConnection conn, String secret) {
        JsonNode collection = getCollection(conn, secret, productionKpisPath());
        List<ExternalProductionKpi> out = new ArrayList<>(collection.size());
        for (JsonNode n : collection) {
            String code = firstText(n, "kpiCode", "code", "Code", "indicator", "KpiCode");
            JsonNode valueNode = firstNode(n, "value", "Value", "amount", "measure");
            if (code == null || code.isBlank() || valueNode == null || valueNode.isNull()) continue;
            out.add(new ExternalProductionKpi(
                    code,
                    new BigDecimal(valueNode.asText()),
                    firstText(n, "unit", "Unit", "uom"),
                    parseInstant(firstText(n, "periodStart", "PeriodStart", "start", "from")),
                    parseInstant(firstText(n, "periodEnd", "PeriodEnd", "end", "to"))));
        }
        return out;
    }

    /**
     * GET la ressource et renvoie la collection extraite (tolérant). Lève
     * {@link ErpSyncException} sur erreur réseau / HTTP / JSON.
     */
    private JsonNode getCollection(ErpConnection conn, String secret, String path) {
        try {
            JsonNode root = client.get()
                    .uri(absoluteUrl(conn.getBaseUrl(), path))
                    .headers(authHeaders(conn, secret))
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .retrieve()
                    .body(JsonNode.class);
            return extractCollection(root);
        } catch (RestClientException e) {
            // Connexion refusée, timeout, 4xx/5xx en aval, body illisible.
            log.warn("ERP {} fetch failed on {}: {}", provider(), path, e.getMessage());
            throw new ErpSyncException(provider() + " fetch failed: " + e.getMessage(), e);
        }
    }

    /** Header d'auth selon le schéma du provider. */
    private Consumer<HttpHeaders> authHeaders(ErpConnection conn, String secret) {
        return headers -> {
            if (authScheme() == AuthScheme.BEARER) {
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + secret);
            } else {
                String raw = (conn.getUsername() == null ? "" : conn.getUsername()) + ":" + secret;
                String b64 = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
                headers.set(HttpHeaders.AUTHORIZATION, "Basic " + b64);
            }
        };
    }

    /** Concatène baseUrl (sans slash final) et path (avec slash initial). */
    static String absoluteUrl(String baseUrl, String path) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String p = path.startsWith("/") ? path : "/" + path;
        return base + p;
    }

    /** Extraction tolérante de la collection, quelle que soit l'enveloppe OData/REST. */
    static JsonNode extractCollection(JsonNode root) {
        if (root == null || root.isNull()) {
            throw new ErpSyncException("Empty ERP response");
        }
        if (root.isArray()) return root;
        // OData v4 / Dynamics / SAP v4 : { "value": [...] }
        if (root.has("value") && root.get("value").isArray()) return root.get("value");
        // OData v2 / SAP Gateway : { "d": { "results": [...] } } ou { "d": [...] }
        if (root.has("d")) {
            JsonNode d = root.get("d");
            if (d.isArray()) return d;
            if (d.has("results") && d.get("results").isArray()) return d.get("results");
        }
        // Oracle Fusion REST : { "items": [...] }
        if (root.has("items") && root.get("items").isArray()) return root.get("items");
        throw new ErpSyncException("Unrecognized ERP collection shape");
    }

    /** Première valeur textuelle non-nulle parmi des noms de champ candidats. */
    static String firstText(JsonNode n, String... fields) {
        JsonNode node = firstNode(n, fields);
        return (node == null || node.isNull()) ? null : node.asText();
    }

    static JsonNode firstNode(JsonNode n, String... fields) {
        for (String f : fields) {
            JsonNode v = n.get(f);
            if (v != null && !v.isNull()) return v;
        }
        return null;
    }

    static Instant parseInstant(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return OffsetDateTime.parse(s).toInstant();
        } catch (Exception ignore) {
            // tolère "yyyy-MM-ddTHH:mm:ssZ" déjà couvert, sinon ISO instant
        }
        try {
            return Instant.parse(s);
        } catch (Exception e) {
            return null;
        }
    }
}
