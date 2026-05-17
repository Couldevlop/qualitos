package com.openlab.qualitos.core.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Utilisateur applicatif QualitOS.
 *
 * <p>Chaque {@code AppUser} correspond à un utilisateur Keycloak (clé {@code keycloakId}).
 * Le {@code tenantId} est toujours extrait du JWT — jamais du body de la requête.
 *
 * <p>Le filtre Hibernate {@code tenantFilter} garantit qu'aucune requête sur cette
 * entité ne peut retourner des lignes d'un tenant différent du tenant courant.
 * La RLS PostgreSQL constitue une deuxième couche de défense.
 */
@Entity
@Table(
    name = "app_users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_app_users_keycloak_id", columnNames = "keycloak_id"),
        @UniqueConstraint(name = "uq_app_users_tenant_email", columnNames = {"tenant_id", "email"})
    }
)
@EntityListeners(AuditingEntityListener.class)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @NotBlank
    @Column(name = "keycloak_id", nullable = false, length = 36)
    private String keycloakId;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(name = "email", nullable = false)
    private String email;

    /**
     * Rôles métier QualitOS du tenant : SUPER_ADMIN, ADMIN, QUALITY_MANAGER,
     * QUALITY_DIRECTOR, AUDITOR, USER, EXTERNAL_AUDITOR.
     * Stockés en JSON dans PostgreSQL pour éviter une table de jointure.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "app_user_roles",
        joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "role", nullable = false, length = 50)
    @Builder.Default
    private Set<String> roles = new HashSet<>();

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
