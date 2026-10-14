package com.openlab.qualitos.quality.audit;

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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Documents joints à un audit.
 *
 * <p>Ce qui se teste : les gardes OWASP (liste blanche, signature binaire, nom
 * assaini, clé construite par le serveur), les bornes annoncées plutôt que
 * contournées, l'isolation par tenant, le verrou d'un audit annulé ou au rapport
 * émis, et l'ordre ligne-puis-binaire qui évite les pièces fantômes.
 */
@ExtendWith(MockitoExtension.class)
class AuditDocumentServiceTest {

    @Mock AuditDocumentRepository documents;
    @Mock AuditPlanRepository plans;
    @Mock AuditReportRepository reports;
    @Mock ObjectProvider<ObjectStorage> storageProvider;
    @Mock ObjectStorage storage;
    @Mock AuditEventService auditEvents;

    private AuditDocumentService service;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID PLAN = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();
    private static final byte[] PDF = "%PDF-1.7 contenu".getBytes();
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1};
    private static final String DOCX =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT.toString());
        lenient().when(storageProvider.getIfAvailable()).thenReturn(storage);
        service = new AuditDocumentService(documents, plans, reports, storageProvider, auditEvents);
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private AuditPlan plan(AuditStatus status) {
        AuditPlan p = new AuditPlan();
        p.setId(PLAN);
        p.setTenantId(TENANT);
        p.setReference("AUD-2026-0001");
        p.setStatus(status);
        return p;
    }

    private void planOuvert() {
        when(plans.findByIdAndTenantId(PLAN, TENANT)).thenReturn(Optional.of(plan(AuditStatus.IN_PROGRESS)));
    }

    private void saveEchoes() {
        when(documents.save(any(AuditDocument.class))).thenAnswer(inv -> {
            AuditDocument d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });
    }

    @Test
    void joint_unPdf_ligneAvantBinaire_etJournalise() {
        planOuvert();
        saveEchoes();

        AuditDocumentDto.Response res = service.upload(PLAN, "application/pdf", "convocation.pdf", PDF, ACTOR);

        assertThat(res.contentType()).isEqualTo("application/pdf");
        assertThat(res.originalFilename()).isEqualTo("convocation.pdf");
        assertThat(res.uploadedBy()).isEqualTo(ACTOR);

        ArgumentCaptor<AuditDocument> saved = ArgumentCaptor.forClass(AuditDocument.class);
        InOrder ordre = inOrder(documents, storage);
        ordre.verify(documents).save(saved.capture());
        ordre.verify(storage).put(eq(saved.getValue().getObjectKey()), eq("application/pdf"), eq(PDF));

        // Clé tenantisée, extension tirée du type validé, jamais du nom client.
        assertThat(saved.getValue().getObjectKey())
                .startsWith("tenants/" + TENANT + "/audits/" + PLAN + "/")
                .endsWith(".pdf");

        ArgumentCaptor<AuditEventDto.RecordEventRequest> evt =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(TENANT), evt.capture());
        assertThat(evt.getValue().action()).isEqualTo("audit.document.uploaded");
        assertThat(evt.getValue().payloadJson()).doesNotContain("tenants/");
    }

    @Test
    void nomDeFichier_hostile_estAssaini_etNEntrePasDansLaCle() {
        planOuvert();
        saveEchoes();

        AuditDocumentDto.Response res = service.upload(PLAN, "application/pdf",
                "..\\..\\etc/pass wd<script>.pdf", PDF, ACTOR);

        assertThat(res.originalFilename()).isEqualTo("pass_wd_script_.pdf");
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(storage).put(key.capture(), anyString(), any());
        assertThat(key.getValue()).doesNotContain("..").doesNotContain("script");
    }

    @Test
    void typeHorsListeBlanche_refuse_rienEcrit() {
        planOuvert();
        assertThatThrownBy(() -> service.upload(PLAN, "application/x-msdownload", "a.exe", PDF, ACTOR))
                .isInstanceOf(AuditDocumentValidationException.class);
        verify(documents, never()).save(any());
        verifyNoInteractions(storage);
    }

    @Test
    void signatureIncoherente_refusee() {
        planOuvert();
        // Un exécutable renommé en .docx : la signature n'est pas celle d'une archive.
        assertThatThrownBy(() -> service.upload(PLAN, DOCX, "rapport.docx", "MZ\u0090".getBytes(), ACTOR))
                .isInstanceOf(AuditDocumentValidationException.class);
        verify(documents, never()).save(any());
    }

    @Test
    void fichierVide_refuse() {
        planOuvert();
        assertThatThrownBy(() -> service.upload(PLAN, "application/pdf", "v.pdf", new byte[0], ACTOR))
                .isInstanceOf(AuditDocumentValidationException.class);
    }

    @Test
    void fichierTropLourd_refuse() {
        planOuvert();
        byte[] gros = new byte[(int) AuditDocumentService.MAX_SIZE_BYTES + 1];
        System.arraycopy(PDF, 0, gros, 0, PDF.length);
        assertThatThrownBy(() -> service.upload(PLAN, "application/pdf", "g.pdf", gros, ACTOR))
                .isInstanceOf(AuditDocumentTooLargeException.class);
    }

    @Test
    void nombreMaximal_atteint_refuse() {
        planOuvert();
        when(documents.countByTenantIdAndPlanId(TENANT, PLAN)).thenReturn((long) AuditDocumentService.MAX_COUNT);
        assertThatThrownBy(() -> service.upload(PLAN, "image/png", "p.png", PNG, ACTOR))
                .isInstanceOf(AuditDocumentTooLargeException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void poidsCumule_depasse_refuse() {
        planOuvert();
        when(documents.sumSizeBytes(TENANT, PLAN)).thenReturn(AuditDocumentService.MAX_TOTAL_BYTES);
        assertThatThrownBy(() -> service.upload(PLAN, "image/png", "p.png", PNG, ACTOR))
                .isInstanceOf(AuditDocumentTooLargeException.class);
    }

    @Test
    void auditAnnule_verrouille() {
        when(plans.findByIdAndTenantId(PLAN, TENANT)).thenReturn(Optional.of(plan(AuditStatus.CANCELLED)));
        assertThatThrownBy(() -> service.upload(PLAN, "application/pdf", "a.pdf", PDF, ACTOR))
                .isInstanceOf(AuditStateException.class);
    }

    @Test
    void rapportEmis_verrouilleAjoutEtRetrait() {
        when(plans.findByIdAndTenantId(PLAN, TENANT)).thenReturn(Optional.of(plan(AuditStatus.COMPLETED)));
        when(reports.existsByPlanIdAndStatus(PLAN, AuditReportStatus.ISSUED)).thenReturn(true);
        assertThatThrownBy(() -> service.upload(PLAN, "application/pdf", "a.pdf", PDF, ACTOR))
                .isInstanceOf(AuditStateException.class);
        assertThatThrownBy(() -> service.delete(PLAN, UUID.randomUUID(), ACTOR))
                .isInstanceOf(AuditStateException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void auditDUnAutreTenant_introuvable() {
        when(plans.findByIdAndTenantId(PLAN, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.list(PLAN)).isInstanceOf(AuditPlanNotFoundException.class);
        assertThatThrownBy(() -> service.upload(PLAN, "application/pdf", "a.pdf", PDF, ACTOR))
                .isInstanceOf(AuditPlanNotFoundException.class);
    }

    @Test
    void sansTenant_refuse() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.list(PLAN)).isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void stockageCoupe_503() {
        when(storageProvider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> service.list(PLAN)).isInstanceOf(StorageDisabledException.class);
    }

    @Test
    void liste_renvoieUneUrlPresignee() throws Exception {
        planOuvert();
        AuditDocument d = new AuditDocument();
        d.setId(UUID.randomUUID());
        d.setPlanId(PLAN);
        d.setTenantId(TENANT);
        d.setObjectKey("tenants/" + TENANT + "/audits/" + PLAN + "/x.pdf");
        d.setContentType("application/pdf");
        d.setSizeBytes(12);
        d.setOriginalFilename("x.pdf");
        when(documents.findByTenantIdAndPlanIdOrderByCreatedAtAsc(TENANT, PLAN)).thenReturn(List.of(d));
        when(storage.presignGet(d.getObjectKey(), AuditDocumentService.PRESIGN_TTL))
                .thenReturn(new URL("https://minio.local/x.pdf?sig=1"));

        List<AuditDocumentDto.ListItem> items = service.list(PLAN);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).url()).isEqualTo("https://minio.local/x.pdf?sig=1");
    }

    @Test
    void retrait_ligneAvantBinaire_etJournalise() {
        planOuvert();
        AuditDocument d = new AuditDocument();
        d.setId(UUID.randomUUID());
        d.setPlanId(PLAN);
        d.setObjectKey("tenants/" + TENANT + "/audits/" + PLAN + "/x.pdf");
        d.setContentType("application/pdf");
        d.setSizeBytes(12);
        when(documents.findByIdAndTenantIdAndPlanId(d.getId(), TENANT, PLAN)).thenReturn(Optional.of(d));

        service.delete(PLAN, d.getId(), ACTOR);

        InOrder ordre = inOrder(documents, storage);
        ordre.verify(documents).delete(d);
        ordre.verify(storage).delete(d.getObjectKey());
        ArgumentCaptor<AuditEventDto.RecordEventRequest> evt =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(TENANT), evt.capture());
        assertThat(evt.getValue().action()).isEqualTo("audit.document.removed");
    }

    @Test
    void retrait_documentDUnAutreAudit_introuvable() {
        planOuvert();
        UUID ailleurs = UUID.randomUUID();
        when(documents.findByIdAndTenantIdAndPlanId(ailleurs, TENANT, PLAN)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(PLAN, ailleurs, ACTOR))
                .isInstanceOf(AuditDocumentNotFoundException.class);
        verifyNoInteractions(storage);
    }
}
