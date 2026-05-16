package com.openlab.qualitos.quality.kpi;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KpiMeasurementRepository extends JpaRepository<KpiMeasurement, UUID> {

    Optional<KpiMeasurement> findByKpiIdAndPeriodStart(UUID kpiId, Instant periodStart);

    Page<KpiMeasurement> findByKpiIdOrderByPeriodStartDesc(UUID kpiId, Pageable pageable);

    /** Pour le calcul de tendance : N dernières mesures par ordre décroissant. */
    List<KpiMeasurement> findTop24ByKpiIdOrderByPeriodStartDesc(UUID kpiId);

    long countByKpiId(UUID kpiId);

    void deleteByKpiId(UUID kpiId);
}
