package com.openlab.qualitos.core.migration;

import com.openlab.qualitos.core.identity.IdentityProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * api-core ENTIER — chaîne de sécurité, filtre Hibernate par client,
 * contraintes, provision à la première connexion — sur un VRAI PostgreSQL dont
 * le schéma vient des migrations Flyway, pas d'une recréation Hibernate.
 *
 * <p><b>Pourquoi ce banc.</b> En préproduction, l'annuaire et la liste des
 * membres répondaient 500 (« operator does not exist: uuid = character
 * varying ») : le filtre par client liait son paramètre en VARCHAR face à une
 * colonne uuid. Et l'invitation d'un membre échouait sur
 * {@code chk_app_user_roles_role}. Toute la suite était verte : elle tournait
 * sur H2, qui convertit implicitement et ne porte pas les contraintes CHECK
 * des migrations. Ce banc passe par les mêmes requêtes HTTP que l'écran.
 *
 * <p>La base est jointe avec le rôle PROPRIÉTAIRE des tables, comme api-core en
 * préproduction : les politiques RLS d'api-core ne s'appliquent pas à ce rôle
 * (ENABLE sans FORCE), l'isolation entre clients repose ici sur le filtre
 * Hibernate et le client explicite des requêtes — ce que le banc vérifie.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("migration")
class DirectoryAndUsersOnPostgresTest {

    private static final Map<String, Jwt> TOKENS = new ConcurrentHashMap<>();
    private static Connection connection;

    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean IdentityProvider identity;

    @BeforeAll
    static void docker() throws SQLException {
        org.junit.jupiter.api.Assumptions.assumeTrue(MigrationPostgres.dockerAvailable(),
                "Docker indisponible : l'annuaire et les membres restent non verifies sur PostgreSQL ici");
        connection = MigrationPostgres.connect();
    }

