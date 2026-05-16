package com.openlab.qualitos.quality.ratelimit.domain;

public class RateLimitPolicyException extends RuntimeException {
    public RateLimitPolicyException(String msg) { super(msg); }
}
