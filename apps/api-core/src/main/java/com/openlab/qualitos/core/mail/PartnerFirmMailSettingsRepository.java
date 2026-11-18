package com.openlab.qualitos.core.mail;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * La messagerie d'un cabinet partenaire.
 *
 * <p>Donnée de plateforme, sans RLS : un cabinet n'appartient à aucun client,
 * il en sert plusieurs. Le cloisonnement est ici celui du <b>rôle</b>, posé par
 * le contrôleur, comme pour {@code partner_firms} elle-même.
 */
public interface PartnerFirmMailSettingsRepository
        extends JpaRepository<PartnerFirmMailSettings, UUID> {
}
