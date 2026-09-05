package com.openlab.qualitos.quality.product.export;

import com.openlab.qualitos.quality.controlplan.application.ControlPlanDto;
import com.openlab.qualitos.quality.controlplan.application.ControlPlanService;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanPhase;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanStatus;
import com.openlab.qualitos.quality.product.application.ProductDto;
import com.openlab.qualitos.quality.product.application.ProductService;
import com.openlab.qualitos.quality.product.domain.ProductNotFoundException;
import com.openlab.qualitos.quality.product.domain.ProductStatus;
import com.openlab.qualitos.quality.risk.FmeaDto;
import com.openlab.qualitos.quality.risk.FmeaService;
import com.openlab.qualitos.quality.risk.FmeaStatus;
import com.openlab.qualitos.quality.risk.FmeaType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ce que le service CHOISIT d'exporter.
 *
 * <p>Le rendu du classeur est deja couvert par {@link ProductWorkbookTest} ;
 * ici on tient les decisions qui le precedent : quelle analyse est retenue,
 * quel plan de surveillance, et ce qui se passe quand l'un ou l'autre manque.
 * Ce sont ces decisions-la qui font qu'un auditeur recoit le bon dossier plutot
 * qu'un brouillon abandonne.
 */
@DisplayName("ProductExportService")
@ExtendWith(MockitoExtension.class)
class ProductExportServiceTest {

    private static final UUID PRODUIT = UUID.randomUUID();

    @Mock ProductService produits;
    @Mock FmeaService fmea;
    @Mock ControlPlanService plans;

    ProductExportService service;

    @BeforeEach
    void monte() {
        service = new ProductExportService(produits, fmea, plans);
    }

