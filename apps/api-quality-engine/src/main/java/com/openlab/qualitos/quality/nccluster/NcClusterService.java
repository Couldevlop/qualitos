package com.openlab.qualitos.quality.nccluster;

import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service applicatif de clustering de NC (§4.3, §12.1) : relaie les textes vers la passerelle
 * IA ({@link AiGatewayClient}) et mappe la réponse JSON brute de {@code ai-service} vers un DTO
 * typé. Aucun calcul ici : TF-IDF + DBSCAN sont faits par {@code ai-service} (NumPy). L'engine
 * relaie et présente (clean architecture), comme pour SPC/anomaly/forecast/NLQ.
 *
 * <p>Mapping tolérant (mêmes conventions que {@code SpcService}) : clés ai-service en snake_case
 * ({@code clustered_ratio}, {@code noise_indices}, {@code cluster_id}, {@code top_terms}).
 */
@Service
public class NcClusterService {

    private final AiGatewayClient ai;

    public NcClusterService(AiGatewayClient ai) {
        this.ai = ai;
    }

    public NcClusterDto.ClusterResponse cluster(NcClusterDto.ClusterRequest request) {
        Map<String, Object> resp = ai.clusterNc(
                request.texts(), request.threshold(), request.minSamples());
        return toResponse(resp);
    }

    // ---- mapping réponse ai-service ----

    private NcClusterDto.ClusterResponse toResponse(Map<String, Object> resp) {
        return new NcClusterDto.ClusterResponse(
                intVal(resp.get("n")),
                dbl(resp.get("clustered_ratio")),
                str(resp.get("method")),
                clusters(resp.get("clusters")),
                intList(resp.get("noise_indices")));
    }

    private List<NcClusterDto.Cluster> clusters(Object o) {
        if (!(o instanceof List<?> l)) {
            return List.of();
        }
        return l.stream().map(this::asMap).map(c -> new NcClusterDto.Cluster(
                intVal(c.get("cluster_id")),
                intList(c.get("indices")),
                intVal(c.get("size")),
                strList(c.get("top_terms")))).toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private List<Integer> intList(Object o) {
        return o instanceof List<?> l
                ? l.stream().map(x -> x instanceof Number n ? n.intValue() : 0).toList()
                : List.of();
    }

    private List<String> strList(Object o) {
        return o instanceof List<?> l ? l.stream().map(this::str).toList() : List.of();
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
