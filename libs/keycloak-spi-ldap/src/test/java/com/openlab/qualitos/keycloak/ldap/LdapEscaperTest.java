package com.openlab.qualitos.keycloak.ldap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LdapEscaperTest {

    @Test
    void escapesNullToEmpty() {
        assertThat(LdapEscaper.escapeFilterValue(null)).isEmpty();
    }

    @Test
    void leavesPlainValueUntouched() {
        assertThat(LdapEscaper.escapeFilterValue("jdupont")).isEqualTo("jdupont");
    }

    @Test
    void escapesSpecialCharactersPerRfc4515() {
        assertThat(LdapEscaper.escapeFilterValue("a*b(c)d\\e\0f"))
                .isEqualTo("a\\2ab\\28c\\29d\\5ce\\00f");
    }

    @Test
    void preventsFilterInjectionAttempt() {
        // Tentative d'injection : *)(uid=* doit être neutralisée.
        String escaped = LdapEscaper.escapeFilterValue("*)(uid=*");
        assertThat(escaped).doesNotContain("*").doesNotContain("(").doesNotContain(")");
        assertThat(escaped).isEqualTo("\\2a\\29\\28uid=\\2a");
    }
}
