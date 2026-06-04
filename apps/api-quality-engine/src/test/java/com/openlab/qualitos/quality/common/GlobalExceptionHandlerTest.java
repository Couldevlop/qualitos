package com.openlab.qualitos.quality.common;

import com.openlab.qualitos.quality.notifications.domain.NotificationNotFoundException;
import com.openlab.qualitos.quality.standards.RoadmapStageNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void noResourceFound_mapsTo404_withoutLeakingDetails() {
        ProblemDetail pd = handler.handleNoResource(
                new NoResourceFoundException(HttpMethod.GET, "/api/v1/does-not-exist"));
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getDetail()).isEqualTo("Resource not found");
        // ne divulgue pas le chemin demandé
        assertThat(pd.getDetail()).doesNotContain("does-not-exist");
    }

    @Test
    void roadmapStageNotFound_mapsTo404() {
        ProblemDetail pd = handler.handleRoadmapStageNotFound(
                new RoadmapStageNotFoundException(UUID.randomUUID()));
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getTitle()).contains("Roadmap Stage");
    }

    @Test
    void missingTenant_mapsTo403() {
        ProblemDetail pd = handler.handleMissingTenant(new MissingTenantContextException());
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void missingRequiredParam_mapsTo400() {
        ProblemDetail pd = handler.handleMissingParam(
                new MissingServletRequestParameterException("category", "Nis2MeasureCategory"));
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(pd.getDetail()).contains("category");
        assertThat(pd.getTitle()).isEqualTo("Missing Required Parameter");
    }

    @Test
    void notificationNotFound_mapsTo404() {
        ProblemDetail pd = handler.handleNotificationNotFound(
                new NotificationNotFoundException(UUID.randomUUID()));
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getTitle()).isEqualTo("Notification Not Found");
    }
}
