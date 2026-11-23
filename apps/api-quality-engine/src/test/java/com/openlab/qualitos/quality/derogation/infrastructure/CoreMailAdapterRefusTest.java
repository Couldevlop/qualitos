package com.openlab.qualitos.quality.derogation.infrastructure;

import com.openlab.qualitos.quality.derogation.application.DerogationMailException;
import com.openlab.qualitos.quality.derogation.application.DerogationMailPort.CustomerMail;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Ce que l'ecran apprend quand api-core refuse d'envoyer.
 *
 * <p>Le refus le plus frequent est aussi le plus actionnable : la messagerie du
 * client n'est pas reglee. Il ne produisait pourtant rien d'autre a l'ecran
 * qu'« Erreur serveur, reessayez dans un instant » -- un conseil faux, puisque
 * reessayer ne regle aucune messagerie, et la perte de la seule phrase qui
 * disait quoi faire. La cause etait ici : l'adaptateur recopiait le corps JSON
 * entier dans son message, et l'ecran refuse par principe d'afficher un detail
 * de serveur.
 *
 * <p>On eprouve donc ce qui SORT de l'adaptateur, et non la forme de l'appel :
 * un api-core local qui refuse, et le message porte par l'exception.
 */
@DisplayName("CoreMailAdapter - ce que dit un refus d'api-core")
class CoreMailAdapterRefusTest {

    private HttpServer serveur;

    /**
     * La passerelle propage le jeton de la requete en cours : sans contexte
     * authentifie, elle refuse AVANT d appeler, et le banc n eprouverait alors
     * que son propre decor.
     */
    @BeforeEach
    void poserUneRequeteAuthentifiee() {
        Jwt jeton = Jwt.withTokenValue("jeton-de-banc")
                .header("alg", "none")
                .claim("sub", UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(jeton));
    }

    @AfterEach
    void fermer() {
        SecurityContextHolder.clearContext();
        if (serveur != null) {
            serveur.stop(0);
        }
    }

    /** Un api-core local qui repond CE corps-la, avec CE statut-la, a tout appel. */
    private CoreMailAdapter adaptateurFaceA(int statut, String corps) throws IOException {
        serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        byte[] octets = corps.getBytes(StandardCharsets.UTF_8);
        serveur.createContext("/", echange -> {
            echange.getResponseHeaders().add("Content-Type", "application/problem+json");
            echange.sendResponseHeaders(statut, octets.length);
            try (OutputStream sortie = echange.getResponseBody()) {
                sortie.write(octets);
            }
        });
        serveur.start();
        String base = "http://127.0.0.1:" + serveur.getAddress().getPort();
        return new CoreMailAdapter(
                new CoreGateway(base, 500, 2_000, 10_000, RestClient.builder()), null, null);
    }

    private static CustomerMail courriel() {
        return new CustomerMail(UUID.randomUUID(), "client@exemple.fr", UUID.randomUUID(),
                "Derogation DER-2026-0001", "Bonjour,", "derogation.pdf",
                new byte[] { 1, 2, 3 });
    }

    @Test
    @DisplayName("la phrase d'api-core arrive telle quelle, sans rien autour")
    void laPhraseDAmontArriveTelleQuelle() throws IOException {
        String attendue = "La messagerie de votre organisation n'est pas configuree :"
                + " reglez-la avant d'envoyer une derogation a un client.";
        CoreMailAdapter adaptateur = adaptateurFaceA(409,
                "{\"status\":409,\"title\":\"Conflict\",\"detail\":\"" + attendue + "\","
                        + "\"instance\":\"/api/v1/mail/derogation\"}");

        assertThatThrownBy(() -> adaptateur.sendToCustomer(courriel()))
                .isInstanceOf(DerogationMailException.class)
                .hasMessage(attendue);
    }

    @Test
    @DisplayName("le corps brut ne voyage pas : ni chemin interne, ni champ d'amont")
    void leCorpsBrutNeVoyagePas() throws IOException {
        CoreMailAdapter adaptateur = adaptateurFaceA(409,
                "{\"status\":409,\"detail\":\"L'hote de messagerie est refuse.\","
                        + "\"instance\":\"/api/v1/mail/derogation\",\"trace\":\"secret\"}");

        assertThatThrownBy(() -> adaptateur.sendToCustomer(courriel()))
                .isInstanceOf(DerogationMailException.class)
                .hasMessage("L'hote de messagerie est refuse.")
                .hasMessageNotContaining("instance")
                .hasMessageNotContaining("secret");
    }

    @Test
    @DisplayName("un amont muet retombe sur une phrase generique, pas sur du vide")
    void unAmontMuetRetombeSurLeGenerique() throws IOException {
        CoreMailAdapter adaptateur = adaptateurFaceA(500, "{\"status\":500}");

        assertThatThrownBy(() -> adaptateur.sendToCustomer(courriel()))
                .isInstanceOf(DerogationMailException.class)
                .hasMessageContaining("Le courriel n'est pas parti")
                .hasMessageContaining("reste renvoyable");
    }

