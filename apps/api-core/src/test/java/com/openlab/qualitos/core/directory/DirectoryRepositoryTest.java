package com.openlab.qualitos.core.directory;

import com.openlab.qualitos.core.security.TenantContext;
import com.openlab.qualitos.core.user.AppUser;
import com.openlab.qualitos.core.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L'annuaire de bout en bout sur une vraie base (H2, mode PostgreSQL) : les
 * requêtes JPQL, la clé de recherche calculée à l'écriture, le tri et les
 * frontières entre clients — ce qu'un service simulé ne peut pas prouver.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Annuaire sur base — recherche, tri, isolation (ADR 0091)")
class DirectoryRepositoryTest {

    @Autowired
    private UserRepository users;

    private DirectoryService directory;

    private final UUID acme = UUID.randomUUID();
    private final UUID globex = UUID.randomUUID();

    private AppUser elodie;
    private AppUser bruno;
    private AppUser ancien;
    private AppUser sansNom;
    private AppUser concurrent;

    @BeforeEach
    void seed() {
        directory = new DirectoryService(users);
        elodie = save(acme, "Élodie Müller", "elodie.muller@acme.com", true);
        bruno = save(acme, "Bruno Achard", "bruno@acme.com", true);
        sansNom = save(acme, null, "chantal@acme.com", true);
        ancien = save(acme, "Élodie Ancienne", "ancienne@acme.com", false);
        concurrent = save(globex, "Élodie Globex", "elodie@globex.com", true);
        TenantContext.setTenantId(acme.toString());
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private AppUser save(UUID tenant, String name, String email, boolean active) {
        return users.saveAndFlush(AppUser.builder()
                .tenantId(tenant)
                .keycloakId(UUID.randomUUID().toString())
                .email(email)
                .displayName(name)
                .roles(new HashSet<>(Set.of("USER")))
                .active(active)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());
    }

    private List<String> names(String q) {
        return directory.search(DirectoryQuery.of(q, 0, 50)).content().stream()
                .map(DirectoryDto.Entry::displayName).toList();
    }

    @Test
    @DisplayName("sans terme : les membres ACTIFS du seul client du jeton, triés par nom")
    void listsActiveMembersOfTheTenantSortedByName() {
        assertThat(names(null)).containsExactly("Bruno Achard", "chantal@acme.com", "Élodie Müller");
    }

    @Test
    @DisplayName("la recherche ignore accents et casse, sur le nom comme sur l'adresse")
    void searchIsAccentAndCaseInsensitive() {
        assertThat(names("ELODIE")).containsExactly("Élodie Müller");
        assertThat(names("muller")).containsExactly("Élodie Müller");
        assertThat(names("ACHARD")).containsExactly("Bruno Achard");
        assertThat(names("chantal@")).containsExactly("chantal@acme.com");
    }

    @Test
    @DisplayName("un membre désactivé n'apparaît pas dans la recherche")
    void inactiveMembersAreExcluded() {
        assertThat(names("ancienne")).isEmpty();
    }

    @Test
    @DisplayName("un membre d'un autre client n'apparaît jamais")
    void otherTenantIsInvisible() {
        assertThat(names("globex")).isEmpty();
    }

    @Test
    @DisplayName("les jokers saisis sont pris à la lettre")
    void wildcardsAreLiteral() {
        assertThat(names("%")).isEmpty();
        assertThat(names("_")).isEmpty();
    }

    @Test
    @DisplayName("la pagination découpe et compte")
    void paginates() {
        DirectoryDto.Page first = directory.search(DirectoryQuery.of(null, 0, 2));
        DirectoryDto.Page second = directory.search(DirectoryQuery.of(null, 1, 2));

        assertThat(first.content()).extracting(DirectoryDto.Entry::id).containsExactly(bruno.getId(), sansNom.getId());
        assertThat(second.content()).extracting(DirectoryDto.Entry::id).containsExactly(elodie.getId());
        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(first.totalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("lecture par identifiant : un ancien membre du client garde son nom")
    void getsFormerMemberById() {
        DirectoryDto.Entry entry = directory.get(ancien.getId());

        assertThat(entry.displayName()).isEqualTo("Élodie Ancienne");
        assertThat(entry.active()).isFalse();
    }

    @Test
    @DisplayName("lecture par identifiant d'un autre client : même 404 qu'un identifiant inconnu")
    void foreignIdIsIndistinguishableFromUnknown() {
        assertThatThrownBy(() -> directory.get(concurrent.getId()))
                .isInstanceOf(DirectoryEntryNotFoundException.class)
                .hasMessage("Directory entry not found");
        assertThatThrownBy(() -> directory.get(UUID.randomUUID()))
                .isInstanceOf(DirectoryEntryNotFoundException.class)
                .hasMessage("Directory entry not found");
    }

    @Test
    @DisplayName("chaque entrée porte le compte de connexion du membre")
    void entriesCarryAccountId() {
        assertThat(directory.search(DirectoryQuery.of("muller", 0, 50)).content())
                .extracting(DirectoryDto.Entry::accountId)
                .containsExactly(elodie.getKeycloakId());
    }

    @Test
    @DisplayName("lecture par compte : un ancien membre du client garde son nom")
    void getsFormerMemberByAccount() {
        DirectoryDto.Entry entry = directory.getByAccount(UUID.fromString(ancien.getKeycloakId()));

        assertThat(entry.id()).isEqualTo(ancien.getId());
        assertThat(entry.displayName()).isEqualTo("Élodie Ancienne");
        assertThat(entry.active()).isFalse();
    }

    @Test
    @DisplayName("lecture par compte d'un autre client : même 404 qu'un compte inconnu")
    void foreignAccountIsIndistinguishableFromUnknown() {
        assertThatThrownBy(() -> directory.getByAccount(UUID.fromString(concurrent.getKeycloakId())))
                .isInstanceOf(DirectoryEntryNotFoundException.class)
                .hasMessage("Directory entry not found");
        assertThatThrownBy(() -> directory.getByAccount(UUID.randomUUID()))
                .isInstanceOf(DirectoryEntryNotFoundException.class)
                .hasMessage("Directory entry not found");
    }

    @Test
    @DisplayName("la clé de recherche suit un changement de nom")
    void searchKeyFollowsRename() {
        bruno.setDisplayName("Bruno Lefèvre");
        users.saveAndFlush(bruno);

        assertThat(names("lefevre")).containsExactly("Bruno Lefèvre");
        assertThat(names("achard")).isEmpty();
    }
}
