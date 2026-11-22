package com.openlab.qualitos.quality.derogation.application;

import com.openlab.qualitos.quality.derogation.domain.CustomerDecision;
import com.openlab.qualitos.quality.derogation.domain.Derogation;
import com.openlab.qualitos.quality.derogation.domain.DerogationLanguage;
import com.openlab.qualitos.quality.derogation.domain.DerogationForbiddenException;
import com.openlab.qualitos.quality.derogation.domain.DerogationNotFoundException;
import com.openlab.qualitos.quality.derogation.domain.DerogationQuery;
import com.openlab.qualitos.quality.derogation.domain.DerogationRepository;
import com.openlab.qualitos.quality.derogation.domain.DerogationStatus;
import com.openlab.qualitos.quality.derogation.domain.DerogationValidationException;
import com.openlab.qualitos.quality.derogation.domain.Identification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ce que le service orchestre autour du dossier : la référence, la visibilité,
 * les notifications, la piste d'audit, et l'ordre de l'envoi au client.
 *
 * <p>Aucun contexte Spring : les huit ports sont des doubles écrits à la main,
 * ce qui rend chaque banc lisible sans détour par un cadre de simulation.
 */
class DerogationServiceTest {

    private static final UUID CLIENT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DEMANDEUR = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID MANAGER = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TIERS = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private DepotEnMemoire depot;
    private AuditEnMemoire audit;
    private NotificationsEnMemoire notifications;
    private CourrielEnMemoire courriel;
    private PdfBidon pdf;
    private AnnuaireEnMemoire annuaire;
    private ActeurFixe acteur;
    private DerogationService service;

    @BeforeEach
    void preparer() {
        depot = new DepotEnMemoire();
        audit = new AuditEnMemoire();
        notifications = new NotificationsEnMemoire();
        courriel = new CourrielEnMemoire();
        pdf = new PdfBidon();
        annuaire = new AnnuaireEnMemoire();
        acteur = new ActeurFixe(DEMANDEUR, "Paul Girard");
        service = new DerogationService(depot, audit, notifications, courriel, pdf, annuaire,
                () -> CLIENT, acteur, new LiensDeTest(),
                Clock.fixed(Instant.parse("2026-11-22T10:00:00Z"), ZoneOffset.UTC));
    }

    // ---- la référence --------------------------------------------------------

    @Test
    void laReferencePorteLAnneeEtSeNumeroteASuivre() {
        assertThat(creer().reference()).isEqualTo("DER-2026-0001");
        assertThat(creer().reference()).isEqualTo("DER-2026-0002");
    }

    @Test
    void uneReferenceDejaPriseEstEnjambee() {
        // Un dossier supprime ou une ecriture concurrente laissent des trous :
        // la contrainte d'unicite est la seule autorite.
        depot.referencesPrises.add("DER-2026-0001");
        assertThat(creer().reference()).isEqualTo("DER-2026-0002");
    }

    @Test
    void laCreationJournaliseEtRendUnBrouillon() {
        DerogationDto.View vue = creer();
        assertThat(vue.status()).isEqualTo(DerogationStatus.DRAFT);
        assertThat(vue.editable()).isTrue();
        assertThat(vue.createdByName()).isEqualTo("Paul Girard");
        assertThat(audit.actions).containsExactly(DerogationAuditPort.Action.CREATED);
    }

    @Test
    void laDateDeDemandeParDefautEstCelleDeLHorloge() {
        assertThat(creer().requestDate()).isEqualTo(LocalDate.of(2026, 11, 22));
    }

    // ---- la visibilité -------------------------------------------------------

    @Test
    void sansLeDroitDeToutVoirUnDossierDAutruiRepondIntrouvable() {
        UUID id = creer().id();
        acteur.compte = TIERS;
        assertThatThrownBy(() -> service.get(id, false))
                .isInstanceOf(DerogationNotFoundException.class);
        // Avec le droit de tout voir, le meme appel passe.
        assertThat(service.get(id, true).id()).isEqualTo(id);
    }

    @Test
    void leManagerDesigneVoitLeDossierQuIlDoitSigner() {
        UUID id = demander();
        acteur.compte = MANAGER;
        assertThat(service.get(id, false).id()).isEqualTo(id);
    }

