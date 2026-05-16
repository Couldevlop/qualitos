package com.openlab.qualitos.quality.privacynotices.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public final class PrivacyNoticeWebDto {

    private PrivacyNoticeWebDto() {}

    public record CreateRequest(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$",
                    message = "reference must match [A-Z][A-Z0-9_-]{1,63}")
            String reference,
            @NotBlank @Size(max = 32)
            @Pattern(regexp = "^[A-Za-z0-9._:-]{1,32}$") String version,
            @NotBlank @Size(min = 2, max = 2)
            @Pattern(regexp = "^[a-z]{2}$") String language,
            @NotBlank @Size(max = 250) String title,
            @Size(max = 2000) String summary,
            @Size(max = 65000) String contentMarkdown,
            Set<UUID> linkedProcessingActivityIds,
            @Size(max = 1024) String publishUrl,
            @Size(max = 250) String contactName,
            @Size(max = 250) String contactEmail,
            @NotNull UUID createdByUserId) {}

    public record EditRequest(
            @NotBlank @Size(max = 250) String title,
            @Size(max = 2000) String summary,
            @Size(max = 65000) String contentMarkdown,
            Set<UUID> linkedProcessingActivityIds,
            @Size(max = 1024) String publishUrl,
            @Size(max = 250) String contactName,
            @Size(max = 250) String contactEmail) {}

    public record PublishRequest(@NotNull UUID publishedByUserId) {}
}
