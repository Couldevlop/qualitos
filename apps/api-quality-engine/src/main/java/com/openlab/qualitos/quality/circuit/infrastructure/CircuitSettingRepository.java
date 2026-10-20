package com.openlab.qualitos.quality.circuit.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

/** L'état actif ou suspendu des circuits. La clé porte le client. */
public interface CircuitSettingRepository extends JpaRepository<CircuitSettingJpaEntity, CircuitSettingJpaEntity.Key> {
}
