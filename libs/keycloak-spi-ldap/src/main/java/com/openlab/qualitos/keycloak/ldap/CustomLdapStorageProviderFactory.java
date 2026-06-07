package com.openlab.qualitos.keycloak.ldap;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory du provider de fédération LDAP custom QualitOS (CLAUDE.md §13.4).
 *
 * <p>Expose la configuration éditable dans la console Keycloak (onglet
 * <em>User Federation</em>) : URL, compte de service, base de recherche,
 * objectClasses, et surtout le <strong>mapping d'attributs</strong> configurable
 * (username/email/prénom/nom/uuid) permettant de couvrir AD, OpenLDAP et FreeIPA
 * sans code spécifique. Le {@code bindCredential} est de type
 * {@link ProviderConfigProperty#PASSWORD_TYPE} (masqué, jamais journalisé).
 */
public class CustomLdapStorageProviderFactory implements UserStorageProviderFactory<CustomLdapStorageProvider> {

    public static final String PROVIDER_ID = "qualitos-ldap-custom";

    private static final Logger LOG = Logger.getLogger(CustomLdapStorageProviderFactory.class);

    /** Cache local partagé par instance de provider (clé = componentId). */
    private final ConcurrentHashMap<String, LdapUserCache> caches = new ConcurrentHashMap<>();

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "QualitOS — Fédération LDAP custom (Active Directory, OpenLDAP, FreeIPA) "
                + "avec mapping d'attributs configurable, cache local et fallback transparent.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                    .name(LdapConfig.CONNECTION_URL)
                    .label("URL de connexion")
                    .helpText("URL LDAP, p.ex. ldap://ad.example.com:389 ou ldaps://ipa.example.com:636")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .add()
                .property()
                    .name(LdapConfig.BIND_DN)
                    .label("Bind DN (compte de service)")
                    .helpText("DN du compte de service pour les recherches. Vide ⇒ bind anonyme.")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .add()
                .property()
                    .name(LdapConfig.BIND_CREDENTIAL)
                    .label("Bind credential")
                    .helpText("Mot de passe du compte de service (stocké chiffré, jamais journalisé).")
                    .type(ProviderConfigProperty.PASSWORD)
                    .secret(true)
                    .add()
                .property()
                    .name(LdapConfig.USERS_DN)
                    .label("Users DN (base de recherche)")
                    .helpText("Branche contenant les utilisateurs, p.ex. ou=people,dc=example,dc=com "
                            + "ou CN=Users,DC=example,DC=com (AD).")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .add()
                .property()
                    .name(LdapConfig.USER_OBJECT_CLASSES)
                    .label("Object classes utilisateur")
                    .helpText("Liste séparée par des virgules. AD: user / OpenLDAP: inetOrgPerson / "
                            + "FreeIPA: inetOrgPerson,posixAccount,ipaUser.")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .defaultValue("inetOrgPerson,organizationalPerson,person")
                    .add()
                .property()
                    .name(LdapConfig.USERNAME_ATTR)
                    .label("Attribut username")
                    .helpText("AD: sAMAccountName — OpenLDAP/FreeIPA: uid")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .defaultValue("uid")
                    .add()
                .property()
                    .name(LdapConfig.EMAIL_ATTR)
                    .label("Attribut email")
                    .helpText("Généralement: mail")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .defaultValue("mail")
                    .add()
                .property()
                    .name(LdapConfig.FIRST_NAME_ATTR)
                    .label("Attribut prénom")
                    .helpText("Généralement: givenName")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .defaultValue("givenName")
                    .add()
                .property()
                    .name(LdapConfig.LAST_NAME_ATTR)
                    .label("Attribut nom")
                    .helpText("Généralement: sn")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .defaultValue("sn")
                    .add()
                .property()
                    .name(LdapConfig.UUID_ATTR)
                    .label("Attribut identifiant unique")
                    .helpText("AD: objectGUID — OpenLDAP: entryUUID — FreeIPA: ipaUniqueID")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .defaultValue("entryUUID")
                    .add()
                .property()
                    .name(LdapConfig.SEARCH_FILTER)
                    .label("Filtre de recherche additionnel")
                    .helpText("Filtre LDAP combiné en AND, p.ex. (memberOf=cn=qualitos,ou=groups,dc=example,dc=com). "
                            + "Vide ⇒ aucun filtre additionnel.")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .add()
                .property()
                    .name(LdapConfig.CACHE_TTL_SECONDS)
                    .label("TTL cache (secondes)")
                    .helpText("Durée de mise en cache des entrées résolues. 0 ⇒ cache désactivé.")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .defaultValue("300")
                    .add()
                .property()
                    .name(LdapConfig.CONNECT_TIMEOUT_MS)
                    .label("Timeout connexion (ms)")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .defaultValue("5000")
                    .add()
                .property()
                    .name(LdapConfig.READ_TIMEOUT_MS)
                    .label("Timeout lecture (ms)")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .defaultValue("5000")
                    .add()
                .build();
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model)
            throws ComponentValidationException {
        try {
            LdapConfig.fromComponentModel(model);
        } catch (IllegalStateException e) {
            throw new ComponentValidationException(e.getMessage());
        }
        // Invalide le cache existant lors d'une reconfiguration.
        caches.remove(model.getId());
    }

    @Override
    public CustomLdapStorageProvider create(KeycloakSession session, ComponentModel model) {
        LdapConfig config = LdapConfig.fromComponentModel(model);
        LdapClient client = new JndiLdapClient(config);
        LdapUserCache cache = caches.computeIfAbsent(model.getId(),
                id -> new LdapUserCache(config.cacheTtlSeconds()));
        return new CustomLdapStorageProvider(session, model, config, client, cache);
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {
        LOG.infof("Initialisation du provider de fédération LDAP custom QualitOS (id=%s)", PROVIDER_ID);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // rien
    }

    @Override
    public void close() {
        caches.clear();
    }
}
