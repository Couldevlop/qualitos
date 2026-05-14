package com.openlab.qualitos.quality.ishikawa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IshikawaCauseRepository extends JpaRepository<IshikawaCause, UUID> {

    Optional<IshikawaCause> findByIdAndDiagramId(UUID id, UUID diagramId);
}
