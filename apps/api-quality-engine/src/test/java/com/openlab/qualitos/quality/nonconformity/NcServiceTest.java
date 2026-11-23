package com.openlab.qualitos.quality.nonconformity;

import com.openlab.qualitos.quality.authz.application.RecordScope;
import com.openlab.qualitos.quality.capa.CapaCriticity;
import com.openlab.qualitos.quality.capa.CapaDto;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.capa.CapaStatus;
import com.openlab.qualitos.quality.capa.CapaSourceType;
import com.openlab.qualitos.quality.capa.CapaType;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.mockito.ArgumentMatchers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NcServiceTest {

    /**
     * « Ni le PFMEA, ni le plan de surveillance » : la réponse des bancs qui ne
     * mesurent pas cette régle-là. Ce n'est pas un vide -- c'est une décision,
     * et c'est bien celle qu'ils veulent : rien ne doit retenir leur clôture.
     */
    private static final NcDto.CloseRequest SANS_IMPACT =
            new NcDto.CloseRequest(false, false);

    @Mock NonConformityRepository repo;
    @Mock NcTeamMemberRepository teamMembers;
    @Mock CapaService capaService;
    @Mock com.openlab.qualitos.quality.pdca.PdcaService pdcaService;
    @Mock com.openlab.qualitos.quality.dmaic.DmaicService dmaicService;
    @Mock org.springframework.context.ApplicationEventPublisher events;
    /** Sans réglage, Mockito rend Optional.empty() : l'utilisateur voit tout (ADR 0081). */
    @Mock RecordScope scope;
    @Mock com.openlab.qualitos.quality.nonconformity.subcategory.NcSubcategoryCatalog subcategories;
    @Mock com.openlab.qualitos.quality.authz.application.AuthorizationService authz;
    /**
     * Sans réglage, Mockito rend {@code false} : aucune révision actée. C'est
     * l'état qui compte, puisque c'est lui qui fait naître le verrou.
     */
    @Mock NcRiskRevisionsPort riskRevisions;
    @InjectMocks NcService service;

    /**
     * Le droit de clore, accorde par defaut dans ces bancs.
     *
     * <p>Sans cela chaque banc de cloture mesurerait le DROIT au lieu de la
     * regle qu il vise. Le second regard et le refus d autorisation ont leurs
     * propres bancs, qui disent explicitement ce qu ils supposent.
     */
    private void jePeuxClore() {
        org.mockito.Mockito.when(authz.has(
                com.openlab.qualitos.quality.authz.domain.Permission.NC_CLOSE)).thenReturn(true);
    }

    /**
     * L'équipe nommée à l'ouverture (D1 du 8D) : deux personnes de l'annuaire,
     * jamais des identifiants techniques saisis à la main.
     */
    static final java.util.List<NcDto.TeamMemberRef> EQUIPE = java.util.List.of(
            new NcDto.TeamMemberRef("kc-marie", "Marie Durand"),
            new NcDto.TeamMemberRef("kc-ali", "Ali Benali"));

    static final UUID TENANT = UUID.randomUUID();
    static final UUID OTHER = UUID.randomUUID();
    static final UUID REPORTER = UUID.randomUUID();
    static final UUID OWNER = UUID.randomUUID();

    @BeforeEach void ctx() { TenantContext.setTenantId(TENANT.toString()); }
    @AfterEach  void clr() { TenantContext.clear(); }

    // --- create + reference generation ---
    @Test
    void create_generatesReference_andSavesOpen() {
        when(repo.countByTenantIdAndReferenceStartingWith(eq(TENANT), anyString())).thenReturn(2L);
        when(repo.existsByTenantIdAndReference(eq(TENANT), anyString())).thenReturn(false);
        when(repo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        NcDto.Response r = service.create(req());

        ArgumentCaptor<NonConformity> cap = ArgumentCaptor.forClass(NonConformity.class);
        verify(repo).saveAndFlush(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(NcStatus.OPEN);
        assertThat(cap.getValue().getTenantId()).isEqualTo(TENANT);
        assertThat(r.reference()).matches("Int-NC-\\d{4}-0003");
        assertThat(r.reference()).endsWith("0003"); // count(2) + 1
    }

    @Test
    void create_referenceCollision_skipsToNext() {
        when(repo.countByTenantIdAndReferenceStartingWith(eq(TENANT), anyString())).thenReturn(0L);
        // première référence prise, deuxième libre
        when(repo.existsByTenantIdAndReference(eq(TENANT), anyString())).thenReturn(true, false);
        when(repo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        NcDto.Response r = service.create(req());
        assertThat(r.reference()).endsWith("0002");
    }

    // --- BUG #4 : race sur la référence → retry puis succès ---
    @Test
    void create_concurrentReferenceCollision_retriesAndSucceeds() {
        when(repo.countByTenantIdAndReferenceStartingWith(eq(TENANT), anyString())).thenReturn(0L);
        when(repo.existsByTenantIdAndReference(eq(TENANT), anyString())).thenReturn(false);
        // 1re insertion : la référence a été prise par une transaction concurrente
        // (violation d'unicité au flush) ; 2e tentative : succès.
        when(repo.saveAndFlush(any()))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("dup reference"))
                .thenAnswer(inv -> inv.getArgument(0));

        NcDto.Response r = service.create(req());

        assertThat(r).isNotNull();
        verify(repo, times(2)).saveAndFlush(any());
    }

    @Test
    void create_referenceCollision_exhaustsRetries_propagates() {
        when(repo.countByTenantIdAndReferenceStartingWith(eq(TENANT), anyString())).thenReturn(0L);
        when(repo.existsByTenantIdAndReference(eq(TENANT), anyString())).thenReturn(false);
        when(repo.saveAndFlush(any()))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("dup reference"));

        assertThatThrownBy(() -> service.create(req()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        verify(repo, times(3)).saveAndFlush(any());
    }

    @Test
    void create_missingTenant_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.create(req()))
                .isInstanceOf(MissingTenantContextException.class);
        verifyNoInteractions(repo);
    }

    // --- sous-catégorie : la nature fine de l'écart ---

    @Test
    void create_verifieLaNatureAupresDuCatalogue() {
        when(repo.countByTenantIdAndReferenceStartingWith(eq(TENANT), anyString())).thenReturn(0L);
        when(repo.existsByTenantIdAndReference(eq(TENANT), anyString())).thenReturn(false);
        when(repo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(subcategories.resolve("prd-dim", NcCategory.PRODUCT)).thenReturn("PRD-DIM");

        NcDto.Response r = service.create(new NcDto.CreateRequest(
                "Joint", "détail", NcCategory.PRODUCT, "prd-dim", NcSeverity.MAJOR,
                Instant.now(), null, null, null, null, REPORTER, null, null, null, null, null, null, null, EQUIPE));

        // Le code enregistré est celui que le catalogue rend, pas celui saisi :
        // la casse se normalise une fois pour toutes.
        assertThat(r.subcategoryCode()).isEqualTo("PRD-DIM");
    }

    @Test
    void create_natureRefusee_remonteLErreur() {
        when(subcategories.resolve("PAS-UN-CODE", NcCategory.PRODUCT))
                .thenThrow(new NcValidationException("Unknown non-conformity subcategory: PAS-UN-CODE"));

        assertThatThrownBy(() -> service.create(new NcDto.CreateRequest(
                "Joint", "détail", NcCategory.PRODUCT, "PAS-UN-CODE", NcSeverity.MAJOR,
                Instant.now(), null, null, null, null, REPORTER, null, null, null, null, null, null, null, EQUIPE)))
                .isInstanceOf(NcValidationException.class);
        verify(repo, never()).saveAndFlush(any());
    }

    @Test
    void update_chaineVide_effaceLeClassementFin() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        n.setSubcategoryCode("PRD-DIM");
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);

        service.update(n.getId(), new NcDto.UpdateRequest(null, null, null, "", null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null));

        assertThat(n.getSubcategoryCode()).isNull();
        verifyNoInteractions(subcategories);
    }

    @Test
    void update_changerDeCategorie_revalideLaNatureEnPlace() {
        // Reclasser un écart produit en écart fournisseur sans toucher à sa nature
        // laisserait une nature produit sous une catégorie fournisseur.
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        n.setSubcategoryCode("PRD-DIM");
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(subcategories.resolve("PRD-DIM", NcCategory.SUPPLIER))
                .thenThrow(new NcValidationException("belongs to category PRODUCT"));

        assertThatThrownBy(() -> service.update(n.getId(), new NcDto.UpdateRequest(
                null, null, NcCategory.SUPPLIER, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null)))
                .isInstanceOf(NcValidationException.class);
    }

    @Test
    void update_natureAbsente_neTouchePasAuClassement() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        n.setSubcategoryCode("PRD-DIM");
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);

        service.update(n.getId(), new NcDto.UpdateRequest("t2", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null));

        assertThat(n.getSubcategoryCode()).isEqualTo("PRD-DIM");
        verifyNoInteractions(subcategories);
    }

    // --- findAll : filtres portés par une spécification ---
    //
    // Les huit tests précédents énuméraient les combinaisons de la cascade de
    // `if/else` qui construisait la requête. Cette cascade a disparu : un
    // quatrième filtre — l'origine — en aurait demandé seize. Ce qui reste à
    // vérifier, c'est le contrat : la requête est bornée au tenant du jeton, et
    // les entités reviennent bien converties. Le comportement des filtres eux-mêmes
    // est vérifié là où il vit désormais, dans la spécification (NcOriginTest et
    // les tests d'intégration du contrôleur).

    @Test
    void findAll_mapsEntitiesToResponses() {
        Pageable p = PageRequest.of(0, 10);
        when(repo.findAll(ArgumentMatchers.<Specification<NonConformity>>any(), eq(p)))
                .thenReturn(new PageImpl<>(java.util.List.of(nc(TENANT, NcStatus.OPEN))));

        Page<NcDto.Response> r = service.findAll(null, null, null, null, null, null, null, p);

        assertThat(r.getContent()).hasSize(1);
        assertThat(r.getContent().get(0).status()).isEqualTo(NcStatus.OPEN);
    }

    @Test
    void findAll_requiresATenant() {
        // Sans tenant, on ne liste RIEN : la requête n'est même pas construite.
        TenantContext.clear();
        Pageable p = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.findAll(null, null, null, null, null, null, null, p))
                .isInstanceOf(MissingTenantContextException.class);
        verifyNoInteractions(repo);
    }

    // --- findById ---
    @Test
    void findById_found() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        assertThat(service.findById(n.getId()).id()).isEqualTo(n.getId());
    }

    @Test
    void findById_notFound() {
        UUID id = UUID.randomUUID();
        when(repo.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(NcNotFoundException.class);
    }

    @Test
    void findById_wrongTenant_notFound() {
        NonConformity n = nc(OTHER, NcStatus.OPEN);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(n.getId())).isInstanceOf(NcNotFoundException.class);
    }

    // --- update ---
    @Test
    void update_success() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);
        service.update(n.getId(), new NcDto.UpdateRequest(
                "t2", "d2", NcCategory.SUPPLIER, null, NcSeverity.CRITICAL, null, "zoneB", 1.0, 2.0, "u1\nu2",
                NcOrigin.EXTERNAL, null, null, null, null, null, null, null, null));
        assertThat(n.getTitle()).isEqualTo("t2");
        assertThat(n.getCategory()).isEqualTo(NcCategory.SUPPLIER);
        assertThat(n.getSeverity()).isEqualTo(NcSeverity.CRITICAL);
        assertThat(n.getZone()).isEqualTo("zoneB");
        assertThat(n.getGeoLat()).isEqualTo(1.0);
        assertThat(n.getPhotoUrls()).isEqualTo("u1\nu2");
        // Une NC d'abord classée interne peut se révéler signalée du dehors.
        assertThat(n.getOrigin()).isEqualTo(NcOrigin.EXTERNAL);
    }

    @Test
    void update_closed_throws() {
        NonConformity n = nc(TENANT, NcStatus.CLOSED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        assertThatThrownBy(() -> service.update(n.getId(),
                new NcDto.UpdateRequest("x", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(NcStateException.class);
    }

    @Test
    void update_cancelled_throws() {
        NonConformity n = nc(TENANT, NcStatus.CANCELLED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        assertThatThrownBy(() -> service.update(n.getId(),
                new NcDto.UpdateRequest("x", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(NcStateException.class);
    }

    // --- startAnalysis ---
    @Test
    void startAnalysis_success_withRootCause() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);
        service.startAnalysis(n.getId(), new NcDto.StartAnalysisRequest("cause racine X"));
        assertThat(n.getStatus()).isEqualTo(NcStatus.UNDER_ANALYSIS);
        assertThat(n.getRootCause()).isEqualTo("cause racine X");
    }

    @Test
    void startAnalysis_nullBody_ok() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);
        service.startAnalysis(n.getId(), null);
        assertThat(n.getStatus()).isEqualTo(NcStatus.UNDER_ANALYSIS);
        assertThat(n.getRootCause()).isNull();
    }

    @Test
    void startAnalysis_notOpen_throws() {
        NonConformity n = nc(TENANT, NcStatus.UNDER_ANALYSIS);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        assertThatThrownBy(() -> service.startAnalysis(n.getId(),
                new NcDto.StartAnalysisRequest(null))).isInstanceOf(NcStateException.class);
    }

    /**
     * L'ECART QUI N'AVAIT PLUS DE SUITE.
     *
     * <p>Le confinement du D3 s'ecrit avant la recherche de cause : on ouvre le
     * dossier CAPA sur un ecart a peine declare pour proteger le client tout de
     * suite. L'ecart restait OPEN, passait UNDER_ANALYSIS au premier clic sur
     * « Analyser » -- et n'en sortait plus jamais, puisque l'ouverture du
     * dossier est le seul chemin vers ACTION_DEFINED et qu'un second dossier
     * est refuse. « Resoudre » restait eteint a vie, sans un mot.
     */
    @Test
    void analyser_un_ecart_qui_porte_deja_son_dossier_definit_l_action() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        n.setCapaCaseId(UUID.randomUUID());
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);

        service.startAnalysis(n.getId(), new NcDto.StartAnalysisRequest("cause racine X"));

        assertThat(n.getStatus()).isEqualTo(NcStatus.ACTION_DEFINED);
        assertThat(n.getRootCause()).isEqualTo("cause racine X");
    }

    /** Sans dossier, rien ne change : l'analyse s'ouvre seule. */
    @Test
    void analyser_un_ecart_sans_dossier_ouvre_l_analyse_seule() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);

        service.startAnalysis(n.getId(), null);

        assertThat(n.getStatus()).isEqualTo(NcStatus.UNDER_ANALYSIS);
    }

    /**
     * LA DATE DU CONSTAT SE CORRIGE.
     *
     * <p>Une reclamation recue lundi et enregistree jeudi portait jeudi, sans
     * recours : c'est pourtant cette date qui fait courir le delai de reponse
     * du par le client, et qui range l'ecart dans le bon mois.
     */
    @Test
    void la_date_du_constat_se_corrige() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        Instant lundi = Instant.parse("2026-11-16T09:00:00Z");
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);

        service.update(n.getId(), new NcDto.UpdateRequest(
                null, null, null, null, null, lundi, null, null, null, null, null,
                null, null, null, null, null, null, null, null));

        assertThat(n.getDetectedAt()).isEqualTo(lundi);
    }

    /** Absente, elle ne touche a rien : la mise a jour reste partielle. */
    @Test
    void la_date_du_constat_absente_ne_touche_a_rien() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        Instant avant = n.getDetectedAt();
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);

        service.update(n.getId(), new NcDto.UpdateRequest(
                "titre corrige", null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null));

        assertThat(n.getDetectedAt()).isEqualTo(avant);
    }

    // --- defineAction ---
    @Test
    void defineAction_success() {
        NonConformity n = nc(TENANT, NcStatus.UNDER_ANALYSIS);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);
        service.defineAction(n.getId());
        assertThat(n.getStatus()).isEqualTo(NcStatus.ACTION_DEFINED);
    }

    @Test
    void defineAction_wrongState_throws() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        assertThatThrownBy(() -> service.defineAction(n.getId())).isInstanceOf(NcStateException.class);
    }

    // --- resolve ---
    @Test
    void resolve_success() {
        NonConformity n = nc(TENANT, NcStatus.ACTION_DEFINED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);
        when(capaService.hasAnyCaseFor(any(), any())).thenReturn(true);
        service.resolve(n.getId(), new NcDto.ResolveRequest("corrigé et vérifié"));
        assertThat(n.getStatus()).isEqualTo(NcStatus.RESOLVED);
        assertThat(n.getResolutionNote()).isEqualTo("corrigé et vérifié");
        assertThat(n.getResolvedAt()).isNotNull();
    }

    /**
     * Resoudre, c est dire « ce qui devait etre fait l a ete ». Un dossier
     * encore en cours dit l inverse sur la fiche voisine : l ecart passait
     * « resolu » au-dessus d actions non executees.
     */
    @Test
    void resolve_tantQuUnDossierALesActionsAMener_refuse() {
        NonConformity n = nc(TENANT, NcStatus.ACTION_DEFINED);
        UUID capaId = UUID.randomUUID();
        n.setCapaCaseId(capaId);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(capaService.unexecutedCasesFor(capaId, n.getReference()))
                .thenReturn(List.of("Remplacer le roulement de broche"));
        when(capaService.unexecutedCaseNamesInScopeFor(capaId, n.getReference()))
                .thenReturn(List.of("Remplacer le roulement de broche"));
        when(capaService.hasAnyCaseFor(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.resolve(n.getId(), new NcDto.ResolveRequest("fait")))
                .isInstanceOf(NcStateException.class)
                // Le refus NOMME le dossier : sinon il faut aller le chercher.
                .hasMessageContaining("Remplacer le roulement de broche");
        assertThat(n.getStatus()).isEqualTo(NcStatus.ACTION_DEFINED);
        verify(repo, never()).save(any());
    }

    /**
     * La question part AVEC LA REFERENCE, et pas seulement avec le lien de
     * l escalade : un dossier ouvert depuis la liste CAPA en citant l ecart
     * retient la resolution au meme titre.
     */
    @Test
    void resolve_interrogeAussiLesDossiersLiesParLaReference() {
        NonConformity n = nc(TENANT, NcStatus.ACTION_DEFINED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(capaService.unexecutedCasesFor(null, n.getReference()))
                .thenReturn(List.of("Auditer le fournisseur"));
        when(capaService.hasAnyCaseFor(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.resolve(n.getId(), new NcDto.ResolveRequest("fait")))
                .isInstanceOf(NcStateException.class);
        verify(capaService).unexecutedCasesFor(null, n.getReference());
    }

    /**
     * Un dossier RESOLU ne retient pas la resolution de l ecart : ses actions
     * sont executees, il reste a juger de leur efficacite, et c est la CLOTURE
     * qui attend ce jugement (ADR 0108).
     */
    @Test
    void resolve_dossierResolu_laisseResoudre() {
        NonConformity n = nc(TENANT, NcStatus.ACTION_DEFINED);
        UUID capaId = UUID.randomUUID();
        n.setCapaCaseId(capaId);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);
        when(capaService.unexecutedCasesFor(capaId, n.getReference())).thenReturn(List.of());
        when(capaService.hasAnyCaseFor(any(), any())).thenReturn(true);

        service.resolve(n.getId(), new NcDto.ResolveRequest("fait"));

        assertThat(n.getStatus()).isEqualTo(NcStatus.RESOLVED);
    }

    // ---------- les motifs de blocage, DITS avant le clic ----------

    /**
     * Le refus arrivait apres le clic, en anglais, construit par le service :
     * l utilisateur decouvrait l obstacle en croyant en avoir fini. La fiche
     * porte desormais le motif et son decompte, que l ecran redige.
     */
    @Test
    void fiche_aLEtapeDeLaResolution_porteLeMotifDesActionsAMener() {
        NonConformity n = nc(TENANT, NcStatus.ACTION_DEFINED);
        UUID capaId = UUID.randomUUID();
        n.setCapaCaseId(capaId);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(capaService.unexecutedActionCountFor(capaId, n.getReference())).thenReturn(2);
        when(capaService.hasAnyCaseFor(any(), any())).thenReturn(true);

        NcDto.Response fiche = service.findById(n.getId());

        assertThat(fiche.blockers()).containsExactly(
                new NcDto.Blocker(NcBlockerCode.CAPA_ACTIONS_TO_CARRY_OUT, 2));
    }

    /**
     * Un ecart qu AUCUN dossier ne traite porte son propre motif.
     *
     * <p>Le bouton etait eteint sans un mot : l ecart disait « action definie »
     * et il n y en avait pas une seule. Le motif nomme le geste -- ajouter une
     * action -- au lieu de laisser chercher.
     */
    @Test
    void fiche_sansAucunDossier_porteLeMotifDeLActionManquante() {
        NonConformity n = nc(TENANT, NcStatus.ACTION_DEFINED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(capaService.hasAnyCaseFor(any(), any())).thenReturn(false);

        NcDto.Response fiche = service.findById(n.getId());

        assertThat(fiche.blockers()).containsExactly(
                new NcDto.Blocker(NcBlockerCode.NO_CAPA_CASE, 0));
    }

    /**
     * Et le refus suit le motif : resoudre un ecart sans la moindre action
     * decidee, c est le declarer traite sans trace de traitement (ISO 9001
     * §10.2). Le motif annonce avant le clic et le refus du clic repondent a la
     * meme question.
     */
    @Test
    void resolve_sansAucunDossier_refuse() {
        NonConformity n = nc(TENANT, NcStatus.ACTION_DEFINED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(capaService.hasAnyCaseFor(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.resolve(n.getId(), new NcDto.ResolveRequest("fait")))
                .isInstanceOf(NcStateException.class)
                .hasMessageContaining("add at least one action");
        assertThat(n.getStatus()).isEqualTo(NcStatus.ACTION_DEFINED);
        verify(repo, never()).save(any());
    }

    /**
     * L invariant compte les dossiers QU ON NE VOIT PAS.
     *
     * <p>La liste nommee est sous portee (ADR 0081) : un lecteur restreint a
     * ses propres dossiers n y voit rien des autres. Posee sur cette liste, la
     * question « aucun dossier ne traite cet ecart ? » aurait repondu OUI pour
     * lui et NON pour le responsable qualite -- meme ecart, meme instant, deux
     * reponses. Il se serait vu refuser la resolution, et annoncer qu aucune
     * action n est inscrite, la ou il en existe trois.
     */
    @Test
    void fiche_dossiersHorsPortee_neDitPasQuAucuneActionNExiste() {
        NonConformity n = nc(TENANT, NcStatus.ACTION_DEFINED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        // Le lecteur ne voit AUCUN dossier nomme...
        when(capaService.casesOf(any(), any())).thenReturn(List.of());
        // ... mais il en existe, et leurs actions sont faites.
        when(capaService.hasAnyCaseFor(any(), any())).thenReturn(true);
        when(capaService.unexecutedActionCountFor(any(), any())).thenReturn(0);

        assertThat(service.findById(n.getId()).blockers()).isEmpty();
    }

    /**
     * A l etape de la cloture, c est l autre motif : un dossier RESOLU ne
     * retient pas la resolution, mais la cloture attend son verdict
     * d efficacite (ADR 0108).
     */
    @Test
    void fiche_aLEtapeDeLaCloture_porteLeMotifDesDossiersNonRegles() {
        NonConformity n = nc(TENANT, NcStatus.RESOLVED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(capaService.unsettledCasesFor(null, n.getReference()))
                .thenReturn(List.of("Remplacer le roulement"));

        NcDto.Response fiche = service.findById(n.getId());

        assertThat(fiche.blockers()).containsExactly(
                new NcDto.Blocker(NcBlockerCode.CAPA_CASES_UNSETTLED, 1));
    }

    /** Rien ne s oppose : aucun motif, et le bouton reste allume. */
    @Test
    void fiche_sansObstacle_neporteAucunMotif() {
        NonConformity n = nc(TENANT, NcStatus.ACTION_DEFINED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(capaService.unexecutedActionCountFor(null, n.getReference())).thenReturn(0);
        when(capaService.hasAnyCaseFor(any(), any())).thenReturn(true);

        assertThat(service.findById(n.getId()).blockers()).isEmpty();
    }

    /**
     * Seule l etape COURANTE est instruite : reprocher a l etape suivante ce
     * qu on ne propose pas encore serait incomprehensible, et chaque motif
     * coute des requetes.
     */
    @Test
    void fiche_avantLEtapeDeLaResolution_nInterrogePasLesDossiers() {
        NonConformity n = nc(TENANT, NcStatus.UNDER_ANALYSIS);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));

        assertThat(service.findById(n.getId()).blockers()).isEmpty();
        verify(capaService, never()).unexecutedCasesFor(any(), any());
        verify(capaService, never()).unsettledCasesFor(any(), any());
    }

    /** Un ecart clos ne porte pas de motif : il n y a plus rien a faire. */
    @Test
    void fiche_dUnEcartClos_neporteAucunMotif() {
        NonConformity n = nc(TENANT, NcStatus.CLOSED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));

        assertThat(service.findById(n.getId()).blockers()).isEmpty();
        verify(capaService, never()).unsettledCasesFor(any(), any());
    }

    /**
     * Une LIGNE DE LISTE ne porte pas de motif : les calculer couterait deux
     * requetes par ligne pour une information qu on n y affiche pas.
     */
    @Test
    void liste_neCalculePasLesMotifs() {
        NonConformity n = nc(TENANT, NcStatus.ACTION_DEFINED);
        when(repo.findAll(ArgumentMatchers.<Specification<NonConformity>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(n)));

        Page<NcDto.Response> page = service.findAll(null, null, null, null, null, null, null,
                PageRequest.of(0, 20));

        assertThat(page.getContent()).singleElement()
                .satisfies(r -> assertThat(r.blockers()).isEmpty());
        verify(capaService, never()).unexecutedCasesFor(any(), any());
    }

    /**
     * L INVARIANT DE CETTE DECISION. Un ecart peut porter PLUSIEURS dossiers :
     * le premier ouvert depuis sa fiche, et un second ouvert depuis la liste en
     * citant sa reference, qui ne remplace pas le lien du premier (ADR 0121).
     *
     * <p>La fiche n en montrait qu UN. Les actions du second retenaient donc la
     * cloture de l ecart sans apparaitre nulle part : l utilisateur lisait
     * « 2 dossiers non regles » au-dessus d un tableau qui n en montrait qu un.
     */
    @Test
    void fiche_porteTousLesDossiersDeLEcart() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        UUID premier = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        n.setCapaCaseId(premier);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(capaService.casesOf(premier, n.getReference())).thenReturn(List.of(
                new CapaDto.CaseRef(premier, "Dossier de la fiche", CapaStatus.IN_PROGRESS,
                        null, List.of(), false),
                new CapaDto.CaseRef(second, "Dossier ouvert depuis la liste", CapaStatus.OPEN,
                        null, List.of(), false)));

        List<NcDto.CapaCaseRef> dossiers = service.findById(n.getId()).capaCases();

        assertThat(dossiers).hasSize(2);
        assertThat(dossiers.get(0).title()).isEqualTo("Dossier de la fiche");
        assertThat(dossiers.get(1).id()).isEqualTo(second);
        // Le STATUT voyage avec le titre : sans lui l ecran devrait relire
        // chaque dossier pour ecrire l en-tete de son tableau d actions.
        assertThat(dossiers.get(1).status()).isEqualTo("OPEN");
    }

    /**
     * Un ecart a peine declare n a ni lien ni reference : rien a chercher, et
     * la requete serait vide par construction.
     */
    @Test
    void fiche_sansLienNiReference_nInterrogePasLesDossiers() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        n.setCapaCaseId(null);
        n.setReference(null);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));

        assertThat(service.findById(n.getId()).capaCases()).isEmpty();
        verify(capaService, never()).casesOf(any(), any());
    }

    /**
     * Une LIGNE DE LISTE ne porte pas les dossiers : comme les motifs, les
     * chercher couterait des requetes par ligne pour ce qu on n y affiche pas.
     */
    @Test
    void liste_neCherchePasLesDossiers() {
        NonConformity n = nc(TENANT, NcStatus.ACTION_DEFINED);
        n.setCapaCaseId(UUID.randomUUID());
        when(repo.findAll(ArgumentMatchers.<Specification<NonConformity>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(n)));

        Page<NcDto.Response> page = service.findAll(null, null, null, null, null, null, null,
                PageRequest.of(0, 20));

        assertThat(page.getContent()).singleElement()
                .satisfies(r -> assertThat(r.capaCases()).isEmpty());
        verify(capaService, never()).casesOf(any(), any());
    }

    @Test
    void resolve_wrongState_throws() {
        NonConformity n = nc(TENANT, NcStatus.UNDER_ANALYSIS);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        assertThatThrownBy(() -> service.resolve(n.getId(),
                new NcDto.ResolveRequest("x"))).isInstanceOf(NcStateException.class);
    }

    // --- close ---
    @Test
    void close_success() {
        jePeuxClore();
        NonConformity n = nc(TENANT, NcStatus.RESOLVED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);
        service.close(n.getId(), SANS_IMPACT);
        assertThat(n.getStatus()).isEqualTo(NcStatus.CLOSED);
        assertThat(n.getClosedAt()).isNotNull();
    }

    /**
     * La reference porte l'origine : « Ext-NC- » pour une reclamation client.
     *
     * <p>Toutes les fiches portaient « NC-2026-0017 », qu'il s'agisse d'un
     * constat d'autocontrole ou d'une reclamation : le numero cite dans un
     * courriel ne disait pas de quel registre il venait.
     */
    @Test
    void create_ecartSignaleDuDehors_prefixeLaReferenceEnExterne() {
        when(repo.countByTenantIdAndReferenceStartingWith(eq(TENANT), anyString())).thenReturn(0L);
        when(repo.existsByTenantIdAndReference(eq(TENANT), anyString())).thenReturn(false);
        when(repo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        NcDto.Response r = service.create(reqExterne());

        assertThat(r.reference()).matches("Ext-NC-\\d{4}-0001");
        // Le comptage porte sur le prefixe de CETTE origine : les deux suites
        // sont independantes, et l'une ne doit pas decaler l'autre.
        verify(repo).countByTenantIdAndReferenceStartingWith(eq(TENANT),
                ArgumentMatchers.startsWith("Ext-NC-"));
    }

    /**
     * Un ecart clos dit « plus rien a faire ici ». Le clore pendant qu un
     * dossier CAPA est encore en travail le faisait disparaitre des listes de
     * travail alors que son action restait a mener, et les indicateurs
     * comptaient un ecart traite et une action en retard pour le meme sujet.
     */
    @Test
    void close_tantQuUnDossierResteEnTravail_refuse() {
        jePeuxClore();
        NonConformity n = nc(TENANT, NcStatus.RESOLVED);
        UUID capaId = UUID.randomUUID();
        n.setCapaCaseId(capaId);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(capaService.unsettledCasesFor(capaId, n.getReference()))
                .thenReturn(List.of("Remplacer le roulement de broche"));
        when(capaService.unsettledCaseNamesInScopeFor(capaId, n.getReference()))
                .thenReturn(List.of("Remplacer le roulement de broche"));

        assertThatThrownBy(() -> service.close(n.getId(), SANS_IMPACT))
                .isInstanceOf(NcStateException.class)
                // Le refus NOMME ce qui reste : sans le titre, il faut aller
                // chercher dans le registre ce que l on vient de nous refuser.
                .hasMessageContaining("Remplacer le roulement de broche");
        assertThat(n.getStatus()).isEqualTo(NcStatus.RESOLVED);
        verify(repo, never()).save(any());
    }

    // ---------- ce que l'écart enseigne à l'analyse de risque ----------

    /**
     * UN ÉCART QUI A IMPACTÉ LE PFMEA NE SE CLÔT PAS AVANT SA RÉVISION.
     *
     * <p>Un défaut survenu pour de bon et une analyse de risque qui l'ignore
     * encore se contredisent sur la même page. L'IATF 16949 en fait une
     * exigence : toute non-conformité se répercute sur la PFMEA et le plan de
     * surveillance. Jusqu'ici personne n'avait à se prononcer, et l'écart se
     * refermait sur la question sans l'avoir posée.
     *
     * <p>Le refus NOMME le document : « il manque une révision » envoie
     * chercher, « le PFMEA attend la sienne » dit quoi faire.
     */
    @Test
    void close_pfmeaImpacteSansRevision_refuseEtLeDit() {
        jePeuxClore();
        NonConformity n = nc(TENANT, NcStatus.RESOLVED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> service.close(n.getId(),
                new NcDto.CloseRequest(true, false)))
                .isInstanceOf(NcGateException.class)
                .satisfies(ex -> assertThat(((NcGateException) ex).reasons())
                        .singleElement().asString().contains("PFMEA"));
        assertThat(n.getStatus()).isEqualTo(NcStatus.RESOLVED);
    }

    /** Le plan de surveillance a son propre verrou : les deux documents se révisent à part. */
    @Test
    void close_planDeSurveillanceImpacteSansRevision_refuseEtLeDit() {
        jePeuxClore();
        NonConformity n = nc(TENANT, NcStatus.RESOLVED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> service.close(n.getId(),
                new NcDto.CloseRequest(false, true)))
                .isInstanceOf(NcGateException.class)
                .satisfies(ex -> assertThat(((NcGateException) ex).reasons())
                        .singleElement().asString().contains("plan de surveillance"));
    }

    /** Les deux impactés, les deux motifs : un seul laisserait chercher lequel retient. */
    @Test
    void close_lesDeuxImpactes_donneLesDeuxMotifs() {
        jePeuxClore();
        NonConformity n = nc(TENANT, NcStatus.RESOLVED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> service.close(n.getId(),
                new NcDto.CloseRequest(true, true)))
                .isInstanceOf(NcGateException.class)
                .satisfies(ex -> assertThat(((NcGateException) ex).reasons()).hasSize(2));
    }

    /** La révision actée lève le verrou : c'est la leçon tirée, et elle l'est. */
    @Test
    void close_pfmeaImpacteEtRevise_passe() {
        jePeuxClore();
        NonConformity n = nc(TENANT, NcStatus.RESOLVED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(riskRevisions.pfmeaRevisedFor(TENANT, n.getId())).thenReturn(true);

        service.close(n.getId(), new NcDto.CloseRequest(true, false));

        assertThat(n.getStatus()).isEqualTo(NcStatus.CLOSED);
        assertThat(n.getPfmeaImpacted()).isTrue();
    }

    /**
     * « NON » EST UNE DÉCISION, et elle s'écrit.
     *
     * <p>Elle ne retient rien -- on a jugé que l'analyse de risque n'était pas
     * en cause -- mais elle est datée et imputable, et c'est elle que
     * l'auditeur vient lire quand le PFMEA n'a pas bougé. {@code null}, lui,
     * ne prouve rien : il dit que la question n'a pas été posée.
     */
    @Test
    void close_sansImpact_inscritLaDecisionEtPasse() {
        jePeuxClore();
        NonConformity n = nc(TENANT, NcStatus.RESOLVED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.close(n.getId(), SANS_IMPACT);

        assertThat(n.getStatus()).isEqualTo(NcStatus.CLOSED);
        assertThat(n.getPfmeaImpacted()).isFalse();
        assertThat(n.getControlPlanImpacted()).isFalse();
    }

    /**
     * LA RÉPONSE SURVIT AU REFUS DE SON PROPRE VERROU : un second essai n'a
     * pas à la redonner, et la fiche montre déjà ce qui a été tranché.
     */
    @Test
    void close_refuseParLeRisque_inscritQuandMemeLaReponse() {
        jePeuxClore();
        NonConformity n = nc(TENANT, NcStatus.RESOLVED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> service.close(n.getId(),
                new NcDto.CloseRequest(true, false)))
                .isInstanceOf(NcGateException.class);

        verify(repo).save(n);
        assertThat(n.getPfmeaImpacted()).isTrue();
    }

    /**
     * LE MOTIF S'ANNONCE AVANT LE CLIC, pour que le bouton ne s'éteigne pas
     * sans un mot. C'est la même règle, lue par l'autre chemin : si les deux
     * divergeaient, l'écran proposerait un geste que le serveur refuse.
     */
    @Test
    void fiche_dUnEcartResoluImpactant_annonceLeMotif() {
        NonConformity n = nc(TENANT, NcStatus.RESOLVED);
        n.setPfmeaImpacted(true);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));

        assertThat(service.findById(n.getId()).blockers())
                .extracting(NcDto.Blocker::code)
                .contains(NcBlockerCode.PFMEA_REVISION_REQUIRED);
    }

    /**
     * LA QUESTION NON POSÉE NE REPROCHE RIEN. {@code null} n'est pas
     * {@code false} : c'est la clôture elle-même qui pose la question, et un
     * motif affiché avant qu'on ait demandé reprocherait de n'avoir pas fait
     * ce qu'on ne propose pas encore.
     */
    @Test
    void fiche_sansReponse_nAnnonceAucunMotifDeRisque() {
        NonConformity n = nc(TENANT, NcStatus.RESOLVED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));

        assertThat(service.findById(n.getId()).blockers())
                .extracting(NcDto.Blocker::code)
                .doesNotContain(NcBlockerCode.PFMEA_REVISION_REQUIRED,
                        NcBlockerCode.CONTROL_PLAN_REVISION_REQUIRED);
    }

    /**
     * LE DECOMPTE est complet, LES NOMS sont ceux qu on a le droit de voir.
     *
     * <p>Le refus enumerait les titres rendus par la question hors portee : un
     * lecteur restreint a ses propres dossiers (ADR 0081) lisait donc, dans un
     * message d erreur, le titre de dossiers que leur propre fiche lui refuse.
     * Il garde son decompte -- l invariant ne depend pas de qui regarde -- et
     * perd les noms.
     */
    @Test
    void close_dossiersHorsPortee_donneLeDecompteSansLesNommer() {
        // Ce banc mesure ce que le refus DIT, pas qui a le droit de clore : sans
        // cet armement il buterait sur le second regard et ne verrait jamais le
        // decompte qu il vient verifier.
        jePeuxClore();
        NonConformity n = nc(TENANT, NcStatus.RESOLVED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        // Deux dossiers retiennent la cloture...
        when(capaService.unsettledCasesFor(null, n.getReference()))
                .thenReturn(List.of("Remplacer le roulement", "Auditer le fournisseur"));
        // ... et ce lecteur n en voit aucun.
        when(capaService.unsettledCaseNamesInScopeFor(null, n.getReference()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.close(n.getId(), SANS_IMPACT))
                .isInstanceOf(NcStateException.class)
                .hasMessageContaining("2 CAPA case(s)")
                .hasMessageNotContaining("Remplacer le roulement")
                .hasMessageNotContaining("Auditer le fournisseur")
                // Ni le deux-points orphelin que laisserait une liste vide.
                .hasMessageNotContaining("remain open:");
        assertThat(n.getStatus()).isEqualTo(NcStatus.RESOLVED);
    }

    /** Et le meme refus, a l etape de la resolution. */
    @Test
    void resolve_dossiersHorsPortee_donneLeDecompteSansLesNommer() {
        NonConformity n = nc(TENANT, NcStatus.ACTION_DEFINED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(capaService.unexecutedCasesFor(null, n.getReference()))
                .thenReturn(List.of("Remplacer le roulement"));
        when(capaService.unexecutedCaseNamesInScopeFor(null, n.getReference()))
                .thenReturn(List.of());
        // Un dossier EXISTE -- il est seulement hors de portee. Sans ce reglage,
        // c est le refus « aucun dossier » qui partirait, et le banc mesurerait
        // un invariant qui n est pas le sien.
        when(capaService.hasAnyCaseFor(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.resolve(n.getId(), new NcDto.ResolveRequest("fait")))
                .isInstanceOf(NcStateException.class)
                .hasMessageContaining("1 CAPA case(s)")
                .hasMessageNotContaining("Remplacer le roulement")
                .hasMessageNotContaining("carry out:");
    }

    /**
     * LA REGLE REGARDE TOUS LES DOSSIERS, pas seulement celui de l escalade.
     * Un dossier ouvert depuis le registre des CAPA en citant la reference de
     * l ecart ne laisse aucune trace dans {@code capaCaseId} : c est par la
     * reference qu on le retrouve, et c est donc la reference qui doit partir
     * avec la question.
     */
    @Test
    void close_interrogeAussiLesDossiersLiesParLaReference() {
        jePeuxClore();
        NonConformity sansEscalade = nc(TENANT, NcStatus.RESOLVED);
        when(repo.findByIdAndTenantId(sansEscalade.getId(), TENANT))
                .thenReturn(Optional.of(sansEscalade));
        when(capaService.unsettledCasesFor(null, "NC-2026-0001"))
                .thenReturn(List.of("Auditer le fournisseur"));
        when(capaService.unsettledCaseNamesInScopeFor(null, "NC-2026-0001"))
                .thenReturn(List.of("Auditer le fournisseur"));

        assertThatThrownBy(() -> service.close(sansEscalade.getId(), SANS_IMPACT))
                .isInstanceOf(NcStateException.class)
                .hasMessageContaining("Auditer le fournisseur");

        verify(capaService).unsettledCasesFor(null, "NC-2026-0001");
    }

    /**
     * Rien en travail : clos, ecarte, ou disparu du client. Un dossier instruit
     * puis ECARTE est une fin, et on ne garde pas un ecart ouvert pour un
     * dossier qui n existe plus -- les deux cas rendent la meme liste vide.
     */
    @Test
    void close_plusAucunDossierEnTravail_laisseCloturer() {
        jePeuxClore();
        NonConformity n = nc(TENANT, NcStatus.RESOLVED);
        UUID capaId = UUID.randomUUID();
        n.setCapaCaseId(capaId);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(capaService.unsettledCasesFor(capaId, n.getReference())).thenReturn(List.of());
        when(repo.save(n)).thenReturn(n);

        service.close(n.getId(), SANS_IMPACT);

        assertThat(n.getStatus()).isEqualTo(NcStatus.CLOSED);
    }

    @Test
    void close_wrongState_throws() {
        // Le DROIT se verifie avant l etat : refuser « seul un ecart resolu se
        // clot » a qui n a pas le droit de clore renseignerait un etranger sur
        // l etat du dossier.
        jePeuxClore();
        NonConformity n = nc(TENANT, NcStatus.ACTION_DEFINED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        assertThatThrownBy(() -> service.close(n.getId(), SANS_IMPACT)).isInstanceOf(NcStateException.class);
    }

    // --- cancel ---
    @Test
    void cancel_fromOpen_success() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);
        service.cancel(n.getId());
        assertThat(n.getStatus()).isEqualTo(NcStatus.CANCELLED);
    }

    @Test
    void cancel_fromUnderAnalysis_success() {
        NonConformity n = nc(TENANT, NcStatus.UNDER_ANALYSIS);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);
        service.cancel(n.getId());
        assertThat(n.getStatus()).isEqualTo(NcStatus.CANCELLED);
    }

    @Test
    void cancel_fromResolved_throws() {
        NonConformity n = nc(TENANT, NcStatus.RESOLVED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        assertThatThrownBy(() -> service.cancel(n.getId())).isInstanceOf(NcStateException.class);
    }

    // --- escalateToCapa ---
    @Test
    void escalate_ouvreLaCapaEtSonAction_parLeModuleCapa_lie_etFaitAvancerLEcart() {
        NonConformity n = nc(TENANT, NcStatus.UNDER_ANALYSIS);
        when(repo.findForUpdateByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        UUID capaId = UUID.randomUUID();
        CapaDto.OpenCaseRequest demande = openReq();
        CapaDto.CaseResponse ouvert = new CapaDto.CaseResponse(capaId, TENANT, "t", null, CapaType.CORRECTIVE,
                CapaCriticity.HIGH, com.openlab.qualitos.quality.capa.CapaStatus.OPEN,
                CapaSourceType.NON_CONFORMITY, n.getReference(), OWNER, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                java.util.List.of(), null, java.util.List.of());
        when(capaService.openCase(demande, CapaSourceType.NON_CONFORMITY, n.getReference())).thenReturn(ouvert);
        when(repo.save(n)).thenReturn(n);

        NcDto.Response r = service.escalateToCapa(n.getId(), demande);

        // La source est la NC elle-même : sa référence, jamais un champ du corps.
        verify(capaService).openCase(demande, CapaSourceType.NON_CONFORMITY, n.getReference());
        assertThat(r.capaCaseId()).isEqualTo(capaId);
        // Ouvrir la CAPA, c'est définir l'action : l'écart avance du même geste.
        // Les deux gestes étaient séparés, et l'écart restait affiché « en analyse »
        // alors que son action était écrite, datée et assignée.
        assertThat(n.getStatus()).isEqualTo(NcStatus.ACTION_DEFINED);
    }

    /**
     * La source du dossier REPREND L'ORIGINE de l'ecart. Un dossier ne d'une
     * reclamation client s'affichait « Non-conformite interne », sur l'ecran
     * meme qui sert a repondre a ce client.
     */
    @Test
    void escalate_depuisUnEcartExterne_ouvreUnDossierDeSourceExterne() {
        NonConformity n = nc(TENANT, NcStatus.UNDER_ANALYSIS);
        n.setOrigin(NcOrigin.EXTERNAL);
        when(repo.findForUpdateByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        CapaDto.OpenCaseRequest demande = openReq();
        CapaDto.CaseResponse ouvert = new CapaDto.CaseResponse(UUID.randomUUID(), TENANT, "t", null,
                CapaType.CORRECTIVE, CapaCriticity.HIGH,
                com.openlab.qualitos.quality.capa.CapaStatus.OPEN,
                CapaSourceType.EXTERNAL_NON_CONFORMITY, n.getReference(), OWNER, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                java.util.List.of(), null, java.util.List.of());
        when(capaService.openCase(demande, CapaSourceType.EXTERNAL_NON_CONFORMITY, n.getReference()))
                .thenReturn(ouvert);
        when(repo.save(n)).thenReturn(n);

        service.escalateToCapa(n.getId(), demande);

        verify(capaService).openCase(demande, CapaSourceType.EXTERNAL_NON_CONFORMITY, n.getReference());
    }

    @Test
    void escalate_depuisUnEcartAPeineDeclare_neSautePasLAnalyse() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        when(repo.findForUpdateByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(capaService.openCase(any(), any(), any())).thenReturn(capaOuverte(UUID.randomUUID(), n));
        when(repo.save(n)).thenReturn(n);

        service.escalateToCapa(n.getId(), openReq());

        // Une CAPA ouverte sur un écart à peine déclaré n'autorise pas à sauter
        // l'analyse : le dossier se rattache, le cycle de vie ne bouge pas.
        assertThat(n.getStatus()).isEqualTo(NcStatus.OPEN);
    }

    @Test
    void escalate_surUnEcartDejaResolu_neRevientPasEnArriere() {
        NonConformity n = nc(TENANT, NcStatus.RESOLVED);
        when(repo.findForUpdateByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(capaService.openCase(any(), any(), any())).thenReturn(capaOuverte(UUID.randomUUID(), n));
        when(repo.save(n)).thenReturn(n);

        service.escalateToCapa(n.getId(), openReq());

        assertThat(n.getStatus()).isEqualTo(NcStatus.RESOLVED);
    }

    private static CapaDto.CaseResponse capaOuverte(UUID capaId, NonConformity n) {
        return new CapaDto.CaseResponse(capaId, TENANT, "t", null, CapaType.CORRECTIVE,
                CapaCriticity.HIGH, com.openlab.qualitos.quality.capa.CapaStatus.OPEN,
                CapaSourceType.NON_CONFORMITY, n.getReference(), OWNER, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                java.util.List.of(), null, java.util.List.of());
    }

    @Test
    void escalate_secondTime_idempotent_refused() {
        NonConformity n = nc(TENANT, NcStatus.UNDER_ANALYSIS);
        n.setCapaCaseId(UUID.randomUUID());
        when(repo.findForUpdateByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        assertThatThrownBy(() -> service.escalateToCapa(n.getId(), openReq()))
                .isInstanceOf(NcStateException.class)
                .hasMessageContaining("already escalated");
        verifyNoInteractions(capaService);
    }

    private static CapaDto.OpenCaseRequest openReq() {
        return new CapaDto.OpenCaseRequest("Reprendre le réglage", null, CapaType.CORRECTIVE,
                CapaCriticity.HIGH, null, null, OWNER, "a.diallo@client.test",
                java.time.LocalDate.now().plusDays(7), null, null, null, null);
    }

    // --- rejet d'une réclamation externe ---

    @Test
    void reject_externalComplaint_recordsReasonAndDate() {
        NonConformity n = nc(TENANT, NcStatus.UNDER_ANALYSIS);
        n.setOrigin(NcOrigin.EXTERNAL);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(any(NonConformity.class))).thenAnswer(i -> i.getArgument(0));

        NcDto.Response apres = service.reject(n.getId(), new NcDto.RejectRequest(
                "  Hors périmètre contractuel, pièce non fournie.  "));

        assertThat(apres.status()).isEqualTo(NcStatus.REJECTED);
        assertThat(apres.rejectionReason())
                .isEqualTo("Hors périmètre contractuel, pièce non fournie.");
        assertThat(apres.rejectedAt()).isNotNull();
    }

    @Test
    void reject_internalFinding_isRefusedByTheService() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        n.setOrigin(NcOrigin.INTERNAL);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));

        // On ne rejette pas un constat qu'on a fait soi-même : on le résout, on le
        // clôt, ou on l'annule. Le refus est porté par le SERVICE, faute de quoi un
        // appel direct à l'API contournerait la règle de l'écran.
        assertThatThrownBy(() -> service.reject(n.getId(), new NcDto.RejectRequest("non")))
                .isInstanceOf(NcStateException.class)
                .hasMessageContaining("EXTERNAL");

        assertThat(n.getStatus()).isEqualTo(NcStatus.OPEN);
        verify(repo, never()).save(any());
    }

    @Test
    void reject_fromResolvedOrClosed_isRefused() {
        for (NcStatus statut : java.util.List.of(NcStatus.ACTION_DEFINED, NcStatus.RESOLVED,
                NcStatus.CLOSED, NcStatus.CANCELLED, NcStatus.REJECTED)) {
            NonConformity n = nc(TENANT, statut);
            n.setOrigin(NcOrigin.EXTERNAL);
            when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));

            // Une réclamation déjà traitée a reçu une réponse : la reprendre en rejet
            // réécrirait l'histoire que le client a lue.
            assertThatThrownBy(() -> service.reject(n.getId(), new NcDto.RejectRequest("tardif")))
                    .as("rejet depuis %s", statut)
                    .isInstanceOf(NcStateException.class);
        }
    }

    @Test
    void rejected_isTerminal_andCannotBeEdited() {
        NonConformity n = nc(TENANT, NcStatus.REJECTED);
        n.setOrigin(NcOrigin.EXTERNAL);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> service.update(n.getId(), new NcDto.UpdateRequest(
                "autre titre", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null)))
                .isInstanceOf(NcStateException.class)
                .hasMessageContaining("REJECTED");
    }

    @Test
    void statistics_countsRejectedInTheSameScopeAsTheTable() {
        lenient().when(repo.countByTenantIdAndOrigin(TENANT, NcOrigin.EXTERNAL)).thenReturn(9L);
        // `lenient` : le denombrement interroge les sept statuts, et les six autres
        // appels n'ont pas a etre declares pour que celui-ci soit verifie.
        lenient().when(repo.countByTenantIdAndOriginAndStatus(
                TENANT, NcOrigin.EXTERNAL, NcStatus.REJECTED)).thenReturn(4L);

        // Les tuiles surmontent un tableau déjà filtré : un total toutes origines
        // confondues serait un chiffre juste au mauvais endroit, donc faux.
        assertThat(service.statistics(NcOrigin.EXTERNAL).rejected()).isEqualTo(4);
    }

    // --- helpers ---
    private NcDto.CreateRequest req() {
        return new NcDto.CreateRequest(
                "Joint torique défectueux", "détail", NcCategory.PRODUCT, null, NcSeverity.MAJOR,
                Instant.now(), "Atelier 3", 48.85, 2.35, "https://s/p1.jpg", REPORTER, null, null, null, null, null, null, null,
                EQUIPE);
    }

    /** La meme requete, mais signalee du dehors : une reclamation client. */
    private NcDto.CreateRequest reqExterne() {
        return new NcDto.CreateRequest(
                "Colis livre endommage", "detail", NcCategory.PRODUCT, null, NcSeverity.MAJOR,
                Instant.now(), "Expedition", null, null, null, REPORTER, NcOrigin.EXTERNAL, null, null, null, null, null, null,
                EQUIPE);
    }

    // ====================== Le client et le lot (D2 du 8D) ==================

    /**
     * Le client qui reclame et le lot incrimine se RANGENT, au lieu de finir
     * noyes dans la description.
     *
     * <p>Ecrits en texte libre dans l enonce, ils ne ressortaient ni en filtre,
     * ni en indicateur, ni dans le rapport remis au client : la question « quels
     * ecarts sur le lot L2611-08 ? » n avait pas de reponse.
     */
    @Test
    void create_rangeLeClientEtLeLot() {
        when(repo.countByTenantIdAndReferenceStartingWith(eq(TENANT), anyString())).thenReturn(0L);
        when(repo.existsByTenantIdAndReference(eq(TENANT), anyString())).thenReturn(false);
        when(repo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        NcDto.Response r = service.create(new NcDto.CreateRequest(
                "Joint", "detail", NcCategory.PRODUCT, null, NcSeverity.MAJOR,
                Instant.now(), null, null, null, null, REPORTER, null, null,
                "Acme Industrie", "L2611-08", null, null, null, EQUIPE));

        assertThat(r.customerName()).isEqualTo("Acme Industrie");
        assertThat(r.batchNumber()).isEqualTo("L2611-08");
    }

    /**
     * Un champ OUVERT PUIS LAISSE EN BLANC n est pas une valeur.
     *
     * <p>Sans ce nettoyage, la fiche portait un nom de client invisible mais
     * present : il s affichait comme une valeur vide, et aucun filtre
     * « client renseigne » ne le retrouvait.
     */
    @Test
    void create_clientEnBlanc_neRangeRien() {
        when(repo.countByTenantIdAndReferenceStartingWith(eq(TENANT), anyString())).thenReturn(0L);
        when(repo.existsByTenantIdAndReference(eq(TENANT), anyString())).thenReturn(false);
        when(repo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        NcDto.Response r = service.create(new NcDto.CreateRequest(
                "Joint", "detail", NcCategory.PRODUCT, null, NcSeverity.MAJOR,
                Instant.now(), null, null, null, null, REPORTER, null, null,
                "   ", "\t", null, null, null, EQUIPE));

        assertThat(r.customerName()).isNull();
        assertThat(r.batchNumber()).isNull();
    }

    /**
     * La chaine VIDE efface, l absence n y touche pas.
     *
     * <p>Une reclamation saisie au mauvais client doit pouvoir PERDRE ce nom, et
     * pas seulement le remplacer par un autre : sans quoi la seule sortie serait
     * d ecrire un faux nom par-dessus.
     */
    @Test
    void update_clientVide_effaceLeNom_absent_nyTouchePas() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        n.setCustomerName("Acme Industrie");
        n.setBatchNumber("L2611-08");
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Le lot n est pas dans la requete : il reste.
        service.update(n.getId(), new NcDto.UpdateRequest(
                null, null, null, null, null, null, null, null, null, null, null, null,
                "", null, null, null, null, null, null));

        assertThat(n.getCustomerName()).isNull();
        assertThat(n.getBatchNumber()).isEqualTo("L2611-08");
    }

    // ============ Le commentaire de l analyse des causes (D4) ===============

    /**
     * Le commentaire du D4 s ecrit, et il ne touche pas a la cause racine.
     *
     * <p>Ce sont deux choses : la cause racine est la CONCLUSION, publiee par le
     * rapport 8D ; le commentaire est le raisonnement autour -- l hypothese
     * ecartee, et pourquoi. Il s ecrivait dans la description de l ecart, d ou
     * plus personne ne le rapprochait de l analyse.
     */
    @Test
    void update_commentaireDAnalyse_sEcritSansToucherALaCauseRacine() {
        NonConformity n = nc(TENANT, NcStatus.UNDER_ANALYSIS);
        n.setRootCause("Joint monte a l envers");
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NcDto.Response r = service.update(n.getId(), new NcDto.UpdateRequest(
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null,
                "Piste fournisseur ecartee : lot conforme au controle d entree"));

        assertThat(n.getCauseAnalysisComment())
                .isEqualTo("Piste fournisseur ecartee : lot conforme au controle d entree");
        assertThat(r.causeAnalysisComment())
                .isEqualTo("Piste fournisseur ecartee : lot conforme au controle d entree");
        assertThat(n.getRootCause()).isEqualTo("Joint monte a l envers");
    }

    /** La chaine VIDE efface : une note ecrite par erreur doit pouvoir partir. */
    @Test
    void update_commentaireDAnalyseVide_efface() {
        NonConformity n = nc(TENANT, NcStatus.UNDER_ANALYSIS);
        n.setCauseAnalysisComment("A retirer");
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(n.getId(), new NcDto.UpdateRequest(
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, "   "));

        assertThat(n.getCauseAnalysisComment()).isNull();
    }

    /** Absent de la requete : le commentaire reste ce qu il etait. */
    @Test
    void update_commentaireDAnalyseAbsent_resteEnPlace() {
        NonConformity n = nc(TENANT, NcStatus.UNDER_ANALYSIS);
        n.setCauseAnalysisComment("Deja note");
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(n.getId(), new NcDto.UpdateRequest(
                "autre titre", null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null));

        assertThat(n.getCauseAnalysisComment()).isEqualTo("Deja note");
    }

    // ====================== La reprise d un ecart refermé ====================

    /**
     * Un ecart REVIENT : recidive constatee apres la cloture, client qui relance.
     *
     * <p>Sans cette reprise, il fallait ouvrir un SECOND ecart -- qui perdait
     * l analyse de cause, les actions deja menees et l equipe du premier, et
     * comptait deux fois le meme probleme dans les indicateurs.
     */
    @Test
    void reopen_depuisClos_rouvreLInstruction() {
        NonConformity n = nc(TENANT, NcStatus.CLOSED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NcDto.Response r = service.reopen(n.getId(),
                new NcDto.ReopenRequest("Le defaut est reapparu sur le lot suivant"));

        // OPEN, et non ACTION_DEFINED : ce qui a manque, c est justement la cause
        // racine ou l action. Reprendre a l etape d apres sauterait l etape qui a
        // failli.
        assertThat(r.status()).isEqualTo(NcStatus.OPEN);
        assertThat(r.reopenedAt()).isNotNull();
        assertThat(r.reopenReason()).isEqualTo("Le defaut est reapparu sur le lot suivant");
    }

    /**
     * La reprise N EFFACE PAS ce qui avait referme l ecart.
     *
     * <p>{@code closedAt}, {@code resolvedAt} et leurs notes sont des faits
     * dates : la reponse que le client a lue ne se reecrit pas. Le statut, lui,
     * dit ou en est l ecart aujourd hui.
     */
    @Test
    void reopen_nEffacePasCeQuiAvaitReferme() {
        NonConformity n = nc(TENANT, NcStatus.CLOSED);
        Instant resolu = Instant.parse("2026-03-01T10:00:00Z");
        Instant clos = Instant.parse("2026-03-15T10:00:00Z");
        n.setResolvedAt(resolu);
        n.setClosedAt(clos);
        n.setResolutionNote("Joint remplace, controle fait");
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NcDto.Response r = service.reopen(n.getId(), new NcDto.ReopenRequest("Recidive"));

        assertThat(r.resolvedAt()).isEqualTo(resolu);
        assertThat(r.closedAt()).isEqualTo(clos);
        assertThat(r.resolutionNote()).isEqualTo("Joint remplace, controle fait");
    }

    /** Les quatre etats refermes se rouvrent, et eux seuls. */
    @Test
    void reopen_lesQuatreEtatsRefermes() {
        for (NcStatus referme : List.of(
                NcStatus.RESOLVED, NcStatus.CLOSED, NcStatus.CANCELLED, NcStatus.REJECTED)) {
            NonConformity n = nc(TENANT, referme);
            when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(service.reopen(n.getId(), new NcDto.ReopenRequest("reprise")).status())
                    .as("depuis %s", referme)
                    .isEqualTo(NcStatus.OPEN);
        }
    }

    /**
     * Un ecart EN COURS ne se rouvre pas : il n est pas ferme.
     *
     * <p>La garde est ICI et pas seulement dans l ecran : l API est publique
     * (§13.1), et un appel direct remettrait sinon a OPEN un ecart en pleine
     * analyse, en perdant l etape franchie.
     */
    @Test
    void reopen_surUnEcartEnCours_refuse() {
        for (NcStatus enCours : List.of(
                NcStatus.OPEN, NcStatus.UNDER_ANALYSIS, NcStatus.ACTION_DEFINED)) {
            NonConformity n = nc(TENANT, enCours);
            when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));

            assertThatThrownBy(() ->
                    service.reopen(n.getId(), new NcDto.ReopenRequest("reprise")))
                    .as("depuis %s", enCours)
                    .isInstanceOf(NcStateException.class)
                    .hasMessageContaining("can be");
            assertThat(n.getStatus()).isEqualTo(enCours);
        }
    }

    /**
     * SANS MOTIF, la reprise passe quand meme.
     *
     * <p>Rien n est obligatoire dans l ecran de traitement. Le motif a son prix
     * -- sans lui, la fiche montre un retour en arriere que personne ne sait
     * expliquer -- mais l exiger bloquerait le geste, et un ecart qui revient
     * doit pouvoir etre repris tout de suite. La DATE, elle, est posee dans tous
     * les cas : c est le fait que compte le taux de recidive.
     */
    @Test
    void reopen_sansMotif_passeQuandMeme() {
        NonConformity n = nc(TENANT, NcStatus.RESOLVED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NcDto.Response r = service.reopen(n.getId(), new NcDto.ReopenRequest("  "));

        assertThat(r.status()).isEqualTo(NcStatus.OPEN);
        assertThat(r.reopenedAt()).isNotNull();
        assertThat(r.reopenReason()).isNull();
    }

    private NonConformity nc(UUID tenant, NcStatus status) {
        NonConformity n = new NonConformity();
        n.setId(UUID.randomUUID());
        n.setTenantId(tenant);
        n.setReference("NC-2026-0001");
        n.setTitle("t");
        n.setCategory(NcCategory.PRODUCT);
        n.setSeverity(NcSeverity.MAJOR);
        n.setStatus(status);
        n.setDetectedAt(Instant.now());
        n.setCreatedAt(Instant.now());
        n.setUpdatedAt(Instant.now());
        return n;
    }
}
