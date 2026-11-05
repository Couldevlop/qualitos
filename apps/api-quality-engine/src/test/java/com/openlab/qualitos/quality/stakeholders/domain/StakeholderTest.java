package com.openlab.qualitos.quality.stakeholders.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Les règles de la fiche d'une partie intéressée : ce qu'elle exige à
 * l'inscription, comment la criticité se calcule, et pourquoi une décision de
 * pertinence ne se prend pas sans la justifier.
 */
class StakeholderTest {

    private static final UUID TENANT = UUID.randomUUID();

    private Stakeholder fiche() {
        return new Stakeholder(UUID.randomUUID(), TENANT, "Client A", StakeholderCategory.CUSTOMERS,
                StakeholderSide.EXTERNAL);
    }

    // ---------- inscription ----------

    @Test
    void uneFicheNaitNonJugee() {
        Stakeholder s = fiche();

        // Poser « pertinente » d'office ferait entrer dans le système des parties
        // qu'on n'a pas examinées.
        assertThat(s.relevance()).isEqualTo(Relevance.TO_DETERMINE);
        assertThat(s.qualityImpact()).isEqualTo(ImpactLevel.LOW);
        assertThat(s.regulatoryImpact()).isEqualTo(ImpactLevel.LOW);
        assertThat(s.criticality()).isEqualTo(ImpactLevel.LOW);
        assertThat(s.requirements()).isEmpty();
        assertThat(s.lastReviewedOn()).isNull();
    }

    @Test
    void uneFicheSansNomNExistePas() {
        assertThatThrownBy(() -> new Stakeholder(UUID.randomUUID(), TENANT, "   ",
                StakeholderCategory.CUSTOMERS, StakeholderSide.EXTERNAL))
                .isInstanceOf(StakeholderValidationException.class)
                .hasMessageContaining("name");
    }

    @Test
    void unNomTropLongEstRefuse() {
        assertThatThrownBy(() -> fiche().rename("C".repeat(Stakeholder.NAME_MAX + 1)))
                .isInstanceOf(StakeholderValidationException.class)
                .hasMessageContaining(String.valueOf(Stakeholder.NAME_MAX));
    }

    @Test
    void leNomEstRogne() {
        Stakeholder s = fiche();

        s.rename("  Client B  ");

        assertThat(s.name()).isEqualTo("Client B");
    }

    @Test
    void uneFicheSansCategorieOuSansCoteNExistePas() {
        Stakeholder s = fiche();

        assertThatThrownBy(() -> s.reclassify(null, StakeholderSide.EXTERNAL))
                .isInstanceOf(StakeholderValidationException.class);
        assertThatThrownBy(() -> s.reclassify(StakeholderCategory.CUSTOMERS, null))
                .isInstanceOf(StakeholderValidationException.class);
    }

    // ---------- criticité ----------

    @Test
    void laCriticiteRetientLePlusEleveDesDeuxImpacts() {
        Stakeholder s = fiche();

        // Une autorité de tutelle dont l'impact qualité est faible reste critique :
        // la moyenne ferait disparaître le seul des deux qui compte.
        s.rate(ImpactLevel.LOW, ImpactLevel.HIGH);
        assertThat(s.criticality()).isEqualTo(ImpactLevel.HIGH);

        s.rate(ImpactLevel.MEDIUM, ImpactLevel.LOW);
        assertThat(s.criticality()).isEqualTo(ImpactLevel.MEDIUM);

        s.rate(ImpactLevel.HIGH, ImpactLevel.HIGH);
        assertThat(s.criticality()).isEqualTo(ImpactLevel.HIGH);
    }

    @Test
    void uneCoteAbsenteVautFaible() {
        Stakeholder s = fiche();

        s.rate(null, null);

        assertThat(s.qualityImpact()).isEqualTo(ImpactLevel.LOW);
        assertThat(s.criticality()).isEqualTo(ImpactLevel.LOW);
    }

    @Test
    void lePireDeDeuxNiveauxTraiteLesAbsences() {
        assertThat(ImpactLevel.worst(null, null)).isEqualTo(ImpactLevel.LOW);
        assertThat(ImpactLevel.worst(null, ImpactLevel.MEDIUM)).isEqualTo(ImpactLevel.MEDIUM);
        assertThat(ImpactLevel.worst(ImpactLevel.HIGH, null)).isEqualTo(ImpactLevel.HIGH);
    }

