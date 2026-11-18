package com.openlab.qualitos.core.mail;

import com.openlab.qualitos.core.billing.invoice.PlatformInvoiceMailPort;
import com.openlab.qualitos.core.security.TenantContext;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("D'ou part le courriel d'une facture")
class TenantAwareInvoiceMailerTest {

    private static final UUID TENANT = UUID.randomUUID();

    private final PlatformInvoiceMailPort plateforme = mock(PlatformInvoiceMailPort.class);
    private final TenantMailSettingsService settings = mock(TenantMailSettingsService.class);
    private final MailSenderFactory factory = mock(MailSenderFactory.class);
    private final JavaMailSender envoyeur = mock(JavaMailSender.class);

    private TenantAwareInvoiceMailer mailer;

    @BeforeEach
    void setUp() {
        mailer = new TenantAwareInvoiceMailer(plateforme, settings, factory);
        given(envoyeur.createMimeMessage())
                .willAnswer(i -> new MimeMessage(Session.getInstance(new java.util.Properties())));
        given(factory.creer(any(), any())).willReturn(envoyeur);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private static MailAccount reglage() {
        return new MailAccount("qualite@client.fr", "Qualite Client", "retour@client.fr",
                "smtp.client.fr", 587, MailSecurity.STARTTLS, "u", "v1.a.b", true);
    }

    @Test
    @DisplayName("Sans messagerie reglee, le courriel part de celle de la PLATEFORME")
    void sansReglageLaPlateforme() {
        TenantContext.setTenantId(TENANT.toString());
        given(settings.pourEnvoi(TENANT)).willReturn(Optional.empty());

        mailer.send("client@exemple.fr", "Facture", "corps", new byte[] {1});

        // C'est le comportement d'avant, inchange pour qui n'a rien regle.
        verify(plateforme).send("client@exemple.fr", "Facture", "corps", new byte[] {1});
        verify(factory, never()).creer(any(), any());
    }

    @Test
    @DisplayName("Avec une messagerie reglee, le courriel part de CELLE DU CLIENT")
    void avecReglageCelleDuClient() {
        TenantContext.setTenantId(TENANT.toString());
        given(settings.pourEnvoi(TENANT)).willReturn(Optional.of(reglage()));
        given(settings.motDePasseClair(any())).willReturn("s3cr3t");

        mailer.send("client@exemple.fr", "Facture F-2026-001", "corps", new byte[] {1});

        // Sans ce branchement, le client aurait saisi son serveur, vu
        // « verifiee », et ses factures seraient quand meme parties de
        // l'adresse de la plateforme.
        verify(envoyeur).send(any(MimeMessage.class));
        verify(plateforme, never()).send(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Le mot de passe DECHIFFRE est celui que l'envoyeur recoit")
    void motDePasseDechiffre() {
        TenantContext.setTenantId(TENANT.toString());
        MailAccount r = reglage();
        given(settings.pourEnvoi(TENANT)).willReturn(Optional.of(r));
        given(settings.motDePasseClair(r)).willReturn("s3cr3t");

        mailer.send("client@exemple.fr", "Facture", "corps", new byte[] {1});

        verify(factory).creer(r, "s3cr3t");
    }

    @Test
    @DisplayName("Un echec de la messagerie du client NE SE RABAT PAS sur la plateforme")
    void echecNeSeRabatPas() {
        TenantContext.setTenantId(TENANT.toString());
        given(settings.pourEnvoi(TENANT)).willReturn(Optional.of(reglage()));
        given(settings.motDePasseClair(any())).willReturn("s3cr3t");
        org.mockito.BDDMockito.willThrow(new org.springframework.mail.MailSendException("refuse"))
                .given(envoyeur).send(any(MimeMessage.class));

        // Se rabattre ferait partir le message d'une adresse que le client n'a
        // pas choisie, sous l'identite de QualitOS, pour un courrier qui
        // l'engage. L'echec remonte, et la facture reste renvoyable.
        assertThatThrownBy(() -> mailer.send("client@exemple.fr", "Facture", "corps", new byte[] {1}))
                .isInstanceOf(org.springframework.mail.MailSendException.class);
        verify(plateforme, never()).send(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Hors contexte de client, on ne DEVINE pas : la plateforme envoie")
    void horsContexte() {
        TenantContext.clear();

        mailer.send("client@exemple.fr", "Facture", "corps", new byte[] {1});

        // Un traitement planifie a l'echelle de la plateforme n'a pas de
        // client ; deviner lequel serait pire que de n'en avoir aucun.
        verify(plateforme).send("client@exemple.fr", "Facture", "corps", new byte[] {1});
    }

    @Test
    @DisplayName("Un contexte de client illisible ne fait pas echouer l'envoi")
    void contexteIllisible() {
        TenantContext.setTenantId("pas-un-uuid");

        mailer.send("client@exemple.fr", "Facture", "corps", new byte[] {1});

        verify(plateforme).send("client@exemple.fr", "Facture", "corps", new byte[] {1});
    }

    @Test
    @DisplayName("Le sujet est ramene sur UNE ligne : un saut de ligne injecte des en-tetes")
    void sujetSurUneLigne() throws Exception {
        TenantContext.setTenantId(TENANT.toString());
        given(settings.pourEnvoi(TENANT)).willReturn(Optional.of(reglage()));
        given(settings.motDePasseClair(any())).willReturn("s3cr3t");

        org.mockito.ArgumentCaptor<MimeMessage> capture =
                org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        mailer.send("client@exemple.fr", "Facture\r\nBcc: espion@ailleurs.fr", "corps",
                new byte[] {1});
        verify(envoyeur).send(capture.capture());

        assertThat(capture.getValue().getSubject()).doesNotContain("\n").doesNotContain("\r");
        assertThat(capture.getValue().getAllRecipients()).hasSize(1);
    }

    @Test
    @DisplayName("L'adresse de reponse du client est posee quand il en a une")
    void adresseDeReponse() throws Exception {
        TenantContext.setTenantId(TENANT.toString());
        given(settings.pourEnvoi(TENANT)).willReturn(Optional.of(reglage()));
        given(settings.motDePasseClair(any())).willReturn("s3cr3t");

        org.mockito.ArgumentCaptor<MimeMessage> capture =
                org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        mailer.send("client@exemple.fr", "Facture", "corps", new byte[] {1});
        verify(envoyeur).send(capture.capture());

        // On emet depuis « qualite@ » mais l'on veut les reponses dans une
        // boite partagee : les confondre perd les reponses des clients.
        assertThat(capture.getValue().getReplyTo()[0].toString()).contains("retour@client.fr");
        assertThat(capture.getValue().getFrom()[0].toString()).contains("qualite@client.fr");
    }
}
