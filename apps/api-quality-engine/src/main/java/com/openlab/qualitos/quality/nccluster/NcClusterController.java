package com.openlab.qualitos.quality.nccluster;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de clustering de non-conformités (§4.3, §12.1). POST car l'appel a un effet de bord
 * (relais vers l'inférence d'{@code ai-service} : TF-IDF + DBSCAN). Authentifié (SecurityConfig
 * {@code anyRequest().authenticated()}, comme {@code SpcController}) ; le tenant est dérivé du
 * JWT côté passerelle, jamais du corps de requête (règle 18.2 #2).
 */
@RestController
@RequestMapping("/api/v1/ai/nc-clusters")
public class NcClusterController {

    private final NcClusterService ncClusterService;

    public NcClusterController(NcClusterService ncClusterService) {
        this.ncClusterService = ncClusterService;
    }

    @PostMapping
    public NcClusterDto.ClusterResponse cluster(@Valid @RequestBody NcClusterDto.ClusterRequest request) {
        return ncClusterService.cluster(request);
    }
}
