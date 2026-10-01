package com.openlab.qualitos.quality.riskregister.application;

import com.openlab.qualitos.quality.riskregister.domain.Opportunity;
import com.openlab.qualitos.quality.riskregister.domain.OpportunityAction;
import com.openlab.qualitos.quality.riskregister.domain.OpportunityActionStatus;
import com.openlab.qualitos.quality.riskregister.domain.OpportunityLevel;
import com.openlab.qualitos.quality.riskregister.domain.OpportunityStatus;
import com.openlab.qualitos.quality.riskregister.domain.RegisterEvent;
import com.openlab.qualitos.quality.riskregister.domain.RegisterEventType;
import com.openlab.qualitos.quality.riskregister.domain.RegisterItemKind;
import com.openlab.qualitos.quality.riskregister.domain.RegisterNotFoundException;
import com.openlab.qualitos.quality.riskregister.domain.RegisterOrigin;
import com.openlab.qualitos.quality.riskregister.domain.RegisterRepositories;
import com.openlab.qualitos.quality.riskregister.domain.RegisterRequirement;
import com.openlab.qualitos.quality.riskregister.domain.RegisterType;
import com.openlab.qualitos.quality.riskregister.domain.RegisterValidationException;
import com.openlab.qualitos.quality.riskregister.domain.Risk;
import com.openlab.qualitos.quality.riskregister.domain.RiskDecision;
import com.openlab.qualitos.quality.riskregister.domain.RiskLevel;
import com.openlab.qualitos.quality.riskregister.domain.RiskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Le service sur des dépôts en mémoire : l'isolation par client, l'attribution
 * des références, le suivi et le lien vers la CAPA s'y vérifient sans base.
 */
class RiskRegisterServiceTest {

    static final UUID TENANT_A = UUID.randomUUID();
    static final UUID TENANT_B = UUID.randomUUID();
    static final UUID ACTEUR = UUID.randomUUID();
    static final Instant MAINTENANT = Instant.parse("2026-10-01T09:00:00Z");

    FakeRisks risks;
    FakeOpportunities opportunities;
    FakeActions actions;
    FakeEvents events;
    FakeCapas capas;
    FakeSources sources;
    FakeContext context;
    RegisterAuditPublisher audit;
    RiskRegisterService service;

    @BeforeEach
    void setUp() {
        risks = new FakeRisks();
        opportunities = new FakeOpportunities();
        actions = new FakeActions();
        events = new FakeEvents();
        capas = new FakeCapas();
        context = new FakeContext();
        audit = mock(RegisterAuditPublisher.class);
        sources = new FakeSources();
        service = new RiskRegisterService(risks, opportunities, actions, events, capas, sources, context, audit,
                Clock.fixed(MAINTENANT, ZoneOffset.UTC));
    }

    static RiskRegisterDto.RiskCommand risque(String titre, int g, int p) {
        return new RiskRegisterDto.RiskCommand(titre, RegisterType.QUALITY, "Production", "Usine A",
                "M. Kone", "Usure buse", "Soudure KO", RegisterOrigin.FMEA, "AMDEC-12", null, g, p, null, null,
                null, null, List.of(RegisterRequirement.ISO_9001_6_1), null, null);
    }

    static RiskRegisterDto.RiskCommand revision(int g, int p, Integer rg, Integer rp, RiskStatus statut) {
        return new RiskRegisterDto.RiskCommand("Dérive soudure", RegisterType.QUALITY, "Production", null,
                "M. Kone", null, null, RegisterOrigin.FMEA, "AMDEC-12", null, g, p, rg, rp, RiskDecision.REDUCE,
                statut, List.of(RegisterRequirement.IATF_16949_6_1_2, RegisterRequirement.ISO_9001_6_1),
                LocalDate.of(2027, 1, 15), "Cpk > 1,33");
    }

