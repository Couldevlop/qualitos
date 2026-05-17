package com.openlab.qualitos.quality.ims.presentation.rest;

import com.openlab.qualitos.quality.ims.application.usecase.GetCoverageMatrixUseCase;
import com.openlab.qualitos.quality.ims.domain.model.CoverageMatrix;
import com.openlab.qualitos.quality.ims.domain.service.CoverageMatrixDomainService;
import com.openlab.qualitos.quality.ims.presentation.dto.CoverageMatrixDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Endpoint IMS — matrice de couverture multi-normes (CLAUDE.md §8.9).
 *
 * Sécurité :
 * - OWASP A01 : {@code @PreAuthorize} restreint l'accès aux rôles autorisés.
 *   Le {@code tenant_id} est lu depuis le JWT par le {@code TenantJwtFilter},
 *   pas depuis l'URL/query.
 * - OWASP A03 : la liste de codes de normes est validée par regex (allow-list
 *   alphanumérique + tirets) avant transmission au use case.
 * - OWASP API4 (Rate Limiting) : géré globalement par api-gateway / WAF.
 */
@RestController
@RequestMapping("/api/v1/standards/coverage-matrix")
@Tag(name = "Standards — IMS Coverage Matrix",
        description = "Matrice de couverture multi-normes (Integrated Management System) — §8.9")
public class CoverageMatrixController {

    private static final Pattern SAFE_CODE = Pattern.compile("^[a-z0-9][a-z0-9\\-]{1,99}$");

    private final GetCoverageMatrixUseCase useCase;
    private final CoverageMatrixDomainService domainService;

    public CoverageMatrixController(GetCoverageMatrixUseCase useCase,
                                    CoverageMatrixDomainService domainService) {
        this.useCase = Objects.requireNonNull(useCase);
        this.domainService = Objects.requireNonNull(domainService);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','AUDITOR','ADMIN_TENANT','SUPER_ADMIN')")
    @Operation(summary = "Matrice de couverture multi-normes du tenant",
            description = "Si 'codes' est omis, utilise les normes adoptées par le tenant courant.")
    public CoverageMatrixDto.CoverageMatrixResponse getCoverageMatrix(
            @Parameter(description = "Codes de normes séparés par virgule (ex: iso-9001,iso-14001,iso-45001)")
            @RequestParam(name = "codes", required = false) String codes) {

        List<String> normCodes = parseAndValidateCodes(codes);
        CoverageMatrix matrix = useCase.execute(normCodes);
        double reuse = domainService.computeReuseRatio(matrix);
        return CoverageMatrixDto.from(matrix, reuse);
    }

    private List<String> parseAndValidateCodes(String codesParam) {
        if (codesParam == null || codesParam.isBlank()) {
            return List.of();
        }
        return Arrays.stream(codesParam.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::validateCode)
                .toList();
    }

    private String validateCode(String c) {
        if (!SAFE_CODE.matcher(c).matches()) {
            throw new IllegalArgumentException("Invalid standard code: " + c);
        }
        return c;
    }
}
