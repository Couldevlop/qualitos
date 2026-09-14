package com.openlab.qualitos.quality.nonconformity.eightd.infrastructure;

import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

/**
 * Ligne de persistance du rapport 8D. Aucune logique : l'agrégat du domaine porte
 * les règles, cette classe porte les colonnes.
 */
@Entity
@Table(name = "nc_eightd_reports")
public class EightDReportJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "nc_id", nullable = false, updatable = false)
    private UUID ncId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EightDStatus status;

    /** D1 — l'équipe qui a traité l'écart. */
    @Column(name = "team", length = 4000)
    private String team;

    /** D3 — la sécurisation immédiate. */
    @Column(name = "containment", length = 4000)
    private String containment;

    /** D8 — la reconnaissance de l'équipe. */
    @Column(name = "recognition", length = 4000)
    private String recognition;

    /**
     * L'instantané figé des huit disciplines (JSON).
     *
     * <p>{@code TEXT} + {@code LONGVARCHAR} et non {@code jsonb} : le contenu n'est
     * jamais interrogé par le SGBD, seulement relu en entier, et sans le type JDBC
     * explicite PostgreSQL refuse l'insertion — {@code null} compris.
     */
    @Column(name = "snapshot_json", columnDefinition = "TEXT")
    @JdbcTypeCode(Types.LONGVARCHAR)
    private String snapshotJson;

    @Column(name = "sha256_hex", length = 64)
    private String sha256Hex;

    @Column(name = "signature", columnDefinition = "TEXT")
    @JdbcTypeCode(Types.LONGVARCHAR)
    private String signature;

    @Column(name = "anchor_tx_ref", length = 200)
    private String anchorTxRef;

    @Column(name = "verification_code", length = 64)
    private String verificationCode;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "issued_by")
    private UUID issuedBy;

    @Column(name = "issued_by_name", length = 255)
    private String issuedByName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getNcId() { return ncId; }
    public void setNcId(UUID ncId) { this.ncId = ncId; }

    public EightDStatus getStatus() { return status; }
    public void setStatus(EightDStatus status) { this.status = status; }

    public String getTeam() { return team; }
    public void setTeam(String team) { this.team = team; }

    public String getContainment() { return containment; }
    public void setContainment(String containment) { this.containment = containment; }

    public String getRecognition() { return recognition; }
    public void setRecognition(String recognition) { this.recognition = recognition; }

    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }

    public String getSha256Hex() { return sha256Hex; }
    public void setSha256Hex(String sha256Hex) { this.sha256Hex = sha256Hex; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getAnchorTxRef() { return anchorTxRef; }
    public void setAnchorTxRef(String anchorTxRef) { this.anchorTxRef = anchorTxRef; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }

    public UUID getIssuedBy() { return issuedBy; }
    public void setIssuedBy(UUID issuedBy) { this.issuedBy = issuedBy; }

    public String getIssuedByName() { return issuedByName; }
    public void setIssuedByName(String issuedByName) { this.issuedByName = issuedByName; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
