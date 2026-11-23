package com.openlab.qualitos.quality.derogation.web;

import com.openlab.qualitos.quality.derogation.application.DerogationMailException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le refus d'envoi doit etre AFFICHABLE, et pas seulement correct.
 *
 * <p>L'ecran ne lit que {@code userMessage} : {@code detail} peut porter un nom
 * de classe, une contrainte de base ou une trace, et il n'est jamais montre
 * (OWASP A09). Sans cette propriete, le refus le plus actionnable qui soit --
 * « la messagerie de votre organisation n'est pas configuree » -- se lisait
 * « Erreur serveur, reessayez dans un instant », conseil faux et cause perdue.
 */
@DisplayName("DerogationExceptionHandler - le refus de courriel")
class DerogationExceptionHandlerTest {

    private final DerogationExceptionHandler gestionnaire = new DerogationExceptionHandler();

    @Test
    @DisplayName("la raison voyage par le canal que l'ecran lit")
    void laRaisonVoyageParLeCanalQueLEcranLit() {
        String raison = "La messagerie de votre organisation n'est pas configuree :"
                + " reglez-la avant d'envoyer une derogation a un client.";

        ProblemDetail probleme = gestionnaire.courriel(new DerogationMailException(raison));

        assertThat(probleme.getProperties()).containsEntry("userMessage", raison);
    }

    @Test
    @DisplayName("le detail porte la meme phrase, et aucune autre")
    void leDetailPorteLaMemePhrase() {
        String raison = "L'hote de messagerie est refuse.";

        ProblemDetail probleme = gestionnaire.courriel(new DerogationMailException(raison));

        assertThat(probleme.getDetail()).isEqualTo(raison);
    }

    @Test
    @DisplayName("le statut et le type du refus ne changent pas")
    void leStatutEtLeTypeNeChangentPas() {
        ProblemDetail probleme = gestionnaire.courriel(
                new DerogationMailException("Le courriel n'est pas parti."));

        assertThat(probleme.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
        assertThat(probleme.getTitle()).isEqualTo("Derogation Mail Not Sent");
        assertThat(probleme.getType())
                .hasToString("https://qualitos.io/errors/derogation-mail-failed");
    }

    @Test
    @DisplayName("l'horodatage reste pose, comme sur tout refus du moteur")
    void lHorodatageRestePose() {
        ProblemDetail probleme = gestionnaire.courriel(
                new DerogationMailException("Le courriel n'est pas parti."));

        assertThat(probleme.getProperties()).containsKey("timestamp");
    }
}
