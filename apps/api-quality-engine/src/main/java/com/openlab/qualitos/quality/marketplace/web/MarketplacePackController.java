package com.openlab.qualitos.quality.marketplace.web;

import com.openlab.qualitos.quality.marketplace.application.MarketplacePackDto;
import com.openlab.qualitos.quality.marketplace.application.MarketplacePackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Marketplace endpoint — CLAUDE.md §8.11.
 * Listing is open to authenticated users; register/verify is SUPER_ADMIN-only
 * (enforced by KeycloakSuperAdminProvider).
 */
@RestController
@RequestMapping("/api/v1/marketplace/packs")
@Validated
public class MarketplacePackController {

    private final MarketplacePackService service;

    public MarketplacePackController(MarketplacePackService service) {
        this.service = service;
    }

    @GetMapping
    public List<MarketplacePackDto.View> list(@RequestParam(required = false) String sector) {
        return service.listVerified(sector);
    }

    @GetMapping("/{id}")
    public MarketplacePackDto.View get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MarketplacePackDto.View register(@Valid @RequestBody MarketplacePackWebDto.RegisterRequest req) {
        return service.register(new MarketplacePackDto.RegisterRequest(
                req.packId(), req.version(), req.publisher(), req.title(),
                req.description(), req.sector(), req.priceCents(), req.currency(),
                req.manifestUrl(), req.signatureHash()));
    }

    @PostMapping("/{id}/verify")
    public MarketplacePackDto.View verify(@PathVariable UUID id) {
        return service.verify(id);
    }
}
