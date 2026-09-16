package com.openlab.qualitos.quality.nonconformity.eightd.application;

import com.openlab.qualitos.quality.nonconformity.NcStatus;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDReport;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDReportNotFoundException;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDReportRepository;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDSnapshot;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
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
 * Le cas d'usage, avec des doublures écrites à la main plutôt que des mocks : on y
 * lit le scénario complet d'une émission — figer, signer, ancrer, journaliser — et
 * les refus qui l'encadrent.
 */
class EightDServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID AUTRE_TENANT = UUID.randomUUID();
    private static final UUID NC = UUID.randomUUID();
    private static final UUID ACTEUR = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-13T10:00:00Z");

    private DepotMemoire depot;
    private SourcesFigees sources;
    private RenduFactice rendu;
    private SceauFactice sceaux;
    private JournalFactice journal;
    private EightDService service;

    @BeforeEach
    void setUp() {
        depot = new DepotMemoire();
        sources = new SourcesFigees(NcStatus.CLOSED);
        rendu = new RenduFactice();
        sceaux = new SceauFactice();
        journal = new JournalFactice();
        service = new EightDService(depot, sources, new EightDSnapshotAssembler(), rendu, sceaux,
                new CodecFactice(), code -> "https://qualitos.test/verify/" + code, journal,
                () -> TENANT, new ActeurFixe(), new SecureRandom(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    // ---------- consulter ----------

    @Test
    void consulter_sans_rapport_existant_agrege_les_sources_et_annonce_le_partiel() {
        EightDDto.ReportView vue = service.consulter(NC);

        assertThat(vue.status()).isEqualTo("DRAFT");
        assertThat(vue.ncReference()).isEqualTo("NC-2026-0007");
        assertThat(vue.disciplines()).hasSize(8);
        assertThat(vue.partial()).isTrue();
        assertThat(vue.missingCodes()).contains("D1", "D3", "D8");
        assertThat(vue.seal()).isNull();
        // La non-conformité est clôturée : le geste d'émission est offert.
        assertThat(vue.issuable()).isTrue();
    }

    @Test
    void seules_d1_d3_et_d8_sont_declarees_modifiables() {
        EightDDto.ReportView vue = service.consulter(NC);

        List<String> modifiables = vue.disciplines().stream()
                .filter(EightDDto.DisciplineView::editable)
                .map(EightDDto.DisciplineView::code)
                .toList();
        assertThat(modifiables).containsExactly("D1", "D3", "D8");
    }

    @Test
    void une_nc_non_cloturee_n_offre_pas_l_emission() {
        sources = new SourcesFigees(NcStatus.RESOLVED);
        service = reconstruire();

        assertThat(service.consulter(NC).issuable()).isFalse();
    }

    // ---------- enregistrer ----------

    @Test
    void enregistrer_cree_le_brouillon_au_premier_passage() {
        EightDDto.ReportView vue = service.enregistrer(NC,
                new EightDDto.SaveCommand("Ada, Grace", "Tri à 100 % du stock", "Merci à l'équipe"));

        assertThat(depot.findByNc(TENANT, NC)).isPresent();
        assertThat(vue.team()).isEqualTo("Ada, Grace");
        // Les trois disciplines saisies ne sont plus vides ; le rapport reste
        // partiel pour les cinq qui n'ont, ici, aucune source a agreger.
        assertThat(vue.missingCodes()).doesNotContain("D1", "D3", "D8");
        assertThat(vue.partial()).isTrue();
    }

    @Test
    void enregistrer_sur_un_rapport_emis_est_refuse() {
        service.enregistrer(NC, new EightDDto.SaveCommand("Ada", "Tri", "Merci"));
        service.emettre(NC);

        assertThatThrownBy(() -> service.enregistrer(NC, new EightDDto.SaveCommand("Autre", null, null)))
                .isInstanceOf(EightDStateException.class)
                .hasMessageContaining("scellé");
    }

    // ---------- émettre ----------

    @Test
    void emettre_avant_la_cloture_est_refuse() {
        sources = new SourcesFigees(NcStatus.RESOLVED);
        service = reconstruire();

        assertThatThrownBy(() -> service.emettre(NC))
                .isInstanceOf(EightDStateException.class)
                .hasMessageContaining("clôture");
        assertThat(sceaux.appels).isZero();
        assertThat(journal.entrees).isEmpty();
    }

    @Test
    void emettre_fige_signe_ancre_et_journalise() {
        service.enregistrer(NC, new EightDDto.SaveCommand("Ada", "Tri à 100 %", "Merci"));

        EightDDto.ReportView vue = service.emettre(NC);

        assertThat(vue.status()).isEqualTo("ISSUED");
        assertThat(vue.issuable()).isFalse();
        assertThat(vue.seal()).isNotNull();
        assertThat(vue.seal().sha256Hex()).isEqualTo(sceaux.derniereEmpreinte);
        assertThat(vue.seal().anchorTxRef()).isEqualTo("tx-1");
        assertThat(vue.seal().issuedAt()).isEqualTo(NOW);
        assertThat(vue.seal().issuedByName()).isEqualTo("Ada Lovelace");
        assertThat(vue.seal().verificationCode()).hasSize(32);
        // L'URL du QR code porte le code qui a été scellé, et non un autre.
        assertThat(rendu.derniereUrl).endsWith(vue.seal().verificationCode());
        assertThat(journal.entrees).hasSize(1);
        assertThat(journal.entrees.get(0)).contains(TENANT.toString(), ACTEUR.toString(),
                "NC-2026-0007");
    }

    @Test
    void emettre_deux_fois_est_refuse() {
        service.emettre(NC);

        assertThatThrownBy(() -> service.emettre(NC))
                .isInstanceOf(EightDStateException.class)
                .hasMessageContaining("déjà été émis");
        assertThat(sceaux.appels).isEqualTo(1);
    }

    @Test
    void le_contenu_emis_ne_suit_plus_les_sources_qui_changent_apres_coup() {
        service.emettre(NC);
        // Après l'émission, une CAPA est escaladée et une cause racine est saisie.
        sources.enrichir();

        EightDDto.ReportView vue = service.consulter(NC);

        // Le rapport émis dit toujours ce qu'il disait : c'est ce que le client a lu.
        assertThat(vue.disciplines().stream()
                .filter(d -> d.code().equals("D5"))
                .findFirst().orElseThrow().sourceLabel())
                .contains("No CAPA");
    }

    // ---------- PDF ----------

    @Test
    void le_pdf_d_un_rapport_non_emis_est_refuse() {
        service.enregistrer(NC, new EightDDto.SaveCommand("Ada", null, null));

        assertThatThrownBy(() -> service.pdf(NC))
                .isInstanceOf(EightDStateException.class)
                .hasMessageContaining("pas encore émis");
    }

    @Test
    void le_pdf_d_une_nc_sans_rapport_est_introuvable() {
        assertThatThrownBy(() -> service.pdf(NC))
                .isInstanceOf(EightDReportNotFoundException.class);
    }

    @Test
    void le_pdf_est_remis_avec_son_empreinte_et_un_nom_de_fichier_parlant() {
        service.emettre(NC);

        EightDDto.PdfResult pdf = service.pdf(NC);

        assertThat(pdf.pdf()).isNotEmpty();
        assertThat(pdf.fileName()).isEqualTo("8d-nc-2026-0007-20260913-100000.pdf");
        assertThat(pdf.sha256Hex()).isEqualTo(sceaux.derniereEmpreinte);
    }

    @Test
    void un_rendu_qui_ne_retombe_plus_sur_l_empreinte_scellee_n_est_pas_remis() {
        service.emettre(NC);
        // Simule une évolution du rendu après l'émission : le document ne correspond
        // plus à ce qui a été signé. Le remettre quand même présenterait comme
        // authentifié un fichier que la signature ne couvre pas.
        rendu.deriver();

        assertThatThrownBy(() -> service.pdf(NC))
                .isInstanceOf(EightDStateException.class)
                .hasMessageContaining("empreinte scellée");
    }

    // ---------- vérification publique ----------

    @Test
    void un_code_inconnu_repond_invalide_sans_detail() {
        EightDDto.VerificationResult resultat = service.verifier("CODEINCONNU12345");

        assertThat(resultat.valid()).isFalse();
        assertThat(resultat.sha256Hex()).isNull();
        assertThat(resultat.anchorTxRef()).isNull();
        assertThat(resultat.ncReference()).isNull();
    }

    @Test
    void un_code_connu_revalide_la_signature_et_rend_les_faits_d_integrite() {
        EightDDto.ReportView vue = service.emettre(NC);

        EightDDto.VerificationResult resultat = service.verifier(vue.seal().verificationCode());

        assertThat(resultat.valid()).isTrue();
        assertThat(resultat.sha256Hex()).isEqualTo(vue.seal().sha256Hex());
        assertThat(resultat.anchorTxRef()).isEqualTo("tx-1");
        assertThat(resultat.ncReference()).isEqualTo("NC-2026-0007");
    }

    @Test
    void une_signature_qui_ne_repond_plus_rend_un_resultat_invalide() {
        EightDDto.ReportView vue = service.emettre(NC);
        sceaux.valide = false;

        assertThat(service.verifier(vue.seal().verificationCode()).valid()).isFalse();
    }

    // ---------- cloisonnement ----------

    @Test
    void un_rapport_d_un_autre_tenant_reste_invisible() {
        service.emettre(NC);

        // Le même identifiant de NC, lu avec le tenant d'un autre client : rien.
        EightDService autre = new EightDService(depot, sources, new EightDSnapshotAssembler(),
                rendu, sceaux, new CodecFactice(), code -> "u/" + code, journal,
                () -> AUTRE_TENANT, new ActeurFixe(), new SecureRandom(),
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> autre.pdf(NC)).isInstanceOf(EightDReportNotFoundException.class);
    }

    private EightDService reconstruire() {
        return new EightDService(depot, sources, new EightDSnapshotAssembler(), rendu, sceaux,
                new CodecFactice(), code -> "https://qualitos.test/verify/" + code, journal,
                () -> TENANT, new ActeurFixe(), new SecureRandom(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    // ---------- doublures ----------

    /** Dépôt en mémoire, tenanté comme le vrai. */
    private static final class DepotMemoire implements EightDReportRepository {

        private final Map<String, EightDReport> parNc = new HashMap<>();
        private final Map<String, EightDReport> parCode = new HashMap<>();

        @Override
        public EightDReport save(EightDReport report) {
            if (report.getId() == null) {
                report.assignId(UUID.randomUUID());
            }
            parNc.put(report.getTenantId() + "/" + report.getNcId(), report);
            if (report.getVerificationCode() != null) {
                parCode.put(report.getVerificationCode(), report);
            }
            return report;
        }

        @Override
        public Optional<EightDReport> findByNc(UUID tenantId, UUID ncId) {
            return Optional.ofNullable(parNc.get(tenantId + "/" + ncId));
        }

        @Override
        public Optional<EightDReport> findByVerificationCode(String verificationCode) {
            return Optional.ofNullable(parCode.get(verificationCode));
        }
    }

    /** Sources figées : une NC seule, éventuellement enrichie après coup. */
    private static final class SourcesFigees implements EightDSourcePort {

        private final NcStatus status;
        private boolean enrichi;

        private SourcesFigees(NcStatus status) {
            this.status = status;
        }

        private void enrichir() {
            this.enrichi = true;
        }

        @Override
        public EightDSources collect(UUID tenantId, UUID ncId) {
            EightDSources.Nc nc = new EightDSources.Nc("NC-2026-0007", "Fuite au presse-étoupe",
                    "Flaque d'huile", "PROCESS", "MAJOR", "INTERNAL", status,
                    Instant.parse("2026-09-01T08:30:00Z"),
                    status == NcStatus.CLOSED ? Instant.parse("2026-09-12T16:00:00Z") : null,
                    "Atelier 3", "Ada Lovelace", 1,
                    enrichi ? "Joint usé" : null, null);
            EightDSources.Capa capa = enrichi
                    ? new EightDSources.Capa("CAPA", "CORRECTIVE", "HIGH", "OPEN", null, null,
                            null, null, 0,
                            List.of(new EightDSources.Action("Remplacer", null, "CORRECTIVE",
                                    "OPEN", null, null, null, 0)))
                    : null;
            return new EightDSources(nc, List.of(), List.of(), capa, null, List.of());
        }
    }

    /** Rendu factice : des octets qui dépendent du contenu, et peuvent « dériver ». */
    private static final class RenduFactice implements EightDPdfRenderPort {

        private String derniereUrl;
        private String variante = "v1";

        private void deriver() {
            this.variante = "v2";
        }

        @Override
        public byte[] render(EightDSnapshot snapshot, String verifyUrl) {
            this.derniereUrl = verifyUrl;
            return (variante + "|" + snapshot.ncReference() + "|" + snapshot.sections().size())
                    .getBytes(StandardCharsets.UTF_8);
        }
    }

    /** Sceau factice : compte les appels, retient l'empreinte, sait se dédire. */
    private static final class SceauFactice implements EightDSealPort {

        private int appels;
        private String derniereEmpreinte;
        private boolean valide = true;

        @Override
        public Seal seal(UUID tenantId, String sha256Hex) {
            appels++;
            derniereEmpreinte = sha256Hex;
            return new Seal("enveloppe-" + appels, "tx-" + appels);
        }

        @Override
        public boolean verify(String signature, String sha256Hex) {
            return valide;
        }
    }

    /** Codec factice : un aller-retour fidèle, sans dépendance Jackson. */
    private static final class CodecFactice implements EightDSupportPorts.SnapshotCodec {

        private final Map<String, EightDSnapshot> magasin = new HashMap<>();

        @Override
        public String encode(EightDSnapshot snapshot) {
            String cle = "snap-" + magasin.size();
            magasin.put(cle, snapshot);
            return cle;
        }

        @Override
        public EightDSnapshot decode(String json) {
            EightDSnapshot snapshot = magasin.get(json);
            if (snapshot == null) {
                throw new IllegalStateException("instantane inconnu: " + json);
            }
            return snapshot;
        }
    }

    private static final class JournalFactice implements EightDSupportPorts.AuditPort {

        private final List<String> entrees = new ArrayList<>();

        @Override
        public void recordIssued(UUID tenantId, UUID actorId, UUID ncId,
                                 String summary, String detailsJson) {
            entrees.add(tenantId + "|" + actorId + "|" + ncId + "|" + summary + "|" + detailsJson);
        }
    }

    private static final class ActeurFixe implements EightDSupportPorts.ActorProvider {

        @Override
        public UUID currentUserId() {
            return ACTEUR;
        }

        @Override
        public String currentUserName() {
            return "Ada Lovelace";
        }
    }
}
