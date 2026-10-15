package com.openlab.qualitos.quality.change;

import jakarta.persistence.*;

import java.util.UUID;

/** Un titulaire d'un rôle de validation : un membre de l'annuaire du client. */
@Entity
@Table(name = "moc_role_holders",
        uniqueConstraints = @UniqueConstraint(name = "uq_moc_role_holders", columnNames = {"role_id", "user_id"}),
        indexes = @Index(name = "ix_moc_role_holders_tenant", columnList = "tenant_id, user_id"))
public class MocRoleHolderRow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_label", nullable = false, length = 200)
    private String userLabel;

    /** Adresse du titulaire, pour le courriel ; nulle pour un titulaire nommé avant la colonne. */
    @Column(name = "user_email", length = 320)
    private String userEmail;

    @Column(nullable = false)
    private int ordinal;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getUserLabel() { return userLabel; }
    public void setUserLabel(String userLabel) { this.userLabel = userLabel; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public int getOrdinal() { return ordinal; }
    public void setOrdinal(int ordinal) { this.ordinal = ordinal; }
}
