package com.openlab.qualitos.quality.derogation.infrastructure;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Les bornes du client HTTP vers api-core.
 *
 * <p>Un api-core qui ACCEPTE la connexion puis n'acquitte jamais est le cas qui
 * a bloqué la demande de validation en préproduction : sans borne de lecture, le
 * fil de la requête était retenu jusqu'à ce que la passerelle rende un 504 au
 * bout d'une minute. L'écran attendait, puis échouait sans rien dire d'utile, et
 * le dossier restait dans son état précédent.
 *
 * <p>On éprouve donc le comportement, et non la présence d'une propriété : un
 * serveur local qui ne répond pas, et l'appel doit rendre la main.
 */
class CoreGatewayTest {

    private HttpServer serveurMuet;
    private String base;
    /** Libère les échanges retenus à la fin du banc, pour ne pas fuir de fil. */
    private final CountDownLatch relache = new CountDownLatch(1);

    @BeforeEach
    void leverUnServeurQuiNAcquittePas() throws IOException {
        serveurMuet = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serveurMuet.createContext("/", echange -> {
            try {
                // Accepté, jamais répondu : exactement l'incident constaté.
                relache.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            echange.close();
        });
        serveurMuet.start();
        base = "http://127.0.0.1:" + serveurMuet.getAddress().getPort();
    }

    @AfterEach
    void coucherLeServeur() {
        relache.countDown();
        serveurMuet.stop(0);
    }

    private CoreGateway passerelle(int lecture, int lectureEnvoi) {
        return new CoreGateway(base, 500, lecture, lectureEnvoi, RestClient.builder());
    }

    @Test
    void uneLectureDAnnuaireRendLaMainAuLieuDeRetenirLeFil() {
        CoreGateway g = passerelle(600, 60_000);
        long debut = System.nanoTime();

        assertThatThrownBy(() -> g.rest().get().uri(g.base() + "/api/v1/directory/accounts/x")
                .retrieve().body(String.class))
                .isInstanceOf(RestClientException.class);

        Duration attendu = Duration.ofNanos(System.nanoTime() - debut);
        // Large, pour ne pas dépendre de la charge de la machine : ce qu'on
        // vérifie est qu'on n'attend PAS la minute de la passerelle.
        assertThat(attendu).isLessThan(Duration.ofSeconds(10));
    }

    /**
     * L'envoi au client n'est pas rejouable : le client recevrait la dérogation
     * deux fois. Sa borne doit donc être plus longue que celle de l'annuaire,
     * le temps qu'api-core mène son relais SMTP à bien.
     */
    @Test
    void lEnvoiAuClientNePartagePasLaBorneBreveDeLAnnuaire() {
        CoreGateway g = passerelle(600, 60_000);

        assertThat(g.restEnvoi()).isNotSameAs(g.rest());
    }

    @Test
    void sansAdresseDeBaseOnLeDitAuLieuDeRendreLaMainCommeSiToutAllaitBien() {
        CoreGateway g = new CoreGateway("  ", 500, 600, 60_000, RestClient.builder());

        assertThat(g.estConfigure()).isFalse();
        assertThatThrownBy(g::base)
                .hasMessageContaining("qualitos.core.base-url");
    }

    @Test
    void uneAdresseDeBaseSeNormaliseSansBarreFinale() {
        CoreGateway g = new CoreGateway("http://api-core:8080///", 500, 600, 60_000,
                RestClient.builder());

        assertThat(g.base()).isEqualTo("http://api-core:8080");
        assertThat(g.estConfigure()).isTrue();
    }
}