    // ---------- pertinence ----------

    @Test
    void retenirUnePartieInteresseeSeJustifie() {
        Stakeholder s = fiche();

        assertThatThrownBy(() -> s.analyse("Pièces conformes", null, Relevance.RELEVANT, null, null))
                .isInstanceOf(StakeholderValidationException.class)
                .hasMessageContaining("justified");
    }

    @Test
    void ecarterUnePartieInteresseeSeJustifieAussi() {
        // Écarter sans dire pourquoi est indéfendable devant un auditeur, qui
        // vient vérifier que le périmètre a été déterminé et non subi.
        Stakeholder s = fiche();

        assertThatThrownBy(() -> s.analyse(null, null, Relevance.NOT_RELEVANT, "   ", null))
                .isInstanceOf(StakeholderValidationException.class)
                .hasMessageContaining("justified");
    }

    @Test
    void uneDecisionJustifieePasse() {
        Stakeholder s = fiche();

        s.analyse("Pièces conformes", "Contrat cadre", Relevance.RELEVANT,
                "40 % du chiffre d'affaires", "Perte du client en cas de NC récurrente");

        assertThat(s.relevance()).isEqualTo(Relevance.RELEVANT);
        assertThat(s.relevanceJustification()).isEqualTo("40 % du chiffre d'affaires");
        assertThat(s.needs()).isEqualTo("Pièces conformes");
    }

    @Test
    void laPertinenceNonTrancheeNExigeAucuneJustification() {
        Stakeholder s = fiche();

        s.analyse(null, null, Relevance.TO_DETERMINE, null, null);

        assertThat(s.relevance()).isEqualTo(Relevance.TO_DETERMINE);
    }

    @Test
    void unePertinenceAbsenteResteATrancher() {
        Stakeholder s = fiche();

        s.analyse(null, null, null, null, null);

        assertThat(s.relevance()).isEqualTo(Relevance.TO_DETERMINE);
    }

    @Test
    void laRelectureDepuisLaBaseNeRejouePasLesRegles() {
        // Une règle durcie après coup ne doit pas rendre illisible une fiche
        // écrite avant elle.
        Stakeholder s = fiche();

        s.restoreAnalysis(null, null, Relevance.RELEVANT, null, null);

        assertThat(s.relevance()).isEqualTo(Relevance.RELEVANT);
        assertThat(s.relevanceJustification()).isNull();
    }

    // ---------- revue ----------

    @Test
    void uneRevueNeSeDatePasDansLeFutur() {
        Stakeholder s = fiche();

        assertThatThrownBy(() -> s.reviewed(LocalDate.now().plusDays(1)))
                .isInstanceOf(StakeholderValidationException.class)
                .hasMessageContaining("future");
        assertThatThrownBy(() -> s.reviewed(null))
                .isInstanceOf(StakeholderValidationException.class);
    }

    @Test
    void uneRevueEnRetardSeSignale() {
        Stakeholder s = fiche();
        LocalDate aujourdhui = LocalDate.of(2026, 11, 4);

        s.steer(null, null, null, null, aujourdhui.minusDays(1));
        assertThat(s.reviewOverdue(aujourdhui)).isTrue();

        s.steer(null, null, null, null, aujourdhui.plusDays(1));
        assertThat(s.reviewOverdue(aujourdhui)).isFalse();

        s.steer(null, null, null, null, null);
        assertThat(s.reviewOverdue(aujourdhui)).isFalse();
    }

    // ---------- exigences ----------

    private StakeholderRequirement exigence(String libelle) {
        return new StakeholderRequirement(null, "Livrer à l'heure", libelle, "Contrat C-2026-114",
                MandatoryNature.CONTRACTUAL, "Retard de livraison", ImpactLevel.MEDIUM,
                "Revue de contrat", "Taux de livraison à l'heure", "Directeur de production",
                "Tableaux de bord", RequirementStatus.APPLICABLE, null, null, null, null, null);
    }

    @Test
    void uneExigenceSansLibelleNExistePas() {
        assertThatThrownBy(() -> exigence("  "))
                .isInstanceOf(StakeholderValidationException.class)
                .hasMessageContaining("what is expected");
    }

    @Test
    void uneExigenceAjouteeRecoitUnIdentifiant() {
        Stakeholder s = fiche();

        StakeholderRequirement stored = s.addRequirement(exigence("Respecter le taux de service"));

        assertThat(stored.id()).isNotNull();
        assertThat(s.requirements()).hasSize(1);
        assertThat(s.requirement(stored.id())).isPresent();
    }

