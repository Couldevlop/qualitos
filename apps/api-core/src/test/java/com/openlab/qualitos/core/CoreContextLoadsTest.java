package com.openlab.qualitos.core;

import com.openlab.qualitos.core.billing.BillingProfileService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boot RÉEL du contexte Spring complet (component scan entier, aucun slice).
 *
 * <p>Les tests existants du module sont des slices Mockito qui ne bootent jamais
 * le contexte : un bug de câblage (ex. {@code @Component} à deux constructeurs
 * sans {@code @Autowired} → « No default constructor found ») passait la CI et
 * ne se révélait qu'au démarrage réel. Ce test garantit qu'un simple
 * {@code mvn test} attrape désormais cette classe de bug.
 *
 * <p>Idiome repris d'api-iot-hub : H2 in-memory MODE=PostgreSQL (profil test,
 * {@code application-test.yml}), Flyway désactivé (schéma Hibernate create-drop),
 * JWT resource server sur issuer factice (décodeur paresseux, non résolu au boot).
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
class CoreContextLoadsTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private BillingProfileService billingProfileService;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    /**
     * {@code BillingProfileService} a un constructeur qui dépend d'un bean
     * {@code Clock} : c'est exactement la classe de bug que ce test de boot
     * réel existe pour attraper (voir javadoc de classe). Une injection par
     * mock ne le verrait jamais — il faut le vrai conteneur Spring.
     */
    @Test
    void leServiceDeFacturationSeCable() {
        assertThat(billingProfileService).isNotNull();
    }
}
