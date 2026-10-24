package com.openlab.qualitos.core.directory;

import com.openlab.qualitos.core.common.GlobalExceptionHandler;
import com.openlab.qualitos.core.common.MissingTenantContextException;
import com.openlab.qualitos.core.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DirectoryController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("DirectoryController — annuaire en lecture restreinte (ADR 0091)")
class DirectoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DirectoryService directory;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID KC_ALICE = UUID.randomUUID();

    private static JwtRequestPostProcessor plainUser() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private static DirectoryDto.Page onePage() {
        return new DirectoryDto.Page(
                List.of(new DirectoryDto.Entry(ALICE, KC_ALICE.toString(), "Alice Durand", "alice@acme.com", true)), 0, 20, 1, 1);
    }

    @Test
    @DisplayName("un utilisateur sans rôle d'administration lit l'annuaire")
    void plainUserCanSearch() throws Exception {
        given(directory.search(any())).willReturn(onePage());

        mockMvc.perform(get("/api/v1/directory/users").param("q", "ali").with(plainUser()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.content[0].id").value(ALICE.toString()))
                .andExpect(jsonPath("$.content[0].displayName").value("Alice Durand"))
                .andExpect(jsonPath("$.content[0].email").value("alice@acme.com"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("ne rend que les champs restreints : ni rôles, ni client, ni compte, ni dates")
    void exposesOnlyRestrictedFields() throws Exception {
        given(directory.search(any())).willReturn(onePage());

        mockMvc.perform(get("/api/v1/directory/users").with(plainUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].roles").doesNotExist())
                .andExpect(jsonPath("$.content[0].tenantId").doesNotExist())
                .andExpect(jsonPath("$.content[0].keycloakId").doesNotExist())
                .andExpect(jsonPath("$.content[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.content[0].updatedAt").doesNotExist());
    }

    @Test
    @DisplayName("sans paramètres : première page de 20")
    void defaultsToFirstPageOfTwenty() throws Exception {
        given(directory.search(any())).willReturn(onePage());

        mockMvc.perform(get("/api/v1/directory/users").with(plainUser())).andExpect(status().isOk());

        ArgumentCaptor<DirectoryQuery> query = ArgumentCaptor.forClass(DirectoryQuery.class);
        verify(directory).search(query.capture());
        assertThat(query.getValue()).isEqualTo(new DirectoryQuery("%", 0, 20));
    }

    @Test
    @DisplayName("le terme est normalisé (accents, casse) et ses jokers neutralisés")
    void termIsNormalizedAndEscaped() throws Exception {
        given(directory.search(any())).willReturn(onePage());

        mockMvc.perform(get("/api/v1/directory/users").param("q", "  ÉLO_die%  ").param("size", "50")
                        .with(plainUser()))
                .andExpect(status().isOk());

        ArgumentCaptor<DirectoryQuery> query = ArgumentCaptor.forClass(DirectoryQuery.class);
        verify(directory).search(query.capture());
        assertThat(query.getValue()).isEqualTo(new DirectoryQuery("%elo!_die!%%", 0, 50));
    }

    @ParameterizedTest(name = "page={0} size={1} → 400")
    @CsvSource({"-1,20", "1000,20", "0,0", "0,51", "0,-3"})
    @DisplayName("la pagination est bornée")
    void paginationIsBounded(String page, String size) throws Exception {
        mockMvc.perform(get("/api/v1/directory/users").param("page", page).param("size", size).with(plainUser()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(directory);
    }

    @Test
    @DisplayName("un terme de plus de 100 caractères est refusé, sans être renvoyé dans la réponse")
    void tooLongTermIsRejected() throws Exception {
        String longTerm = "x".repeat(101);
        mockMvc.perform(get("/api/v1/directory/users").param("q", longTerm).with(plainUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(containsString(longTerm))));
        verifyNoInteractions(directory);
    }

    @Test
    @DisplayName("un terme avec caractère de contrôle est refusé")
    void controlCharacterIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/directory/users").param("q", "al\u0000ice").with(plainUser()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(directory);
    }

    @Test
    @DisplayName("une page non numérique est refusée en 400")
    void nonNumericPageIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/directory/users").param("page", "abc").with(plainUser()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("sans jeton : 401")
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/directory/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/directory/users/{id}", ALICE)).andExpect(status().isUnauthorized());
        verifyNoInteractions(directory);
    }

    @Test
    @DisplayName("l'annuaire est en lecture seule : toute écriture est refusée")
    void writesAreDenied() throws Exception {
        mockMvc.perform(post("/api/v1/directory/users").with(plainUser())).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/directory/users/{id}", ALICE).with(plainUser()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(directory);
    }

    @Test
    @DisplayName("lit le nom d'une personne par son identifiant")
    void getsOneEntry() throws Exception {
        given(directory.get(ALICE)).willReturn(new DirectoryDto.Entry(ALICE, KC_ALICE.toString(), "Alice Durand", "alice@acme.com", false));

        mockMvc.perform(get("/api/v1/directory/users/{id}", ALICE).with(plainUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Alice Durand"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.roles").doesNotExist());
    }

    @Test
    @DisplayName("absente du client (inexistante ou d'un autre client) : 404 au message neutre")
    void unknownOrForeignEntryIs404() throws Exception {
        given(directory.get(ALICE)).willThrow(new DirectoryEntryNotFoundException());

        mockMvc.perform(get("/api/v1/directory/users/{id}", ALICE).with(plainUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Directory entry not found"));
    }

    @Test
    @DisplayName("un identifiant mal formé : 400")
    void malformedIdIs400() throws Exception {
        mockMvc.perform(get("/api/v1/directory/users/{id}", "pas-un-uuid").with(plainUser()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(directory);
    }

    @Test
    @DisplayName("la recherche rend le compte de connexion, identité de référence du moteur qualité")
    void searchExposesAccountId() throws Exception {
        given(directory.search(any())).willReturn(onePage());

        mockMvc.perform(get("/api/v1/directory/users").with(plainUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].accountId").value(KC_ALICE.toString()));
    }

    @Test
    @DisplayName("lit une personne par son compte de connexion, ancien membre compris")
    void getsByAccount() throws Exception {
        given(directory.getByAccount(KC_ALICE)).willReturn(
                new DirectoryDto.Entry(ALICE, KC_ALICE.toString(), "Alice Durand", "alice@acme.com", false));

        mockMvc.perform(get("/api/v1/directory/accounts/{accountId}", KC_ALICE).with(plainUser()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.accountId").value(KC_ALICE.toString()))
                .andExpect(jsonPath("$.displayName").value("Alice Durand"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.roles").doesNotExist())
                .andExpect(jsonPath("$.keycloakId").doesNotExist());
    }

    @Test
    @DisplayName("compte absent du client : même 404 neutre")
    void unknownAccountIs404() throws Exception {
        given(directory.getByAccount(KC_ALICE)).willThrow(new DirectoryEntryNotFoundException());

        mockMvc.perform(get("/api/v1/directory/accounts/{accountId}", KC_ALICE).with(plainUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Directory entry not found"));
    }

    @Test
    @DisplayName("compte mal formé : 400 ; sans jeton : 401 ; écriture : 403")
    void accountEndpointGuards() throws Exception {
        mockMvc.perform(get("/api/v1/directory/accounts/{accountId}", "pas-un-uuid").with(plainUser()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/directory/accounts/{accountId}", KC_ALICE))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/directory/accounts/{accountId}", KC_ALICE).with(plainUser()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(directory);
    }

    @Test
    @DisplayName("un jeton sans client : 401")
    void tokenWithoutTenantIs401() throws Exception {
        given(directory.search(any())).willThrow(new MissingTenantContextException());

        mockMvc.perform(get("/api/v1/directory/users").with(plainUser()))
                .andExpect(status().isUnauthorized());
    }
}
