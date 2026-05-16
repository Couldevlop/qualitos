package com.openlab.qualitos.quality.apikeys.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class ApiKeyWebDto {

    private ApiKeyWebDto() {}

    public record CreateRequest(
            @NotBlank @Size(max = 120) String name,
            Set<String> scopes,
            Instant expiresAt,
            @NotNull UUID actor) {}

    public record RotateRequest(@NotNull UUID actor) {}
    public record RevokeRequest(@NotNull UUID actor) {}
}
