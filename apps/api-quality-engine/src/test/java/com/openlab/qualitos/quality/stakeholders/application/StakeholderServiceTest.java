package com.openlab.qualitos.quality.stakeholders.application;

import com.openlab.qualitos.quality.stakeholders.domain.ImpactLevel;
import com.openlab.qualitos.quality.stakeholders.domain.MandatoryNature;
import com.openlab.qualitos.quality.stakeholders.domain.Person;
import com.openlab.qualitos.quality.stakeholders.domain.Relevance;
import com.openlab.qualitos.quality.stakeholders.domain.RequirementLink;
import com.openlab.qualitos.quality.stakeholders.domain.RequirementStatus;
import com.openlab.qualitos.quality.stakeholders.domain.Stakeholder;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderCategory;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderNotFoundException;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderRepository;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderRequirement;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderSide;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le cas d'usage du registre : l'ordre de lecture, les dénombrements, la
 * matrice, l'ouverture d'une CAPA et la frontière que l'IA ne franchit pas.
 */
class StakeholderServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ACTEUR = UUID.randomUUID();
    private static final Instant MAINTENANT = Instant.parse("2026-11-04T09:00:00Z");
    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 4);

    private FakeRepository repository;
    private FakeCapa capa;
    private FakeAssistant assistant;
    private StakeholderService service;

    @BeforeEach
    void setUp() {
        repository = new FakeRepository();
        capa = new FakeCapa();
        assistant = new FakeAssistant();
        service = new StakeholderService(repository, new FakeContext(), capa, assistant,
                Clock.fixed(MAINTENANT, ZoneOffset.UTC));
    }

    // ---------- inscription et lecture ----------

    @Test
    void inscrireDemandeTroisChampsEtRienDePlus() {
        StakeholderDto.Sheet sheet = service.create("Client A", StakeholderCategory.CUSTOMERS,
                StakeholderSide.EXTERNAL);

        assertThat(sheet.name()).isEqualTo("Client A");
        assertThat(sheet.relevance()).isEqualTo(Relevance.TO_DETERMINE);
        assertThat(sheet.criticality()).isEqualTo(ImpactLevel.LOW);
        assertThat(sheet.requirements()).isEmpty();
    }

    @Test
    void unDoublonDansLaMemeFamilleEstRefuse() {
        service.create("Client A", StakeholderCategory.CUSTOMERS, StakeholderSide.EXTERNAL);

        assertThatThrownBy(() -> service.create("client a", StakeholderCategory.CUSTOMERS,
                StakeholderSide.EXTERNAL))
                .isInstanceOf(StakeholderValidationException.class)
                .hasMessageContaining("already in the register");

        // Le même nom dans une AUTRE famille n'est pas un doublon : « Dupont »
        // peut être à la fois client et fournisseur.
        assertThat(service.create("Client A", StakeholderCategory.SUPPLIERS, StakeholderSide.EXTERNAL))
                .isNotNull();
    }

    @Test
    void leRegistreSeLitDuPlusCritiqueAuMoins() {
        // L'ordre porte du sens : deux écrans ne doivent pas en proposer deux.
        UUID faible = service.create("Banque C", StakeholderCategory.FINANCIAL_PARTNERS,
                StakeholderSide.EXTERNAL).id();
        UUID eleve = service.create("Autorité", StakeholderCategory.REGULATORS,
                StakeholderSide.EXTERNAL).id();
        UUID moyen = service.create("Fournisseur B", StakeholderCategory.SUPPLIERS,
                StakeholderSide.EXTERNAL).id();
        service.rate(Map.of(
                eleve, new StakeholderService.Rating(ImpactLevel.LOW, ImpactLevel.HIGH),
                moyen, new StakeholderService.Rating(ImpactLevel.MEDIUM, ImpactLevel.LOW)));

        List<StakeholderDto.Row> rows = service.list(null, null);

        assertThat(rows).extracting(StakeholderDto.Row::id).containsExactly(eleve, moyen, faible);
        assertThat(rows.get(0).criticality()).isEqualTo(ImpactLevel.HIGH);
    }

    @Test
    void leRegistreSeFiltreParCoteEtParNom() {
        service.create("Client A", StakeholderCategory.CUSTOMERS, StakeholderSide.EXTERNAL);
        service.create("Collaborateurs", StakeholderCategory.EMPLOYEES, StakeholderSide.INTERNAL);

        assertThat(service.list(StakeholderSide.INTERNAL, null)).hasSize(1);
        assertThat(service.list(null, "client")).extracting(StakeholderDto.Row::name)
                .containsExactly("Client A");
        assertThat(service.list(null, "  ")).hasSize(2);
        assertThat(service.list(null, "introuvable")).isEmpty();
    }

    @Test
    void uneFicheInconnueNEstPasRendue() {
        assertThatThrownBy(() -> service.get(UUID.randomUUID()))
                .isInstanceOf(StakeholderNotFoundException.class);
    }

    @Test
    void lesTuilesComptentCeQuUneRevueDeDirectionRegarde() {
        UUID critique = service.create("Autorité", StakeholderCategory.REGULATORS,
                StakeholderSide.EXTERNAL).id();
        // La fiche s'enregistre d'un BLOC : la cotation part avec elle. La coter
        // avant d'enregistrer la date de revue l'effacerait — c'est voulu, et
        // c'est pourquoi la matrice a sa propre route.
        service.update(critique, update().withNextReview(AUJOURDHUI.minusDays(3)).build());
        service.rate(Map.of(critique, new StakeholderService.Rating(ImpactLevel.HIGH, ImpactLevel.HIGH)));
        UUID interne = service.create("Collaborateurs", StakeholderCategory.EMPLOYEES,
                StakeholderSide.INTERNAL).id();
        service.addRequirement(interne, exigence("Être informé des changements"));
        service.setRequirementStatus(interne,
                service.get(interne).requirements().get(0).id(), RequirementStatus.TO_REASSESS);

        StakeholderDto.Statistics stats = service.statistics();

        assertThat(stats.total()).isEqualTo(2);
        assertThat(stats.internal()).isEqualTo(1);
        assertThat(stats.external()).isEqualTo(1);
        assertThat(stats.critical()).isEqualTo(1);
        assertThat(stats.overdue()).isEqualTo(1);
        assertThat(stats.toDetermine()).isEqualTo(2);
        assertThat(stats.requirementsToReassess()).isEqualTo(1);
    }

    @Test
    void laFicheSEnregistreDUnBlocEtLaCotationPartAvecElle() {
        // Conséquence assumée du « d'un bloc » : un champ absent du corps est
        // un champ EFFACÉ, pas un champ inchangé. L'écran envoie toujours la
        // fiche entière ; la matrice, qui ne cote que, a sa propre route.
        UUID id = service.create("Client A", StakeholderCategory.CUSTOMERS,
                StakeholderSide.EXTERNAL).id();
        service.rate(Map.of(id, new StakeholderService.Rating(ImpactLevel.HIGH, ImpactLevel.HIGH)));

        StakeholderDto.Sheet sheet = service.update(id, update().build());

        assertThat(sheet.criticality()).isEqualTo(ImpactLevel.LOW);
    }

    // ---------- enregistrement de la fiche ----------

    @Test
    void laFicheSEnregistreDUnBloc() {
        UUID id = service.create("Client A", StakeholderCategory.CUSTOMERS,
                StakeholderSide.EXTERNAL).id();
        Person pilote = new Person(ACTEUR, "Alice Martin");

        StakeholderDto.Sheet sheet = service.update(id, new StakeholderService.Update(
                "Client A", StakeholderCategory.CUSTOMERS, StakeholderSide.EXTERNAL,
                "Donneur d'ordre aéronautique", "contact@client-a.example", "Tous les sites",
                "Pièces conformes", "Contrat cadre C-2026-114", Relevance.RELEVANT,
                "40 % du chiffre d'affaires", "Perte du client en cas de NC récurrente",
                ImpactLevel.HIGH, ImpactLevel.MEDIUM, pilote,
                "Revue de contrat annuelle", "Taux de livraison à l'heure", "Contrat C-2026-114",
                "ACT-14", AUJOURDHUI.plusMonths(6)));

        assertThat(sheet.description()).isEqualTo("Donneur d'ordre aéronautique");
        assertThat(sheet.criticality()).isEqualTo(ImpactLevel.HIGH);
        assertThat(sheet.owner()).isEqualTo(pilote);
        assertThat(sheet.reviewOverdue()).isFalse();
    }

    @Test
    void renommerVersUnNomDejaPrisEstRefuse() {
        service.create("Client A", StakeholderCategory.CUSTOMERS, StakeholderSide.EXTERNAL);
        UUID autre = service.create("Client B", StakeholderCategory.CUSTOMERS,
                StakeholderSide.EXTERNAL).id();

        assertThatThrownBy(() -> service.update(autre, update().withName("Client A").build()))
                .isInstanceOf(StakeholderValidationException.class);
    }

    @Test
    void seRenommerSoiMemeNEstPasUnDoublon() {
        UUID id = service.create("Client A", StakeholderCategory.CUSTOMERS,
                StakeholderSide.EXTERNAL).id();

        assertThat(service.update(id, update().withName("Client A").build()).name())
                .isEqualTo("Client A");
    }

    @Test
    void laRevueSeMarqueALaDateDuJour() {
        UUID id = service.create("Client A", StakeholderCategory.CUSTOMERS,
                StakeholderSide.EXTERNAL).id();

        StakeholderDto.Sheet sheet = service.markReviewed(id);

        assertThat(sheet.lastReviewedOn()).isEqualTo(AUJOURDHUI);
    }

    // ---------- suppression ----------

    @Test
    void uneFicheSansExigenceSeRetire() {
        UUID id = service.create("Banque C", StakeholderCategory.FINANCIAL_PARTNERS,
                StakeholderSide.EXTERNAL).id();

        service.delete(id);

        assertThat(service.list(null, null)).isEmpty();
    }

    @Test
    void uneFichePortantUneExigenceNeSeSupprimePas() {
        // Une exigence est une preuve de §4.2, et une preuve effacée ne se
        // présente pas à l'auditeur.
        UUID id = service.create("Client A", StakeholderCategory.CUSTOMERS,
                StakeholderSide.EXTERNAL).id();
        service.addRequirement(id, exigence("Respecter le taux de service"));

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(StakeholderValidationException.class)
                .hasMessageContaining("not relevant");
    }

    // ---------- matrice ----------

    @Test
    void laMatriceCoteUneLigneOuPlusieurs() {
        UUID a = service.create("Client A", StakeholderCategory.CUSTOMERS,
                StakeholderSide.EXTERNAL).id();
        UUID b = service.create("Fournisseur B", StakeholderCategory.SUPPLIERS,
                StakeholderSide.EXTERNAL).id();

        List<StakeholderDto.Row> rows = service.rate(new LinkedHashMap<>(Map.of(
                a, new StakeholderService.Rating(ImpactLevel.HIGH, ImpactLevel.LOW),
                b, new StakeholderService.Rating(ImpactLevel.LOW, ImpactLevel.MEDIUM))));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).id()).isEqualTo(a);
        assertThat(rows.get(0).criticality()).isEqualTo(ImpactLevel.HIGH);
    }

    @Test
    void laMatriceSansLigneEtUneLigneInconnueSontRefusees() {
        assertThatThrownBy(() -> service.rate(Map.of()))
                .isInstanceOf(StakeholderValidationException.class);
        assertThatThrownBy(() -> service.rate(null))
                .isInstanceOf(StakeholderValidationException.class);
        assertThatThrownBy(() -> service.rate(Map.of(UUID.randomUUID(),
                new StakeholderService.Rating(ImpactLevel.LOW, ImpactLevel.LOW))))
                .isInstanceOf(StakeholderNotFoundException.class);
    }

    // ---------- exigences ----------

    @Test
    void uneExigenceSAjouteEtSeSuit() {
        UUID id = service.create("Client A", StakeholderCategory.CUSTOMERS,
                StakeholderSide.EXTERNAL).id();

        StakeholderDto.Requirement added = service.addRequirement(id,
                exigence("Respecter le taux de service"));

        assertThat(added.id()).isNotNull();
        assertThat(service.get(id).requirements()).hasSize(1);

        UUID cible = UUID.randomUUID();
        assertThat(service.linkRequirement(id, added.id(), RequirementLink.AUDIT, cible).auditId())
                .isEqualTo(cible);
        assertThat(service.setRequirementStatus(id, added.id(), RequirementStatus.CONTROLLED).status())
                .isEqualTo(RequirementStatus.CONTROLLED);

        service.removeRequirement(id, added.id());
        assertThat(service.get(id).requirements()).isEmpty();
    }

    @Test
    void ouvrirUneCapaRattacheLeDossierEtFaitReevaluer() {
        UUID id = service.create("Client A", StakeholderCategory.CUSTOMERS,
                StakeholderSide.EXTERNAL).id();
        UUID rid = service.addRequirement(id, exigence("Respecter le taux de service")).id();
        service.setRequirementStatus(id, rid, RequirementStatus.CONTROLLED);

        StakeholderDto.Requirement updated = service.openCapa(id, rid, "Remettre le taux de service",
                ACTEUR, "Alice Martin", AUJOURDHUI.plusDays(30));

        assertThat(updated.capaId()).isNotNull();
        assertThat(updated.status()).isEqualTo(RequirementStatus.TO_REASSESS);
        // La référence dit de qui et de quelle exigence il s'agit : c'est ce
        // qu'on lira dans le registre CAPA six mois plus tard.
        assertThat(capa.lastReference).isEqualTo("Client A / Respecter le taux de service");
        assertThat(capa.lastDescription).contains("Client A").contains("Contrat C-2026-114");
    }

    @Test
    void uneSecondeCapaSurLaMemeExigenceEstRefusee() {
        UUID id = service.create("Client A", StakeholderCategory.CUSTOMERS,
                StakeholderSide.EXTERNAL).id();
        UUID rid = service.addRequirement(id, exigence("Respecter le taux de service")).id();
        service.openCapa(id, rid, "Remettre le taux", ACTEUR, "Alice", AUJOURDHUI.plusDays(30));

        assertThatThrownBy(() -> service.openCapa(id, rid, "Encore", ACTEUR, "Alice",
                AUJOURDHUI.plusDays(30)))
                .isInstanceOf(StakeholderValidationException.class)
                .hasMessageContaining("already has a CAPA");
    }

    @Test
    void uneCapaSurUneExigenceInconnueEstRefusee() {
        UUID id = service.create("Client A", StakeholderCategory.CUSTOMERS,
                StakeholderSide.EXTERNAL).id();

        assertThatThrownBy(() -> service.openCapa(id, UUID.randomUUID(), "Titre", ACTEUR, "Alice",
                AUJOURDHUI.plusDays(30)))
                .isInstanceOf(StakeholderValidationException.class)
                .hasMessageContaining("Unknown requirement");
    }

    // ---------- assistant IA : il propose, il n'écrit pas ----------

    @Test
    void lAssistantProposeSansRienInscrire() {
        List<StakeholderPorts.SuggestedStakeholder> proposees = service.suggest("Aéronautique",
                "PME, 80 salariés");

        assertThat(proposees).hasSize(1);
        // Le registre n'a pas bougé : l'inscription est un appel distinct.
        assertThat(service.list(null, null)).isEmpty();
    }

    @Test
    void unSecteurManquantRendLaSuggestionInutile() {
        assertThatThrownBy(() -> service.suggest("  ", "PME"))
                .isInstanceOf(StakeholderValidationException.class)
                .hasMessageContaining("sector");
    }

    @Test
    void unDocumentTropCourtNEstPasAnalyse() {
        // Sous 40 caractères, il n'y a rien à lire : l'IA inventerait.
        assertThatThrownBy(() -> service.extract("contrat.pdf", "Trop court."))
                .isInstanceOf(StakeholderValidationException.class)
                .hasMessageContaining("too short");
        assertThatThrownBy(() -> service.changes("contrat.pdf", longText(), "court"))
                .isInstanceOf(StakeholderValidationException.class);
        assertThatThrownBy(() -> service.changes("contrat.pdf", "court", longText()))
                .isInstanceOf(StakeholderValidationException.class);
    }

    @Test
    void lesExigencesExtraitesNeSontQueProposees() {
        UUID id = service.create("Client A", StakeholderCategory.CUSTOMERS,
                StakeholderSide.EXTERNAL).id();

        List<StakeholderPorts.ExtractedRequirement> extraites = service.extract("contrat.pdf",
                longText());

        assertThat(extraites).hasSize(1);
        assertThat(service.get(id).requirements()).isEmpty();
        assertThat(assistant.lastDocumentName).isEqualTo("contrat.pdf");
    }

    @Test
    void unDocumentSansNomSAnalyseQuandMeme() {
        service.extract(null, longText());
        assertThat(assistant.lastDocumentName).isEqualTo("document");

        service.changes(null, longText(), longText());
        assertThat(assistant.lastDocumentName).isEqualTo("document");
    }

    @Test
    void lesChangementsDetectesNeTouchentPasAuRegistre() {
        UUID id = service.create("Client A", StakeholderCategory.CUSTOMERS,
                StakeholderSide.EXTERNAL).id();
        UUID rid = service.addRequirement(id, exigence("Respecter le taux de service")).id();
        service.setRequirementStatus(id, rid, RequirementStatus.CONTROLLED);

        assertThat(service.changes("contrat.pdf", longText(), longText())).hasSize(1);

        // Rien n'a bougé : c'est l'humain qui désigne ce que le changement remet
        // en cause.
        assertThat(service.get(id).requirements().get(0).status())
                .isEqualTo(RequirementStatus.CONTROLLED);
    }

    @Test
    void lHumainDesigneLesExigencesQueLeChangementRemetEnCause() {
        UUID id = service.create("Client A", StakeholderCategory.CUSTOMERS,
                StakeholderSide.EXTERNAL).id();
        UUID un = service.addRequirement(id, exigence("Respecter le taux de service")).id();
        UUID deux = service.addRequirement(id, exigence("Tracer les lots")).id();
        service.setRequirementStatus(id, un, RequirementStatus.CONTROLLED);
        service.setRequirementStatus(id, deux, RequirementStatus.CONTROLLED);

        List<StakeholderDto.Requirement> updated = service.markToReassess(id, List.of(un));

        assertThat(updated).singleElement().extracting(StakeholderDto.Requirement::status)
                .isEqualTo(RequirementStatus.TO_REASSESS);
        // La seconde n'a pas bougé : seules celles qu'on a nommées changent.
        assertThat(service.get(id).requirements().stream()
                .filter(r -> r.id().equals(deux)).findFirst().orElseThrow().status())
                .isEqualTo(RequirementStatus.CONTROLLED);
    }

    @Test
    void uneReevaluationSansExigenceNommeeEstRefusee() {
        UUID id = service.create("Client A", StakeholderCategory.CUSTOMERS,
                StakeholderSide.EXTERNAL).id();

        assertThatThrownBy(() -> service.markToReassess(id, List.of()))
                .isInstanceOf(StakeholderValidationException.class);
        assertThatThrownBy(() -> service.markToReassess(id, null))
                .isInstanceOf(StakeholderValidationException.class);
    }

    // ---------- aides ----------

    private static String longText() {
        return "Les pièces sont livrées avec un certificat matière de type 3.1 à chaque livraison.";
    }

    private static StakeholderRequirement exigence(String libelle) {
        return new StakeholderRequirement(null, "Livrer à l'heure", libelle, "Contrat C-2026-114",
                MandatoryNature.CONTRACTUAL, "Retard de livraison", ImpactLevel.MEDIUM,
                "Revue de contrat", "Taux de livraison à l'heure", "Directeur de production",
                "Tableaux de bord", RequirementStatus.APPLICABLE, null, null, null, null, null);
    }

    /** Une mise à jour minimale, modifiable par petites touches. */
    private static Builder update() {
        return new Builder();
    }

    /** De quoi écrire une mise à jour sans répéter dix-neuf nuls. */
    private static final class Builder {
        private String name;
        private LocalDate nextReviewOn;

        Builder withName(String value) {
            this.name = value;
            return this;
        }

        Builder withNextReview(LocalDate value) {
            this.nextReviewOn = value;
            return this;
        }

        StakeholderService.Update build() {
            return new StakeholderService.Update(name, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, nextReviewOn);
        }
    }

    // --- doublures ---

    private static final class FakeContext implements StakeholderPorts.Context {
        @Override public UUID requireTenantId() { return TENANT; }

        @Override public UUID requireActorId() { return ACTEUR; }

        @Override public String actorName() { return "Alice Martin"; }
    }

    private static final class FakeCapa implements StakeholderPorts.CapaGateway {
        private String lastReference;
        private String lastDescription;

        @Override
        public UUID open(String reference, String title, String description, UUID ownerId,
                         String ownerName, LocalDate dueDate) {
            this.lastReference = reference;
            this.lastDescription = description;
            return UUID.randomUUID();
        }
    }

    private static final class FakeAssistant implements StakeholderPorts.Assistant {
        private String lastDocumentName;

        @Override
        public List<StakeholderPorts.SuggestedStakeholder> suggestStakeholders(String sector,
                                                                               String characteristics) {
            return List.of(new StakeholderPorts.SuggestedStakeholder("Donneurs d'ordre",
                    StakeholderCategory.CUSTOMERS, "Secteur " + sector));
        }

        @Override
        public List<StakeholderPorts.ExtractedRequirement> extractRequirements(String documentName,
                                                                                String documentText) {
            this.lastDocumentName = documentName;
            return List.of(new StakeholderPorts.ExtractedRequirement("citation", "art. 7.2",
                    "Fournir un certificat matière 3.1", MandatoryNature.CONTRACTUAL,
                    "Contrôle documentaire à réception"));
        }

        @Override
        public List<StakeholderPorts.DetectedChange> detectChanges(String documentName,
                                                                    String previousText,
                                                                    String currentText) {
            this.lastDocumentName = documentName;
            return List.of(new StakeholderPorts.DetectedChange("Taux de service porté à 98 %",
                    "Art. 9.1 modifié", "Avenant n° 2", ImpactLevel.HIGH));
        }
    }

    /** Un dépôt en mémoire : les règles testées sont celles du service. */
    private static final class FakeRepository implements StakeholderRepository {

        private final Map<UUID, Stakeholder> parId = new HashMap<>();

        @Override
        public Stakeholder save(Stakeholder stakeholder) {
            parId.put(stakeholder.id(), stakeholder);
            return stakeholder;
        }

        @Override
        public Optional<Stakeholder> findByIdAndTenant(UUID id, UUID tenantId) {
            return Optional.ofNullable(parId.get(id))
                    .filter(s -> s.tenantId().equals(tenantId));
        }

        @Override
        public List<Stakeholder> findByTenant(UUID tenantId) {
            return new ArrayList<>(parId.values().stream()
                    .filter(s -> s.tenantId().equals(tenantId)).toList());
        }

        @Override
        public boolean nameTaken(UUID tenantId, StakeholderCategory category, String name,
                                 UUID exceptId) {
            return parId.values().stream()
                    .filter(s -> s.tenantId().equals(tenantId))
                    .filter(s -> s.category() == category)
                    .filter(s -> s.name().toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT)))
                    .anyMatch(s -> exceptId == null || !s.id().equals(exceptId));
        }

        @Override
        public void delete(Stakeholder stakeholder) {
            parId.remove(stakeholder.id());
        }
    }
}
