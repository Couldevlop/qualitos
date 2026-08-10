package com.openlab.qualitos.quality.audit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditMailPropertiesTest {

    @Test
    void mailIsOffByDefaultAndCarriesNoBuiltInSender() {
        // §18.2.3 : aucun secret ni valeur d'exploitation en dur. Une adresse
        // d'expédition par défaut ferait partir des messages depuis un domaine
        // qui n'appartient à personne.
        AuditMailProperties p = new AuditMailProperties();
        assertThat(p.isEnabled()).isFalse();
        assertThat(p.getFrom()).isNull();
    }

    @Test
    void readsBackWhatWasConfigured() {
        AuditMailProperties p = new AuditMailProperties();
        p.setEnabled(true);
        p.setFrom("qms@exemple.test");
        assertThat(p.isEnabled()).isTrue();
        assertThat(p.getFrom()).isEqualTo("qms@exemple.test");
    }
}
