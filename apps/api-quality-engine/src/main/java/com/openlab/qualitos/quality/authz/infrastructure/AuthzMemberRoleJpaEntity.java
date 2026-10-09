package com.openlab.qualitos.quality.authz.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "authz_member_roles")
@IdClass(AuthzMemberRoleJpaEntity.Key.class)
@Getter
@Setter
@NoArgsConstructor
public class AuthzMemberRoleJpaEntity {

    @Id
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Id
    @Column(name = "role_code", nullable = false, updatable = false, length = 64)
    private String roleCode;

    @Column(name = "assigned_by", nullable = false, updatable = false)
    private UUID assignedBy;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    /** La clé composée : un rôle donné une fois à un membre d'un client. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private UUID tenantId;
        private UUID userId;
        private String roleCode;
    }
}
