package com.openlab.qualitos.quality.derogation.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le cycle de vie d'une dérogation, exercé sans contexte Spring.
 *
 * <p>C'est l'intérêt de porter les règles sur l'agrégat : ces bancs n'ont besoin
 * ni de base, ni de jeton, ni de serveur — seulement d'un dossier et de qui le
 * manipule.
 */
class DerogationTest {

    private static final UUID CLIENT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DEMANDEUR = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID MANAGER = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TIERS = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant T0 = Instant.parse("2026-11-22T10:00:00Z");

    // ---- le brouillon --------------------------------------------------------

    @Test
    void unDossierNeufEstUnBrouillonModifiable() {
        Derogation d = brouillonVide();
        assertThat(d.getStatus()).isEqualTo(DerogationStatus.DRAFT);
        assertThat(d.getStatus().isEditable()).isTrue();
        assertThat(d.getStatus().isTerminal()).isFalse();
        assertThat(d.getAnswer().decision()).isEqualTo(CustomerDecision.PENDING);
        assertThat(d.getDispatch().estParti()).isFalse();
        assertThat(d.getApproval().estSignee()).isFalse();
    }

    @Test
    void unBrouillonSEnregistreIncomplet() {
        // La maquette enregistre un brouillon « autant de fois qu'il veut » : le
        // controle des six champs n'intervient qu'a la demande de signature.
        Derogation d = brouillonVide();
        d.edit(DEMANDEUR, new Identification(null, null, null, null, null, null, null, null),
                Nature.parDefaut(), List.of(), null, Analysis.vide(), T0);
        assertThat(d.getStatus()).isEqualTo(DerogationStatus.DRAFT);
        assertThat(d.missingFields()).hasSize(6);
    }

    @Test
    void seulLInitiateurModifieSonBrouillon() {
        Derogation d = brouillonVide();
        assertThatThrownBy(() -> d.edit(TIERS, identificationComplete(), Nature.parDefaut(),
                List.of(), "ok", analyseComplete(), T0))
                .isInstanceOf(DerogationForbiddenException.class)
                .hasMessageContaining("initiateur");
    }

    @Test
    void lesLignesVidesNeSontPasRetenuesEtLeRangEstRefait() {
        // La maquette ouvre DEUX lignes vides : les garder remplirait le registre
        // de defauts sans texte.
        Derogation d = brouillonComplet();
        d.edit(DEMANDEUR, identificationComplete(), Nature.parDefaut(),
                List.of(new DerogationLine(7, null, null, null),
                        new DerogationLine(9, "Cote 12,4", "12,9", new BigDecimal("4")),
                        new DerogationLine(3, "   ", "  ", null),
                        new DerogationLine(1, "Rugosite Ra 1,6", "Ra 2,1", null)),
                "ok", analyseComplete(), T0);
        assertThat(d.getLines()).extracting(DerogationLine::position).containsExactly(1, 2);
        assertThat(d.getLines()).extracting(DerogationLine::specification)
                .containsExactly("Cote 12,4", "Rugosite Ra 1,6");
        assertThat(d.firstLine()).isPresent();
        assertThat(d.firstLine().get().summary()).isEqualTo("Cote 12,4 → 12,9");
    }

    // ---- les six champs obligatoires ----------------------------------------

    @Test
    void lesSixChampsManquantsSortentDansLOrdreDeLaMaquette() {
        Derogation d = brouillonVide();
        assertThat(d.missingFields()).containsExactly(
                "client", "réf. pièce", "quantité", "justification", "cause",
                "action corrective");
    }

    @Test
    void uneQuantiteNulleOuNegativeCompteCommeManquante() {
        Derogation d = brouillonVide();
        d.edit(DEMANDEUR, new Identification("ACME", null, null, "P-1", null, null,
                        BigDecimal.ZERO, LocalDate.of(2026, 11, 22)),
                Nature.parDefaut(), List.of(), "ok", analyseComplete(), T0);
        assertThat(d.missingFields()).containsExactly("quantité");
    }

