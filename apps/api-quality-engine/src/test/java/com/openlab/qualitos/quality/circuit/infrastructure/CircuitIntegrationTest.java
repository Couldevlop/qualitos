package com.openlab.qualitos.quality.circuit.infrastructure;

import com.openlab.qualitos.quality.circuit.application.CircuitDto;
import com.openlab.qualitos.quality.circuit.application.CircuitService;
import com.openlab.qualitos.quality.circuit.domain.CircuitException;
import com.openlab.qualitos.quality.circuit.domain.CircuitSubject;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.docs.DocumentBlob;
import com.openlab.qualitos.quality.docs.DocumentBlobRepository;
import com.openlab.qualitos.quality.docs.DocumentDto;
import com.openlab.qualitos.quality.docs.DocumentService;
import com.openlab.qualitos.quality.docs.DocumentVersion;
import com.openlab.qualitos.quality.docs.DocumentVersionRepository;
import com.openlab.qualitos.quality.docs.DocumentType;
import com.openlab.qualitos.quality.docs.VersionStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Un circuit de bout en bout sur le mapping JPA réel : le réglage du client,
 * le passage ouvert sur une version en revue, les décisions qui se relisent
 * d'une transaction à l'autre, l'approbation finale, le refus et le
 * cloisonnement par client. Un dépôt simulé ne dirait rien d'une collection
 * de décisions mal rechargée.
 *
 * <p>Depuis l'ADR 0099, la GED soumet avec des signataires désignés et n'ouvre
 * plus de passage. Les versions déjà en revue sous un circuit par rôles le
 * terminent : c'est ce que ces scénarios rejouent, en ouvrant le passage comme
 * le faisait la soumission d'avant.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
class CircuitIntegrationTest {

    @Autowired CircuitService circuits;
    @Autowired DocumentService documents;
    @Autowired DocumentVersionRepository versions;
    @Autowired DocumentBlobRepository blobs;
    @Autowired TransactionTemplate tx;

    final UUID tenant = UUID.randomUUID();
    final UUID autre = UUID.randomUUID();
    final UUID admin = UUID.randomUUID();
    final UUID autrice = UUID.randomUUID();
    final UUID alice = UUID.randomUUID();
    final UUID bob = UUID.randomUUID();
    final UUID chloe = UUID.randomUUID();

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

    /**
     * Un document en revue sous le circuit d'avant l'ADR 0099, rédigé par
     * l'autrice : la version passe en revue et son passage s'ouvre, s'il y a un
     * circuit réglé. Rend [documentId, versionId].
     */
    UUID[] documentEnRevue(String code) {
        connecte(tenant, autrice, "QUALITY_MANAGER");
        DocumentDto.DocumentResponse d = dans(() -> documents.createDocument(new DocumentDto.CreateDocumentRequest(
                code, "Procédure " + code, null, DocumentType.PROCEDURE, autrice, false, "contenu", null, "v1")));
        UUID version = d.versions().get(0).id();
        dans(() -> {
            DocumentVersion v = versions.findById(version).orElseThrow();
            v.setStatus(VersionStatus.IN_REVIEW);
            versions.save(v);
            return circuits.start(CircuitSubject.DOCUMENT_VERSION, version, autrice);
        });
        return new UUID[] {d.id(), version};
    }

    void reglerDeuxEtapes() {
        connecte(tenant, admin, "ADMIN_TENANT");
        dans(() -> circuits.replace(CircuitSubject.DOCUMENT_VERSION, List.of(
                new CircuitDto.StepView("Relecture", "QUALITY_MANAGER", 2),
                new CircuitDto.StepView("Signature", "QUALITY_DIRECTOR", 1))));
    }

    @Test
    void unDocumentFranchitLesEtapes_puisEstApprouve() {
        reglerDeuxEtapes();
        UUID[] ids = documentEnRevue("PRO-C1");

        connecte(tenant, alice, "QUALITY_MANAGER");
        DocumentDto.VersionResponse apresAlice = dans(() -> documents.approveVersion(ids[0], ids[1],
                new DocumentDto.ApprovalRequest(null, "RAS")));
        assertThat(apresAlice.status()).isEqualTo(VersionStatus.IN_REVIEW);

        connecte(tenant, bob, "QUALITY_MANAGER");
        dans(() -> documents.approveVersion(ids[0], ids[1], new DocumentDto.ApprovalRequest(null)));
        CircuitDto.RunView etape2 = dans(() -> circuits.run(CircuitSubject.DOCUMENT_VERSION, ids[1])).orElseThrow();
        assertThat(etape2.currentStep()).isEqualTo(1);
        assertThat(etape2.decisions()).extracting(CircuitDto.DecisionView::actorId).containsExactly(alice, bob);
        assertThat(etape2.decisions().get(0).comment()).isEqualTo("RAS");

        connecte(tenant, chloe, "QUALITY_DIRECTOR");
        DocumentDto.VersionResponse finale = dans(() -> documents.approveVersion(ids[0], ids[1],
                new DocumentDto.ApprovalRequest(null)));

        assertThat(finale.status()).isEqualTo(VersionStatus.APPROVED);
        assertThat(finale.approvedBy()).isEqualTo(chloe);
        assertThat(dans(() -> circuits.run(CircuitSubject.DOCUMENT_VERSION, ids[1])).orElseThrow().status())
                .isEqualTo("APPROVED");
    }

