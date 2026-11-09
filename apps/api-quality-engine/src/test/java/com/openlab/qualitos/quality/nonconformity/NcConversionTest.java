package com.openlab.qualitos.quality.nonconformity;

import com.openlab.qualitos.quality.authz.application.RecordScope;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.dmaic.DmaicDto;
import com.openlab.qualitos.quality.dmaic.DmaicPhase;
import com.openlab.qualitos.quality.dmaic.DmaicService;
import com.openlab.qualitos.quality.dmaic.DmaicStatus;
import com.openlab.qualitos.quality.nonconformity.subcategory.NcSubcategoryCatalog;
import com.openlab.qualitos.quality.pdca.PdcaDto;
import com.openlab.qualitos.quality.pdca.PdcaService;
import com.openlab.qualitos.quality.pdca.PdcaStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * « Une non-conformité devient un PDCA ou un projet DMAIC selon sa gravité. »
 *
 * <p>Ce chemin manquait. Un écart partait en CAPA — une action corrective et sa
 * vérification d'efficacité — mais la CAPA ne couvre pas tout : un écart qui
 * révèle un procédé instable demande des mesures et une phase de maîtrise, un
 * écart qui révèle une pratique à corriger demande un cycle. Les deux modules
 * existaient, sans porte depuis l'écart.
 *
 * <p>Ce qui est tenu ici, dans l'ordre : la gravité choisit la méthode ; ce que
 * le cycle ou le projet reprend de l'écart, sans quoi, ouvert seul, il ne
 * saurait plus d'où il vient ; et les refus — deux fois le même écart, un écart
 * écarté, un écart d'un autre client.
 */
@ExtendWith(MockitoExtension.class)
class NcConversionTest {

    @Mock NonConformityRepository repo;
    @Mock CapaService capaService;
    @Mock PdcaService pdcaService;
    @Mock DmaicService dmaicService;
    @Mock ApplicationEventPublisher events;
    @Mock RecordScope scope;
    @Mock NcSubcategoryCatalog subcategories;
    @InjectMocks NcService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID PILOTE = UUID.randomUUID();

    @BeforeEach void ctx() { TenantContext.setTenantId(TENANT.toString()); }
    @AfterEach  void clr() { TenantContext.clear(); }

    // ---------- la gravité choisit, et la personne peut trancher autrement ----------

    @Test
    void ecartMajeur_ouvreUnCyclePdca_quiCiteLEcart() {
        NonConformity n = ecart(NcSeverity.MAJOR, NcStatus.UNDER_ANALYSIS);
        n.setTitle("Jeu excessif sur le mandrin");
        n.setDescription("  Constate au changement de serie.  ");
        n.setZone("Atelier B");
        attendu(n);
        UUID cycleId = UUID.randomUUID();
        when(pdcaService.createCycle(any())).thenReturn(cycle(cycleId));

        NcDto.Response r = service.convert(n.getId(), new NcDto.ConvertRequest(null, PILOTE, null));

        ArgumentCaptor<PdcaDto.CreateCycleRequest> cap =
                ArgumentCaptor.forClass(PdcaDto.CreateCycleRequest.class);
        verify(pdcaService).createCycle(cap.capture());
        assertThat(cap.getValue().title()).isEqualTo("PDCA — Jeu excessif sur le mandrin");
        assertThat(cap.getValue().ownerId()).isEqualTo(PILOTE);
        assertThat(cap.getValue().description())
                .contains("NC-2026-0001").contains("MAJOR")
                .contains("Jeu excessif sur le mandrin")
                .contains("Constate au changement de serie.")
                .contains("Zone : Atelier B.");
        assertThat(r.pdcaCycleId()).isEqualTo(cycleId);
        assertThat(r.dmaicProjectId()).isNull();
        verifyNoInteractions(dmaicService);
    }

    @Test
    void ecartMineur_ouvreAussiUnCyclePdca() {
        NonConformity n = ecart(NcSeverity.MINOR, NcStatus.OPEN);
        attendu(n);
        when(pdcaService.createCycle(any())).thenReturn(cycle(UUID.randomUUID()));

        service.convert(n.getId(), new NcDto.ConvertRequest(null, PILOTE, null));

        verify(pdcaService).createCycle(any());
        verifyNoInteractions(dmaicService);
    }

