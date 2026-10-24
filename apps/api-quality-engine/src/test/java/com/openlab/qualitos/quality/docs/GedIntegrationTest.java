package com.openlab.qualitos.quality.docs;

import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.nonconformity.storage.InMemoryObjectStorage;
import com.openlab.qualitos.quality.nonconformity.storage.ObjectStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La GED de bout en bout sur le mapping JPA réel (ADR 0094) : l'arborescence
 * semée au premier usage, les filtres de la liste, la vue « à valider par moi »
 * (requête native), le dépôt dédoublonné et compressé, le remplacement, la
 * conservation et l'occupation du stockage.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
class GedIntegrationTest {

    @TestConfiguration
    static class Storage {
        @Bean
        InMemoryObjectStorage inMemoryObjectStorage() {
            return new InMemoryObjectStorage();
        }
    }

    @Autowired DocumentService documents;
    @Autowired DocumentCatalogService catalog;
    @Autowired DocumentFileService files;
    @Autowired DocumentVersionRepository versions;
    @Autowired ObjectStorage storage;
    @Autowired TransactionTemplate tx;

    final UUID tenant = UUID.randomUUID();
    final UUID autre = UUID.randomUUID();
    final UUID redacteur = UUID.randomUUID();
    final UUID valideur = UUID.randomUUID();

    static final byte[] PDF = "%PDF-1.7\n1 0 obj\n<<>>\nendobj\n".getBytes(StandardCharsets.US_ASCII);

    <T> T dans(Supplier<T> f) {
        return tx.execute(s -> f.get());
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    void connecte(UUID t, UUID user, String... roles) {
        TenantContext.setTenantId(t.toString());
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(user.toString())
                .claim("tenant_id", t.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
                Arrays.stream(roles).map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList(), user.toString()));
    }

    DocumentDto.DocumentResponse cree(String code, String titre, UUID folder, UUID process) {
        return dans(() -> documents.createDocument(new DocumentDto.CreateDocumentRequest(code, titre, null,
                DocumentType.PROCEDURE, redacteur, false, null, null, null, folder, process)));
    }

    List<String> codes(DocumentDto.ListFilter f) {
        return dans(() -> documents.search(f, PageRequest.of(0, 50)).map(DocumentDto.DocumentResponse::code)
                .getContent());
    }

    /** Le circuit de signature d'une soumission : les signataires, dans l'ordre (ADR 0099). */
    static DocumentDto.SubmitRequest signataires(UUID... ids) {
        return new DocumentDto.SubmitRequest(Arrays.stream(ids)
                .map(id -> new DocumentDto.SignatoryRequest(id, "Personne " + id)).toList());
    }

    static DocumentDto.ListFilter vue(DocumentView v) {
        return new DocumentDto.ListFilter(v, null, null, null, null, null, null);
    }