    @Test
    void lePdfPorteLaMemeGardeQueLaFiche() {
        UUID id = creer().id();
        acteur.compte = TIERS;
        assertThatThrownBy(() -> service.renderPdf(id, false))
                .isInstanceOf(DerogationNotFoundException.class);
        assertThat(service.renderPdf(id, true)).isNotEmpty();
    }

    @Test
    void rouvrirPorteAussiCetteGarde() {
        UUID id = signer();
        acteur.compte = TIERS;
        assertThatThrownBy(() -> service.reopen(id, false))
                .isInstanceOf(DerogationNotFoundException.class);
    }

    @Test
    void unTiersNeRouvrePasMemeAvecLeDroitDeToutVoir() {
        // `derogation.view.all` est accordee a TOUS les roles par defaut, comme
        // pour les CAPA : la garde de visibilite ne protegerait donc rien ici.
        // C'est l'agregat qui refuse.
        UUID id = signer();
        acteur.compte = TIERS;
        assertThatThrownBy(() -> service.reopen(id, true))
                .isInstanceOf(DerogationForbiddenException.class);
        assertThat(service.get(id, true).status())
                .isEqualTo(DerogationStatus.QUALITY_APPROVED);
    }

    @Test
    void laRechercheBorneeNePasseLaRestrictionQueSansLeDroit() {
        creer();
        service.search(new DerogationDto.SearchCommand(null, null, null, null, 0, 20), false);
        assertThat(depot.dernierCritere.visibleTo()).isEqualTo(DEMANDEUR);
        service.search(new DerogationDto.SearchCommand(null, null, null, null, 0, 20), true);
        assertThat(depot.dernierCritere.visibleTo()).isNull();
    }

    @Test
    void laRechercheTransmetLesQuatreFiltresDeLaPresentation() {
        service.search(new DerogationDto.SearchCommand(DEMANDEUR, "P-4420", "ACME",
                DerogationStatus.PENDING_CUSTOMER, 2, 50), true);
        assertThat(depot.dernierCritere.owner()).isEqualTo(DEMANDEUR);
        assertThat(depot.dernierCritere.partReference()).isEqualTo("P-4420");
        assertThat(depot.dernierCritere.customerName()).isEqualTo("ACME");
        assertThat(depot.dernierCritere.status()).isEqualTo(DerogationStatus.PENDING_CUSTOMER);
        assertThat(depot.dernierCritere.page()).isEqualTo(2);
        assertThat(depot.dernierCritere.size()).isEqualTo(50);
    }

    @Test
    void uneLigneDeRegistrePorteLePremierEcartEtLeNomDuCreateur() {
        UUID id = creer().id();
        service.update(id, fiche(List.of(
                new DerogationDto.LineCommand("Cote 12,4", "12,9", new BigDecimal("4")),
                new DerogationDto.LineCommand("Ra 1,6", "Ra 2,1", null))));
        var page = service.search(
                new DerogationDto.SearchCommand(null, null, null, null, 0, 20), true);
        assertThat(page.content()).hasSize(1);
        DerogationDto.RowView ligne = page.content().get(0);
        assertThat(ligne.firstDeviation()).isEqualTo("Cote 12,4 → 12,9");
        assertThat(ligne.createdByName()).isEqualTo("Paul Girard");
        assertThat(ligne.partReference()).isEqualTo("P-4420 Bride acier");
        assertThat(ligne.customerDecision()).isEqualTo(CustomerDecision.PENDING);
    }

    // ---- « Mes validations » -------------------------------------------------

    @Test
    void mesValidationsEtSonCompteurNeRegardentQueMoi() {
        demander();
        acteur.compte = MANAGER;
        assertThat(service.myPendingApprovalCount().count()).isEqualTo(1L);
        assertThat(service.myPendingApprovals(0, 20).content()).hasSize(1);
        acteur.compte = TIERS;
        assertThat(service.myPendingApprovalCount().count()).isZero();
        assertThat(service.myPendingApprovals(0, 20).content()).isEmpty();
    }

    // ---- la demande de validation -------------------------------------------

    @Test
    void laDemandeFigeLeNomLuDansLAnnuaireEtPrevientLeManager() {
        annuaire.noms.put(MANAGER, "Nina Costa");
        UUID id = demander();
        assertThat(service.get(id, true).managerAccountName()).isEqualTo("Nina Costa");
        assertThat(notifications.demandes).containsExactly(id);
        assertThat(courriel.managersPrevenus).containsExactly(MANAGER);
        assertThat(courriel.liens.get(0)).isEqualTo("https://qualitos.test/derogations/" + id);
        assertThat(audit.actions).contains(DerogationAuditPort.Action.APPROVAL_REQUESTED);
    }

