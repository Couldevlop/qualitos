package com.openlab.qualitos.core.directory;

import com.openlab.qualitos.core.common.MissingTenantContextException;
import com.openlab.qualitos.core.security.TenantContext;
import com.openlab.qualitos.core.user.DirectoryRow;
import com.openlab.qualitos.core.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("DirectoryService")
class DirectoryServiceTest {

    @Mock
    private UserRepository users;

    @InjectMocks
    private DirectoryService directory;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ID = UUID.randomUUID();
    private static final String KC = UUID.randomUUID().toString();

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("cherche dans le client du jeton, avec la page demandée")
    void searchesInTheTokenTenant() {
        TenantContext.setTenantId(TENANT.toString());
        given(users.searchDirectory(TENANT, "%al%", PageRequest.of(1, 10))).willReturn(new PageImpl<>(
                List.of(new DirectoryRow(ID, KC, "Alice", "alice@acme.com", true)), PageRequest.of(1, 10), 11));

        DirectoryDto.Page page = directory.search(new DirectoryQuery("%al%", 1, 10));

        assertThat(page.content()).containsExactly(new DirectoryDto.Entry(ID, KC, "Alice", "alice@acme.com", true));
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(10);
        assertThat(page.totalElements()).isEqualTo(11);
        assertThat(page.totalPages()).isEqualTo(2);
        verify(users).searchDirectory(TENANT, "%al%", PageRequest.of(1, 10));
    }

    @Test
    @DisplayName("sans nom enregistré, l'adresse tient lieu de nom")
    void emailStandsInForMissingName() {
        TenantContext.setTenantId(TENANT.toString());
        given(users.findDirectoryRow(TENANT, ID)).willReturn(Optional.of(new DirectoryRow(ID, KC, "  ", "bob@acme.com", true)));

        assertThat(directory.get(ID).displayName()).isEqualTo("bob@acme.com");
    }

    @Test
    @DisplayName("lit par compte de connexion dans le client du jeton")
    void getsByAccountInTheTokenTenant() {
        TenantContext.setTenantId(TENANT.toString());
        UUID account = UUID.fromString(KC);
        given(users.findDirectoryRowByAccount(TENANT, KC))
                .willReturn(Optional.of(new DirectoryRow(ID, KC, "Alice", "alice@acme.com", false)));

        DirectoryDto.Entry entry = directory.getByAccount(account);

        assertThat(entry.accountId()).isEqualTo(KC);
        assertThat(entry.active()).isFalse();
    }

    @Test
    @DisplayName("compte absent du client : exception neutre ; sans client : aucune lecture")
    void absentAccountIsNotFound() {
        UUID account = UUID.fromString(KC);
        assertThatThrownBy(() -> directory.getByAccount(account)).isInstanceOf(MissingTenantContextException.class);
        TenantContext.setTenantId(TENANT.toString());
        given(users.findDirectoryRowByAccount(TENANT, KC)).willReturn(Optional.empty());

        assertThatThrownBy(() -> directory.getByAccount(account)).isInstanceOf(DirectoryEntryNotFoundException.class);
    }

    @Test
    @DisplayName("absent du client : exception neutre")
    void absentIsNotFound() {
        TenantContext.setTenantId(TENANT.toString());
        given(users.findDirectoryRow(TENANT, ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> directory.get(ID)).isInstanceOf(DirectoryEntryNotFoundException.class);
    }

    @Test
    @DisplayName("sans client dans le jeton, ou client illisible : aucune lecture")
    void noTenantNoRead() {
        assertThatThrownBy(() -> directory.search(new DirectoryQuery("%", 0, 20)))
                .isInstanceOf(MissingTenantContextException.class);
        TenantContext.setTenantId("pas-un-uuid");
        assertThatThrownBy(() -> directory.get(ID)).isInstanceOf(MissingTenantContextException.class);
        verifyNoInteractions(users);
    }
}
