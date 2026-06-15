package com.openlab.qualitos.quality.nccluster;

import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NcClusterServiceTest {

    @Mock AiGatewayClient ai;
    @InjectMocks NcClusterService service;

    @Test
    void cluster_mapsFullGatewayResponse() {
        when(ai.clusterNc(any(), isNull(), isNull())).thenReturn(Map.of(
                "n", 5,
                "clustered_ratio", 0.8,
                "method", "dbscan",
                "clusters", List.of(Map.of(
                        "cluster_id", 0,
                        "indices", List.of(0, 1),
                        "size", 2,
                        "top_terms", List.of("fuite", "huile"))),
                "noise_indices", List.of(4)));

        NcClusterDto.ClusterResponse r = service.cluster(new NcClusterDto.ClusterRequest(
                List.of("fuite huile presse", "fuite huile presse ligne", "x", "y", "capteur"),
                null, null));

        assertThat(r.n()).isEqualTo(5);
        assertThat(r.clusteredRatio()).isEqualTo(0.8);
        assertThat(r.method()).isEqualTo("dbscan");
        assertThat(r.clusters()).hasSize(1);
        assertThat(r.clusters().get(0).topTerms()).containsExactly("fuite", "huile");
        assertThat(r.noiseIndices()).containsExactly(4);
    }

    @Test
    void cluster_toleratesMissingOrMistypedFields() {
        when(ai.clusterNc(any(), any(), any())).thenReturn(Map.of("clusters", "not-a-list"));

        NcClusterDto.ClusterResponse r = service.cluster(new NcClusterDto.ClusterRequest(
                List.of("a b", "a b"), 0.5, 3));

        assertThat(r.n()).isZero();
        assertThat(r.method()).isEmpty();
        assertThat(r.clusters()).isEmpty();
    }

    @Test
    void cluster_passesThresholdAndMinSamples() {
        when(ai.clusterNc(any(), eq(0.5), eq(3))).thenReturn(Map.of());

        service.cluster(new NcClusterDto.ClusterRequest(List.of("a b", "a c"), 0.5, 3));

        ArgumentCaptor<Double> thr = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Integer> ms = ArgumentCaptor.forClass(Integer.class);
        verify(ai).clusterNc(any(), thr.capture(), ms.capture());
        assertThat(thr.getValue()).isEqualTo(0.5);
        assertThat(ms.getValue()).isEqualTo(3);
    }
}
