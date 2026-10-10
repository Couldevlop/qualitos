package com.openlab.qualitos.quality.authz.infrastructure;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "authz_roles")
@Getter
@Setter
@NoArgsConstructor
public class AuthzRoleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, updatable = false, length = 64)
    private String code;

    @Column(length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "system_role", nullable = false, updatable = false)
    private boolean systemRole;

    /** Les codes d'actions ({@code capa.close}) ; un rôle en porte au plus une trentaine. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "authz_role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission", nullable = false, length = 80)
    private Set<String> permissions = new HashSet<>();

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
