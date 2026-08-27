package com.openlab.qualitos.quality.pdca;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
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

import java.net.URL;
import java.net.URI;
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
 * Preuves d'étape d'un cycle PDCA (§3.1, ADR 0061).
 *
 * <p>Ce qui se teste ici n'est pas « le fichier arrive-t-il » — c'est que les
 * bornes tiennent et que le cycle reste crédible : un type falsifié est refusé,
 * une étape qui porte déjà sa pièce ne se laisse pas regarnir en silence, un
 * cycle clos ne bouge plus, une étape d'un autre cycle est introuvable, et rien
 * n'est écrit dans le stockage qui ne soit d'abord enregistré en base.
 */
@ExtendWith(MockitoExtension.class)
class PdcaStepEvidenceServiceTest {

    @Mock PdcaStepEvidenceRepository evidenceRepo;
    @Mock PdcaCycleRepository cycleRepo;
    @Mock PdcaStepRepository stepRepo;
    @Mock ObjectProvider<ObjectStorage> storageProvider;
    @Mock ObjectStorage storage;
    @Mock AuditEventService auditEvents;

    private PdcaStepEvidenceService service;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID CYCLE = UUID.randomUUID();
    private static final UUID STEP = UUID.randomUUID();
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
        service = new PdcaStepEvidenceService(evidenceRepo, cycleRepo, stepRepo, storageProvider, auditEvents);
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    // --- fixtures ---------------------------------------------------------------

    private PdcaCycle cycle(PdcaStatus status) {
        PdcaCycle c = new PdcaCycle();
        c.setId(CYCLE);
        c.setTenantId(TENANT);
        c.setStatus(status);
        return c;
    }

    private PdcaStep step() {
        PdcaStep s = new PdcaStep();
        s.setId(STEP);
        s.setPhase(PdcaPhase.DO);
        s.setTitle("Déployer le détrompeur");
        s.setStatus(StepStatus.DONE);
        return s;
    }

    private void liveCycle() {
        when(cycleRepo.findByIdAndTenantId(CYCLE, TENANT)).thenReturn(Optional.of(cycle(PdcaStatus.DO)));
    }

    private void stepBelongsToCycle() {
        when(stepRepo.findByIdAndCycleId(STEP, CYCLE)).thenReturn(Optional.of(step()));
    }

