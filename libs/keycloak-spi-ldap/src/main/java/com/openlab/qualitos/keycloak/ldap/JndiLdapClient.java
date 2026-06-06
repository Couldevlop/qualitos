package com.openlab.qualitos.keycloak.ldap;

import org.jboss.logging.Logger;

import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation de production de {@link LdapClient} basée sur JNDI
 * ({@code javax.naming.ldap}) — aucune dépendance lourde, fournie par le JDK.
 *
 * <p>Sécurité :
 * <ul>
 *   <li>les valeurs de filtre sont échappées (RFC 4515) via {@link LdapConfig#buildFilter};</li>
 *   <li>le {@code bindCredential} n'est jamais journalisé ;</li>
 *   <li>connect/read timeouts imposés pour éviter les blocages ;</li>
 *   <li>un bind d'authentification en échec renvoie {@code false} (pas d'exception) ;</li>
 *   <li>une indisponibilité réseau lève {@link LdapUnavailableException} → fallback.</li>
 * </ul>
 */
public class JndiLdapClient implements LdapClient {

    private static final Logger LOG = Logger.getLogger(JndiLdapClient.class);

    private final LdapConfig config;

    public JndiLdapClient(LdapConfig config) {
        this.config = config;
    }

    @Override
    public Optional<LdapUserEntry> findByAttribute(String attribute, String value) {
        return searchOne(config.buildFilter(attribute, value));
    }

    @Override
    public Optional<LdapUserEntry> findByUuid(String uuid) {
        return searchOne(config.buildFilter(config.uuidAttribute(), uuid));
    }

    @Override
    public List<LdapUserEntry> searchAll(int firstResult, int maxResults) {
        // Filtre "présence de l'attribut username" = toutes les entrées utilisateur.
        String filter = config.buildFilter(config.usernameAttribute(), "*RAW_PRESENCE*")
                .replace(LdapEscaper.escapeFilterValue("*RAW_PRESENCE*"), "*");
        LdapContext ctx = null;
        try {
            ctx = serviceContext();
            SearchControls controls = searchControls();
            int wanted = maxResults <= 0 ? 0 : firstResult + maxResults;
            if (wanted > 0) {
                controls.setCountLimit(wanted);
            }
            NamingEnumeration<SearchResult> results = ctx.search(config.usersDn(), filter, controls);
            List<LdapUserEntry> all = new ArrayList<>();
            int idx = 0;
            while (results.hasMore()) {
                SearchResult r = results.next();
                if (idx++ < firstResult) {
                    continue;
                }
                all.add(toEntry(r));
                if (maxResults > 0 && all.size() >= maxResults) {
                    break;
                }
            }
            return all;
        } catch (CommunicationException | ServiceUnavailableException e) {
            throw unavailable("searchAll", e);
        } catch (NamingException e) {
            LOG.warnf("Recherche LDAP échouée (searchAll) : %s", e.getMessage());
            return List.of();
        } finally {
            close(ctx);
        }
    }

    @Override
    public boolean authenticate(String userDn, String password) {
        if (userDn == null || userDn.isBlank() || password == null || password.isEmpty()) {
            return false;
        }
        LdapContext ctx = null;
        try {
            ctx = bindContext(userDn, password);
            return true;
        } catch (AuthenticationException e) {
            // Mauvais identifiants : ce n'est PAS une indisponibilité.
            return false;
        } catch (CommunicationException | ServiceUnavailableException e) {
            throw unavailable("authenticate", e);
        } catch (NamingException e) {
            // Toute autre erreur de nommage = refus d'auth (ne crashe pas le login).
            LOG.debugf("Bind d'authentification refusé : %s", e.getMessage());
            return false;
        } finally {
            close(ctx);
        }
    }

    // ---------------------------------------------------------------------

    private Optional<LdapUserEntry> searchOne(String filter) {
        LdapContext ctx = null;
        try {
            ctx = serviceContext();
            SearchControls controls = searchControls();
            controls.setCountLimit(1);
            NamingEnumeration<SearchResult> results = ctx.search(config.usersDn(), filter, controls);
            if (results.hasMore()) {
                return Optional.of(toEntry(results.next()));
            }
            return Optional.empty();
        } catch (CommunicationException | ServiceUnavailableException e) {
            throw unavailable("search", e);
        } catch (NamingException e) {
            LOG.warnf("Recherche LDAP échouée : %s", e.getMessage());
            return Optional.empty();
        } finally {
            close(ctx);
        }
    }

    private LdapUserEntry toEntry(SearchResult result) throws NamingException {
        String dn = result.getNameInNamespace();
        Attributes attrs = result.getAttributes();
        String username = attrValue(attrs, config.usernameAttribute());
        if (username == null) {
            // Sans username exploitable l'entrée est inutilisable : on dérive du DN.
            username = dn;
        }
        return new LdapUserEntry(
                dn,
                attrValue(attrs, config.uuidAttribute()),
                username,
                attrValue(attrs, config.emailAttribute()),
                attrValue(attrs, config.firstNameAttribute()),
                attrValue(attrs, config.lastNameAttribute()));
    }

    private static String attrValue(Attributes attrs, String name) throws NamingException {
        if (attrs == null || name == null) {
            return null;
        }
        Attribute a = attrs.get(name);
        if (a == null || a.size() == 0) {
            return null;
        }
        Object v = a.get();
        return v == null ? null : v.toString();
    }

    private SearchControls searchControls() {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setTimeLimit(config.readTimeoutMs());
        controls.setReturningAttributes(new String[]{
                config.usernameAttribute(),
                config.emailAttribute(),
                config.firstNameAttribute(),
                config.lastNameAttribute(),
                config.uuidAttribute()
        });
        return controls;
    }

    /** Contexte ouvert avec le compte de service (bindDn / bindCredential). */
    private LdapContext serviceContext() throws NamingException {
        return bindContext(config.bindDn(), config.bindCredential());
    }

    private LdapContext bindContext(String principal, String credential) throws NamingException {
        Hashtable<String, Object> env = baseEnv();
        if (principal != null && !principal.isBlank()) {
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, principal);
            env.put(Context.SECURITY_CREDENTIALS, credential == null ? "" : credential);
        } else {
            env.put(Context.SECURITY_AUTHENTICATION, "none");
        }
        return new InitialLdapContext(env, null);
    }

    private Hashtable<String, Object> baseEnv() {
        Hashtable<String, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, config.connectionUrl());
        env.put("com.sun.jndi.ldap.connect.timeout", Integer.toString(config.connectTimeoutMs()));
        env.put("com.sun.jndi.ldap.read.timeout", Integer.toString(config.readTimeoutMs()));
        env.put("com.sun.jndi.ldap.connect.pool", "false");
        if (config.connectionUrl().toLowerCase().startsWith("ldaps://")) {
            env.put(Context.SECURITY_PROTOCOL, "ssl");
        }
        return env;
    }

    private LdapUnavailableException unavailable(String op, NamingException cause) {
        // On ne logge JAMAIS les credentials ; uniquement l'opération et le message.
        LOG.warnf("Annuaire LDAP injoignable lors de '%s' : %s", op, cause.getMessage());
        return new LdapUnavailableException("LDAP indisponible (" + op + ")", cause);
    }

    private static void close(LdapContext ctx) {
        if (ctx != null) {
            try {
                ctx.close();
            } catch (NamingException ignored) {
                // best-effort
            }
        }
    }
}
