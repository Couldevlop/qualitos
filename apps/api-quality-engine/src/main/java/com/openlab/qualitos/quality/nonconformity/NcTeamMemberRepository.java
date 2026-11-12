package com.openlab.qualitos.quality.nonconformity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * L'équipe qui prend un écart en charge (D1 du 8D).
 *
 * <p>La lecture par lot existe pour la liste paginée : une requête par ligne
 * ferait une dizaine d'allers-retours pour afficher une page de dix écarts.
 */
public interface NcTeamMemberRepository extends JpaRepository<NcTeamMember, UUID> {

    List<NcTeamMember> findByTenantIdAndNcIdOrderByPositionAsc(UUID tenantId, UUID ncId);

    List<NcTeamMember> findByTenantIdAndNcIdInOrderByPositionAsc(UUID tenantId, Collection<UUID> ncIds);

    void deleteByTenantIdAndNcId(UUID tenantId, UUID ncId);
}