    private void savedEvidenceEchoesInput() {
        when(evidenceRepo.save(any(PdcaStepEvidence.class))).thenAnswer(inv -> {
            PdcaStepEvidence e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
    }

    private PdcaStepEvidence stored(String key) {
        PdcaStepEvidence e = new PdcaStepEvidence();
        e.setId(UUID.randomUUID());
        e.setTenantId(TENANT);
        e.setCycleId(CYCLE);
        e.setStepId(STEP);
        e.setObjectKey(key);
        e.setContentType("application/pdf");
        e.setSizeBytes(42L);
        e.setOriginalFilename("releve.pdf");
        e.setUploadedBy(ACTOR);
        return e;
    }

    // --- dépôt nominal ----------------------------------------------------------

    @Test
    void depose_unePreuve_etEcritLeBinaireApresLaLigne() {
        liveCycle();
        stepBelongsToCycle();
        savedEvidenceEchoesInput();

        PdcaStepEvidenceDto.Response res = service.upload(
                CYCLE, STEP, "application/pdf", "relevé 3 mois.pdf", PDF, ACTOR);

        assertThat(res.cycleId()).isEqualTo(CYCLE);
        assertThat(res.stepId()).isEqualTo(STEP);
        assertThat(res.contentType()).isEqualTo("application/pdf");
        assertThat(res.sizeBytes()).isEqualTo(PDF.length);
        assertThat(res.uploadedBy()).isEqualTo(ACTOR);

        // L'ordre compte : la ligne d'abord, l'objet ensuite. Si le put échouait,
        // la transaction annulerait la ligne et rien ne resterait.
        var order = org.mockito.Mockito.inOrder(evidenceRepo, storage);
        order.verify(evidenceRepo).save(any(PdcaStepEvidence.class));
        order.verify(storage).put(anyString(), eq("application/pdf"), eq(PDF));
    }

    @Test
    void range_laPiece_sousUneCleTenantiseeAvecLExtensionDuTypeValide() {
        liveCycle();
        stepBelongsToCycle();
        savedEvidenceEchoesInput();

        service.upload(CYCLE, STEP, "application/pdf", "../../etc/passwd", PDF, ACTOR);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(storage).put(key.capture(), anyString(), any());
        assertThat(key.getValue())
                .startsWith("tenants/" + TENANT + "/pdca/" + CYCLE + "/steps/" + STEP + "/")
                .endsWith(".pdf")
                // Le nom fourni par le client n'entre jamais dans la clé : aucune
                // remontée de chemin n'est possible, même avec un nom hostile.
                .doesNotContain("..")
                .doesNotContain("passwd");
    }

    @Test
    void neutralise_leNomDOrigine_sansJamaisLeReinjecterDansLaCle() {
        liveCycle();
        stepBelongsToCycle();
        savedEvidenceEchoesInput();

        PdcaStepEvidenceDto.Response res = service.upload(
                CYCLE, STEP, "application/pdf", "C:\\dossier\\relevé final.pdf", PDF, ACTOR);

        assertThat(res.originalFilename()).isEqualTo("relev__final.pdf");
    }

    @Test
    void tronque_unNomDOrigineDemesure_a255Caracteres() {
        liveCycle();
        stepBelongsToCycle();
        savedEvidenceEchoesInput();

        String enorme = "a".repeat(400) + ".pdf";
        PdcaStepEvidenceDto.Response res = service.upload(
                CYCLE, STEP, "application/pdf", enorme, PDF, ACTOR);

        assertThat(res.originalFilename()).hasSize(255);
    }

    @Test
    void accepte_unNomAbsent_sansInventerDeNom() {
        liveCycle();
        stepBelongsToCycle();
        savedEvidenceEchoesInput();

        assertThat(service.upload(CYCLE, STEP, "application/pdf", null, PDF, ACTOR)
                .originalFilename()).isNull();
        assertThat(service.upload(CYCLE, STEP, "application/pdf", "   ", PDF, ACTOR)
                .originalFilename()).isNull();
    }

    @Test
    void normalise_leTypeDeclare_casseEtEspaces() {
        liveCycle();
        stepBelongsToCycle();
        savedEvidenceEchoesInput();

        PdcaStepEvidenceDto.Response res = service.upload(
                CYCLE, STEP, "  APPLICATION/PDF  ", "x.pdf", PDF, ACTOR);

        assertThat(res.contentType()).isEqualTo("application/pdf");
    }

    // --- refus à l'entrée --------------------------------------------------------

    @Test
    void refuse_unFichierVide() {
        liveCycle();
        stepBelongsToCycle();

        assertThatThrownBy(() -> service.upload(CYCLE, STEP, "application/pdf", "x.pdf", new byte[0], ACTOR))
                .isInstanceOf(PdcaStepEvidenceValidationException.class);
        assertThatThrownBy(() -> service.upload(CYCLE, STEP, "application/pdf", "x.pdf", null, ACTOR))
                .isInstanceOf(PdcaStepEvidenceValidationException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void refuse_unePieceAuDelaDuPlafondUnitaire() {
        liveCycle();
        stepBelongsToCycle();

        byte[] trop = new byte[(int) PdcaStepEvidenceService.MAX_SIZE_BYTES + 1];
        trop[0] = 0x25; trop[1] = 0x50; trop[2] = 0x44; trop[3] = 0x46; trop[4] = 0x2D;

        assertThatThrownBy(() -> service.upload(CYCLE, STEP, "application/pdf", "gros.pdf", trop, ACTOR))
                .isInstanceOf(PdcaStepEvidenceTooLargeException.class);
        verify(evidenceRepo, never()).save(any());
    }

    @Test
    void refuse_unTypeHorsListeBlanche() {
        liveCycle();
        stepBelongsToCycle();

        assertThatThrownBy(() -> service.upload(CYCLE, STEP, "application/x-msdownload", "x.exe", PDF, ACTOR))
                .isInstanceOf(PdcaStepEvidenceValidationException.class)
                .hasMessageContaining("Unsupported content type");
    }

    @Test
    void refuse_unTypeAbsent() {
        liveCycle();
        stepBelongsToCycle();

        assertThatThrownBy(() -> service.upload(CYCLE, STEP, null, "x.pdf", PDF, ACTOR))
                .isInstanceOf(PdcaStepEvidenceValidationException.class);
    }

    @Test
    void refuse_unContenuQuiNeCorrespondPasAuTypeAnnonce() {
        liveCycle();
        stepBelongsToCycle();

        // Un exécutable renommé en PDF : le type déclaré passe la liste blanche,
        // la signature binaire non.
        byte[] faux = new byte[] { 'M', 'Z', 0x00, 0x01, 0x02 };

        assertThatThrownBy(() -> service.upload(CYCLE, STEP, "application/pdf", "faux.pdf", faux, ACTOR))
                .isInstanceOf(PdcaStepEvidenceValidationException.class)
                .hasMessageContaining("does not match the declared type");
        verify(evidenceRepo, never()).save(any());
    }

    // --- bornes ------------------------------------------------------------------

    @Test
    void refuse_uneSecondePieceSurLaMemeEtape() {
        liveCycle();
        stepBelongsToCycle();
        when(evidenceRepo.countByTenantIdAndStepId(TENANT, STEP))
                .thenReturn((long) PdcaStepEvidenceService.MAX_PER_STEP);

        assertThatThrownBy(() -> service.upload(CYCLE, STEP, "application/pdf", "x.pdf", PDF, ACTOR))
                .isInstanceOf(PdcaStateException.class)
                .hasMessageContaining("already carries its evidence file");
        verify(evidenceRepo, never()).save(any());
    }

    @Test
    void refuse_unDepotQuiFeraitDeborderLePoidsCumuleDuCycle() {
        liveCycle();
        stepBelongsToCycle();
        when(evidenceRepo.sumSizeBytes(TENANT, CYCLE))
                .thenReturn(PdcaStepEvidenceService.MAX_TOTAL_BYTES);

        assertThatThrownBy(() -> service.upload(CYCLE, STEP, "application/pdf", "x.pdf", PDF, ACTOR))
                .isInstanceOf(PdcaStateException.class)
                .hasMessageContaining("25 MB");
        verify(evidenceRepo, never()).save(any());
    }

    // --- verrou d'état -----------------------------------------------------------

    @Test
    void refuse_toutDepotSurUnCycleClosOuAnnule() {
        when(cycleRepo.findByIdAndTenantId(CYCLE, TENANT))
                .thenReturn(Optional.of(cycle(PdcaStatus.COMPLETED)));

        assertThatThrownBy(() -> service.upload(CYCLE, STEP, "application/pdf", "x.pdf", PDF, ACTOR))
                .isInstanceOf(PdcaStateException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    void refuse_toutRetraitSurUnCycleAnnule() {
        when(cycleRepo.findByIdAndTenantId(CYCLE, TENANT))
                .thenReturn(Optional.of(cycle(PdcaStatus.CANCELLED)));

        assertThatThrownBy(() -> service.delete(CYCLE, STEP, UUID.randomUUID(), ACTOR))
                .isInstanceOf(PdcaStateException.class)
                .hasMessageContaining("CANCELLED");
    }

    // --- isolation ---------------------------------------------------------------

    @Test
    void refuse_unCycleDUnAutreTenant_enDisantQuIlNExistePas() {
        // Le dépôt filtre déjà par tenant : un cycle d'un tenant voisin ne
        // remonte pas, et l'appelant reçoit 404 — jamais 403, qui confirmerait
        // l'existence de la ressource (OWASP A01).
        when(cycleRepo.findByIdAndTenantId(CYCLE, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upload(CYCLE, STEP, "application/pdf", "x.pdf", PDF, ACTOR))
                .isInstanceOf(PdcaCycleNotFoundException.class);
        assertThatThrownBy(() -> service.listForCycle(CYCLE))
                .isInstanceOf(PdcaCycleNotFoundException.class);
        assertThatThrownBy(() -> service.delete(CYCLE, STEP, UUID.randomUUID(), ACTOR))
                .isInstanceOf(PdcaCycleNotFoundException.class);
        verifyNoInteractions(evidenceRepo);
    }

    @Test
    void refuse_uneEtapeQuiNAppartientPasAuCycleVise() {
        liveCycle();
        when(stepRepo.findByIdAndCycleId(STEP, CYCLE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upload(CYCLE, STEP, "application/pdf", "x.pdf", PDF, ACTOR))
                .isInstanceOf(PdcaStepNotFoundException.class);
        verify(evidenceRepo, never()).save(any());
    }

    @Test
    void exige_unContexteTenant() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.listForCycle(CYCLE))
                .isInstanceOf(MissingTenantContextException.class);
        assertThatThrownBy(() -> service.upload(CYCLE, STEP, "application/pdf", "x.pdf", PDF, ACTOR))
                .isInstanceOf(MissingTenantContextException.class);
        assertThatThrownBy(() -> service.delete(CYCLE, STEP, UUID.randomUUID(), ACTOR))
                .isInstanceOf(MissingTenantContextException.class);
        verifyNoInteractions(cycleRepo, evidenceRepo, storage);
    }

    @Test
    void annonce_leStockageCoupe_plutotQueDeLaisserCroireAUneEtapeSansPiece() {
        when(storageProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> service.listForCycle(CYCLE))
                .isInstanceOf(StorageDisabledException.class);
        assertThatThrownBy(() -> service.upload(CYCLE, STEP, "application/pdf", "x.pdf", PDF, ACTOR))
                .isInstanceOf(StorageDisabledException.class);
        assertThatThrownBy(() -> service.delete(CYCLE, STEP, UUID.randomUUID(), ACTOR))
                .isInstanceOf(StorageDisabledException.class);
        verifyNoInteractions(cycleRepo, evidenceRepo);
    }

    // --- lecture -------------------------------------------------------------------

    @Test
    void liste_lesPiecesDuCycle_avecUneUrlDeLectureAcourteDuree() throws Exception {
        when(cycleRepo.findByIdAndTenantId(CYCLE, TENANT)).thenReturn(Optional.of(cycle(PdcaStatus.CHECK)));
        PdcaStepEvidence e = stored("tenants/x/pdca/y/steps/z/a.pdf");
        when(evidenceRepo.findForCycle(TENANT, CYCLE)).thenReturn(List.of(e));
        when(storage.presignGet(e.getObjectKey(), PdcaStepEvidenceService.PRESIGN_TTL))
                .thenReturn(URI.create("https://minio.local/a.pdf?sig=x").toURL());

        List<PdcaStepEvidenceDto.ListItem> items = service.listForCycle(CYCLE);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).stepId()).isEqualTo(STEP);
        assertThat(items.get(0).url()).isEqualTo("https://minio.local/a.pdf?sig=x");
        assertThat(items.get(0).originalFilename()).isEqualTo("releve.pdf");
    }

    @Test
    void liste_uneUrlAbsente_sansFabriquerDeLienMort() {
        when(cycleRepo.findByIdAndTenantId(CYCLE, TENANT)).thenReturn(Optional.of(cycle(PdcaStatus.ACT)));
        PdcaStepEvidence e = stored("tenants/x/pdca/y/steps/z/b.pdf");
        when(evidenceRepo.findForCycle(TENANT, CYCLE)).thenReturn(List.of(e));
        when(storage.presignGet(anyString(), any())).thenReturn((URL) null);

        assertThat(service.listForCycle(CYCLE).get(0).url()).isNull();
    }

    @Test
    void liste_reste_lisibleSurUnCycleClos() {
        // La lecture n'est pas verrouillée par l'état : un cycle clos garde ses
        // preuves consultables, c'est même à ce moment-là qu'on les regarde.
        when(cycleRepo.findByIdAndTenantId(CYCLE, TENANT))
                .thenReturn(Optional.of(cycle(PdcaStatus.COMPLETED)));
        when(evidenceRepo.findForCycle(TENANT, CYCLE)).thenReturn(List.of());

        assertThat(service.listForCycle(CYCLE)).isEmpty();
    }

    // --- retrait -------------------------------------------------------------------

    @Test
    void retire_laLigneAvantLObjet() {
        liveCycle();
        stepBelongsToCycle();
        PdcaStepEvidence e = stored("tenants/x/pdca/y/steps/z/c.pdf");
        when(evidenceRepo.findByIdAndTenantIdAndStepId(e.getId(), TENANT, STEP))
                .thenReturn(Optional.of(e));

        service.delete(CYCLE, STEP, e.getId(), ACTOR);

        var order = org.mockito.Mockito.inOrder(evidenceRepo, storage);
        order.verify(evidenceRepo).delete(e);
        order.verify(storage).delete(e.getObjectKey());
    }

    @Test
    void refuse_leRetraitDUnePieceQuiNEstPasCelleDeLEtape() {
        liveCycle();
        stepBelongsToCycle();
        UUID inconnue = UUID.randomUUID();
        when(evidenceRepo.findByIdAndTenantIdAndStepId(inconnue, TENANT, STEP))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(CYCLE, STEP, inconnue, ACTOR))
                .isInstanceOf(PdcaStepEvidenceNotFoundException.class);
        verify(storage, never()).delete(anyString());
    }

    @Test
    void refuse_leRetraitSurUneEtapeEtrangereAuCycle() {
        liveCycle();
        when(stepRepo.findByIdAndCycleId(STEP, CYCLE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(CYCLE, STEP, UUID.randomUUID(), ACTOR))
                .isInstanceOf(PdcaStepNotFoundException.class);
        verifyNoInteractions(evidenceRepo);
    }

    // --- journal d'audit -------------------------------------------------------------

    @Test
    void consigne_leDepot_avecLEtapeVisee_etSansLaCleDObjet() {
        liveCycle();
        stepBelongsToCycle();
        savedEvidenceEchoesInput();

        service.upload(CYCLE, STEP, "application/pdf", "relevé.pdf", PDF, ACTOR);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> req =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(TENANT), req.capture());
        assertThat(req.getValue().action()).isEqualTo("pdca.step-evidence.uploaded");
        assertThat(req.getValue().resourceType()).isEqualTo("pdca_step_evidence");
        assertThat(req.getValue().actorType()).isEqualTo(ActorType.USER);
        assertThat(req.getValue().actorUserId()).isEqualTo(ACTOR);
        assertThat(req.getValue().summary()).contains(STEP.toString()).contains(CYCLE.toString());
        assertThat(req.getValue().payloadJson())
                .contains("\"stepId\":\"" + STEP + "\"")
                .contains("relev_.pdf")
                // La clé d'objet donnerait un chemin de stockage dans un journal
                // qui se relit et s'exporte.
                .doesNotContain("tenants/");
    }

    @Test
    void consigne_unDepotSansAuteurExploitable_commeSysteme() {
        liveCycle();
        stepBelongsToCycle();
        savedEvidenceEchoesInput();

        service.upload(CYCLE, STEP, "application/pdf", null, PDF, null);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> req =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(TENANT), req.capture());
        assertThat(req.getValue().actorType()).isEqualTo(ActorType.SYSTEM);
        assertThat(req.getValue().actorUserId()).isNull();
        assertThat(req.getValue().payloadJson()).contains("\"originalFilename\":null");
    }

    @Test
    void echappe_unNomHostile_pourNePasCasserLeJsonDuJournal() {
        liveCycle();
        stepBelongsToCycle();
        when(evidenceRepo.save(any(PdcaStepEvidence.class))).thenAnswer(inv -> {
            PdcaStepEvidence e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            // On force un nom que l'assainissement n'aurait jamais laissé passer :
            // le journal ne doit pas dépendre d'une hypothèse tenue ailleurs.
            e.setOriginalFilename("a\"b\\c.pdf");
            return e;
        });

        service.upload(CYCLE, STEP, "application/pdf", "a.pdf", PDF, ACTOR);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> req =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(TENANT), req.capture());
        assertThat(req.getValue().payloadJson()).contains("a\\\"b\\\\c.pdf");
    }

    @Test
    void consigne_leRetrait_carCEstLeSeulGesteQuiFaitDisparaitreUnePreuve() {
        liveCycle();
        stepBelongsToCycle();
        PdcaStepEvidence e = stored("tenants/x/pdca/y/steps/z/d.pdf");
        when(evidenceRepo.findByIdAndTenantIdAndStepId(e.getId(), TENANT, STEP))
                .thenReturn(Optional.of(e));

        service.delete(CYCLE, STEP, e.getId(), ACTOR);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> req =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(TENANT), req.capture());
        assertThat(req.getValue().action()).isEqualTo("pdca.step-evidence.removed");
        assertThat(req.getValue().summary()).contains("retirée");
    }

