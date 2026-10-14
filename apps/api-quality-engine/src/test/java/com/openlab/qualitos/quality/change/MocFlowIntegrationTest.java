package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.change.domain.ImpactDomain;
import com.openlab.qualitos.quality.change.domain.ImpactLevel;
import com.openlab.qualitos.quality.change.domain.MocClassification;
import com.openlab.qualitos.quality.change.domain.MocException;
import com.openlab.qualitos.quality.change.domain.MocHistoryEvent;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.openlab.qualitos.quality.change.domain.ImpactDomain.*;
import static com.openlab.qualitos.quality.change.domain.ImpactLevel.*;
import static com.openlab.qualitos.quality.change.domain.MocClassification.CRITICAL;
import static com.openlab.qualitos.quality.change.domain.MocClassification.MAJOR;
import static com.openlab.qualitos.quality.change.domain.MocClassification.MINOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Le cycle complet d'une demande de changement, sur le mapping JPA réel (H2 en
 * mode PostgreSQL) : création, analyse, soumission, validation par rôles,
 * refus, réouverture, suppression, paramétrage, isolation entre clients.
 *
 * <p>Chaque test travaille dans un client neuf : aucune donnée partagée.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
class MocFlowIntegrationTest {

    @Autowired MocService service;
    @Autowired MocSettingsService settings;
    @Autowired ChangeRequestRepository requests;
    @Autowired ChangeHistoryRepository history;
    @MockitoBean MocNotifier notifier;

    UUID tenant;
    final UUID demandeur = UUID.randomUUID();
    final UUID responsable = UUID.randomUUID();
    final UUID hse = UUID.randomUUID();
    final UUID hseSuppleant = UUID.randomUUID();
    final UUID technique = UUID.randomUUID();
    final UUID site = UUID.randomUUID();
    final UUID admin = UUID.randomUUID();
    final UUID intrus = UUID.randomUUID();

