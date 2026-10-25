package com.openlab.qualitos.quality.supplier;

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

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Documents joints aux NC fournisseur et aux certificats.
 *
 * <p>Ce qui se teste : les gardes (liste blanche, signature binaire, nom assaini,
 * clé construite par le serveur), les bornes, l'isolation par tenant et par
 * fournisseur, la nature admise par porteur, le verrou d'une NC close et l'ordre
 * ligne-puis-binaire.
 */
@ExtendWith(MockitoExtension.class)
class SupplierDocumentServiceTest {

    @Mock SupplierDocumentRepository documents;
    @Mock SupplierRepository suppliers;
    @Mock SupplierNonConformityRepository ncs;
    @Mock SupplierCertificateRepository certificates;
    @Mock ObjectProvider<ObjectStorage> storageProvider;
    @Mock ObjectStorage storage;
    @Mock AuditEventService auditEvents;

    private SupplierDocumentService service;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID SUP = UUID.randomUUID();
    private static final UUID NC = UUID.randomUUID();
    private static final UUID CERT = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();
    private static final byte[] PDF = "%PDF-1.7 rapport 8D".getBytes();
    private static final SupplierDocumentOwnerType ON_NC = SupplierDocumentOwnerType.NON_CONFORMITY;
    private static final SupplierDocumentOwnerType ON_CERT = SupplierDocumentOwnerType.CERTIFICATE;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT.toString());
        lenient().when(storageProvider.getIfAvailable()).thenReturn(storage);
        service = new SupplierDocumentService(documents, suppliers, ncs, certificates, storageProvider, auditEvents);
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private Supplier supplier(UUID tenant) {
        Supplier s = new Supplier();
        s.setId(SUP);
        s.setTenantId(tenant);
        s.setCode("ACME-01");
        s.setName("Acme");
        return s;
    }

    private SupplierNonConformity nc(NonConformityStatus status) {
        SupplierNonConformity n = new SupplierNonConformity();
        n.setId(NC);
        n.setTenantId(TENANT);
        n.setSupplierId(SUP);
        n.setPartNumber("PN-42");
        n.setStatus(status);
        n.setSeverity(NonConformitySeverity.MAJOR);
        n.setDetectedOn(LocalDate.parse("2026-10-01"));
        return n;
    }

    private SupplierCertificate cert() {
        SupplierCertificate c = new SupplierCertificate();
        c.setId(CERT);
        c.setTenantId(TENANT);
        c.setSupplierId(SUP);
        c.setStandardCode("iso-9001");
        return c;
    }

    private void openNc() {
        when(suppliers.findById(SUP)).thenReturn(Optional.of(supplier(TENANT)));
        when(ncs.findById(NC)).thenReturn(Optional.of(nc(NonConformityStatus.OPEN)));
    }

    private void saveEchoes() {
        when(documents.save(any())).thenAnswer(inv -> {
            SupplierDocument d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            d.setCreatedAt(Instant.parse("2026-10-24T10:00:00Z"));
            return d;
        });
    }

    @Test
    void joint_un8D_ligneAvantBinaire_cleTenantisee_etJournalise() {
        openNc();
        saveEchoes();

        SupplierDocumentDto.Response out = service.upload(SUP, ON_NC, NC, SupplierDocumentCategory.EIGHT_D,
                "application/pdf", "8D-acme.pdf", PDF, ACTOR);

        assertThat(out.category()).isEqualTo(SupplierDocumentCategory.EIGHT_D);
        assertThat(out.originalFilename()).isEqualTo("8D-acme.pdf");
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        InOrder order = inOrder(documents, storage);
        order.verify(documents).save(any());
        order.verify(storage).put(key.capture(), eq("application/pdf"), eq(PDF));
        assertThat(key.getValue())
                .startsWith("tenants/" + TENANT + "/suppliers/" + SUP + "/non-conformities/" + NC + "/")
                .endsWith(".pdf");
        ArgumentCaptor<AuditEventDto.RecordEventRequest> event =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(TENANT), event.capture());
        assertThat(event.getValue().action()).isEqualTo("supplier.document.uploaded");
        assertThat(event.getValue().summary()).contains("ACME-01").contains("PN-42");
        assertThat(event.getValue().payloadJson()).contains("EIGHT_D").doesNotContain(key.getValue());
    }

    @Test
    void certificat_sansNature_prendCertificat_etCleDeCertificat() {
        when(suppliers.findById(SUP)).thenReturn(Optional.of(supplier(TENANT)));
        when(certificates.findById(CERT)).thenReturn(Optional.of(cert()));
        saveEchoes();

        SupplierDocumentDto.Response out = service.upload(SUP, ON_CERT, CERT, null,
                "application/pdf", "iso9001.pdf", PDF, null);

        assertThat(out.category()).isEqualTo(SupplierDocumentCategory.CERTIFICATE);
        verify(storage).put(contains("/certificates/" + CERT + "/"), eq("application/pdf"), eq(PDF));
        ArgumentCaptor<AuditEventDto.RecordEventRequest> event =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(TENANT), event.capture());
        assertThat(event.getValue().summary()).contains("iso-9001");
    }

    @Test
    void nc_sansNature_prendAutre() {
        openNc();
        saveEchoes();
        assertThat(service.upload(SUP, ON_NC, NC, null, "application/pdf", "x.pdf", PDF, ACTOR).category())
                .isEqualTo(SupplierDocumentCategory.OTHER);
    }

    @Test
    void natureEtrangereAuPorteur_refusee() {
        when(suppliers.findById(SUP)).thenReturn(Optional.of(supplier(TENANT)));
        when(certificates.findById(CERT)).thenReturn(Optional.of(cert()));
        assertThatThrownBy(() -> service.upload(SUP, ON_CERT, CERT, SupplierDocumentCategory.EIGHT_D,
                "application/pdf", "x.pdf", PDF, ACTOR))
                .isInstanceOf(SupplierDocumentValidationException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void nomHostile_assaini_horsDeLaCle() {
        openNc();
        saveEchoes();
        service.upload(SUP, ON_NC, NC, SupplierDocumentCategory.CAPA_EVIDENCE,
                "application/pdf", "../../etc/passwd.pdf", PDF, ACTOR);
        ArgumentCaptor<SupplierDocument> saved = ArgumentCaptor.forClass(SupplierDocument.class);
        verify(documents).save(saved.capture());
        assertThat(saved.getValue().getOriginalFilename()).doesNotContain("/").doesNotContain("..");
        assertThat(saved.getValue().getObjectKey()).doesNotContain("passwd");
    }

    @Test
    void typeHorsListeBlanche_refuse() {
        openNc();
        assertThatThrownBy(() -> service.upload(SUP, ON_NC, NC, SupplierDocumentCategory.OTHER,
                "application/x-msdownload", "a.exe", PDF, ACTOR))
                .isInstanceOf(SupplierDocumentValidationException.class);
        verify(documents, never()).save(any());
        verifyNoInteractions(storage);
    }

    @Test
    void signatureIncoherente_refusee() {
        openNc();
        assertThatThrownBy(() -> service.upload(SUP, ON_NC, NC, SupplierDocumentCategory.OTHER,
                "application/pdf", "faux.pdf", "MZ executable".getBytes(), ACTOR))
                .isInstanceOf(SupplierDocumentValidationException.class);
    }

    @Test
    void fichierVide_refuse() {
        openNc();
        assertThatThrownBy(() -> service.upload(SUP, ON_NC, NC, SupplierDocumentCategory.OTHER,
                "application/pdf", "v.pdf", new byte[0], ACTOR))
                .isInstanceOf(SupplierDocumentValidationException.class);
    }

    @Test
    void fichierTropLourd_refuse() {
        openNc();
        byte[] big = new byte[(int) SupplierDocumentService.MAX_SIZE_BYTES + 1];
        assertThatThrownBy(() -> service.upload(SUP, ON_NC, NC, SupplierDocumentCategory.OTHER,
                "application/pdf", "g.pdf", big, ACTOR))
                .isInstanceOf(SupplierDocumentTooLargeException.class);
    }

    @Test
    void nombreMaximal_atteint_refuse() {
        openNc();
        when(documents.countByTenantIdAndOwnerTypeAndOwnerId(TENANT, ON_NC, NC))
                .thenReturn((long) SupplierDocumentService.MAX_COUNT);
        assertThatThrownBy(() -> service.upload(SUP, ON_NC, NC, SupplierDocumentCategory.OTHER,
                "application/pdf", "x.pdf", PDF, ACTOR))
                .isInstanceOf(SupplierDocumentTooLargeException.class);
    }

    @Test
    void poidsCumule_depasse_refuse() {
        openNc();
        when(documents.sumSizeBytes(TENANT, ON_NC, NC)).thenReturn(SupplierDocumentService.MAX_TOTAL_BYTES);
        assertThatThrownBy(() -> service.upload(SUP, ON_NC, NC, SupplierDocumentCategory.OTHER,
                "application/pdf", "x.pdf", PDF, ACTOR))
                .isInstanceOf(SupplierDocumentTooLargeException.class);
    }

    @Test
    void ncClose_verrouilleAjoutEtRetrait_maisPasLaLecture() {
        when(suppliers.findById(SUP)).thenReturn(Optional.of(supplier(TENANT)));
        when(ncs.findById(NC)).thenReturn(Optional.of(nc(NonConformityStatus.RESOLVED)));
        assertThatThrownBy(() -> service.upload(SUP, ON_NC, NC, SupplierDocumentCategory.OTHER,
                "application/pdf", "x.pdf", PDF, ACTOR))
                .isInstanceOf(SupplierStateException.class);
        assertThatThrownBy(() -> service.delete(SUP, ON_NC, NC, UUID.randomUUID(), ACTOR))
                .isInstanceOf(SupplierStateException.class);

        when(documents.findByTenantIdAndOwnerTypeAndOwnerIdOrderByCreatedAtAsc(TENANT, ON_NC, NC)).thenReturn(List.of());
        assertThat(service.list(SUP, ON_NC, NC)).isEmpty();
    }

    @Test
    void ncRejetee_verrouillee() {
        when(suppliers.findById(SUP)).thenReturn(Optional.of(supplier(TENANT)));
        when(ncs.findById(NC)).thenReturn(Optional.of(nc(NonConformityStatus.REJECTED)));
        assertThatThrownBy(() -> service.upload(SUP, ON_NC, NC, SupplierDocumentCategory.OTHER,
                "application/pdf", "x.pdf", PDF, ACTOR))
                .isInstanceOf(SupplierStateException.class);
    }

    @Test
    void fournisseurDUnAutreTenant_introuvable() {
        when(suppliers.findById(SUP)).thenReturn(Optional.of(supplier(UUID.randomUUID())));
        assertThatThrownBy(() -> service.list(SUP, ON_NC, NC)).isInstanceOf(SupplierNotFoundException.class);
    }

    @Test
    void ncDUnAutreFournisseur_introuvable() {
        when(suppliers.findById(SUP)).thenReturn(Optional.of(supplier(TENANT)));
        SupplierNonConformity other = nc(NonConformityStatus.OPEN);
        other.setSupplierId(UUID.randomUUID());
        when(ncs.findById(NC)).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.list(SUP, ON_NC, NC)).isInstanceOf(SupplierChildNotFoundException.class);
    }

    @Test
    void certificatDUnAutreTenant_introuvable() {
        when(suppliers.findById(SUP)).thenReturn(Optional.of(supplier(TENANT)));
        SupplierCertificate c = cert();
        c.setTenantId(UUID.randomUUID());
        when(certificates.findById(CERT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.list(SUP, ON_CERT, CERT))
                .isInstanceOf(SupplierChildNotFoundException.class);
    }

    @Test
    void sansTenant_refuse() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.list(SUP, ON_NC, NC)).isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void stockageCoupe_503() {
        when(storageProvider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> service.list(SUP, ON_NC, NC)).isInstanceOf(StorageDisabledException.class);
    }

    @Test
    void liste_renvoieUneUrlPresignee_jamaisLaCle() throws Exception {
        openNc();
        SupplierDocument d = new SupplierDocument();
        d.setId(UUID.randomUUID());
        d.setSupplierId(SUP);
        d.setOwnerType(ON_NC);
        d.setOwnerId(NC);
        d.setCategory(SupplierDocumentCategory.EIGHT_D);
        d.setObjectKey("tenants/x/key.pdf");
        d.setContentType("application/pdf");
        d.setSizeBytes(42);
        when(documents.findByTenantIdAndOwnerTypeAndOwnerIdOrderByCreatedAtAsc(TENANT, ON_NC, NC)).thenReturn(List.of(d));
        when(storage.presignGet(eq("tenants/x/key.pdf"), any())).thenReturn(URI.create("https://minio/x?sig=1").toURL());

        List<SupplierDocumentDto.ListItem> out = service.list(SUP, ON_NC, NC);

        assertThat(out).singleElement().satisfies(i -> {
            assertThat(i.url()).isEqualTo("https://minio/x?sig=1");
            assertThat(i.category()).isEqualTo(SupplierDocumentCategory.EIGHT_D);
        });
    }

    @Test
    void retrait_ligneAvantBinaire_etJournalise() {
        openNc();
        UUID docId = UUID.randomUUID();
        SupplierDocument d = new SupplierDocument();
        d.setId(docId);
        d.setObjectKey("tenants/x/k.pdf");
        d.setOwnerType(ON_NC);
        d.setOwnerId(NC);
        d.setSupplierId(SUP);
        d.setCategory(SupplierDocumentCategory.OTHER);
        when(documents.findByIdAndTenantIdAndOwnerTypeAndOwnerId(docId, TENANT, ON_NC, NC)).thenReturn(Optional.of(d));

        service.delete(SUP, ON_NC, NC, docId, ACTOR);

        InOrder order = inOrder(documents, storage);
        order.verify(documents).delete(d);
        order.verify(storage).delete("tenants/x/k.pdf");
        ArgumentCaptor<AuditEventDto.RecordEventRequest> event =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(TENANT), event.capture());
        assertThat(event.getValue().action()).isEqualTo("supplier.document.removed");
    }

    @Test
    void retrait_documentDUnAutrePorteur_introuvable() {
        openNc();
        UUID docId = UUID.randomUUID();
        when(documents.findByIdAndTenantIdAndOwnerTypeAndOwnerId(docId, TENANT, ON_NC, NC)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(SUP, ON_NC, NC, docId, ACTOR))
                .isInstanceOf(SupplierChildNotFoundException.class);
        verify(storage, never()).delete(anyString());
    }

    @Test
    void comptesParPorteur() {
        when(documents.countByOwners(TENANT, ON_NC, List.of(NC))).thenReturn(List.<Object[]>of(new Object[]{NC, 3L}));
        assertThat(service.countByOwners(TENANT, ON_NC, List.of(NC))).isEqualTo(Map.of(NC, 3L));
        assertThat(service.countByOwners(TENANT, ON_NC, List.of())).isEmpty();
        verify(documents, times(1)).countByOwners(any(), any(), any());
    }

    @Test
    void purge_retireLignesEtBinaires() {
        SupplierDocument d = new SupplierDocument();
        d.setObjectKey("tenants/x/a.pdf");
        when(documents.findByTenantIdAndSupplierId(TENANT, SUP)).thenReturn(List.of(d));

        service.purgeSupplier(TENANT, SUP);

        verify(documents).deleteAll(List.of(d));
        verify(storage).delete("tenants/x/a.pdf");
    }

    @Test
    void purge_stockageCoupe_retireQuandMemeLesLignes() {
        SupplierDocument d = new SupplierDocument();
        d.setObjectKey("tenants/x/a.pdf");
        when(documents.findByTenantIdAndOwnerTypeAndOwnerIdOrderByCreatedAtAsc(TENANT, ON_CERT, CERT)).thenReturn(List.of(d));
        when(storageProvider.getIfAvailable()).thenReturn(null);

        service.purgeOwner(TENANT, ON_CERT, CERT);

        verify(documents).deleteAll(List.of(d));
        verifyNoInteractions(storage);
    }

    @Test
    void purge_sansDocument_neFaitRien() {
        when(documents.findByTenantIdAndSupplierId(TENANT, SUP)).thenReturn(List.of());
        service.purgeSupplier(TENANT, SUP);
        verify(documents, never()).deleteAll(any());
    }

    @Test
    void ficheImprimee_lesDocumentsDeLaNc() {
        when(documents.findByTenantIdAndOwnerTypeAndOwnerIdOrderByCreatedAtAsc(TENANT, ON_NC, NC)).thenReturn(List.of());
        assertThat(service.forReport(TENANT, NC)).isEmpty();
    }

    @Test
    void proprietaireDeclareAuBalayeur() {
        SupplierDocumentObjectOwner owner = new SupplierDocumentObjectOwner(documents);
        when(documents.existsByObjectKey("k")).thenReturn(true);
        assertThat(owner.isReferenced("k")).isTrue();
        assertThat(owner.ownerName()).isEqualTo("supplier-document");
    }

    // --- vue GED « Documents fournisseurs » ---

    private SupplierDocument stored(SupplierDocumentOwnerType type, UUID owner, String key) {
        SupplierDocument d = new SupplierDocument();
        d.setId(UUID.randomUUID());
        d.setTenantId(TENANT);
        d.setSupplierId(SUP);
        d.setOwnerType(type);
        d.setOwnerId(owner);
        d.setCategory(type == ON_NC ? SupplierDocumentCategory.EIGHT_D : SupplierDocumentCategory.CERTIFICATE);
        d.setObjectKey(key);
        d.setContentType("application/pdf");
        d.setSizeBytes(12);
        d.setOriginalFilename(key + ".pdf");
        d.setCreatedAt(Instant.parse("2026-10-24T10:00:00Z"));
        return d;
    }

    @Test
    @SuppressWarnings("unchecked")
    void vueGed_nommeFournisseurEtPorteur_trieDuPlusRecent() throws Exception {
        SupplierDocument onNc = stored(ON_NC, NC, "a");
        SupplierDocument onCert = stored(ON_CERT, CERT, "b");
        when(documents.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(onNc, onCert)));
        when(suppliers.findAllById(List.of(SUP))).thenReturn(List.of(supplier(TENANT)));
        when(ncs.findAllById(List.of(NC))).thenReturn(List.of(nc(NonConformityStatus.OPEN)));
        SupplierCertificate c = cert();
        c.setReference("AFAQ-1");
        when(certificates.findAllById(List.of(CERT))).thenReturn(List.of(c));
        when(storage.presignGet(anyString(), any())).thenReturn(URI.create("https://minio/x").toURL());

        var page = service.listAll(SUP, null, null, "  8D_100%  ",
                org.springframework.data.domain.PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(SupplierDocumentDto.CatalogItem::ownerLabel)
                .containsExactly("PN-42", "iso-9001 — AFAQ-1");
        assertThat(page.getContent()).allSatisfy(i -> {
            assertThat(i.supplierCode()).isEqualTo("ACME-01");
            assertThat(i.url()).isEqualTo("https://minio/x");
        });
        ArgumentCaptor<org.springframework.data.domain.Pageable> pageable =
                ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(documents).findAll(any(org.springframework.data.jpa.domain.Specification.class), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt")).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void vueGed_porteurOuFournisseurEtranger_resteAnonyme() {
        SupplierDocument onNc = stored(ON_NC, NC, "a");
        when(documents.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(onNc)));
        when(suppliers.findAllById(List.of(SUP))).thenReturn(List.of(supplier(UUID.randomUUID())));
        SupplierNonConformity foreign = nc(NonConformityStatus.OPEN);
        foreign.setTenantId(UUID.randomUUID());
        when(ncs.findAllById(List.of(NC))).thenReturn(List.of(foreign));
        when(certificates.findAllById(List.of())).thenReturn(List.of());
        org.springframework.data.domain.PageRequest bySize =
                org.springframework.data.domain.PageRequest.of(0, 5, org.springframework.data.domain.Sort.by("sizeBytes"));

        var page = service.listAll(null, ON_NC, SupplierDocumentCategory.EIGHT_D, null, bySize);

        assertThat(page.getContent()).singleElement().satisfies(i -> {
            assertThat(i.supplierName()).isNull();
            assertThat(i.ownerLabel()).isNull();
            assertThat(i.url()).isNull();
        });
        verify(documents).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(bySize));
    }

    @Test
    void vueGed_stockageCoupe_503() {
        when(storageProvider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> service.listAll(null, null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 5)))
                .isInstanceOf(StorageDisabledException.class);
    }

    @Test
    void echappementDeLaRecherche() {
        assertThat(SupplierDocumentService.escapeLike("a%b_c\\d")).isEqualTo("a\\%b\\_c\\\\d");
    }
}
