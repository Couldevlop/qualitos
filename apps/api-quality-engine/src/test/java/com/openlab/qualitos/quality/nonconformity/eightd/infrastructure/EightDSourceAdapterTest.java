package com.openlab.qualitos.quality.nonconformity.eightd.infrastructure;

import com.openlab.qualitos.quality.capa.CapaAction;
import com.openlab.qualitos.quality.capa.CapaActionStatus;
import com.openlab.qualitos.quality.capa.CapaActionType;
import com.openlab.qualitos.quality.capa.CapaCase;
import com.openlab.qualitos.quality.capa.CapaSourceType;
import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.capa.CapaCriticity;
import com.openlab.qualitos.quality.capa.CapaEvidenceRepository;
import com.openlab.qualitos.quality.capa.CapaEvidence;
import com.openlab.qualitos.quality.capa.effectiveness.application.EffectivenessReviewPort;
import com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessReview;
import com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict;
import com.openlab.qualitos.quality.derogation.application.DirectoryPort;
import com.openlab.qualitos.quality.derogation.application.DirectoryUnavailableException;
import com.openlab.qualitos.quality.capa.CapaStatus;
import com.openlab.qualitos.quality.capa.CapaType;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanRepository;
import com.openlab.qualitos.quality.product.domain.ProductLookup;
import com.openlab.qualitos.quality.fivewhys.FiveWhysAnalysis;
import com.openlab.qualitos.quality.fivewhys.FiveWhysAnalysisRepository;
import com.openlab.qualitos.quality.fivewhys.FiveWhysStep;
import com.openlab.qualitos.quality.fivewhys.FiveWhysStepRepository;
import com.openlab.qualitos.quality.ishikawa.CauseCategory;
import com.openlab.qualitos.quality.ishikawa.IshikawaCause;
import com.openlab.qualitos.quality.ishikawa.IshikawaDiagram;
import com.openlab.qualitos.quality.ishikawa.IshikawaDiagramRepository;
import com.openlab.qualitos.quality.ishikawa.IshikawaStatus;
import com.openlab.qualitos.quality.nonconformity.NcCategory;
import com.openlab.qualitos.quality.nonconformity.NcNotFoundException;
import com.openlab.qualitos.quality.nonconformity.NcOrigin;
import com.openlab.qualitos.quality.nonconformity.NcPhotoRepository;
import com.openlab.qualitos.quality.nonconformity.NcTeamMemberRepository;
import com.openlab.qualitos.quality.nonconformity.NcTeamMember;
import com.openlab.qualitos.quality.nonconformity.NcSeverity;
import com.openlab.qualitos.quality.nonconformity.NcStatus;
import com.openlab.qualitos.quality.nonconformity.NonConformity;
import com.openlab.qualitos.quality.nonconformity.NonConformityRepository;
import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDSources;
import com.openlab.qualitos.quality.risk.FmeaItem;
import com.openlab.qualitos.quality.risk.FmeaItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * La collecte des sources : les chemins de liaison, et le cloisonnement.
 *
 * <p>Ce qui est vérifié ici n'est pas « les données remontent », c'est que CHAQUE
 * lecture porte le tenant, et qu'un lien manquant ou rompu se traduit par une
 * absence de source — jamais par une erreur qui empêcherait d'émettre le rapport.
 */
class EightDSourceAdapterTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID NC = UUID.randomUUID();
    private static final UUID CAPA = UUID.randomUUID();
    private static final UUID FMEA = UUID.randomUUID();
    private static final UUID PRODUIT = UUID.randomUUID();

    private NonConformityRepository ncs;
    private NcPhotoRepository photos;
    private NcTeamMemberRepository equipes;
    private IshikawaDiagramRepository ishikawas;
    private FiveWhysAnalysisRepository fiveWhys;
    private FiveWhysStepRepository etapes;
    private CapaCaseRepository capas;
    private CapaEvidenceRepository preuves;
    private FmeaItemRepository fmeaItems;
    private ControlPlanRepository plans;
    /** Le referentiel produit, pour nommer le produit vise au D2. */
    private ProductLookup produits;
    private DirectoryPort annuaire;
    private EffectivenessReviewPort efficacite;
    private EightDSourceAdapter adapter;

    @BeforeEach
    void setUp() {
        ncs = mock(NonConformityRepository.class);
        photos = mock(NcPhotoRepository.class);
        ishikawas = mock(IshikawaDiagramRepository.class);
        fiveWhys = mock(FiveWhysAnalysisRepository.class);
        etapes = mock(FiveWhysStepRepository.class);
        capas = mock(CapaCaseRepository.class);
        preuves = mock(CapaEvidenceRepository.class);
        fmeaItems = mock(FmeaItemRepository.class);
        equipes = mock(NcTeamMemberRepository.class);
        plans = mock(ControlPlanRepository.class);
        produits = mock(ProductLookup.class);
        annuaire = mock(DirectoryPort.class);
        when(annuaire.emailOf(any())).thenReturn(Optional.empty());
        efficacite = mock(EffectivenessReviewPort.class);
        when(efficacite.findLastPerActionOfCase(any(), any())).thenReturn(Map.of());
        adapter = new EightDSourceAdapter(ncs, photos, equipes, annuaire, efficacite, ishikawas, fiveWhys, etapes,
                capas, preuves, fmeaItems, plans, produits, viewAll -> Optional.empty());

        when(photos.findByTenantIdAndNcIdOrderByCreatedAtAsc(any(), any())).thenReturn(List.of());
        when(equipes.findByTenantIdAndNcIdOrderByPositionAsc(any(), any())).thenReturn(List.of());
        when(ishikawas.findByTenantIdAndNcIdOrderByCreatedAtDesc(any(), any())).thenReturn(List.of());
        when(fiveWhys.findByNonConformityIdAndTenantIdOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of());
    }

    @Test
    void le_8d_d_une_nc_hors_de_portee_est_introuvable() {
        // ADR 0081 : sans « voir toutes les NC », celle d'un autre déclarant n'existe pas.
        adapter = new EightDSourceAdapter(ncs, photos, equipes, annuaire, efficacite, ishikawas, fiveWhys, etapes,
                capas, preuves, fmeaItems, plans, produits,
                viewAll -> Optional.of(UUID.randomUUID()));
        when(ncs.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.of(nc()));

        assertThatThrownBy(() -> adapter.collect(TENANT, NC)).isInstanceOf(NcNotFoundException.class);
    }

    @Test
    void une_nc_absente_du_tenant_est_introuvable() {
        when(ncs.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.collect(TENANT, NC))
                .isInstanceOf(NcNotFoundException.class);
        // Rien d'autre n'est lu : on ne va pas chercher les analyses d'un écart
        // dont on vient de constater qu'il n'appartient pas à ce client.
        verifyNoInteractions(ishikawas, fiveWhys, capas, fmeaItems, plans);
    }

    @Test
    void la_nc_est_lue_avec_son_tenant_et_ses_photos_sont_comptees() {
        when(ncs.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.of(nc()));
        when(photos.findByTenantIdAndNcIdOrderByCreatedAtAsc(TENANT, NC))
                .thenReturn(List.of(new com.openlab.qualitos.quality.nonconformity.NcPhoto(),
                        new com.openlab.qualitos.quality.nonconformity.NcPhoto()));

        EightDSources sources = adapter.collect(TENANT, NC);

        verify(ncs).findByIdAndTenantId(NC, TENANT);
        assertThat(sources.nc().reference()).isEqualTo("NC-2026-0007");
        assertThat(sources.nc().status()).isEqualTo(NcStatus.CLOSED);
        assertThat(sources.nc().severity()).isEqualTo("MAJOR");
        assertThat(sources.nc().origin()).isEqualTo("INTERNAL");
        assertThat(sources.nc().photoCount()).isEqualTo(2);
    }

    @Test
    void sans_capa_sans_pfmea_et_sans_produit_rien_n_est_interroge_inutilement() {
        when(ncs.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.of(nc()));

        EightDSources sources = adapter.collect(TENANT, NC);

        assertThat(sources.capas()).isEmpty();
        assertThat(sources.fmea()).isNull();
        assertThat(sources.surveillance()).isEmpty();
        // Le PFMEA et les plans se cherchent par un identifiant que l'\u00e9cart ne
        // porte pas : rien \u00e0 demander. Les dossiers, eux, se cherchent aussi par
        // la R\u00c9F\u00c9RENCE de l'\u00e9cart -- c'est le prix de les voir TOUS, et la
        // cl\u00f4ture paie d\u00e9j\u00e0 ce prix.
        verifyNoInteractions(fmeaItems, plans);
    }

    @Test
    void une_capa_escaladee_remonte_avec_ses_actions_et_leurs_preuves() {
        NonConformity nc = nc();
        nc.setCapaCaseId(CAPA);
        CapaCase dossier = capa();
        UUID action = dossier.getActions().get(0).getId();
        when(ncs.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.of(nc));
        when(capas.findByIdAndTenantId(CAPA, TENANT)).thenReturn(Optional.of(dossier));
        when(preuves.findCaseLevel(TENANT, CAPA)).thenReturn(List.of(
                piece(null, "bilan-efficacite.pdf"), piece(null, "pv-cloture.pdf"),
                piece(null, "photo-apres.jpg")));
        when(preuves.findActionLevel(TENANT, CAPA))
                .thenReturn(List.of(piece(action, "rapport-essai.pdf")));

        EightDSources sources = adapter.collect(TENANT, NC);

        assertThat(sources.capas()).hasSize(1);
        // LE NOM DES PIECES, et non leur seul nombre : le D6 les nomme, et un
        // compte ne se verifie pas.
        assertThat(sources.capas().get(0).caseEvidenceNames())
                .containsExactly("bilan-efficacite.pdf", "pv-cloture.pdf", "photo-apres.jpg");
        assertThat(sources.capas().get(0).caseEvidenceCount()).isEqualTo(3L);
        assertThat(sources.capas().get(0).actions()).hasSize(1);
        assertThat(sources.capas().get(0).actions().get(0).evidenceNames())
                .containsExactly("rapport-essai.pdf");
        assertThat(sources.capas().get(0).actions().get(0).evidenceCount()).isEqualTo(1L);
        assertThat(sources.capas().get(0).actions().get(0).actionType()).isEqualTo("CORRECTIVE");
    }

    // ---------- le D1 : les noms de la fiche, les adresses de l'annuaire ----------

    /**
     * L'ADRESSE VIENT DE L'ANNUAIRE, le nom de la fiche.
     *
     * <p>La fiche ne retient que le nom et le compte ; le tableau du D1 porte
     * une colonne d'adresse, et c'est l'annuaire qui la connait. Le nom, lui,
     * reste celui FIGE a la saisie : un compte renomme ne doit pas reecrire un
     * rapport emis.
     */
    @Test
    void l_equipe_remonte_avec_l_adresse_de_chaque_membre() {
        when(ncs.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.of(nc()));
        UUID compte = UUID.randomUUID();
        when(equipes.findByTenantIdAndNcIdOrderByPositionAsc(TENANT, NC))
                .thenReturn(List.of(membre("Ada Lovelace", compte.toString()),
                        membre("Grace Hopper", null)));
        when(annuaire.emailOf(compte)).thenReturn(Optional.of("ada.lovelace@exemple.fr"));

        EightDSources sources = adapter.collect(TENANT, NC);

        assertThat(sources.team()).containsExactly(
                new EightDSources.Member("Ada Lovelace", "ada.lovelace@exemple.fr"),
                // SANS COMPTE, PAS D'ADRESSE : la case reste vide plutot que
                // d'en inventer une.
                new EightDSources.Member("Grace Hopper", null));
    }

    /**
     * UNE PANNE DE L'ANNUAIRE N'EMPECHE PAS D'ASSEMBLER UN RAPPORT.
     *
     * <p>L'adresse est une commodite, le rapport est une preuve : refuser de
     * l'assembler parce qu'api-core ne repond pas priverait le client de son
     * 8D pour une colonne.
     */
    @Test
    void un_annuaire_injoignable_laisse_l_adresse_vide_sans_rien_casser() {
        when(ncs.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.of(nc()));
        UUID compte = UUID.randomUUID();
        when(equipes.findByTenantIdAndNcIdOrderByPositionAsc(TENANT, NC))
                .thenReturn(List.of(membre("Ada Lovelace", compte.toString())));
        when(annuaire.emailOf(compte))
                .thenThrow(new DirectoryUnavailableException("injoignable", null));

        EightDSources sources = adapter.collect(TENANT, NC);

        assertThat(sources.team())
                .containsExactly(new EightDSources.Member("Ada Lovelace", null));
    }

    /** Un identifiant de compte qui n'est pas un UUID ne fait pas lever l'assemblage. */
    @Test
    void un_compte_illisible_laisse_l_adresse_vide() {
        when(ncs.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.of(nc()));
        when(equipes.findByTenantIdAndNcIdOrderByPositionAsc(TENANT, NC))
                .thenReturn(List.of(membre("Ada Lovelace", "pas-un-uuid")));

        EightDSources sources = adapter.collect(TENANT, NC);

        assertThat(sources.team())
                .containsExactly(new EightDSources.Member("Ada Lovelace", null));
        verifyNoInteractions(annuaire);
    }

    /**
     * LE VERDICT D'EFFICACITE DE CHAQUE ACTION remonte avec elle (ADR 0131) :
     * le rapport disait qu'un geste avait ete fait sans dire s'il avait servi.
     */
    @Test
    void une_action_jugee_remonte_avec_son_verdict() {
        NonConformity nc = nc();
        nc.setCapaCaseId(CAPA);
        CapaCase dossier = capa();
        UUID action = dossier.getActions().get(0).getId();
        when(ncs.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.of(nc));
        when(capas.findByIdAndTenantId(CAPA, TENANT)).thenReturn(Optional.of(dossier));
        when(efficacite.findLastPerActionOfCase(TENANT, CAPA)).thenReturn(Map.of(
                action, jugement(action)));

        EightDSources sources = adapter.collect(TENANT, NC);

        EightDSources.Action remontee = sources.capas().get(0).actions().get(0);
        assertThat(remontee.effectivenessVerdict()).isEqualTo("PARTIAL");
        assertThat(remontee.effectivenessReviewedAt()).isEqualTo(JUGE_LE);
    }

    /** Une action que personne n'a jugee ne porte pas de verdict. */
    @Test
    void une_action_non_jugee_ne_porte_aucun_verdict() {
        NonConformity nc = nc();
        nc.setCapaCaseId(CAPA);
        when(ncs.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.of(nc));
        when(capas.findByIdAndTenantId(CAPA, TENANT)).thenReturn(Optional.of(capa()));

        EightDSources sources = adapter.collect(TENANT, NC);

        EightDSources.Action remontee = sources.capas().get(0).actions().get(0);
        assertThat(remontee.effectivenessVerdict()).isNull();
        assertThat(remontee.effectivenessReviewedAt()).isNull();
    }

    /**
     * LE VERDICT DU DOSSIER VAUT POUR SON ACTION UNIQUE (ADR 0144).
     *
     * <p>Un jugement porte depuis l'ecran d'efficacite est une evaluation DU
     * DOSSIER : le rapport ne lisait que celles des actions, et taisait donc un
     * verdict pourtant rendu. Depuis l'ADR 0135 un dossier porte une action et
     * une seule : le juger, c'est la juger.
     */
    @Test
    void le_verdict_du_dossier_remonte_sur_son_action_unique() {
        NonConformity nc = nc();
        nc.setCapaCaseId(CAPA);
        CapaCase dossier = capa();
        assertThat(dossier.getActions()).hasSize(1);
        when(ncs.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.of(nc));
        when(capas.findByIdAndTenantId(CAPA, TENANT)).thenReturn(Optional.of(dossier));
        when(efficacite.findLastByCase(TENANT, CAPA)).thenReturn(Optional.of(
                EffectivenessReview.rehydrate(UUID.randomUUID(), TENANT, CAPA,
                        EffectivenessVerdict.EFFECTIVE, "Rebut sous 2 %", "4,2 %", "2 %", "0 %",
                        null, UUID.randomUUID(), "Vera Verificatrice", JUGE_LE)));

        EightDSources sources = adapter.collect(TENANT, NC);

        EightDSources.Action remontee = sources.capas().get(0).actions().get(0);
        assertThat(remontee.effectivenessVerdict()).isEqualTo("EFFECTIVE");
        assertThat(remontee.effectivenessReviewedAt()).isEqualTo(JUGE_LE);
    }

    /**
     * LE DERNIER JUGEMENT GAGNE. La requete du dossier rend tout son historique
     * du plus recent au plus ancien, actions comprises : le verdict rendu depuis
     * couvre donc celui de l'action, et jamais l'inverse. Un rapport qui
     * afficherait un « partiellement » depasse sous un « efficace » rendu depuis
     * serait signe tel quel.
     */
    @Test
    void le_dernier_jugement_du_dossier_couvre_celui_de_l_action() {
        NonConformity nc = nc();
        nc.setCapaCaseId(CAPA);
        CapaCase dossier = capa();
        UUID action = dossier.getActions().get(0).getId();
        when(ncs.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.of(nc));
        when(capas.findByIdAndTenantId(CAPA, TENANT)).thenReturn(Optional.of(dossier));
        when(efficacite.findLastPerActionOfCase(TENANT, CAPA)).thenReturn(Map.of(
                action, jugement(action)));
        when(efficacite.findLastByCase(TENANT, CAPA)).thenReturn(Optional.of(
                EffectivenessReview.rehydrate(UUID.randomUUID(), TENANT, CAPA,
                        EffectivenessVerdict.EFFECTIVE, "Rebut sous 2 %", "4,2 %", "2 %", "0 %",
                        null, UUID.randomUUID(), "Vera Verificatrice", PLUS_TARD)));

        EightDSources sources = adapter.collect(TENANT, NC);

        EightDSources.Action remontee = sources.capas().get(0).actions().get(0);
        assertThat(remontee.effectivenessVerdict()).isEqualTo("EFFECTIVE");
        assertThat(remontee.effectivenessReviewedAt()).isEqualTo(PLUS_TARD);
    }

    /**
     * ET LE REPLI S'ARRETE LA : un dossier ancien qui porte plusieurs actions
     * ne recopie pas le verdict du dossier sur chaque ligne, ce qui pretendrait
     * juger chacune. Seules les actions jugees pour elles-memes portent un
     * verdict.
     */
    @Test
    void un_dossier_a_plusieurs_actions_ne_recopie_pas_le_verdict_du_dossier() {
        NonConformity nc = nc();
        nc.setCapaCaseId(CAPA);
        CapaCase dossier = capa();
        CapaAction seconde = new CapaAction();
        seconde.setId(UUID.randomUUID());
        seconde.setTitle("Former l'equipe de nuit");
        seconde.setActionType(CapaActionType.PREVENTIVE);
        seconde.setStatus(CapaActionStatus.DONE);
        dossier.getActions().add(seconde);
        when(ncs.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.of(nc));
        when(capas.findByIdAndTenantId(CAPA, TENANT)).thenReturn(Optional.of(dossier));

        EightDSources sources = adapter.collect(TENANT, NC);

        assertThat(sources.capas().get(0).actions())
                .extracting(EightDSources.Action::effectivenessVerdict)
                .containsOnlyNulls();
        verify(efficacite, never()).findLastByCase(any(), any());
    }

    /** Une piece sans nom d'origine se designe par sa cle : jamais une ligne vide. */
    @Test
    void une_piece_sans_nom_d_origine_se_designe_par_sa_cle() {
        NonConformity nc = nc();
        nc.setCapaCaseId(CAPA);
        when(ncs.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.of(nc));
        when(capas.findByIdAndTenantId(CAPA, TENANT)).thenReturn(Optional.of(capa()));
        CapaEvidence sansNom = piece(null, null);
        sansNom.setObjectKey("tenant/capa/2026/piece-17.bin");
        when(preuves.findCaseLevel(TENANT, CAPA)).thenReturn(List.of(sansNom));

        EightDSources sources = adapter.collect(TENANT, NC);

        assertThat(sources.capas().get(0).caseEvidenceNames())
                .containsExactly("tenant/capa/2026/piece-17.bin");
    }

    /** Un membre de l'equipe nomme sur la fiche. */
    private NcTeamMember membre(String nom, String compte) {
        NcTeamMember membre = new NcTeamMember();
        membre.setId(UUID.randomUUID());
        membre.setTenantId(TENANT);
        membre.setNcId(NC);
        membre.setDisplayName(nom);
        membre.setAccountId(compte);
        return membre;
    }

    /** Le dernier jugement porte sur une action : partiellement efficace. */
    private EffectivenessReview jugement(UUID action) {
        return EffectivenessReview.rehydrateForAction(UUID.randomUUID(), TENANT, CAPA, action,
                EffectivenessVerdict.PARTIAL, "Rebut sous 2 %", "4,2 %", "2 %", "2,5 %",
                null, UUID.randomUUID(), "Ada Lovelace", JUGE_LE);
    }

    private static final Instant JUGE_LE = Instant.parse("2026-10-02T09:00:00Z");

    /** Un jugement rendu APRES celui de l'action. */
    private static final Instant PLUS_TARD = Instant.parse("2026-10-20T09:00:00Z");

    /** Une piece deposee : son nom d'origine est ce que le D6 ecrit. */
    private CapaEvidence piece(UUID actionId, String nom) {
        CapaEvidence piece = new CapaEvidence();
        piece.setId(UUID.randomUUID());
        piece.setTenantId(TENANT);
        piece.setCapaId(CAPA);
        piece.setActionId(actionId);
        piece.setOriginalFilename(nom);
        return piece;
    }

    /**
     * L'INVARIANT DE CETTE D\u00c9CISION. Un \u00e9cart peut porter PLUSIEURS dossiers :
     * le premier ouvert depuis sa fiche, et un second ouvert depuis la liste en
     * citant sa r\u00e9f\u00e9rence, qui ne remplace pas le lien du premier (ADR 0121).
     *
     * <p>La cl\u00f4ture de l'\u00e9cart attend les deux ; le rapport n'en lisait qu'un.
     * Les actions du second retenaient donc la cl\u00f4ture sans appara\u00eetre nulle
     * part -- ni sur la fiche, ni dans le document remis au client.
     */
    @Test
    void les_dossiers_ouverts_depuis_la_liste_remontent_avec_celui_de_la_fiche() {
        NonConformity nc = nc();
        nc.setCapaCaseId(CAPA);
        CapaCase second = capa();
        second.setId(UUID.randomUUID());
        second.setTitle("Dossier ouvert depuis la liste");
        when(ncs.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.of(nc));
        when(capas.findByIdAndTenantId(CAPA, TENANT)).thenReturn(Optional.of(capa()));
        when(capas.findByTenantIdAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
                TENANT, CapaSourceType.NON_CONFORMITY, nc.getReference()))
                .thenReturn(List.of(second));

        EightDSources sources = adapter.collect(TENANT, NC);

        assertThat(sources.capas()).hasSize(2);
        assertThat(sources.capas().get(1).title()).isEqualTo("Dossier ouvert depuis la liste");
    }

    /** Le dossier de la fiche ne compte qu'UNE fois, meme s'il cite aussi la reference. */
    @Test
    void un_dossier_atteint_par_les_deux_chemins_ne_figure_qu_une_fois() {
        NonConformity nc = nc();
        nc.setCapaCaseId(CAPA);
        CapaCase lui = capa();
        when(ncs.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.of(nc));
        when(capas.findByIdAndTenantId(CAPA, TENANT)).thenReturn(Optional.of(lui));
        when(capas.findByTenantIdAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
                TENANT, CapaSourceType.NON_CONFORMITY, nc.getReference()))
                .thenReturn(List.of(lui));

        assertThat(adapter.collect(TENANT, NC).capas()).hasSize(1);
    }

    @Test
    void un_lien_de_capa_rompu_devient_une_absence_de_source_et_non_une_erreur() {
        // La CAPA a été supprimée : le rapport doit rester émissible, et le dire.
        NonConformity nc = nc();
        nc.setCapaCaseId(CAPA);
        when(ncs.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.of(nc));
        when(capas.findByIdAndTenantId(CAPA, TENANT)).thenReturn(Optional.empty());

        assertThat(adapter.collect(TENANT, NC).capas()).isEmpty();
    }

    @Test
    void les_analyses_de_cause_remontent_dans_l_ordre_de_la_methode() {
        when(ncs.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.of(nc()));
        when(ishikawas.findByTenantIdAndNcIdOrderByCreatedAtDesc(TENANT, NC))
                .thenReturn(List.of(diagramme()));
        FiveWhysAnalysis analyse = analyse();
        when(fiveWhys.findByNonConformityIdAndTenantIdOrderByCreatedAtDesc(NC, TENANT))
                .thenReturn(List.of(analyse));
        when(etapes.findByAnalysisIdAndTenantIdOrderByPositionAsc(analyse.getId(), TENANT))
                .thenReturn(List.of(etape("Le joint a cédé"), etape("Il n'a jamais été remplacé")));

        EightDSources sources = adapter.collect(TENANT, NC);

        assertThat(sources.ishikawas()).hasSize(1);
        assertThat(sources.ishikawas().get(0).causes()).hasSize(1);
        assertThat(sources.ishikawas().get(0).causes().get(0).category()).isEqualTo("MATERIALS");
        assertThat(sources.fiveWhys()).hasSize(1);
        assertThat(sources.fiveWhys().get(0).answers())
                .containsExactly("Le joint a cédé", "Il n'a jamais été remplacé");
    }

    @Test
    void le_mode_de_defaillance_pfmea_remonte_quand_la_nc_le_designe() {
        NonConformity nc = nc();
        nc.setFmeaItemId(FMEA);
        when(ncs.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.of(nc));
        FmeaItem item = new FmeaItem();
        item.setFailureMode("Fuite au joint");
        item.setRpn(160);
        item.setRpnAfter(40);
        when(fmeaItems.findByIdAndTenantId(FMEA, TENANT)).thenReturn(Optional.of(item));

        EightDSources sources = adapter.collect(TENANT, NC);

        assertThat(sources.fmea()).isNotNull();
        assertThat(sources.fmea().failureMode()).isEqualTo("Fuite au joint");
        assertThat(sources.fmea().rpn()).isEqualTo(160);
        assertThat(sources.fmea().rpnAfter()).isEqualTo(40);
    }

    @Test
    void les_plans_de_surveillance_se_trouvent_par_le_produit() {
        NonConformity nc = nc();
        nc.setProductId(PRODUIT);
        when(ncs.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.of(nc));
        when(plans.findByProduct(TENANT, PRODUIT)).thenReturn(List.of());

        EightDSources sources = adapter.collect(TENANT, NC);

        verify(plans).findByProduct(TENANT, PRODUIT);
        assertThat(sources.surveillance()).isEmpty();
    }

    // ---------- fixtures ----------

    private NonConformity nc() {
        NonConformity nc = new NonConformity();
        nc.setId(NC);
        nc.setTenantId(TENANT);
        nc.setReference("NC-2026-0007");
        nc.setTitle("Fuite au presse-étoupe");
        nc.setDescription("Flaque d'huile sous la pompe P-12");
        nc.setCategory(NcCategory.PROCESS);
        nc.setSeverity(NcSeverity.MAJOR);
        nc.setStatus(NcStatus.CLOSED);
        nc.setOrigin(NcOrigin.INTERNAL);
        nc.setDetectedAt(Instant.parse("2026-09-01T08:30:00Z"));
        nc.setClosedAt(Instant.parse("2026-09-12T16:00:00Z"));
        nc.setReporterName("Ada Lovelace");
        return nc;
    }

    private CapaCase capa() {
        CapaCase capa = new CapaCase();
        capa.setId(CAPA);
        capa.setTenantId(TENANT);
        capa.setTitle("Fuite presse-étoupe");
        capa.setType(CapaType.CORRECTIVE);
        capa.setCriticity(CapaCriticity.HIGH);
        capa.setStatus(CapaStatus.CLOSED);
        CapaAction action = new CapaAction();
        action.setId(UUID.randomUUID());
        action.setTitle("Remplacer le joint");
        action.setActionType(CapaActionType.CORRECTIVE);
        action.setStatus(CapaActionStatus.DONE);
        action.setCompletedAt(Instant.parse("2026-09-09T09:00:00Z"));
        capa.getActions().add(action);
        return capa;
    }

    private IshikawaDiagram diagramme() {
        IshikawaDiagram diagram = new IshikawaDiagram();
        diagram.setId(UUID.randomUUID());
        diagram.setTenantId(TENANT);
        diagram.setNcId(NC);
        diagram.setProblemStatement("Fuite au presse-étoupe");
        diagram.setStatus(IshikawaStatus.VALIDATED);
        IshikawaCause cause = new IshikawaCause();
        cause.setCategory(CauseCategory.MATERIALS);
        cause.setLabel("Joint usé");
        diagram.getCauses().add(cause);
        return diagram;
    }

    private FiveWhysAnalysis analyse() {
        FiveWhysAnalysis analyse = new FiveWhysAnalysis();
        analyse.setId(UUID.randomUUID());
        analyse.setTenantId(TENANT);
        analyse.setProblem("Fuite constatée");
        analyse.setRootCause("Absence de plan préventif");
        return analyse;
    }

    private FiveWhysStep etape(String reponse) {
        FiveWhysStep etape = new FiveWhysStep();
        etape.setAnswer(reponse);
        return etape;
    }
}
