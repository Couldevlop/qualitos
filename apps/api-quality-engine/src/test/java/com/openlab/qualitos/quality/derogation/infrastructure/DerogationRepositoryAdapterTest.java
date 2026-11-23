package com.openlab.qualitos.quality.derogation.infrastructure;

import com.openlab.qualitos.quality.derogation.domain.Analysis;
import com.openlab.qualitos.quality.derogation.domain.CustomerDecision;
import com.openlab.qualitos.quality.derogation.domain.Derogation;
import com.openlab.qualitos.quality.derogation.domain.DerogationLanguage;
import com.openlab.qualitos.quality.derogation.domain.DerogationLine;
import com.openlab.qualitos.quality.derogation.domain.DerogationQuery;
import com.openlab.qualitos.quality.derogation.domain.DerogationRepository;
import com.openlab.qualitos.quality.derogation.domain.DerogationStatus;
import com.openlab.qualitos.quality.derogation.domain.Identification;
import com.openlab.qualitos.quality.derogation.domain.Nature;
import com.openlab.qualitos.quality.derogation.domain.SignatureTrace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le dépôt sur un vrai mapping JPA (contexte complet, H2 en mode PostgreSQL).
 *
 * <p>Le banc qui justifie ce fichier est
 * {@link #reecrireLesLignesDeuxFoisNeHeurtePasLaContrainteDUnicite()} : Hibernate
 * ordonne les INSERT <b>avant</b> les DELETE d'un même vidage. Réécrire les
 * lignes d'un dossier insérerait donc le nouveau rang 1 avant d'avoir supprimé
 * l'ancien, et heurterait {@code uq_derogation_lines_position}. Aucune doublure
 * en mémoire ne peut voir ce défaut — il n'existe que face à un moteur.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
class DerogationRepositoryAdapterTest {

    private static final UUID DEMANDEUR = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID MANAGER = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant T0 = Instant.parse("2026-11-22T10:00:00Z");

    @Autowired DerogationJpaRepository dossiersJpa;
    @Autowired DerogationLineJpaRepository lignesJpa;
    @Autowired TransactionTemplate transactions;

    private DerogationRepository depot;
    private UUID client;

    @BeforeEach
    void preparer() {
        depot = new DerogationRepositoryAdapter(dossiersJpa, lignesJpa);
        client = UUID.randomUUID();
    }

    @Test
    void unDossierSeRelitIdentiqueAvecSesLignes() {
        Derogation ecrit = transactions.execute(t -> depot.save(dossier("DER-2026-0001",
                List.of(new DerogationLine(1, "Cote 12,4", "12,9", new BigDecimal("4")),
                        new DerogationLine(2, "Ra 1,6", "Ra 2,1", null)))));

        Derogation relu = transactions.execute(t ->
                depot.findById(client, ecrit.getId()).orElseThrow());

        assertThat(relu.getReference()).isEqualTo("DER-2026-0001");
        assertThat(relu.getStatus()).isEqualTo(DerogationStatus.DRAFT);
        assertThat(relu.getIdentification().customerName()).isEqualTo("ACME Industrie");
        assertThat(relu.getIdentification().quantity())
                .isEqualByComparingTo(new BigDecimal("120"));
        assertThat(relu.getIdentification().requestDate())
                .isEqualTo(LocalDate.of(2026, 11, 22));
        assertThat(relu.getAnalysis().rootCause()).isEqualTo("Usure de l'outil");
        assertThat(relu.getCreatedByName()).isEqualTo("Paul Girard");
        assertThat(relu.getLines()).extracting(DerogationLine::specification)
                .containsExactly("Cote 12,4", "Ra 1,6");
        assertThat(relu.getLines()).extracting(DerogationLine::position)
                .containsExactly(1, 2);
        assertThat(relu.getLines().get(0).nonConformingQty())
                .isEqualByComparingTo(new BigDecimal("4"));
    }

    @Test
    void reecrireLesLignesDeuxFoisNeHeurtePasLaContrainteDUnicite() {
        // Hibernate ordonne les INSERT AVANT les DELETE : sans suppression
        // explicite suivie d'un vidage, le nouveau rang 1 arriverait avant que
        // l'ancien ne soit parti, et `uq_derogation_lines_position` leverait.
        Derogation ecrit = transactions.execute(t -> depot.save(dossier("DER-2026-0002",
                List.of(new DerogationLine(1, "Premiere", "a", null),
                        new DerogationLine(2, "Deuxieme", "b", null),
                        new DerogationLine(3, "Troisieme", "c", null)))));

        transactions.execute(t -> {
            Derogation d = depot.findById(client, ecrit.getId()).orElseThrow();
            d.edit(DEMANDEUR, identification(), Nature.parDefaut(),
                    List.of(new DerogationLine(1, "Remplacante", "z", null)),
                    "ok", analyse(), T0);
            return depot.save(d);
        });

        Derogation relu = transactions.execute(t ->
                depot.findById(client, ecrit.getId()).orElseThrow());
        assertThat(relu.getLines()).extracting(DerogationLine::specification)
                .containsExactly("Remplacante");
        assertThat(relu.getLines()).extracting(DerogationLine::position).containsExactly(1);
    }

    @Test
    void retirerToutesLesLignesLesRetireVraiment() {
        Derogation ecrit = transactions.execute(t -> depot.save(dossier("DER-2026-0003",
                List.of(new DerogationLine(1, "A supprimer", "x", null)))));

        transactions.execute(t -> {
            Derogation d = depot.findById(client, ecrit.getId()).orElseThrow();
            d.edit(DEMANDEUR, identification(), Nature.parDefaut(), List.of(),
                    "ok", analyse(), T0);
            return depot.save(d);
        });

        List<DerogationLine> restantes = transactions.execute(t ->
                depot.findById(client, ecrit.getId()).orElseThrow().getLines());
        assertThat(restantes).isEmpty();
    }

    @Test
    void unDossierDunAutreClientEstInvisible() {
        // La frontiere se verifie contre le moteur : le filtre est dans la
        // requete, pas dans un `if` cote Java.
        Derogation ecrit = transactions.execute(t ->
                depot.save(dossier("DER-2026-0004", List.of())));
        Optional<Derogation> vu = transactions.execute(t ->
                depot.findById(UUID.randomUUID(), ecrit.getId()));
        assertThat(vu).isEmpty();
    }

    @Test
    void laSignatureEtLesChampsDEnvoiTraversentLAllerRetour() {
        byte[] trace = png();
        Derogation ecrit = transactions.execute(t -> {
            Derogation d = dossier("DER-2026-0005",
                    List.of(new DerogationLine(1, "Cote", "12,9", null)));
            d.requestApproval(DEMANDEUR, MANAGER, "Nina Costa", T0);
            d.approve(MANAGER, "Nina Costa", SignatureTrace.of(trace), T0);
            d.markSentToCustomer(DEMANDEUR, "qualite@client.fr", "Lot urgent",
                    DerogationLanguage.EN, T0);
            d.recordCustomerAnswer(DEMANDEUR, CustomerDecision.ACCEPTED_WITH_CONDITIONS,
                    LocalDate.of(2026, 11, 25), T0);
            return depot.save(d);
        });

        Derogation relu = transactions.execute(t ->
                depot.findById(client, ecrit.getId()).orElseThrow());
        assertThat(relu.getStatus()).isEqualTo(DerogationStatus.ACCEPTED);
        assertThat(relu.getApproval().managerAccountId()).isEqualTo(MANAGER);
        assertThat(relu.getApproval().signedByName()).isEqualTo("Nina Costa");
        assertThat(relu.getApproval().signaturePng()).isEqualTo(trace);
        assertThat(relu.getDispatch().customerEmail()).isEqualTo("qualite@client.fr");
        assertThat(relu.getDispatch().comment()).isEqualTo("Lot urgent");
        assertThat(relu.getDispatch().language()).isEqualTo(DerogationLanguage.EN);
        assertThat(relu.getAnswer().decision())
                .isEqualTo(CustomerDecision.ACCEPTED_WITH_CONDITIONS);
        assertThat(relu.getAnswer().decidedOn()).isEqualTo(LocalDate.of(2026, 11, 25));
    }

    @Test
    void laRechercheFiltreEtPagineSurLeMoteur() {
        transactions.execute(t -> {
            depot.save(dossier("DER-2026-0010", List.of()));
            Derogation autre = dossier("DER-2026-0011", List.of());
            autre.edit(DEMANDEUR, new Identification("BETA SAS", null, null,
                            "P-9000 Couvercle", null, null, new BigDecimal("5"),
                            LocalDate.of(2026, 11, 22)),
                    Nature.parDefaut(), List.of(), "ok", analyse(), T0);
            depot.save(autre);
            return null;
        });

        DerogationRepository.Slice<Derogation> acme = transactions.execute(t ->
                depot.search(new DerogationQuery(client, null, null, "acme", null, null, 0, 20)));
        assertThat(acme.content()).extracting(Derogation::getReference)
                .containsExactly("DER-2026-0010");

        DerogationRepository.Slice<Derogation> piece = transactions.execute(t ->
                depot.search(new DerogationQuery(client, null, "P-9000", null, null, null, 0, 20)));
        assertThat(piece.content()).extracting(Derogation::getReference)
                .containsExactly("DER-2026-0011");

        DerogationRepository.Slice<Derogation> page = transactions.execute(t ->
                depot.search(new DerogationQuery(client, null, null, null, null, null, 0, 1)));
        assertThat(page.content()).hasSize(1);
        assertThat(page.total()).isEqualTo(2L);
    }

    @Test
    void lesJokersDuFiltreSontEchappes() {
        // « % » saisi dans un filtre ne doit pas tout ramener : sans
        // echappement, le filtre devient un joker et la restriction s'evapore.
        transactions.execute(t -> depot.save(dossier("DER-2026-0020", List.of())));
        DerogationRepository.Slice<Derogation> rien = transactions.execute(t ->
                depot.search(new DerogationQuery(client, null, null, "%", null, null, 0, 20)));
        assertThat(rien.content()).isEmpty();
    }

    @Test
    void lesValidationsEnAttenteNeRemontentQueCellesDuManager() {
        transactions.execute(t -> {
            Derogation d = dossier("DER-2026-0030", List.of());
            d.requestApproval(DEMANDEUR, MANAGER, "Nina", T0);
            depot.save(d);
            depot.save(dossier("DER-2026-0031", List.of()));
            return null;
        });

        long pourNina = transactions.execute(t -> depot.countPendingApprovals(client, MANAGER));
        long pourUnTiers = transactions.execute(t ->
                depot.countPendingApprovals(client, UUID.randomUUID()));
        assertThat(pourNina).isEqualTo(1L);
        assertThat(pourUnTiers).isZero();
        assertThat(transactions.execute(t ->
                depot.findPendingApprovals(client, MANAGER, 0, 20)).content())
                .extracting(Derogation::getReference).containsExactly("DER-2026-0030");
    }

    @Test
    void laVisibiliteRestreinteRendLeSienEtCeQuOnDoitSigner() {
        UUID tiers = UUID.randomUUID();
        transactions.execute(t -> {
            Derogation mien = dossier("DER-2026-0040", List.of());
            depot.save(mien);
            Derogation aSigner = Derogation.create(client, "DER-2026-0041", tiers,
                    "Autre", identification(), T0);
            aSigner.edit(tiers, identification(), Nature.parDefaut(), List.of(),
                    "ok", analyse(), T0);
            aSigner.requestApproval(tiers, DEMANDEUR, "Paul", T0);
            depot.save(aSigner);
            return null;
        });

        DerogationRepository.Slice<Derogation> miens = transactions.execute(t ->
                depot.search(new DerogationQuery(client, null, null, null, null,
                        DEMANDEUR, 0, 20)));
        assertThat(miens.content()).extracting(Derogation::getReference)
                .containsExactlyInAnyOrder("DER-2026-0040", "DER-2026-0041");

        DerogationRepository.Slice<Derogation> aucun = transactions.execute(t ->
                depot.search(new DerogationQuery(client, null, null, null, null,
                        UUID.randomUUID(), 0, 20)));
        assertThat(aucun.content()).isEmpty();
    }

    @Test
    void laReferenceSeCompteParPrefixeEtSonUniciteSeVerifie() {
        transactions.execute(t -> {
            depot.save(dossier("DER-2026-0050", List.of()));
            depot.save(dossier("DER-2026-0051", List.of()));
            depot.save(dossier("DER-2025-0001", List.of()));
            return null;
        });
        long de2026 = transactions.execute(t ->
                depot.countByReferencePrefix(client, "DER-2026-"));
        boolean prise = transactions.execute(t ->
                depot.existsByReference(client, "DER-2026-0050"));
        boolean libre = transactions.execute(t ->
                depot.existsByReference(client, "DER-2026-9999"));
        assertThat(de2026).isEqualTo(2L);
        assertThat(prise).isTrue();
        assertThat(libre).isFalse();
    }

    // ---- fabriques -----------------------------------------------------------

    private Derogation dossier(String reference, List<DerogationLine> lignes) {
        Derogation d = Derogation.create(client, reference, DEMANDEUR, "Paul Girard",
                identification(), T0);
        d.edit(DEMANDEUR, identification(), Nature.parDefaut(), lignes,
                "Pieces fonctionnelles", analyse(), T0);
        return d;
    }

    private static Identification identification() {
        return new Identification("ACME Industrie", "qualite@acme.fr", "CMD-7781",
                "P-4420 Bride acier", "C", "LOT-2026-11", new BigDecimal("120"),
                LocalDate.of(2026, 11, 22));
    }

    private static Analysis analyse() {
        return new Analysis("Usure de l'outil", "Lot trie a 100 %",
                "Remplacement de l'outil", "8D-2026-0042", "Rapport de mesures");
    }

    private static byte[] png() {
        BufferedImage image = new BufferedImage(40, 15, BufferedImage.TYPE_INT_ARGB);
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