    @Test
    @DisplayName("un corps illisible ne fait pas echouer le refus lui-meme")
    void unCorpsIllisibleRetombeSurLeGenerique() throws IOException {
        CoreMailAdapter adaptateur = adaptateurFaceA(502, "<html>passerelle</html>");

        assertThatThrownBy(() -> adaptateur.sendToCustomer(courriel()))
                .isInstanceOf(DerogationMailException.class)
                .hasMessageContaining("Le courriel n'est pas parti");
    }

    @Test
    @DisplayName("une trace technique est ecartee : elle n'a rien a faire a l'ecran")
    void uneTraceTechniqueEstEcartee() throws IOException {
        CoreMailAdapter adaptateur = adaptateurFaceA(500,
                "{\"detail\":\"Echec at com.openlab.Truc.machin(Truc.java:42)\"}");

        assertThatThrownBy(() -> adaptateur.sendToCustomer(courriel()))
                .isInstanceOf(DerogationMailException.class)
                .hasMessageNotContaining("Truc.java")
                .hasMessageContaining("Le courriel n'est pas parti");
    }

    @Test
    @DisplayName("un message sur deux lignes est ecarte : l'ecran l'ecarterait aussi")
    void unMessageSurDeuxLignesEstEcarte() throws IOException {
        CoreMailAdapter adaptateur = adaptateurFaceA(500,
                "{\"detail\":\"Premiere ligne.\\nSeconde ligne.\"}");

        assertThatThrownBy(() -> adaptateur.sendToCustomer(courriel()))
                .isInstanceOf(DerogationMailException.class)
                .hasMessageNotContaining("Seconde ligne")
                .hasMessageContaining("Le courriel n'est pas parti");
    }

    @Test
    @DisplayName("une phrase trop longue est ecartee : la meme borne qu'a l'ecran")
    void unePhraseTropLongueEstEcartee() throws IOException {
        String fleuve = "a".repeat(301);
        CoreMailAdapter adaptateur = adaptateurFaceA(409, "{\"detail\":\"" + fleuve + "\"}");

        assertThatThrownBy(() -> adaptateur.sendToCustomer(courriel()))
                .isInstanceOf(DerogationMailException.class)
                .hasMessageNotContaining(fleuve)
                .hasMessageContaining("Le courriel n'est pas parti");
    }

    @Test
    @DisplayName("une phrase de 300 signes passe : la borne est inclusive")
    void unePhraseALaBornePasse() throws IOException {
        String juste = "b".repeat(300);
        CoreMailAdapter adaptateur = adaptateurFaceA(409, "{\"detail\":\"" + juste + "\"}");

        assertThatThrownBy(() -> adaptateur.sendToCustomer(courriel()))
                .isInstanceOf(DerogationMailException.class)
                .hasMessage(juste);
    }

    @Test
    @DisplayName("un detail qui n'est pas du texte retombe sur le generique")
    void unDetailQuiNEstPasDuTexteRetombeSurLeGenerique() throws IOException {
        CoreMailAdapter adaptateur = adaptateurFaceA(409, "{\"detail\":{\"code\":12}}");

        assertThatThrownBy(() -> adaptateur.sendToCustomer(courriel()))
                .isInstanceOf(DerogationMailException.class)
                .hasMessageContaining("Le courriel n'est pas parti");
    }

    @Test
    @DisplayName("la phrase generique ne promet pas un nouvel essai immediat")
    void laPhraseGeneriqueNePrometPasUnNouvelEssai() throws IOException {
        CoreMailAdapter adaptateur = adaptateurFaceA(503, "{\"status\":503}");

        Throwable leve = catchThrowable(() -> adaptateur.sendToCustomer(courriel()));

        assertThat(leve).isInstanceOf(DerogationMailException.class);
        assertThat(leve.getMessage()).doesNotContain("reessayez dans un instant");
    }

    @Test
    @DisplayName("l'adresse est verifiee avant tout appel sortant")
    void lAdresseEstVerifieeAvantToutAppel() {
        CoreMailAdapter adaptateur = new CoreMailAdapter(
                new CoreGateway("http://inatteignable.invalid", 100, 100, 100,
                        RestClient.builder()), null, null);

        assertThatThrownBy(() -> adaptateur.sendToCustomer(
                new CustomerMail(UUID.randomUUID(), "a@b.fr\nbcc: ailleurs@exemple.fr",
                        UUID.randomUUID(), "s", "c", "d.pdf", new byte[] { 1 })))
                .isInstanceOf(DerogationMailException.class)
                .hasMessageContaining("saut de ligne");
    }
}
