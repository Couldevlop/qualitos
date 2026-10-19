package com.openlab.qualitos.quality.docs;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.nonconformity.storage.InMemoryObjectStorage;
import com.openlab.qualitos.quality.nonconformity.storage.ObjectStorage;
import com.openlab.qualitos.quality.nonconformity.storage.StorageDisabledException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentFileServiceTest {

    @Mock DocumentRepository documents;
    @Mock DocumentVersionRepository versions;
    @Mock DocumentBlobRepository blobs;
    @Mock DocumentCatalogService catalog;
    @Mock ObjectProvider<ObjectStorage> storageProvider;
    @Mock AuditEventService auditEvents;

    static final Instant NOW = Instant.parse("2026-10-19T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final byte[] PDF = "%PDF-1.7 contenu".getBytes(StandardCharsets.US_ASCII);

    InMemoryObjectStorage storage;
    DocumentFileService service;
    DocumentSettingsRow settings;
    Document doc;
    DocumentVersion version;

    @BeforeEach
    void setUp() {
        storage = new InMemoryObjectStorage();
        when(storageProvider.getIfAvailable()).thenReturn(storage);
        service = new DocumentFileService(documents, versions, blobs, catalog, storageProvider, auditEvents, CLOCK);
        TenantContext.setTenantId(TENANT.toString());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER.toString(), "n/a", List.of()));

        settings = DocumentSettingsRow.defaults(TENANT);
        when(catalog.settingsOf(TENANT)).thenReturn(settings);

        doc = new Document();
        doc.setId(UUID.randomUUID());
        doc.setTenantId(TENANT);
        doc.setCode("PRO-001");
        doc.setStatus(DocumentStatus.ACTIVE);
        version = new DocumentVersion();
        version.setId(UUID.randomUUID());
        version.setDocument(doc);
        version.setVersionNumber(2);
        version.setStatus(VersionStatus.DRAFT);

        when(documents.findByIdAndTenantId(doc.getId(), TENANT)).thenReturn(Optional.of(doc));
        when(versions.findByIdAndDocumentId(version.getId(), doc.getId())).thenReturn(Optional.of(version));
        when(versions.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(blobs.findByTenantIdAndSha256(any(), any())).thenReturn(Optional.empty());
        when(blobs.saveAndFlush(any())).thenAnswer(inv -> {
            DocumentBlob b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    DocumentDto.VersionResponse upload(String type, String name, byte[] content) {
        return service.upload(doc.getId(), version.getId(), type, name, content);
    }

    static DocumentBlob blob(String key) {
        DocumentBlob b = new DocumentBlob();
        b.setId(UUID.randomUUID());
        b.setTenantId(TENANT);
        b.setObjectKey(key);
        b.setSha256("x".repeat(64));
        b.setContentType("application/pdf");
        b.setOriginalSize(42);
        b.setStoredSize(42);
        return b;
    }

    // ---------- dépôt : refus ----------

    @Test
    void upload_stockageDesactive_503() {
        when(storageProvider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> upload("application/pdf", "a.pdf", PDF)).isInstanceOf(StorageDisabledException.class);
    }

    @Test
    void upload_sansTenant_refuse() {
        TenantContext.clear();
        assertThatThrownBy(() -> upload("application/pdf", "a.pdf", PDF))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void upload_documentInconnu() {
        when(documents.findByIdAndTenantId(doc.getId(), TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> upload("application/pdf", "a.pdf", PDF)).isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    void upload_documentArchive() {
        doc.setStatus(DocumentStatus.ARCHIVED);
        assertThatThrownBy(() -> upload("application/pdf", "a.pdf", PDF)).isInstanceOf(DocumentStateException.class);
    }

    @Test
    void upload_versionInconnue() {
        when(versions.findByIdAndDocumentId(version.getId(), doc.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> upload("application/pdf", "a.pdf", PDF))
                .isInstanceOf(DocumentVersionNotFoundException.class);
    }

    @Test
    void upload_versionHorsBrouillon() {
        version.setStatus(VersionStatus.PUBLISHED);
        assertThatThrownBy(() -> upload("application/pdf", "a.pdf", PDF))
                .isInstanceOf(DocumentStateException.class).hasMessageContaining("DRAFT");
    }

    @Test
    void upload_fichierVideOuNull() {
        assertThatThrownBy(() -> upload("application/pdf", "a.pdf", new byte[0]))
                .isInstanceOf(DocumentFileRejectedException.class);
        assertThatThrownBy(() -> upload("application/pdf", "a.pdf", null))
                .isInstanceOf(DocumentFileRejectedException.class);
    }

    @Test
    void upload_auDelaDeLaTailleDuClient_413() {
        settings.setMaxFileMb(1);
        byte[] big = new byte[1024 * 1024 + 1];
        assertThatThrownBy(() -> upload("application/pdf", "a.pdf", big))
                .isInstanceOf(DocumentFileTooLargeException.class).hasMessageContaining("1 MB");
    }

    @Test
    void upload_typeNonAdmis() {
        assertThatThrownBy(() -> upload("application/x-msdownload", "a.exe", PDF))
                .isInstanceOf(DocumentFileRejectedException.class).hasMessageContaining("Unsupported");
    }

    @Test
    void upload_contenuNeCorrespondPasAuType() {
        assertThatThrownBy(() -> upload("application/pdf", "a.pdf", "MZ exe".getBytes(StandardCharsets.US_ASCII)))
                .isInstanceOf(DocumentFileRejectedException.class).hasMessageContaining("does not match");
        assertThat(storage.size()).isZero();
    }

    // ---------- dépôt : succès ----------

    @Test
    void upload_nouveauContenu_ligneObjetEtTrace() {
        DocumentDto.VersionResponse r = upload("application/pdf", "../Procédure achats.pdf", PDF);

        String sha = DocumentFiles.sha256(PDF);
        ArgumentCaptor<DocumentBlob> b = ArgumentCaptor.forClass(DocumentBlob.class);
        verify(blobs).saveAndFlush(b.capture());
        DocumentBlob saved = b.getValue();
        assertThat(saved.getTenantId()).isEqualTo(TENANT);
        assertThat(saved.getSha256()).isEqualTo(sha);
        assertThat(saved.getObjectKey()).startsWith("tenants/" + TENANT + "/documents/blobs/" + sha + "-");
        assertThat(saved.getContentType()).isEqualTo("application/pdf");
        assertThat(saved.getOriginalSize()).isEqualTo(PDF.length);
        assertThat(storage.contains(saved.getObjectKey())).isTrue();
        assertThat(storage.get(saved.getObjectKey()).contentType()).isEqualTo("application/pdf");

        assertThat(version.getBlob()).isSameAs(saved);
        assertThat(version.getFileName()).isEqualTo("Proc_dure_achats.pdf");
        assertThat(version.getContentHash()).isEqualTo(sha);
        assertThat(r.contentHash()).isEqualTo(sha);
        assertThat(r.file().fileName()).isEqualTo("Proc_dure_achats.pdf");
        assertThat(r.file().previewable()).isTrue();

        ArgumentCaptor<AuditEventDto.RecordEventRequest> ev = ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(TENANT), ev.capture());
        assertThat(ev.getValue().action()).isEqualTo("document.file.uploaded");
        assertThat(ev.getValue().actorType()).isEqualTo(ActorType.USER);
        assertThat(ev.getValue().actorUserId()).isEqualTo(USER);
        assertThat(ev.getValue().payloadJson()).contains(sha).doesNotContain(saved.getObjectKey());
    }

    @Test
    void upload_texteCompressible_stockeEnGzip_sansNom_nomParDefaut() {
        byte[] csv = "a;b;c\n".repeat(5_000).getBytes(StandardCharsets.US_ASCII);
        upload("text/csv", null, csv);
        DocumentBlob saved = version.getBlob();
        assertThat(saved.getContentEncoding()).isEqualTo("gzip");
        assertThat(saved.getStoredSize()).isLessThan(saved.getOriginalSize());
        assertThat(storage.encodingOf(saved.getObjectKey())).isEqualTo("gzip");
        assertThat(version.getFileName()).isEqualTo("document.csv");
    }

    @Test
    void upload_contenuDejaConnu_reutiliseLeBlob() {
        DocumentBlob existing = blob("tenants/t/documents/blobs/existing");
        when(blobs.findByTenantIdAndSha256(TENANT, DocumentFiles.sha256(PDF))).thenReturn(Optional.of(existing));

        upload("application/pdf", "a.pdf", PDF);

        verify(blobs, never()).saveAndFlush(any());
        assertThat(storage.size()).isZero();
        assertThat(version.getBlob()).isSameAs(existing);
    }

    @Test
    void upload_memeFichierRedepose_neLibereRien() {
        DocumentBlob existing = blob("k");
        version.setBlob(existing);
        when(blobs.findByTenantIdAndSha256(TENANT, DocumentFiles.sha256(PDF))).thenReturn(Optional.of(existing));

        upload("application/pdf", "a.pdf", PDF);

        verify(versions, never()).existsByBlobId(any());
        verify(blobs, never()).delete(any());
    }

    @Test
    void upload_quotaDepasse_413_sansObjet() {
        settings.setQuotaMb(1);
        when(blobs.sumStoredBytes(TENANT)).thenReturn(1024L * 1024 - 2);
        assertThatThrownBy(() -> upload("application/pdf", "a.pdf", PDF))
                .isInstanceOf(DocumentFileTooLargeException.class).hasMessageContaining("quota");
        verify(blobs, never()).saveAndFlush(any());
        assertThat(storage.size()).isZero();
    }

    @Test
    void upload_quotaRespecte_admis() {
        settings.setQuotaMb(1);
        when(blobs.sumStoredBytes(TENANT)).thenReturn(10L);
        upload("application/pdf", "a.pdf", PDF);
        assertThat(storage.size()).isEqualTo(1);
    }

    @Test
    void upload_remplacement_libereLAncienSansTransaction_immediatement() {
        DocumentBlob old = blob("old-key");
        storage.put("old-key", "application/pdf", PDF);
        version.setBlob(old);
        version.setFilePurgedAt(NOW);
        when(versions.existsByBlobId(old.getId())).thenReturn(false);

        upload("application/pdf", "a.pdf", PDF);

        verify(blobs).delete(old);
        assertThat(storage.contains("old-key")).isFalse();
        assertThat(version.getFilePurgedAt()).isNull();
    }

    @Test
    void upload_remplacement_ancienEncorePartage_conserve() {
        DocumentBlob old = blob("old-key");
        storage.put("old-key", "application/pdf", PDF);
        version.setBlob(old);
        when(versions.existsByBlobId(old.getId())).thenReturn(true);

        upload("application/pdf", "a.pdf", PDF);

        verify(blobs, never()).delete(any());
        assertThat(storage.contains("old-key")).isTrue();
    }

    @Test
    void upload_remplacement_sousTransaction_suppressionApresValidation() {
        DocumentBlob old = blob("old-key");
        storage.put("old-key", "application/pdf", PDF);
        version.setBlob(old);
        when(versions.existsByBlobId(old.getId())).thenReturn(false);
        TransactionSynchronizationManager.initSynchronization();

        upload("application/pdf", "a.pdf", PDF);

        verify(blobs).delete(old);
        assertThat(storage.contains("old-key")).isTrue();
        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        assertThat(syncs).hasSize(1);
        syncs.forEach(TransactionSynchronization::afterCommit);
        assertThat(storage.contains("old-key")).isFalse();
    }

    @Test
    void upload_echecDeSuppression_absorbe() {
        InMemoryObjectStorage failing = new InMemoryObjectStorage() {
            @Override
            public void delete(String key) {
                throw new IllegalStateException("s3 down");
            }
        };
        when(storageProvider.getIfAvailable()).thenReturn(failing);
        DocumentBlob old = blob("old-key");
        version.setBlob(old);
        when(versions.existsByBlobId(old.getId())).thenReturn(false);

        assertThatCode(() -> upload("application/pdf", "a.pdf", PDF)).doesNotThrowAnyException();
        verify(blobs).delete(old);
    }

    @Test
    void upload_sansUtilisateur_traceSysteme() {
        SecurityContextHolder.clearContext();
        upload("application/pdf", "a.pdf", PDF);
        ArgumentCaptor<AuditEventDto.RecordEventRequest> ev = ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(TENANT), ev.capture());
        assertThat(ev.getValue().actorType()).isEqualTo(ActorType.SYSTEM);
        assertThat(ev.getValue().actorUserId()).isNull();
    }

    // ---------- lien ----------

    @Test
    void link_telechargementEtApercu() {
        DocumentBlob b = blob("tenants/t/documents/blobs/k");
        version.setBlob(b);
        version.setFileName("proc.pdf");

        DocumentDto.FileLink dl = service.link(doc.getId(), version.getId(), true);
        assertThat(dl.url()).contains("disposition=attachment").contains("filename=proc.pdf").contains("ttl=600");
        assertThat(dl.fileName()).isEqualTo("proc.pdf");
        assertThat(dl.contentType()).isEqualTo("application/pdf");
        assertThat(dl.sizeBytes()).isEqualTo(42);
        assertThat(dl.expiresInSeconds()).isEqualTo(600);

        assertThat(service.link(doc.getId(), version.getId(), false).url()).contains("disposition=inline");
    }

    @Test
    void link_sansFichier_ouFichierRetire() {
        assertThatThrownBy(() -> service.link(doc.getId(), version.getId(), true))
                .isInstanceOf(DocumentStateException.class).hasMessageContaining("no file");
        version.setFilePurgedAt(NOW);
        assertThatThrownBy(() -> service.link(doc.getId(), version.getId(), true))
                .isInstanceOf(DocumentStateException.class).hasMessageContaining("retention");
    }

    @Test
    void link_refus() {
        when(storageProvider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> service.link(doc.getId(), version.getId(), true))
                .isInstanceOf(StorageDisabledException.class);
        when(storageProvider.getIfAvailable()).thenReturn(storage);

        UUID ghostVersion = UUID.randomUUID();
        when(versions.findByIdAndDocumentId(ghostVersion, doc.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.link(doc.getId(), ghostVersion, true))
                .isInstanceOf(DocumentVersionNotFoundException.class);

        UUID ghostDoc = UUID.randomUUID();
        when(documents.findByIdAndTenantId(ghostDoc, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.link(ghostDoc, version.getId(), true))
                .isInstanceOf(DocumentNotFoundException.class);

        TenantContext.clear();
        assertThatThrownBy(() -> service.link(doc.getId(), version.getId(), true))
                .isInstanceOf(MissingTenantContextException.class);
    }

    // ---------- occupation ----------

    @Test
    void usage_sansEtAvecQuota() {
        when(blobs.countFiles(TENANT)).thenReturn(3L);
        when(versions.sumLogicalBytes(TENANT)).thenReturn(300L);
        when(blobs.sumOriginalBytes(TENANT)).thenReturn(200L);
        when(blobs.sumStoredBytes(TENANT)).thenReturn(100L);

        assertThat(service.usage()).isEqualTo(new DocumentDto.StorageUsage(3, 300, 200, 100, null));
        settings.setQuotaMb(2);
        assertThat(service.usage().quotaBytes()).isEqualTo(2L * 1024 * 1024);
    }

    @Test
    void usage_sansTenant() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.usage()).isInstanceOf(MissingTenantContextException.class);
    }

    // ---------- conservation ----------

    @Test
    void purgeExpired_retireLesFichiersEtTrace() {
        SecurityContextHolder.clearContext();
        DocumentBlob shared = blob("shared");
        DocumentBlob alone = blob("alone");
        storage.put("shared", "application/pdf", PDF);
        storage.put("alone", "application/pdf", PDF);
        DocumentVersion v1 = obsolete(shared);
        DocumentVersion v2 = obsolete(alone);
        when(versions.findPurgeable(eq(TENANT), eq(VersionStatus.OBSOLETE), any(), any(Pageable.class)))
                .thenReturn(List.of(v1, v2));
        when(versions.existsByBlobId(shared.getId())).thenReturn(true);
        when(versions.existsByBlobId(alone.getId())).thenReturn(false);

        int n = service.purgeExpired(TENANT, 3);

        assertThat(n).isEqualTo(2);
        ArgumentCaptor<Instant> before = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(versions).findPurgeable(eq(TENANT), eq(VersionStatus.OBSOLETE), before.capture(), page.capture());
        assertThat(before.getValue()).isEqualTo(Instant.parse("2023-10-19T10:00:00Z"));
        assertThat(page.getValue().getPageSize()).isEqualTo(DocumentFileService.PURGE_BATCH);
        assertThat(v1.getBlob()).isNull();
        assertThat(v1.getFilePurgedAt()).isEqualTo(NOW);
        assertThat(storage.contains("shared")).isTrue();
        assertThat(storage.contains("alone")).isFalse();
        verify(blobs).delete(alone);
        verify(blobs, never()).delete(shared);
        ArgumentCaptor<AuditEventDto.RecordEventRequest> ev = ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents, times(2)).recordForTenant(eq(TENANT), ev.capture());
        assertThat(ev.getAllValues()).allMatch(e -> e.action().equals("document.file.purged")
                && e.actorType() == ActorType.SYSTEM);
    }

    @Test
    void purgeExpired_stockageAbsent_ligneRetireeSeulement() {
        when(storageProvider.getIfAvailable()).thenReturn(null);
        DocumentBlob b = blob("k");
        when(versions.findPurgeable(eq(TENANT), eq(VersionStatus.OBSOLETE), any(), any(Pageable.class)))
                .thenReturn(List.of(obsolete(b)));
        assertThat(service.purgeExpired(TENANT, 1)).isEqualTo(1);
        verify(blobs).delete(b);
    }

    @Test
    void purgeExpired_rienADire() {
        when(versions.findPurgeable(any(), any(), any(), any(Pageable.class))).thenReturn(List.of());
        assertThat(service.purgeExpired(TENANT, 1)).isZero();
        verifyNoInteractions(auditEvents);
    }

    DocumentVersion obsolete(DocumentBlob b) {
        DocumentVersion v = new DocumentVersion();
        v.setId(UUID.randomUUID());
        v.setDocument(doc);
        v.setVersionNumber(1);
        v.setStatus(VersionStatus.OBSOLETE);
        v.setBlob(b);
        v.setContentHash(b.getSha256());
        return v;
    }
}
