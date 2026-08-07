package com.openlab.qualitos.quality.capa;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.nonconformity.storage.ObjectStorage;
import com.openlab.qualitos.quality.nonconformity.storage.StorageDisabledException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Preuves jointes à un dossier CAPA (§4.2, ISO 9001 §10.2).
 *
 * <p>Ce qui se teste ici n'est pas « le fichier arrive-t-il » — c'est que les
 * bornes tiennent et que le dossier reste crédible : un type falsifié est
 * refusé, une borne atteinte est annoncée plutôt que contournée, un dossier clos
 * ne se regarnit pas, et rien n'est écrit dans le stockage qui ne soit d'abord
 * enregistré en base.
 */
@ExtendWith(MockitoExtension.class)
class CapaEvidenceServiceTest {

    @Mock CapaEvidenceRepository evidenceRepo;
    @Mock CapaCaseRepository caseRepo;
    @Mock ObjectProvider<ObjectStorage> storageProvider;
    @Mock ObjectStorage storage;

    private CapaEvidenceService service;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID CAPA = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();

    /** Un PDF minimal : la signature suffit, le reste n'est jamais lu. */
    private static final byte[] PDF = "%PDF-1.7 contenu".getBytes();
    private static final byte[] ZIP = new byte[] { 0x50, 0x4B, 0x03, 0x04, 1, 2, 3 };
    private static final String DOCX =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT.toString());
        lenient().when(storageProvider.getIfAvailable()).thenReturn(storage);
        service = new CapaEvidenceService(evidenceRepo, caseRepo, storageProvider);
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private CapaCase capa(CapaStatus status) {
        CapaCase c = new CapaCase();
        c.setId(CAPA);
        c.setTenantId(TENANT);
        c.setStatus(status);
        return c;
    }

    private void openCase() {
        when(caseRepo.findByIdAndTenantId(CAPA, TENANT)).thenReturn(Optional.of(capa(CapaStatus.OPEN)));
    }

    private void savedEvidenceEchoesInput() {
        when(evidenceRepo.save(any(CapaEvidence.class))).thenAnswer(inv -> {
            CapaEvidence e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
    }

    // --- dépôt nominal ----------------------------------------------------------

    @Test
    void depose_unePreuve_etEcritLeBinaireApresLaLigne() {
        openCase();
        savedEvidenceEchoesInput();

        CapaEvidenceDto.Response res = service.upload(
                CAPA, "application/pdf", "relevé 3 mois.pdf", PDF, ACTOR);

        assertThat(res.contentType()).isEqualTo("application/pdf");
        assertThat(res.sizeBytes()).isEqualTo(PDF.length);
        assertThat(res.uploadedBy()).isEqualTo(ACTOR);

        // L'ordre compte : la ligne d'abord, l'objet ensuite. Si le put échouait,
        // la transaction annulerait la ligne et rien ne resterait.
        var order = org.mockito.Mockito.inOrder(evidenceRepo, storage);
        order.verify(evidenceRepo).save(any(CapaEvidence.class));
        order.verify(storage).put(anyString(), eq("application/pdf"), eq(PDF));
    }

    @Test
    void range_lePiece_sousUneCleTenantiseeAvecLExtensionDuTypeValide() {
        openCase();
        savedEvidenceEchoesInput();

        service.upload(CAPA, "application/pdf", "../../etc/passwd", PDF, ACTOR);

        ArgumentCaptor<CapaEvidence> saved = ArgumentCaptor.forClass(CapaEvidence.class);
        verify(evidenceRepo).save(saved.capture());
        String key = saved.getValue().getObjectKey();

        assertThat(key).startsWith("tenants/" + TENANT + "/capa/" + CAPA + "/");
        assertThat(key).endsWith(".pdf");
        // Le nom client ne doit JAMAIS entrer dans la clé : il peut porter une
        // traversée de chemin. Il est conservé, neutralisé, à titre informatif.
        assertThat(key).doesNotContain("passwd").doesNotContain("..");
        // Le nom retenu est le dernier segment, débarrassé du chemin : ce qui reste
        // est informatif et inoffensif.
        assertThat(saved.getValue().getOriginalFilename()).isEqualTo("passwd");
    }

    @Test
    void accepte_lesFormatsBureautiques_admisParChoixDExploitation() {
        openCase();
        savedEvidenceEchoesInput();

        assertThat(service.upload(CAPA, DOCX, "consigne.docx", ZIP, ACTOR).contentType()).isEqualTo(DOCX);
        assertThat(service.upload(CAPA, XLSX, "releve.xlsx", ZIP, ACTOR).contentType()).isEqualTo(XLSX);
    }

    // --- validation d'entrée -----------------------------------------------------

    @Test
    void refuse_unFichierVide() {
        openCase();

        assertThatThrownBy(() -> service.upload(CAPA, "application/pdf", "vide.pdf", new byte[0], ACTOR))
                .isInstanceOf(CapaEvidenceValidationException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void refuse_unTypeHorsListeBlanche() {
        openCase();

        assertThatThrownBy(() -> service.upload(
                CAPA, "application/x-msdownload", "outil.exe", PDF, ACTOR))
                .isInstanceOf(CapaEvidenceValidationException.class)
                .hasMessageContaining("Unsupported content type");
        verify(evidenceRepo, never()).save(any());
    }

    @Test
    void refuse_unTypeAnnonceQueLaSignatureDement() {
        // Le content-type est déclaré par le client, donc falsifiable : un
        // exécutable annoncé en PDF passerait sans cette vérification.
        openCase();
        byte[] executable = new byte[] { 0x4D, 0x5A, (byte) 0x90, 0x00 }; // "MZ"

        assertThatThrownBy(() -> service.upload(CAPA, "application/pdf", "piege.pdf", executable, ACTOR))
                .isInstanceOf(CapaEvidenceValidationException.class)
                .hasMessageContaining("does not match the declared type");
        verify(evidenceRepo, never()).save(any());
    }

    @Test
    void refuse_unePieceTropLourde() {
        openCase();
        byte[] enorme = new byte[(int) CapaEvidenceService.MAX_SIZE_BYTES + 1];
        enorme[0] = 0x25; enorme[1] = 0x50; enorme[2] = 0x44; enorme[3] = 0x46; enorme[4] = 0x2D;

        assertThatThrownBy(() -> service.upload(CAPA, "application/pdf", "gros.pdf", enorme, ACTOR))
                .isInstanceOf(CapaEvidenceTooLargeException.class);
        verify(evidenceRepo, never()).save(any());
    }

    // --- bornes du dossier --------------------------------------------------------

    @Test
    void refuse_laOnziemePiece_enDisantPourquoi() {
        openCase();
        when(evidenceRepo.countByTenantIdAndCapaId(TENANT, CAPA))
                .thenReturn((long) CapaEvidenceService.MAX_COUNT);

        assertThatThrownBy(() -> service.upload(CAPA, "application/pdf", "onze.pdf", PDF, ACTOR))
                .isInstanceOf(CapaStateException.class)
                .hasMessageContaining("remove one before adding another");
        // Refus explicite : on ne supprime pas d'office la pièce la plus ancienne,
        // ce qui reviendrait à vider un dossier d'audit en silence.
        verify(evidenceRepo, never()).delete(any());
        verify(evidenceRepo, never()).save(any());
    }

    @Test
    void refuse_lePoidsCumuleDepasse() {
        openCase();
        when(evidenceRepo.countByTenantIdAndCapaId(TENANT, CAPA)).thenReturn(2L);
        when(evidenceRepo.sumSizeBytes(TENANT, CAPA))
                .thenReturn(CapaEvidenceService.MAX_TOTAL_BYTES - 1);

        assertThatThrownBy(() -> service.upload(CAPA, "application/pdf", "goutte.pdf", PDF, ACTOR))
                .isInstanceOf(CapaStateException.class)
                .hasMessageContaining("MB of evidence");
        verify(evidenceRepo, never()).save(any());
    }

    @Test
    void accepte_laPieceQuiTientPileDansLePoidsRestant() {
        openCase();
        savedEvidenceEchoesInput();
        when(evidenceRepo.countByTenantIdAndCapaId(TENANT, CAPA)).thenReturn(1L);
        when(evidenceRepo.sumSizeBytes(TENANT, CAPA))
                .thenReturn(CapaEvidenceService.MAX_TOTAL_BYTES - PDF.length);

        // La borne est un plafond, pas un seuil : ce qui tient exactement passe.
        assertThat(service.upload(CAPA, "application/pdf", "juste.pdf", PDF, ACTOR)).isNotNull();
    }

    // --- verrou d'état -------------------------------------------------------------

    @Test
    void refuse_deVerserSurUnDossierClos() {
        when(caseRepo.findByIdAndTenantId(CAPA, TENANT))
                .thenReturn(Optional.of(capa(CapaStatus.CLOSED)));

        assertThatThrownBy(() -> service.upload(CAPA, "application/pdf", "tardif.pdf", PDF, ACTOR))
                .isInstanceOf(CapaStateException.class)
                .hasMessageContaining("CLOSED");
    }

    @Test
    void refuse_deRetirerUnePieceDUnDossierRejete() {
        when(caseRepo.findByIdAndTenantId(CAPA, TENANT))
                .thenReturn(Optional.of(capa(CapaStatus.REJECTED)));

        assertThatThrownBy(() -> service.delete(CAPA, UUID.randomUUID()))
                .isInstanceOf(CapaStateException.class);
        verify(evidenceRepo, never()).delete(any());
    }

    @Test
    void laisse_lireLesPreuvesDUnDossierClos() {
        // La lecture reste ouverte : c'est la preuve qui explique la clôture.
        when(caseRepo.findByIdAndTenantId(CAPA, TENANT))
                .thenReturn(Optional.of(capa(CapaStatus.CLOSED)));
        when(evidenceRepo.findByTenantIdAndCapaIdOrderByCreatedAtAsc(TENANT, CAPA))
                .thenReturn(List.of(evidence()));

        assertThat(service.list(CAPA)).hasSize(1);
    }

    // --- lecture --------------------------------------------------------------------

    @Test
    void liste_avecUneUrlDeLectureAtemporaire() throws Exception {
        when(caseRepo.findByIdAndTenantId(CAPA, TENANT)).thenReturn(Optional.of(capa(CapaStatus.OPEN)));
        CapaEvidence e = evidence();
        when(evidenceRepo.findByTenantIdAndCapaIdOrderByCreatedAtAsc(TENANT, CAPA))
                .thenReturn(List.of(e));
        when(storage.presignGet(e.getObjectKey(), CapaEvidenceService.PRESIGN_TTL))
                .thenReturn(java.net.URI.create("https://stockage.example/preuve?signature=abc").toURL());

        List<CapaEvidenceDto.ListItem> items = service.list(CAPA);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).url()).contains("signature=abc");
        assertThat(items.get(0).originalFilename()).isEqualTo("releve.pdf");
    }

    @Test
    void liste_supporteUnStockageQuiNeSignePas() {
        when(caseRepo.findByIdAndTenantId(CAPA, TENANT)).thenReturn(Optional.of(capa(CapaStatus.OPEN)));
        when(evidenceRepo.findByTenantIdAndCapaIdOrderByCreatedAtAsc(TENANT, CAPA))
                .thenReturn(List.of(evidence()));
        when(storage.presignGet(anyString(), any())).thenReturn(null);

        assertThat(service.list(CAPA).get(0).url()).isNull();
    }

    // --- suppression -------------------------------------------------------------------

    @Test
    void retire_laLigneAvantLObjet() {
        openCase();
        CapaEvidence e = evidence();
        when(evidenceRepo.findByIdAndTenantIdAndCapaId(e.getId(), TENANT, CAPA))
                .thenReturn(Optional.of(e));

        service.delete(CAPA, e.getId());

        var order = org.mockito.Mockito.inOrder(evidenceRepo, storage);
        order.verify(evidenceRepo).delete(e);
        order.verify(storage).delete(e.getObjectKey());
    }

    @Test
    void refuse_deRetirerUnePieceInconnue() {
        openCase();
        UUID inconnu = UUID.randomUUID();
        when(evidenceRepo.findByIdAndTenantIdAndCapaId(inconnu, TENANT, CAPA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(CAPA, inconnu))
                .isInstanceOf(CapaEvidenceNotFoundException.class);
        verifyNoInteractions(storage);
    }

    // --- isolation et disponibilité -------------------------------------------------------

    @Test
    void refuse_unDossierDUnAutreTenant() {
        when(caseRepo.findByIdAndTenantId(CAPA, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upload(CAPA, "application/pdf", "x.pdf", PDF, ACTOR))
                .isInstanceOf(CapaNotFoundException.class);
    }

    @Test
    void refuse_toutTraitementSansTenantDansLeJeton() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.list(CAPA)).isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void annonce_leStockageCoupe_plutotQueDeMentirSurUnDossierVide() {
        when(storageProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> service.list(CAPA)).isInstanceOf(StorageDisabledException.class);
        assertThatThrownBy(() -> service.upload(CAPA, "application/pdf", "x.pdf", PDF, ACTOR))
                .isInstanceOf(StorageDisabledException.class);
    }

    // --- signatures binaires ------------------------------------------------------------

    @Test
    void reconnait_lesSignaturesDesTypesAdmis() {
        assertThat(CapaEvidenceService.magicBytesMatch("application/pdf", PDF)).isTrue();
        assertThat(CapaEvidenceService.magicBytesMatch(DOCX, ZIP)).isTrue();
        assertThat(CapaEvidenceService.magicBytesMatch(XLSX, ZIP)).isTrue();
        assertThat(CapaEvidenceService.magicBytesMatch("image/jpeg",
                new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0 })).isTrue();
        assertThat(CapaEvidenceService.magicBytesMatch("image/png", new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A })).isTrue();
        assertThat(CapaEvidenceService.magicBytesMatch("image/webp",
                "RIFF____WEBPxxxx".getBytes())).isTrue();
        assertThat(CapaEvidenceService.magicBytesMatch("image/heic",
                new byte[] { 0, 0, 0, 0x18, 'f', 't', 'y', 'p' })).isTrue();
    }

    @Test
    void rejette_lesSignaturesTropCourtesOuInconnues() {
        assertThat(CapaEvidenceService.magicBytesMatch("application/pdf", new byte[] { 0x25 })).isFalse();
        assertThat(CapaEvidenceService.magicBytesMatch("image/webp", "RIFF".getBytes())).isFalse();
        assertThat(CapaEvidenceService.magicBytesMatch("image/heic", new byte[] { 1, 2 })).isFalse();
        assertThat(CapaEvidenceService.magicBytesMatch("text/plain", PDF)).isFalse();
    }

    private CapaEvidence evidence() {
        CapaEvidence e = new CapaEvidence();
        e.setId(UUID.randomUUID());
        e.setTenantId(TENANT);
        e.setCapaId(CAPA);
        e.setObjectKey("tenants/" + TENANT + "/capa/" + CAPA + "/" + UUID.randomUUID() + ".pdf");
        e.setContentType("application/pdf");
        e.setSizeBytes(PDF.length);
        e.setOriginalFilename("releve.pdf");
        e.setUploadedBy(ACTOR);
        return e;
    }
}