    @Test
    void ecartCritique_ouvreUnProjetDmaic_avecSonEcheanceEtSonObjectif() {
        NonConformity n = ecart(NcSeverity.CRITICAL, NcStatus.OPEN);
        attendu(n);
        UUID projetId = UUID.randomUUID();
        when(dmaicService.createProject(any())).thenReturn(projet(projetId));
        LocalDate echeance = LocalDate.now().plusMonths(3);

        NcDto.Response r = service.convert(n.getId(),
                new NcDto.ConvertRequest(null, PILOTE, echeance));

        ArgumentCaptor<DmaicDto.CreateProjectRequest> cap =
                ArgumentCaptor.forClass(DmaicDto.CreateProjectRequest.class);
        verify(dmaicService).createProject(cap.capture());
        assertThat(cap.getValue().title()).startsWith("DMAIC — ");
        assertThat(cap.getValue().blackBeltId()).isEqualTo(PILOTE);
        assertThat(cap.getValue().targetCompletionDate()).isEqualTo(echeance);
        assertThat(cap.getValue().goalStatement()).contains("NC-2026-0001");
        assertThat(cap.getValue().problemStatement()).contains("CRITICAL");
        assertThat(r.dmaicProjectId()).isEqualTo(projetId);
        assertThat(r.pdcaCycleId()).isNull();
        verifyNoInteractions(pdcaService);
    }

    @Test
    void laMethodeDemandee_lEmporteSurLaGravite() {
        // Un ecart majeur sur un procede deja sous carte de controle peut meriter
        // un DMAIC : la gravite propose, la personne qui tient le dossier decide.
        NonConformity n = ecart(NcSeverity.MAJOR, NcStatus.OPEN);
        attendu(n);
        when(dmaicService.createProject(any())).thenReturn(projet(UUID.randomUUID()));

        service.convert(n.getId(), new NcDto.ConvertRequest(NcMethod.DMAIC, PILOTE, null));

        verify(dmaicService).createProject(any());
        verifyNoInteractions(pdcaService);
    }

    @Test
    void laTableDeCorrespondance_estLisibleSeule() {
        assertThat(NcMethod.fromSeverity(NcSeverity.MINOR)).isEqualTo(NcMethod.PDCA);
        assertThat(NcMethod.fromSeverity(NcSeverity.MAJOR)).isEqualTo(NcMethod.PDCA);
        assertThat(NcMethod.fromSeverity(NcSeverity.CRITICAL)).isEqualTo(NcMethod.DMAIC);
    }

    // ---------- ce qui est repris, et ce qui ne fait pas echouer ----------

    @Test
    void ecartSansDescriptionNiZone_donneUnEnonceQuiTientDebout() {
        NonConformity n = ecart(NcSeverity.MAJOR, NcStatus.OPEN);
        n.setDescription("   ");
        n.setZone(null);
        attendu(n);
        when(pdcaService.createCycle(any())).thenReturn(cycle(UUID.randomUUID()));

        service.convert(n.getId(), new NcDto.ConvertRequest(null, PILOTE, null));

        ArgumentCaptor<PdcaDto.CreateCycleRequest> cap =
                ArgumentCaptor.forClass(PdcaDto.CreateCycleRequest.class);
        verify(pdcaService).createCycle(cap.capture());
        assertThat(cap.getValue().description())
                .contains("NC-2026-0001")
                .doesNotContain("Zone :")
                .doesNotContain("null");
    }

    @Test
    void titreTresLong_estTronqueAuLieuDEchouer() {
        // La colonne fait 255 : un titre d'ecart bavard ne doit pas faire echouer
        // la conversion au moment de l'insertion.
        NonConformity n = ecart(NcSeverity.MAJOR, NcStatus.OPEN);
        n.setTitle("x".repeat(300));
        attendu(n);
        when(pdcaService.createCycle(any())).thenReturn(cycle(UUID.randomUUID()));

        service.convert(n.getId(), new NcDto.ConvertRequest(null, PILOTE, null));

        ArgumentCaptor<PdcaDto.CreateCycleRequest> cap =
                ArgumentCaptor.forClass(PdcaDto.CreateCycleRequest.class);
        verify(pdcaService).createCycle(cap.capture());
        assertThat(cap.getValue().title()).hasSize(255).endsWith("…");
    }

    @Test
    void descriptionFleuve_estTronqueeAussi() {
        NonConformity n = ecart(NcSeverity.MAJOR, NcStatus.OPEN);
        n.setDescription("y".repeat(5000));
        attendu(n);
        when(pdcaService.createCycle(any())).thenReturn(cycle(UUID.randomUUID()));

        service.convert(n.getId(), new NcDto.ConvertRequest(null, PILOTE, null));

        ArgumentCaptor<PdcaDto.CreateCycleRequest> cap =
                ArgumentCaptor.forClass(PdcaDto.CreateCycleRequest.class);
        verify(pdcaService).createCycle(cap.capture());
        assertThat(cap.getValue().description()).hasSize(4000).endsWith("…");
    }