    @Test
    void unManagerInconnuDeLAnnuaireEstRefuse() {
        // Sinon le dossier n'aurait PERSONNE pour le signer : l'agregat n'accepte
        // que le manager designe, et ce compte-la n'existe pas pour se presenter.
        UUID id = creer().id();
        service.update(id, fiche(List.of()));
        annuaire.noms.clear();
        assertThatThrownBy(() -> service.requestApproval(id,
                new DerogationDto.RequestApprovalCommand(MANAGER)))
                .isInstanceOf(DerogationValidationException.class)
                .hasMessageContaining("annuaire");
        assertThat(service.get(id, true).status()).isEqualTo(DerogationStatus.DRAFT);
        assertThat(notifications.demandes).isEmpty();
    }

    // ---- la signature et le renvoi ------------------------------------------

    @Test
    void laSignaturePrevientLInitiateurEtRendLeTraceRelisible() {
        UUID id = signer();
        DerogationDto.View vue = service.get(id, true);
        assertThat(vue.status()).isEqualTo(DerogationStatus.QUALITY_APPROVED);
        assertThat(vue.signaturePngBase64()).isNotBlank();
        assertThat(vue.signedByName()).isEqualTo("Nina Costa");
        assertThat(notifications.approbations).containsExactly(id);
        assertThat(audit.actions).contains(DerogationAuditPort.Action.APPROVED);
    }

    @Test
    void leRenvoiPrevientLInitiateurAvecSonMotif() {
        UUID id = demander();
        acteur.compte = MANAGER;
        service.sendBack(id, new DerogationDto.SendBackCommand("Cote mesuree absente"));
        assertThat(notifications.renvois).containsExactly(id);
        assertThat(service.get(id, true).amendReason()).isEqualTo("Cote mesuree absente");
        assertThat(audit.actions).contains(DerogationAuditPort.Action.SENT_BACK);
    }

    @Test
    void leRetraitRevientAuBrouillonEtSeJournalise() {
        UUID id = demander();
        service.withdraw(id);
        assertThat(service.get(id, true).status()).isEqualTo(DerogationStatus.DRAFT);
        assertThat(audit.actions).contains(DerogationAuditPort.Action.WITHDRAWN);
    }

    @Test
    void rouvrirEffaceLaSignatureEtSeJournalise() {
        UUID id = signer();
        service.reopen(id, true);
        DerogationDto.View vue = service.get(id, true);
        assertThat(vue.status()).isEqualTo(DerogationStatus.DRAFT);
        assertThat(vue.signaturePngBase64()).isNull();
        assertThat(audit.actions).contains(DerogationAuditPort.Action.REOPENED);
    }

    // ---- l'envoi au client ---------------------------------------------------

    @Test
    void lEnvoiEcritLEtatAVANTDePartir() {
        // L'ordre est la regle : si le courriel partait d'abord et que
        // l'enregistrement echouait, un client detiendrait un document signe pour
        // un dossier que la plateforme croirait encore interne.
        UUID id = signer();
        depot.journal.clear();
        service.sendToCustomer(id, new DerogationDto.SendToCustomerCommand(
                "qualite@client.fr", "Lot urgent", DerogationLanguage.FR));
        assertThat(depot.journal).isNotEmpty();
        assertThat(courriel.envoyes).hasSize(1);
        assertThat(depot.ordreDesEcritures.indexOf("save"))
                .isLessThan(depot.ordreDesEcritures.indexOf("mail"));
    }

    @Test
    void lEnvoiNeDonneNiLeClientNiLaCopieCacheeDansLeCorps() {
        // api-core les tire du jeton : les lui dire ici laisserait choisir de
        // quelle organisation un courriel parait partir (section 18.2 #2).
        UUID id = signer();
        service.sendToCustomer(id, new DerogationDto.SendToCustomerCommand(
                "qualite@client.fr", null, DerogationLanguage.FR));
        DerogationMailPort.CustomerMail parti = courriel.envoyes.get(0);
        assertThat(parti.to()).isEqualTo("qualite@client.fr");
        assertThat(parti.bccAccountId()).isEqualTo(DEMANDEUR);
        assertThat(parti.attachmentName()).isEqualTo("DER-2026-0001.pdf");
        assertThat(parti.pdf()).isNotEmpty();
    }