    @Test
    void laDirectionNePeutPasSauterLaRelecture() {
        reglerDeuxEtapes();
        UUID[] ids = documentEnRevue("PRO-C2");

        connecte(tenant, chloe, "QUALITY_DIRECTOR");
        assertThatThrownBy(() -> dans(() -> documents.approveVersion(ids[0], ids[1],
                new DocumentDto.ApprovalRequest(null))))
                .isInstanceOf(CircuitException.class)
                .extracting(e -> ((CircuitException) e).getReason())
                .isEqualTo(CircuitException.Reason.NOT_YOUR_STEP);
    }

    @Test
    void unRefusRenvoieEnBrouillon_etLaNouvelleSoumissionPasseParLesSignataires() {
        reglerDeuxEtapes();
        UUID[] ids = documentEnRevue("PRO-C3");

        connecte(tenant, alice, "QUALITY_MANAGER");
        DocumentDto.VersionResponse refusee = dans(() -> documents.rejectVersion(ids[0], ids[1],
                new DocumentDto.RejectionRequest("Section 4 incomplète")));
        assertThat(refusee.status()).isEqualTo(VersionStatus.DRAFT);
        assertThat(refusee.rejectionReason()).isEqualTo("Section 4 incomplète");
        UUID premier = dans(() -> circuits.run(CircuitSubject.DOCUMENT_VERSION, ids[1])).orElseThrow().id();

        // La version corrigée repart avec ses signataires : aucun nouveau passage par rôles.
        connecte(tenant, autrice, "QUALITY_MANAGER");
        dans(() -> {
            DocumentVersion v = versions.findById(ids[1]).orElseThrow();
            DocumentBlob b = new DocumentBlob();
            b.setTenantId(tenant);
            b.setSha256("a".repeat(64));
            b.setObjectKey("t/" + UUID.randomUUID());
            b.setContentType("application/pdf");
            b.setOriginalSize(8);
            b.setStoredSize(8);
            v.setBlob(blobs.save(b));
            return versions.save(v);
        });
        DocumentDto.VersionResponse resoumise = dans(() -> documents.submitForReview(ids[0], ids[1],
                new DocumentDto.SubmitRequest(List.of(new DocumentDto.SignatoryRequest(alice, "Alice")))));
        CircuitDto.RunView dernier = dans(() -> circuits.run(CircuitSubject.DOCUMENT_VERSION, ids[1])).orElseThrow();

        assertThat(dernier.id()).isEqualTo(premier);
        assertThat(dernier.status()).isEqualTo("REJECTED");
        assertThat(resoumise.signatories()).extracting(DocumentDto.SignatoryView::userId).containsExactly(alice);

        connecte(tenant, alice, "QUALITY_MANAGER");
        assertThat(dans(() -> documents.approveVersion(ids[0], ids[1], new DocumentDto.ApprovalRequest(null)))
                .status()).isEqualTo(VersionStatus.APPROVED);
    }

    @Test
    void sansCircuit_uneApprobationSuffit_etLeCircuitDunAutreClientNeComptePas() {
        connecte(autre, admin, "ADMIN_TENANT");
        dans(() -> circuits.replace(CircuitSubject.DOCUMENT_VERSION, List.of(
                new CircuitDto.StepView("Relecture", "QUALITY_MANAGER", 3))));
        UUID[] ids = documentEnRevue("PRO-C4");

        connecte(tenant, alice, "QUALITY_MANAGER");
        DocumentDto.VersionResponse r = dans(() -> documents.approveVersion(ids[0], ids[1],
                new DocumentDto.ApprovalRequest(null)));

        assertThat(r.status()).isEqualTo(VersionStatus.APPROVED);
        assertThat(dans(() -> circuits.run(CircuitSubject.DOCUMENT_VERSION, ids[1]))).isEmpty();
    }

    @Test
    void unCircuitSuspendu_gardeSesEtapes_etUneApprobationSuffit() {
        connecte(tenant, admin, "ADMIN_TENANT");
        dans(() -> circuits.replace(CircuitSubject.DOCUMENT_VERSION, List.of(
                new CircuitDto.StepView("Relecture", "QUALITY_MANAGER", 2)), false));
        CircuitDto.CircuitView relu = dans(() -> circuits.circuit(CircuitSubject.DOCUMENT_VERSION));
        assertThat(relu.enabled()).isFalse();
        assertThat(relu.steps()).hasSize(1);

        UUID[] ids = documentEnRevue("PRO-C5");
        connecte(tenant, alice, "QUALITY_MANAGER");
        DocumentDto.VersionResponse r = dans(() -> documents.approveVersion(ids[0], ids[1],
                new DocumentDto.ApprovalRequest(null)));

        assertThat(r.status()).isEqualTo(VersionStatus.APPROVED);
        assertThat(dans(() -> circuits.run(CircuitSubject.DOCUMENT_VERSION, ids[1]))).isEmpty();

        // Réactivé, il reprend ses étapes pour les soumissions suivantes.
        connecte(tenant, admin, "ADMIN_TENANT");
        dans(() -> circuits.replace(CircuitSubject.DOCUMENT_VERSION, relu.steps(), true));
        assertThat(dans(() -> circuits.circuit(CircuitSubject.DOCUMENT_VERSION)).enabled()).isTrue();
    }

    @Test
    void unRoleSansLeDroitDApprouver_nePeutPasPorterUneEtape() {
        connecte(tenant, admin, "ADMIN_TENANT");
        assertThatThrownBy(() -> dans(() -> circuits.replace(CircuitSubject.DOCUMENT_VERSION, List.of(
                new CircuitDto.StepView("Relecture", "USER", 1)))))
                .isInstanceOf(CircuitException.class)
                .hasMessageContaining("document.approve");
    }
}