    @Test
    void arborescenceSemeeAuPremierUsage_etFiltresDeLaListe() {
        connecte(tenant, redacteur, "QUALITY_MANAGER");
        List<DocumentDto.FolderView> dossiers = dans(catalog::folders);
        assertThat(dossiers).extracting(DocumentDto.FolderView::name)
                .contains("Manuel qualité", "Procédures", "Processus de réalisation", "Documents externes");
        // Une seconde lecture ne sème pas deux fois.
        assertThat(dans(catalog::folders)).hasSameSizeAs(dossiers);
        assertThat(dans(catalog::processes)).extracting(DocumentDto.ProcessView::name).contains("Management qualité");

        UUID procedures = dossiers.stream().filter(f -> f.name().equals("Procédures")).findFirst().orElseThrow().id();
        UUID realisation = dossiers.stream().filter(f -> f.name().equals("Processus de réalisation"))
                .findFirst().orElseThrow().id();
        UUID manuel = dossiers.stream().filter(f -> f.name().equals("Manuel qualité")).findFirst().orElseThrow().id();
        UUID achats = dans(catalog::processes).stream().filter(p -> p.name().equals("Achats"))
                .findFirst().orElseThrow().id();

        cree("PRO-ACH-003", "Évaluation des fournisseurs", realisation, achats);
        cree("MAN-QUA-001", "Manuel qualité", manuel, null);
        cree("PRO-100%", "Cent pour cent", procedures, null);

        // Un dossier montre aussi ce que contiennent ses sous-dossiers.
        assertThat(codes(new DocumentDto.ListFilter(DocumentView.ALL, procedures, null, null, null, null, null)))
                .containsExactly("PRO-100%", "PRO-ACH-003");
        assertThat(codes(new DocumentDto.ListFilter(DocumentView.ALL, null, "fournis", null, null, null, null)))
                .containsExactly("PRO-ACH-003");
        // Un « % » tapé se cherche tel quel.
        assertThat(codes(new DocumentDto.ListFilter(DocumentView.ALL, null, "%", null, null, null, null)))
                .containsExactly("PRO-100%");
        assertThat(codes(new DocumentDto.ListFilter(DocumentView.ALL, null, null, null, null, achats, null)))
                .containsExactly("PRO-ACH-003");
        assertThat(codes(new DocumentDto.ListFilter(DocumentView.ALL, null, null, DisplayStatus.DRAFT,
                DocumentType.PROCEDURE, null, redacteur))).hasSize(3);
        assertThat(codes(vue(DocumentView.MINE))).hasSize(3);
        connecte(tenant, valideur, "QUALITY_MANAGER");
        assertThat(codes(vue(DocumentView.MINE))).isEmpty();
        connecte(tenant, redacteur, "QUALITY_MANAGER");

        // Un dossier qui contient des documents ne se supprime pas ; un client voisin ne le voit pas.
        assertThatThrownBy(() -> dans(() -> { catalog.deleteFolder(manuel); return null; }))
                .isInstanceOf(DocumentStateException.class);
        assertThatThrownBy(() -> dans(() -> { catalog.deleteFolder(procedures); return null; }))
                .isInstanceOf(DocumentStateException.class);
        connecte(autre, redacteur, "QUALITY_MANAGER");
        assertThat(codes(vue(DocumentView.ALL))).isEmpty();
        assertThatThrownBy(() -> codes(new DocumentDto.ListFilter(DocumentView.ALL, procedures, null, null,
                null, null, null))).isInstanceOf(DocumentCatalogNotFoundException.class);
    }