    static RiskRegisterDto.OpportunityCommand opportunite(String titre, int gain, int faisabilite) {
        return new RiskRegisterDto.OpportunityCommand(titre, RegisterType.QUALITY, "Production", null,
                "Mme Diallo", LocalDate.of(2027, 3, 31), "Contexte", "Bénéfice", RegisterOrigin.AUDIT,
                "AUD-7", gain, faisabilite, null, null, List.of(RegisterRequirement.ISO_9001_10_3), null);
    }

    // ---------- risques ----------

    @Test
    void lesReferencesSeSuiventParClient() {
        assertThat(service.createRisk(risque("Un", 3, 3)).reference()).isEqualTo("R-001");
        assertThat(service.createRisk(risque("Deux", 3, 3)).reference()).isEqualTo("R-002");

        context.tenant = TENANT_B;
        assertThat(service.createRisk(risque("Autre client", 3, 3)).reference()).isEqualTo("R-001");
    }

    @Test
    void uneReferenceDejaPriseEstSautee() {
        service.createRisk(risque("Premier", 2, 2));
        // Un compte faux (ligne disparue, saisie concurrente) : R-001 est déjà prise.
        risks.countOverride = 0L;

        assertThat(service.createRisk(risque("Suivant", 3, 3)).reference()).isEqualTo("R-002");
    }

