package com.openlab.qualitos.quality.rls;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Isolation des clients par la base elle-même (Row-Level Security, ADR 0086).
 *
 * <p>{@code appUser} : le rôle PostgreSQL sous lequel l'application travaille. Il
 * n'est ni superutilisateur ni propriétaire des tables — sans quoi PostgreSQL
 * l'exempterait de toute politique. Les migrations, elles, gardent le rôle
 * propriétaire. Vide : l'application travaille sous le rôle propriétaire, comme
 * avant (et les politiques, même actives, ne la contraignent pas).
 *
 * <p>{@code enabled} : active les politiques. Désactiver suffit à revenir en
 * arrière au démarrage suivant, sans migration.
 */
@Component
@ConfigurationProperties(prefix = "qualitos.rls")
public class RlsProperties {

    private boolean enabled = false;
    private String appUser = "";
    private String appPassword = "";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getAppUser() { return appUser; }
    public void setAppUser(String appUser) { this.appUser = appUser == null ? "" : appUser.trim(); }
    public String getAppPassword() { return appPassword; }
    public void setAppPassword(String appPassword) { this.appPassword = appPassword == null ? "" : appPassword; }

    /** Un rôle applicatif distinct est demandé. */
    public boolean hasAppUser() { return !appUser.isEmpty(); }
}
