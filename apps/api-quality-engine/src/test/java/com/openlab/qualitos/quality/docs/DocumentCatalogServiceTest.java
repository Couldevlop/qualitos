package com.openlab.qualitos.quality.docs;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentCatalogServiceTest {

    @Mock DocumentFolderRepository folders;
    @Mock DocumentProcessRepository processes;
    @Mock DocumentSettingsRowRepository settings;
    @Mock DocumentRepository documents;
    @Mock DocumentCatalogSeeder seeder;
    DocumentCatalogService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID ADMIN = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DocumentCatalogService(folders, processes, settings, documents, seeder);
        TenantContext.setTenantId(TENANT.toString());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ADMIN.toString(), "n/a", List.of()));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    static DocumentFolder folder(UUID parent, String name, int position) {
        DocumentFolder f = new DocumentFolder();
        f.setId(UUID.randomUUID());
        f.setTenantId(TENANT);
        f.setParentId(parent);
        f.setName(name);
        f.setPosition(position);
        return f;
    }

    static DocumentProcess process(String name, int position) {
        DocumentProcess p = new DocumentProcess();
        p.setId(UUID.randomUUID());
        p.setTenantId(TENANT);
        p.setName(name);
        p.setPosition(position);
        return p;
    }

    /** Une chaîne racine → … de {@code n} niveaux. */
    static List<DocumentFolder> chain(int n) {
        List<DocumentFolder> all = new ArrayList<>();
        UUID parent = null;
        for (int i = 0; i < n; i++) {
            DocumentFolder f = folder(parent, "N" + i, 0);
            all.add(f);
            parent = f.getId();
        }
        return all;
    }

    void seeded() {
        when(settings.existsById(TENANT)).thenReturn(true);
    }

    void tree(List<DocumentFolder> all) {
        when(folders.findByTenantIdOrderByPositionAscNameAsc(TENANT)).thenReturn(all);
    }

    void savesFolder() {
        when(folders.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- paramétrage ----------

    @Test
    void settingsOf_sansLigne_rendLesValeursDeLaPlateforme() {
        when(settings.findById(TENANT)).thenReturn(Optional.empty());
        DocumentSettingsRow s = service.settingsOf(TENANT);
        assertThat(s.getTenantId()).isEqualTo(TENANT);
        assertThat(s.getMaxFileMb()).isEqualTo(DocumentSettingsRow.DEFAULT_MAX_FILE_MB);
        assertThat(s.getQuotaMb()).isNull();
    }

    @Test
    void settings_vue() {
        DocumentSettingsRow row = DocumentSettingsRow.defaults(TENANT);
        row.setMaxFileMb(10);
        row.setQuotaMb(500);
        row.setObsoleteRetentionYears(5);
        when(settings.findById(TENANT)).thenReturn(Optional.of(row));
        DocumentDto.SettingsView v = service.settings();
        assertThat(v).isEqualTo(new DocumentDto.SettingsView(24, 30, 10,
                DocumentSettingsRow.PLATFORM_MAX_FILE_MB, 500, 5));
    }

    @Test
    void settings_sansTenant_refuse() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.settings()).isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void replaceSettings_premierReglage_semeEtEnregistre() {
        when(settings.existsById(TENANT)).thenReturn(false);
        when(settings.findById(TENANT)).thenReturn(Optional.empty());
        when(settings.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DocumentDto.SettingsView v = service.replaceSettings(new DocumentDto.SettingsForm(12, 15, 20, 1000, 7));

        verify(seeder).seed(TENANT);
        ArgumentCaptor<DocumentSettingsRow> saved = ArgumentCaptor.forClass(DocumentSettingsRow.class);
        verify(settings).save(saved.capture());
        assertThat(saved.getValue().getUpdatedBy()).isEqualTo(ADMIN);
        assertThat(v).isEqualTo(new DocumentDto.SettingsView(12, 15, 20,
                DocumentSettingsRow.PLATFORM_MAX_FILE_MB, 1000, 7));
    }

    @Test
    void replaceSettings_ligneExistante_misAJour() {
        seeded();
        DocumentSettingsRow row = DocumentSettingsRow.defaults(TENANT);
        when(settings.findById(TENANT)).thenReturn(Optional.of(row));
        when(settings.save(row)).thenReturn(row);

        service.replaceSettings(new DocumentDto.SettingsForm(6, 0, 1, null, null));

        verify(seeder, never()).seed(any());
        assertThat(row.getReviewPeriodMonths()).isEqualTo(6);
        assertThat(row.getQuotaMb()).isNull();
    }

    @Test
    void ensureSeeded_concurrence_absorbee() {
        when(settings.existsById(TENANT)).thenReturn(false);
        doThrow(new DataIntegrityViolationException("dup")).when(seeder).seed(TENANT);
        assertThatCode(() -> service.ensureSeeded(TENANT)).doesNotThrowAnyException();
    }

    @Test
    void uploadPolicy_listeBlancheEtTaille() {
        when(settings.findById(TENANT)).thenReturn(Optional.empty());
        DocumentDto.UploadPolicy p = service.uploadPolicy();
        assertThat(p.maxFileMb()).isEqualTo(25);
        assertThat(p.allowedTypes()).hasSize(15).contains("application/pdf", "text/xml");
        // « xml » n'apparaît qu'une fois malgré deux types XML
        assertThat(p.allowedExtensions()).hasSize(14).containsOnlyOnce("xml");
    }

    // ---------- arborescence ----------

    @Test
    void folders_semeEtListe() {
        when(settings.existsById(TENANT)).thenReturn(false);
        DocumentFolder f = folder(null, "Procédures", 1);
        tree(List.of(f));
        List<DocumentDto.FolderView> out = service.folders();
        verify(seeder).seed(TENANT);
        assertThat(out).containsExactly(new DocumentDto.FolderView(f.getId(), null, "Procédures", 1));
    }

    @Test
    void createFolder_racine_positionSuivante() {
        seeded();
        tree(List.of(folder(null, "A", 0), folder(null, "B", 3), folder(UUID.randomUUID(), "C", 9)));
        savesFolder();
        DocumentDto.FolderView v = service.createFolder(new DocumentDto.FolderRequest("  Nouveau  ", null, null));
        assertThat(v.name()).isEqualTo("Nouveau");
        assertThat(v.parentId()).isNull();
        assertThat(v.position()).isEqualTo(4);
    }

    @Test
    void createFolder_premierSousDossier_positionZero_etPositionExplicite() {
        seeded();
        DocumentFolder root = folder(null, "A", 0);
        tree(List.of(root));
        savesFolder();
        assertThat(service.createFolder(new DocumentDto.FolderRequest("X", root.getId(), null)).position()).isZero();
        assertThat(service.createFolder(new DocumentDto.FolderRequest("Y", root.getId(), 7)).position()).isEqualTo(7);
    }

    @Test
    void createFolder_parentInconnu_404() {
        seeded();
        tree(List.of());
        UUID ghost = UUID.randomUUID();
        assertThatThrownBy(() -> service.createFolder(new DocumentDto.FolderRequest("X", ghost, null)))
                .isInstanceOf(DocumentCatalogNotFoundException.class);
    }

    @Test
    void createFolder_auDelaDeQuatreNiveaux_refuse() {
        seeded();
        List<DocumentFolder> all = chain(4);
        tree(all);
        assertThatThrownBy(() -> service.createFolder(new DocumentDto.FolderRequest("X", all.get(3).getId(), null)))
                .isInstanceOf(DocumentStateException.class).hasMessageContaining("4 levels");
    }

    @Test
    void createFolder_quatriemeNiveau_admis() {
        seeded();
        List<DocumentFolder> all = chain(3);
        tree(all);
        savesFolder();
        assertThat(service.createFolder(new DocumentDto.FolderRequest("X", all.get(2).getId(), null)).parentId())
                .isEqualTo(all.get(2).getId());
    }

    @Test
    void createFolder_nomDejaPrisChezLesFreres_sansEgardALaCasse() {
        seeded();
        DocumentFolder root = folder(null, "Procédures", 0);
        tree(List.of(root, folder(root.getId(), "Achats", 0)));
        assertThatThrownBy(() -> service.createFolder(new DocumentDto.FolderRequest("PROCÉDURES", null, null)))
                .isInstanceOf(DocumentStateException.class);
        assertThatThrownBy(() -> service.createFolder(new DocumentDto.FolderRequest("achats", root.getId(), null)))
                .isInstanceOf(DocumentStateException.class);
    }

    @Test
    void createFolder_memeNomSousUnAutreParent_admis() {
        seeded();
        DocumentFolder a = folder(null, "A", 0);
        tree(List.of(a, folder(a.getId(), "Achats", 0)));
        savesFolder();
        assertThat(service.createFolder(new DocumentDto.FolderRequest("Achats", null, null)).name()).isEqualTo("Achats");
    }

    @Test
    void updateFolder_inconnu_404() {
        tree(List.of());
        assertThatThrownBy(() -> service.updateFolder(UUID.randomUUID(), new DocumentDto.FolderRequest("X", null, null)))
                .isInstanceOf(DocumentCatalogNotFoundException.class);
    }

    @Test
    void updateFolder_renommeSurPlace_gardeSaPositionSansPositionFournie() {
        DocumentFolder f = folder(null, "Ancien", 5);
        tree(List.of(f));
        savesFolder();
        DocumentDto.FolderView v = service.updateFolder(f.getId(), new DocumentDto.FolderRequest("ancien", null, null));
        assertThat(v.name()).isEqualTo("ancien");
        assertThat(v.position()).isEqualTo(5);
    }

    @Test
    void updateFolder_positionFournie_appliquee() {
        DocumentFolder f = folder(null, "A", 5);
        tree(List.of(f));
        savesFolder();
        assertThat(service.updateFolder(f.getId(), new DocumentDto.FolderRequest("A", null, 2)).position()).isEqualTo(2);
    }

    @Test
    void updateFolder_remonteALaRacine() {
        DocumentFolder root = folder(null, "R", 0);
        DocumentFolder child = folder(root.getId(), "C", 0);
        tree(List.of(root, child));
        savesFolder();
        assertThat(service.updateFolder(child.getId(), new DocumentDto.FolderRequest("C", null, null)).parentId())
                .isNull();
    }

    @Test
    void updateFolder_deplaceSousUnAutre() {
        DocumentFolder a = folder(null, "A", 0);
        DocumentFolder b = folder(null, "B", 1);
        tree(List.of(a, b));
        savesFolder();
        assertThat(service.updateFolder(b.getId(), new DocumentDto.FolderRequest("B", a.getId(), null)).parentId())
                .isEqualTo(a.getId());
    }

    @Test
    void updateFolder_nouveauParentInconnu_404() {
        DocumentFolder a = folder(null, "A", 0);
        tree(List.of(a));
        assertThatThrownBy(() -> service.updateFolder(a.getId(),
                new DocumentDto.FolderRequest("A", UUID.randomUUID(), null)))
                .isInstanceOf(DocumentCatalogNotFoundException.class);
    }

    @Test
    void updateFolder_sousLuiMeme_refuse() {
        DocumentFolder a = folder(null, "A", 0);
        tree(List.of(a));
        assertThatThrownBy(() -> service.updateFolder(a.getId(), new DocumentDto.FolderRequest("A", a.getId(), null)))
                .isInstanceOf(DocumentStateException.class).hasMessageContaining("under itself");
    }

    @Test
    void updateFolder_sousSaPropreDescendance_refuse() {
        List<DocumentFolder> all = chain(3);
        tree(all);
        assertThatThrownBy(() -> service.updateFolder(all.get(0).getId(),
                new DocumentDto.FolderRequest("N0", all.get(2).getId(), null)))
                .isInstanceOf(DocumentStateException.class).hasMessageContaining("under itself");
    }

    @Test
    void updateFolder_profondeurDepassee_refuse() {
        List<DocumentFolder> deep = chain(3);
        DocumentFolder other = folder(null, "O", 1);
        DocumentFolder otherChild = folder(other.getId(), "OC", 0);
        List<DocumentFolder> all = new ArrayList<>(deep);
        all.add(other);
        all.add(otherChild);
        tree(all);
        // 3 (niveau du parent) + 2 (hauteur du dossier déplacé) > 4
        assertThatThrownBy(() -> service.updateFolder(other.getId(),
                new DocumentDto.FolderRequest("O", deep.get(2).getId(), null)))
                .isInstanceOf(DocumentStateException.class).hasMessageContaining("levels");
    }

    @Test
    void updateFolder_nomPrisChezLesNouveauxFreres_refuse() {
        DocumentFolder a = folder(null, "A", 0);
        DocumentFolder b = folder(null, "B", 1);
        tree(List.of(a, b));
        assertThatThrownBy(() -> service.updateFolder(b.getId(), new DocumentDto.FolderRequest("a", null, null)))
                .isInstanceOf(DocumentStateException.class);
    }

    @Test
    void deleteFolder_inconnu_404() {
        UUID id = UUID.randomUUID();
        when(folders.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteFolder(id)).isInstanceOf(DocumentCatalogNotFoundException.class);
    }

    @Test
    void deleteFolder_avecSousDossiers_refuse() {
        DocumentFolder f = folder(null, "A", 0);
        when(folders.findByIdAndTenantId(f.getId(), TENANT)).thenReturn(Optional.of(f));
        when(folders.existsByTenantIdAndParentId(TENANT, f.getId())).thenReturn(true);
        assertThatThrownBy(() -> service.deleteFolder(f.getId())).isInstanceOf(DocumentStateException.class)
                .hasMessageContaining("sub-folders");
        verify(folders, never()).delete(any());
    }

    @Test
    void deleteFolder_avecDocuments_refuse() {
        DocumentFolder f = folder(null, "A", 0);
        when(folders.findByIdAndTenantId(f.getId(), TENANT)).thenReturn(Optional.of(f));
        when(documents.existsByTenantIdAndFolderId(TENANT, f.getId())).thenReturn(true);
        assertThatThrownBy(() -> service.deleteFolder(f.getId())).isInstanceOf(DocumentStateException.class)
                .hasMessageContaining("documents");
        verify(folders, never()).delete(any());
    }

    @Test
    void deleteFolder_vide_supprime() {
        DocumentFolder f = folder(null, "A", 0);
        when(folders.findByIdAndTenantId(f.getId(), TENANT)).thenReturn(Optional.of(f));
        service.deleteFolder(f.getId());
        verify(folders).delete(f);
    }

    @Test
    void folderAndDescendants_inclutToutLeSousArbre() {
        DocumentFolder root = folder(null, "R", 0);
        DocumentFolder c1 = folder(root.getId(), "C1", 0);
        DocumentFolder c2 = folder(c1.getId(), "C2", 0);
        DocumentFolder autre = folder(null, "X", 1);
        tree(List.of(root, c1, c2, autre));
        assertThat(service.folderAndDescendants(TENANT, root.getId()))
                .containsExactlyInAnyOrder(root.getId(), c1.getId(), c2.getId());
    }

    @Test
    void folderAndDescendants_inconnu_404() {
        tree(List.of());
        assertThatThrownBy(() -> service.folderAndDescendants(TENANT, UUID.randomUUID()))
                .isInstanceOf(DocumentCatalogNotFoundException.class);
    }

    @Test
    void requireFolderOf() {
        DocumentFolder f = folder(null, "A", 0);
        when(folders.findByIdAndTenantId(f.getId(), TENANT)).thenReturn(Optional.of(f));
        assertThatCode(() -> service.requireFolderOf(TENANT, f.getId())).doesNotThrowAnyException();
        UUID ghost = UUID.randomUUID();
        when(folders.findByIdAndTenantId(ghost, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.requireFolderOf(TENANT, ghost))
                .isInstanceOf(DocumentCatalogNotFoundException.class);
    }

    // ---------- processus ----------

    @Test
    void processes_semeEtListe() {
        seeded();
        DocumentProcess p = process("Achats", 2);
        when(processes.findByTenantIdOrderByPositionAscNameAsc(TENANT)).thenReturn(List.of(p));
        assertThat(service.processes()).containsExactly(new DocumentDto.ProcessView(p.getId(), "Achats", 2));
        verify(seeder, never()).seed(any());
    }

    @Test
    void createProcess_nomPris_refuse() {
        seeded();
        when(processes.existsByTenantIdAndNameIgnoreCase(TENANT, "Achats")).thenReturn(true);
        assertThatThrownBy(() -> service.createProcess(new DocumentDto.ProcessRequest(" Achats ", null)))
                .isInstanceOf(DocumentStateException.class);
    }

    @Test
    void createProcess_positionSuivanteOuExplicite() {
        seeded();
        when(processes.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processes.findByTenantIdOrderByPositionAscNameAsc(TENANT)).thenReturn(List.of());
        assertThat(service.createProcess(new DocumentDto.ProcessRequest("A", null)).position()).isZero();

        when(processes.findByTenantIdOrderByPositionAscNameAsc(TENANT))
                .thenReturn(List.of(process("X", 1), process("Y", 6)));
        assertThat(service.createProcess(new DocumentDto.ProcessRequest("B", null)).position()).isEqualTo(7);
        assertThat(service.createProcess(new DocumentDto.ProcessRequest("C", 3)).position()).isEqualTo(3);
    }

    @Test
    void updateProcess_inconnu_404() {
        UUID id = UUID.randomUUID();
        when(processes.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateProcess(id, new DocumentDto.ProcessRequest("A", null)))
                .isInstanceOf(DocumentCatalogNotFoundException.class);
    }

    @Test
    void updateProcess_nomDUnAutre_refuse() {
        DocumentProcess p = process("Achats", 0);
        when(processes.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(processes.existsByTenantIdAndNameIgnoreCase(TENANT, "Support")).thenReturn(true);
        assertThatThrownBy(() -> service.updateProcess(p.getId(), new DocumentDto.ProcessRequest("Support", null)))
                .isInstanceOf(DocumentStateException.class);
    }

    @Test
    void updateProcess_changementDeCasse_sansVerifierLUnicite() {
        DocumentProcess p = process("Achats", 4);
        when(processes.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(processes.save(p)).thenReturn(p);
        DocumentDto.ProcessView v = service.updateProcess(p.getId(), new DocumentDto.ProcessRequest("ACHATS", null));
        assertThat(v.name()).isEqualTo("ACHATS");
        assertThat(v.position()).isEqualTo(4);
        verify(processes, never()).existsByTenantIdAndNameIgnoreCase(any(), any());
    }

    @Test
    void updateProcess_nouveauNomLibre_etPosition() {
        DocumentProcess p = process("Achats", 4);
        when(processes.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(processes.existsByTenantIdAndNameIgnoreCase(TENANT, "Approvisionnement")).thenReturn(false);
        when(processes.save(p)).thenReturn(p);
        DocumentDto.ProcessView v = service.updateProcess(p.getId(),
                new DocumentDto.ProcessRequest("Approvisionnement", 1));
        assertThat(v.name()).isEqualTo("Approvisionnement");
        assertThat(v.position()).isEqualTo(1);
    }

    @Test
    void deleteProcess_inconnu_utilise_libre() {
        UUID ghost = UUID.randomUUID();
        when(processes.findByIdAndTenantId(ghost, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteProcess(ghost)).isInstanceOf(DocumentCatalogNotFoundException.class);

        DocumentProcess used = process("Achats", 0);
        when(processes.findByIdAndTenantId(used.getId(), TENANT)).thenReturn(Optional.of(used));
        when(documents.existsByTenantIdAndProcessId(TENANT, used.getId())).thenReturn(true);
        assertThatThrownBy(() -> service.deleteProcess(used.getId())).isInstanceOf(DocumentStateException.class);
        verify(processes, never()).delete(any());

        DocumentProcess free = process("Support", 1);
        when(processes.findByIdAndTenantId(free.getId(), TENANT)).thenReturn(Optional.of(free));
        service.deleteProcess(free.getId());
        verify(processes).delete(free);
    }

    @Test
    void requireProcessOf() {
        DocumentProcess p = process("A", 0);
        when(processes.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatCode(() -> service.requireProcessOf(TENANT, p.getId())).doesNotThrowAnyException();
        UUID ghost = UUID.randomUUID();
        when(processes.findByIdAndTenantId(ghost, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.requireProcessOf(TENANT, ghost))
                .isInstanceOf(DocumentCatalogNotFoundException.class);
    }

    // ---------- outils statiques ----------

    @Test
    void depthOf_racineUnEtChaine() {
        List<DocumentFolder> all = chain(3);
        assertThat(DocumentCatalogService.depthOf(all, all.get(0).getId())).isEqualTo(1);
        assertThat(DocumentCatalogService.depthOf(all, all.get(2).getId())).isEqualTo(3);
    }

    @Test
    void depthOf_cycleEnBase_termine() {
        DocumentFolder a = folder(null, "A", 0);
        DocumentFolder b = folder(a.getId(), "B", 0);
        a.setParentId(b.getId());
        assertThat(DocumentCatalogService.depthOf(List.of(a, b), a.getId())).isEqualTo(2);
    }

    @Test
    void heightOf_feuilleEtBrancheLaPlusLongue() {
        List<DocumentFolder> all = new ArrayList<>(chain(3));
        all.add(folder(all.get(0).getId(), "court", 1));
        assertThat(DocumentCatalogService.heightOf(all, all.get(2).getId())).isEqualTo(1);
        assertThat(DocumentCatalogService.heightOf(all, all.get(0).getId())).isEqualTo(3);
    }

    @Test
    void descendantsOf_exclutLeDossierLuiMeme() {
        List<DocumentFolder> all = chain(3);
        assertThat(DocumentCatalogService.descendantsOf(all, all.get(0).getId()))
                .containsExactlyInAnyOrder(all.get(1).getId(), all.get(2).getId());
        assertThat(DocumentCatalogService.descendantsOf(all, all.get(2).getId())).isEmpty();
    }
}
