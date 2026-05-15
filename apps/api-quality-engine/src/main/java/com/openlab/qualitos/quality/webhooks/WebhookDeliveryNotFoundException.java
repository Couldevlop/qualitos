package com.openlab.qualitos.quality.webhooks;

import java.util.UUID;

public class WebhookDeliveryNotFoundException extends RuntimeException {
    public WebhookDeliveryNotFoundException(UUID id) {
        super("Webhook delivery not found: " + id);
    }
}
