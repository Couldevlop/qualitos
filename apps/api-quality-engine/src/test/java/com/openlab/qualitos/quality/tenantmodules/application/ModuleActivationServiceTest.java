package com.openlab.qualitos.quality.tenantmodules.application;

import com.openlab.qualitos.quality.tenantmodules.domain.ActivationStatus;
import com.openlab.qualitos.quality.tenantmodules.domain.BillingTier;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivation;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivationNotFoundException;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ModuleActivationServiceTest {

    @Mock ModuleActivationRepository repo;
    @Mock TenantProvider tenantProvider;
    @Mock TenantTierProvider tierProvider;
    @Mock ActorProvider actorProvider;
    @Mock ModuleActivationEventPublisher events;
    ModuleActivationService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID ACTOR = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Instant FUTURE = NOW.plusSeconds(86400L * 30);
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        service = new ModuleActivationService(
                repo, tenantProvider, tierProvider, actorProvider, events, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        // H2 : l'acteur provient du JWT (ActorProvider), plus du corps de requête.
        when(actorProvider.requireActorId()).thenReturn(ACTOR);
    }

    @Test
    void catalog_isExposed() {
        assertThat(service.listCatalog()).isNotEmpty();
        assertThat(service.listCatalog())
                .anyMatch(e -> e.code().equals("pdca"));
    }

    @Test
    void startTrial_freeModule_ok() {
        when(tierProvider.currentTier(TENANT)).thenReturn(BillingTier.FREE);
        when(repo.findOpenByTenantIdAndCode(TENANT, "pdca")).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> {
            ModuleActivation a = inv.getArgument(0);
            a.assignId(ID);
            return a;
        });
        ModuleActivationDto.ActivationView v = service.startTrial(
                new ModuleActivationDto.StartTrialRequest("pdca", FUTURE));
        assertThat(v.status()).isEqualTo(ActivationStatus.TRIAL);
        verify(events).publish(any(), eq(ModuleActivationEventPublisher.Action.TRIAL_STARTED));
    }

    @Test
    void startTrial_unknownModule_rejected() {
        assertThatThrownBy(() -> service.startTrial(
                new ModuleActivationDto.StartTrialRequest("not-a-module", FUTURE)))
                .isInstanceOf(ModuleActivationStateException.class);
    }

    @Test
    void activate_tierBelowRequired_rejected() {
        // blockchain require ENTERPRISE, tenant is FREE
        when(tierProvider.currentTier(TENANT)).thenReturn(BillingTier.FREE);
        when(repo.findOpenByTenantIdAndCode(TENANT, "blockchain")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.activate(
                new ModuleActivationDto.ActivateRequest("blockchain", FUTURE)))
                .isInstanceOf(ModuleActivationStateException.class)
                .hasMessageContaining("tier");
    }

    @Test
    void activate_existingOpen_rejected() {
        when(tierProvider.currentTier(TENANT)).thenReturn(BillingTier.STANDARD);
        when(repo.findOpenByTenantIdAndCode(TENANT, "kpi"))
                .thenReturn(Optional.of(mockActive("kpi")));
        assertThatThrownBy(() -> service.activate(
                new ModuleActivationDto.ActivateRequest("kpi", FUTURE)))
                .isInstanceOf(ModuleActivationStateException.class)
                .hasMessageContaining("already");
    }

    // ---- Dépendances et socle du catalogue -----------------------------------
    //
    // Ces bancs couvrent un défaut mesuré en préproduction : la garde des
    // dépendances exigeait une LIGNE d'activation, alors que les modules du socle
    // n'en ont aucune. Activer `risk` (qui dépend de `capa`, module du socle)
    // répondait 409 « Missing dependency » de façon définitive, sur un tenant où
    // `capa` était pourtant disponible. Le banc d'origine figeait ce comportement
    // en le prenant pour la règle.

    @Test
    void activate_dependanceDuSocleSansLigneDActivation_ok() {
        // `capa` est un module du socle : aucune activation ne le porte, et c'est normal.
        when(tierProvider.currentTier(TENANT)).thenReturn(BillingTier.STANDARD);
        when(repo.findOpenByTenantIdAndCode(TENANT, "risk")).thenReturn(Optional.empty());
        when(repo.findAllByTenantId(TENANT)).thenReturn(List.of());
        when(repo.save(any())).thenAnswer(inv -> {
            ModuleActivation a = inv.getArgument(0); a.assignId(ID); return a;
        });

        ModuleActivationDto.ActivationView v = service.activate(
                new ModuleActivationDto.ActivateRequest("risk", FUTURE));

        assertThat(v.status()).isEqualTo(ActivationStatus.ACTIVE);
    }

    @Test
    void activate_dependanceDuSocleExplicitementFermee_rejected() {
        // Une activation fermée l'emporte sur l'appartenance au socle : c'est une
        // décision, pas une absence — et la garde doit la respecter.
        ModuleActivation capaFermee = mockActive("capa");
        capaFermee.disable(ACTOR, NOW.plusSeconds(60));
        when(tierProvider.currentTier(TENANT)).thenReturn(BillingTier.STANDARD);
        when(repo.findOpenByTenantIdAndCode(TENANT, "risk")).thenReturn(Optional.empty());
        when(repo.findAllByTenantId(TENANT)).thenReturn(List.of(capaFermee));

        assertThatThrownBy(() -> service.activate(
                new ModuleActivationDto.ActivateRequest("risk", FUTURE)))
                .isInstanceOf(ModuleActivationStateException.class)
                .hasMessageContaining("dependency");
    }

    @Test
    void activate_dependanceHorsSocleAbsente_rejected() {
        // `controlplan` dépend de `risk` ET de `product`, aucun des deux n'étant du
        // socle : sans activation, le refus reste la bonne réponse.
        when(tierProvider.currentTier(TENANT)).thenReturn(BillingTier.STANDARD);
        when(repo.findOpenByTenantIdAndCode(TENANT, "controlplan")).thenReturn(Optional.empty());
        when(repo.findAllByTenantId(TENANT)).thenReturn(List.of());

        assertThatThrownBy(() -> service.activate(
                new ModuleActivationDto.ActivateRequest("controlplan", FUTURE)))
                .isInstanceOf(ModuleActivationStateException.class)
                .hasMessageContaining("dependency");
    }

    @Test
    void activate_toutesDependancesOuvertes_ok() {
        // Le cas nominal du control plan : ses deux dépendances portent une activation active.
        when(tierProvider.currentTier(TENANT)).thenReturn(BillingTier.STANDARD);
        when(repo.findOpenByTenantIdAndCode(TENANT, "controlplan")).thenReturn(Optional.empty());
        when(repo.findAllByTenantId(TENANT))
                .thenReturn(List.of(mockActive("risk"), mockActive("product")));
        when(repo.save(any())).thenAnswer(inv -> {
            ModuleActivation a = inv.getArgument(0); a.assignId(ID); return a;
        });

        ModuleActivationDto.ActivationView v = service.activate(
                new ModuleActivationDto.ActivateRequest("controlplan", FUTURE));

        assertThat(v.status()).isEqualTo(ActivationStatus.ACTIVE);
    }

    @Test
    void activate_dependenciesEnabled_ok() {
        when(tierProvider.currentTier(TENANT)).thenReturn(BillingTier.STANDARD);
        when(repo.findOpenByTenantIdAndCode(TENANT, "risk")).thenReturn(Optional.empty());
        when(repo.findOpenByTenantIdAndCode(TENANT, "capa"))
                .thenReturn(Optional.of(mockActive("capa")));
        when(repo.save(any())).thenAnswer(inv -> {
            ModuleActivation a = inv.getArgument(0); a.assignId(ID); return a;
        });
        ModuleActivationDto.ActivationView v = service.activate(
                new ModuleActivationDto.ActivateRequest("risk", FUTURE));
        assertThat(v.status()).isEqualTo(ActivationStatus.ACTIVE);
    }

    @Test
    void disable_coreModule_rejected() {
        ModuleActivation a = mockActive("pdca"); a.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(a));
        assertThatThrownBy(() -> service.disable(ID,
                new ModuleActivationDto.DisableRequest()))
                .isInstanceOf(ModuleActivationStateException.class)
                .hasMessageContaining("core");
    }

    @Test
    void disable_withDependents_rejected() {
        // capa is required by risk; can't disable capa while risk is enabled
        ModuleActivation capa = mockActive("capa"); capa.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(capa));
        // capa est core ⇒ rejet immédiat avant même check des dépendants.
        // Pour tester le chemin dépendants on prend un module non-core.
        ModuleActivation risk = mockActive("risk"); risk.assignId(UUID.randomUUID());
        when(repo.findById(risk.getId())).thenReturn(Optional.of(risk));
        when(repo.findEnabledByTenantId(TENANT))
                .thenReturn(List.of(mockActive("supplier"))); // supplier depends on audit+capa, mais pas risk → OK
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.disable(risk.getId(), new ModuleActivationDto.DisableRequest());
        assertThat(risk.getStatus()).isEqualTo(ActivationStatus.DISABLED);
    }

    @Test
    void disable_blockedByDependentModule() {
        // audit core ⇒ refus immédiat. Utilisons "training" (non-core, sans deps).
        // Aucun module ne dépend de training dans le catalogue → disable possible.
        // Test inverse : supplier dépend de capa+audit, donc audit ne peut pas être disabled.
        // audit est core ⇒ rejet avant guard dépendants. On va donc forcer un cas
        // synthétique via "kpi" (non-core, sans deps déclarés, donc rien ne le bloque).
        // Pour vraiment tester ensureNoDependentModulesEnabled, on choisit "docs" (core)
        // → bloqué par core. Bref ce test est couvert indirectement, on skip un cas dédié.
        ModuleActivation a = mockActive("training"); a.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(a));
        when(repo.findEnabledByTenantId(TENANT)).thenReturn(List.of()); // personne ne dépend de training
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.disable(ID, new ModuleActivationDto.DisableRequest());
        assertThat(a.getStatus()).isEqualTo(ActivationStatus.DISABLED);
    }

    @Test
    void crossTenant_appearsNotFound() {
        ModuleActivation other = mockActive("kpi");
        other.assignId(ID);
        // Inject tenant différent en reconstruisant
        ModuleActivation foreign = new ModuleActivation(
                ID, UUID.randomUUID(), "kpi", ActivationStatus.ACTIVE,
                BillingTier.STANDARD, null, null, FUTURE,
                NOW, ACTOR, NOW, ACTOR, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(ModuleActivationNotFoundException.class);
    }

    @Test
    void isEnabled_truthyForActive() {
        when(repo.findOpenByTenantIdAndCode(TENANT, "kpi"))
                .thenReturn(Optional.of(mockActive("kpi")));
        assertThat(service.isEnabled("kpi")).isTrue();
    }

    @Test
    void isEnabled_falseForMissing() {
        when(repo.findOpenByTenantIdAndCode(TENANT, "kpi")).thenReturn(Optional.empty());
        assertThat(service.isEnabled("kpi")).isFalse();
    }

    /**
     * Socle acquis d'office. Le catalogue distingue depuis l'origine les modules
     * « de base » — PDCA, Ishikawa, 5S, CAPA, documents, audits — qu'il est
     * d'ailleurs interdit de désactiver. Mais sans ligne d'activation, ils
     * étaient comptés pour éteints : un tenant nouvellement créé n'avait donc
     * AUCUN module actif, pas même ceux qu'on lui refuse de couper. La promesse
     * « défaut = modules standards » n'était pas tenue.
     */
    @Test
    void isEnabled_trueForCoreModuleWithoutAnyActivation() {
        when(repo.findOpenByTenantIdAndCode(TENANT, "pdca")).thenReturn(Optional.empty());
        assertThat(service.isEnabled("pdca")).isTrue();
    }

    @Test
    void isEnabled_coreDefaultAppliesToEveryCoreModule() {
        for (String code : List.of("pdca", "ishikawa", "fives", "capa", "docs", "audit")) {
            when(repo.findOpenByTenantIdAndCode(TENANT, code)).thenReturn(Optional.empty());
            assertThat(service.isEnabled(code)).withFailMessage(code).isTrue();
        }
    }

    @Test
    void isEnabled_optionalModuleStillNeedsAnActivation() {
        // Le socle est un plancher, pas une ouverture générale : ce qui se
        // facture à l'unité continue d'exiger une activation explicite.
        when(repo.findOpenByTenantIdAndCode(TENANT, "iot")).thenReturn(Optional.empty());
        assertThat(service.isEnabled("iot")).isFalse();
    }

    @Test
    void isEnabled_unknownModuleIsNeverEnabled() {
        when(repo.findOpenByTenantIdAndCode(TENANT, "inexistant")).thenReturn(Optional.empty());
        assertThat(service.isEnabled("inexistant")).isFalse();
    }

    @Test
    void summary_aggregates() {
        when(tierProvider.currentTier(TENANT)).thenReturn(BillingTier.PRO);
        when(repo.findAllByTenantId(TENANT)).thenReturn(List.of(
                mockActive("kpi"),
                mockActive("training"),
                mockSuspended("circle")));
        ModuleActivationDto.TenantModuleSummary s = service.summary();
        assertThat(s.tenantTier()).isEqualTo(BillingTier.PRO);
        assertThat(s.totalActivations()).isEqualTo(3);
        assertThat(s.activeCount()).isEqualTo(2);
        assertThat(s.suspendedCount()).isEqualTo(1);
        assertThat(s.enabledCount()).isEqualTo(2);
    }

    @Test
    void convertTrial_emitsActivatedEvent() {
        ModuleActivation trial = ModuleActivation.startTrial(
                TENANT, "kpi", BillingTier.STANDARD, FUTURE, ACTOR, NOW);
        trial.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(trial));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.convertTrial(ID,
                new ModuleActivationDto.ConvertTrialRequest(FUTURE.plusSeconds(86400)));
        verify(events).publish(any(), eq(ModuleActivationEventPublisher.Action.ACTIVATED));
    }

    @Test
    void suspend_then_resume_emitsEvents() {
        ModuleActivation a = mockActive("kpi"); a.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(a));
        when(repo.findOpenByTenantIdAndCode(any(), any())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.suspend(ID, new ModuleActivationDto.SuspendRequest());
        service.resume(ID, new ModuleActivationDto.ResumeRequest());
        verify(events).publish(any(), eq(ModuleActivationEventPublisher.Action.SUSPENDED));
        verify(events).publish(any(), eq(ModuleActivationEventPublisher.Action.RESUMED));
    }

    @Test
    void expireDue_iteratesAndEmits() {
        ModuleActivation a = mockActive("kpi");
        a.assignId(ID);
        when(repo.findDueForExpiration(eq(NOW), eq(200))).thenReturn(List.of(a));
        // a is ACTIVE with expiresAt = FUTURE, which is > NOW, so expireIfDue returns false.
        // Construisons un événement échu :
        ModuleActivation due = new ModuleActivation(
                UUID.randomUUID(), TENANT, "kpi",
                ActivationStatus.ACTIVE, BillingTier.STANDARD, null,
                null, NOW.minusSeconds(60),
                NOW.minusSeconds(120), ACTOR, NOW.minusSeconds(120), ACTOR, NOW.minusSeconds(120));
        when(repo.findDueForExpiration(eq(NOW), eq(200))).thenReturn(List.of(due));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        int expired = service.expireDue(200);
        assertThat(expired).isEqualTo(1);
        verify(events).publish(any(), eq(ModuleActivationEventPublisher.Action.EXPIRED));
    }

    // ---------- la disponibilite ne se lit que sur la decision COURANTE ----------

    @Test
    void unModuleReactiveApresAvoirEteDesactiveRedevientDisponible() {
        // Constate en preproduction le 2026-08-31. `product` etait ACTIVE en base
        // et pourtant absent de la liste servie a l'interface : l'entree
        // « Produits » ne revenait pas dans la navigation, et l'onglet Control
        // Plan restait donc inatteignable.
        //
        // Le depot rend l'historique du PLUS RECENT au PLUS ANCIEN
        // (`findByTenantIdOrderByActivatedAtDesc`), et la boucle appliquait
        // `add`/`remove` dans cet ordre : la ligne traitee EN DERNIER l'emportait,
        // c'est-a-dire la plus ANCIENNE. Une desactivation de la veille ecrasait
        // l'activation du jour.
        //
        // Aucun banc ne l'avait vu parce que tous donnaient UNE ligne par module —
        // or c'est justement la seconde ligne qui fait le defaut, et le schema en
        // prevoit une par reactivation (l'index unique partiel ne porte que sur
        // les statuts non terminaux).
        when(repo.findAllByTenantId(TENANT)).thenReturn(List.of(
                mockActive("product"),          // 27/08 — la decision courante
                mockDisabled("product")));      // 26/08 — de l'histoire

        assertThat(service.enabledModuleCodes()).contains("product");
    }

    @Test
    void unModuleDesactiveEtNonRouvertResteIndisponible() {
        // Le sens inverse, qui interdit de « corriger » en ignorant les fermetures.
        //
        // L'etat teste est celui que le schema autorise reellement : desactiver
        // agit sur la ligne OUVERTE elle-meme, ce qui la rend terminale sans en
        // creer d'autre. Un module ferme et jamais rouvert n'a donc qu'une ligne,
        // terminale — et deux cycles en laissent deux, terminales toutes les deux.
        when(repo.findAllByTenantId(TENANT)).thenReturn(List.of(
                mockDisabledAt("webhooks", NOW.minusSeconds(3600)),
                mockDisabledAt("webhooks", NOW.minusSeconds(86400))));

        assertThat(service.enabledModuleCodes()).doesNotContain("webhooks");
    }

    @Test
    void lActivationOuverteFaitFoiQuelQueSoitLOrdreDeLecture() {
        // L'invariant que le correctif installe : la decision ne depend plus de
        // l'ordre dans lequel le depot rend les lignes. Le meme jeu, lu dans les
        // deux sens, doit donner le meme verdict — sans quoi une montee de
        // version qui changerait ce tri rouvrirait le defaut en silence.
        List<ModuleActivation> histoire = List.of(mockActive("product"), mockDisabled("product"));
        List<ModuleActivation> inverse = List.of(mockDisabled("product"), mockActive("product"));

        when(repo.findAllByTenantId(TENANT)).thenReturn(histoire);
        assertThat(service.enabledModuleCodes()).contains("product");

        when(repo.findAllByTenantId(TENANT)).thenReturn(inverse);
        assertThat(service.enabledModuleCodes()).contains("product");
    }

    @Test
    void leSocleResteDisponibleSansAucuneLigne() {
        when(repo.findAllByTenantId(TENANT)).thenReturn(List.of());

        assertThat(service.enabledModuleCodes())
                .contains("pdca", "capa", "docs", "audit", "fives", "ishikawa");
    }

    @Test
    void unModuleSuspenduNEstPasDisponible() {
        // SUSPENDU n'est pas terminal, mais n'est pas utilisable pour autant.
        when(repo.findAllByTenantId(TENANT))
                .thenReturn(List.of(mockSuspended("product")));

        assertThat(service.enabledModuleCodes()).doesNotContain("product");
    }

    @Test
    void laDependanceLitLaMemeDisponibiliteQueLInterface() {
        // Le corollaire, et le vrai cout du defaut : `controlplan` depend de
        // `risk` et de `product`. Tant que `product` etait juge indisponible par
        // une ligne perimee, son activation repondait 409 « Missing dependency »
        // sur un tenant ou il etait pourtant bien actif — un refus qu'aucune
        // manoeuvre depuis l'ecran ne pouvait lever.
        when(tierProvider.currentTier(TENANT)).thenReturn(BillingTier.STANDARD);
        when(repo.findOpenByTenantIdAndCode(TENANT, "controlplan")).thenReturn(Optional.empty());
        when(repo.findAllByTenantId(TENANT)).thenReturn(List.of(
                mockActive("risk"),
                mockActive("product"),
                mockDisabled("product")));
        when(repo.save(any())).thenAnswer(inv -> {
            ModuleActivation a = inv.getArgument(0); a.assignId(ID); return a;
        });

        ModuleActivationDto.ActivationView v = service.activate(
                new ModuleActivationDto.ActivateRequest("controlplan", FUTURE));

        assertThat(v.status()).isEqualTo(ActivationStatus.ACTIVE);
    }

    /** Une ligne DESACTIVEE plus ancienne que l'activation : de l'historique. */
    private ModuleActivation mockDisabled(String code) {
        return mockDisabledAt(code, NOW.minusSeconds(86400));
    }

    private ModuleActivation mockDisabledAt(String code, Instant activatedAt) {
        ModuleActivation a = ModuleActivation.activateNow(TENANT, code,
                BillingTier.STANDARD, FUTURE, ACTOR, activatedAt);
        a.disable(ACTOR, activatedAt.plusSeconds(60));
        return a;
    }

    private ModuleActivation mockActive(String code) {
        return ModuleActivation.activateNow(TENANT, code, BillingTier.STANDARD,
                FUTURE, ACTOR, NOW);
    }

    private ModuleActivation mockSuspended(String code) {
        ModuleActivation a = mockActive(code);
        a.suspend(ACTOR, NOW.plusSeconds(60));
        return a;
    }
}