    @Test
    void creerRendLaVueCalculeeTraceLaCreationEtAudite() {
        RiskRegisterDto.RiskView vue = service.createRisk(risque("Dérive soudure", 4, 3));

        assertThat(vue.grossScore()).isEqualTo(12);
        assertThat(vue.grossLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(vue.residualScore()).isNull();
        assertThat(vue.status()).isEqualTo(RiskStatus.TO_TREAT);
        assertThat(vue.requirements()).containsExactly(RegisterRequirement.ISO_9001_6_1);

        assertThat(events.all).singleElement().satisfies(e -> {
            assertThat(e.type()).isEqualTo(RegisterEventType.CREATED);
            assertThat(e.toValue()).isEqualTo("FMEA");
            assertThat(e.detail()).isEqualTo("AMDEC-12");
            assertThat(e.actorId()).isEqualTo(ACTEUR);
            assertThat(e.at()).isEqualTo(MAINTENANT);
        });
        verify(audit).riskRecorded(any(Risk.class), eq(ACTEUR));
    }

    @Test
    void reviserTraceChaqueChangementEtAudite() {
        UUID id = service.createRisk(risque("Dérive soudure", 3, 3)).id();
        events.decaler = false;

        RiskRegisterDto.RiskView vue = service.reviseRisk(id, revision(4, 3, 4, 2, RiskStatus.IN_TREATMENT));
        // Une microseconde d'écart par changement d'une même révision : l'ordre ne dépend pas de la base.
        assertThat(events.all.subList(1, 5)).extracting(RegisterEvent::at).isSorted().doesNotHaveDuplicates();

        assertThat(vue.residualScore()).isEqualTo(8);
        assertThat(vue.residualLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(vue.requirements()).containsExactly(
                RegisterRequirement.ISO_9001_6_1, RegisterRequirement.IATF_16949_6_1_2);
        assertThat(events.all).extracting(RegisterEvent::type).containsExactly(
                RegisterEventType.CREATED, RegisterEventType.RATING_CHANGED, RegisterEventType.RESIDUAL_CHANGED,
                RegisterEventType.STATUS_CHANGED, RegisterEventType.DECISION_CHANGED);
        verify(audit).riskRevised(any(Risk.class), eq(ACTEUR));
    }

    @Test
    void laFicheRendLaLigneSesCapaEtSonSuiviDuPlusRecent() {
        UUID id = service.createRisk(risque("Dérive soudure", 3, 3)).id();
        service.reviseRisk(id, revision(4, 3, null, null, RiskStatus.TO_TREAT));
        capas.lies.add(new RiskCapaGateway.LinkedCapa(UUID.randomUUID(), "Carte SPC", LocalDate.of(2026, 11, 1),
                "IN_PROGRESS"));

        RiskRegisterDto.RiskSheet fiche = service.risk(id);

        assertThat(fiche.risk().reference()).isEqualTo("R-001");
        assertThat(fiche.capas()).singleElement().satisfies(c -> {
            assertThat(c.title()).isEqualTo("Carte SPC");
            assertThat(c.status()).isEqualTo("IN_PROGRESS");
        });
        assertThat(fiche.events()).extracting(RiskRegisterDto.EventView::type)
                .containsExactly(RegisterEventType.DECISION_CHANGED, RegisterEventType.RATING_CHANGED,
                        RegisterEventType.CREATED);
    }

    @Test
    void unAutreClientNeVoitNiNeTouchePasLaFiche() {
        UUID id = service.createRisk(risque("Secret", 3, 3)).id();

        context.tenant = TENANT_B;
        assertThat(service.risks()).isEmpty();
        assertThatThrownBy(() -> service.risk(id)).isInstanceOf(RegisterNotFoundException.class);
        assertThatThrownBy(() -> service.reviseRisk(id, revision(4, 3, null, null, null)))
                .isInstanceOf(RegisterNotFoundException.class);
        assertThatThrownBy(() -> service.openCapa(id, new RiskRegisterDto.CapaCommand("x", null, null)))
                .isInstanceOf(RegisterNotFoundException.class);
        assertThat(capas.ouvertes).isEmpty();
    }

    @Test
    void laListeEstRangeeParReference() {
        service.createRisk(risque("Un", 1, 1));
        service.createRisk(risque("Deux", 5, 5));
        assertThat(service.risks()).extracting(RiskRegisterDto.RiskView::reference)
                .containsExactly("R-001", "R-002");
    }

    @Test
    void ouvrirUneCapaPasseParLaPasserelleTraceEtAudite() {
        UUID id = service.createRisk(risque("Dérive soudure", 4, 3)).id();

        RiskRegisterDto.CapaView capa = service.openCapa(id,
                new RiskRegisterDto.CapaCommand("  Carte SPC sur le cordon ", " ", LocalDate.of(2026, 12, 1)));

        assertThat(capa.title()).isEqualTo("Carte SPC sur le cordon");
        assertThat(capas.ouvertes).singleElement().satisfies(o -> {
            assertThat(o.title()).isEqualTo("Carte SPC sur le cordon");
            assertThat(o.description()).isNull();
            assertThat(o.owner()).isEqualTo(ACTEUR);
            assertThat(o.risk().getReference()).isEqualTo("R-001");
        });
        assertThat(events.all.get(events.all.size() - 1)).satisfies(e -> {
            assertThat(e.type()).isEqualTo(RegisterEventType.ACTION_OPENED);
            assertThat(e.detail()).isEqualTo("Carte SPC sur le cordon");
        });
        verify(audit).riskCapaOpened(any(Risk.class), eq(capa.id()), eq(ACTEUR));
    }

    @Test
    void unRisqueClosNOuvrePlusDeCapa() {
        UUID id = service.createRisk(risque("Dérive soudure", 4, 3)).id();
        service.reviseRisk(id, revision(4, 3, null, null, RiskStatus.CLOSED));

        assertThatThrownBy(() -> service.openCapa(id, new RiskRegisterDto.CapaCommand("x", null, null)))
                .isInstanceOf(RegisterValidationException.class)
                .extracting("field").isEqualTo("status");
        assertThat(capas.ouvertes).isEmpty();
    }

    @Test
    void uneCapaSansIntituleOuTropLongueEstRefusee() {
        UUID id = service.createRisk(risque("Dérive soudure", 4, 3)).id();

        assertThatThrownBy(() -> service.openCapa(id, new RiskRegisterDto.CapaCommand(" ", null, null)))
                .extracting("field").isEqualTo("title");
        assertThatThrownBy(() -> service.openCapa(id, null)).extracting("field").isEqualTo("title");
        assertThatThrownBy(() -> service.openCapa(id, new RiskRegisterDto.CapaCommand("t",
                "d".repeat(RiskRegisterService.CAPA_DESCRIPTION_MAX + 1), null)))
                .extracting("field").isEqualTo("description");
        verify(audit, never()).riskCapaOpened(any(), any(), any());
    }

    @Test
    void uneCommandeAbsenteEstRefuseeSurLIntitule() {
        assertThatThrownBy(() -> service.createRisk(null)).extracting("field").isEqualTo("title");
        assertThatThrownBy(() -> service.createOpportunity(null)).extracting("field").isEqualTo("title");
    }

    // ---------- depuis une source ----------

    static final UUID LIGNE_AMDEC = UUID.randomUUID();

    static RiskRegisterDto.RiskCommand depuisAmdec(UUID source, String refTapee) {
        return new RiskRegisterDto.RiskCommand("Dérive soudure", RegisterType.QUALITY, "Production", null,
                "M. Kone", null, null, RegisterOrigin.FMEA, refTapee, source, 4, 3, null, null, null, null,
                List.of(), null, null);
    }

    @Test
    void leBrouillonVientDeLaSourceEtListeLesRisquesDejaIssus() {
        sources.objets.put(LIGNE_AMDEC, new RiskSourceCatalog.SourceDraft("PFMEA-7 #3", "Cordon poreux",
                "Buse usée", "Fuite", 4, 3, null, true, null));

        RiskRegisterDto.RiskDraft vide = service.draft(RegisterOrigin.FMEA, LIGNE_AMDEC);
        assertThat(vide.title()).isEqualTo("Cordon poreux");
        assertThat(vide.originRef()).isEqualTo("PFMEA-7 #3");
        assertThat(vide.eligible()).isTrue();
        assertThat(vide.existing()).isEmpty();

        service.createRisk(depuisAmdec(LIGNE_AMDEC, null));
        assertThat(service.draft(RegisterOrigin.FMEA, LIGNE_AMDEC).existing())
                .extracting(RiskRegisterDto.RiskLink::reference).containsExactly("R-001");
    }

    @Test
    void laReferenceDOrigineEstCelleDeLaSourcePasCelleDuCorps() {
        sources.objets.put(LIGNE_AMDEC, new RiskSourceCatalog.SourceDraft("PFMEA-7 #3", "x", null, null,
                4, 3, null, true, null));

        RiskRegisterDto.RiskView vue = service.createRisk(depuisAmdec(LIGNE_AMDEC, "AMDEC-INVENTEE"));

        assertThat(vue.originRef()).isEqualTo("PFMEA-7 #3");
        assertThat(vue.sourceId()).isEqualTo(LIGNE_AMDEC);
    }

    @Test
    void uneSourceSousLeSeuilNeDevientPasUnRisque() {
        sources.objets.put(LIGNE_AMDEC, new RiskSourceCatalog.SourceDraft("PFMEA-7 #3", "x", null, null,
                1, 1, null, false, "BELOW_THRESHOLD"));

        assertThatThrownBy(() -> service.createRisk(depuisAmdec(LIGNE_AMDEC, null)))
                .isInstanceOf(RegisterValidationException.class)
                .extracting("field").isEqualTo("sourceId");
        assertThat(risks.rows).isEmpty();
    }

    @Test
    void uneSourceInconnueOuDUnAutreClientRend404() {
        sources.objets.put(LIGNE_AMDEC, new RiskSourceCatalog.SourceDraft("PFMEA-7 #3", "x", null, null,
                4, 3, null, true, null));
        UUID inconnue = UUID.randomUUID();

        assertThatThrownBy(() -> service.createRisk(depuisAmdec(inconnue, null)))
                .isInstanceOf(RegisterNotFoundException.class);

        context.tenant = TENANT_B;
        assertThatThrownBy(() -> service.draft(RegisterOrigin.FMEA, LIGNE_AMDEC))
                .isInstanceOf(RegisterNotFoundException.class);
        assertThatThrownBy(() -> service.createRisk(depuisAmdec(LIGNE_AMDEC, null)))
                .isInstanceOf(RegisterNotFoundException.class);
    }

    @Test
    void uneOrigineSansObjetNePeutPasPorterDeSource() {
        RiskRegisterDto.RiskCommand directe = new RiskRegisterDto.RiskCommand("x", RegisterType.QUALITY, "P",
                null, "O", null, null, RegisterOrigin.DIRECT, null, UUID.randomUUID(), 2, 2, null, null, null,
                null, null, null, null);

        assertThatThrownBy(() -> service.createRisk(directe)).extracting("field").isEqualTo("origin");
        assertThatThrownBy(() -> service.draft(RegisterOrigin.MONITORING, UUID.randomUUID()))
                .extracting("field").isEqualTo("origin");
        assertThatThrownBy(() -> service.draft(null, UUID.randomUUID())).extracting("field").isEqualTo("origin");
        assertThatThrownBy(() -> service.draft(RegisterOrigin.FMEA, null)).extracting("field").isEqualTo("sourceId");
    }

    @Test
    void reviserNeDetachePasUnRisqueDeSaSource() {
        sources.objets.put(LIGNE_AMDEC, new RiskSourceCatalog.SourceDraft("PFMEA-7 #3", "x", null, null,
                4, 3, null, true, null));
        UUID id = service.createRisk(depuisAmdec(LIGNE_AMDEC, null)).id();
        RiskRegisterDto.RiskCommand detache = new RiskRegisterDto.RiskCommand("Dérive", RegisterType.QUALITY,
                "Production", null, "M. Kone", null, null, RegisterOrigin.DIRECT, "AUTRE", null, 4, 3, null, null,
                null, null, null, null, null);

        RiskRegisterDto.RiskView vue = service.reviseRisk(id, detache);

        assertThat(vue.origin()).isEqualTo(RegisterOrigin.FMEA);
        assertThat(vue.originRef()).isEqualTo("PFMEA-7 #3");
        assertThat(vue.sourceId()).isEqualTo(LIGNE_AMDEC);
        assertThat(vue.title()).isEqualTo("Dérive");
    }

    // ---------- opportunités ----------

    @Test
    void uneOpportuniteSeCreeSeReviseEtSeLit() {
        RiskRegisterDto.OpportunityView vue = service.createOpportunity(opportunite("Automatiser SPC", 4, 4));
        assertThat(vue.reference()).isEqualTo("O-001");
        assertThat(vue.score()).isEqualTo(16);
        assertThat(vue.level()).isEqualTo(OpportunityLevel.PRIORITY);
        verify(audit).opportunityRecorded(any(Opportunity.class), eq(ACTEUR));

        RiskRegisterDto.OpportunityCommand revue = new RiskRegisterDto.OpportunityCommand("Automatiser SPC",
                RegisterType.QUALITY, "Production", null, "Mme Diallo", null, null, null, RegisterOrigin.AUDIT,
                null, 4, 3, null, OpportunityStatus.PLANNED, List.of(), null);
        assertThat(service.reviseOpportunity(vue.id(), revue).level()).isEqualTo(OpportunityLevel.HIGH);
        verify(audit).opportunityRevised(any(Opportunity.class), eq(ACTEUR));

        RiskRegisterDto.OpportunitySheet fiche = service.opportunity(vue.id());
        assertThat(fiche.events()).extracting(RiskRegisterDto.EventView::type).containsExactly(
                RegisterEventType.STATUS_CHANGED, RegisterEventType.RATING_CHANGED, RegisterEventType.CREATED);
        assertThat(service.opportunities()).hasSize(1);
    }

    @Test
    void lesActionsSeNumerotentDansLeClientEtSeRangentParNumero() {
        UUID o1 = service.createOpportunity(opportunite("Une", 3, 3)).id();
        UUID o2 = service.createOpportunity(opportunite("Deux", 3, 3)).id();

        RiskRegisterDto.ActionView a1 = service.addAction(o1, new RiskRegisterDto.ActionCommand("Chiffrer", null, null));
        RiskRegisterDto.ActionView a2 = service.addAction(o2, new RiskRegisterDto.ActionCommand("Piloter", null, null));
        RiskRegisterDto.ActionView a3 = service.addAction(o1,
                new RiskRegisterDto.ActionCommand("Déployer", LocalDate.of(2027, 1, 1), OpportunityActionStatus.IN_PROGRESS));

        assertThat(List.of(a1.number(), a2.number(), a3.number())).containsExactly(1, 2, 3);
        assertThat(service.opportunity(o1).actions()).extracting(RiskRegisterDto.ActionView::number)
                .containsExactly(1, 3);
        assertThat(events.all).filteredOn(e -> e.type() == RegisterEventType.ACTION_OPENED)
                .extracting(RegisterEvent::toValue).containsExactly("ACT-1", "ACT-2", "ACT-3");
        verify(audit, org.mockito.Mockito.times(3)).actionRecorded(any(OpportunityAction.class), eq(ACTEUR));
    }

    @Test
    void uneActionSeReviseEtSeSupprime() {
        UUID o = service.createOpportunity(opportunite("Une", 3, 3)).id();
        UUID a = service.addAction(o, new RiskRegisterDto.ActionCommand("Chiffrer", null, null)).id();

        RiskRegisterDto.ActionView revue = service.reviseAction(o, a,
                new RiskRegisterDto.ActionCommand("Chiffrer le raccordement", null, OpportunityActionStatus.DONE));
        assertThat(revue.status()).isEqualTo(OpportunityActionStatus.DONE);
        verify(audit).actionRevised(any(OpportunityAction.class), eq(ACTEUR));

        service.deleteAction(o, a);
        assertThat(service.opportunity(o).actions()).isEmpty();
        verify(audit).actionDeleted(any(OpportunityAction.class), eq(ACTEUR));
    }

    @Test
    void uneActionNeSAtteintQueParSonOpportunite() {
        UUID o1 = service.createOpportunity(opportunite("Une", 3, 3)).id();
        UUID o2 = service.createOpportunity(opportunite("Deux", 3, 3)).id();
        UUID a = service.addAction(o1, new RiskRegisterDto.ActionCommand("Chiffrer", null, null)).id();
        RiskRegisterDto.ActionCommand c = new RiskRegisterDto.ActionCommand("x", null, null);

        assertThatThrownBy(() -> service.reviseAction(o2, a, c)).isInstanceOf(RegisterNotFoundException.class);
        assertThatThrownBy(() -> service.deleteAction(o2, a)).isInstanceOf(RegisterNotFoundException.class);
        assertThatThrownBy(() -> service.addAction(o1, null)).extracting("field").isEqualTo("title");
        assertThatThrownBy(() -> service.reviseAction(o1, a, null)).extracting("field").isEqualTo("title");

        context.tenant = TENANT_B;
        assertThatThrownBy(() -> service.reviseAction(o1, a, c)).isInstanceOf(RegisterNotFoundException.class);
        assertThatThrownBy(() -> service.addAction(o1, c)).isInstanceOf(RegisterNotFoundException.class);
        assertThatThrownBy(() -> service.opportunity(o1)).isInstanceOf(RegisterNotFoundException.class);
        assertThat(service.opportunities()).isEmpty();
    }

    // ---------- saisie assistée ----------

    @Test
    void lesSuggestionsSontDedoubleesSansCasseEtGardentLaPremiereOrthographe() {
        service.createRisk(risque("Un", 3, 3));
        RiskRegisterDto.RiskCommand casse = new RiskRegisterDto.RiskCommand("Deux", RegisterType.QUALITY,
                "  production ", null, "m.  kone", null, null, null, null, null, 2, 2, null, null, null, null,
                null, null, null);
        service.createRisk(casse);
        service.createOpportunity(opportunite("Opp", 3, 3));

        RiskRegisterDto.Suggestions s = service.suggestions();

        assertThat(s.processes()).containsExactly("Production");
        assertThat(s.sites()).containsExactly("Usine A");
        // « m.  kone » se normalise en « m. kone » : c'est le même propriétaire que « M. Kone ».
        assertThat(s.owners()).containsExactly("M. Kone", "Mme Diallo");

        context.tenant = TENANT_B;
        assertThat(service.suggestions().processes()).isEmpty();
    }

    @Test
    void prochaineReferenceSautePlusieursTrous() {
        assertThat(RiskRegisterService.prochaine("R-", 0, ref -> ref.equals("R-001") || ref.equals("R-002")))
                .isEqualTo("R-003");
        assertThat(RiskRegisterService.prochaine("O-", 41, ref -> false)).isEqualTo("O-042");
        assertThat(RiskRegisterService.prochaine("R-", 999, ref -> false)).isEqualTo("R-1000");
    }

    // ---------- doublures ----------

    static final class FakeContext implements RegisterContext {
        UUID tenant = TENANT_A;

        @Override public UUID requireTenantId() { return tenant; }
        @Override public UUID requireActorId() { return ACTEUR; }
    }

    static final class FakeRisks implements RegisterRepositories.Risks {
        final Map<UUID, Risk> rows = new LinkedHashMap<>();
        Long countOverride;

        @Override
        public Risk save(Risk r) {
            Risk stored = r.getId() != null ? r : new Risk(UUID.randomUUID(), r.getTenantId(), r.getReference(),
                    r.getSourceId(), r.getIdentification(), r.getCause(), r.getEffect(), r.getGross(), r.getResidual(),
                    r.getDecision(), r.getStatus(), r.getNextReviewOn(), r.getEffectivenessCriterion(),
                    r.getCreatedBy(), r.getCreatedAt(), r.getUpdatedAt());
            rows.put(stored.getId(), stored);
            return stored;
        }

        @Override
        public Optional<Risk> findByIdAndTenant(UUID id, UUID tenantId) {
            return Optional.ofNullable(rows.get(id)).filter(r -> r.getTenantId().equals(tenantId));
        }

        @Override
        public List<Risk> findByTenant(UUID tenantId) {
            // Ordre inverse de l'insertion : le service doit trier lui-même.
            List<Risk> l = new ArrayList<>(rows.values().stream().filter(r -> r.getTenantId().equals(tenantId)).toList());
            java.util.Collections.reverse(l);
            return l;
        }

        @Override
        public long countByTenant(UUID tenantId) {
            return countOverride != null ? countOverride : findByTenant(tenantId).size();
        }

        @Override
        public boolean referenceTaken(UUID tenantId, String reference) {
            return findByTenant(tenantId).stream().anyMatch(r -> r.getReference().equals(reference));
        }

        @Override
        public List<Risk> findBySource(UUID tenantId, RegisterOrigin origin, UUID sourceId) {
            return findByTenant(tenantId).stream()
                    .filter(r -> r.getIdentification().origin() == origin && sourceId.equals(r.getSourceId()))
                    .toList();
        }
    }

    static final class FakeOpportunities implements RegisterRepositories.Opportunities {
        final Map<UUID, Opportunity> rows = new LinkedHashMap<>();

        @Override
        public Opportunity save(Opportunity o) {
            Opportunity stored = o.getId() != null ? o : new Opportunity(UUID.randomUUID(), o.getTenantId(),
                    o.getReference(), o.getIdentification(), o.getTargetDate(), o.getContext(), o.getBenefit(),
                    o.getEvaluation(), o.getDecision(), o.getStatus(), o.getBenefitCriterion(), o.getCreatedBy(),
                    o.getCreatedAt(), o.getUpdatedAt());
            rows.put(stored.getId(), stored);
            return stored;
        }

        @Override
        public Optional<Opportunity> findByIdAndTenant(UUID id, UUID tenantId) {
            return Optional.ofNullable(rows.get(id)).filter(o -> o.getTenantId().equals(tenantId));
        }

        @Override
        public List<Opportunity> findByTenant(UUID tenantId) {
            return rows.values().stream().filter(o -> o.getTenantId().equals(tenantId)).toList();
        }

        @Override
        public long countByTenant(UUID tenantId) {
            return findByTenant(tenantId).size();
        }

        @Override
        public boolean referenceTaken(UUID tenantId, String reference) {
            return findByTenant(tenantId).stream().anyMatch(o -> o.getReference().equals(reference));
        }
    }

    static final class FakeActions implements RegisterRepositories.Actions {
        final Map<UUID, OpportunityAction> rows = new LinkedHashMap<>();

        @Override
        public OpportunityAction save(OpportunityAction a) {
            OpportunityAction stored = a.getId() != null ? a : new OpportunityAction(UUID.randomUUID(),
                    a.getTenantId(), a.getOpportunityId(), a.getNumber(), a.getTitle(), a.getDueDate(),
                    a.getStatus(), a.getCreatedAt(), a.getUpdatedAt());
            rows.put(stored.getId(), stored);
            return stored;
        }

        @Override
        public Optional<OpportunityAction> findByIdAndTenant(UUID id, UUID tenantId) {
            return Optional.ofNullable(rows.get(id)).filter(a -> a.getTenantId().equals(tenantId));
        }

        @Override
        public List<OpportunityAction> findByOpportunity(UUID tenantId, UUID opportunityId) {
            return rows.values().stream()
                    .filter(a -> a.getTenantId().equals(tenantId) && a.getOpportunityId().equals(opportunityId))
                    .sorted(Comparator.comparing(OpportunityAction::getNumber).reversed())
                    .toList();
        }

        @Override
        public int maxNumber(UUID tenantId) {
            return rows.values().stream().filter(a -> a.getTenantId().equals(tenantId))
                    .mapToInt(OpportunityAction::getNumber).max().orElse(0);
        }

        @Override
        public void delete(OpportunityAction action) {
            rows.remove(action.getId());
        }
    }

    static final class FakeEvents implements RegisterRepositories.Events {
        final List<RegisterEvent> all = new ArrayList<>();
        long tick;
        /** Faux : garde l'horodatage du service tel quel, pour vérifier ses propres écarts. */
        boolean decaler = true;

        @Override
        public RegisterEvent save(RegisterEvent e) {
            // Horodatages croissants : l'ordre du suivi doit pouvoir se vérifier.
            RegisterEvent stored = new RegisterEvent(UUID.randomUUID(), e.tenantId(), e.itemKind(), e.itemId(),
                    e.type(), e.fromValue(), e.toValue(), e.detail(), e.actorId(),
                    decaler ? e.at().plusMillis(tick++) : e.at());
            all.add(stored);
            return stored;
        }

        @Override
        public List<RegisterEvent> findByItem(UUID tenantId, RegisterItemKind kind, UUID itemId) {
            return all.stream()
                    .filter(e -> e.tenantId().equals(tenantId) && e.itemKind() == kind && e.itemId().equals(itemId))
                    .sorted(Comparator.comparing(RegisterEvent::at).reversed())
                    .toList();
        }
    }

    /** Un catalogue qui ne connaît que les objets du client A qu'on lui déclare. */
    static final class FakeSources implements RiskSourceCatalog {
        final Map<UUID, SourceDraft> objets = new LinkedHashMap<>();
        RegisterOrigin origine = RegisterOrigin.FMEA;

        @Override
        public Optional<SourceDraft> find(RegisterOrigin origin, UUID tenantId, UUID sourceId) {
            if (origin != origine || !TENANT_A.equals(tenantId)) return Optional.empty();
            return Optional.ofNullable(objets.get(sourceId));
        }
    }

    record Ouverture(Risk risk, String title, String description, LocalDate due, UUID owner) {}

    static final class FakeCapas implements RiskCapaGateway {
        final List<Ouverture> ouvertes = new ArrayList<>();
        final List<LinkedCapa> lies = new ArrayList<>();

        @Override
        public LinkedCapa open(Risk risk, String title, String description, LocalDate dueDate, UUID ownerId) {
            ouvertes.add(new Ouverture(risk, title, description, dueDate, ownerId));
            LinkedCapa capa = new LinkedCapa(UUID.randomUUID(), title, dueDate, "OPEN");
            lies.add(capa);
            return capa;
        }

        @Override
        public List<LinkedCapa> linkedTo(Risk risk) {
            return lies;
        }
    }
}
