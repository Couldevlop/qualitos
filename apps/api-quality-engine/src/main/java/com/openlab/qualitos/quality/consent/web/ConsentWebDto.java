package com.openlab.qualitos.quality.consent.web;

import com.openlab.qualitos.quality.consent.domain.ConsentSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class ConsentWebDto {

    private ConsentWebDto() {}

    public record GrantRequest(
            @NotBlank @Size(max = 320) String subjectIdentifier,
            @Size(max = 250) String subjectIdentifierLabel,
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[a-z][a-z0-9._-]{1,63}$",
                    message = "purposeCode must match [a-z][a-z0-9._-]{1,63}")
            String purposeCode,
            @NotBlank @Size(max = 32)
            @Pattern(regexp = "^[A-Za-z0-9._:-]{1,32}$",
                    message = "purposeVersion must be 1..32 chars in [A-Za-z0-9._:-]")
            String purposeVersion,
            @NotNull ConsentSource source,
            @Size(max = 1024) String evidenceUrl,
            @Size(max = 64) String ipAddress,
            @Size(max = 500) String userAgent,
            @NotNull UUID grantedByUserId,
            Instant expiresAt) {}

    public record WithdrawRequest(
            @NotNull UUID actorUserId,
            @Size(max = 2000) String reason) {}
}
