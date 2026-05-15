package com.openlab.qualitos.quality.industry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IndustryPackRepository extends JpaRepository<IndustryPack, UUID> {

    Optional<IndustryPack> findByCode(String code);

    Page<IndustryPack> findAll(Pageable pageable);
}
