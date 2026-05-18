package com.openlab.qualitos.quality.dashboards.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Aggregate — a user-built dashboard layout (drag &amp; drop builder).
 * CLAUDE.md §7.1 / §7.3.
 *
 * Invariants:
 *   - tenant_id from JWT (never body)
 *   - name unique per (tenant, user)
 *   - layout_json must be non-blank
 *   - signature_hash optional — populated after ML-DSA sign + Fabric anchor
 */
public final class DashboardLayout {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\w\\s.\\-_/()\\[\\]]{2,120}$");

    private UUID id;
    private final UUID tenantId;
    private final UUID userId;
    private String name;
    private String description;
    private String layoutJson;
    private boolean shared;
    private String signatureHash;
    private int version;
    private final Instant createdAt;
    private Instant updatedAt;

    public DashboardLayout(UUID id, UUID tenantId, UUID userId,
                           String name, String description,
                           String layoutJson, boolean shared,
                           String signatureHash, int version,
                           Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.name = requireName(name);
        this.description = description;
        this.layoutJson = requireLayout(layoutJson);
        this.shared = shared;
        this.signatureHash = signatureHash;
        this.version = Math.max(version, 1);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
    }

    public static DashboardLayout create(UUID tenantId, UUID userId,
                                         String name, String description,
                                         String layoutJson, boolean shared,
                                         Instant now) {
        return new DashboardLayout(null, tenantId, userId, name, description,
                layoutJson, shared, null, 1, now, now);
    }

    public void update(String name, String description, String layoutJson,
                       boolean shared, Instant now) {
        this.name = requireName(name);
        this.description = description;
        this.layoutJson = requireLayout(layoutJson);
        this.shared = shared;
        this.signatureHash = null; // re-sign after change
        this.version = this.version + 1;
        this.updatedAt = now;
    }

    public void attachSignature(String signatureHash) {
        if (signatureHash == null || signatureHash.length() < 16) {
            throw new IllegalArgumentException("signatureHash too short");
        }
        this.signatureHash = signatureHash;
    }

    public void assignId(UUID id) { this.id = id; }

    private static String requireName(String name) {
        if (name == null || !NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    "name must be 2..120 chars (letters/digits/spaces/.-_/()[]).");
        }
        return name;
    }

    private static String requireLayout(String layoutJson) {
        if (layoutJson == null || layoutJson.isBlank()) {
            throw new IllegalArgumentException("layoutJson required");
        }
        if (!layoutJson.trim().startsWith("{")) {
            throw new IllegalArgumentException("layoutJson must be a JSON object");
        }
        return layoutJson;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getLayoutJson() { return layoutJson; }
    public boolean isShared() { return shared; }
    public String getSignatureHash() { return signatureHash; }
    public int getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
