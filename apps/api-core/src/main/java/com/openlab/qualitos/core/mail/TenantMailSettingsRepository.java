package com.openlab.qualitos.core.mail;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** La messagerie d'un client. Cloisonnée par RLS, comme toute donnée de client. */
public interface TenantMailSettingsRepository extends JpaRepository<TenantMailSettings, UUID> {
}
