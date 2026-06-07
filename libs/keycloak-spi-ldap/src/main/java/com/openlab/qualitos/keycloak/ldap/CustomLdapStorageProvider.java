package com.openlab.qualitos.keycloak.ldap;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * UserStorageProvider QualitOS qui fédère des annuaires LDAP non standards
 * (Active Directory, OpenLDAP, FreeIPA) — CLAUDE.md §13.4.
 *
 * <p>Capacités :
 * <ul>
 *   <li>{@link UserLookupProvider} : résolution par id / username / email ;</li>
 *   <li>{@link UserQueryProvider} : recherche paginée pour la console admin ;</li>
 *   <li>{@link CredentialInputValidator} : validation du mot de passe par bind LDAP.</li>
 * </ul>
 *
 * <p>Le mapping d'attributs est entièrement piloté par la configuration
 * ({@link LdapConfig}) : un même code couvre {@code sAMAccountName} (AD) ou
 * {@code uid} (OpenLDAP/FreeIPA).
 *
 * <p><strong>Fallback transparent</strong> : si l'annuaire est injoignable
 * ({@link LdapUnavailableException}), le provider journalise et renvoie
 * {@code null}/{@code false} au lieu de propager l'erreur — Keycloak délègue alors
 * aux autres providers / au stockage local et le login local n'est pas cassé.
 */
