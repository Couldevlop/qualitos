package com.openlab.qualitos.quality.ratelimit.domain;

import java.util.UUID;

public class RateLimitPolicyNotFoundException extends RuntimeException {
    public RateLimitPolicyNotFoundException(UUID id) {
        super("Rate limit policy not found: " + id);
    }
}
