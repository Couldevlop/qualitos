package com.openlab.qualitos.keycloak.ldap;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class LdapUserCacheTest {

    private static LdapUserEntry sample() {
        return new LdapUserEntry("uid=jdupont,ou=people,dc=example,dc=com",
                "uuid-1", "jdupont", "j.dupont@example.com", "Jean", "Dupont");
    }

    @Test
    void cachesByUsernameEmailAndUuid() {
        LdapUserCache cache = new LdapUserCache(300);
        cache.put(sample());

        assertThat(cache.get(LdapUserCache.usernameKey("jdupont"))).isPresent();
        assertThat(cache.get(LdapUserCache.emailKey("j.dupont@example.com"))).isPresent();
        assertThat(cache.get(LdapUserCache.uuidKey("uuid-1"))).isPresent();
    }

    @Test
    void usernameLookupIsCaseInsensitive() {
        LdapUserCache cache = new LdapUserCache(300);
        cache.put(sample());
        assertThat(cache.get(LdapUserCache.usernameKey("JDupont"))).isPresent();
    }

    @Test
    void expiresAfterTtl() {
        AtomicLong now = new AtomicLong(0);
        LdapUserCache cache = new LdapUserCache(10, now::get); // TTL = 10s
        cache.put(sample());
        assertThat(cache.get(LdapUserCache.usernameKey("jdupont"))).isPresent();

        now.set(10_001); // > 10s
        assertThat(cache.get(LdapUserCache.usernameKey("jdupont"))).isEmpty();
    }

    @Test
    void ttlZeroDisablesCache() {
        LdapUserCache cache = new LdapUserCache(0);
        cache.put(sample());
        assertThat(cache.get(LdapUserCache.usernameKey("jdupont"))).isEmpty();
        assertThat(cache.size()).isZero();
    }

    @Test
    void invalidateAllClearsStore() {
        LdapUserCache cache = new LdapUserCache(300);
        cache.put(sample());
        cache.invalidateAll();
        assertThat(cache.size()).isZero();
    }

    @Test
    void putNullIsIgnored() {
        LdapUserCache cache = new LdapUserCache(300);
        cache.put(null);
        assertThat(cache.size()).isZero();
    }

    @Test
    void entryWithoutEmailDoesNotCacheEmailKey() {
        LdapUserCache cache = new LdapUserCache(300);
        cache.put(new LdapUserEntry("dn", "uuid", "user", null, null, null));
        assertThat(cache.get(LdapUserCache.emailKey(""))).isEmpty();
        assertThat(cache.get(LdapUserCache.usernameKey("user"))).isPresent();
    }
}
