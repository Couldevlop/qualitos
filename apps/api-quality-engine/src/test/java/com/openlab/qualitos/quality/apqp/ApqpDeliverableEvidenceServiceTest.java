package com.openlab.qualitos.quality.apqp;

import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.nonconformity.storage.ObjectStorage;
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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Les pièces versées en preuve d'un livrable APQP.
 *
 * <p>Ce qui se joue ici n'est pas le téléversement — c'est ce qu'on REFUSE : un
 * type qui n'a rien à faire dans un dossier PPAP, des octets qui démentent le
 * type déclaré, un fichier trop lourd, un livrable emprunté à un autre client.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApqpDeliverableEvidenceServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID PHASE = UUID.randomUUID();
    private static final UUID LIVRABLE = UUID.randomUUID();
    private static final UUID ACTEUR = UUID.randomUUID();

    private static final String DOCX =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    /** Une archive ZIP minimale : c'est la signature qu'un .docx doit porter. */
    private static final byte[] OCTETS_DOCX = new byte[] { 0x50, 0x4B, 0x03, 0x04, 0x14, 0x00 };

    @Mock ApqpDeliverableEvidenceRepository evidences;
    @Mock ApqpPhaseRepository phases;
    @Mock ObjectStorage storage;
    @Mock ObjectProvider<ObjectStorage> storageProvider;
    @Mock AuditEventService auditEvents;

    ApqpDeliverableEvidenceService service;

    @BeforeEach
    void monter() {
        TenantContext.setTenantId(TENANT.toString());
        when(storageProvider.getIfAvailable()).thenReturn(storage);
        service = new ApqpDeliverableEvidenceService(
                evidences, phases, storageProvider, auditEvents);
    }

    @AfterEach
    void demonter() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("un .docx est accepté : c'est la forme réelle de ces livrables")
    void docx_estAccepte() {
        when(phases.findByIdAndTenantId(PHASE, TENANT)).thenReturn(Optional.of(phaseAvecLivrable()));
        when(evidences.countByTenantIdAndDeliverableId(TENANT, LIVRABLE)).thenReturn(0L);
        when(evidences.sumSizeBytes(TENANT)).thenReturn(0L);
        when(evidences.save(any())).thenAnswer(i -> i.getArgument(0));

        var reponse = service.upload(PHASE, LIVRABLE, DOCX, "control plan.docx",
                OCTETS_DOCX, ACTEUR);

        // La clé est construite d'identifiants tenus par la plateforme, et son
        // extension vient du type VALIDÉ — jamais du nom du fichier.
        assertThat(reponse.objectKey())
                .startsWith("tenants/" + TENANT + "/apqp/" + PHASE + "/deliverables/" + LIVRABLE + "/")
                .endsWith(".docx");
        // Le nom d'origine est conservé, neutralisé : il s'affiche et se journalise.
        assertThat(reponse.originalFilename()).isEqualTo("control_plan.docx");
        verify(storage).put(anyString(), any(), any());
    }

    @Test
    @DisplayName("un type hors liste blanche est refusé avant toute écriture")
    void typeInterdit_refuse() {
        when(phases.findByIdAndTenantId(PHASE, TENANT)).thenReturn(Optional.of(phaseAvecLivrable()));

        assertThatThrownBy(() -> service.upload(PHASE, LIVRABLE, "application/zip",
                "tout.zip", OCTETS_DOCX, ACTEUR))
                .isInstanceOf(ApqpDeliverableEvidenceValidationException.class)
                .hasMessageContaining("Unsupported content type");

        verify(storage, never()).put(anyString(), any(), any());
        verify(evidences, never()).save(any());
    }

    @Test
    @DisplayName("des octets qui démentent le type déclaré sont refusés")
    void octetsIncoherents_refuses() {
        when(phases.findByIdAndTenantId(PHASE, TENANT)).thenReturn(Optional.of(phaseAvecLivrable()));

        // Un exécutable renommé en .docx passerait la liste blanche : c'est la
        // signature binaire qui l'arrête.
        byte[] pasUneArchive = "MZ executable".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> service.upload(PHASE, LIVRABLE, DOCX, "plan.docx",
                pasUneArchive, ACTEUR))
                .isInstanceOf(ApqpDeliverableEvidenceValidationException.class)
                .hasMessageContaining("does not match");

        verify(storage, never()).put(anyString(), any(), any());
    }

    @Test
    @DisplayName("un fichier vide est refusé : il ne prouve rien")
    void fichierVide_refuse() {
        when(phases.findByIdAndTenantId(PHASE, TENANT)).thenReturn(Optional.of(phaseAvecLivrable()));

        assertThatThrownBy(() -> service.upload(PHASE, LIVRABLE, DOCX, "vide.docx",
                new byte[0], ACTEUR))
                .isInstanceOf(ApqpDeliverableEvidenceValidationException.class);
    }

    @Test
    @DisplayName("au-delà de dix mégaoctets par fichier, c'est refusé")
    void fichierTropLourd_refuse() {
        when(phases.findByIdAndTenantId(PHASE, TENANT)).thenReturn(Optional.of(phaseAvecLivrable()));

        byte[] trop = new byte[10 * 1024 * 1024 + 1];
        trop[0] = 0x50;
        trop[1] = 0x4B;
        trop[2] = 0x03;
        trop[3] = 0x04;

        assertThatThrownBy(() -> service.upload(PHASE, LIVRABLE, DOCX, "gros.docx", trop, ACTEUR))
                .isInstanceOf(ApqpDeliverableEvidenceTooLargeException.class)
                .hasMessageContaining("single file");
    }

    @Test
    @DisplayName("la sixième pièce d'un même livrable est refusée")
    void sixiemePiece_refusee() {
        when(phases.findByIdAndTenantId(PHASE, TENANT)).thenReturn(Optional.of(phaseAvecLivrable()));
        when(evidences.countByTenantIdAndDeliverableId(TENANT, LIVRABLE)).thenReturn(5L);

        // Cinq, et non une comme sur une étape PDCA : un dossier PPAP se compose de
        // pièces distinctes. Mais pas sans fin.
        assertThatThrownBy(() -> service.upload(PHASE, LIVRABLE, DOCX, "sixieme.docx",
                OCTETS_DOCX, ACTEUR))
                .isInstanceOf(ApqpDeliverableEvidenceTooLargeException.class)
                .hasMessageContaining("at most 5");
    }

    @Test
    @DisplayName("au-delà de cinquante mégaoctets pour le cycle entier, c'est refusé")
    void cycleTropLourd_refuse() {
        when(phases.findByIdAndTenantId(PHASE, TENANT)).thenReturn(Optional.of(phaseAvecLivrable()));
        when(evidences.countByTenantIdAndDeliverableId(TENANT, LIVRABLE)).thenReturn(1L);
        when(evidences.sumSizeBytes(TENANT)).thenReturn(50L * 1024 * 1024);

        assertThatThrownBy(() -> service.upload(PHASE, LIVRABLE, DOCX, "goutte.docx",
                OCTETS_DOCX, ACTEUR))
                .isInstanceOf(ApqpDeliverableEvidenceTooLargeException.class)
                .hasMessageContaining("APQP cycle");
    }

    @Test
    @DisplayName("la phase d'un autre client est introuvable, pas interdite")
    void phaseDUnAutreClient_introuvable() {
        when(phases.findByIdAndTenantId(PHASE, TENANT)).thenReturn(Optional.empty());

        // 404 et non 403 : ne rien dire de l'existence de la ressource (OWASP A01).
        assertThatThrownBy(() -> service.list(PHASE, LIVRABLE))
                .isInstanceOf(ApqpPhaseNotFoundException.class);
    }

    @Test
    @DisplayName("un livrable d'une autre phase est introuvable")
    void livrableDUneAutrePhase_introuvable() {
        when(phases.findByIdAndTenantId(PHASE, TENANT)).thenReturn(Optional.of(phaseAvecLivrable()));

        assertThatThrownBy(() -> service.list(PHASE, UUID.randomUUID()))
                .isInstanceOf(ApqpDeliverableNotFoundException.class);
    }

    @Test
    @DisplayName("la lecture présigne une URL par pièce, sans la stocker")
    void lecture_presigneLesUrl() throws Exception {
        when(phases.findByIdAndTenantId(PHASE, TENANT)).thenReturn(Optional.of(phaseAvecLivrable()));
        ApqpDeliverableEvidence piece = piece("tenants/x/apqp/y.docx");
        when(evidences.findByTenantIdAndDeliverableIdOrderByCreatedAtAsc(TENANT, LIVRABLE))
                .thenReturn(List.of(piece));
        when(storage.presignGet(piece.getObjectKey(), ApqpDeliverableEvidenceService.PRESIGN_TTL))
                .thenReturn(URI.create("https://minio.local/lien-court").toURL());

        List<ApqpDeliverableEvidenceDto.ListItem> lues = service.list(PHASE, LIVRABLE);

        assertThat(lues).singleElement()
                .satisfies(item -> assertThat(item.url()).isEqualTo("https://minio.local/lien-court"));
    }

    @Test
    @DisplayName("retirer une pièce efface la ligne et le binaire, et se consigne")
    void retrait_effaceLigneEtBinaire() {
        when(phases.findByIdAndTenantId(PHASE, TENANT)).thenReturn(Optional.of(phaseAvecLivrable()));
        ApqpDeliverableEvidence piece = piece("tenants/x/apqp/y.docx");
        when(evidences.findByIdAndTenantIdAndDeliverableId(piece.getId(), TENANT, LIVRABLE))
                .thenReturn(Optional.of(piece));

        service.delete(PHASE, LIVRABLE, piece.getId(), ACTEUR);

        verify(evidences).delete(piece);
        verify(storage).delete("tenants/x/apqp/y.docx");
        // Le retrait est la seule opération qui fait disparaître une preuve d'un
        // dossier PPAP : sans trace, le dossier ne dirait plus ce qu'il a porté.
        verify(auditEvents).recordForTenant(any(), any());
    }

    @Test
    @DisplayName("une pièce versée sur un autre livrable ne se supprime pas d'ici")
    void pieceDUnAutreLivrable_introuvable() {
        when(phases.findByIdAndTenantId(PHASE, TENANT)).thenReturn(Optional.of(phaseAvecLivrable()));
        UUID etrangere = UUID.randomUUID();
        when(evidences.findByIdAndTenantIdAndDeliverableId(etrangere, TENANT, LIVRABLE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(PHASE, LIVRABLE, etrangere, ACTEUR))
                .isInstanceOf(ApqpDeliverableEvidenceNotFoundException.class);
        verify(storage, never()).delete(anyString());
    }

    // ---------- fabriques ----------

    private ApqpPhase phaseAvecLivrable() {
        ApqpPhase phase = new ApqpPhase();
        phase.setId(PHASE);
        phase.setTenantId(TENANT);
        phase.setPosition(4);
        phase.setTitle("Product and Process Validation");

        ApqpDeliverable livrable = new ApqpDeliverable();
        livrable.setId(LIVRABLE);
        livrable.setPosition(8);
        livrable.setLabel("PPAP file and approval form");
        livrable.setPpap(true);
        phase.addDeliverable(livrable);
        return phase;
    }

    private ApqpDeliverableEvidence piece(String cle) {
        ApqpDeliverableEvidence piece = new ApqpDeliverableEvidence();
        piece.setId(UUID.randomUUID());
        piece.setTenantId(TENANT);
        piece.setPhaseId(PHASE);
        piece.setDeliverableId(LIVRABLE);
        piece.setObjectKey(cle);
        piece.setContentType(DOCX);
        piece.setSizeBytes(2048);
        piece.setOriginalFilename("ppap.docx");
        piece.setUploadedBy(ACTEUR);
        return piece;
    }
}
