package com.openlab.qualitos.keycloak.ldap;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LdapConfigTest {

    @Test
    void appliesOpenLdapDefaultsWhenAttributesOmitted() {
        LdapConfig cfg = LdapConfig.fromComponentModel(ComponentModels.of(ComponentModels.minimalValid()));

        assertThat(cfg.connectionUrl()).isEqualTo("ldap://localhost:389");
        assertThat(cfg.usersDn()).isEqualTo("ou=people,dc=example,dc=com");
        assertThat(cfg.usernameAttribute()).isEqualTo("uid");
        assertThat(cfg.emailAttribute()).isEqualTo("mail");
        assertThat(cfg.firstNameAttribute()).isEqualTo("givenName");
        assertThat(cfg.lastNameAttribute()).isEqualTo("sn");
        assertThat(cfg.uuidAttribute()).isEqualTo("entryUUID");
        assertThat(cfg.cacheTtlSeconds()).isEqualTo(300L);
        assertThat(cfg.connectTimeoutMs()).isEqualTo(5000);
        assertThat(cfg.readTimeoutMs()).isEqualTo(5000);
        assertThat(cfg.userObjectClasses()).containsExactly("inetOrgPerson", "organizationalPerson", "person");
    }

    @Test
    void mapsActiveDirectoryAttributes() {
        var m = ComponentModels.minimalValid();
        m.put(LdapConfig.CONNECTION_URL, "ldaps://ad.example.com:636");
        m.put(LdapConfig.USERNAME_ATTR, "sAMAccountName");
        m.put(LdapConfig.UUID_ATTR, "objectGUID");
        m.put(LdapConfig.USER_OBJECT_CLASSES, "user, person");

        LdapConfig cfg = LdapConfig.fromComponentModel(ComponentModels.of(m));

        assertThat(cfg.usernameAttribute()).isEqualTo("sAMAccountName");
        assertThat(cfg.uuidAttribute()).isEqualTo("objectGUID");
        assertThat(cfg.userObjectClasses()).containsExactly("user", "person");
    }

    @Test
    void mapsFreeIpaAttributes() {
        var m = ComponentModels.minimalValid();
        m.put(LdapConfig.USERNAME_ATTR, "uid");
        m.put(LdapConfig.UUID_ATTR, "ipaUniqueID");
        m.put(LdapConfig.USER_OBJECT_CLASSES, "inetOrgPerson,posixAccount,ipaUser");

        LdapConfig cfg = LdapConfig.fromComponentModel(ComponentModels.of(m));

        assertThat(cfg.uuidAttribute()).isEqualTo("ipaUniqueID");
        assertThat(cfg.userObjectClasses()).containsExactly("inetOrgPerson", "posixAccount", "ipaUser");
    }

    @Test
    void requiresConnectionUrl() {
        Map<String, String> m = new HashMap<>();
        m.put(LdapConfig.USERS_DN, "ou=people,dc=example,dc=com");
        assertThatThrownBy(() -> LdapConfig.fromComponentModel(ComponentModels.of(m)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(LdapConfig.CONNECTION_URL);
    }

    @Test
    void requiresUsersDn() {
        Map<String, String> m = new HashMap<>();
        m.put(LdapConfig.CONNECTION_URL, "ldap://localhost:389");
        assertThatThrownBy(() -> LdapConfig.fromComponentModel(ComponentModels.of(m)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(LdapConfig.USERS_DN);
    }

    @Test
    void parsesNumericFieldsAndFallsBackOnGarbage() {
        var m = ComponentModels.minimalValid();
        m.put(LdapConfig.CACHE_TTL_SECONDS, "not-a-number");
        m.put(LdapConfig.CONNECT_TIMEOUT_MS, "1234");
        m.put(LdapConfig.READ_TIMEOUT_MS, "-1"); // négatif → fallback

        LdapConfig cfg = LdapConfig.fromComponentModel(ComponentModels.of(m));

        assertThat(cfg.cacheTtlSeconds()).isEqualTo(300L);
        assertThat(cfg.connectTimeoutMs()).isEqualTo(1234);
        assertThat(cfg.readTimeoutMs()).isEqualTo(5000);
    }

    @Test
    void buildsSimpleEqualityFilterWithEscaping() {
        LdapConfig cfg = LdapConfig.fromComponentModel(ComponentModels.of(ComponentModels.minimalValid()));
        assertThat(cfg.buildFilter("uid", "j*dupont")).isEqualTo("(uid=j\\2adupont)");
    }

    @Test
    void combinesCustomSearchFilterWithAnd() {
        var m = ComponentModels.minimalValid();
        m.put(LdapConfig.SEARCH_FILTER, "(memberOf=cn=qualitos,ou=groups,dc=example,dc=com)");
        LdapConfig cfg = LdapConfig.fromComponentModel(ComponentModels.of(m));

        assertThat(cfg.buildFilter("uid", "jdupont"))
                .isEqualTo("(&(uid=jdupont)(memberOf=cn=qualitos,ou=groups,dc=example,dc=com))");
    }

    @Test
    void wrapsCustomFilterMissingParentheses() {
        var m = ComponentModels.minimalValid();
        m.put(LdapConfig.SEARCH_FILTER, "objectClass=person");
        LdapConfig cfg = LdapConfig.fromComponentModel(ComponentModels.of(m));

        assertThat(cfg.buildFilter("uid", "x")).isEqualTo("(&(uid=x)(objectClass=person))");
    }

    @Test
    void toStringNeverLeaksBindCredential() {
        var m = ComponentModels.minimalValid();
        m.put(LdapConfig.BIND_DN, "cn=svc,dc=example,dc=com");
        m.put(LdapConfig.BIND_CREDENTIAL, "SuperSecretP@ss");
        LdapConfig cfg = LdapConfig.fromComponentModel(ComponentModels.of(m));

        assertThat(cfg.toString())
                .doesNotContain("SuperSecretP@ss")
                .contains("bindCredential=***");
    }

    @Test
    void emptyBindCredentialShownAsNone() {
        LdapConfig cfg = LdapConfig.fromComponentModel(ComponentModels.of(ComponentModels.minimalValid()));
        assertThat(cfg.toString()).contains("bindCredential=<none>");
    }
}
