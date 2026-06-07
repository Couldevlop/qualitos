package com.openlab.qualitos.keycloak.ldap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomLdapStorageProviderTest {

    @Mock
    KeycloakSession session;
    @Mock
    RealmModel realm;

    private ComponentModel model;
    private LdapConfig config;
    private FakeLdapClient client;
    private LdapUserCache cache;
    private CustomLdapStorageProvider provider;

    private static final LdapUserEntry JDUPONT = new LdapUserEntry(
            "uid=jdupont,ou=people,dc=example,dc=com",
            "uuid-jdupont", "jdupont", "j.dupont@example.com", "Jean", "Dupont");

    @BeforeEach
    void setUp() {
        var cfg = ComponentModels.minimalValid();
        model = ComponentModels.of(cfg);
        config = LdapConfig.fromComponentModel(model);
        client = new FakeLdapClient();
        cache = new LdapUserCache(300);
        provider = new CustomLdapStorageProvider(session, model, config, client, cache);
        lenient().when(realm.getName()).thenReturn("qualitos");
    }

    // --------------------------- lookup ---------------------------

    @Test
    void getUserByUsernameMapsAttributes() {
        client.byUsername.put("jdupont", JDUPONT);

        UserModel user = provider.getUserByUsername(realm, "jdupont");

        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo("jdupont");
        assertThat(user.getEmail()).isEqualTo("j.dupont@example.com");
        assertThat(user.getFirstName()).isEqualTo("Jean");
        assertThat(user.getLastName()).isEqualTo("Dupont");
    }

    @Test
    void getUserByUsernameReturnsNullWhenAbsent() {
        assertThat(provider.getUserByUsername(realm, "ghost")).isNull();
    }

    @Test
    void getUserByEmailResolves() {
        client.byEmail.put("j.dupont@example.com", JDUPONT);
        UserModel user = provider.getUserByEmail(realm, "j.dupont@example.com");
        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo("jdupont");
    }

    @Test
    void secondLookupServedFromCacheWithoutHittingClient() {
        client.byUsername.put("jdupont", JDUPONT);
        provider.getUserByUsername(realm, "jdupont");
        int callsAfterFirst = client.findByAttributeCalls;

        provider.getUserByUsername(realm, "jdupont");
        assertThat(client.findByAttributeCalls).isEqualTo(callsAfterFirst); // pas de nouvel appel
    }

    // --------------------------- fallback ---------------------------

    @Test
    void fallbackReturnsNullWhenDirectoryUnavailable() {
        client.unavailable = true;
        assertThat(provider.getUserByUsername(realm, "jdupont")).isNull();
        assertThat(provider.getUserByEmail(realm, "x@example.com")).isNull();
    }

    @Test
    void fallbackPasswordValidationReturnsFalseWhenUnavailable() {
        client.byUsername.put("jdupont", JDUPONT);
        // résout l'entrée d'abord (réussi), puis bind échoue pour indispo
        cache.put(JDUPONT);
        client.unavailable = true;

        boolean valid = provider.isValid(realm, userNamed("jdupont"), password("whatever"));
        assertThat(valid).isFalse();
    }

    @Test
    void searchCountIsZeroWhenUnavailable() {
        client.unavailable = true;
        assertThat(provider.getUsersCount(realm)).isZero();
        assertThat(provider.searchForUserStream(realm, Map.of(UserModel.SEARCH, "x"), 0, 10)).isEmpty();
    }

    // --------------------------- credentials ---------------------------

    @Test
    void supportsOnlyPasswordCredentialType() {
        assertThat(provider.supportsCredentialType(PasswordCredentialModel.TYPE)).isTrue();
        assertThat(provider.supportsCredentialType("otp")).isFalse();
    }

    @Test
    void validPasswordBindsSuccessfully() {
        client.byUsername.put("jdupont", JDUPONT);
        client.validPasswords.put(JDUPONT.dn(), "correct-horse");

        assertThat(provider.isValid(realm, userNamed("jdupont"), password("correct-horse"))).isTrue();
    }

    @Test
    void wrongPasswordRejected() {
        client.byUsername.put("jdupont", JDUPONT);
        client.validPasswords.put(JDUPONT.dn(), "correct-horse");

        assertThat(provider.isValid(realm, userNamed("jdupont"), password("wrong"))).isFalse();
    }

    @Test
    void emptyPasswordRejected() {
        client.byUsername.put("jdupont", JDUPONT);
        assertThat(provider.isValid(realm, userNamed("jdupont"), password(""))).isFalse();
    }

    @Test
    void passwordForUnknownUserRejected() {
        assertThat(provider.isValid(realm, userNamed("ghost"), password("x"))).isFalse();
    }

    // --------------------------- search ---------------------------

    @Test
    void searchByTermFindsByUsername() {
        client.byUsername.put("jdupont", JDUPONT);
        var result = provider.searchForUserStream(realm, Map.of(UserModel.SEARCH, "jdupont"), 0, 10).toList();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("jdupont");
    }

    @Test
    void searchAllListsEntries() {
        client.all.add(JDUPONT);
        var result = provider.searchForUserStream(realm, Map.of(), 0, 10).toList();
        assertThat(result).hasSize(1);
    }

    @Test
    void usersCountReflectsDirectory() {
        client.all.add(JDUPONT);
        assertThat(provider.getUsersCount(realm)).isEqualTo(1);
    }

    @Test
    void closeIsNoop() {
        provider.close();
    }

    // --------------------------- helpers ---------------------------

    private UserModel userNamed(String username) {
        UserModel u = org.mockito.Mockito.mock(UserModel.class);
        when(u.getUsername()).thenReturn(username);
        return u;
    }

    private CredentialInput password(String value) {
        CredentialInput input = org.mockito.Mockito.mock(CredentialInput.class);
        when(input.getType()).thenReturn(PasswordCredentialModel.TYPE);
        when(input.getChallengeResponse()).thenReturn(value);
        return input;
    }

    /** Fausse implémentation de LdapClient, sans serveur. */
    static final class FakeLdapClient implements LdapClient {
        final Map<String, LdapUserEntry> byUsername = new java.util.HashMap<>();
        final Map<String, LdapUserEntry> byEmail = new java.util.HashMap<>();
        final Map<String, LdapUserEntry> byUuid = new java.util.HashMap<>();
        final java.util.List<LdapUserEntry> all = new java.util.ArrayList<>();
        final Map<String, String> validPasswords = new java.util.HashMap<>();
        boolean unavailable = false;
        int findByAttributeCalls = 0;

        @Override
        public Optional<LdapUserEntry> findByAttribute(String attribute, String value) {
            findByAttributeCalls++;
            if (unavailable) {
                throw new LdapUnavailableException("down", null);
            }
            if ("mail".equals(attribute)) {
                return Optional.ofNullable(byEmail.get(value));
            }
            return Optional.ofNullable(byUsername.get(value));
        }

        @Override
        public Optional<LdapUserEntry> findByUuid(String uuid) {
            if (unavailable) {
                throw new LdapUnavailableException("down", null);
            }
            return Optional.ofNullable(byUuid.get(uuid));
        }

        @Override
        public java.util.List<LdapUserEntry> searchAll(int firstResult, int maxResults) {
            if (unavailable) {
                throw new LdapUnavailableException("down", null);
            }
            return java.util.List.copyOf(all);
        }

        @Override
        public boolean authenticate(String userDn, String password) {
            if (unavailable) {
                throw new LdapUnavailableException("down", null);
            }
            return password != null && password.equals(validPasswords.get(userDn));
        }
    }
}