    @Test
    void cycleDeVie_aValiderParMoi_revueEtStatutAffiche() {
        connecte(tenant, redacteur, "QUALITY_MANAGER");
        DocumentDto.DocumentResponse d = cree("PRO-QUA-001", "Maîtrise des documents", null, null);
        UUID v1 = d.versions().get(0).id();
        // Sans fichier, la version ne part pas en validation : un document se valide sur pièce.
        assertThatThrownBy(() -> dans(() -> documents.submitForReview(d.id(), v1, signataires(valideur))))
                .isInstanceOf(DocumentStateException.class);
        dans(() -> files.upload(d.id(), v1, "application/pdf", "procedure.pdf", PDF));
        UUID second = UUID.randomUUID();
        dans(() -> documents.submitForReview(d.id(), v1, signataires(valideur, second)));

        // L'auteur ne valide pas sa propre version ; le signataire 1, si — et lui seul pour l'instant.
        assertThat(codes(vue(DocumentView.TO_VALIDATE))).isEmpty();
        connecte(tenant, valideur, "QUALITY_MANAGER");
        assertThat(codes(vue(DocumentView.TO_VALIDATE))).containsExactly("PRO-QUA-001");
        assertThat(dans(documents::viewCounts).toValidate()).isEqualTo(1);
        // Le signataire 2 attend son tour ; une personne non désignée n'a rien à signer.
        connecte(tenant, second, "USER");
        assertThat(codes(vue(DocumentView.TO_VALIDATE))).isEmpty();
        connecte(tenant, UUID.randomUUID(), "QUALITY_MANAGER");
        assertThat(codes(vue(DocumentView.TO_VALIDATE))).isEmpty();

        connecte(tenant, valideur, "QUALITY_MANAGER");
        DocumentDto.VersionResponse signee = dans(() -> documents.approveVersion(d.id(), v1,
                new DocumentDto.ApprovalRequest(null, "Conforme")));
        assertThat(signee.status()).isEqualTo(VersionStatus.IN_REVIEW);
        assertThat(signee.signatories()).extracting(DocumentDto.SignatoryView::decision)
                .containsExactly(SignatoryDecision.APPROVED, SignatoryDecision.PENDING);

        // À son tour, le signataire 2 — un simple utilisateur : la désignation fait le droit.
        connecte(tenant, second, "USER");
        assertThat(codes(vue(DocumentView.TO_VALIDATE))).containsExactly("PRO-QUA-001");
        assertThat(dans(() -> documents.approveVersion(d.id(), v1, new DocumentDto.ApprovalRequest(null)))
                .status()).isEqualTo(VersionStatus.APPROVED);
        assertThat(codes(vue(DocumentView.TO_VALIDATE))).isEmpty();

        connecte(tenant, valideur, "QUALITY_MANAGER");
        dans(() -> documents.publishVersion(d.id(), v1));
        DocumentDto.DocumentResponse publie = dans(() -> documents.findById(d.id()));
        assertThat(publie.displayStatus()).isEqualTo(DisplayStatus.IN_FORCE);
        assertThat(publie.nextReviewDate()).isAfter(LocalDate.now().plusMonths(23));

        // Une nouvelle version en préparation : « En révision ».
        dans(() -> documents.createVersion(d.id(), new DocumentDto.CreateVersionRequest(null, null, "v2", null)));
        assertThat(dans(() -> documents.findById(d.id())).displayStatus()).isEqualTo(DisplayStatus.IN_REVISION);

        // Une revue dépassée l'emporte, et se retrouve dans la vue des revues à échéance.
        dans(() -> documents.updateDocument(d.id(), new DocumentDto.UpdateDocumentRequest(null, null, null, null,
                null, null, null, LocalDate.now().minusDays(1), null, null)));
        assertThat(dans(() -> documents.findById(d.id())).displayStatus()).isEqualTo(DisplayStatus.REVIEW_OVERDUE);
        assertThat(codes(new DocumentDto.ListFilter(DocumentView.REVIEW_DUE, null, null,
                DisplayStatus.REVIEW_OVERDUE, null, null, null))).containsExactly("PRO-QUA-001");
        assertThat(codes(new DocumentDto.ListFilter(DocumentView.ALL, null, null,
                DisplayStatus.IN_REVISION, null, null, null))).isEmpty();

        dans(() -> documents.archiveDocument(d.id()));
        assertThat(codes(vue(DocumentView.ARCHIVED))).containsExactly("PRO-QUA-001");
        assertThat(codes(vue(DocumentView.ALL))).isEmpty();
    }

