package com.openlab.qualitos.quality.ims.application.usecase;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.ims.domain.model.ClauseMapping;
import com.openlab.qualitos.quality.ims.domain.model.CoverageMatrix;
import com.openlab.qualitos.quality.ims.domain.port.ClauseMappingRepository;
import com.openlab.qualitos.quality.ims.domain.port.TenantStandardCodesProvider;
import com.openlab.qualitos.quality.ims.domain.service.CoverageMatrixDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Use case applicatif : retourne la matrice de couverture multi-normes (IMS)
 * pour le tenant courant, restreinte au set de normes fourni (ou
 * automatiquement aux normes adoptées si non fourni).
 *
 * Multi-tenancy : tenant_id provient EXCLUSIVEMENT du JWT (TenantContext).
 */
@Service
public class GetCoverageMatrixUseCase {

    private final ClauseMappingRepository mappingRepo;
    private final TenantStandardCodesProvider tenantStandardCodes;
    private final CoverageMatrixDomainService domainService;

    public GetCoverageMatrixUseCase(ClauseMappingRepository mappingRepo,
                                    TenantStandardCodesProvider tenantStandardCodes,
                                    CoverageMatrixDomainService domainService) {
        this.mappingRepo = Objects.requireNonNull(mappingRepo);
        this.tenantStandardCodes = Objects.requireNonNull(tenantStandardCodes);
        this.domainService = Objects.requireNonNull(domainService);
    }

    @Transactional(readOnly = true)
    public CoverageMatrix execute(List<String> requestedCodes) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new MissingTenantContextException();
        }

        // Filtre tenant — par défaut, utiliser les normes adoptées par le tenant.
        List<String> codes = (requestedCodes == null || requestedCodes.isEmpty())
                ? tenantStandardCodes.findAdoptedStandardCodes()
                : requestedCodes;

        // Sanity : si aucun code, matrice vide (mais tenant validé).
        if (codes == null || codes.isEmpty()) {
            return domainService.build(tenantId, List.of(), List.of());
        }

        List<ClauseMapping> mappings = mappingRepo.findMappingsBetween(codes);
        return domainService.build(tenantId, codes, mappings);
    }
}
