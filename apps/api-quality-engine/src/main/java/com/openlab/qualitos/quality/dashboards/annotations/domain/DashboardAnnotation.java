package com.openlab.qualitos.quality.dashboards.annotations.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Aggregate — a collaborative annotation (comment) posted on a dashboard chart
 * (CLAUDE.md §7.3 "Annotations collaboratives").
 *
 * <p>Invariants (OWASP A01 / §18.2 #2) :</p>
 * <ul>
 *   <li>{@code tenantId} and {@code authorId} always come from the validated JWT,
 *       never from the request body.</li>
 *   <li>{@code chartKey} identifies the chart within a dashboard (e.g.
 *       {@code "exec.trend"}); it is a stable, opaque key — never free HTML.</li>
 *   <li>{@code body} is plain text, 1..2000 chars, stored as-is and escaped at
 *       render time by Angular (no innerHTML).</li>
 *   <li>An annotation is immutable except for deletion (audit-friendly).</li>
 * </ul>
 */
public final class DashboardAnnotation {

    /** Chart key: dotted lowercase segments, e.g. {@code exec.trend}. */
    private static final Pattern CHART_KEY_PATTERN =
            Pattern.compile("^[a-z0-9]+(?:[._-][a-z0-9]+){0,5}$");

    public static final int BODY_MAX = 2000;
    public static final int LABEL_MAX = 120;

    private UUID id;
    private final UUID tenantId;
    private final UUID authorId;
    private final String chartKey;
    /** Optional category label of the point the comment is anchored to (nullable). */
    private final String anchorLabel;
    private final String body;
    private final Instant createdAt;

    public DashboardAnnotation(UUID id, UUID tenantId, UUID authorId,
                               String chartKey, String anchorLabel,
                               String body, Instant createdAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.authorId = Objects.requireNonNull(authorId, "authorId");
        this.chartKey = requireChartKey(chartKey);
        this.anchorLabel = normalizeAnchor(anchorLabel);
        this.body = requireBody(body);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static DashboardAnnotation create(UUID tenantId, UUID authorId,
                                             String chartKey, String anchorLabel,
                                             String body, Instant now) {
        return new DashboardAnnotation(null, tenantId, authorId, chartKey,
                anchorLabel, body, now);
    }

    public void assignId(UUID id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    /** @return true if {@code userId} is the author and may therefore delete it. */
    public boolean isAuthoredBy(UUID userId) {
        return authorId.equals(userId);
    }

    private static String requireChartKey(String chartKey) {
        if (chartKey == null || !CHART_KEY_PATTERN.matcher(chartKey).matches()) {
            throw new IllegalArgumentException(
                    "chartKey must be dotted lowercase segments (e.g. 'exec.trend')");
        }
        return chartKey;
    }

    private static String requireBody(String body) {
        if (body == null) {
            throw new IllegalArgumentException("body required");
        }
        String trimmed = body.strip();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("body must not be blank");
        }
        if (trimmed.length() > BODY_MAX) {
            throw new IllegalArgumentException("body must be at most " + BODY_MAX + " chars");
        }
        return trimmed;
    }

    private static String normalizeAnchor(String anchorLabel) {
        if (anchorLabel == null) {
            return null;
        }
        String trimmed = anchorLabel.strip();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > LABEL_MAX) {
            throw new IllegalArgumentException("anchorLabel must be at most " + LABEL_MAX + " chars");
        }
        return trimmed;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getAuthorId() { return authorId; }
    public String getChartKey() { return chartKey; }
    public String getAnchorLabel() { return anchorLabel; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
}
