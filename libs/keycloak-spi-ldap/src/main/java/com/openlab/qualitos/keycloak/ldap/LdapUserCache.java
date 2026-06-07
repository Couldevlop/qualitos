package com.openlab.qualitos.keycloak.ldap;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache local simple à TTL pour les entrées LDAP résolues, afin de limiter les
 * allers-retours réseau (un login lookup + getUserById + searchForUser peuvent
 * cibler le même utilisateur). Partagé au niveau de la factory (donc entre
 * sessions Keycloak), clé = {@code <type>:<valeur>}.
 *
 * <p>Implémentation volontairement minimale (pas de dépendance externe) : éviction
 * paresseuse à la lecture. Pour un cache distribué, Keycloak fournit Infinispan,
 * mais ce cache local suffit au pattern lookup→auth d'une même requête de login.
 */
public final class LdapUserCache {

    private record Entry(LdapUserEntry value, long expiresAtMs) {
        boolean isExpired(long nowMs) {
            return nowMs >= expiresAtMs;
        }
    }

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();
    private final long ttlMs;
    private final java.util.function.LongSupplier clock;

    public LdapUserCache(long ttlSeconds) {
        this(ttlSeconds, System::currentTimeMillis);
    }

    /** Constructeur testable avec horloge injectable. */
    LdapUserCache(long ttlSeconds, java.util.function.LongSupplier clock) {
        this.ttlMs = Math.max(0L, ttlSeconds) * 1000L;
        this.clock = clock;
    }

    /** @return l'entrée si présente et non expirée. */
    public Optional<LdapUserEntry> get(String key) {
        if (ttlMs == 0L) {
            return Optional.empty();
        }
        Entry e = store.get(key);
        if (e == null) {
            return Optional.empty();
        }
        if (e.isExpired(clock.getAsLong())) {
            store.remove(key, e);
            return Optional.empty();
        }
        return Optional.of(e.value());
    }

    /** Met en cache une entrée résolue (sous toutes ses clés naturelles). */
    public void put(LdapUserEntry entry) {
        if (ttlMs == 0L || entry == null) {
            return;
        }
        long expiry = clock.getAsLong() + ttlMs;
        store.put(usernameKey(entry.username()), new Entry(entry, expiry));
        store.put(uuidKey(entry.uuid()), new Entry(entry, expiry));
        if (entry.email() != null && !entry.email().isBlank()) {
            store.put(emailKey(entry.email()), new Entry(entry, expiry));
        }
    }

    public void invalidateAll() {
        store.clear();
    }

    int size() {
        return store.size();
    }

    public static String usernameKey(String username) {
        return "u:" + lower(username);
    }

    public static String emailKey(String email) {
        return "e:" + lower(email);
    }

    public static String uuidKey(String uuid) {
        return "id:" + uuid;
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase();
    }
}
