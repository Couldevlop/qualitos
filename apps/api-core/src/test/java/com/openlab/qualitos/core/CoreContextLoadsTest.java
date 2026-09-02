package com.openlab.qualitos.core;

import com.openlab.qualitos.core.billing.BillingProfileService;
import com.openlab.qualitos.core.billing.ModuleActivationPort;
import com.openlab.qualitos.core.billing.SubscriptionService;
import com.openlab.qualitos.core.billing.invoice.InvoiceMailPort;
import com.openlab.qualitos.core.billing.invoice.InvoiceService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private ModuleActivationPort moduleActivationPort;

    @Autowired
    private InvoiceMailPort invoiceMailPort;

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

    /**
     * Toute la chaine de facturation se cable : abonnements, factures, et les
     * deux ports sortants.
     *
     * <p>{@code InvoiceService} depend de CINQ beans, dont deux ports dont
     * l'implementation est conditionnelle. C'est exactement la ou un contexte
     * casse sans qu'aucun banc Mockito ne s'en apercoive — et c'est ce qui
     * s'est produit : une {@code @Configuration} nommee comme sa methode
     * {@code @Bean} rendait le demarrage impossible, sans qu'aucun des 260
     * autres bancs ne bronche.
     */
    @Test
    void laChaineDeFacturationSeCableEntierement() {
        assertThat(subscriptionService).isNotNull();
        assertThat(invoiceService).isNotNull();
        assertThat(moduleActivationPort).isNotNull();
    }

    /**
     * Sans {@code qualitos.mail.enabled}, c'est le repli qui est cable — et il
     * REFUSE l'envoi au lieu de le simuler.
     *
     * <p>Un repli silencieux marquerait la facture « envoyee » alors qu'elle
     * n'est jamais partie : le client ne la recevrait pas, la plateforme
     * affirmerait le contraire, et l'ecart ne se verrait qu'a la relance.
     */
    @Test
    void sansSmtpLeRepliRefuseLEnvoiAuLieuDeLeSimuler() {
        assertThat(invoiceMailPort).isNotNull();
        assertThatThrownBy(() -> invoiceMailPort.send(
                "compta@acme.example", "Facture", "Bonjour", new byte[0]))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("qualitos.mail.enabled");
    }
}
