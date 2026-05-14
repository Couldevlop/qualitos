package com.openlab.qualitos.core.tenant;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Représente un tenant (organisation cliente) dans QualitOS.
 *
 * <p>La table {@code tenants} est protégée par RLS PostgreSQL.
 * Seul le Super Admin (role interne) peut lire tous les tenants.
 * Les utilisateurs d'un tenant ne voient que leur propre ligne via RLS.
 *
 * <p>Cette entité n'est PAS filtrée par le tenantFilter Hibernate
 * car elle est gérée exclusivement par le Super Admin sans filtre de tenant.
 */
@Entity
@Table(
    name = "tenants",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_tenants_slug", columnNames = "slug")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Size(min = 3, max = 63)
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$",
             message = "Slug must be lowercase alphanumeric with hyphens, 3-63 chars")
    @Column(name = "slug", nullable = false, length = 63)
    private String slug;

    @NotBlank
    @Size(max = 255)
    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 20)
    @Builder.Default
    private Plan plan = Plan.STARTER;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum Plan {
        STARTER,
        PRO,
        ENTERPRISE
    }
}
