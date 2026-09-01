package com.openlab.qualitos.core.billing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Ce que cet adaptateur doit garantir, et que rien d'autre ne garantit :
 *
 * <ol>
 *   <li>le jeton de l'éditeur est PROPAGÉ au moteur — sans quoi le journal
 *       chaîné attribuerait l'activation à « api-core » plutôt qu'à la personne
 *       qui a signé (§18.2 règle 5) ;</li>
 *   <li>aucun repli silencieux : refus, panne réseau ou configuration absente
 *       se terminent tous en {@link ModuleActivationFailedException}, seule
 *       façon pour {@link SubscriptionService} d'abandonner l'abonnement.</li>
 * </ol>
 */
@DisplayName("QualityEngineActivationAdapter")
class QualityEngineActivationAdapterTest {

    private static final String BASE_URL = "https://engine.qualitos.test";
    private static final UUID CLIENT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String JETON = "jeton-de-l-editeur";
    private static final String URI_ATTENDUE =
            BASE_URL + "/api/v1/platform/tenants/" + CLIENT + "/modules/controlplan";

    private MockRestServiceServer server;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private QualityEngineActivationAdapter adapter(String baseUrl) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        return new QualityEngineActivationAdapter(baseUrl, builder);
    }

    /** Un contexte de sécurité portant un vrai JWT, comme en production. */
    private void editeurAuthentifie() {
        Jwt jwt = Jwt.withTokenValue(JETON)
                .header("alg", "RS256")
                .subject(UUID.randomUUID().toString())
                .issuedAt(Instant.parse("2026-09-15T10:00:00Z"))
                .expiresAt(Instant.parse("2026-09-15T11:00:00Z"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @Test
    void activerAppelleLaSurfacePlateformeAvecLeJetonDeLEditeur() {
        // Le coeur du dispositif : c'est le jeton de la PERSONNE qui souscrit
        // qui voyage, pas un jeton de service au nom d'api-core. Sans cela, le
        // journal chaine du moteur nommerait un service la ou il doit nommer un
        // signataire.
        QualityEngineActivationAdapter adapter = adapter(BASE_URL);
        editeurAuthentifie();
        server.expect(requestTo(URI_ATTENDUE))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + JETON))
                .andRespond(withStatus(HttpStatus.CREATED));

        adapter.activate(CLIENT, "controlplan");

        server.verify();
    }

    @Test
    void desactiverEnvoieUnDeleteSurLaMemeRessource() {
        QualityEngineActivationAdapter adapter = adapter(BASE_URL);
        editeurAuthentifie();
        server.expect(requestTo(URI_ATTENDUE))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("Authorization", "Bearer " + JETON))
                .andRespond(withSuccess());

        adapter.deactivate(CLIENT, "controlplan");

        server.verify();
    }

    @Test
    void unRefusDuMoteurNEstPasAvale() {
        // 409 « Missing dependency » : le module ne s'ouvre pas, donc
        // l'abonnement ne doit pas s'enregistrer. Rendre la main sans rien dire
        // ferait facturer un module ferme.
        QualityEngineActivationAdapter adapter = adapter(BASE_URL);
        editeurAuthentifie();
        server.expect(requestTo(URI_ATTENDUE))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> adapter.activate(CLIENT, "controlplan"))
                .isInstanceOf(ModuleActivationFailedException.class)
                .hasMessageContaining("controlplan")
                .hasMessageContaining(CLIENT.toString());
    }

    @Test
    void unePanneDuMoteurNEstPasAvalee() {
        QualityEngineActivationAdapter adapter = adapter(BASE_URL);
        editeurAuthentifie();
        server.expect(requestTo(URI_ATTENDUE))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> adapter.deactivate(CLIENT, "controlplan"))
                .isInstanceOf(ModuleActivationFailedException.class);
    }

    @Test
    void sansUrlConfigureeOnRefuseAuLieuDeSeTaire() {
        // Fail-closed : un adaptateur silencieux laisserait passer des
        // souscriptions dont aucun module ne serait jamais ouvert.
        QualityEngineActivationAdapter adapter = adapter("   ");
        editeurAuthentifie();

        assertThatThrownBy(() -> adapter.activate(CLIENT, "controlplan"))
                .isInstanceOf(ModuleActivationFailedException.class)
                .hasMessageContaining("base-url");

        server.verify();   // aucun appel n'est parti
    }

    @Test
    void uneUrlNulleEstTraiteeCommeUneUrlAbsente() {
        QualityEngineActivationAdapter adapter = adapter(null);
        editeurAuthentifie();

        assertThatThrownBy(() -> adapter.activate(CLIENT, "controlplan"))
                .isInstanceOf(ModuleActivationFailedException.class)
                .hasMessageContaining("base-url");
    }

    @Test
    void sansJetonJwtOnRefuseAuLieuDAppelerSansAutorisation() {
        // Appeler le moteur sans en-tete produirait un 401, et donc un message
        // d'erreur trompeur : on nomme la vraie cause ici.
        QualityEngineActivationAdapter adapter = adapter(BASE_URL);

        assertThatThrownBy(() -> adapter.activate(CLIENT, "controlplan"))
                .isInstanceOf(ModuleActivationFailedException.class)
                .hasMessageContaining("jeton JWT");

        server.verify();
    }

    @Test
    void unPrincipalNonJwtNEstPasPropageNonPlus() {
        // Compte technique, jeton opaque, banc mal configure : il n'y a rien a
        // propager, et se rabattre sur un appel anonyme masquerait le probleme.
        QualityEngineActivationAdapter adapter = adapter(BASE_URL);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("service-account", "n/a", "ROLE_SUPER_ADMIN"));

        assertThatThrownBy(() -> adapter.deactivate(CLIENT, "controlplan"))
                .isInstanceOf(ModuleActivationFailedException.class)
                .hasMessageContaining("jeton JWT");

        server.verify();
    }

    @Test
    void lUrlDeBaseEstNettoyeeDeSesEspaces() {
        // Une variable d'environnement recopiee a la main finit souvent avec un
        // espace ; concatener tel quel produirait une URI invalide et une erreur
        // sans rapport avec la cause.
        QualityEngineActivationAdapter adapter = adapter("  " + BASE_URL + "  ");
        editeurAuthentifie();
        server.expect(requestTo(URI_ATTENDUE)).andRespond(withStatus(HttpStatus.CREATED));

        adapter.activate(CLIENT, "controlplan");

        server.verify();
    }
}
