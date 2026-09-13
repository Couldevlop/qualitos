package com.openlab.qualitos.quality.nonconformity.eightd.infrastructure;

import com.openlab.qualitos.quality.capa.CapaAction;
import com.openlab.qualitos.quality.capa.CapaActionStatus;
import com.openlab.qualitos.quality.capa.CapaActionType;
import com.openlab.qualitos.quality.capa.CapaCase;
import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.capa.CapaCriticity;
import com.openlab.qualitos.quality.capa.CapaEvidenceRepository;
import com.openlab.qualitos.quality.capa.CapaStatus;
import com.openlab.qualitos.quality.capa.CapaType;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    private IshikawaDiagramRepository ishikawas;
    private FiveWhysAnalysisRepository fiveWhys;
    private FiveWhysStepRepository etapes;
    private CapaCaseRepository capas;
    private CapaEvidenceRepository preuves;
    private FmeaItemRepository fmeaItems;
    private ControlPlanRepository plans;
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
        plans = mock(ControlPlanRepository.class);
        adapter = new EightDSourceAdapter(ncs, photos, ishikawas, fiveWhys, etapes,
                capas, preuves, fmeaItems, plans);

        when(photos.findByTenantIdAndNcIdOrderByCreatedAtAsc(any(), any())).thenReturn(List.of());
        when(ishikawas.findByTenantIdAndNcIdOrderByCreatedAtDesc(any(), any())).thenReturn(List.of());
        when(fiveWhys.findByNonConformityIdAndTenantIdOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of());
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

        assertThat(sources.capa()).isNull();
        assertThat(sources.fmea()).isNull();
        assertThat(sources.surveillance()).isEmpty();
        verifyNoInteractions(capas, fmeaItems, plans);
    }

    @Test
    void une_capa_escaladee_remonte_avec_ses_actions_et_leurs_preuves() {
        NonConformity nc = nc();
        nc.setCapaCaseId(CAPA);
        when(ncs.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.of(nc));
        when(capas.findByIdAndTenantId(CAPA, TENANT)).thenReturn(Optional.of(capa()));
        when(preuves.countCaseLevel(TENANT, CAPA)).thenReturn(3L);
        when(preuves.countByTenantIdAndActionId(any(), any())).thenReturn(1L);

        EightDSources sources = adapter.collect(TENANT, NC);

        assertThat(sources.capa()).isNotNull();
        assertThat(sources.capa().caseEvidenceCount()).isEqualTo(3L);
        assertThat(sources.capa().actions()).hasSize(1);
        assertThat(sources.capa().actions().get(0).evidenceCount()).isEqualTo(1L);
        assertThat(sources.capa().actions().get(0).actionType()).isEqualTo("CORRECTIVE");
    }

    @Test
    void un_lien_de_capa_rompu_devient_une_absence_de_source_et_non_une_erreur() {
        // La CAPA a été supprimée : le rapport doit rester émissible, et le dire.
        NonConformity nc = nc();
        nc.setCapaCaseId(CAPA);
        when(ncs.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.of(nc));
        when(capas.findByIdAndTenantId(CAPA, TENANT)).thenReturn(Optional.empty());

        assertThat(adapter.collect(TENANT, NC).capa()).isNull();
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
