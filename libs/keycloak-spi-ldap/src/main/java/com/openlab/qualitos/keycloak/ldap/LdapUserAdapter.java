package com.openlab.qualitos.keycloak.ldap;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

/**
 * Adapte une {@link LdapUserEntry} en {@link org.keycloak.models.UserModel}.
 *
 * <p>S'appuie sur {@link AbstractUserAdapterFederatedStorage} : les attributs
 * en lecture seule (username, email, prénom, nom) proviennent de l'annuaire ;
 * les rôles/groupes/attributs additionnels éventuels sont gérés par le federated
 * storage de Keycloak. L'identifiant fédéré est {@code f:<providerId>:<uuid>}.
 */
public class LdapUserAdapter extends AbstractUserAdapterFederatedStorage {

    private final LdapUserEntry entry;
    private final String keycloakId;

    public LdapUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, LdapUserEntry entry) {
        super(session, realm, model);
        this.entry = entry;
        this.keycloakId = StorageId.keycloakId(model, entry.uuid());
    }

    @Override
    public String getId() {
        return keycloakId;
    }

    @Override
    public String getUsername() {
        return entry.username();
    }

    @Override
    public void setUsername(String username) {
        // Annuaire en lecture seule dans cette V1 : pas d'écriture vers LDAP.
        throw new UnsupportedOperationException("Le username est en lecture seule (fédération LDAP)");
    }

    @Override
    public String getEmail() {
        return entry.email();
    }

    @Override
    public void setEmail(String email) {
        throw new UnsupportedOperationException("L'email est en lecture seule (fédération LDAP)");
    }

    @Override
    public String getFirstName() {
        return entry.firstName();
    }

    @Override
    public void setFirstName(String firstName) {
        throw new UnsupportedOperationException("Le prénom est en lecture seule (fédération LDAP)");
    }

    @Override
    public String getLastName() {
        return entry.lastName();
    }

    @Override
    public void setLastName(String lastName) {
        throw new UnsupportedOperationException("Le nom est en lecture seule (fédération LDAP)");
    }

    /** DN LDAP exposé pour l'audit/debug (jamais de credential). */
    public String getDn() {
        return entry.dn();
    }
}
