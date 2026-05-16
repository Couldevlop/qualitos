package com.openlab.qualitos.quality.ratelimit.web;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.ratelimit.application.RateLimitDto;
import com.openlab.qualitos.quality.ratelimit.application.RateLimitService;
import com.openlab.qualitos.quality.ratelimit.domain.RateLimitDecision;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rate-limits")
@Validated
public class RateLimitController {

    private final RateLimitService service;

    public RateLimitController(RateLimitService service) { this.service = service; }

    @GetMapping("/policies")
    public List<RateLimitDto.PolicyView> list() { return service.list(); }

    @GetMapping("/policies/{id}")
    public RateLimitDto.PolicyView get(@PathVariable UUID id) { return service.get(id); }

    @PutMapping("/policies")
    @ResponseStatus(HttpStatus.OK)
    public RateLimitDto.PolicyView upsert(@Valid @RequestBody RateLimitWebDto.UpsertRequest req) {
        return service.upsert(new RateLimitDto.UpsertPolicyRequest(
                req.scope(), req.windowSeconds(), req.maxRequests(), req.enabled()));
    }

    @DeleteMapping("/policies/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }

    /** Introspection : état du compteur sans incrément. */
    @GetMapping("/check/{scope}")
    public RateLimitDecision check(@PathVariable String scope) {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return service.peek(UUID.fromString(TenantContext.getTenantId()), scope);
    }
}
