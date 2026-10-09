package com.openlab.qualitos.quality.circuit.infrastructure;

import com.openlab.qualitos.quality.circuit.application.CircuitDto;
import com.openlab.qualitos.quality.circuit.application.CircuitService;
import com.openlab.qualitos.quality.circuit.domain.CircuitException;
import com.openlab.qualitos.quality.circuit.domain.CircuitSubject;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.docs.DocumentDto;
import com.openlab.qualitos.quality.docs.DocumentService;
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
 * la soumission d'un document qui ouvre le passage, les décisions qui se
 * relisent d'une transaction à l'autre, l'approbation finale, le refus et le
 * cloisonnement par client. Un dépôt simulé ne dirait rien d'une collection
 * de décisions mal rechargée.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
class CircuitIntegrationTest {

    @Autowired CircuitService circuits;
    @Autowired DocumentService documents;
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

    /** Un document en revue, rédigé par l'autrice. Rend [documentId, versionId]. */
    UUID[] documentEnRevue(String code) {
        connecte(tenant, autrice, "QUALITY_MANAGER");
        DocumentDto.DocumentResponse d = dans(() -> documents.createDocument(new DocumentDto.CreateDocumentRequest(
                code, "Procédure " + code, null, DocumentType.PROCEDURE, autrice, false, "contenu", null, "v1")));
        UUID version = d.versions().get(0).id();
        dans(() -> documents.submitForReview(d.id(), version));
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
    void unRefusRenvoieEnBrouillon_etUneNouvelleSoumissionRepartDeZero() {
        reglerDeuxEtapes();
        UUID[] ids = documentEnRevue("PRO-C3");

        connecte(tenant, alice, "QUALITY_MANAGER");
        DocumentDto.VersionResponse refusee = dans(() -> documents.rejectVersion(ids[0], ids[1],
                new DocumentDto.RejectionRequest("Section 4 incomplète")));
        assertThat(refusee.status()).isEqualTo(VersionStatus.DRAFT);
        assertThat(refusee.rejectionReason()).isEqualTo("Section 4 incomplète");
        UUID premier = dans(() -> circuits.run(CircuitSubject.DOCUMENT_VERSION, ids[1])).orElseThrow().id();

        connecte(tenant, autrice, "QUALITY_MANAGER");
        dans(() -> documents.submitForReview(ids[0], ids[1]));
        CircuitDto.RunView second = dans(() -> circuits.run(CircuitSubject.DOCUMENT_VERSION, ids[1])).orElseThrow();

        assertThat(second.id()).isNotEqualTo(premier);
        assertThat(second.status()).isEqualTo("IN_PROGRESS");
        assertThat(second.decisions()).isEmpty();
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
    void unRoleSansLeDroitDApprouver_nePeutPasPorterUneEtape() {
        connecte(tenant, admin, "ADMIN_TENANT");
        assertThatThrownBy(() -> dans(() -> circuits.replace(CircuitSubject.DOCUMENT_VERSION, List.of(
                new CircuitDto.StepView("Relecture", "USER", 1)))))
                .isInstanceOf(CircuitException.class)
                .hasMessageContaining("document.approve");
    }
}