    @Test
    void leSujetEtLeCorpsSuiventLaLangueChoisie() {
        UUID id = signer();
        service.sendToCustomer(id, new DerogationDto.SendToCustomerCommand(
                "qa@client.com", "Please confirm", DerogationLanguage.EN));
        DerogationMailPort.CustomerMail parti = courriel.envoyes.get(0);
        assertThat(parti.subject()).startsWith("Concession request DER-2026-0001");
        assertThat(parti.body()).contains("Dear Sir or Madam")
                .contains("Please confirm")
                .contains("Paul Girard");
        assertThat(pdf.langues).containsExactly(DerogationLanguage.EN);
    }

    @Test
    void enFrancaisLeCorpsEstFrancais() {
        UUID id = signer();
        service.sendToCustomer(id, new DerogationDto.SendToCustomerCommand(
                "qualite@client.fr", null, DerogationLanguage.FR));
        assertThat(courriel.envoyes.get(0).body()).contains("Madame, Monsieur")
                .contains("pièce jointe");
    }

    @Test
    void sansLangueChoisieLeCourrierPartEnFrancais() {
        UUID id = signer();
        service.sendToCustomer(id, new DerogationDto.SendToCustomerCommand(
                "qualite@client.fr", null, null));
        assertThat(service.get(id, true).language()).isEqualTo(DerogationLanguage.FR);
    }

    @Test
    void unEchecDEnvoiLaisseRemonterEtNeJournalisePasLEnvoi() {
        UUID id = signer();
        courriel.panne = true;
        audit.actions.clear();
        assertThatThrownBy(() -> service.sendToCustomer(id,
                new DerogationDto.SendToCustomerCommand("qualite@client.fr", null,
                        DerogationLanguage.FR)))
                .isInstanceOf(DerogationMailException.class);
        assertThat(audit.actions).doesNotContain(DerogationAuditPort.Action.SENT_TO_CUSTOMER);
    }

    @Test
    void lePdfReluSuitLaLangueFigeeAuDossier() {
        UUID id = signer();
        service.sendToCustomer(id, new DerogationDto.SendToCustomerCommand(
                "qa@client.com", null, DerogationLanguage.EN));
        pdf.langues.clear();
        service.renderPdf(id, true);
        assertThat(pdf.langues).containsExactly(DerogationLanguage.EN);
    }

    @Test
    void avantTouteEmissionLePdfEstEnFrancais() {
        UUID id = creer().id();
        service.renderPdf(id, true);
        assertThat(pdf.langues).containsExactly(DerogationLanguage.FR);
    }

    // ---- la décision du client ----------------------------------------------

    @Test
    void laDecisionDuClientTermineEtSeJournalise() {
        UUID id = signer();
        service.sendToCustomer(id, new DerogationDto.SendToCustomerCommand(
                "qualite@client.fr", null, DerogationLanguage.FR));
        service.recordCustomerDecision(id, new DerogationDto.CustomerDecisionCommand(
                CustomerDecision.ACCEPTED_WITH_CONDITIONS, LocalDate.of(2026, 11, 25)));
        DerogationDto.View vue = service.get(id, true);
        assertThat(vue.status()).isEqualTo(DerogationStatus.ACCEPTED);
        assertThat(vue.customerDecision())
                .isEqualTo(CustomerDecision.ACCEPTED_WITH_CONDITIONS);
        assertThat(vue.customerDecisionDate()).isEqualTo(LocalDate.of(2026, 11, 25));
        assertThat(audit.actions).contains(DerogationAuditPort.Action.CUSTOMER_DECIDED);
    }

    // ---- la fiche ------------------------------------------------------------

    @Test
    void unDossierInconnuRepondIntrouvable() {
        UUID fantome = UUID.randomUUID();
        assertThatThrownBy(() -> service.get(fantome, true))
                .isInstanceOf(DerogationNotFoundException.class);
        assertThatThrownBy(() -> service.update(fantome, fiche(List.of())))
                .isInstanceOf(DerogationNotFoundException.class);
    }