    @Test
    void uneDemandeIncompleteEstRefuseeAvecLaListeDesManquants() {
        Derogation d = brouillonVide();
        assertThatThrownBy(() -> d.requestApproval(DEMANDEUR, MANAGER, "Nina", T0))
                .isInstanceOf(DerogationValidationException.class)
                .hasMessageContaining("Champs obligatoires manquants :")
                .extracting(e -> ((DerogationValidationException) e).getMissingFields())
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.list(String.class))
                .hasSize(6);
    }

    @Test
    void uneLigneDEcartNEstPasExigeePourDemanderLaSignature() {
        // Six champs, pas sept : la maquette n'en demande pas davantage.
        Derogation d = brouillonComplet();
        d.requestApproval(DEMANDEUR, MANAGER, "Nina", T0);
        assertThat(d.getStatus()).isEqualTo(DerogationStatus.PENDING_INTERNAL);
    }

    // ---- la validation interne ----------------------------------------------

    @Test
    void laDemandeFigeLeNomDuManagerEtEffaceLeMotifPrecedent() {
        Derogation d = enAttenteInterne();
        d.sendBack(MANAGER, "Il manque la cote mesuree", T0);
        assertThat(d.getApproval().amendReason()).isEqualTo("Il manque la cote mesuree");
        d.requestApproval(DEMANDEUR, MANAGER, "Nina Costa", T0);
        assertThat(d.getApproval().amendReason()).isNull();
        assertThat(d.getApproval().managerAccountName()).isEqualTo("Nina Costa");
    }

    @Test
    void seulLeManagerDesigneSigne() {
        Derogation d = enAttenteInterne();
        assertThatThrownBy(() -> d.approve(TIERS, "Autre", trace(), T0))
                .isInstanceOf(DerogationForbiddenException.class)
                .hasMessageContaining("autre manager");
        assertThatThrownBy(() -> d.sendBack(TIERS, "motif", T0))
                .isInstanceOf(DerogationForbiddenException.class);
    }

    @Test
    void lInitiateurNeSignePasSonPropreDossier() {
        Derogation d = enAttenteInterne();
        assertThatThrownBy(() -> d.approve(DEMANDEUR, "Moi", trace(), T0))
                .isInstanceOf(DerogationForbiddenException.class);
    }

    @Test
    void leRenvoiExigeUnMotif() {
        Derogation d = enAttenteInterne();
        assertThatThrownBy(() -> d.sendBack(MANAGER, "   ", T0))
                .isInstanceOf(DerogationValidationException.class)
                .hasMessageContaining("corrigé");
    }

    @Test
    void leRenvoiRameneAUnBrouillonModifiableQuiPorteSonMotif() {
        Derogation d = enAttenteInterne();
        d.sendBack(MANAGER, "Cote mesuree absente", T0);
        assertThat(d.getStatus()).isEqualTo(DerogationStatus.TO_AMEND);
        assertThat(d.getStatus().isEditable()).isTrue();
        assertThat(d.getApproval().amendReason()).isEqualTo("Cote mesuree absente");
        assertThat(d.getApproval().estSignee()).isFalse();
    }

    @Test
    void laSignatureExigeUnNomEtUnTrace() {
        Derogation d = enAttenteInterne();
        assertThatThrownBy(() -> d.approve(MANAGER, " ", trace(), T0))
                .isInstanceOf(DerogationValidationException.class)
                .hasMessageContaining("Indiquez votre nom");
        assertThatThrownBy(() -> d.approve(MANAGER, "Nina", null, T0))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void laSignatureMetLeDossierEnApprouveQualite() {
        Derogation d = enAttenteInterne();
        d.approve(MANAGER, "Nina Costa", trace(), T0);
        assertThat(d.getStatus()).isEqualTo(DerogationStatus.QUALITY_APPROVED);
        assertThat(d.getApproval().signedByName()).isEqualTo("Nina Costa");
        assertThat(d.getApproval().signedAt()).isEqualTo(T0);
        assertThat(d.getApproval().estSignee()).isTrue();
        // Encore INTERNE : rien n'est parti au client.
        assertThat(d.getDispatch().estParti()).isFalse();
    }

    @Test
    void leRetraitNEstQuAuDemandeurEtQuEnAttenteInterne() {
        Derogation d = enAttenteInterne();
        assertThatThrownBy(() -> d.withdraw(MANAGER, T0))
                .isInstanceOf(DerogationForbiddenException.class);
        d.withdraw(DEMANDEUR, T0);
        assertThat(d.getStatus()).isEqualTo(DerogationStatus.DRAFT);
        assertThatThrownBy(() -> d.withdraw(DEMANDEUR, T0))
                .isInstanceOf(DerogationStateException.class)
                .hasMessageContaining("en attente de validation");
    }

    @Test
    void onNeModifiePasUnDossierQuiAttendLeManager() {
        Derogation d = enAttenteInterne();
        assertThatThrownBy(() -> d.edit(DEMANDEUR, identificationComplete(), Nature.parDefaut(),
                List.of(), "ok", analyseComplete(), T0))
                .isInstanceOf(DerogationStateException.class)
                .hasMessageContaining("retirez la demande");
    }

    // ---- la réouverture ------------------------------------------------------

    @Test
    void rouvrirEffaceLaSignatureEtRendLeBrouillon() {
        Derogation d = signe();
        d.reopen(DEMANDEUR, T0);
        assertThat(d.getStatus()).isEqualTo(DerogationStatus.DRAFT);
        assertThat(d.getApproval().estSignee()).isFalse();
        assertThat(d.getApproval().signaturePng()).isNull();
        assertThat(d.getApproval().signedByName()).isNull();
        // Le manager DESIGNE reste : c'est a lui qu'on redemandera.
        assertThat(d.getApproval().managerAccountId()).isEqualTo(MANAGER);
    }

    @Test
    void unDossierPartiAuClientNeSeRouvrePlus() {
        Derogation d = partiAuClient();
        assertThatThrownBy(() -> d.reopen(DEMANDEUR, T0))
                .isInstanceOf(DerogationStateException.class)
                .hasMessageContaining("parti au client");
    }

    @Test
    void unTiersNeRouvrePasUnDossierSigne() {
        // C'est le seul geste qui DETRUIT quelque chose -- la signature d'un
        // manager. La maquette montre le bouton a tous, mais la visibilite d'un
        // bouton n'est pas une decision de droits.
        Derogation d = signe();
        assertThatThrownBy(() -> d.reopen(TIERS, T0))
                .isInstanceOf(DerogationForbiddenException.class)
                .hasMessageContaining("manager qualité");
        assertThat(d.getStatus()).isEqualTo(DerogationStatus.QUALITY_APPROVED);
        assertThat(d.getApproval().estSignee()).isTrue();
    }

    @Test
    void leManagerQuiASigneRouvreAussi() {
        Derogation d = signe();
        d.reopen(MANAGER, T0);
        assertThat(d.getStatus()).isEqualTo(DerogationStatus.DRAFT);
    }

    @Test
    void onNeRouvrePasUnBrouillon() {
        Derogation d = brouillonComplet();
        assertThatThrownBy(() -> d.reopen(DEMANDEUR, T0))
                .isInstanceOf(DerogationStateException.class)
                .hasMessageContaining("approuvé par la qualité");
    }

    // ---- l'envoi au client ---------------------------------------------------

    @Test
    void onNEnvoieRienAvantLaSignatureDuManager() {
        Derogation d = enAttenteInterne();
        assertThatThrownBy(() -> d.markSentToCustomer(DEMANDEUR, "qualite@client.fr", null,
                DerogationLanguage.FR, T0))
                .isInstanceOf(DerogationStateException.class)
                .hasMessageContaining("doit avoir signé");
    }

    @Test
    void seulLInitiateurEnvoieAuClient() {
        Derogation d = signe();
        assertThatThrownBy(() -> d.markSentToCustomer(MANAGER, "qualite@client.fr", null,
                DerogationLanguage.FR, T0))
                .isInstanceOf(DerogationForbiddenException.class);
    }

    @Test
    void lEnvoiRetientLaLangueEtLesDeuxChampsAjoutes() {
        Derogation d = signe();
        d.markSentToCustomer(DEMANDEUR, " qualite@client.fr ", " Lot urgent ",
                DerogationLanguage.EN, T0);
        assertThat(d.getStatus()).isEqualTo(DerogationStatus.PENDING_CUSTOMER);
        assertThat(d.getDispatch().customerEmail()).isEqualTo("qualite@client.fr");
        assertThat(d.getDispatch().comment()).isEqualTo("Lot urgent");
        assertThat(d.getDispatch().language()).isEqualTo(DerogationLanguage.EN);
        assertThat(d.getDispatch().sentAt()).isEqualTo(T0);
    }

    @Test
    void lEnvoiExigeUneAdresse() {
        Derogation d = signe();
        assertThatThrownBy(() -> d.markSentToCustomer(DEMANDEUR, "  ", null,
                DerogationLanguage.FR, T0))
                .isInstanceOf(DerogationValidationException.class)
                .hasMessageContaining("adresse du client");
    }

    // ---- la décision du client ----------------------------------------------

    @Test
    void laDecisionNeSePorteQuApresLEnvoi() {
        Derogation d = signe();
        assertThatThrownBy(() -> d.recordCustomerAnswer(DEMANDEUR, CustomerDecision.ACCEPTED,
                LocalDate.of(2026, 11, 25), T0))
                .isInstanceOf(DerogationStateException.class)
                .hasMessageContaining("une fois la demande envoyée");
    }

    @Test
    void seulLInitiateurPorteLaDecision() {
        Derogation d = partiAuClient();
        assertThatThrownBy(() -> d.recordCustomerAnswer(MANAGER, CustomerDecision.ACCEPTED,
                LocalDate.of(2026, 11, 25), T0))
                .isInstanceOf(DerogationForbiddenException.class)
                .hasMessageContaining("porte la décision");
    }

    @Test
    void enAttenteNEstPasUneDecision() {
        Derogation d = partiAuClient();
        assertThatThrownBy(() -> d.recordCustomerAnswer(DEMANDEUR, CustomerDecision.PENDING,
                LocalDate.of(2026, 11, 25), T0))
                .isInstanceOf(DerogationValidationException.class);
        assertThatThrownBy(() -> d.recordCustomerAnswer(DEMANDEUR, null,
                LocalDate.of(2026, 11, 25), T0))
                .isInstanceOf(DerogationValidationException.class);
    }

    @Test
    void laDecisionExigeSaDate() {
        Derogation d = partiAuClient();
        assertThatThrownBy(() -> d.recordCustomerAnswer(DEMANDEUR, CustomerDecision.ACCEPTED,
                null, T0))
                .isInstanceOf(DerogationValidationException.class)
                .hasMessageContaining("date de la décision");
    }

    @Test
    void accepteSousConditionsGardeSaNuanceEtVautUnAccord() {
        Derogation d = partiAuClient();
        d.recordCustomerAnswer(DEMANDEUR, CustomerDecision.ACCEPTED_WITH_CONDITIONS,
                LocalDate.of(2026, 11, 25), T0);
        assertThat(d.getStatus()).isEqualTo(DerogationStatus.ACCEPTED);
        assertThat(d.getAnswer().decision())
                .isEqualTo(CustomerDecision.ACCEPTED_WITH_CONDITIONS);
        assertThat(d.getStatus().isTerminal()).isTrue();
    }

    @Test
    void leRefusDuClientTermineLeDossier() {
        Derogation d = partiAuClient();
        d.recordCustomerAnswer(DEMANDEUR, CustomerDecision.REFUSED,
                LocalDate.of(2026, 11, 26), T0);
        assertThat(d.getStatus()).isEqualTo(DerogationStatus.REFUSED);
        assertThat(d.getStatus().isEditable()).isFalse();
    }

    @Test
    void uneDecisionSaisieDeTraversSeCorrige() {
        Derogation d = partiAuClient();
        d.recordCustomerAnswer(DEMANDEUR, CustomerDecision.REFUSED,
                LocalDate.of(2026, 11, 26), T0);
        d.recordCustomerAnswer(DEMANDEUR, CustomerDecision.ACCEPTED,
                LocalDate.of(2026, 11, 26), T0);
        assertThat(d.getStatus()).isEqualTo(DerogationStatus.ACCEPTED);
    }

    @Test
    void unDossierTermineNeSeModifiePlus() {
        Derogation d = partiAuClient();
        d.recordCustomerAnswer(DEMANDEUR, CustomerDecision.ACCEPTED,
                LocalDate.of(2026, 11, 26), T0);
        assertThatThrownBy(() -> d.edit(DEMANDEUR, identificationComplete(), Nature.parDefaut(),
                List.of(), "ok", analyseComplete(), T0))
                .isInstanceOf(DerogationStateException.class)
                .hasMessageContaining("terminé");
    }

    // ---- identité ------------------------------------------------------------

    @Test
    void onReconnaitLInitiateurEtLeManager() {
        Derogation d = enAttenteInterne();
        assertThat(d.isInitiator(DEMANDEUR)).isTrue();
        assertThat(d.isInitiator(MANAGER)).isFalse();
        assertThat(d.isDesignatedManager(MANAGER)).isTrue();
        assertThat(d.isDesignatedManager(DEMANDEUR)).isFalse();
    }

    @Test
    void unDossierSansManagerNEnADaucun() {
        // `null` ne vaut pas joker : personne ne peut signer un dossier non confie.
        Derogation d = brouillonComplet();
        assertThat(d.isDesignatedManager(null)).isFalse();
        assertThat(d.isDesignatedManager(MANAGER)).isFalse();
    }

    @Test
    void laListeDesLignesRendueEstUneCopie() {
        Derogation d = brouillonComplet();
        assertThatThrownBy(() -> d.getLines().add(new DerogationLine(1, "x", "y", null)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ---- fabriques -----------------------------------------------------------

    private static Derogation brouillonVide() {
        return Derogation.create(CLIENT, "DER-2026-0001", DEMANDEUR, "Paul Girard",
                new Identification(null, null, null, null, null, null, null,
                        LocalDate.of(2026, 11, 22)), T0);
    }

    private static Derogation brouillonComplet() {
        Derogation d = brouillonVide();
        d.edit(DEMANDEUR, identificationComplete(), Nature.parDefaut(),
                List.of(new DerogationLine(1, "Cote 12,4", "12,9", new BigDecimal("4"))),
                "Pièces fonctionnelles, hors zone d'ajustement", analyseComplete(), T0);
        return d;
    }

    private static Derogation enAttenteInterne() {
        Derogation d = brouillonComplet();
        d.requestApproval(DEMANDEUR, MANAGER, "Nina", T0);
        return d;
    }

    private static Derogation signe() {
        Derogation d = enAttenteInterne();
        d.approve(MANAGER, "Nina Costa", trace(), T0);
        return d;
    }

    private static Derogation partiAuClient() {
        Derogation d = signe();
        d.markSentToCustomer(DEMANDEUR, "qualite@client.fr", null, DerogationLanguage.FR, T0);
        return d;
    }

    private static Identification identificationComplete() {
        return new Identification("ACME Industrie", "qualite@acme.fr", "CMD-7781",
                "P-4420 Bride acier", "C", "LOT-2026-11", new BigDecimal("120"),
                LocalDate.of(2026, 11, 22));
    }

    private static Analysis analyseComplete() {
        return new Analysis("Usure de l'outil de reprise", "Lot isolé et trié à 100 %",
                "Remplacement de l'outil et contrôle en cours de série",
                "8D-2026-0042, prévu le 15/12/2026", "Rapport de mesures, 3 photos");
    }

    static SignatureTrace trace() {
        return SignatureTrace.of(SignatureTraceTest.pngMinimal());
    }
}