    // --- signatures binaires ----------------------------------------------------------

    @Test
    void reconnait_lesSignaturesDeChaqueTypeAdmis() {
        assertThat(PdcaStepEvidenceService.magicBytesMatch("application/pdf", PDF)).isTrue();
        assertThat(PdcaStepEvidenceService.magicBytesMatch(DOCX, ZIP)).isTrue();
        assertThat(PdcaStepEvidenceService.magicBytesMatch(XLSX, ZIP)).isTrue();
        assertThat(PdcaStepEvidenceService.magicBytesMatch("image/jpeg",
                new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00 })).isTrue();
        assertThat(PdcaStepEvidenceService.magicBytesMatch("image/png",
                new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A })).isTrue();
        assertThat(PdcaStepEvidenceService.magicBytesMatch("image/webp",
                new byte[] { 'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P' })).isTrue();
        assertThat(PdcaStepEvidenceService.magicBytesMatch("image/heic",
                new byte[] { 0, 0, 0, 0x18, 'f', 't', 'y', 'p' })).isTrue();
    }

    @Test
    void rejette_lesSignaturesIncoherentesEtLesContenusTropCourts() {
        assertThat(PdcaStepEvidenceService.magicBytesMatch("application/pdf", new byte[] { 0x25 })).isFalse();
        assertThat(PdcaStepEvidenceService.magicBytesMatch(DOCX, PDF)).isFalse();
        assertThat(PdcaStepEvidenceService.magicBytesMatch("image/jpeg", new byte[] { 0x00, 0x01, 0x02 })).isFalse();
        assertThat(PdcaStepEvidenceService.magicBytesMatch("image/png", new byte[] { (byte) 0x89, 0x50 })).isFalse();
        assertThat(PdcaStepEvidenceService.magicBytesMatch("image/webp", new byte[] { 'R', 'I', 'F', 'F' })).isFalse();
        assertThat(PdcaStepEvidenceService.magicBytesMatch("image/heic", new byte[] { 0, 0, 0 })).isFalse();
        // Type hors liste blanche : déjà refusé en amont, la signature ne le rattrape pas.
        assertThat(PdcaStepEvidenceService.magicBytesMatch("text/plain", PDF)).isFalse();
    }

    /**
     * Un conteneur RIFF n'est pas un WEBP : WAV, AVI et bien d'autres ouvrent sur
     * les mêmes quatre octets. Chacune des quatre lettres du marqueur doit donc
     * être vérifiée — un contrôle qui s'arrêterait au premier caractère laisserait
     * passer tout le format RIFF.
     */
    @Test
    void rejette_unConteneurRiffQuiNEstPasUnWebp() {
        for (String marqueur : List.of("XEBP", "WXBP", "WEXP", "WEBX")) {
            byte[] c = new byte[] { 'R', 'I', 'F', 'F', 0, 0, 0, 0,
                    (byte) marqueur.charAt(0), (byte) marqueur.charAt(1),
                    (byte) marqueur.charAt(2), (byte) marqueur.charAt(3) };
            assertThat(PdcaStepEvidenceService.magicBytesMatch("image/webp", c))
                    .describedAs(marqueur).isFalse();
        }
    }

    /**
     * Même raisonnement pour la boîte ISO-BMFF : « ftyp » distingue un HEIC d'un
     * MP4 ou d'un MOV, qui partagent la même structure d'en-tête.
     */
    @Test
    void rejette_uneBoiteIsoQuiNEstPasUnFtyp() {
        for (String boite : List.of("Xtyp", "fXyp", "ftXp", "ftyX")) {
            byte[] c = new byte[] { 0, 0, 0, 0x18,
                    (byte) boite.charAt(0), (byte) boite.charAt(1),
                    (byte) boite.charAt(2), (byte) boite.charAt(3) };
            assertThat(PdcaStepEvidenceService.magicBytesMatch("image/heic", c))
                    .describedAs(boite).isFalse();
        }
    }

    @Test
    void accepte_chaqueTypeDeLaListeBlanche_deBoutEnBout() {
        liveCycle();
        stepBelongsToCycle();
        savedEvidenceEchoesInput();

        assertThat(service.upload(CYCLE, STEP, DOCX, "procedure.docx", ZIP, ACTOR).contentType())
                .isEqualTo(DOCX);
        assertThat(service.upload(CYCLE, STEP, XLSX, "releve.xlsx", ZIP, ACTOR).contentType())
                .isEqualTo(XLSX);

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(storage, org.mockito.Mockito.times(2)).put(keys.capture(), anyString(), any());
        assertThat(keys.getAllValues().get(0)).endsWith(".docx");
        assertThat(keys.getAllValues().get(1)).endsWith(".xlsx");
    }
}
