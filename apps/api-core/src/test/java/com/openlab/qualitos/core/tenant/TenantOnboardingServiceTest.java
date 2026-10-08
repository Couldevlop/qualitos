package com.openlab.qualitos.core.tenant;

import com.openlab.qualitos.core.billing.ModuleActivationFailedException;
import com.openlab.qualitos.core.billing.ModuleActivationPort;
import com.openlab.qualitos.core.identity.AccountAlreadyExistsException;
import com.openlab.qualitos.core.identity.IdentityProvider;
import com.openlab.qualitos.core.user.AppUser;
import com.openlab.qualitos.core.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantOnboardingServiceTest {

    TenantService tenants;
    UserRepository users;
    IdentityProvider identity;
    ModuleActivationPort modules;
    PlatformTransactionManager txManager;
    TenantOnboardingService service;

    final UUID tenantId = UUID.randomUUID();
    final TransactionStatus status = new SimpleTransactionStatus();

    @BeforeEach
    void setUp() {
        tenants = mock(TenantService.class);
        users = mock(UserRepository.class);
        identity = mock(IdentityProvider.class);
        modules = mock(ModuleActivationPort.class);
        txManager = mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any())).thenReturn(status);
        service = new TenantOnboardingService(tenants, users, identity, modules, new TransactionTemplate(txManager));

        when(tenants.create(any())).thenReturn(new TenantDto.Response(tenantId, "acme", "ACME", Tenant.Plan.STARTER,
                true, Instant.now(), Instant.now()));
        when(identity.create(any())).thenReturn(new IdentityProvider.CreatedAccount("kc-admin", "Tmp4Pass", false));
        when(users.saveAndFlush(any(AppUser.class))).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
    }

    static OnboardingDto.Request demande(List<String> modules) {
        return new OnboardingDto.Request(" ACME ", "acme", null, modules,
                new OnboardingDto.Admin(" alice@acme.fr ", "Alice", "Durand"));
    }

    @Test
    void creeLeClientSonAdministrateurEtOuvreSesModules() {
        OnboardingDto.Response r = service.onboard(demande(List.of("pdca", "capa", "pdca")));

        ArgumentCaptor<TenantDto.CreateRequest> client = ArgumentCaptor.forClass(TenantDto.CreateRequest.class);
        verify(tenants).create(client.capture());
        assertThat(client.getValue().name()).isEqualTo("ACME");
        assertThat(client.getValue().slug()).isEqualTo("acme");

        ArgumentCaptor<IdentityProvider.NewAccount> compte = ArgumentCaptor.forClass(IdentityProvider.NewAccount.class);
        verify(identity).create(compte.capture());
        // Rattaché au NOUVEAU client, pas à celui du super administrateur.
        assertThat(compte.getValue().tenantId()).isEqualTo(tenantId);
        assertThat(compte.getValue().email()).isEqualTo("alice@acme.fr");
        assertThat(compte.getValue().roles()).containsExactly("admin_tenant");

        assertThat(r.admin().tenantId()).isEqualTo(tenantId);
        assertThat(r.admin().roles()).containsExactly("admin_tenant");
        assertThat(r.temporaryPassword()).isEqualTo("Tmp4Pass");
        // Un module demandé deux fois n'est ouvert qu'une fois.
        assertThat(r.modules()).extracting(OnboardingDto.ModuleOutcome::code).containsExactly("pdca", "capa");
        assertThat(r.modules()).allMatch(OnboardingDto.ModuleOutcome::activated);
        verify(txManager).commit(status);
    }

    @Test
    void unModuleRefuseNAnnulePasLeClientEtSeSignale() {
        doThrow(new ModuleActivationFailedException("moteur injoignable")).when(modules).activate(tenantId, "iot");

        OnboardingDto.Response r = service.onboard(demande(List.of("capa", "iot")));

        assertThat(r.modules()).filteredOn(m -> m.code().equals("iot")).singleElement().satisfies(m -> {
            assertThat(m.activated()).isFalse();
            assertThat(m.message()).contains("moteur injoignable");
        });
        assertThat(r.tenant().id()).isEqualTo(tenantId);
    }

    @Test
    void sansCompteRienNEstEnregistre() {
        when(identity.create(any())).thenThrow(new AccountAlreadyExistsException("alice@acme.fr"));

        assertThatThrownBy(() -> service.onboard(demande(null))).isInstanceOf(AccountAlreadyExistsException.class);

        verify(txManager).rollback(status);
        verify(users, never()).saveAndFlush(any());
        verify(modules, never()).activate(any(), any());
    }

    @Test
    void siLAdministrateurNeSEnregistrePasSonCompteEstSupprime() {
        when(users.saveAndFlush(any(AppUser.class))).thenThrow(new IllegalStateException("contrainte"));

        assertThatThrownBy(() -> service.onboard(demande(List.of("capa")))).isInstanceOf(IllegalStateException.class);

        verify(identity).delete("kc-admin");
        verify(txManager).rollback(status);
        verify(modules, never()).activate(any(), any());
    }

    @Test
    void sansModuleDemandeLaListeEstVide() {
        assertThat(service.onboard(demande(null)).modules()).isEmpty();
        assertThat(Set.copyOf(service.onboard(demande(List.of())).modules())).isEmpty();
    }
}