    @AfterAll
    static void close() throws SQLException {
        if (connection != null) connection.close();
    }

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MigrationPostgres::jdbcUrl);
        r.add("spring.datasource.username", MigrationPostgres::username);
        r.add("spring.datasource.password", MigrationPostgres::password);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        // Le schéma est celui des migrations, déjà rejouées sur le serveur partagé.
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        r.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        r.add("spring.flyway.enabled", () -> "false");
    }

    // ---------- jetons ----------

    private String token(UUID sub, UUID tenant, String email, String name, String... roles) {
        String value = "t-" + UUID.randomUUID();
        register(value, sub, tenant, email, name, roles);
        return value;
    }

    private void register(String value, UUID sub, UUID tenant, String email, String name, String... roles) {
        Jwt.Builder b = Jwt.withTokenValue(value).header("alg", "RS256").subject(sub.toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600))
                .claim("email", email).claim("realm_access", Map.of("roles", List.of(roles)));
        if (tenant != null) b.claim("tenant_id", tenant.toString());
        if (name != null) b.claim("name", name);
        TOKENS.put(value, b.build());
        given(jwtDecoder.decode(anyString())).willAnswer(inv -> TOKENS.get(inv.<String>getArgument(0)));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    // ---------- données ----------

    private static UUID tenant(String slug) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement st = connection.prepareStatement(
                "INSERT INTO tenants (id, slug, name) VALUES (?, ?, 'Client de test')")) {
            st.setObject(1, id);
            st.setString(2, slug + "-" + id.toString().substring(0, 8));
            st.executeUpdate();
        }
        return id;
    }

    private static UUID member(UUID tenant, String email, String name, String role) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement st = connection.prepareStatement(
                "INSERT INTO app_users (id, tenant_id, keycloak_id, email, display_name, search_key)"
                        + " VALUES (?, ?, ?, ?, ?, ?)")) {
            st.setObject(1, id);
            st.setObject(2, tenant);
            st.setString(3, UUID.randomUUID().toString());
            st.setString(4, email);
            st.setString(5, name);
            st.setString(6, (name.toLowerCase() + " " + email).strip());
            st.executeUpdate();
        }
        try (PreparedStatement st = connection.prepareStatement(
                "INSERT INTO app_user_roles (user_id, role) VALUES (?, ?)")) {
            st.setObject(1, id);
            st.setString(2, role);
            st.executeUpdate();
        }
        return id;
    }

    private static List<Map<String, Object>> rowsFor(String accountId) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT tenant_id, email, display_name, active FROM app_users WHERE keycloak_id = ?")) {
            st.setString(1, accountId);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    rows.add(Map.of("tenant", rs.getObject(1), "email", rs.getString(2),
                            "name", String.valueOf(rs.getString(3)), "active", rs.getBoolean(4)));
                }
            }
        }
        return rows;
    }

    private static Set<String> rolesOf(String accountId) throws SQLException {
        Set<String> roles = new TreeSet<>();
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT r.role FROM app_user_roles r JOIN app_users u ON u.id = r.user_id WHERE u.keycloak_id = ?")) {
            st.setString(1, accountId);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) roles.add(rs.getString(1));
            }
        }
        return roles;
    }

    // ---------- 1. le filtre par client sur PostgreSQL ----------

    @Test
    void lAnnuaireRepondEtNeMontreQueLesMembresDuClient() throws Exception {
        UUID acme = tenant("acme");
        UUID globex = tenant("globex");
        member(acme, "alice@acme.example", "Alice Durand", "user");
        member(globex, "bob@globex.example", "Bob Globex", "user");
        String jeton = token(UUID.randomUUID(), acme, "lecteur@acme.example", "Lecteur Acme", "user");

        mvc.perform(get("/api/v1/directory/users").header("Authorization", bearer(jeton)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].email").value(hasItem("alice@acme.example")))
                .andExpect(jsonPath("$.content[*].email").value(not(hasItem("bob@globex.example"))));
    }

    @Test
    void laListeDesMembresRepondEtResteDansLeClient() throws Exception {
        UUID acme = tenant("acme-liste");
        UUID globex = tenant("globex-liste");
        member(acme, "carla@acme.example", "Carla", "quality_manager");
        member(globex, "dan@globex.example", "Dan", "user");
        String admin = token(UUID.randomUUID(), acme, "admin@acme.example", "Admin Acme", "admin_tenant");

        mvc.perform(get("/api/v1/users").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].email").value(hasItem("carla@acme.example")))
                .andExpect(jsonPath("$.content[*].email").value(not(hasItem("dan@globex.example"))))
                .andExpect(jsonPath("$.content[*].tenantId").value(not(hasItem(globex.toString()))));
    }

    // ---------- 2. l'invitation et la création écrivent la forme du realm ----------

    @Test
    void lInvitationEnregistreLeMembreAvecLesRolesDuRealm() throws Exception {
        UUID acme = tenant("acme-invite");
        String admin = token(UUID.randomUUID(), acme, "admin@acme-invite.example", "Admin", "admin_tenant");
        String compte = UUID.randomUUID().toString();
        given(identity.create(any())).willReturn(new IdentityProvider.CreatedAccount(compte, "Provisoire2026x", false));

        mvc.perform(post("/api/v1/users/invite").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nina@acme.example\",\"firstName\":\"Nina\",\"lastName\":\"Petit\","
                                + "\"roles\":[\"ADMIN_TENANT\",\"quality_manager\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value("nina@acme.example"));

        assertThat(rolesOf(compte)).containsExactly("admin_tenant", "quality_manager");
    }

    @Test
    void laCreationDirecteNormaliseLesRoles() throws Exception {
        UUID acme = tenant("acme-create");
        String admin = token(UUID.randomUUID(), acme, "admin@acme-create.example", "Admin", "admin_tenant");
        String compte = UUID.randomUUID().toString();

        mvc.perform(post("/api/v1/users").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keycloakId\":\"" + compte + "\",\"email\":\"omar@acme.example\","
                                + "\"roles\":[\"QUALITY_MANAGER\",\"DIRECTOR_QUALITY\"]}"))
                .andExpect(status().isCreated());

        assertThat(rolesOf(compte)).containsExactly("quality_director", "quality_manager");
    }

    // ---------- 3. la provision à la première connexion ----------

    @Test
    void laPremiereConnexionInscritLeMembreEtKeycloakFaitFoi() throws Exception {
        UUID acme = tenant("acme-jit");
        UUID sub = UUID.randomUUID();
        String premier = token(sub, acme, "paul@acme.example", "Paul Martin", "user", "offline_access",
                "default-roles-qualitos");

        mvc.perform(get("/api/v1/directory/users").param("q", "paul").header("Authorization", bearer(premier)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].displayName").value("Paul Martin"));
        assertThat(rowsFor(sub.toString())).singleElement().satisfies(r -> {
            assertThat(r.get("tenant")).isEqualTo(acme);
            assertThat(r.get("email")).isEqualTo("paul@acme.example");
            assertThat(r.get("active")).isEqualTo(true);
        });
        assertThat(rolesOf(sub.toString())).containsExactly("user");

        // Renommé et promu dans Keycloak : l'annuaire suit.
        String ensuite = token(sub, acme, "paul.martin@acme.example", "Paul Martin-Durand", "quality_manager");
        mvc.perform(get("/api/v1/directory/users").header("Authorization", bearer(ensuite)))
                .andExpect(status().isOk());
        assertThat(rowsFor(sub.toString())).singleElement().satisfies(r -> {
            assertThat(r.get("email")).isEqualTo("paul.martin@acme.example");
            assertThat(r.get("name")).isEqualTo("Paul Martin-Durand");
        });
        assertThat(rolesOf(sub.toString())).containsExactly("quality_manager");
        // Aucune écriture vers Keycloak, aucun envoi : l'utilisateur est déjà entré.
        verifyNoInteractions(identity);
    }

    @Test
    void sansLigneClientRienNEstEcritEtLAnnuaireResteVide() throws Exception {
        UUID inconnu = UUID.randomUUID();
        UUID sub = UUID.randomUUID();
        String jeton = token(sub, inconnu, "zoe@inconnu.example", "Zoé", "user");

        mvc.perform(get("/api/v1/directory/users").header("Authorization", bearer(jeton)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        assertThat(rowsFor(sub.toString())).isEmpty();
        try (PreparedStatement st = connection.prepareStatement("SELECT count(*) FROM tenants WHERE id = ?")) {
            st.setObject(1, inconnu);
            try (ResultSet rs = st.executeQuery()) {
                rs.next();
                assertThat(rs.getLong(1)).as("la ligne client n'est jamais créée par la provision").isZero();
            }
        }
    }

    @Test
    void unJetonDeServiceNInscritPersonne() throws Exception {
        UUID acme = tenant("acme-svc");
        UUID sub = UUID.randomUUID();
        String value = "svc-" + UUID.randomUUID();
        TOKENS.put(value, Jwt.withTokenValue(value).header("alg", "RS256").subject(sub.toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600))
                .claim("tenant_id", acme.toString()).claim("client_id", "api-quality-engine-ai")
                .claim("preferred_username", "service-account-api-quality-engine-ai")
                .claim("email", "svc@acme.example").build());
        given(jwtDecoder.decode(anyString())).willAnswer(inv -> TOKENS.get(inv.<String>getArgument(0)));

        mvc.perform(get("/api/v1/directory/users").header("Authorization", bearer(value)))
                .andExpect(status().isOk());
        assertThat(rowsFor(sub.toString())).isEmpty();
    }

    @Test
    void deuxPremieresRequetesSimultaneesNeFontNiDoublonNi500() throws Exception {
        UUID acme = tenant("acme-race");
        UUID sub = UUID.randomUUID();
        String jeton = token(sub, acme, "rita@acme.example", "Rita", "user");
        int n = 8;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch depart = new CountDownLatch(1);
        try {
            List<Future<Integer>> statuts = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                statuts.add(pool.submit(() -> {
                    depart.await();
                    return mvc.perform(get("/api/v1/directory/users").header("Authorization", bearer(jeton)))
                            .andReturn().getResponse().getStatus();
                }));
            }
            depart.countDown();
            for (Future<Integer> f : statuts) {
                assertThat(f.get(60, TimeUnit.SECONDS)).isEqualTo(200);
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(rowsFor(sub.toString())).hasSize(1);
    }
}