    @Test
    void depotDedoublonneCompresseRemplaceEtPurge() {
        InMemoryObjectStorage mem = (InMemoryObjectStorage) storage;
        connecte(tenant, redacteur, "QUALITY_MANAGER");
        DocumentDto.DocumentResponse a = cree("FOR-QUA-007", "Fiche de NC", null, null);
        DocumentDto.DocumentResponse b = cree("FOR-QUA-008", "Fiche de NC bis", null, null);
        UUID va = a.versions().get(0).id();
        UUID vb = b.versions().get(0).id();

        // Un CSV répétitif se compresse ; le même fichier déposé deux fois n'est stocké qu'une fois.
        byte[] csv = "code;libelle;statut\n".repeat(2000).getBytes(StandardCharsets.UTF_8);
        int avant = mem.size();
        DocumentDto.VersionResponse ra = dans(() -> files.upload(a.id(), va, "text/csv", "nc.csv", csv));
        DocumentDto.VersionResponse rb = dans(() -> files.upload(b.id(), vb, "", "copie.csv", csv));
        assertThat(mem.size()).isEqualTo(avant + 1);
        assertThat(ra.contentHash()).isEqualTo(rb.contentHash()).isEqualTo(DocumentFiles.sha256(csv));
        assertThat(rb.file().fileName()).isEqualTo("copie.csv");
        DocumentDto.StorageUsage usage = dans(files::usage);
        assertThat(usage.files()).isEqualTo(1);
        assertThat(usage.logicalBytes()).isEqualTo(2L * csv.length);
        assertThat(usage.storedBytes()).isLessThan(csv.length / 5);

        DocumentDto.FileLink link = dans(() -> files.link(a.id(), va, true));
        assertThat(link.url()).contains("disposition=attachment").contains("filename=nc.csv");

        // Remplacer le fichier de B ne retire pas l'objet que A désigne encore.
        dans(() -> files.upload(b.id(), vb, "application/pdf", "fiche.pdf", PDF));
        assertThat(mem.size()).isEqualTo(avant + 2);
        // Remplacer celui de A le libère : plus aucune version ne le désigne.
        dans(() -> files.upload(a.id(), va, "application/pdf", "fiche.pdf", PDF));
        assertThat(mem.size()).isEqualTo(avant + 1);

        // Un exécutable renommé est refusé.
        assertThatThrownBy(() -> dans(() -> files.upload(a.id(), va, "application/pdf", "x.pdf",
                new byte[]{'M', 'Z', 0, 0})))
                .isInstanceOf(DocumentFileRejectedException.class);

        // Publication puis v2 : la v1 devient obsolète, la v2 hérite du même fichier.
        connecte(tenant, redacteur, "QUALITY_MANAGER");
        dans(() -> documents.submitForReview(a.id(), va, signataires(valideur)));
        connecte(tenant, valideur, "QUALITY_MANAGER");
        dans(() -> documents.approveVersion(a.id(), va, new DocumentDto.ApprovalRequest(null)));
        dans(() -> documents.publishVersion(a.id(), va));
        DocumentDto.VersionResponse v2 = dans(() -> documents.createVersion(a.id(),
                new DocumentDto.CreateVersionRequest(null, null, "v2", null)));
        assertThat(v2.file().fileName()).isEqualTo("fiche.pdf");
        dans(() -> documents.submitForReview(a.id(), v2.id(), signataires(redacteur)));
        connecte(tenant, redacteur, "QUALITY_MANAGER");
        dans(() -> documents.approveVersion(a.id(), v2.id(), new DocumentDto.ApprovalRequest(null)));
        dans(() -> documents.publishVersion(a.id(), v2.id()));

        // La v1 obsolète, vieillie au-delà de la conservation : son fichier est retiré,
        // mais l'objet partagé avec la v2 et B reste.
        dans(() -> {
            DocumentVersion v1 = versions.findById(va).orElseThrow();
            v1.setObsoletedAt(Instant.now().minusSeconds(3L * 366 * 24 * 3600));
            return versions.save(v1);
        });
        int purges = dans(() -> files.purgeExpired(tenant, 2));
        assertThat(purges).isEqualTo(1);
        DocumentDto.DocumentResponse apres = dans(() -> documents.findById(a.id()));
        DocumentDto.VersionResponse v1 = apres.versions().stream().filter(v -> v.id().equals(va)).findFirst()
                .orElseThrow();
        assertThat(v1.file().purged()).isTrue();
        assertThat(v1.contentHash()).isEqualTo(DocumentFiles.sha256(PDF));
        assertThat(mem.size()).isEqualTo(avant + 1);
        assertThatThrownBy(() -> dans(() -> files.link(a.id(), va, false)))
                .isInstanceOf(DocumentStateException.class);
        assertThat(dans(() -> files.purgeExpired(tenant, 2))).isZero();
    }
}
