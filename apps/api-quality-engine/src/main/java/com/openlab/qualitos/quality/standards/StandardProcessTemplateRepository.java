package com.openlab.qualitos.quality.standards;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StandardProcessTemplateRepository
        extends JpaRepository<StandardProcessTemplate, UUID> {

    List<StandardProcessTemplate> findByStandardIdOrderByOrderIndexAsc(UUID standardId);
}