    @BeforeEach
    void client() {
        tenant = UUID.randomUUID();
        TenantContext.setTenantId(tenant.toString());
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    static void connecte(UUID user, String nom) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(user.toString()).claim("name", nom).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    /** Les quatre rôles de la maquette, avec leurs titulaires (HSE a un suppléant). */
    MocDto.SettingsView parametrer(int majeur, int critique, Map<ImpactDomain, Integer> poids) {
        connecte(admin, "Admin");
        return settings.replace(new MocDto.SettingsForm(majeur, critique, 3, poids, List.of(
                role(null, "Responsable de service", EnumSet.of(MINOR, MAJOR, CRITICAL), responsable),
                role(null, "HSE", EnumSet.of(MINOR, MAJOR, CRITICAL), hse, hseSuppleant),
                role(null, "Directeur technique", EnumSet.of(MAJOR, CRITICAL), technique),
                role(null, "Directeur de site", EnumSet.of(CRITICAL), site))));
    }

    static MocDto.RoleForm role(UUID id, String label, Set<MocClassification> requis, UUID... titulaires) {
        List<MocDto.HolderForm> h = new ArrayList<>();
        for (UUID t : titulaires) {
            h.add(new MocDto.HolderForm(t, t.toString().substring(0, 8) + "@acme.test"));
        }
        return new MocDto.RoleForm(id, label, requis, h);
    }

    static Map<ImpactDomain, Integer> poids(int w) {
        Map<ImpactDomain, Integer> m = new EnumMap<>(ImpactDomain.class);
        for (ImpactDomain d : ImpactDomain.values()) m.put(d, w);
        return m;
    }

    static final Map<ImpactDomain, ImpactLevel> MAQUETTE = Map.of(SAFETY, MEDIUM, HEALTH_ENVIRONMENT, LOW,
            QUALITY, LOW, PRODUCTION, HIGH, MAINTENANCE, MEDIUM, COSTS, MEDIUM, REGULATORY, NONE, TRAINING, MEDIUM);

    MocDto.Detail creer(String titre) {
        connecte(demandeur, "S. Benali");
        return service.create(new MocDto.RequestForm(titre, ChangeRequestType.PERMANENT, "Atelier 2",
                "Remplacement du four", "Fin de vie", null));
    }

    /** Une demande en analyse, ses huit domaines évalués comme la maquette. */
    MocDto.Detail analysee(String titre) {
        MocDto.Detail d = creer(titre);
        service.sendToAnalysis(d.id());
        connecte(responsable, "R. Martin");
        MocDto.Detail r = null;
        for (ImpactDomain dom : ImpactDomain.values()) {
            r = service.assess(d.id(), dom, new MocDto.ImpactForm(MAQUETTE.get(dom), "Nature " + dom));
        }
        return r;
    }

    MocDto.Detail decider(UUID qui, String nom, UUID changeId, String role, ApprovalDecision decision, String commentaire) {
        connecte(qui, nom);
        UUID carte = service.get(changeId).validations().stream().filter(v -> v.roleLabel().equals(role))
                .findFirst().orElseThrow().id();
        return service.decide(changeId, carte, new MocDto.DecisionForm(decision, commentaire));
    }

    static String code(Throwable e) {
        return ((MocException) e).code();
    }

    // ---------- création ----------

    @Test
    void laCreationAttribueUnCodeSequentielParClientEtParAnnee() {
        int annee = LocalDate.now(ZoneOffset.UTC).getYear();
        MocDto.Detail a = creer("Premier");
        MocDto.Detail b = creer("Deuxième");
        assertThat(a.code()).isEqualTo("MOC-" + annee + "-001");
        assertThat(b.code()).isEqualTo("MOC-" + annee + "-002");
        assertThat(a.status()).isEqualTo(ChangeRequestStatus.DRAFT);
        assertThat(a.requesterName()).isEqualTo("S. Benali");
        assertThat(a.requestedByMe()).isTrue();
        assertThat(a.classification()).isEqualTo(MocClassification.NOT_ASSESSED);
        assertThat(a.history()).extracting(MocDto.HistoryView::event).containsExactly(MocHistoryEvent.CREATED);
        assertThat(a.history().get(0).actorName()).isEqualTo("S. Benali");

        // Un brouillon supprimé ne rend pas son numéro.
        service.delete(b.id());
        assertThat(creer("Troisième").code()).isEqualTo("MOC-" + annee + "-003");

        // Un autre client repart de 001.
        TenantContext.setTenantId(UUID.randomUUID().toString());
        assertThat(creer("Ailleurs").code()).isEqualTo("MOC-" + annee + "-001");
    }

    @Test
    void unCodeRepriDAvantLeMocNEstPasReattribue() {
        int annee = LocalDate.now(ZoneOffset.UTC).getYear();
        ChangeRequest ancien = new ChangeRequest();
        ancien.setTenantId(tenant);
        ancien.setCode("MOC-" + annee + "-001");
        ancien.setTitle("Repris");
        ancien.setRequesterUserId(demandeur);
        requests.save(ancien);
        assertThat(creer("Nouveau").code()).isEqualTo("MOC-" + annee + "-002");
    }

    // ---------- cycle nominal ----------

    @Test
    void leCycleCompletJusquALApprobation() {
        parametrer(25, 50, poids(1));
        MocDto.Detail d = creer("Changement de four");
        connecte(demandeur, "S. Benali");
        MocDto.Detail modifie = service.update(d.id(), new MocDto.RequestForm("Changement du four 3",
                ChangeRequestType.TEMPORARY, "Atelier 3", "desc", "just", LocalDate.of(2026, 12, 1)));
        assertThat(modifie.title()).isEqualTo("Changement du four 3");
        assertThat(modifie.type()).isEqualTo(ChangeRequestType.TEMPORARY);
        assertThat(modifie.plannedFor()).isEqualTo(LocalDate.of(2026, 12, 1));

        assertThatThrownBy(() -> service.assess(d.id(), SAFETY, new MocDto.ImpactForm(LOW, null)))
                .satisfies(e -> assertThat(code(e)).isEqualTo("moc.not-in-analysis"));
        assertThatThrownBy(() -> service.submit(d.id()))
                .satisfies(e -> assertThat(code(e)).isEqualTo("moc.not-in-analysis"));

        service.sendToAnalysis(d.id());
        assertThatThrownBy(() -> service.sendToAnalysis(d.id()))
                .satisfies(e -> assertThat(code(e)).isEqualTo("moc.not-draft"));

        connecte(responsable, "R. Martin");
        MocDto.Detail partiel = null;
        for (ImpactDomain dom : List.of(SAFETY, HEALTH_ENVIRONMENT, QUALITY, PRODUCTION, MAINTENANCE, COSTS, REGULATORY)) {
            partiel = service.assess(d.id(), dom, new MocDto.ImpactForm(MAQUETTE.get(dom), "Nature " + dom));
        }
        assertThat(partiel.classification()).isEqualTo(MocClassification.NOT_ASSESSED);
        assertThat(partiel.score()).isNull();
        assertThat(partiel.validations()).isEmpty();
        // Sept domaines sur huit : la soumission reste bloquée.
        assertThatThrownBy(() -> service.submit(d.id()))
                .satisfies(e -> assertThat(code(e)).isEqualTo("moc.analysis-incomplete"));

        MocDto.Detail complet = service.assess(d.id(), TRAINING, new MocDto.ImpactForm(MEDIUM, "Former"));
        assertThat(complet.score()).isEqualTo(54);
        assertThat(complet.classification()).isEqualTo(CRITICAL);
        assertThat(complet.impacts()).hasSize(8).allSatisfy(i -> assertThat(i.weight()).isEqualTo(1));
        // Avant la soumission, les cartes sont un aperçu de ce que la classe exigera.
        assertThat(complet.validations()).hasSize(4).allSatisfy(v -> {
            assertThat(v.preview()).isTrue();
            assertThat(v.canDecide()).isFalse();
        });
        assertThat(complet.history()).extracting(MocDto.HistoryView::event).contains(MocHistoryEvent.ANALYSIS_COMPLETED);
        // La réévaluation d'un domaine ne rejoue pas « analyse terminée ».
        MocDto.Detail reevalue = service.assess(d.id(), TRAINING, new MocDto.ImpactForm(MEDIUM, "Former tous"));
        assertThat(reevalue.history()).filteredOn(h -> h.event() == MocHistoryEvent.ANALYSIS_COMPLETED).hasSize(1);
        assertThat(requests.findById(d.id()).orElseThrow().getImpactSummary()).startsWith("Nature PRODUCTION");

        MocDto.Detail soumise = service.submit(d.id());
        assertThat(soumise.status()).isEqualTo(ChangeRequestStatus.IN_VALIDATION);
        assertThat(soumise.validations()).extracting(MocDto.ValidationView::roleLabel)
                .containsExactly("Responsable de service", "HSE", "Directeur technique", "Directeur de site");
        assertThat(soumise.validations()).allSatisfy(v -> assertThat(v.preview()).isFalse());
        assertThat(soumise.validations().get(1).holders()).hasSize(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MocNotifier.Recipient>> dest = ArgumentCaptor.forClass(Collection.class);
        verify(notifier).awaitingValidation(any(), dest.capture());
        assertThat(dest.getValue()).extracting(MocNotifier.Recipient::userId)
                .containsExactlyInAnyOrder(responsable, hse, hseSuppleant, technique, site);

        // L'analyse est figée.
        assertThatThrownBy(() -> service.assess(d.id(), SAFETY, new MocDto.ImpactForm(HIGH, null)))
                .satisfies(e -> assertThat(code(e)).isEqualTo("moc.not-in-analysis"));
        connecte(demandeur, "S. Benali");
        assertThatThrownBy(() -> service.update(d.id(), new MocDto.RequestForm("x", ChangeRequestType.PERMANENT,
                null, null, null, null))).satisfies(e -> assertThat(code(e)).isEqualTo("moc.not-editable"));

        // Le titulaire voit sa carte actionnable ; pas le demandeur.
        connecte(hse, "J. Dupont");
        MocDto.Detail vueHse = service.get(d.id());
        assertThat(vueHse.validations().get(1).canDecide()).isTrue();
        assertThat(vueHse.validations().get(0).canDecide()).isFalse();
        assertThat(service.list()).filteredOn(s -> s.id().equals(d.id()))
                .singleElement().satisfies(s -> assertThat(s.awaitingMe()).isTrue());
        connecte(demandeur, "S. Benali");
        assertThat(service.get(d.id()).validations()).noneMatch(MocDto.ValidationView::canDecide);
        assertThat(service.list()).filteredOn(s -> s.id().equals(d.id()))
                .singleElement().satisfies(s -> assertThat(s.awaitingMe()).isFalse());

        decider(responsable, "R. Martin", d.id(), "Responsable de service", ApprovalDecision.APPROVED, null);
        // Le suppléant statue pour HSE.
        MocDto.Detail apresHse = decider(hseSuppleant, "M. Martin", d.id(), "HSE", ApprovalDecision.APPROVED, "OK");
        assertThat(apresHse.validations().get(1).decidedByName()).isEqualTo("M. Martin");
        assertThat(apresHse.validations().get(1).decidedAt()).isNotNull();
        decider(technique, "D. Tech", d.id(), "Directeur technique", ApprovalDecision.APPROVED, null);
        verify(notifier, never()).decided(any(), eq(true), any());
        MocDto.Detail fin = decider(site, "D. Site", d.id(), "Directeur de site", ApprovalDecision.APPROVED, null);

        assertThat(fin.status()).isEqualTo(ChangeRequestStatus.APPROVED);
        assertThat(fin.closedAt()).isNotNull();
        verify(notifier).decided(any(), eq(true), eq(null));
        assertThat(fin.history()).extracting(MocDto.HistoryView::event).contains(MocHistoryEvent.APPROVED,
                MocHistoryEvent.VALIDATION_APPROVED, MocHistoryEvent.SUBMITTED);
        // Le plus récent en haut, la création en bas.
        assertThat(fin.history().get(fin.history().size() - 1).event()).isEqualTo(MocHistoryEvent.CREATED);
        assertThatThrownBy(() -> service.reopen(d.id()))
                .satisfies(e -> assertThat(code(e)).isEqualTo("moc.not-rejected"));
    }

    // ---------- validation : qui statue ----------

    @Test
    void seulUnTitulaireStatueJamaisLeDemandeurEtUnRefusSeMotive() {
        parametrer(25, 50, poids(1));
        MocDto.Detail d = analysee("Four");
        service.submit(d.id());

        assertThatThrownBy(() -> decider(intrus, "Intrus", d.id(), "HSE", ApprovalDecision.APPROVED, null))
                .isInstanceOf(MocException.class)
                .satisfies(e -> {
                    assertThat(((MocException) e).kind()).isEqualTo(MocException.Kind.FORBIDDEN);
                    assertThat(code(e)).isEqualTo("moc.not-holder");
                    assertThat(((MocException) e).details()).containsEntry("role", "HSE");
                });
        // Le titulaire d'un autre rôle ne statue pas sur celui-ci.
        assertThatThrownBy(() -> decider(technique, "T", d.id(), "HSE", ApprovalDecision.APPROVED, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo("moc.not-holder"));
        assertThatThrownBy(() -> decider(demandeur, "S. Benali", d.id(), "HSE", ApprovalDecision.APPROVED, null))
                .satisfies(e -> {
                    assertThat(((MocException) e).kind()).isEqualTo(MocException.Kind.FORBIDDEN);
                    assertThat(code(e)).isEqualTo("moc.self-validation");
                });
        assertThatThrownBy(() -> decider(hse, "J. Dupont", d.id(), "HSE", ApprovalDecision.REJECTED, "  "))
                .satisfies(e -> {
                    assertThat(((MocException) e).kind()).isEqualTo(MocException.Kind.INVALID);
                    assertThat(code(e)).isEqualTo("moc.comment-required");
                });
        assertThatThrownBy(() -> decider(hse, "J. Dupont", d.id(), "HSE", ApprovalDecision.PENDING, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo("moc.decision-required"));
        connecte(hse, "J. Dupont");
        assertThatThrownBy(() -> service.decide(d.id(), UUID.randomUUID(),
                new MocDto.DecisionForm(ApprovalDecision.APPROVED, null)))
                .satisfies(e -> assertThat(((MocException) e).kind()).isEqualTo(MocException.Kind.NOT_FOUND));

        decider(hse, "J. Dupont", d.id(), "HSE", ApprovalDecision.APPROVED, null);
        // Le suppléant ne statue pas une seconde fois sur la même carte.
        assertThatThrownBy(() -> decider(hseSuppleant, "M", d.id(), "HSE", ApprovalDecision.APPROVED, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo("moc.already-decided"));
    }

    @Test
    void unSeulRefusRejetteLaDemandeQuiPeutEtreRouverte() {
        parametrer(25, 50, poids(1));
        MocDto.Detail d = analysee("Four");
        service.submit(d.id());
        decider(responsable, "R", d.id(), "Responsable de service", ApprovalDecision.APPROVED, null);
        clearInvocations(notifier);

        MocDto.Detail rejete = decider(technique, "D. Tech", d.id(), "Directeur technique",
                ApprovalDecision.REJECTED, "Analyse des coûts insuffisante");
        assertThat(rejete.status()).isEqualTo(ChangeRequestStatus.REJECTED);
        assertThat(rejete.validations().get(2).comment()).isEqualTo("Analyse des coûts insuffisante");
        assertThat(rejete.validations()).noneMatch(MocDto.ValidationView::canDecide);
        verify(notifier).decided(any(), eq(false), eq("Analyse des coûts insuffisante"));
        verify(notifier).retire(any(), eq(technique));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MocNotifier.Recipient>> encore = ArgumentCaptor.forClass(Collection.class);
        verify(notifier).validationClosed(any(), encore.capture());
        // Restent en attente : HSE (deux titulaires) et le directeur de site.
        assertThat(encore.getValue()).extracting(MocNotifier.Recipient::userId)
                .containsExactlyInAnyOrder(hse, hseSuppleant, site);

        assertThatThrownBy(() -> decider(hse, "J", d.id(), "HSE", ApprovalDecision.APPROVED, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo("moc.not-in-validation"));
        connecte(demandeur, "S");
        assertThatThrownBy(() -> service.delete(d.id()))
                .satisfies(e -> assertThat(code(e)).isEqualTo("moc.delete-not-draft"));

        connecte(responsable, "R");
        MocDto.Detail rouverte = service.reopen(d.id());
        assertThat(rouverte.status()).isEqualTo(ChangeRequestStatus.IN_ANALYSIS);
        assertThat(rouverte.submittedAt()).isNull();
        assertThat(rouverte.validations()).allSatisfy(v -> assertThat(v.preview()).isTrue());
        assertThat(rouverte.history()).extracting(MocDto.HistoryView::event)
                .contains(MocHistoryEvent.REOPENED, MocHistoryEvent.VALIDATION_REJECTED, MocHistoryEvent.REJECTED);

        clearInvocations(notifier);
        MocDto.Detail resoumise = service.submit(d.id());
        assertThat(resoumise.validations()).allSatisfy(v -> assertThat(v.decision()).isEqualTo(ApprovalDecision.PENDING));
        verify(notifier).awaitingValidation(any(), any());
    }

    @Test
    void laSoumissionExigeUnTitulaireAutreQueLeDemandeur() {
        // Paramétrage livré : les rôles n'ont aucun titulaire.
        MocDto.Detail d = analysee("Four");
        assertThatThrownBy(() -> service.submit(d.id())).satisfies(e -> {
            assertThat(((MocException) e).kind()).isEqualTo(MocException.Kind.CONFLICT);
            assertThat(code(e)).isEqualTo("moc.role-without-holder");
            assertThat(((MocException) e).details()).containsEntry("role", "Responsable de service");
        });

        connecte(admin, "Admin");
        settings.replace(new MocDto.SettingsForm(25, 50, 3, poids(1), List.of(
                role(null, "Responsable", EnumSet.of(MINOR, MAJOR, CRITICAL), demandeur))));
        connecte(responsable, "R");
        assertThatThrownBy(() -> service.submit(d.id()))
                .satisfies(e -> assertThat(code(e)).isEqualTo("moc.role-only-requester"));
    }

    // ---------- suppression ----------

    @Test
    void seulUnBrouillonSeSupprime() {
        MocDto.Detail brouillon = creer("A supprimer");
        service.delete(brouillon.id());
        assertThat(requests.findById(brouillon.id())).isEmpty();
        assertThat(history.findByTenantIdAndChangeIdOrderByOccurredAtDesc(tenant, brouillon.id())).isEmpty();

        MocDto.Detail enAnalyse = creer("En analyse");
        service.sendToAnalysis(enAnalyse.id());
        assertThatThrownBy(() -> service.delete(enAnalyse.id()))
                .satisfies(e -> assertThat(code(e)).isEqualTo("moc.delete-not-draft"));
    }

    // ---------- isolation ----------

    @Test
    void unClientNeVoitNiNeTouchelesDemandesDUnAutre() {
        MocDto.Detail d = creer("Secret");
        TenantContext.setTenantId(UUID.randomUUID().toString());
        assertThat(service.list()).isEmpty();
        assertThatThrownBy(() -> service.get(d.id())).isInstanceOf(ChangeRequestNotFoundException.class);
        assertThatThrownBy(() -> service.delete(d.id())).isInstanceOf(ChangeRequestNotFoundException.class);
        assertThatThrownBy(() -> service.sendToAnalysis(d.id())).isInstanceOf(ChangeRequestNotFoundException.class);
        assertThat(service.exportRows()).isEmpty();
    }

    // ---------- paramétrage ----------

    @Test
    void unNouveauParametrageReclasseLesDemandesEnAnalyseMaisPasCellesEnValidation() {
        parametrer(25, 50, poids(1));
        MocDto.Detail figee = analysee("Soumise");
        service.submit(figee.id());
        MocDto.Detail ouverte = analysee("Ouverte");
        assertThat(ouverte.classification()).isEqualTo(CRITICAL);

        // Seuils relevés et production sans poids : 48 → mineur sous un seuil majeur à 50.
        Map<ImpactDomain, Integer> w = poids(1);
        w.put(PRODUCTION, 0);
        MocDto.SettingsView avant = settings.view();
        connecte(admin, "Admin");
        MocDto.SettingsView apres = settings.replace(new MocDto.SettingsForm(50, 80, 5, w,
                avant.roles().stream().map(r -> new MocDto.RoleForm(r.id(), r.label(), r.requiredFor(),
                        r.holders().stream().map(h -> new MocDto.HolderForm(h.userId(), h.label())).toList()))
                        .toList()));
        assertThat(apres.reminderDays()).isEqualTo(5);
        assertThat(apres.weights()).containsEntry(PRODUCTION, 0);

        connecte(responsable, "R");
        MocDto.Detail reclassee = service.get(ouverte.id());
        assertThat(reclassee.score()).isEqualTo(48);
        assertThat(reclassee.classification()).isEqualTo(MINOR);
        assertThat(reclassee.impacts()).filteredOn(i -> i.domain() == PRODUCTION)
                .singleElement().satisfies(i -> assertThat(i.weight()).isZero());
        MocDto.Detail gelee = service.get(figee.id());
        assertThat(gelee.score()).isEqualTo(54);
        assertThat(gelee.classification()).isEqualTo(CRITICAL);
        assertThat(gelee.validations()).hasSize(4);
    }

    @Test
    void unRoleAttenduNeSeRetireNiNeSeVide() {
        MocDto.SettingsView s = parametrer(25, 50, poids(1));
        MocDto.Detail d = analysee("Four");
        service.submit(d.id());
        connecte(admin, "Admin");

        List<MocDto.RoleForm> sansSite = new ArrayList<>(formes(s));
        sansSite.removeIf(r -> r.label().equals("Directeur de site"));
        assertThatThrownBy(() -> settings.replace(new MocDto.SettingsForm(25, 50, 3, poids(1), sansSite)))
                .satisfies(e -> {
                    assertThat(code(e)).isEqualTo("moc.settings.role-in-use");
                    assertThat(((MocException) e).details()).containsEntry("role", "Directeur de site");
                });

        List<MocDto.RoleForm> siteVide = formes(s).stream().map(r -> r.label().equals("Directeur de site")
                ? new MocDto.RoleForm(r.id(), r.label(), r.requiredFor(), List.of()) : r).toList();
        assertThatThrownBy(() -> settings.replace(new MocDto.SettingsForm(25, 50, 3, poids(1), siteVide)))
                .satisfies(e -> assertThat(code(e)).isEqualTo("moc.settings.role-in-use-holders"));

        assertThatThrownBy(() -> settings.replace(new MocDto.SettingsForm(25, 50, 3, poids(1), List.of(
                role(UUID.randomUUID(), "Inconnu", EnumSet.of(MINOR, MAJOR, CRITICAL), hse)))))
                .satisfies(e -> assertThat(code(e)).isEqualTo("moc.settings.role-id"));
    }

    @Test
    void unNouveauTitulaireDUnRoleAttenduEstPrevenuEtPeutStatuer() {
        MocDto.SettingsView s = parametrer(25, 50, poids(1));
        MocDto.Detail d = analysee("Four");
        service.submit(d.id());
        clearInvocations(notifier);
        UUID remplacant = UUID.randomUUID();

        connecte(admin, "Admin");
        List<MocDto.RoleForm> roles = formes(s).stream().map(r -> r.label().equals("Directeur de site")
                ? new MocDto.RoleForm(r.id(), "Direction du site", r.requiredFor(),
                List.of(new MocDto.HolderForm(remplacant, "nouveau@acme.test")))
                : r).toList();
        settings.replace(new MocDto.SettingsForm(25, 50, 3, poids(1), roles));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MocNotifier.Recipient>> dest = ArgumentCaptor.forClass(Collection.class);
        verify(notifier).awaitingValidation(any(), dest.capture());
        assertThat(dest.getValue()).singleElement().satisfies(r -> {
            assertThat(r.userId()).isEqualTo(remplacant);
            // Le libellé figé à la soumission, pas le nouveau nom du rôle.
            assertThat(r.roles()).containsExactly("Directeur de site");
        });

        // L'éligibilité se résout à la décision : l'ancien titulaire ne statue plus, le nouveau si.
        assertThatThrownBy(() -> decider(site, "Ancien", d.id(), "Directeur de site", ApprovalDecision.APPROVED, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo("moc.not-holder"));
        MocDto.Detail ok = decider(remplacant, "Nouveau", d.id(), "Directeur de site", ApprovalDecision.APPROVED, null);
        assertThat(ok.validations().get(3).decision()).isEqualTo(ApprovalDecision.APPROVED);
        assertThat(ok.validations().get(3).roleLabel()).isEqualTo("Directeur de site");
    }

    @Test
    void sansParametrageLesValeursLivreesSAppliquent() {
        connecte(admin, "Admin");
        MocDto.SettingsView v = settings.view();
        assertThat(v.majorThreshold()).isEqualTo(25);
        assertThat(v.criticalThreshold()).isEqualTo(50);
        assertThat(v.reminderDays()).isEqualTo(3);
        assertThat(v.roles()).hasSize(4).allSatisfy(r -> {
            assertThat(r.id()).isNull();
            assertThat(r.holders()).isEmpty();
        });
    }

    @Test
    void lExportReprendToutesLesDemandesDuClient() {
        parametrer(25, 50, poids(1));
        MocDto.Detail d = analysee("Four");
        service.submit(d.id());
        decider(hse, "J. Dupont", d.id(), "HSE", ApprovalDecision.APPROVED, "OK");
        creer("Brouillon");
        List<MocWorkbook.Row> rows = service.exportRows();
        assertThat(rows).hasSize(2);
        MocWorkbook.Row soumise = rows.stream().filter(r -> r.request().getId().equals(d.id())).findFirst().orElseThrow();
        assertThat(soumise.levels()).hasSize(8).containsEntry(PRODUCTION, HIGH);
        assertThat(soumise.validations()).extracting(ChangeValidation::getRoleLabel)
                .containsExactly("Responsable de service", "HSE", "Directeur technique", "Directeur de site");
        assertThat(MocWorkbook.build(rows)).isNotEmpty();
    }

    private static List<MocDto.RoleForm> formes(MocDto.SettingsView s) {
        return s.roles().stream().map(r -> new MocDto.RoleForm(r.id(), r.label(), r.requiredFor(),
                r.holders().stream().map(h -> new MocDto.HolderForm(h.userId(), h.label())).toList())).toList();
    }
}
