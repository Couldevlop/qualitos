package com.openlab.qualitos.core.tenant;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Représente un module activé pour un tenant donné.
 *
 * <p>Cette entité est filtrée par tenant_id via le filtre Hibernate {@code tenantFilter}.
 * Le filtre est activé programmatiquement dans chaque session via
 * {@link com.openlab.qualitos.core.config.TenantHibernateFilterInterceptor}.
 *
 * <p>La table est également protégée par une RLS policy PostgreSQL
 * qui utilise {@code current_setting('app.tenant_id')} pour une défense en profondeur.
 */
@Entity
@Table(
    name = "tenant_modules",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_tenant_modules_tenant_module",
            columnNames = {"tenant_id", "module_name"}
        )
    }
)
@EntityListeners(AuditingEntityListener.class)
@FilterDef(
    name = "tenantFilter",
    parameters = @ParamDef(name = "tenantId", type = String.class)
)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantModule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @NotBlank
    @Size(max = 100)
    @Column(name = "module_name", nullable = false, length = 100)
    private String moduleName;

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
