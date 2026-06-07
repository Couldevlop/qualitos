package com.openlab.qualitos.keycloak.ldap;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;

import java.util.Map;

/** Fabrique de {@link ComponentModel} pour les tests (config par mapping de clés). */
final class ComponentModels {

    private ComponentModels() {
    }

    static ComponentModel of(Map<String, String> config) {
        ComponentModel model = new ComponentModel();
        model.setId("test-component");
        model.setProviderId(CustomLdapStorageProviderFactory.PROVIDER_ID);
        MultivaluedHashMap<String, String> mv = new MultivaluedHashMap<>();
        config.forEach(mv::putSingle);
        model.setConfig(mv);
        return model;
    }

    /** Config valide minimale (OpenLDAP par défaut). */
    static java.util.HashMap<String, String> minimalValid() {
        var m = new java.util.HashMap<String, String>();
        m.put(LdapConfig.CONNECTION_URL, "ldap://localhost:389");
        m.put(LdapConfig.USERS_DN, "ou=people,dc=example,dc=com");
        return m;
    }
}
