package com.openlab.qualitos.iot.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "iot_devices")
public class DeviceEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Column(name = "code", nullable = false, length = 100)
  private String code;

  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 32)
  private com.openlab.qualitos.iot.domain.model.DeviceType type;

  @Enumerated(EnumType.STRING)
  @Column(name = "protocol", nullable = false, length = 32)
  private com.openlab.qualitos.iot.domain.model.Protocol protocol;

  @Column(name = "enterprise", length = 100) private String enterprise;
  @Column(name = "site", length = 100)       private String site;
  @Column(name = "area", length = 100)       private String area;
  @Column(name = "work_center", length = 100) private String workCenter;
  @Column(name = "equipment", length = 100)  private String equipment;

  @Column(name = "cert_fingerprint_sha256", length = 64) private String certFingerprintSha256;

  /** Stored as JSON text — we keep it intentionally simple at the JPA layer. */
  @Column(name = "twin_json", columnDefinition = "TEXT") private String twinJson;

  @Column(name = "provisioned_at", nullable = false) private Instant provisionedAt;
  @Column(name = "last_seen_at") private Instant lastSeenAt;

  protected DeviceEntity() {}

  // ----- getters / setters --------------------------------------------------
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getTenantId() { return tenantId; }
  public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public com.openlab.qualitos.iot.domain.model.DeviceType getType() { return type; }
  public void setType(com.openlab.qualitos.iot.domain.model.DeviceType type) { this.type = type; }
  public com.openlab.qualitos.iot.domain.model.Protocol getProtocol() { return protocol; }
  public void setProtocol(com.openlab.qualitos.iot.domain.model.Protocol protocol) { this.protocol = protocol; }
  public String getEnterprise() { return enterprise; }
  public void setEnterprise(String enterprise) { this.enterprise = enterprise; }
  public String getSite() { return site; }
  public void setSite(String site) { this.site = site; }
  public String getArea() { return area; }
  public void setArea(String area) { this.area = area; }
  public String getWorkCenter() { return workCenter; }
  public void setWorkCenter(String workCenter) { this.workCenter = workCenter; }
  public String getEquipment() { return equipment; }
  public void setEquipment(String equipment) { this.equipment = equipment; }
  public String getCertFingerprintSha256() { return certFingerprintSha256; }
  public void setCertFingerprintSha256(String certFingerprintSha256) { this.certFingerprintSha256 = certFingerprintSha256; }
  public String getTwinJson() { return twinJson; }
  public void setTwinJson(String twinJson) { this.twinJson = twinJson; }
  public Instant getProvisionedAt() { return provisionedAt; }
  public void setProvisionedAt(Instant provisionedAt) { this.provisionedAt = provisionedAt; }
  public Instant getLastSeenAt() { return lastSeenAt; }
  public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}
