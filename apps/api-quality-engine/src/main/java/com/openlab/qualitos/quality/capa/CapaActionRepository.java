package com.openlab.qualitos.quality.capa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CapaActionRepository extends JpaRepository<CapaAction, UUID> {

    Optional<CapaAction> findByIdAndCapaId(UUID id, UUID capaId);
}
