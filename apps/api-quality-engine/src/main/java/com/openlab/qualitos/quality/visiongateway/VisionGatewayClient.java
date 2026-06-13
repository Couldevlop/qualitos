package com.openlab.qualitos.quality.visiongateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client serveur-à-serveur vers le service de vision {@code ai-vision-5s} (CLAUDE.md §3.2 :
 * détection automatique des non-conformités 5S sur photo). Calqué EXACTEMENT sur
 * {@code AiGatewayClient} : le SPA appelle api-quality-engine (JWT utilisateur validé) ;
 * api-quality-engine relaie l'image vers ai-vision-5s en {@code multipart/form-data}.
 *
 * <p>Auth interne, mode configurable {@code qualitos.vision.auth} (ADR 0021) :
 * <ul>
 *   <li>{@code dev-claims} (défaut, dev uniquement) : en-tête {@code X-Dev-Claims}
 *       (JSON {@code {sub,tid,roles}}) — ai-vision-5s avec {@code AUTH_BYPASS=true}
 *       n'exige de toute façon aucun en-tête ;</li>
 *   <li>{@code bearer} (OBLIGATOIRE en prod) : jeton OIDC client_credentials obtenu via
 *       {@link ServiceTokenProvider} ({@code aud=api-ai-vision-5s}) + en-tête
 *       {@code X-Tenant-Id} (accepté côté service uniquement pour un {@code azp} de
 *       confiance). Échec d'obtention du jeton → fail-closed 503, pas de repli dev ;</li>
 *   <li>{@code none} : aucun en-tête (réseau de confiance/mTLS mesh, déconseillé).</li>
 * </ul>
 * Dans tous les modes, le tenant provient du {@link TenantContext} (JWT, JAMAIS du body,
 * règle 18.2 #2).
 *
 * <p>Le drapeau {@code qualitos.vision.enabled} est OFF par défaut : quand il est à
 * false, le client ne tente aucun appel réseau et lève {@link VisionUnavailableException}
 * (→ 503 propre), exactement comme le module IA reste inerte sans son back-end.
 */
@Component
public class VisionGatewayClient {

    /** Mode d'auth interne vers ai-vision-5s (propriété {@code qualitos.vision.auth}). */
    enum AuthMode {
        NONE, DEV_CLAIMS, BEARER;

        /** Parse stricte : une valeur inconnue doit faire échouer le boot (pas de magie). */
        static AuthMode parse(String raw) {
            return switch (raw == null ? "" : raw.trim().toLowerCase()) {
                case "none" -> NONE;
                case "dev-claims" -> DEV_CLAIMS;
                case "bearer" -> BEARER;
                default -> throw new IllegalArgumentException(
                        "qualitos.vision.auth invalide : '" + raw + "' (attendu : none | dev-claims | bearer)");
            };
        }
    }

    /** Sujet de service (déterministe) porté dans X-Dev-Claims pour l'appel interne. */
    private static final UUID SERVICE_SUBJECT = UUID.fromString("0000000a-0000-0000-0000-000000000a02");

    private final RestClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final boolean enabled;
    private final AuthMode authMode;
    private final ServiceTokenProvider serviceTokenProvider;

    public VisionGatewayClient(
            @Value("${qualitos.vision.enabled:false}") boolean enabled,
            @Value("${qualitos.vision.base-url:http://localhost:8086}") String baseUrl,
            @Value("${qualitos.vision.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${qualitos.vision.read-timeout-ms:30000}") int readTimeoutMs,
            @Value("${qualitos.vision.auth:dev-claims}") String authMode,
            ServiceTokenProvider serviceTokenProvider) {
        this.enabled = enabled;
        this.authMode = AuthMode.parse(authMode);
        this.serviceTokenProvider = serviceTokenProvider;
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(connectTimeoutMs);
        rf.setReadTimeout(readTimeoutMs);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(rf).build();
    }

    /**
     * Relaie une image vers {@code POST /v1/vision/5s/analyze} (champ multipart
     * {@code image}) et mappe la réponse JSON (snake_case) vers {@link VisionDto.VisionAnalysis}.
     * Le tenant provient du {@link TenantContext} (JWT), jamais du body.
     *
     * @throws VisionUnavailableException service désactivé (flag OFF), injoignable ou timeout (→ 503)
     * @throws VisionGatewayException     réponse vide/invalide ou erreur en aval (→ 502)
     */
    @SuppressWarnings("unchecked")
    public VisionDto.VisionAnalysis analyze(String contentType, String filename, byte[] image) {
        if (!enabled) {
            throw new VisionUnavailableException(
                    "Le service de vision 5S est désactivé (qualitos.vision.enabled=false)");
        }
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        UUID tenantId = UUID.fromString(TenantContext.getTenantId());

        RestClient.RequestBodySpec spec = client.post()
                .uri("/v1/vision/5s/analyze")
                .contentType(MediaType.MULTIPART_FORM_DATA);
        switch (authMode) {
            case DEV_CLAIMS -> spec = spec.header("X-Dev-Claims", devClaims(tenantId));
            // Jeton de service client_credentials + tenant par en-tête (le token de service
            // ne porte pas de tenant par requête ; ai-vision-5s n'accepte X-Tenant-Id que
            // d'un azp de confiance, cf. ADR 0021). getToken() échoue en
            // VisionUnavailableException → fail-closed 503 AVANT tout appel réseau.
            case BEARER -> spec = spec
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceTokenProvider.getToken())
                    .header("X-Tenant-Id", tenantId.toString());
            case NONE -> { /* aucun en-tête : auth déléguée au réseau (mTLS mesh) */ }
        }
        try {
            Map<String, Object> resp = spec
                    .body(multipartBody(contentType, filename, image))
                    .retrieve()
                    .body(Map.class);
            if (resp == null) {
                throw new VisionGatewayException("Réponse vide du service de vision 5S");
            }
            return toAnalysis(resp);
        } catch (RestClientException e) {
            // Connexion refusée / timeout / 5xx en aval : le service est de facto
            // indisponible pour le SPA → 503 (cohérent avec le flag OFF).
            throw new VisionUnavailableException(
                    "Service de vision 5S indisponible : " + e.getMessage(), e);
        }
    }

    private MultiValueMap<String, Object> multipartBody(String contentType, String filename, byte[] image) {
        ByteArrayResource resource = new ByteArrayResource(image) {
            @Override
            public String getFilename() {
                return filename == null || filename.isBlank() ? "image" : filename;
            }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        // Le contrat ai-vision-5s attend le champ "image".
        body.add("image", resource);
        return body;
    }

    private VisionDto.VisionAnalysis toAnalysis(Map<String, Object> resp) {
        Map<String, Object> scoreNode = asMap(resp.get("score"));
        VisionDto.VisionScore score = new VisionDto.VisionScore(
                intVal(scoreNode.get("seiri")),
                intVal(scoreNode.get("seiton")),
                intVal(scoreNode.get("seiso")),
                intVal(scoreNode.get("seiketsu")),
                intVal(scoreNode.get("shitsuke")),
                intVal(scoreNode.get("overall")));
        return new VisionDto.VisionAnalysis(
                str(resp.get("image_sha256")),
                intVal(resp.get("width")),
                intVal(resp.get("height")),
                score,
                findings(resp.get("findings")));
    }

    private List<VisionDto.VisionFinding> findings(Object o) {
        if (!(o instanceof List<?> l)) {
            return List.of();
        }
        return l.stream().map(this::asMap).map(f -> new VisionDto.VisionFinding(
                str(f.get("pillar")),
                str(f.get("description")),
                str(f.get("severity")),
                dbl(f.get("confidence")),
                intList(f.get("bbox")))).toList();
    }

    private String devClaims(UUID tenantId) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "sub", SERVICE_SUBJECT.toString(),
                    "tid", tenantId.toString(),
                    "roles", List.of("quality_manager")));
        } catch (Exception e) {
            throw new VisionGatewayException("Sérialisation des claims impossible", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    /** bbox = [x, y, w, h] ; {@code null} si non localisé (le record l'accepte). */
    private List<Integer> intList(Object o) {
        return o instanceof List<?> l
                ? l.stream().map(x -> x instanceof Number n ? n.intValue() : 0).toList()
                : null;
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private int intVal(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }

    private double dbl(Object o) {
        return o instanceof Number n ? n.doubleValue() : 0.0;
    }
}
