package com.openlab.qualitos.quality.webhooks;

import java.util.UUID;

public class WebhookSubscriptionNotFoundException extends RuntimeException {
    public WebhookSubscriptionNotFoundException(UUID id) {
        super("Webhook subscription not found: " + id);
    }
}
