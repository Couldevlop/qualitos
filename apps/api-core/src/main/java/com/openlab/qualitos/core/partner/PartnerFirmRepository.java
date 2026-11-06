package com.openlab.qualitos.core.partner;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Les cabinets. Table de plateforme : pas de cloisonnement par client. */
public interface PartnerFirmRepository extends JpaRepository<PartnerFirm, UUID> {

    Optional<PartnerFirm> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<PartnerFirm> findAllByOrderByNameAsc(Pageable pageable);
}
