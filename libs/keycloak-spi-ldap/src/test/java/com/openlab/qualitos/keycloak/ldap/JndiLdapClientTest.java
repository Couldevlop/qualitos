package com.openlab.qualitos.keycloak.ldap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests d'intégration légers de {@link JndiLdapClient} sans serveur réel : on
 * pointe vers un port fermé (timeout court) pour valider la traduction
 * "indisponibilité réseau ⇒ {@link LdapUnavailableException}" qui déclenche le
 * fallback, et le rejet gracieux des entrées invalides.
 */
class JndiLdapClientTest {

    private LdapConfig unreachableConfig() {
        var m = ComponentModels.minimalValid();
        // Port très probablement fermé sur l'hôte de build (loopback).
        m.put(LdapConfig.CONNECTION_URL, "ldap://127.0.0.1:1");
        m.put(LdapConfig.CONNECT_TIMEOUT_MS, "300");
        m.put(LdapConfig.READ_TIMEOUT_MS, "300");
        return LdapConfig.fromComponentModel(ComponentModels.of(m));
    }

    @Test
    void findByAttributeThrowsUnavailableWhenServerDown() {
        JndiLdapClient client = new JndiLdapClient(unreachableConfig());
        assertThatThrownBy(() -> client.findByAttribute("uid", "jdupont"))
                .isInstanceOf(LdapUnavailableException.class);
    }

    @Test
    void findByUuidThrowsUnavailableWhenServerDown() {
        JndiLdapClient client = new JndiLdapClient(unreachableConfig());
        assertThatThrownBy(() -> client.findByUuid("uuid-1"))
                .isInstanceOf(LdapUnavailableException.class);
    }

    @Test
    void searchAllThrowsUnavailableWhenServerDown() {
        JndiLdapClient client = new JndiLdapClient(unreachableConfig());
        assertThatThrownBy(() -> client.searchAll(0, 10))
                .isInstanceOf(LdapUnavailableException.class);
    }

    @Test
    void authenticateWithBlankInputsReturnsFalseWithoutNetwork() {
        JndiLdapClient client = new JndiLdapClient(unreachableConfig());
        // Aucune connexion n'est tentée si les entrées sont vides : pas d'exception.
        assertThat(client.authenticate(null, "x")).isFalse();
        assertThat(client.authenticate("uid=x,dc=e,dc=c", "")).isFalse();
        assertThat(client.authenticate("uid=x,dc=e,dc=c", null)).isFalse();
    }

    @Test
    void authenticateThrowsUnavailableWhenServerDown() {
        JndiLdapClient client = new JndiLdapClient(unreachableConfig());
        assertThatThrownBy(() -> client.authenticate("uid=x,ou=people,dc=example,dc=com", "secret"))
                .isInstanceOf(LdapUnavailableException.class);
    }
}
