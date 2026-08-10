package com.openlab.qualitos.quality.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpAuditReminderMailerTest {

    @Mock JavaMailSender sender;

    @Test
    void sendsAPlainTextMessageFromTheConfiguredAddress() {
        SmtpAuditReminderMailer mailer = new SmtpAuditReminderMailer(sender, props("qms@exemple.test"));

        mailer.send("pilote@exemple.test", "Audit à préparer : X", "Corps du rappel");

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(sent.capture());
        SimpleMailMessage m = sent.getValue();
        assertThat(m.getFrom()).isEqualTo("qms@exemple.test");
        assertThat(m.getTo()).containsExactly("pilote@exemple.test");
        assertThat(m.getSubject()).isEqualTo("Audit à préparer : X");
        assertThat(m.getText()).isEqualTo("Corps du rappel");
    }

    @Test
    void refusesToStartWithoutASenderAddress() {
        // Échouer au démarrage, pas au premier envoi : un expéditeur manquant se
        // découvrirait sinon un mois plus tard, dans un ordonnanceur muet.
        assertThatThrownBy(() -> new SmtpAuditReminderMailer(sender, props(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("qualitos.mail.from");
        assertThatThrownBy(() -> new SmtpAuditReminderMailer(sender, props("   ")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void letsSmtpFailuresPropagate_soTheCallerCanDecide() {
        SmtpAuditReminderMailer mailer = new SmtpAuditReminderMailer(sender, props("qms@exemple.test"));
        doThrow(new MailSendException("relais injoignable")).when(sender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> mailer.send("a@b.test", "s", "b"))
                .isInstanceOf(MailSendException.class);
    }

    @Test
    void isGatedOffByDefault_likeObjectStorageAndKafka() {
        ConditionalOnProperty gate = SmtpAuditReminderMailer.class.getAnnotation(ConditionalOnProperty.class);
        assertThat(gate).isNotNull();
        assertThat(gate.prefix()).isEqualTo("qualitos.mail");
        assertThat(gate.name()).containsExactly("enabled");
        assertThat(gate.havingValue()).isEqualTo("true");
        // Pas de matchIfMissing : sans la propriété, aucun bean — l'application
        // démarre et le rappel reste interne.
        assertThat(gate.matchIfMissing()).isFalse();
    }

    private AuditMailProperties props(String from) {
        AuditMailProperties p = new AuditMailProperties();
        p.setEnabled(true);
        p.setFrom(from);
        return p;
    }
}