    @Test
    void laModificationSeJournaliseEtRetireLesLignesVides() {
        UUID id = creer().id();
        DerogationDto.View vue = service.update(id, fiche(List.of(
                new DerogationDto.LineCommand(null, null, null),
                new DerogationDto.LineCommand("Cote 12,4", "12,9", new BigDecimal("4")))));
        assertThat(vue.lines()).hasSize(1);
        assertThat(vue.lines().get(0).position()).isEqualTo(1);
        assertThat(audit.actions).contains(DerogationAuditPort.Action.UPDATED);
    }

    @Test
    void unTableauDeLignesAbsentVautAucuneLigne() {
        UUID id = creer().id();
        assertThat(service.update(id, fiche(null)).lines()).isEmpty();
    }

    // ---- enchaînements -------------------------------------------------------

    private DerogationDto.View creer() {
        acteur.compte = DEMANDEUR;
        return service.create(fiche(List.of()));
    }

    private UUID demander() {
        annuaire.noms.putIfAbsent(MANAGER, "Nina");
        UUID id = creer().id();
        service.update(id, fiche(List.of()));
        service.requestApproval(id, new DerogationDto.RequestApprovalCommand(MANAGER));
        return id;
    }

    private UUID signer() {
        UUID id = demander();
        acteur.compte = MANAGER;
        service.approve(id, new DerogationDto.ApproveCommand("Nina Costa", png()));
        acteur.compte = DEMANDEUR;
        return id;
    }

    private static DerogationDto.SaveCommand fiche(List<DerogationDto.LineCommand> lignes) {
        return new DerogationDto.SaveCommand("ACME Industrie", "qualite@acme.fr", "CMD-7781",
                "P-4420 Bride acier", "C", "LOT-2026-11", new BigDecimal("120"), null,
                null, null, null, null, null, lignes,
                "Pièces fonctionnelles, hors zone d'ajustement",
                "Usure de l'outil", "Lot trié à 100 %", "Remplacement de l'outil",
                "8D-2026-0042", "Rapport de mesures");
    }

