package com.openlab.qualitos.quality.revisionrequests.web;

import com.openlab.qualitos.quality.product.domain.ProductLookup;
import com.openlab.qualitos.quality.product.domain.ProductNotFoundException;
import com.openlab.qualitos.quality.revisionrequests.domain.FailureModeMatcher;
import com.openlab.qualitos.quality.risk.FmeaItem;
import com.openlab.qualitos.quality.risk.FmeaItemRepository;
import com.openlab.qualitos.quality.risk.FmeaProject;
import com.openlab.qualitos.quality.risk.FmeaProjectRepository;
import com.openlab.qualitos.quality.risk.FmeaStatus;
import com.openlab.qualitos.quality.risk.FmeaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Propose à la saisie d'une non-conformité les modes de défaillance déjà analysés
 * qui lui ressemblent.
 *
 * <p>Le calcul reste local et déterministe : il tourne pendant qu'un opérateur
 * décrit un défaut au poste, et faire dépendre cette saisie d'un service
 * d'inférence reviendrait à empêcher de déclarer un défaut quand ce service tombe.
 */
@RestController
@RequestMapping("/api/v1/products/{productId}/failure-mode-suggestions")
@Validated
@PreAuthorize("isAuthenticated()")
public class FailureModeSuggestionController {

    /** Au-delà de trois, l'utilisateur ne choisit plus, il subit une liste. */
    private static final int MAX_SUGGESTIONS = 3;

    private final ProductLookup products;
    private final FmeaProjectRepository projects;
    private final FmeaItemRepository items;

    public FailureModeSuggestionController(ProductLookup products, FmeaProjectRepository projects,
                                           FmeaItemRepository items) {
        this.products = products;
        this.items = items;
        this.projects = projects;
    }

    @GetMapping
    public List<FailureModeMatcher.Suggestion> suggest(
            @PathVariable UUID productId,
            @RequestParam @NotBlank @Size(max = 2000) String text) {

        UUID tenantId = products.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId))
                .getTenantId();

        return projects.findByTenantIdAndProductId(tenantId, productId).stream()
                .filter(project -> project.getType() == FmeaType.PROCESS_FMEA)
                .filter(project -> project.getStatus() == FmeaStatus.ACTIVE)
                .findFirst()
                .map(project -> FailureModeMatcher.suggest(text, candidatesOf(project), MAX_SUGGESTIONS))
                .orElseGet(List::of);
    }

    private List<FailureModeMatcher.Candidate> candidatesOf(FmeaProject project) {
        return items.findByProjectIdOrderBySequenceNoAsc(project.getId()).stream()
                .map(FailureModeSuggestionController::toCandidate)
                .toList();
    }

    private static FailureModeMatcher.Candidate toCandidate(FmeaItem item) {
        return new FailureModeMatcher.Candidate(
                item.getId(),
                item.getFailureMode() == null ? "" : item.getFailureMode(),
                item.getFailureEffect() == null ? "" : item.getFailureEffect());
    }
}
