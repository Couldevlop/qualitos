package com.openlab.qualitos.quality.standards;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StandardRepository extends JpaRepository<Standard, UUID> {

    Page<Standard> findByStatus(StandardStatus status, Pageable pageable);

    Page<Standard> findByFamily(String family, Pageable pageable);

    Optional<Standard> findByCode(String code);
}
