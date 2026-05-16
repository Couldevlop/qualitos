package com.openlab.qualitos.quality.apikeys.web;

import com.openlab.qualitos.quality.apikeys.application.ApiKeyDto;
import com.openlab.qualitos.quality.apikeys.application.ApiKeyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/api-keys")
@Validated
public class ApiKeyController {

    private final ApiKeyService service;

    public ApiKeyController(ApiKeyService service) { this.service = service; }

    @GetMapping
    public List<ApiKeyDto.View> list() { return service.list(); }

    @GetMapping("/{id}")
    public ApiKeyDto.View get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiKeyDto.IssuedKey create(@Valid @RequestBody ApiKeyWebDto.CreateRequest req) {
        return service.create(new ApiKeyDto.CreateRequest(
                req.name(), req.scopes(), req.expiresAt(), req.actor()));
    }

    @PostMapping("/{id}/rotate")
    public ApiKeyDto.IssuedKey rotate(@PathVariable UUID id,
                                      @Valid @RequestBody ApiKeyWebDto.RotateRequest req) {
        return service.rotate(id, new ApiKeyDto.RotateRequest(req.actor()));
    }

    @PostMapping("/{id}/revoke")
    public ApiKeyDto.View revoke(@PathVariable UUID id,
                                 @Valid @RequestBody ApiKeyWebDto.RevokeRequest req) {
        return service.revoke(id, new ApiKeyDto.RevokeRequest(req.actor()));
    }

    @PostMapping("/expire-due")
    public Map<String, Integer> expireDue(
            @RequestParam(defaultValue = "200") @Min(1) @Max(500) int limit) {
        return Map.of("expired", service.expireDue(limit));
    }
}