    static byte[] png() {
        BufferedImage image = new BufferedImage(60, 20, BufferedImage.TYPE_INT_ARGB);
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---- les doubles ---------------------------------------------------------

    /** Un dépôt en mémoire, fidèle sur le seul point qui compte : il rend des copies relues. */
    private static final class DepotEnMemoire implements DerogationRepository {

        private final Map<UUID, Derogation> contenu = new HashMap<>();
        private final List<String> journal = new ArrayList<>();
        private final List<String> ordreDesEcritures = new ArrayList<>();
        private final List<String> referencesPrises = new ArrayList<>();
        private DerogationQuery dernierCritere;

        @Override
        public Derogation save(Derogation dossier) {
            if (dossier.getId() == null) {
                dossier.assignId(UUID.randomUUID());
            }
            contenu.put(dossier.getId(), dossier);
            journal.add(dossier.getReference());
            ordreDesEcritures.add("save");
            return dossier;
        }

        @Override
        public Optional<Derogation> findById(UUID tenantId, UUID id) {
            return Optional.ofNullable(contenu.get(id))
                    .filter(d -> d.getTenantId().equals(tenantId));
        }

        @Override
        public Slice<Derogation> search(DerogationQuery query) {
            dernierCritere = query;
            List<Derogation> trouves = contenu.values().stream()
                    .filter(d -> query.owner() == null
                            || d.getCreatedByUserId().equals(query.owner()))
                    .filter(d -> query.status() == null || d.getStatus() == query.status())
                    .filter(d -> query.visibleTo() == null
                            || d.isInitiator(query.visibleTo())
                            || d.isDesignatedManager(query.visibleTo()))
                    .toList();
            return new Slice<>(trouves, trouves.size(), query.page(), query.size());
        }

        @Override
        public Slice<Derogation> findPendingApprovals(UUID tenantId, UUID manager,
                                                      int page, int size) {
            List<Derogation> trouves = contenu.values().stream()
                    .filter(d -> d.getStatus() == DerogationStatus.PENDING_INTERNAL)
                    .filter(d -> d.isDesignatedManager(manager))
                    .toList();
            return new Slice<>(trouves, trouves.size(), page, size);
        }

        @Override
        public long countPendingApprovals(UUID tenantId, UUID manager) {
            return findPendingApprovals(tenantId, manager, 0, 100).total();
        }

        @Override
        public long countByReferencePrefix(UUID tenantId, String prefix) {
            return contenu.values().stream()
                    .filter(d -> d.getReference().startsWith(prefix)).count();
        }

        @Override
        public boolean existsByReference(UUID tenantId, String reference) {
            return referencesPrises.contains(reference)
                    || contenu.values().stream()
                            .anyMatch(d -> d.getReference().equals(reference));
        }
    }

    private static final class AuditEnMemoire implements DerogationAuditPort {

        private final List<Action> actions = new ArrayList<>();

        @Override
        public void record(Derogation dossier, Action action) {
            actions.add(action);
        }
    }

    private static final class NotificationsEnMemoire implements DerogationNotificationPort {

        private final List<UUID> demandes = new ArrayList<>();
        private final List<UUID> approbations = new ArrayList<>();
        private final List<UUID> renvois = new ArrayList<>();

        @Override
        public void approvalRequested(Derogation d) {
            demandes.add(d.getId());
        }

        @Override
        public void approved(Derogation d) {
            approbations.add(d.getId());
        }

        @Override
        public void sentBack(Derogation d) {
            renvois.add(d.getId());
        }
    }

    private final class CourrielEnMemoire implements DerogationMailPort {

        private final List<UUID> managersPrevenus = new ArrayList<>();
        private final List<String> liens = new ArrayList<>();
        private final List<CustomerMail> envoyes = new ArrayList<>();
        private boolean panne;

        @Override
        public void notifyManager(UUID manager, String reference, String link) {
            managersPrevenus.add(manager);
            liens.add(link);
        }

        @Override
        public void sendToCustomer(CustomerMail courrier) {
            if (panne) {
                throw new DerogationMailException("relais injoignable");
            }
            envoyes.add(courrier);
            depot.ordreDesEcritures.add("mail");
        }
    }

    /** Le lien sous ses deux formes, comme en production. */
    private static final class LiensDeTest implements DerogationLinkBuilder {

        @Override
        public String linkTo(UUID id) {
            return "https://qualitos.test" + pathTo(id);
        }

        @Override
        public String pathTo(UUID id) {
            return "/derogations/" + id;
        }
    }

    private static final class PdfBidon implements DerogationPdfPort {

        private final List<DerogationLanguage> langues = new ArrayList<>();

        @Override
        public byte[] render(Derogation dossier, DerogationLanguage langue) {
            langues.add(langue);
            return "%PDF-1.7 faux".getBytes();
        }
    }

    private static final class AnnuaireEnMemoire implements DirectoryPort {

        private final Map<UUID, String> noms = new HashMap<>();

        @Override
        public Optional<String> displayNameOf(UUID compte) {
            return Optional.ofNullable(noms.get(compte));
        }

        @Override
        public Optional<String> emailOf(UUID compte) {
            return Optional.of("compte@test.fr");
        }
    }

    private static final class ActeurFixe implements ActorProvider {

        private UUID compte;
        private final String nom;

        ActeurFixe(UUID compte, String nom) {
            this.compte = compte;
            this.nom = nom;
        }

        @Override
        public UUID requireAccountId() {
            return compte;
        }

        @Override
        public Optional<String> displayName() {
            return Optional.ofNullable(nom);
        }
    }

    /** Un dossier neuf ne doit pas dépendre d'une identification déjà remplie. */
    @Test
    void laCreationAccepteUneIdentificationVide() {
        acteur.compte = DEMANDEUR;
        DerogationDto.View vue = service.create(new DerogationDto.SaveCommand(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null));
        assertThat(vue.status()).isEqualTo(DerogationStatus.DRAFT);
        assertThat(vue.customerName()).isNull();
        assertThat(vue.quantity()).isNull();
    }

    /** Une identification se construit nette : les blancs de bord tombent. */
    @Test
    void lesBlancsDeBordNeSurviventPas() {
        acteur.compte = DEMANDEUR;
        Identification ident = new Identification("  ACME  ", "   ", null, " P-1 ",
                null, null, BigDecimal.ONE, LocalDate.of(2026, 11, 22));
        assertThat(ident.customerName()).isEqualTo("ACME");
        assertThat(ident.customerQualityContact()).isNull();
        assertThat(ident.partReference()).isEqualTo("P-1");
    }
}
