package com.openlab.qualitos.quality.risk;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface FmeaItemRepository extends JpaRepository<FmeaItem, UUID> {

    Page<FmeaItem> findByProjectIdOrderBySequenceNoAsc(UUID projectId, Pageable pageable);

    List<FmeaItem> findByProjectIdOrderBySequenceNoAsc(UUID projectId);

    long countByProjectIdAndRpnGreaterThanEqual(UUID projectId, int threshold);

    @Query("select coalesce(max(i.sequenceNo), 0) from FmeaItem i where i.projectId = :projectId")
    int findMaxSequenceNo(UUID projectId);

    @Query("select coalesce(avg(i.rpn), 0) from FmeaItem i where i.projectId = :projectId")
    double averageRpn(UUID projectId);

    @Query("select coalesce(max(i.rpn), 0) from FmeaItem i where i.projectId = :projectId")
    int maxRpn(UUID projectId);

    long countByProjectId(UUID projectId);

    void deleteByProjectId(UUID projectId);
}
