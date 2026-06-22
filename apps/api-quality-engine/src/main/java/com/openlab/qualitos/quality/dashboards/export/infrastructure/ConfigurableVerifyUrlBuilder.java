package com.openlab.qualitos.quality.dashboards.export.infrastructure;

import com.openlab.qualitos.quality.dashboards.export.application.VerifyUrlBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/**
 * Builds the absolute public verification URL embedded in the export QR code.
 * Base URL is configured per environment ({@code qualitos.export.public-base-url},
 * default {@code https://app.qualitos.io}). The code is path-encoded (defence in
 * depth — codes are URL-safe by construction).
 */
@Component
public class ConfigurableVerifyUrlBuilder implements VerifyUrlBuilder {

    private final String baseUrl;

    public ConfigurableVerifyUrlBuilder(
            @Value("${qualitos.export.public-base-url:https://app.qualitos.io}") String baseUrl) {
        this.baseUrl = stripTrailingSlash(baseUrl);
    }

    @Override
    public String verifyUrl(String verificationCode) {
        String code = UriUtils.encodePathSegment(verificationCode, StandardCharsets.UTF_8);
        return baseUrl + "/api/v1/dashboards/public/exports/" + code + "/verify";
    }

    private static String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
