package com.openlab.qualitos.keycloak.ldap;

import org.junit.jupiter.api.Test;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class CustomLdapStorageProviderFactoryTest {

    private final CustomLdapStorageProviderFactory factory = new CustomLdapStorageProviderFactory();

    @Test
    void providerIdIsStable() {
        assertThat(factory.getId()).isEqualTo("qualitos-ldap-custom");
    }

    @Test
    void exposesAllConfigPropertiesIncludingMappings() {
        List<ProviderConfigProperty> props = factory.getConfigProperties();
        List<String> names = props.stream().map(ProviderConfigProperty::getName).toList();

        assertThat(names).contains(
                LdapConfig.CONNECTION_URL, LdapConfig.BIND_DN, LdapConfig.BIND_CREDENTIAL,
                LdapConfig.USERS_DN, LdapConfig.USER_OBJECT_CLASSES,
                LdapConfig.USERNAME_ATTR, LdapConfig.EMAIL_ATTR,
                LdapConfig.FIRST_NAME_ATTR, LdapConfig.LAST_NAME_ATTR, LdapConfig.UUID_ATTR,
                LdapConfig.SEARCH_FILTER, LdapConfig.CACHE_TTL_SECONDS,
                LdapConfig.CONNECT_TIMEOUT_MS, LdapConfig.READ_TIMEOUT_MS);
    }

    @Test
    void bindCredentialIsMarkedSecretPassword() {
        ProviderConfigProperty cred = factory.getConfigProperties().stream()
                .filter(p -> p.getName().equals(LdapConfig.BIND_CREDENTIAL))
                .findFirst().orElseThrow();

        assertThat(cred.getType()).isEqualTo(ProviderConfigProperty.PASSWORD);
        assertThat(cred.isSecret()).isTrue();
    }

    @Test
    void validateConfigurationAcceptsValidModel() {
        var session = mock(KeycloakSession.class);
        var realm = mock(RealmModel.class);
        assertThatCode(() -> factory.validateConfiguration(session, realm,
                ComponentModels.of(ComponentModels.minimalValid())))
                .doesNotThrowAnyException();
    }

    @Test
    void validateConfigurationRejectsMissingUrl() {
        var session = mock(KeycloakSession.class);
        var realm = mock(RealmModel.class);
        var bad = new HashMap<String, String>();
        bad.put(LdapConfig.USERS_DN, "ou=people,dc=example,dc=com");

        assertThatThrownBy(() -> factory.validateConfiguration(session, realm, ComponentModels.of(bad)))
                .isInstanceOf(ComponentValidationException.class);
    }

    @Test
    void createBuildsProvider() {
        var session = mock(KeycloakSession.class);
        CustomLdapStorageProvider provider = factory.create(session,
                ComponentModels.of(ComponentModels.minimalValid()));
        assertThat(provider).isNotNull();
        factory.close();
    }

    @Test
    void helpTextDescribesSupportedDirectories() {
        assertThat(factory.getHelpText())
                .contains("Active Directory")
                .contains("OpenLDAP")
                .contains("FreeIPA");
    }
}
