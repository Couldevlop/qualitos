package com.openlab.qualitos.quality.revisionrequests.web;

import com.openlab.qualitos.quality.revisionrequests.application.RevisionRequestDto;
import com.openlab.qualitos.quality.revisionrequests.application.RevisionRequestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints — propositions de révision.
 *
 * <p>Accepter ou refuser est une décision qualité : réservé aux rôles qui pilotent
 * le système. Le refus exige une note, contrôlée ici pour répondre 400 plutôt que
 * de laisser le domaine lever une exception traduite en 500.
 */
@RestController
@RequestMapping("/api/v1")
@Validated
@PreAuthorize("isAuthenticated()")
public class RevisionRequestController {

    private static final String DECIDE_ROLES =
            "hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','ADMIN_TENANT','SUPER_ADMIN')";

    private final RevisionRequestService service;

    public RevisionRequestController(RevisionRequestService service) {
        this.service = service;
    }

    @GetMapping("/products/{productId}/revision-requests")
    public List<RevisionRequestDto.View> pendingForProduct(@PathVariable UUID productId) {
        return service.pendingForProduct(productId);
    }

    @GetMapping("/revision-requests")
    public List<RevisionRequestDto.View> forTrigger(@RequestParam UUID triggerRefId) {
        return service.forTrigger(triggerRefId);
    }

    @PostMapping("/revision-requests/{id}/accept")
    @PreAuthorize(DECIDE_ROLES)
    public RevisionRequestDto.View accept(@PathVariable UUID id) {
        return service.accept(id);
    }

    @PostMapping("/revision-requests/{id}/reject")
    @PreAuthorize(DECIDE_ROLES)
    public RevisionRequestDto.View reject(@PathVariable UUID id,
                                          @Valid @RequestBody RejectRequest req) {
        return service.reject(id, req.note());
    }

    /** La note de refus : « on n'a pas bougé » sans raison écrite est l'écart type. */
    public record RejectRequest(@NotBlank @Size(max = 1000) String note) {}
}
