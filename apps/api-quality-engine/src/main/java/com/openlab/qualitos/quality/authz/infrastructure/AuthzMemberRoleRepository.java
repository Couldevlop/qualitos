package com.openlab.qualitos.quality.authz.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** Les rôles attribués aux membres d'un client. Toutes les requêtes portent le client. */
public interface AuthzMemberRoleRepository
        extends JpaRepository<AuthzMemberRoleJpaEntity, AuthzMemberRoleJpaEntity.Key> {

    List<AuthzMemberRoleJpaEntity> findByTenantId(UUID tenantId);

    List<AuthzMemberRoleJpaEntity> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    @Modifying
    @Query("delete from AuthzMemberRoleJpaEntity m where m.tenantId = :tenantId and m.userId = :userId")
    void deleteMember(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

    @Modifying
    @Query("delete from AuthzMemberRoleJpaEntity m where m.tenantId = :tenantId and m.roleCode = :code")
    void deleteRole(@Param("tenantId") UUID tenantId, @Param("code") String code);
}