    // ---------- les refus ----------

    @Test
    void deuxFoisLeMemeEcart_refuse_cotePdca() {
        // Deux dossiers ouverts sur le meme ecart le traiteraient deux fois, et
        // chacun compterait dans les indicateurs.
        NonConformity n = ecart(NcSeverity.MAJOR, NcStatus.OPEN);
        n.setPdcaCycleId(UUID.randomUUID());
        attendu(n);

        assertThatThrownBy(() -> service.convert(n.getId(),
                new NcDto.ConvertRequest(null, PILOTE, null)))
                .isInstanceOf(NcStateException.class)
                .hasMessageContaining("already converted to PDCA");
        verifyNoInteractions(pdcaService, dmaicService);
    }

    @Test
    void deuxFoisLeMemeEcart_refuse_coteDmaic() {
        NonConformity n = ecart(NcSeverity.CRITICAL, NcStatus.OPEN);
        n.setDmaicProjectId(UUID.randomUUID());
        attendu(n);

        assertThatThrownBy(() -> service.convert(n.getId(),
                new NcDto.ConvertRequest(null, PILOTE, null)))
                .isInstanceOf(NcStateException.class)
                .hasMessageContaining("already converted to DMAIC");
        verifyNoInteractions(pdcaService, dmaicService);
    }

    @Test
    void ecartEcarte_refuse() {
        // On a juge qu'il n'y avait rien a traiter : ouvrir un cycle dessus
        // ferait compter un chantier d'amelioration qui n'a pas d'objet.
        NonConformity n = ecart(NcSeverity.MAJOR, NcStatus.REJECTED);
        attendu(n);

        assertThatThrownBy(() -> service.convert(n.getId(),
                new NcDto.ConvertRequest(null, PILOTE, null)))
                .isInstanceOf(NcStateException.class)
                .hasMessageContaining("nothing left to treat");
        verifyNoInteractions(pdcaService, dmaicService);
    }

    @Test
    void ecartDUnAutreClient_introuvable() {
        UUID inconnu = UUID.randomUUID();
        when(repo.findForUpdateByIdAndTenantId(inconnu, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.convert(inconnu,
                new NcDto.ConvertRequest(null, PILOTE, null)))
                .isInstanceOf(NcNotFoundException.class);
        verifyNoInteractions(pdcaService, dmaicService);
    }

    // ---------- fixtures ----------

    /**
     * La fiche est chargee SOUS VERROU : deux onglets ne doivent pas ouvrir deux
     * cycles dont le second effacerait le lien du premier.
     *
     * <p>L'enregistrement est pose en {@code lenient} : les cas de refus
     * n'atteignent jamais {@code save}, et un stub strict les ferait echouer
     * pour stub inutilise — un echec qui ne dirait rien du comportement teste.
     */
    private void attendu(NonConformity n) {
        when(repo.findForUpdateByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        org.mockito.Mockito.lenient().when(repo.save(n)).thenReturn(n);
    }

    private NonConformity ecart(NcSeverity severite, NcStatus statut) {
        NonConformity n = new NonConformity();
        n.setId(UUID.randomUUID());
        n.setTenantId(TENANT);
        n.setReference("NC-2026-0001");
        n.setTitle("Ecart constate");
        n.setCategory(NcCategory.PRODUCT);
        n.setSeverity(severite);
        n.setStatus(statut);
        n.setDetectedAt(Instant.now());
        n.setCreatedAt(Instant.now());
        n.setUpdatedAt(Instant.now());
        return n;
    }

    private static PdcaDto.CycleResponse cycle(UUID id) {
        return new PdcaDto.CycleResponse(id, TENANT, "t", null, PdcaStatus.PLAN,
                PILOTE, Instant.now(), Instant.now(), null, List.of());
    }

    private static DmaicDto.ProjectResponse projet(UUID id) {
        return new DmaicDto.ProjectResponse(id, TENANT, "t", null, null,
                DmaicPhase.DEFINE, DmaicStatus.ACTIVE, null, PILOTE, null,
                null, null, null, null, null, 0, 0, null, null,
                Instant.now(), Instant.now());
    }
}