    @Test
    void retientLAnalyseActiveEtNonLaPlusRecente() {
        // Un brouillon cree hier ne remplace pas l'analyse en vigueur : c'est
        // l'ACTIVE que l'ecran montre, et c'est elle que l'audit attend.
        produitConnu();
        FmeaDto.ProjectResponse brouillon = projet(FmeaStatus.DRAFT);
        FmeaDto.ProjectResponse active = projet(FmeaStatus.ACTIVE);
        when(fmea.listProjects(any(), eq(FmeaType.PROCESS_FMEA), eq(PRODUIT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(brouillon, active)));
        when(fmea.listItems(eq(active.id()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item(1, 160))));
        when(plans.listForProduct(PRODUIT)).thenReturn(List.of());

        service.export(PRODUIT);

        verify(fmea).listItems(eq(active.id()), any(Pageable.class));
        verify(fmea, never()).listItems(eq(brouillon.id()), any(Pageable.class));
    }

    @Test
    void sansAnalyseActiveRetientLaPlusRecente() {
        // Aucune n'est en vigueur : mieux vaut le dernier etat de la reflexion
        // qu'une feuille vide, qui laisserait croire qu'on n'a rien analyse.
        produitConnu();
        FmeaDto.ProjectResponse recent = projet(FmeaStatus.DRAFT);
        FmeaDto.ProjectResponse ancien = projet(FmeaStatus.ARCHIVED);
        // Le service demande deja un tri decroissant sur createdAt : le premier
        // de la page EST le plus recent.
        when(fmea.listProjects(any(), any(), eq(PRODUIT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(recent, ancien)));
        when(fmea.listItems(eq(recent.id()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item(1, 160))));
        when(plans.listForProduct(PRODUIT)).thenReturn(List.of());

        service.export(PRODUIT);

        verify(fmea).listItems(eq(recent.id()), any(Pageable.class));
    }

    @Test
    void demandeLesProjetsLesPlusRecentsDAbord() {
        // Le choix « le plus recent » ci-dessus ne tient QUE si la page est
        // triee ainsi. Le tri est donc affirme, pas suppose.
        produitConnu();
        when(fmea.listProjects(any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(plans.listForProduct(PRODUIT)).thenReturn(List.of());

        service.export(PRODUIT);

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(fmea).listProjects(any(), any(), any(), page.capture());
        Sort.Order ordre = page.getValue().getSort().getOrderFor("createdAt");
        assertThat(ordre).isNotNull();
        assertThat(ordre.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void plafonneLeNombreDeLignesLues() {
        // Un clic ne doit pas pouvoir immobiliser le service sur une analyse
        // qui aurait derive a des dizaines de milliers de lignes.
        produitConnu();
        FmeaDto.ProjectResponse actif = projet(FmeaStatus.ACTIVE);
        when(fmea.listProjects(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(actif)));
        when(fmea.listItems(any(), any(Pageable.class))).thenReturn(Page.empty());
        when(plans.listForProduct(PRODUIT)).thenReturn(List.of());

        service.export(PRODUIT);

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(fmea).listItems(any(), page.capture());
        assertThat(page.getValue().getPageSize()).isEqualTo(ProductExportService.MAX_ROWS);
    }

    @Test
    void retientLaRevisionLaPlusElevee() {
        // Un produit porte souvent un plan approuve et une revision en cours :
        // c'est la plus avancee que l'ecran ouvre, et qu'un auditeur demande.
        produitConnu();
        sansAnalyse();
        ControlPlanDto.View v1 = plan(1);
        ControlPlanDto.View v4 = plan(4);
        when(plans.listForProduct(PRODUIT)).thenReturn(List.of(v1, v4));
        when(plans.get(PRODUIT, v4.id())).thenReturn(new ControlPlanDto.Detail(v4, List.of()));

        service.export(PRODUIT);

        verify(plans).get(PRODUIT, v4.id());
        verify(plans, never()).get(PRODUIT, v1.id());
    }

    @Test
    void unModuleFermeNeFaitPasTomberLExportDuPfmea() throws IOException {
        // Le client n'a pas souscrit le plan de surveillance : le garde de
        // module refuse la lecture. Perdre le PFMEA avec lui serait
        // disproportionne - la feuille sort vide et dit ce qu'elle est.
        produitConnu();
        FmeaDto.ProjectResponse actif = projet(FmeaStatus.ACTIVE);
        when(fmea.listProjects(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(actif)));
        when(fmea.listItems(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item(1, 160))));
        when(plans.listForProduct(PRODUIT)).thenThrow(new IllegalStateException("module ferme"));

        ProductExportService.Export export = service.export(PRODUIT);

        try (Workbook w = new XSSFWorkbook(new ByteArrayInputStream(export.content()))) {
            assertThat(w.getNumberOfSheets()).isEqualTo(2);
            // La ligne du PFMEA est bien la : l'incident du plan n'a rien emporte.
            assertThat(w.getSheetAt(0).getRow(3)).isNotNull();
        }
    }

    @Test
    void unProduitInconnuRemonteTelQuel() {
        // Le service produit filtre deja par client : un produit d'un autre
        // tenant est « inconnu » vu d'ici, et l'export n'a pas a le deguiser en
        // classeur vide, ce qui ferait croire a un dossier reellement vierge.
        when(produits.get(PRODUIT)).thenThrow(new ProductNotFoundException(PRODUIT));

        assertThatThrownBy(() -> service.export(PRODUIT))
                .isInstanceOf(ProductNotFoundException.class);
        verify(fmea, never()).listProjects(any(), any(), any(), any(Pageable.class));
    }

    @Test
    void leNomDuFichierEstReduitAuxCaracteresSurs() {
        // Un `/` dans un code produit ouvre une traversee de chemin au moment
        // de l'enregistrement, et un accent casse l'en-tete selon le navigateur.
        assertThat(ProductExportService.filename(produit("Ref/Ete 2026")))
                .isEqualTo("ref-ete-2026-pfmea-plan-surveillance.xlsx");
    }

    @Test
    void lesAccentsSontReduitsSansPerdreLaLettre() {
        // « Réf » doit devenir « ref » et non « r-f » : sinon deux produits
        // distincts sortiraient sous le meme nom de fichier.
        assertThat(ProductExportService.filename(produit("Réf-Été")))
                .isEqualTo("ref-ete-pfmea-plan-surveillance.xlsx");
    }

    @Test
    void unCodeSansAucunCaractereSurRetombeSurUnNomNeutre() {
        // Sinon le fichier arriverait nomme « -pfmea... », voire sans base.
        assertThat(ProductExportService.filename(produit("///")))
                .isEqualTo("produit-pfmea-plan-surveillance.xlsx");
    }

    @Test
    void unCodeAbsentRetombeAussiSurUnNomNeutre() {
        assertThat(ProductExportService.filename(produit(null)))
                .isEqualTo("produit-pfmea-plan-surveillance.xlsx");
    }

    @Test
    void leClasseurPorteLeCodeEtLaDesignationDuProduit() throws IOException {
        // Une feuille de cotation qu'on ne rattache plus a un produit ne vaut
        // rien : le titre doit suffire a l'identifier hors de la plateforme.
        when(produits.get(PRODUIT)).thenReturn(new ProductDto.View(
                PRODUIT, "P-001", "Support moteur", "Mecanique", "B",
                ProductStatus.ACTIVE, "Client A", "Site 1", UUID.randomUUID(),
                Instant.now(), Instant.now()));
        sansAnalyse();
        when(plans.listForProduct(PRODUIT)).thenReturn(List.of());

        ProductExportService.Export export = service.export(PRODUIT);

        try (Workbook w = new XSSFWorkbook(new ByteArrayInputStream(export.content()))) {
            assertThat(w.getSheetAt(0).getRow(0).getCell(0).getStringCellValue())
                    .contains("P-001").contains("Support moteur");
        }
        assertThat(export.filename()).isEqualTo("p-001-pfmea-plan-surveillance.xlsx");
    }

    @Test
    void uneDesignationAbsenteNeLaissePasLeMotNullDansLeTitre() {
        // `null` concatene rendrait litteralement « P-001 - null » en tete de
        // feuille, ce qui part chez le client tel quel.
        when(produits.get(PRODUIT)).thenReturn(new ProductDto.View(
                PRODUIT, "P-001", null, "Mecanique", "A",
                ProductStatus.ACTIVE, "Client A", "Site 1", UUID.randomUUID(),
                Instant.now(), Instant.now()));
        sansAnalyse();
        when(plans.listForProduct(PRODUIT)).thenReturn(List.of());

        ProductExportService.Export export = service.export(PRODUIT);

        try (Workbook w = new XSSFWorkbook(new ByteArrayInputStream(export.content()))) {
            assertThat(w.getSheetAt(0).getRow(0).getCell(0).getStringCellValue())
                    .doesNotContain("null");
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    // ---------- montage ----------

    private void produitConnu() {
        when(produits.get(PRODUIT)).thenReturn(produit("P-001"));
    }

    /** Aucun PFMEA : la page de projets revient vide. */
    private void sansAnalyse() {
        when(fmea.listProjects(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());
    }

    private static ProductDto.View produit(String code) {
        return new ProductDto.View(PRODUIT, code, "Support", "Mecanique", "A",
                ProductStatus.ACTIVE, "Client A", "Site 1", UUID.randomUUID(),
                Instant.now(), Instant.now());
    }

    private static FmeaDto.ProjectResponse projet(FmeaStatus statut) {
        return new FmeaDto.ProjectResponse(UUID.randomUUID(), UUID.randomUUID(),
                "PF-001", "PFMEA support", "Ligne 1", FmeaType.PROCESS_FMEA, statut,
                100, 1, PRODUIT, UUID.randomUUID(), Instant.now(), UUID.randomUUID(),
                Instant.now(), Instant.now());
    }

    private static ControlPlanDto.View plan(int revision) {
        return new ControlPlanDto.View(UUID.randomUUID(), PRODUIT,
                ControlPlanPhase.PRODUCTION, "CP-001", revision, ControlPlanStatus.ACTIVE,
                UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
                Instant.now(), Instant.now(), "sha", null);
    }

    private static FmeaDto.ItemResponse item(int rang, int rpn) {
        return new FmeaDto.ItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), rang,
                "Assembler", "Sertissage incomplet", "Perte de contact",
                "Outil use", "Controle visuel",
                8, 4, 5, rpn,
                "Ajouter un capteur", UUID.randomUUID(), "A. Martin",
                LocalDate.of(2026, 11, 30), "Capteur pose", LocalDate.of(2026, 10, 15),
                8, 2, 3, 48,
                null, null, null, false, Instant.now(), Instant.now());
    }
}
