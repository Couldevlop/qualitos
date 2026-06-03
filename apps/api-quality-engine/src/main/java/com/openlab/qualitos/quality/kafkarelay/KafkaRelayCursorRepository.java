package com.openlab.qualitos.quality.kafkarelay;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface KafkaRelayCursorRepository extends JpaRepository<KafkaRelayCursor, UUID> {
}