    @Test
    void unStatutAbsentVautAAnalyser() {
        StakeholderRequirement r = new StakeholderRequirement(null, null, "Tracer les lots", null,
                null, null, null, null, null, null, null, null, null, null, null, null, null);

        assertThat(r.status()).isEqualTo(RequirementStatus.TO_ANALYSE);
    }

    @Test
    void lesChampsVidesDUneExigenceDeviennentNuls() {
        StakeholderRequirement r = new StakeholderRequirement(null, "  ", "  Tracer les lots  ", "",
                null, null, null, null, null, null, null, null, null, null, null, null, null);

        assertThat(r.requirement()).isEqualTo("Tracer les lots");
        assertThat(r.need()).isNull();
        assertThat(r.origin()).isNull();
    }

    @Test
    void leNombreDExigencesEstBorne() {
        Stakeholder s = fiche();
        for (int i = 0; i < Stakeholder.REQUIREMENTS_MAX; i++) {
            s.addRequirement(exigence("Exigence " + i));
        }

        assertThatThrownBy(() -> s.addRequirement(exigence("Une de trop")))
                .isInstanceOf(StakeholderValidationException.class)
                .hasMessageContaining(String.valueOf(Stakeholder.REQUIREMENTS_MAX));
    }

    @Test
    void unStatutSeChange() {
        Stakeholder s = fiche();
        UUID id = s.addRequirement(exigence("Tracer les lots")).id();

        StakeholderRequirement updated = s.setRequirementStatus(id, RequirementStatus.CONTROLLED);

        assertThat(updated.status()).isEqualTo(RequirementStatus.CONTROLLED);
        assertThat(s.requirement(id).orElseThrow().status()).isEqualTo(RequirementStatus.CONTROLLED);
    }

    @Test
    void unStatutNulOuUneExigenceInconnueSontRefuses() {
        Stakeholder s = fiche();
        UUID id = s.addRequirement(exigence("Tracer les lots")).id();

        assertThatThrownBy(() -> s.setRequirementStatus(id, null))
                .isInstanceOf(StakeholderValidationException.class);
        assertThatThrownBy(() -> s.setRequirementStatus(UUID.randomUUID(), RequirementStatus.CONTROLLED))
                .isInstanceOf(StakeholderValidationException.class)
                .hasMessageContaining("Unknown requirement");
    }

    @Test
    void uneExigenceSeRattacheAUnObjetDUnAutreModule() {
        Stakeholder s = fiche();
        UUID id = s.addRequirement(exigence("Tracer les lots")).id();
        UUID cible = UUID.randomUUID();

        assertThat(s.linkRequirement(id, RequirementLink.RISK, cible).riskId()).isEqualTo(cible);
        assertThat(s.linkRequirement(id, RequirementLink.NON_CONFORMITY, cible).ncId()).isEqualTo(cible);
        assertThat(s.linkRequirement(id, RequirementLink.AUDIT, cible).auditId()).isEqualTo(cible);
        assertThat(s.linkRequirement(id, RequirementLink.DOCUMENT, cible).documentId()).isEqualTo(cible);
        // Les quatre liens coexistent : ils ne se remplacent pas l'un l'autre.
        StakeholderRequirement r = s.requirement(id).orElseThrow();
        assertThat(r.riskId()).isEqualTo(cible);
        assertThat(r.documentId()).isEqualTo(cible);
    }

    @Test
    void unLienSansGenreOuSansCibleEstRefuse() {
        Stakeholder s = fiche();
        UUID id = s.addRequirement(exigence("Tracer les lots")).id();

        assertThatThrownBy(() -> s.linkRequirement(id, null, UUID.randomUUID()))
                .isInstanceOf(StakeholderValidationException.class);
        assertThatThrownBy(() -> s.linkRequirement(id, RequirementLink.RISK, null))
                .isInstanceOf(StakeholderValidationException.class);
    }

