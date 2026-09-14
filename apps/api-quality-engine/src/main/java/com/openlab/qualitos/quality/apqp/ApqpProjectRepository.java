package com.openlab.qualitos.quality.apqp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApqpProjectRepository extends JpaRepository<ApqpProject, UUID> {

    /**
     * Les projets d'un client, le dernier ouvert en tête.
     *
     * <p>Le plus récent d'abord parce que c'est celui sur lequel on travaille :
     * un tri alphabétique enterrerait le programme en cours sous ceux d'il y a
     * trois ans.
     */
    List<ApqpProject> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    /** Un projet, à condition qu'il appartienne au client du jeton. */
    Optional<ApqpProject> findByIdAndTenantId(UUID id, UUID tenantId);
}
