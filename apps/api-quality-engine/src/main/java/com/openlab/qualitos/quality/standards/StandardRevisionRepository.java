package com.openlab.qualitos.quality.standards;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StandardRevisionRepository
        extends JpaRepository<StandardRevision, UUID> {

    List<StandardRevision> findByStandardIdOrderByOrderIndexAsc(UUID standardId);
}