public class CustomLdapStorageProvider
        implements UserStorageProvider, UserLookupProvider, UserQueryProvider, CredentialInputValidator {

    private static final Logger LOG = Logger.getLogger(CustomLdapStorageProvider.class);

    private final KeycloakSession session;
    private final ComponentModel model;
    private final LdapConfig config;
    private final LdapClient client;
    private final LdapUserCache cache;

    public CustomLdapStorageProvider(KeycloakSession session,
                                     ComponentModel model,
                                     LdapConfig config,
                                     LdapClient client,
                                     LdapUserCache cache) {
        this.session = session;
        this.model = model;
        this.config = config;
        this.client = client;
        this.cache = cache;
    }

    // ===================== UserLookupProvider =====================

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        // id Keycloak fédéré = "f:<providerId>:<externalId>" ; on extrait l'externalId.
        StorageId storageId = new StorageId(id);
        String externalId = storageId.getExternalId();
        return resolveByUuid(realm, externalId).orElse(null);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        return lookup(realm, LdapUserCache.usernameKey(username),
                () -> client.findByAttribute(config.usernameAttribute(), username))
                .orElse(null);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return lookup(realm, LdapUserCache.emailKey(email),
                () -> client.findByAttribute(config.emailAttribute(), email))
                .orElse(null);
    }

    private Optional<UserModel> resolveByUuid(RealmModel realm, String uuid) {
        return lookup(realm, LdapUserCache.uuidKey(uuid), () -> client.findByUuid(uuid));
    }

    /**
     * Lookup générique : cache → LDAP → mapping vers UserModel. Applique le
     * fallback transparent en cas d'indisponibilité de l'annuaire.
     */
    private Optional<UserModel> lookup(RealmModel realm, String cacheKey,
                                       java.util.function.Supplier<Optional<LdapUserEntry>> loader) {
        Optional<LdapUserEntry> cached = cache.get(cacheKey);
        if (cached.isPresent()) {
            return Optional.of(toUserModel(realm, cached.get()));
        }
        try {
            Optional<LdapUserEntry> entry = loader.get();
            entry.ifPresent(cache::put);
            return entry.map(e -> toUserModel(realm, e));
        } catch (LdapUnavailableException e) {
            // Fallback : ne casse pas le login local.
            LOG.warnf("Fallback transparent — annuaire injoignable, délégation au stockage local : %s",
                    e.getMessage());
            return Optional.empty();
        }
    }

    private UserModel toUserModel(RealmModel realm, LdapUserEntry entry) {
        return new LdapUserAdapter(session, realm, model, entry);
    }

    // ===================== CredentialInputValidator =====================

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        // Le mot de passe est détenu par l'annuaire LDAP, pas localement.
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof CredentialInput)) {
            return false;
        }
        String password = input.getChallengeResponse();
        if (password == null || password.isEmpty()) {
            return false;
        }
        // Résout le DN à partir du username (cache d'abord), puis bind avec le mot de passe.
        Optional<LdapUserEntry> entry = resolveEntry(user.getUsername());
        if (entry.isEmpty()) {
            return false;
        }
        try {
            return client.authenticate(entry.get().dn(), password);
        } catch (LdapUnavailableException e) {
            // Fallback : annuaire injoignable → on ne valide pas mais on ne crashe pas.
            LOG.warnf("Fallback transparent — validation mot de passe impossible (LDAP injoignable) : %s",
                    e.getMessage());
            return false;
        }
    }

    private Optional<LdapUserEntry> resolveEntry(String username) {
        if (username == null) {
            return Optional.empty();
        }
        Optional<LdapUserEntry> cached = cache.get(LdapUserCache.usernameKey(username));
        if (cached.isPresent()) {
            return cached;
        }
        try {
            Optional<LdapUserEntry> entry = client.findByAttribute(config.usernameAttribute(), username);
            entry.ifPresent(cache::put);
            return entry;
        } catch (LdapUnavailableException e) {
            LOG.warnf("Fallback transparent — résolution utilisateur impossible (LDAP injoignable) : %s",
                    e.getMessage());
            return Optional.empty();
        }
    }

    // ===================== UserQueryProvider =====================

    @Override
    public int getUsersCount(RealmModel realm) {
        try {
            return client.searchAll(0, 0).size();
        } catch (LdapUnavailableException e) {
            LOG.warnf("Fallback transparent — comptage impossible (LDAP injoignable) : %s", e.getMessage());
            return 0;
        }
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params,
                                                 Integer firstResult, Integer maxResults) {
        String search = params == null ? null : params.get(UserModel.SEARCH);
        int first = firstResult == null ? 0 : Math.max(0, firstResult);
        int max = maxResults == null ? -1 : maxResults;

        try {
            if (search != null && !search.isBlank() && !"*".equals(search.trim())) {
                // Recherche ciblée par username puis email.
                String term = search.trim();
                Optional<LdapUserEntry> byUsername = client.findByAttribute(config.usernameAttribute(), term);
                if (byUsername.isPresent()) {
                    byUsername.ifPresent(cache::put);
                    return Stream.of(toUserModel(realm, byUsername.get()));
                }
                Optional<LdapUserEntry> byEmail = client.findByAttribute(config.emailAttribute(), term);
                byEmail.ifPresent(cache::put);
                return byEmail.map(e -> Stream.of(toUserModel(realm, e))).orElseGet(Stream::empty);
            }
            return client.searchAll(first, max).stream()
                    .peek(cache::put)
                    .map(e -> toUserModel(realm, e));
        } catch (LdapUnavailableException e) {
            LOG.warnf("Fallback transparent — recherche impossible (LDAP injoignable) : %s", e.getMessage());
            return Stream.empty();
        }
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group,
                                                   Integer firstResult, Integer maxResults) {
        // Pas de fédération de groupes dans cette V1.
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        try {
            return client.findByAttribute(attrName, attrValue)
                    .map(e -> Stream.of(toUserModel(realm, e)))
                    .orElseGet(Stream::empty);
        } catch (LdapUnavailableException e) {
            LOG.warnf("Fallback transparent — recherche par attribut impossible (LDAP injoignable) : %s",
                    e.getMessage());
            return Stream.empty();
        }
    }

    // ===================== UserStorageProvider =====================

    @Override
    public void close() {
        // Le client JNDI ouvre/ferme un contexte par opération ; rien à libérer ici.
    }
}
