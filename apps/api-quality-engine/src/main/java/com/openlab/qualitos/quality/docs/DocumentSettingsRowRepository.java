package com.openlab.qualitos.quality.docs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DocumentSettingsRowRepository extends JpaRepository<DocumentSettingsRow, UUID> {

    /** Les clients qui conservent leurs versions obsolètes pour une durée bornée. */
    @Query("select s from DocumentSettingsRow s where s.obsoleteRetentionYears is not null")
    List<DocumentSettingsRow> findWithRetention();
}
