package com.openlab.qualitos.iot.infrastructure.external;

import com.openlab.qualitos.iot.domain.model.Threshold;
import com.openlab.qualitos.iot.domain.model.ThresholdBreachEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class QualityEngineNcPublisherTest {

    private ApplicationEventPublisher eventBus;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        eventBus = mock(ApplicationEventPublisher.class);
        restTemplate = mock(RestTemplate.class);
    }

    private static ThresholdBreachEvent sampleEvent() {
        return new ThresholdBreachEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "temperature",
                42.5,
                new Threshold("temperature", 0.0, 40.0, Threshold.Severity.WARNING),
                Instant.parse("2026-05-19T10:00:00Z"));
    }

    @Test
    void notifyBreach_allowedHost_publishesEventAndPosts() {
        QualityEngineNcPublisher publisher = new QualityEngineNcPublisher(
                eventBus, restTemplate,
                "http://api-quality-engine:8082",
                "api-quality-engine,localhost");
        ThresholdBreachEvent event = sampleEvent();

        publisher.notifyBreach(event);

        verify(eventBus, times(1)).publishEvent(event);
        verify(restTemplate, times(1)).postForLocation(any(URI.class), any(HttpEntity.class));
    }

    @Test
    void notifyBreach_hostNotInAllowList_dropsPostButStillPublishesEvent() {
        QualityEngineNcPublisher publisher = new QualityEngineNcPublisher(
                eventBus, restTemplate,
                "http://malicious-host:8082",
                "api-quality-engine,localhost");
        ThresholdBreachEvent event = sampleEvent();

        publisher.notifyBreach(event);

        verify(eventBus, times(1)).publishEvent(event);
        verify(restTemplate, never()).postForLocation(any(URI.class), any(HttpEntity.class));
    }

    @Test
    void notifyBreach_restClientException_isCaughtAndLogged() {
        QualityEngineNcPublisher publisher = new QualityEngineNcPublisher(
                eventBus, restTemplate,
                "http://api-quality-engine:8082",
                "api-quality-engine");
        doThrow(new RestClientException("connection refused"))
                .when(restTemplate).postForLocation(any(URI.class), any(HttpEntity.class));
        ThresholdBreachEvent event = sampleEvent();

        // Must not propagate the exception
        publisher.notifyBreach(event);

        verify(eventBus, times(1)).publishEvent(event);
        verify(restTemplate, times(1)).postForLocation(any(URI.class), any(HttpEntity.class));
    }

    @Test
    void notifyBreach_localhostAllowed() {
        QualityEngineNcPublisher publisher = new QualityEngineNcPublisher(
                eventBus, restTemplate,
                "http://localhost:8082",
                "api-quality-engine,localhost");
        publisher.notifyBreach(sampleEvent());
        verify(restTemplate, times(1)).postForLocation(any(URI.class), any(HttpEntity.class));
    }
}
