package com.openlab.qualitos.core.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KeycloakIdentityProviderTest {

    static final String KC = "http://keycloak:8080/auth";
    static final String ADMIN = KC + "/admin/realms/qualitos";
    static final UUID TENANT = UUID.randomUUID();
    static final String TOKEN_JSON = "{\"access_token\":\"tok\",\"expires_in\":300}";

    MockRestServiceServer server;
    KeycloakIdentityProvider provider;

    KeycloakIdentityProvider build(String mode) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        return new KeycloakIdentityProvider(new IdentityProperties(KC + "/", "qualitos", null, "s3cret", mode),
                builder, Clock.fixed(Instant.parse("2026-10-08T09:00:00Z"), ZoneOffset.UTC));
    }

    @BeforeEach
    void setUp() {
        provider = build(null);
    }

    void jeton() {
        server.expect(once(), requestTo(KC + "/realms/qualitos/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=client_credentials")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("client_id=qualitos-provisioner")))
                .andRespond(withSuccess(TOKEN_JSON, MediaType.APPLICATION_JSON));
    }

    @Test
    void creeLeCompteRattacheAuClientAvecSesRolesEtUnMotDePasseProvisoire() {
        jeton();
        server.expect(requestTo(ADMIN + "/users")).andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer tok"))
                .andExpect(jsonPath("$.email").value("marie@acme.fr"))
                .andExpect(jsonPath("$.attributes.tenant_id[0]").value(TENANT.toString()))
                .andExpect(jsonPath("$.requiredActions[0]").value("UPDATE_PASSWORD"))
                .andRespond(withCreatedEntity(URI.create(ADMIN + "/users/kc-1")));
        server.expect(requestTo(ADMIN + "/users/kc-1/role-mappings/realm")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"id\":\"r0\",\"name\":\"default-roles-qualitos\"}]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(ADMIN + "/users/kc-1/role-mappings/realm/available"))
                .andRespond(withSuccess("[{\"id\":\"r1\",\"name\":\"quality_manager\"}]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(ADMIN + "/users/kc-1/role-mappings/realm")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$[0].name").value("quality_manager"))
                .andRespond(withSuccess());
        server.expect(requestTo(ADMIN + "/users/kc-1/reset-password")).andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.temporary").value(true))
                .andRespond(withSuccess());

        IdentityProvider.CreatedAccount compte = provider.create(new IdentityProvider.NewAccount(
                TENANT, "marie@acme.fr", "Marie", "Dupont", Set.of("quality_manager")));

        assertThat(compte.accountId()).isEqualTo("kc-1");
        assertThat(compte.invitationSent()).isFalse();
        assertThat(compte.temporaryPassword()).hasSize(16)
                .matches(".*[A-Z].*").matches(".*[a-z].*").matches(".*[2-9].*").doesNotContain("0", "O", "l", "1");
        server.verify();
    }

    @Test
    void enModeEmailKeycloakEnvoieLInvitationEtAucunMotDePasseNeSort() {
        provider = build("email");
        jeton();
        server.expect(requestTo(ADMIN + "/users")).andRespond(withCreatedEntity(URI.create(ADMIN + "/users/kc-2")));
        server.expect(requestTo(ADMIN + "/users/kc-2/role-mappings/realm"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(ADMIN + "/users/kc-2/role-mappings/realm/available"))
                .andRespond(withSuccess("[{\"id\":\"r2\",\"name\":\"user\"}]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(ADMIN + "/users/kc-2/role-mappings/realm")).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());
        server.expect(requestTo(ADMIN + "/users/kc-2/execute-actions-email")).andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$[0]").value("UPDATE_PASSWORD"))
                .andRespond(withSuccess());

        IdentityProvider.CreatedAccount compte = provider.create(new IdentityProvider.NewAccount(
                TENANT, "paul@acme.fr", null, null, Set.of("user")));

        assertThat(compte.invitationSent()).isTrue();
        assertThat(compte.temporaryPassword()).isNull();
        server.verify();
    }

    @Test
    void uneAdresseDejaPriseDonneUnConflitEtUnEchecApresCreationSupprimeLeCompte() {
        jeton();
        server.expect(requestTo(ADMIN + "/users")).andRespond(withStatus(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> provider.create(new IdentityProvider.NewAccount(
                TENANT, "marie@acme.fr", null, null, Set.of("user")))).isInstanceOf(AccountAlreadyExistsException.class);
        server.verify();

        provider = build(null);
        jeton();
        server.expect(requestTo(ADMIN + "/users")).andRespond(withCreatedEntity(URI.create(ADMIN + "/users/kc-3")));
        server.expect(requestTo(ADMIN + "/users/kc-3/role-mappings/realm"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));
        server.expect(requestTo(ADMIN + "/users/kc-3")).andExpect(method(HttpMethod.DELETE)).andRespond(withSuccess());
        assertThatThrownBy(() -> provider.create(new IdentityProvider.NewAccount(
                TENANT, "x@acme.fr", null, null, Set.of("user")))).isInstanceOf(IdentityProviderException.class);
        server.verify();
    }

    @Test
    void reglerLesRolesNeTouchePasAuxRolesQueQualitosNAdministrePas() {
        jeton();
        server.expect(requestTo(ADMIN + "/users/kc-1/role-mappings/realm")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"id\":\"r0\",\"name\":\"offline_access\"},{\"id\":\"r1\",\"name\":\"user\"},"
                        + "{\"id\":\"r9\",\"name\":\"super_admin\"}]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(ADMIN + "/users/kc-1/role-mappings/realm/available"))
                .andRespond(withSuccess("[{\"id\":\"r3\",\"name\":\"auditor\"}]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(ADMIN + "/users/kc-1/role-mappings/realm")).andExpect(method(HttpMethod.DELETE))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("user"))
                .andRespond(withSuccess());
        server.expect(requestTo(ADMIN + "/users/kc-1/role-mappings/realm")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$[0].name").value("auditor"))
                .andRespond(withSuccess());

        provider.setRoles("kc-1", Set.of("auditor"));
        server.verify();
    }

    @Test
    void unRoleQueLeRealmNeProposePasEstUneErreurFranche() {
        jeton();
        server.expect(requestTo(ADMIN + "/users/kc-1/role-mappings/realm")).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(ADMIN + "/users/kc-1/role-mappings/realm/available"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> provider.setRoles("kc-1", Set.of("auditor")))
                .isInstanceOf(IdentityProviderException.class).hasMessageContaining("auditor");
        server.verify();
    }

    @Test
    void activerDesactiverLireLeClientEtSupprimer() {
        jeton();
        server.expect(requestTo(ADMIN + "/users/kc-1")).andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.enabled").value(false)).andRespond(withSuccess());
        server.expect(requestTo(ADMIN + "/users/kc-1")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"attributes\":{\"tenant_id\":[\"" + TENANT + "\"]}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(ADMIN + "/users/kc-2")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"attributes\":{}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(ADMIN + "/users/kc-3")).andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo(ADMIN + "/users/kc-4")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"attributes\":{\"tenant_id\":[\"pas-un-uuid\"]}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(ADMIN + "/users/kc-1")).andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        provider.setEnabled("kc-1", false);
        assertThat(provider.tenantOf("kc-1")).contains(TENANT);
        assertThat(provider.tenantOf("kc-2")).isEmpty();
        assertThat(provider.tenantOf("kc-3")).isEmpty();
        assertThat(provider.tenantOf("kc-4")).isEmpty();
        // Une compensation qui échoue est journalisée, pas relancée.
        provider.delete("kc-1");
        server.verify();
    }

    @Test
    void sansSecretRienNePartEtLeJetonSeRecycleJusquASonExpiration() {
        KeycloakIdentityProvider sansSecret = new KeycloakIdentityProvider(
                new IdentityProperties(KC, null, null, " ", null), RestClient.builder(), Clock.systemUTC());
        assertThatThrownBy(() -> sansSecret.setEnabled("kc", true)).isInstanceOf(IdentityProviderException.class)
                .hasMessageContaining("pas configuré");

        jeton();
        assertThat(provider.bearer()).isEqualTo("tok");
        assertThat(provider.bearer()).isEqualTo("tok");
        server.verify();
    }

    @Test
    void unJetonRefuseEstUneErreurFranche() {
        server.expect(requestTo(KC + "/realms/qualitos/protocol/openid-connect/token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> provider.bearer()).isInstanceOf(IdentityProviderException.class);
        provider = build(null);
        server.expect(requestTo(KC + "/realms/qualitos/protocol/openid-connect/token"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> provider.bearer()).isInstanceOf(IdentityProviderException.class);
    }

    @Test
    void lesRolesAttribuablesExcluentLeSuperAdministrateur() {
        assertThat(PlatformRoles.validated(Set.of(" Quality_Manager ", "user"))).containsExactly("quality_manager", "user");
        assertThatThrownBy(() -> PlatformRoles.validated(Set.of("super_admin"))).isInstanceOf(InvalidRoleException.class);
        assertThatThrownBy(() -> PlatformRoles.validated(Set.of())).isInstanceOf(InvalidRoleException.class);
        assertThat(new IdentityProperties(null, null, null, null, null).configured()).isFalse();
        assertThat(new IdentityProperties(KC, null, null, "s", "EMAIL").emailInvitations()).isTrue();
    }
}
