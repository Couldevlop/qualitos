package com.openlab.qualitos.quality.dmaic;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PokaYokeDeviceRepository extends JpaRepository<PokaYokeDevice, UUID> {

    Page<PokaYokeDevice> findByType(PokaYokeType type, Pageable pageable);

    Page<PokaYokeDevice> findByMechanism(PokaYokeMechanism mechanism, Pageable pageable);

    Optional<PokaYokeDevice> findByCode(String code);
}
