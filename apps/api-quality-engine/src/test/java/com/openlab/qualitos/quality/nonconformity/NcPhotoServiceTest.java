package com.openlab.qualitos.quality.nonconformity;

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
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NcPhotoServiceTest {

    @Mock NcPhotoRepository photoRepo;
    @Mock NonConformityRepository ncRepo;
    @Mock ObjectProvider<ObjectStorage> storageProvider;

    NcPhotoService service;
    InMemoryObjectStorage storage;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID OTHER = UUID.randomUUID();
    static final UUID NC_ID = UUID.randomUUID();

    // Signatures binaires réelles (M1 : les magic bytes doivent matcher le content-type).
    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};

    @BeforeEach
    void setup() {
        storage = new InMemoryObjectStorage();
        service = new NcPhotoService(photoRepo, ncRepo, storageProvider);
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void clear() { TenantContext.clear(); }

    // --- upload happy path ---
    @Test
    void upload_storesTenantizedKey_andPersistsMetadata() {
        when(storageProvider.getIfAvailable()).thenReturn(storage);
        when(ncRepo.findByIdAndTenantId(NC_ID, TENANT)).thenReturn(Optional.of(nc(TENANT, NcStatus.OPEN)));
        when(photoRepo.save(any())).thenAnswer(inv -> {
            NcPhoto p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(Instant.now());
            return p;
        });

        NcPhotoDto.Response r = service.upload(NC_ID, "image/png", "../../etc/passwd.png", PNG);

        ArgumentCaptor<NcPhoto> cap = ArgumentCaptor.forClass(NcPhoto.class);
        verify(photoRepo).save(cap.capture());
        NcPhoto saved = cap.getValue();
        assertThat(saved.getTenantId()).isEqualTo(TENANT);
        assertThat(saved.getNcId()).isEqualTo(NC_ID);
        assertThat(saved.getObjectKey())
                .startsWith("tenants/" + TENANT + "/nc/" + NC_ID + "/")
                .endsWith(".png");
        // L'extension vient du content-type, pas du nom de fichier (path traversal neutralisé).
        assertThat(saved.getObjectKey()).doesNotContain("passwd").doesNotContain("..");
        assertThat(saved.getContentType()).isEqualTo("image/png");
        assertThat(saved.getSizeBytes()).isEqualTo(PNG.length);
        assertThat(saved.getOriginalFilename()).isEqualTo("passwd.png");
        assertThat(storage.contains(saved.getObjectKey())).isTrue();
        assertThat(r.contentType()).isEqualTo("image/png");
        assertThat(r.objectKey()).isEqualTo(saved.getObjectKey());
    }

    @Test
    void upload_normalizesContentTypeCase() {
        when(storageProvider.getIfAvailable()).thenReturn(storage);
        when(ncRepo.findByIdAndTenantId(NC_ID, TENANT)).thenReturn(Optional.of(nc(TENANT, NcStatus.OPEN)));
        when(photoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NcPhotoDto.Response r = service.upload(NC_ID, "IMAGE/JPEG", "p.jpg", JPEG);
        assertThat(r.contentType()).isEqualTo("image/jpeg");
        assertThat(r.objectKey()).endsWith(".jpg");
    }

    // --- M1 : magic bytes ---
    @Test
    void upload_contentTypeMismatchMagicBytes_400() {
        when(storageProvider.getIfAvailable()).thenReturn(storage);
        when(ncRepo.findByIdAndTenantId(NC_ID, TENANT)).thenReturn(Optional.of(nc(TENANT, NcStatus.OPEN)));
        // Déclaré image/png mais octets JPEG → rejet 400, rien n'est persisté ni stocké.
        assertThatThrownBy(() -> service.upload(NC_ID, "image/png", "x.png", JPEG))
                .isInstanceOf(NcPhotoValidationException.class)
                .hasMessageContaining("does not match");
        verifyNoInteractions(photoRepo);
        assertThat(storage.size()).isZero();
    }

    @Test
    void upload_disguisedExecutable_400() {
        when(storageProvider.getIfAvailable()).thenReturn(storage);
        when(ncRepo.findByIdAndTenantId(NC_ID, TENANT)).thenReturn(Optional.of(nc(TENANT, NcStatus.OPEN)));
        // 'MZ...' (PE/EXE) renommé en image/png : refusé.
        byte[] exe = {0x4D, 0x5A, (byte) 0x90, 0x00};
        assertThatThrownBy(() -> service.upload(NC_ID, "image/png", "evil.png", exe))
                .isInstanceOf(NcPhotoValidationException.class);
        verifyNoInteractions(photoRepo);
        assertThat(storage.size()).isZero();
    }

    @Test
    void upload_acceptsWebpSignature() {
        when(storageProvider.getIfAvailable()).thenReturn(storage);
        when(ncRepo.findByIdAndTenantId(NC_ID, TENANT)).thenReturn(Optional.of(nc(TENANT, NcStatus.OPEN)));
        when(photoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // 'RIFF' .... 'WEBP'
        byte[] webp = {0x52, 0x49, 0x46, 0x46, 0x10, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50};
        NcPhotoDto.Response r = service.upload(NC_ID, "image/webp", "x.webp", webp);
        assertThat(r.objectKey()).endsWith(".webp");
    }

    // --- BUG #5 : métadonnées AVANT binaire ---
    @Test
    void upload_persistsMetadataBeforeStorage() {
        when(storageProvider.getIfAvailable()).thenReturn(storage);
        when(ncRepo.findByIdAndTenantId(NC_ID, TENANT)).thenReturn(Optional.of(nc(TENANT, NcStatus.OPEN)));
        when(photoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.upload(NC_ID, "image/png", "p.png", PNG);

        // L'ordre attendu : save (ligne) PUIS put (objet). Avec un storage réel mockable on
        // vérifie qu'aucun objet n'est écrit si le save échoue (voir test suivant).
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(photoRepo);
        order.verify(photoRepo).save(any());
    }

    @Test
    void upload_metadataSaveFails_noOrphanObject() {
        // Storage mockable pour observer l'absence d'appel put quand le save plante.
        ObjectStorage mockStorage = org.mockito.Mockito.mock(ObjectStorage.class);
        when(storageProvider.getIfAvailable()).thenReturn(mockStorage);
        when(ncRepo.findByIdAndTenantId(NC_ID, TENANT)).thenReturn(Optional.of(nc(TENANT, NcStatus.OPEN)));
        when(photoRepo.save(any())).thenThrow(
                new org.springframework.dao.DataIntegrityViolationException("db down"));

        assertThatThrownBy(() -> service.upload(NC_ID, "image/png", "p.png", PNG))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        // L'objet n'a jamais été écrit → aucun binaire orphelin.
        verify(mockStorage, never()).put(any(), any(), any());
    }

    // --- validation ---
    @Test
    void upload_rejectsUnsupportedContentType_400() {
        when(storageProvider.getIfAvailable()).thenReturn(storage);
        when(ncRepo.findByIdAndTenantId(NC_ID, TENANT)).thenReturn(Optional.of(nc(TENANT, NcStatus.OPEN)));
        assertThatThrownBy(() -> service.upload(NC_ID, "application/pdf", "x.pdf", PNG))
                .isInstanceOf(NcPhotoValidationException.class);
        verifyNoInteractions(photoRepo);
        assertThat(storage.size()).isZero();
    }

    @Test
    void upload_rejectsEmptyContent_400() {
        when(storageProvider.getIfAvailable()).thenReturn(storage);
        when(ncRepo.findByIdAndTenantId(NC_ID, TENANT)).thenReturn(Optional.of(nc(TENANT, NcStatus.OPEN)));
        assertThatThrownBy(() -> service.upload(NC_ID, "image/png", "x.png", new byte[0]))
                .isInstanceOf(NcPhotoValidationException.class);
        verifyNoInteractions(photoRepo);
    }

    @Test
    void upload_rejectsOversized_413() {
        when(storageProvider.getIfAvailable()).thenReturn(storage);
        when(ncRepo.findByIdAndTenantId(NC_ID, TENANT)).thenReturn(Optional.of(nc(TENANT, NcStatus.OPEN)));
        byte[] big = new byte[(int) (NcPhotoService.MAX_SIZE_BYTES + 1)];
        assertThatThrownBy(() -> service.upload(NC_ID, "image/png", "x.png", big))
                .isInstanceOf(NcPhotoTooLargeException.class);
        verifyNoInteractions(photoRepo);
        assertThat(storage.size()).isZero();
    }

    // --- NC state / existence ---
    @Test
    void upload_onClosedNc_409() {
        when(storageProvider.getIfAvailable()).thenReturn(storage);
        when(ncRepo.findByIdAndTenantId(NC_ID, TENANT)).thenReturn(Optional.of(nc(TENANT, NcStatus.CLOSED)));
        assertThatThrownBy(() -> service.upload(NC_ID, "image/png", "x.png", PNG))
                .isInstanceOf(NcStateException.class);
        verifyNoInteractions(photoRepo);
    }

    @Test
    void upload_onCancelledNc_409() {
        when(storageProvider.getIfAvailable()).thenReturn(storage);
        when(ncRepo.findByIdAndTenantId(NC_ID, TENANT)).thenReturn(Optional.of(nc(TENANT, NcStatus.CANCELLED)));
        assertThatThrownBy(() -> service.upload(NC_ID, "image/png", "x.png", PNG))
                .isInstanceOf(NcStateException.class);
    }

    @Test
    void upload_ncOfOtherTenant_404() {
        when(storageProvider.getIfAvailable()).thenReturn(storage);
        when(ncRepo.findByIdAndTenantId(NC_ID, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.upload(NC_ID, "image/png", "x.png", PNG))
                .isInstanceOf(NcNotFoundException.class);
    }

    @Test
    void upload_storageDisabled_503() {
        when(storageProvider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> service.upload(NC_ID, "image/png", "x.png", PNG))
                .isInstanceOf(StorageDisabledException.class);
        verifyNoInteractions(ncRepo);
        verifyNoInteractions(photoRepo);
    }

    @Test
    void upload_missingTenant_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.upload(NC_ID, "image/png", "x.png", PNG))
                .isInstanceOf(MissingTenantContextException.class);
    }

    // --- list ---
    @Test
    void list_returnsPresignedUrls() {
        when(storageProvider.getIfAvailable()).thenReturn(storage);
        when(ncRepo.findByIdAndTenantId(NC_ID, TENANT)).thenReturn(Optional.of(nc(TENANT, NcStatus.OPEN)));
        NcPhoto p = photo(TENANT, NC_ID, "tenants/" + TENANT + "/nc/" + NC_ID + "/a.png");
        when(photoRepo.findByTenantIdAndNcIdOrderByCreatedAtAsc(TENANT, NC_ID)).thenReturn(List.of(p));

        List<NcPhotoDto.ListItem> items = service.list(NC_ID);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).url())
                .startsWith("https://storage.test/")
                .contains(p.getObjectKey())
                .contains("ttl=900"); // 15 min
    }

    @Test
    void list_ncOfOtherTenant_404() {
        when(storageProvider.getIfAvailable()).thenReturn(storage);
        when(ncRepo.findByIdAndTenantId(NC_ID, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.list(NC_ID)).isInstanceOf(NcNotFoundException.class);
    }

    // --- delete ---
    @Test
    void delete_removesObjectAndRow() {
        when(storageProvider.getIfAvailable()).thenReturn(storage);
        UUID photoId = UUID.randomUUID();
        String key = "tenants/" + TENANT + "/nc/" + NC_ID + "/a.png";
        storage.put(key, "image/png", PNG);
        NcPhoto p = photo(TENANT, NC_ID, key);
        p.setId(photoId);
        when(photoRepo.findByIdAndTenantIdAndNcId(photoId, TENANT, NC_ID)).thenReturn(Optional.of(p));

        service.delete(NC_ID, photoId);

        assertThat(storage.contains(key)).isFalse();
        verify(photoRepo).delete(p);
    }

    @Test
    void delete_photoOfOtherTenant_404() {
        when(storageProvider.getIfAvailable()).thenReturn(storage);
        UUID photoId = UUID.randomUUID();
        when(photoRepo.findByIdAndTenantIdAndNcId(photoId, TENANT, NC_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(NC_ID, photoId))
                .isInstanceOf(NcPhotoNotFoundException.class);
        verify(photoRepo, never()).delete(any());
    }

    // --- helpers ---
    private NonConformity nc(UUID tenant, NcStatus status) {
        NonConformity n = new NonConformity();
        n.setId(NC_ID);
        n.setTenantId(tenant);
        n.setReference("NC-2026-0001");
        n.setTitle("t");
        n.setCategory(NcCategory.PRODUCT);
        n.setSeverity(NcSeverity.MAJOR);
        n.setStatus(status);
        n.setDetectedAt(Instant.now());
        return n;
    }

    private NcPhoto photo(UUID tenant, UUID ncId, String key) {
        NcPhoto p = new NcPhoto();
        p.setId(UUID.randomUUID());
        p.setTenantId(tenant);
        p.setNcId(ncId);
        p.setObjectKey(key);
        p.setContentType("image/png");
        p.setSizeBytes(PNG.length);
        p.setOriginalFilename("a.png");
        p.setCreatedAt(Instant.now());
        return p;
    }
}