    @Test
    void ouvrirUneActionFaitRepasserLExigenceAReevaluer() {
        // Ouvrir une action, c'est constater un écart : on ne peut plus afficher
        // l'exigence maîtrisée dans la même seconde.
        Stakeholder s = fiche();
        UUID id = s.addRequirement(exigence("Tracer les lots")).id();
        s.setRequirementStatus(id, RequirementStatus.CONTROLLED);
        UUID capa = UUID.randomUUID();

        StakeholderRequirement updated = s.attachCapa(id, capa);

        assertThat(updated.capaId()).isEqualTo(capa);
        assertThat(updated.status()).isEqualTo(RequirementStatus.TO_REASSESS);
    }

    @Test
    void cloturerLeDossierRendLExigenceMaitrisee() {
        // La boucle de retour : ouvrir fait passer « à réévaluer », clôturer
        // rend maîtrisée. Personne n'a deux tableaux à tenir.
        Stakeholder s = fiche();
        UUID id = s.addRequirement(exigence("Tracer les lots")).id();
        UUID capa = UUID.randomUUID();
        s.attachCapa(id, capa);

        assertThat(s.closeCapa(capa)).isTrue();
        assertThat(s.requirement(id)).get()
                .extracting(StakeholderRequirement::status)
                .isEqualTo(RequirementStatus.CONTROLLED);
    }

    @Test
    void laClotureNEcrasePasUnJugementHumainPosteriieur() {
        // Quelqu'un a replacé l'exigence « applicable » parce que l'écart
        // courait encore : la clôture du dossier ne doit pas passer par-dessus.
        Stakeholder s = fiche();
        UUID id = s.addRequirement(exigence("Tracer les lots")).id();
        UUID capa = UUID.randomUUID();
        s.attachCapa(id, capa);
        s.setRequirementStatus(id, RequirementStatus.APPLICABLE);

        assertThat(s.closeCapa(capa)).isFalse();
        assertThat(s.requirement(id)).get()
                .extracting(StakeholderRequirement::status)
                .isEqualTo(RequirementStatus.APPLICABLE);
    }

    @Test
    void laClotureDUnDossierEtrangerOuNulNeChangeRien() {
        Stakeholder s = fiche();
        UUID id = s.addRequirement(exigence("Tracer les lots")).id();
        s.attachCapa(id, UUID.randomUUID());

        assertThat(s.closeCapa(UUID.randomUUID())).isFalse();
        assertThat(s.closeCapa(null)).isFalse();
        assertThat(s.requirement(id)).get()
                .extracting(StakeholderRequirement::status)
                .isEqualTo(RequirementStatus.TO_REASSESS);
    }

    @Test
    void unRattachementCapaSansDossierEstRefuse() {
        Stakeholder s = fiche();
        UUID id = s.addRequirement(exigence("Tracer les lots")).id();

        assertThatThrownBy(() -> s.attachCapa(id, null))
                .isInstanceOf(StakeholderValidationException.class);
    }

    @Test
    void uneExigenceSeRetire() {
        Stakeholder s = fiche();
        UUID id = s.addRequirement(exigence("Tracer les lots")).id();

        s.removeRequirement(id);

        assertThat(s.requirements()).isEmpty();
        assertThatThrownBy(() -> s.removeRequirement(id))
                .isInstanceOf(StakeholderValidationException.class)
                .hasMessageContaining("Unknown requirement");
    }

    @Test
    void uneExigenceRelueGardeSonIdentifiant() {
        Stakeholder s = fiche();
        UUID id = UUID.randomUUID();

        s.restoreRequirement(exigence("Tracer les lots").withId(id));

        assertThat(s.requirements()).singleElement().extracting(StakeholderRequirement::id)
                .isEqualTo(id);
    }

    // ---------- personne ----------

    @Test
    void unePersonneSeDesignePasUnCompteEtUnNom() {
        assertThatThrownBy(() -> new Person(null, "Alice"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Person(UUID.randomUUID(), "  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new Person(UUID.randomUUID(), "N".repeat(Person.NAME_MAX + 10)).name())
                .hasSize(Person.NAME_MAX);
    }

    @Test
    void lesTextesLongsSontRognesPlutotQueRefuses() {
        // Un copier-coller un peu long ne doit pas faire perdre la saisie.
        Stakeholder s = fiche();

        s.describe("D".repeat(Stakeholder.TEXT_MAX + 100), "C".repeat(Stakeholder.LINE_MAX + 50), "  ");

        assertThat(s.description()).hasSize(Stakeholder.TEXT_MAX);
        assertThat(s.contact()).hasSize(Stakeholder.LINE_MAX);
        assertThat(s.sites()).isNull();
    }
}
