package com.openlab.qualitos.quality.standards;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StandardDocumentTemplateRepository
        extends JpaRepository<StandardDocumentTemplate, UUID> {

    List<StandardDocumentTemplate> findByStandardIdOrderByOrderIndexAscNameAsc(UUID standardId);

    Optional<StandardDocumentTemplate> findByIdAndStandardId(UUID id, UUID standardId);
}
