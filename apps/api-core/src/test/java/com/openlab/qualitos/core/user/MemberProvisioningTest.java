package com.openlab.qualitos.core.user;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.openlab.qualitos.core.edition.EditionExceptions;
import com.openlab.qualitos.core.edition.MemberLimit;
import com.openlab.qualitos.core.tenant.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("MemberProvisioning — l'annuaire se remplit à la première connexion (ADR 0093)")
class MemberProvisioningTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID SUB = UUID.randomUUID();

    UserRepository users;
    TenantRepository tenants;
    MemberLimit limit;
    AtomicReference<Instant> now;
    MemberProvisioning provisioning;
    ListAppender<ILoggingEvent> journal;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        tenants = mock(TenantRepository.class);
        limit = mock(MemberLimit.class);
        now = new AtomicReference<>(Instant.parse("2026-10-15T08:00:00Z"));
        Clock clock = new Clock() {
            @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return now.get(); }
        };
        provisioning = new MemberProvisioning(users, tenants, limit, mock(PlatformTransactionManager.class), clock);
        given(tenants.existsById(TENANT)).willReturn(true);
        given(users.saveAndFlush(any())).willAnswer(inv -> inv.getArgument(0));
        journal = new ListAppender<>();
        journal.start();
        ((Logger) LoggerFactory.getLogger(MemberProvisioning.class)).addAppender(journal);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(MemberProvisioning.class)).detachAppender(journal);
    }

    /** Les tests voisins écrivent sur le même logger : on lit une copie prise sous le verrou de l'appender. */
    private java.util.List<ILoggingEvent> logged() {
        synchronized (journal) {
            return java.util.List.copyOf(journal.list);
        }
    }

    static Jwt jwt(Map<String, Object> claims) {
        Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "none");
        claims.forEach(b::claim);
        return b.build();
    }

    static Jwt person(String email, String name, String... roles) {
        return jwt(Map.of("sub", SUB.toString(), "tenant_id", TENANT.toString(), "email", email, "name", name,
                "realm_access", Map.of("roles", List.of(roles))));
    }

    @Test
    void creeLeMembreDepuisLeJeton() {
        given(users.findByKeycloakId(SUB.toString())).willReturn(Optional.empty());

        assertThat(provisioning.provision(person("ana@acme.example", "Ana Lopez", "USER", "offline_access",
                "default-roles-qualitos", "quality_director")))
                .isEqualTo(MemberProvisioning.Outcome.CREATED);

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(users).saveAndFlush(saved.capture());
        AppUser u = saved.getValue();
        assertThat(u.getTenantId()).isEqualTo(TENANT);
        assertThat(u.getKeycloakId()).isEqualTo(SUB.toString());
        assertThat(u.getEmail()).isEqualTo("ana@acme.example");
        assertThat(u.getDisplayName()).isEqualTo("Ana Lopez");
        assertThat(u.getRoles()).containsExactlyInAnyOrder("user", "quality_director");
        assertThat(u.isActive()).isTrue();
    }

    @Test
    void leNomRetombeSurPrenomEtNomPuisSurLIdentifiant() {
        assertThat(MemberProvisioning.displayName(jwt(Map.of("sub", "x", "given_name", "Léa", "family_name", "Roy"))))
                .isEqualTo("Léa Roy");
        assertThat(MemberProvisioning.displayName(jwt(Map.of("sub", "x", "preferred_username", "lroy"))))
                .isEqualTo("lroy");
        assertThat(MemberProvisioning.displayName(jwt(Map.of("sub", "x")))).isNull();
        assertThat(MemberProvisioning.displayName(jwt(Map.of("sub", "x", "name", "x".repeat(250))))).hasSize(200);
    }

    @Test
    void keycloakFaitFoi_leMembreExistantEstMisAJour() {
        AppUser existant = AppUser.builder().id(UUID.randomUUID()).tenantId(TENANT).keycloakId(SUB.toString())
                .email("old@acme.example").displayName("Ancien").roles(new HashSet<>(Set.of("user"))).build();
        given(users.findByKeycloakId(SUB.toString())).willReturn(Optional.of(existant));

        assertThat(provisioning.provision(person("new@acme.example", "Nouveau", "quality_manager")))
                .isEqualTo(MemberProvisioning.Outcome.UPDATED);
        assertThat(existant.getEmail()).isEqualTo("new@acme.example");
        assertThat(existant.getDisplayName()).isEqualTo("Nouveau");
        assertThat(existant.getRoles()).containsExactly("quality_manager");
        verify(users).saveAndFlush(existant);
        verify(limit, never()).ensureRoomForOneMore(any(Long.class));
    }

    @Test
    void rienNAChange_aucuneEcriture_puisLeCacheEvitemmeLaLecture() {
        AppUser existant = AppUser.builder().id(UUID.randomUUID()).tenantId(TENANT).keycloakId(SUB.toString())
                .email("ana@acme.example").displayName("Ana").roles(new HashSet<>(Set.of("user"))).build();
        given(users.findByKeycloakId(SUB.toString())).willReturn(Optional.of(existant));
        Jwt jeton = person("ana@acme.example", "Ana", "user");

        assertThat(provisioning.provision(jeton)).isEqualTo(MemberProvisioning.Outcome.UNCHANGED);
        assertThat(provisioning.provision(jeton)).isEqualTo(MemberProvisioning.Outcome.CACHED);
        verify(users, times(1)).findByKeycloakId(anyString());
        verify(users, never()).saveAndFlush(any());

        // Passé le délai, on relit ; un jeton qui change est relu tout de suite.
        now.set(now.get().plus(MemberProvisioning.TTL).plusSeconds(1));
        assertThat(provisioning.provision(jeton)).isEqualTo(MemberProvisioning.Outcome.UNCHANGED);
        assertThat(provisioning.provision(person("ana@acme.example", "Ana B.", "user")))
                .isEqualTo(MemberProvisioning.Outcome.UPDATED);
        verify(users, times(3)).findByKeycloakId(anyString());
    }

    @Test
    void uneInsertionConcurrenteGagnanteEstRelueSansErreur() {
        AppUser gagnant = AppUser.builder().id(UUID.randomUUID()).tenantId(TENANT).keycloakId(SUB.toString())
                .email("ana@acme.example").displayName("Ana").roles(new HashSet<>(Set.of("user"))).build();
        when(users.findByKeycloakId(SUB.toString())).thenReturn(Optional.empty(), Optional.of(gagnant));
        when(users.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_app_users_keycloak_id"));

        assertThat(provisioning.provision(person("ana@acme.example", "Ana", "user")))
                .isEqualTo(MemberProvisioning.Outcome.UNCHANGED);
    }

    @Test
    void unConflitPersistantEstJournaliseEtNeLevePas() {
        when(users.findByKeycloakId(SUB.toString())).thenReturn(Optional.empty());
        when(users.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_app_users_tenant_email"));

        assertThat(provisioning.provision(person("ana@acme.example", "Ana", "user")))
                .isEqualTo(MemberProvisioning.Outcome.CONFLICT);
        assertThat(logged()).anySatisfy(e -> {
            assertThat(e.getLevel()).isEqualTo(Level.WARN);
            assertThat(e.getFormattedMessage()).startsWith("directory.provisioning.conflict")
                    .doesNotContain("ana@acme.example");
        });
    }

    @Test
    void unMembreDUnAutreClientNEstJamaisDeplace() {
        AppUser ailleurs = AppUser.builder().id(UUID.randomUUID()).tenantId(UUID.randomUUID())
                .keycloakId(SUB.toString()).email("ana@acme.example").roles(new HashSet<>()).build();
        given(users.findByKeycloakId(SUB.toString())).willReturn(Optional.of(ailleurs));

        assertThat(provisioning.provision(person("ana@acme.example", "Ana", "user")))
                .isEqualTo(MemberProvisioning.Outcome.CONFLICT);
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void unJetonDeServiceOuSansClientNEstJamaisProvisionne() {
        Jwt service = jwt(Map.of("sub", SUB.toString(), "tenant_id", TENANT.toString(),
                "client_id", "api-quality-engine-ai", "email", "svc@acme.example"));
        Jwt compteDeService = jwt(Map.of("sub", SUB.toString(), "tenant_id", TENANT.toString(),
                "preferred_username", "service-account-qualitos-provisioner", "email", "svc@acme.example"));
        Jwt superAdmin = jwt(Map.of("sub", SUB.toString(), "email", "root@editeur.example",
                "realm_access", Map.of("roles", List.of("super_admin"))));
        Jwt clientIllisible = jwt(Map.of("sub", SUB.toString(), "tenant_id", "pas-un-uuid", "email", "a@b.example"));

        assertThat(provisioning.provision(service)).isEqualTo(MemberProvisioning.Outcome.SERVICE_TOKEN);
        assertThat(provisioning.provision(compteDeService)).isEqualTo(MemberProvisioning.Outcome.SERVICE_TOKEN);
        assertThat(provisioning.provision(superAdmin)).isEqualTo(MemberProvisioning.Outcome.NO_TENANT);
        assertThat(provisioning.provision(clientIllisible)).isEqualTo(MemberProvisioning.Outcome.NO_TENANT);
        verifyNoInteractions(users, tenants);
    }

    @Test
    void unJetonSansAdresseUtilisableNEcritRien() {
        Jwt sansAdresse = jwt(Map.of("sub", SUB.toString(), "tenant_id", TENANT.toString()));
        Jwt adresseInvalide = jwt(Map.of("sub", SUB.toString(), "tenant_id", TENANT.toString(), "email", "pas-une-adresse"));
        Jwt subIllisible = jwt(Map.of("sub", "abc", "tenant_id", TENANT.toString(), "email", "a@b.example"));

        assertThat(provisioning.provision(sansAdresse)).isEqualTo(MemberProvisioning.Outcome.UNUSABLE);
        assertThat(provisioning.provision(adresseInvalide)).isEqualTo(MemberProvisioning.Outcome.UNUSABLE);
        assertThat(provisioning.provision(subIllisible)).isEqualTo(MemberProvisioning.Outcome.UNUSABLE);
        verifyNoInteractions(users, tenants);
    }

    @Test
    void clientSansLigneTenants_rienNEstEcritEtUnAvertissementSansDonneePersonnelle() {
        UUID inconnu = UUID.randomUUID();
        given(tenants.existsById(inconnu)).willReturn(false);
        Jwt jeton = jwt(Map.of("sub", SUB.toString(), "tenant_id", inconnu.toString(),
                "email", "zoe@inconnu.example", "name", "Zoé Secret"));

        assertThat(provisioning.provision(jeton)).isEqualTo(MemberProvisioning.Outcome.TENANT_MISSING);
        verify(users, never()).saveAndFlush(any());
        verify(users, never()).findByKeycloakId(anyString());
        assertThat(logged()).anySatisfy(e -> {
            assertThat(e.getLevel()).isEqualTo(Level.WARN);
            assertThat(e.getFormattedMessage())
                    .isEqualTo("directory.provisioning.tenant-missing tenant_id=" + inconnu);
        });
        assertThat(logged()).noneSatisfy(e -> assertThat(e.getFormattedMessage())
                .containsAnyOf("zoe@inconnu.example", "Zoé Secret"));
        // Le cache évite de reposer la question à chaque requête.
        assertThat(provisioning.provision(jeton)).isEqualTo(MemberProvisioning.Outcome.CACHED);
        verify(tenants, times(1)).existsById(inconnu);
    }

    @Test
    void lePlafondDeLaLicenceEstRespecte() {
        given(users.findByKeycloakId(SUB.toString())).willReturn(Optional.empty());
        given(users.countByTenantIdAndActiveTrue(TENANT)).willReturn(40L);
        org.mockito.Mockito.doThrow(new EditionExceptions.MemberLimitReached(40)).when(limit).ensureRoomForOneMore(40L);

        assertThat(provisioning.provision(person("ana@acme.example", "Ana", "user")))
                .isEqualTo(MemberProvisioning.Outcome.LIMIT_REACHED);
        verify(users, never()).saveAndFlush(any());
    }
}
