package com.openlab.qualitos.quality.tenantmodules.application;

import com.openlab.qualitos.quality.tenantmodules.domain.ActivationStatus;
import com.openlab.qualitos.quality.tenantmodules.domain.BillingTier;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivation;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivationRepository;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivationStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * La surface PLATEFORME du service : ouvrir et fermer un module pour un client
 * <b>désigné</b>, et non pour celui du contexte.
 *
 * <p>Deux propriétés sont vérifiées ici, et nulle part ailleurs :
 *
 * <ol>
 *   <li><b>le tenant du contexte n'est jamais lu.</b> Chaque banc termine par
 *       {@code verifyNoInteractions(tenantProvider)} : c'est la seule façon de
 *       prouver que l'éditeur agit bien chez le client du chemin. Un appel
 *       oublié à {@code requireTenantId()} ferait ouvrir le module chez
 *       l'éditeur lui-même, et le client attendrait un module qui n'arriverait
 *       jamais ;</li>
 *   <li><b>l'idempotence.</b> L'appelant est {@code api-core}, qui exprime un
 *       état voulu, pas une transition. Refuser en 409 sur un module déjà
 *       ouvert rendrait certaines souscriptions impossibles pour toujours —
 *       le piège corrigé par la PR #122.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlatformModuleActivationServiceTest {

    @Mock ModuleActivationRepository repo;
    @Mock TenantProvider tenantProvider;
    @Mock TenantTierProvider tierProvider;
    @Mock ActorProvider actorProvider;
    @Mock ModuleActivationEventPublisher events;

    ModuleActivationService service;

    static final UUID CLIENT = UUID.randomUUID();
    static final UUID EDITEUR = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-09-15T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        service = new ModuleActivationService(
                repo, tenantProvider, tierProvider, actorProvider, events, CLOCK);
        when(actorProvider.requireActorId()).thenReturn(EDITEUR);
        when(tierProvider.currentTier(CLIENT)).thenReturn(BillingTier.STANDARD);
        when(repo.save(any())).thenAnswer(invocation -> {
            ModuleActivation activation = invocation.getArgument(0);
            if (activation.getId() == null) {
                activation.assignId(UUID.randomUUID());
            }
            return activation;
        });
    }

    // ---------- ouverture ----------

    @Test
    void ouvrirUnModuleFermeCreeLActivationChezLeClientDuChemin() {
        when(repo.findOpenByTenantIdAndCode(CLIENT, "risk")).thenReturn(Optional.empty());

        ModuleActivationDto.ActivationView vue = service.activateFor(CLIENT, "risk", null);

        assertThat(vue.tenantId()).isEqualTo(CLIENT);
        assertThat(vue.status()).isEqualTo(ActivationStatus.ACTIVE);
        assertThat(vue.enabled()).isTrue();
        verify(events).publish(any(),
                eq(ModuleActivationEventPublisher.Action.PLATFORM_ACTIVATED));
        verifyNoInteractions(tenantProvider);
    }

    @Test
    void lActeurEstLEditeurLuDuJeton() {
        // §18.2 regle 5 : l'acte est attribuable. C'est cet identifiant que le
        // journal chaine du client portera — et il n'appartient a aucun de ses
        // annuaires, d'ou l'action nommee a part.
        when(repo.findOpenByTenantIdAndCode(CLIENT, "risk")).thenReturn(Optional.empty());

        ModuleActivationDto.ActivationView vue = service.activateFor(CLIENT, "risk", null);

        assertThat(vue.activatedBy()).isEqualTo(EDITEUR);
    }

    @Test
    void ouvrirUnModuleDejaOuvertNeChangeRienEtNeJournalisePas() {
        // Le point central. Un module peut etre deja ouvert sans qu'aucune
        // souscription ne le sache : active a la main, ou herite du socle.
        // Refuser rendrait la souscription impossible pour toujours.
        ModuleActivation ouvert = active("risk");
        when(repo.findOpenByTenantIdAndCode(CLIENT, "risk")).thenReturn(Optional.of(ouvert));

        ModuleActivationDto.ActivationView vue = service.activateFor(CLIENT, "risk", null);

        assertThat(vue.enabled()).isTrue();
        verify(repo, never()).save(any());
        // Aucun evenement : un acte sans effet allongerait la chaine d'audit
        // sans rien dire.
        verifyNoInteractions(events);
        verifyNoInteractions(tenantProvider);
    }

    @Test
    void ouvrirUnModuleSuspenduLeReprendAuLieuDenDoublerUnSecond() {
        // L'index d'unicite n'admet qu'une ligne non terminale par (tenant,
        // module) : creer une seconde activation echouerait en base apres avoir
        // semble reussir.
        ModuleActivation suspendu = active("risk");
        suspendu.suspend(EDITEUR, NOW);
        when(repo.findOpenByTenantIdAndCode(CLIENT, "risk")).thenReturn(Optional.of(suspendu));

        ModuleActivationDto.ActivationView vue = service.activateFor(CLIENT, "risk", null);

        assertThat(vue.status()).isEqualTo(ActivationStatus.ACTIVE);
        verify(repo).save(suspendu);
        verify(events).publish(eq(suspendu),
                eq(ModuleActivationEventPublisher.Action.PLATFORM_ACTIVATED));
    }

    @Test
    void ouvrirUnModuleHorsCatalogueEstRefuse() {
        assertThatThrownBy(() -> service.activateFor(CLIENT, "module-invente", null))
                .isInstanceOf(RuntimeException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void ouvrirEnDessousDuPalierRequisEstRefuse() {
        // Souscrire ne dispense pas d'avoir le palier : la garde vaut aussi
        // quand c'est l'editeur qui ouvre.
        when(tierProvider.currentTier(CLIENT)).thenReturn(BillingTier.FREE);
        when(repo.findOpenByTenantIdAndCode(CLIENT, "risk")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activateFor(CLIENT, "risk", null))
                .isInstanceOf(ModuleActivationStateException.class)
                .hasMessageContaining("below required");
        verify(repo, never()).save(any());
    }

    @Test
    void ouvrirUnModuleDontUneDependanceManqueEstRefuse() {
        // controlplan depend de risk et product : ouvrir le premier sans les
        // seconds livrerait un module inutilisable, et facture.
        when(repo.findOpenByTenantIdAndCode(CLIENT, "controlplan")).thenReturn(Optional.empty());
        when(repo.findAllByTenantId(CLIENT)).thenReturn(List.of());

        assertThatThrownBy(() -> service.activateFor(CLIENT, "controlplan", null))
                .isInstanceOf(ModuleActivationStateException.class)
                .hasMessageContaining("Missing dependency");
        verify(repo, never()).save(any());
    }

    // ---------- fermeture ----------

    @Test
    void fermerUnModuleOuvertLeDesactiveEtLeJournalise() {
        ModuleActivation ouvert = active("risk");
        when(repo.findOpenByTenantIdAndCode(CLIENT, "risk")).thenReturn(Optional.of(ouvert));
        when(repo.findEnabledByTenantId(CLIENT)).thenReturn(List.of(ouvert));

        service.deactivateFor(CLIENT, "risk");

        assertThat(ouvert.getStatus()).isEqualTo(ActivationStatus.DISABLED);
        assertThat(ouvert.getLastChangedBy()).isEqualTo(EDITEUR);
        verify(events).publish(eq(ouvert),
                eq(ModuleActivationEventPublisher.Action.PLATFORM_DEACTIVATED));
        verifyNoInteractions(tenantProvider);
    }

    @Test
    void fermerUnModuleDejaFermeNEstPasUneErreur() {
        // C'est l'etat demande : rejouer une resiliation apres une panne reseau
        // ne doit pas echouer.
        when(repo.findOpenByTenantIdAndCode(CLIENT, "risk")).thenReturn(Optional.empty());

        service.deactivateFor(CLIENT, "risk");

        verify(repo, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void fermerUnModuleDuSocleEstRefuse() {
        // capa est un module de base : le fermer priverait le client d'ecriture
        // sur ce dont il dispose de droit.
        assertThatThrownBy(() -> service.deactivateFor(CLIENT, "capa"))
                .isInstanceOf(ModuleActivationStateException.class)
                .hasMessageContaining("core module");
        verify(repo, never()).save(any());
    }

    @Test
    void fermerUnModuleDontUnAutreDependEstRefuse() {
        // Fermer risk alors que controlplan tourne encore couperait l'ecriture
        // sur un module que le client paie toujours.
        ModuleActivation risk = active("risk");
        ModuleActivation controlplan = active("controlplan");
        when(repo.findOpenByTenantIdAndCode(CLIENT, "risk")).thenReturn(Optional.of(risk));
        when(repo.findEnabledByTenantId(CLIENT)).thenReturn(List.of(risk, controlplan));

        assertThatThrownBy(() -> service.deactivateFor(CLIENT, "risk"))
                .isInstanceOf(ModuleActivationStateException.class)
                .hasMessageContaining("required by");
        assertThat(risk.getStatus()).isEqualTo(ActivationStatus.ACTIVE);
    }

    // ---------- lecture ----------

    @Test
    void listerRendLesActivationsDuClientDuChemin() {
        when(repo.findAllByTenantId(CLIENT)).thenReturn(List.of(active("risk")));

        List<ModuleActivationDto.ActivationView> vues = service.listFor(CLIENT);

        assertThat(vues).hasSize(1);
        assertThat(vues.get(0).moduleCode()).isEqualTo("risk");
        assertThat(vues.get(0).tenantId()).isEqualTo(CLIENT);
        verifyNoInteractions(tenantProvider);
    }

    @Test
    void unClientSansModuleRendUneListeVide() {
        when(repo.findAllByTenantId(CLIENT)).thenReturn(List.of());

        assertThat(service.listFor(CLIENT)).isEmpty();
    }

    // ---------- fixtures ----------

    private static ModuleActivation active(String moduleCode) {
        ModuleActivation activation = ModuleActivation.activateNow(
                CLIENT, moduleCode, BillingTier.STANDARD, null, EDITEUR, NOW);
        activation.assignId(UUID.randomUUID());
        return activation;
    }
}
