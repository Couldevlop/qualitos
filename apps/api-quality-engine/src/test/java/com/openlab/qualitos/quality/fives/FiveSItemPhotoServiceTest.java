package com.openlab.qualitos.quality.fives;

import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.nonconformity.storage.ObjectStorage;
import com.openlab.qualitos.quality.nonconformity.storage.StorageDisabledException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La photo prise sur le terrain, attachée à un pilier d'un audit 5S (§3.2).
 *
 * <p>Ce qui se joue ici n'est pas le téléversement — c'est ce qu'on refuse et ce
 * qu'on laisse volontairement passer. Une photo arrivée après la clôture est
 * acceptée, parce qu'elle a été prise pendant la visite et que la file hors
 * ligne ne se vide qu'au retour du réseau ; un retrait après clôture, lui, est
 * refusé, parce qu'un dossier dont on retire les pièces ne prouve plus rien.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FiveSItemPhotoServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID AUDIT = UUID.randomUUID();
    private static final UUID ACTEUR = UUID.randomUUID();

    /** Les trois premiers octets qu'un JPEG doit porter. */
    private static final byte[] OCTETS_JPEG = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x11 };

    @Mock FiveSItemPhotoRepository photos;
    @Mock FiveSAuditRepository audits;
    @Mock FiveSAuditItemRepository items;
    @Mock ObjectStorage storage;
    @Mock ObjectProvider<ObjectStorage> storageProvider;
    @Mock AuditEventService auditEvents;

    FiveSItemPhotoService service;

    @BeforeEach
    void monter() {
        TenantContext.setTenantId(TENANT.toString());
        when(storageProvider.getIfAvailable()).thenReturn(storage);
        service = new FiveSItemPhotoService(photos, audits, items, storageProvider, auditEvents);
    }

    @AfterEach
    void demonter() {
        TenantContext.clear();
    }

    // ---------- ce qui est versé ----------

    @Test
    @DisplayName("la clé de stockage porte le client, l'audit et le pilier, et son extension vient du type validé")
    void depot_cleTenantiseeEtExtensionDuType() {
        audit(FiveSAuditStatus.IN_PROGRESS);
        pilierNote(FiveSPillar.SEIRI, 3);
        when(photos.save(any())).thenAnswer(i -> i.getArgument(0));

        service.upload(AUDIT, FiveSPillar.SEIRI, "image/jpeg", "zone encombrée.JPG",
                OCTETS_JPEG, ACTEUR);

        var enregistree = capturee();
        assertThat(enregistree.getObjectKey())
                .startsWith("tenants/" + TENANT + "/fives/" + AUDIT + "/SEIRI/")
                .endsWith(".jpg");
        // Le nom d'origine est conservé, neutralisé : il s'affiche et se journalise,
        // mais ne revient jamais dans la clé. L'accent tombe avec le reste de ce qui
        // sort de l'alphabet retenu — un nom de fichier n'est pas un texte à relire.
        assertThat(enregistree.getOriginalFilename()).isEqualTo("zone_encombr_e.JPG");
        assertThat(enregistree.getTenantId()).isEqualTo(TENANT);
        assertThat(enregistree.getPillar()).isEqualTo(FiveSPillar.SEIRI);
        assertThat(enregistree.getUploadedBy()).isEqualTo(ACTEUR);
        verify(storage).put(anyString(), eq("image/jpeg"), any());
    }

    @Test
    @DisplayName("la métadonnée est écrite avant le binaire : un dépôt qui échoue ne laisse pas d'orphelin")
    void depot_metadonneeAvantBinaire() {
        audit(FiveSAuditStatus.IN_PROGRESS);
        pilierNote(FiveSPillar.SEISO, 5);
        when(photos.save(any())).thenAnswer(i -> i.getArgument(0));

        service.upload(AUDIT, FiveSPillar.SEISO, "image/png", "sol.png", octetsPng(), ACTEUR);

        var ordre = org.mockito.Mockito.inOrder(photos, storage);
        ordre.verify(photos).save(any());
        ordre.verify(storage).put(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("le dépôt est inscrit au journal du client")
    void depot_journalise() {
        audit(FiveSAuditStatus.IN_PROGRESS);
        pilierNote(FiveSPillar.SEIRI, 3);
        when(photos.save(any())).thenAnswer(i -> i.getArgument(0));

        service.upload(AUDIT, FiveSPillar.SEIRI, "image/jpeg", "a.jpg", OCTETS_JPEG, ACTEUR);

        verify(auditEvents).recordForTenant(eq(TENANT), any());
    }

    // ---------- ce qui est refusé ----------

    @Test
    @DisplayName("un type hors liste blanche est refusé avant toute écriture")
    void typeInterdit_refuse() {
        audit(FiveSAuditStatus.IN_PROGRESS);
        pilierNote(FiveSPillar.SEIRI, 3);

        assertThatThrownBy(() -> service.upload(AUDIT, FiveSPillar.SEIRI, "application/pdf",
                "rapport.pdf", OCTETS_JPEG, ACTEUR))
                .isInstanceOf(FiveSItemPhotoValidationException.class)
                .hasMessageContaining("Unsupported content type");

        verify(photos, never()).save(any());
        verify(storage, never()).put(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("des octets qui démentent le type déclaré sont refusés")
    void octetsIncoherents_refuses() {
        audit(FiveSAuditStatus.IN_PROGRESS);
        pilierNote(FiveSPillar.SEIRI, 3);

        assertThatThrownBy(() -> service.upload(AUDIT, FiveSPillar.SEIRI, "image/png",
                "faux.png", new byte[] { 0x4D, 0x5A, 0x00 }, ACTEUR))
                .isInstanceOf(FiveSItemPhotoValidationException.class)
                .hasMessageContaining("does not match the declared type");

        verify(photos, never()).save(any());
    }

    @Test
    @DisplayName("une photo vide est refusée")
    void photoVide_refusee() {
        audit(FiveSAuditStatus.IN_PROGRESS);
        pilierNote(FiveSPillar.SEIRI, 3);

        assertThatThrownBy(() -> service.upload(AUDIT, FiveSPillar.SEIRI, "image/jpeg",
                "vide.jpg", new byte[0], ACTEUR))
                .isInstanceOf(FiveSItemPhotoValidationException.class);
    }

    @Test
    @DisplayName("au-delà de 10 Mo la photo est refusée pour sa taille, pas pour sa validité")
    void photoTropLourde_refusee() {
        audit(FiveSAuditStatus.IN_PROGRESS);
        pilierNote(FiveSPillar.SEIRI, 3);
        byte[] enorme = new byte[(int) FiveSItemPhotoService.MAX_SIZE_BYTES + 1];
        enorme[0] = (byte) 0xFF;
        enorme[1] = (byte) 0xD8;
        enorme[2] = (byte) 0xFF;

        assertThatThrownBy(() -> service.upload(AUDIT, FiveSPillar.SEIRI, "image/jpeg",
                "panoramique.jpg", enorme, ACTEUR))
                .isInstanceOf(FiveSItemPhotoTooLargeException.class)
                .hasMessageContaining("single photo");
    }

    @Test
    @DisplayName("la sixième photo d'un pilier est refusée, et le message dit lequel des plafonds a parlé")
    void sixiemePhotoDunPilier_refusee() {
        audit(FiveSAuditStatus.IN_PROGRESS);
        pilierNote(FiveSPillar.SEITON, 4);
        when(photos.countByTenantIdAndAuditIdAndPillar(TENANT, AUDIT, FiveSPillar.SEITON))
                .thenReturn((long) FiveSItemPhotoService.MAX_PER_PILLAR);

        assertThatThrownBy(() -> service.upload(AUDIT, FiveSPillar.SEITON, "image/jpeg",
                "angle6.jpg", OCTETS_JPEG, ACTEUR))
                .isInstanceOf(FiveSItemPhotoTooLargeException.class)
                .hasMessageContaining("at most " + FiveSItemPhotoService.MAX_PER_PILLAR);

        verify(storage, never()).put(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("le plafond cumulé de l'audit est opposé même si le pilier est encore libre")
    void plafondCumuleDeLaudit_oppose() {
        audit(FiveSAuditStatus.IN_PROGRESS);
        pilierNote(FiveSPillar.SEIRI, 3);
        when(photos.countByTenantIdAndAuditIdAndPillar(TENANT, AUDIT, FiveSPillar.SEIRI))
                .thenReturn(1L);
        when(photos.sumSizeBytes(TENANT, AUDIT))
                .thenReturn(FiveSItemPhotoService.MAX_TOTAL_BYTES);

        assertThatThrownBy(() -> service.upload(AUDIT, FiveSPillar.SEIRI, "image/jpeg",
                "encore.jpg", OCTETS_JPEG, ACTEUR))
                .isInstanceOf(FiveSItemPhotoTooLargeException.class)
                .hasMessageContaining("5S audit");
    }

    @Test
    @DisplayName("un pilier sans note ne porte pas de photo : il n'y a pas de constat à montrer")
    void pilierNonNote_refuse() {
        audit(FiveSAuditStatus.IN_PROGRESS);
        when(items.findByAuditIdAndPillar(AUDIT, FiveSPillar.SHITSUKE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upload(AUDIT, FiveSPillar.SHITSUKE, "image/jpeg",
                "a.jpg", OCTETS_JPEG, ACTEUR))
                .isInstanceOf(FiveSStateException.class)
                .hasMessageContaining("has no score");
    }

    @Test
    @DisplayName("une ligne de pilier créée sans note ne suffit pas non plus")
    void pilierSansNote_refuse() {
        audit(FiveSAuditStatus.IN_PROGRESS);
        FiveSAuditItem sansNote = new FiveSAuditItem();
        sansNote.setPillar(FiveSPillar.SEIRI);
        when(items.findByAuditIdAndPillar(AUDIT, FiveSPillar.SEIRI))
                .thenReturn(Optional.of(sansNote));

        assertThatThrownBy(() -> service.upload(AUDIT, FiveSPillar.SEIRI, "image/jpeg",
                "a.jpg", OCTETS_JPEG, ACTEUR))
                .isInstanceOf(FiveSStateException.class);
    }

    @Test
    @DisplayName("un audit annulé n'accepte plus rien")
    void auditAnnule_refuse() {
        audit(FiveSAuditStatus.CANCELLED);

        assertThatThrownBy(() -> service.upload(AUDIT, FiveSPillar.SEIRI, "image/jpeg",
                "a.jpg", OCTETS_JPEG, ACTEUR))
                .isInstanceOf(FiveSStateException.class)
                .hasMessageContaining("CANCELLED");
    }

    /**
     * Le cas du terrain : l'auditeur photographie en zone blanche, rentre, clôt
     * son audit, et la file se vide ensuite. Refuser ces photos détruirait la
     * preuve de ce qui a bien été vu pendant la visite.
     */
    @Test
    @DisplayName("une photo rejouée après la clôture est acceptée : elle a été prise pendant la visite")
    void photoRejoueeApresCloture_acceptee() {
        audit(FiveSAuditStatus.COMPLETED);
        pilierNote(FiveSPillar.SEIRI, 3);
        when(photos.save(any())).thenAnswer(i -> i.getArgument(0));

        var reponse = service.upload(AUDIT, FiveSPillar.SEIRI, "image/jpeg", "a.jpg",
                OCTETS_JPEG, ACTEUR);

        assertThat(reponse.pillar()).isEqualTo(FiveSPillar.SEIRI);
        verify(storage).put(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("un audit d'un autre client n'existe pas")
    void auditDunAutreClient_introuvable() {
        when(audits.findByIdAndTenantId(AUDIT, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upload(AUDIT, FiveSPillar.SEIRI, "image/jpeg",
                "a.jpg", OCTETS_JPEG, ACTEUR))
                .isInstanceOf(FiveSAuditNotFoundException.class);
    }

    @Test
    @DisplayName("sans contexte de client, rien n'est écrit")
    void sansClient_rienNestEcrit() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.upload(AUDIT, FiveSPillar.SEIRI, "image/jpeg",
                "a.jpg", OCTETS_JPEG, ACTEUR))
                .isInstanceOf(MissingTenantContextException.class);

        verify(photos, never()).save(any());
    }

    @Test
    @DisplayName("stockage désactivé : on le dit, on n'écrit pas la métadonnée seule")
    void stockageDesactive_refuse() {
        when(storageProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> service.upload(AUDIT, FiveSPillar.SEIRI, "image/jpeg",
                "a.jpg", OCTETS_JPEG, ACTEUR))
                .isInstanceOf(StorageDisabledException.class);

        verify(photos, never()).save(any());
    }

    // ---------- la lecture ----------

    @Test
    @DisplayName("la liste d'un audit porte une URL présignée, jamais la clé de stockage")
    void liste_urlPresignee() throws MalformedURLException {
        audit(FiveSAuditStatus.COMPLETED);
        when(photos.findByTenantIdAndAuditIdOrderByCreatedAtAsc(TENANT, AUDIT))
                .thenReturn(List.of(photo(FiveSPillar.SEIRI)));
        when(storage.presignGet(anyString(), any(Duration.class)))
                .thenReturn(URI.create("https://objets.test/signee").toURL());

        var liste = service.listForAudit(AUDIT);

        assertThat(liste).hasSize(1);
        assertThat(liste.get(0).url()).isEqualTo("https://objets.test/signee");
    }

    @Test
    @DisplayName("la liste d'un pilier ne demande que ce pilier")
    void listeDunPilier_filtree() throws MalformedURLException {
        audit(FiveSAuditStatus.COMPLETED);
        when(photos.findByTenantIdAndAuditIdAndPillarOrderByCreatedAtAsc(
                TENANT, AUDIT, FiveSPillar.SEISO))
                .thenReturn(List.of(photo(FiveSPillar.SEISO)));
        when(storage.presignGet(anyString(), any(Duration.class)))
                .thenReturn(URI.create("https://objets.test/s").toURL());

        var liste = service.listForPillar(AUDIT, FiveSPillar.SEISO);

        assertThat(liste).singleElement()
                .satisfies(p -> assertThat(p.pillar()).isEqualTo(FiveSPillar.SEISO));
        verify(photos, never()).findByTenantIdAndAuditIdOrderByCreatedAtAsc(any(), any());
    }

    @Test
    @DisplayName("une URL que le stockage ne sait pas signer laisse la vignette vide, sans échouer")
    void urlNonSignable_vignetteVide() {
        audit(FiveSAuditStatus.COMPLETED);
        when(photos.findByTenantIdAndAuditIdOrderByCreatedAtAsc(TENANT, AUDIT))
                .thenReturn(List.of(photo(FiveSPillar.SEIRI)));
        when(storage.presignGet(anyString(), any(Duration.class))).thenReturn((URL) null);

        assertThat(service.listForAudit(AUDIT).get(0).url()).isNull();
    }

    // ---------- le retrait ----------

    @Test
    @DisplayName("le retrait supprime la ligne avant l'objet, et se journalise")
    void retrait_ligneAvantObjet() {
        audit(FiveSAuditStatus.IN_PROGRESS);
        FiveSItemPhoto p = photo(FiveSPillar.SEIRI);
        when(photos.findByIdAndTenantIdAndAuditId(p.getId(), TENANT, AUDIT))
                .thenReturn(Optional.of(p));

        service.delete(AUDIT, p.getId(), ACTEUR);

        var ordre = org.mockito.Mockito.inOrder(photos, storage);
        ordre.verify(photos).delete(p);
        ordre.verify(storage).delete(p.getObjectKey());
        verify(auditEvents).recordForTenant(eq(TENANT), any());
    }

    @Test
    @DisplayName("une photo prise sur un autre audit ne disparaît pas d'ici")
    void photoDunAutreAudit_introuvable() {
        audit(FiveSAuditStatus.IN_PROGRESS);
        UUID ailleurs = UUID.randomUUID();
        when(photos.findByIdAndTenantIdAndAuditId(ailleurs, TENANT, AUDIT))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(AUDIT, ailleurs, ACTEUR))
                .isInstanceOf(FiveSItemPhotoNotFoundException.class);

        verify(storage, never()).delete(anyString());
    }

    /**
     * L'asymétrie assumée du module : on accepte une photo après la clôture,
     * on ne la retire plus. Verser, c'est compléter un constat fait pendant la
     * visite ; retirer, c'est défaire une preuve après signature.
     */
    @Test
    @DisplayName("on ne retire plus rien d'un audit clôturé, alors qu'on y verse encore")
    void retraitApresCloture_refuse() {
        audit(FiveSAuditStatus.COMPLETED);

        assertThatThrownBy(() -> service.delete(AUDIT, UUID.randomUUID(), ACTEUR))
                .isInstanceOf(FiveSStateException.class)
                .hasMessageContaining("COMPLETED");

        verify(photos, never()).delete(any(FiveSItemPhoto.class));
    }

    @Test
    @DisplayName("on ne retire rien d'un audit annulé non plus")
    void retraitSurAuditAnnule_refuse() {
        audit(FiveSAuditStatus.CANCELLED);

        assertThatThrownBy(() -> service.delete(AUDIT, UUID.randomUUID(), ACTEUR))
                .isInstanceOf(FiveSStateException.class);
    }

    // ---------- fixtures ----------

    private void audit(FiveSAuditStatus statut) {
        FiveSAudit a = new FiveSAudit();
        a.setId(AUDIT);
        a.setTenantId(TENANT);
        a.setZone("Atelier B");
        a.setStatus(statut);
        when(audits.findByIdAndTenantId(AUDIT, TENANT)).thenReturn(Optional.of(a));
    }

    private void pilierNote(FiveSPillar pilier, int note) {
        FiveSAuditItem it = new FiveSAuditItem();
        it.setId(UUID.randomUUID());
        it.setPillar(pilier);
        it.setScore(note);
        when(items.findByAuditIdAndPillar(AUDIT, pilier)).thenReturn(Optional.of(it));
    }

    private FiveSItemPhoto photo(FiveSPillar pilier) {
        FiveSItemPhoto p = new FiveSItemPhoto();
        p.setId(UUID.randomUUID());
        p.setTenantId(TENANT);
        p.setAuditId(AUDIT);
        p.setPillar(pilier);
        p.setObjectKey("tenants/" + TENANT + "/fives/" + AUDIT + "/" + pilier + "/x.jpg");
        p.setContentType("image/jpeg");
        p.setSizeBytes(1024);
        return p;
    }

    private FiveSItemPhoto capturee() {
        var captor = org.mockito.ArgumentCaptor.forClass(FiveSItemPhoto.class);
        verify(photos).save(captor.capture());
        return captor.getValue();
    }

    /** Les huit octets de signature d'un PNG. */
    private static byte[] octetsPng() {
        return new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
    }
}
