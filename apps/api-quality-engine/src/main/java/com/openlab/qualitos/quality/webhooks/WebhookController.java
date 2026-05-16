package com.openlab.qualitos.quality.webhooks;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final WebhookService service;

    public WebhookController(WebhookService service) { this.service = service; }

    // ---- Subscriptions ----

    @GetMapping("/subscriptions")
    public Page<WebhookDto.SubscriptionResponse> list(
            @PageableDefault(size = 20) Pageable pageable) {
        return service.listSubscriptions(pageable);
    }

    @PostMapping("/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    public WebhookDto.CreatedSubscriptionResponse create(
            @Valid @RequestBody WebhookDto.CreateSubscriptionRequest req) {
        return service.createSubscription(req);
    }

    @GetMapping("/subscriptions/{id}")
    public WebhookDto.SubscriptionResponse get(@PathVariable UUID id) {
        return service.getSubscription(id);
    }

    @PatchMapping("/subscriptions/{id}")
    public WebhookDto.SubscriptionResponse update(@PathVariable UUID id,
                                                 @Valid @RequestBody WebhookDto.UpdateSubscriptionRequest req) {
        return service.updateSubscription(id, req);
    }

    @DeleteMapping("/subscriptions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.deleteSubscription(id); }

    @PostMapping("/subscriptions/{id}/test")
    public WebhookDto.TestPingResponse testPing(@PathVariable UUID id) {
        return service.testPing(id);
    }

    // ---- Deliveries ----

    @GetMapping("/deliveries")
    public Page<WebhookDto.DeliveryResponse> deliveries(
            @RequestParam(required = false) UUID subscriptionId,
            @RequestParam(required = false) DeliveryStatus status,
            @PageableDefault(size = 50) Pageable pageable) {
        return service.listDeliveries(subscriptionId, status, pageable);
    }

    @GetMapping("/deliveries/{id}")
    public WebhookDto.DeliveryResponse getDelivery(@PathVariable UUID id) {
        return service.getDelivery(id);
    }

    @PostMapping("/deliveries/{id}/retry")
    public WebhookDto.DeliveryResponse retry(@PathVariable UUID id) {
        return service.retryDelivery(id);
    }
}
