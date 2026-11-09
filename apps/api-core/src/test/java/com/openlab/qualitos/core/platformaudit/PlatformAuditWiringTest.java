package com.openlab.qualitos.core.platformaudit;

import com.openlab.qualitos.core.billing.BillingPeriod;
import com.openlab.qualitos.core.billing.BillingTier;
import com.openlab.qualitos.core.billing.ModuleActivationPort;
import com.openlab.qualitos.core.billing.ModulePrice;
import com.openlab.qualitos.core.billing.ModulePriceDto;
import com.openlab.qualitos.core.billing.ModulePriceRepository;
import com.openlab.qualitos.core.billing.ModulePriceService;
import com.openlab.qualitos.core.billing.Money;
import com.openlab.qualitos.core.billing.Subscription;
import com.openlab.qualitos.core.billing.SubscriptionDto;
import com.openlab.qualitos.core.billing.SubscriptionRepository;
import com.openlab.qualitos.core.billing.SubscriptionService;
import com.openlab.qualitos.core.partner.EngagementScope;
import com.openlab.qualitos.core.partner.EngagementStatus;
import com.openlab.qualitos.core.partner.PartnerAccessService;
import com.openlab.qualitos.core.partner.PartnerConsultant;
import com.openlab.qualitos.core.partner.PartnerConsultantRepository;
import com.openlab.qualitos.core.partner.PartnerDto;
import com.openlab.qualitos.core.partner.PartnerEngagement;
import com.openlab.qualitos.core.partner.PartnerEngagementRepository;
import com.openlab.qualitos.core.partner.PartnerFirm;
import com.openlab.qualitos.core.partner.PartnerFirmRepository;
import com.openlab.qualitos.core.partner.PartnerFirmService;
import com.openlab.qualitos.core.tenant.Tenant;
import com.openlab.qualitos.core.tenant.TenantDto;
import com.openlab.qualitos.core.tenant.TenantRepository;
import com.openlab.qualitos.core.tenant.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Les actes sensibles de la plateforme arrivent bien dans la piste d'audit.
 *
 * <p><b>Pourquoi un banc à part, et pas une ligne ajoutée dans chaque banc de
 * service.</b> Ce qui se vérifie ici n'est pas le comportement d'un service,
 * c'est une propriété de la plateforme : la liste de ses actes consignés. Un
 * lecteur — un auditeur interne, le prochain développeur — doit pouvoir la
 * lire d'un trait, et constater qu'il n'y manque rien. Éparpillée dans cinq
 * bancs, elle ne se lit plus, et c'est comme ça qu'un acte finit par ne plus
 * être couvert sans que personne ne le remarque.
 *
 * <p>{@link PlatformAuditCoversSensitiveActsTest} attrape l'acte DÉCLARÉ mais
 * jamais branché, en lisant les sources. Ce banc-ci vérifie l'inverse : que
 * l'appel part bien, depuis le vrai service, avec le bon acte et le bon client
 * visé. Les deux sont nécessaires — le premier ne sait pas si l'appel est
 * correct, le second ne sait pas s'il en manque.
 */
@DisplayName("Les actes sensibles sont consignés")
class PlatformAuditWiringTest {

