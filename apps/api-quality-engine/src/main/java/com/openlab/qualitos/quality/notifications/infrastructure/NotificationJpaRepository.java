package com.openlab.qualitos.quality.notifications.infrastructure;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, UUID> {

    Optional<NotificationJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    /** Visibles par l'utilisateur (destinataire explicite OU diffusion tenant), récentes d'abord. */
    @Query("select n from NotificationJpaEntity n where n.tenantId = :tenantId "
            + "and (n.recipientUserId is null or n.recipientUserId = :userId) "
            + "order by n.createdAt desc")
    List<NotificationJpaEntity> findVisible(@Param("tenantId") UUID tenantId,
                                            @Param("userId") String userId,
                                            Pageable pageable);

    @Query("select count(n) from NotificationJpaEntity n where n.tenantId = :tenantId "
            + "and (n.recipientUserId is null or n.recipientUserId = :userId) "
            + "and n.readAt is null")
    long countUnread(@Param("tenantId") UUID tenantId, @Param("userId") String userId);

    @Modifying
    @Query("update NotificationJpaEntity n set n.readAt = :at where n.tenantId = :tenantId "
            + "and (n.recipientUserId is null or n.recipientUserId = :userId) and n.readAt is null")
    int markAllRead(@Param("tenantId") UUID tenantId, @Param("userId") String userId,
                    @Param("at") Instant at);
}
