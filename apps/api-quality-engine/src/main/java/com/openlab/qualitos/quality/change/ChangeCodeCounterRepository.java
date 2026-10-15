package com.openlab.qualitos.quality.change;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface ChangeCodeCounterRepository extends JpaRepository<ChangeCodeCounter, ChangeCodeCounter.Key> {

    /** Le compteur d'un client pour une année, sous verrou : les créations concurrentes sont sérialisées. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ChangeCodeCounter> findById(ChangeCodeCounter.Key key);
}