    private static final UUID CLIENT = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID CABINET = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
    private static final UUID ACCORD = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final UUID ACTEUR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final LocalDate AUJOURD_HUI = LocalDate.of(2026, 11, 8);
    private static final Clock HORLOGE =
            Clock.fixed(AUJOURD_HUI.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

    private PlatformAuditService auditTrail;

    @BeforeEach
    void setUp() {
        auditTrail = mock(PlatformAuditService.class);
    }

    @Nested
    @DisplayName("le dossier du client")
    class DossierClient {

        private TenantRepository tenants;
        private TenantService service;

        @BeforeEach
        void setUp() {
            tenants = mock(TenantRepository.class);
            service = new TenantService(tenants, auditTrail);
            when(tenants.save(any(Tenant.class))).thenAnswer(appel -> {
                Tenant client = appel.getArgument(0);
                if (client.getId() == null) {
                    client.setId(CLIENT);
                }
                return client;
            });
        }

        @Test
        @DisplayName("creer un client est consigne, avec son palier")
        void creation() {
            when(tenants.existsBySlug("metalux")).thenReturn(false);

            service.create(new TenantDto.CreateRequest("metalux", "Metalux SA", Tenant.Plan.PRO));

            verify(auditTrail).record(eq(PlatformAuditAction.TENANT_CREATED), eq("tenant"),
                    eq(CLIENT), eq(CLIENT), contains("Metalux SA"), contains("PRO"));
        }

        @Test
        @DisplayName("modifier un client consigne l'etat d'AVANT, pas seulement celui d'apres")
        void modification() {
            when(tenants.findById(CLIENT)).thenReturn(Optional.of(client()));

            service.update(CLIENT, new TenantDto.UpdateRequest("Metalux Groupe",
                    Tenant.Plan.ENTERPRISE, true));

            // Un journal qui ne dit que l'état d'arrivée laisse l'auditeur
            // incapable de voir ce qui a changé.
            verify(auditTrail).record(eq(PlatformAuditAction.TENANT_UPDATED), eq("tenant"),
                    eq(CLIENT), eq(CLIENT), contains("Metalux SA/PRO/actif -> "), anyString());
        }

        @Test
        @DisplayName("desactiver un client est consigne")
        void desactivation() {
            when(tenants.findById(CLIENT)).thenReturn(Optional.of(client()));

            service.deactivate(CLIENT);

            verify(auditTrail).record(eq(PlatformAuditAction.TENANT_DEACTIVATED), eq("tenant"),
                    eq(CLIENT), eq(CLIENT), contains("Metalux SA"), eq(null));
        }

        private Tenant client() {
            return Tenant.builder().id(CLIENT).slug("metalux").name("Metalux SA")
                    .plan(Tenant.Plan.PRO).active(true).build();
        }
    }

    @Nested
    @DisplayName("ce que le client peut utiliser")
    class Modules {

        private ModulePriceRepository prix;
        private ModulePriceService tarifs;
        private SubscriptionRepository abonnements;
        private ModuleActivationPort moteur;
        private TenantRepository tenants;
        private SubscriptionService service;

        @BeforeEach
        void setUp() {
            prix = mock(ModulePriceRepository.class);
            tarifs = mock(ModulePriceService.class);
            abonnements = mock(SubscriptionRepository.class);
            moteur = mock(ModuleActivationPort.class);
            tenants = mock(TenantRepository.class);
            service = new SubscriptionService(abonnements, tarifs, moteur, tenants, HORLOGE, auditTrail);
        }

        @Test
        @DisplayName("fixer un tarif est consigne, avec la valeur d'avant")
        void tarif() {
            ModulePriceService reel = new ModulePriceService(prix, HORLOGE, auditTrail);
            ModulePrice existant = ModulePrice.builder()
                    .id(UUID.randomUUID()).moduleCode("spc")
                    .billingTier(BillingTier.PRO).period(BillingPeriod.MONTHLY)
                    .amountCents(9900L).currency("EUR")
                    .updatedAt(Instant.parse("2026-01-01T00:00:00Z")).updatedBy(ACTEUR)
                    .build();
            when(prix.find("spc", BillingTier.PRO, BillingPeriod.MONTHLY))
                    .thenReturn(Optional.of(existant));
            when(prix.save(any(ModulePrice.class))).thenAnswer(appel -> appel.getArgument(0));

            reel.setPrice(new ModulePriceDto.SaveCommand("spc", BillingTier.PRO,
                    BillingPeriod.MONTHLY, 12900L, "EUR"), ACTEUR);

            // Un changement de tarif sans son ancienne valeur n'explique rien à
            // l'auditeur qui cherche pourquoi une facture a doublé.
            verify(auditTrail).record(eq(PlatformAuditAction.MODULE_PRICE_SET), eq("module_price"),
                    any(UUID.class), eq(null), contains("9900 EUR -> 12900 EUR"), anyString());
        }

        @Test
        @DisplayName("ouvrir un module est consigne, et attache au client")
        void ouverture() {
            when(tenants.existsById(CLIENT)).thenReturn(true);
            when(abonnements.findLiveByTenantAndModule(CLIENT, "spc")).thenReturn(Optional.empty());
            when(tarifs.priceOf("spc", BillingTier.PRO, BillingPeriod.MONTHLY))
                    .thenReturn(Optional.of(Money.of(9900L, "EUR")));
            when(abonnements.save(any(Subscription.class))).thenAnswer(appel -> appel.getArgument(0));

            service.subscribe(CLIENT, new SubscriptionDto.SubscribeCommand("spc",
                    BillingTier.PRO, BillingPeriod.MONTHLY), ACTEUR);

            verify(auditTrail).record(eq(PlatformAuditAction.MODULE_SUBSCRIBED), eq("subscription"),
                    any(UUID.class), eq(CLIENT), contains("spc"), anyString());
        }

        @Test
        @DisplayName("fermer un module est consigne, et attache au client")
        void fermeture() {
            UUID abonnement = UUID.randomUUID();
            Subscription vivant = Subscription.builder()
                    .id(abonnement).tenantId(CLIENT).moduleCode("spc")
                    .billingTier(BillingTier.PRO).period(BillingPeriod.MONTHLY)
                    .amountCents(9900L).currency("EUR")
                    .startedOn(AUJOURD_HUI.minusMonths(3)).nextRenewal(AUJOURD_HUI.plusMonths(1))
                    .createdAt(Instant.parse("2026-08-08T00:00:00Z")).createdBy(ACTEUR)
                    .build();
            when(abonnements.findById(abonnement)).thenReturn(Optional.of(vivant));
            when(abonnements.save(any(Subscription.class))).thenAnswer(appel -> appel.getArgument(0));

            service.cancel(CLIENT, abonnement, ACTEUR);

            verify(auditTrail).record(eq(PlatformAuditAction.MODULE_CANCELLED), eq("subscription"),
                    eq(abonnement), eq(CLIENT), contains("spc"), anyString());
        }
    }

    @Nested
    @DisplayName("les cabinets partenaires")
    class Cabinets {

        private PartnerFirmRepository cabinets;
        private PartnerConsultantRepository consultants;
        private PartnerEngagementRepository accords;
        private TenantRepository tenants;
        private PartnerFirmService cote_editeur;
        private PartnerAccessService cote_client;

        @BeforeEach
        void setUp() {
            cabinets = mock(PartnerFirmRepository.class);
            consultants = mock(PartnerConsultantRepository.class);
            accords = mock(PartnerEngagementRepository.class);
            tenants = mock(TenantRepository.class);
            cote_editeur = new PartnerFirmService(cabinets, consultants, accords, tenants,
                    HORLOGE, auditTrail);
            cote_client = new PartnerAccessService(cabinets, consultants, accords, tenants,
                    HORLOGE, auditTrail);
            when(cabinets.save(any(PartnerFirm.class))).thenAnswer(appel -> {
                PartnerFirm cabinet = appel.getArgument(0);
                if (cabinet.getId() == null) {
                    cabinet.setId(CABINET);
                }
                return cabinet;
            });
            // Le dépôt réel attribue l'identifiant à l'insertion. Le rendre
            // tel quel laisserait un accord sans identifiant, donc une ligne
            // de piste d'audit qui ne désigne rien.
            when(accords.save(any(PartnerEngagement.class))).thenAnswer(appel -> {
                PartnerEngagement accord = appel.getArgument(0);
                if (accord.getId() == null) {
                    accord.setId(ACCORD);
                }
                return accord;
            });
        }

        @Test
        @DisplayName("creer un cabinet est consigne")
        void creation() {
            when(cabinets.existsBySlug("cabinet-qualite")).thenReturn(false);

            cote_editeur.create(new PartnerDto.FirmRequest("cabinet-qualite",
                    "Cabinet Qualite", null, null, null));

            verify(auditTrail).record(eq(PlatformAuditAction.PARTNER_FIRM_CREATED),
                    eq("partner_firm"), eq(CABINET), eq(null),
                    contains("Cabinet Qualite"), anyString());
        }

        @Test
        @DisplayName("suspendre un cabinet est consigne : l'acces de tous ses consultants tombe")
        void suspension() {
            when(cabinets.findById(CABINET)).thenReturn(Optional.of(cabinet()));

            cote_editeur.setActive(CABINET, false);

            verify(auditTrail).record(eq(PlatformAuditAction.PARTNER_FIRM_ACTIVE_CHANGED),
                    eq("partner_firm"), eq(CABINET), eq(null),
                    contains("suspendu"), contains("false"));
        }

        @Test
        @DisplayName("rattacher un consultant est consigne")
        void consultant_ajoute() {
            when(cabinets.findById(CABINET)).thenReturn(Optional.of(cabinet()));
            when(consultants.existsByFirmIdAndEmailIgnoreCase(eq(CABINET), anyString()))
                    .thenReturn(false);
            when(consultants.save(any(PartnerConsultant.class))).thenAnswer(appel -> {
                PartnerConsultant fiche = appel.getArgument(0);
                fiche.setId(UUID.randomUUID());
                return fiche;
            });

            cote_editeur.addConsultant(CABINET,
                    new PartnerDto.ConsultantRequest("Camille Roux", "camille@cabinet.fr"));

            verify(auditTrail).record(eq(PlatformAuditAction.PARTNER_CONSULTANT_ADDED),
                    eq("partner_consultant"), any(UUID.class), eq(null),
                    contains("Camille Roux"), anyString());
        }

        @Test
        @DisplayName("desactiver un consultant est consigne")
        void consultant_desactive() {
            UUID fiche = UUID.randomUUID();
            when(cabinets.findById(CABINET)).thenReturn(Optional.of(cabinet()));
            when(consultants.findById(fiche)).thenReturn(Optional.of(
                    PartnerConsultant.builder().id(fiche).firmId(CABINET)
                            .displayName("Camille Roux").email("camille@cabinet.fr")
                            .active(true).build()));
            when(consultants.save(any(PartnerConsultant.class)))
                    .thenAnswer(appel -> appel.getArgument(0));

            cote_editeur.setConsultantActive(CABINET, fiche, false);

            verify(auditTrail).record(eq(PlatformAuditAction.PARTNER_CONSULTANT_ACTIVE_CHANGED),
                    eq("partner_consultant"), eq(fiche), eq(null),
                    contains("desactive"), anyString());
        }

        @Test
        @DisplayName("proposer une mission est consigne, et attache au client vise")
        void proposition() {
            when(cabinets.findById(CABINET)).thenReturn(Optional.of(cabinet()));
            when(tenants.findById(CLIENT)).thenReturn(Optional.of(
                    Tenant.builder().id(CLIENT).slug("metalux").name("Metalux SA").build()));
            when(accords.findLive(CLIENT, CABINET)).thenReturn(Optional.empty());

            cote_editeur.propose(CABINET,
                    new PartnerDto.ProposeRequest(CLIENT, EngagementScope.READ, "Audit blanc"),
                    ACTEUR);

            verify(auditTrail).record(eq(PlatformAuditAction.PARTNER_ENGAGEMENT_PROPOSED),
                    eq("partner_engagement"), any(UUID.class), eq(CLIENT),
                    contains("Metalux SA"), anyString());
        }

        @Test
        @DisplayName("ACCORDER un acces a un tiers est consigne, avec sa portee et son terme")
        void acces_accorde() {
            // L'acte le plus sensible de tout api-core : la première ligne
            // qu'une autorité cherchera.
            when(accords.findById(ACCORD)).thenReturn(Optional.of(accord(EngagementStatus.PROPOSED)));
            when(cabinets.findById(CABINET)).thenReturn(Optional.of(cabinet()));

            cote_client.grant(CLIENT, ACCORD, new PartnerDto.GrantRequest(
                    EngagementScope.READ, AUJOURD_HUI.plusMonths(6), "Audit blanc"), ACTEUR);

            verify(auditTrail).record(eq(PlatformAuditAction.PARTNER_ENGAGEMENT_GRANTED),
                    eq("partner_engagement"), eq(ACCORD), eq(CLIENT),
                    contains("portee READ"), contains("2027-05-08"));
        }

        @Test
        @DisplayName("refuser une mission est consigne")
        void acces_refuse() {
            when(accords.findById(ACCORD)).thenReturn(Optional.of(accord(EngagementStatus.PROPOSED)));
            when(cabinets.findById(CABINET)).thenReturn(Optional.of(cabinet()));

            cote_client.decline(CLIENT, ACCORD, ACTEUR);

            verify(auditTrail).record(eq(PlatformAuditAction.PARTNER_ENGAGEMENT_DECLINED),
                    eq("partner_engagement"), eq(ACCORD), eq(CLIENT), anyString(), anyString());
        }

        @Test
        @DisplayName("RETIRER un acces est consigne")
        void acces_retire() {
            when(accords.findById(ACCORD)).thenReturn(Optional.of(accord(EngagementStatus.GRANTED)));
            when(cabinets.findById(CABINET)).thenReturn(Optional.of(cabinet()));

            cote_client.revoke(CLIENT, ACCORD, ACTEUR);

            verify(auditTrail).record(eq(PlatformAuditAction.PARTNER_ENGAGEMENT_REVOKED),
                    eq("partner_engagement"), eq(ACCORD), eq(CLIENT), anyString(), anyString());
        }

        private PartnerFirm cabinet() {
            return PartnerFirm.builder().id(CABINET).slug("cabinet-qualite")
                    .name("Cabinet Qualite").active(true).build();
        }

        private PartnerEngagement accord(EngagementStatus statut) {
            return PartnerEngagement.builder().id(ACCORD).tenantId(CLIENT).firmId(CABINET)
                    .scope(EngagementScope.READ).status(statut)
                    .proposedBy(ACTEUR).proposedAt(Instant.parse("2026-11-01T00:00:00Z"))
                    .startsOn(statut == EngagementStatus.GRANTED ? AUJOURD_HUI.minusDays(10) : null)
                    .endsOn(statut == EngagementStatus.GRANTED ? AUJOURD_HUI.plusMonths(3) : null)
                    .build();
        }
    }
}
