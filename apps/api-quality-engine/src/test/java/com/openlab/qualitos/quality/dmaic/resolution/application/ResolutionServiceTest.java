package com.openlab.qualitos.quality.dmaic.resolution.application;

import com.openlab.qualitos.quality.dmaic.resolution.domain.ActionLine;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ActionStatus;
import com.openlab.qualitos.quality.dmaic.resolution.domain.AnswerType;
import com.openlab.qualitos.quality.dmaic.resolution.domain.DefineTopic;
import com.openlab.qualitos.quality.dmaic.resolution.domain.FailureType;
import com.openlab.qualitos.quality.dmaic.resolution.domain.Person;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionContent;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionNotFoundException;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionSettings;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionSheet;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionValidationException;
import com.openlab.qualitos.quality.dmaic.resolution.domain.SeverityLevel;
import com.openlab.qualitos.quality.dmaic.resolution.domain.SheetPhoto;
import com.openlab.qualitos.quality.dmaic.resolution.domain.SheetQuestion;
import com.openlab.qualitos.quality.dmaic.resolution.domain.SheetSection;
import com.openlab.qualitos.quality.nonconformity.storage.StorageDisabledException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
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
 * Le service de la fiche de résolution, sur des ports en mémoire : le projet
 * est lu dans le tenant du jeton, la fiche se vérifie contre le paramétrage du
 * client, les photos passent les gardes du binaire, et chaque écriture laisse
 * sa trace.
 */
class ResolutionServiceTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID AUTRE_TENANT = UUID.randomUUID();
    static final UUID ACTEUR = UUID.randomUUID();
    static final UUID PROJET = UUID.randomUUID();
    static final UUID ISHIKAWA = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-10-24T09:00:00Z");
    static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2};

    FakeSheets sheets;
    FakeSettings settings;
    FakePhotos photos;
    FakeStorage storage;
    List<String> journal;
    ClaimReport[] rendu;
    UUID tenantCourant;
    ResolutionService service;

    @BeforeEach
    void setUp() {
        sheets = new FakeSheets();
        settings = new FakeSettings();
        photos = new FakePhotos();
        storage = new FakeStorage();
        journal = new ArrayList<>();
        rendu = new ClaimReport[1];
        tenantCourant = TENANT;
        ResolutionPorts.Context context = new ResolutionPorts.Context() {
            @Override public UUID requireTenantId() { return tenantCourant; }
            @Override public UUID requireActorId() { return ACTEUR; }
        };
        ResolutionPorts.Projects projects = (t, p) -> t.equals(TENANT) && p.equals(PROJET)
                ? Optional.of(new ResolutionPorts.ProjectHeader(PROJET, "Fissures carter", "Carters fissurés"))
                : Optional.empty();
        ResolutionPorts.Ishikawas ishikawas = (t, id) -> t.equals(TENANT) && id.equals(ISHIKAWA);
        ResolutionPorts.Audit audit = new ResolutionPorts.Audit() {
            @Override public void sheetSaved(ResolutionSheet s, UUID a) { journal.add("saved"); }
            @Override public void settingsReplaced(UUID t, ResolutionSettings s, UUID a) { journal.add("settings"); }
            @Override public void settingsReset(UUID t, UUID a) { journal.add("reset"); }
            @Override public void photoAdded(SheetPhoto p, UUID a) { journal.add("photo+" + p.slot()); }
            @Override public void photoRemoved(SheetPhoto p, UUID a) { journal.add("photo-" + p.slot()); }
        };
        service = new ResolutionService(context, projects, sheets, settings, photos, storage, ishikawas, audit,
                r -> {
                    rendu[0] = r;
                    return new byte[]{'%', 'P', 'D', 'F'};
                }, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    static ResolutionContent contenu(Map<String, String> answers) {
        return new ResolutionContent("Acme", "CL-7", new Person(ACTEUR, "Alice"), List.of(), LocalDate.of(2026, 10, 1),
                null, 1, "V10", "Fissure", answers, "Mesures", "Tri", "Outil", "Nouvel outil", "Contrôle",
                List.of(), List.of(new ActionLine(null, "Changer", null, ActionStatus.OPEN, null, null, 0, null)),
                null);
    }

    // ---------- paramétrage ----------

    @Test
    void sansReglageLaPlateformeProposeLeClasseur() {
        ResolutionDto.SettingsView v = service.settings();
        assertThat(v.defaults()).isTrue();
        assertThat(v.questions()).hasSize(48);
    }

    @Test
    void lAdministrateurRemplaceEtRendLeParametrage() {
        ResolutionDto.SettingsView v = service.replaceSettings(
                List.of(new SheetQuestion("q1", SheetSection.MEASURE, null, "Mesuré ?", AnswerType.YES_NO)),
                List.of(new FailureType("X1", "Rayure")), List.of(new SeverityLevel(1, "Haute")));

        assertThat(v.defaults()).isFalse();
        assertThat(v.questions().get(0).code()).isEqualTo("Q1");
        assertThat(service.settings().failureTypes()).extracting(FailureType::code).containsExactly("X1");
        assertThat(settings.actor).isEqualTo(ACTEUR);

        assertThat(service.resetSettings().defaults()).isTrue();
        assertThat(service.settings().defaults()).isTrue();
        assertThat(journal).containsExactly("settings", "reset");
    }

    // ---------- fiche ----------

    @Test
    void uneFicheJamaisEnregistreeSeLitViergeSansRienEcrire() {
        ResolutionDto.SheetView v = service.sheet(PROJET);

        assertThat(v.saved()).isFalse();
        assertThat(v.projectTitle()).isEqualTo("Fissures carter");
        assertThat(v.problemStatement()).isEqualTo("Carters fissurés");
        assertThat(v.content()).isEqualTo(ResolutionContent.empty());
        assertThat(v.settings().defaults()).isTrue();
        assertThat(sheets.store).isEmpty();
        assertThat(journal).isEmpty();
    }

    @Test
    void unProjetDUnAutreClientNExistePas() {
        tenantCourant = AUTRE_TENANT;
        assertThatThrownBy(() -> service.sheet(PROJET)).isInstanceOf(ResolutionNotFoundException.class);
        assertThatThrownBy(() -> service.save(PROJET, contenu(Map.of())))
                .isInstanceOf(ResolutionNotFoundException.class);
        assertThatThrownBy(() -> service.claimReport(PROJET)).isInstanceOf(ResolutionNotFoundException.class);
        assertThat(sheets.store).isEmpty();
    }

    @Test
    void enregistrerCreePuisRemplaceLaFiche() {
        ResolutionDto.SheetView v = service.save(PROJET, contenu(Map.of("DEF-04", "YES")));

        assertThat(v.saved()).isTrue();
        assertThat(v.content().answers()).containsEntry("DEF-04", "YES");
        assertThat(v.updatedAt()).isEqualTo(NOW);
        assertThat(sheets.store).hasSize(1);
        UUID id = sheets.store.values().iterator().next().getId();

        service.save(PROJET, contenu(Map.of("DEF-04", "NO")));

        assertThat(sheets.store).hasSize(1);
        assertThat(sheets.store.values().iterator().next().getId()).isEqualTo(id);
        assertThat(service.sheet(PROJET).content().answers()).containsEntry("DEF-04", "NO");
        assertThat(journal).containsExactly("saved", "saved");
    }

    @Test
    void lesReponsesAuxQuestionsRetireesNeSontPasRendues() {
        service.save(PROJET, contenu(Map.of("DEF-04", "YES", "MES-02", "Cpk 1,33")));
        service.replaceSettings(
                List.of(new SheetQuestion("MES-02", SheetSection.MEASURE, null, "Performance ?", AnswerType.TEXT)),
                List.of(new FailureType("V10", "Fissure")), List.of(new SeverityLevel(1, "Haute")));

        assertThat(service.sheet(PROJET).content().answers()).containsOnly(Map.entry("MES-02", "Cpk 1,33"));
        assertThat(sheets.store.values().iterator().next().getContent().answers()).containsKey("DEF-04");
    }

    @Test
    void unIshikawaLieDoitExisterChezLeClient() {
        ResolutionContent avecIshikawa = new ResolutionContent(null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, UUID.randomUUID());
        assertThatThrownBy(() -> service.save(PROJET, avecIshikawa)).isInstanceOf(ResolutionValidationException.class)
                .hasMessageContaining("Ishikawa");

        ResolutionContent bon = new ResolutionContent(null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, ISHIKAWA);
        assertThat(service.save(PROJET, bon).content().ishikawaId()).isEqualTo(ISHIKAWA);
    }

    // ---------- photos ----------

    @Test
    void unePhotoVerseeEstStockeeSousUneCleDuClient() {
        ResolutionDto.PhotoView v = service.uploadPhoto(PROJET, 3, "IMAGE/PNG ", PNG, " Vue de face ");

        assertThat(v.slot()).isEqualTo(3);
        assertThat(v.contentType()).isEqualTo("image/png");
        assertThat(v.remark()).isEqualTo("Vue de face");
        assertThat(v.url()).startsWith("https://storage.test/tenants/" + TENANT + "/dmaic/" + PROJET + "/");
        assertThat(v.url()).endsWith(".png");
        assertThat(storage.objects).hasSize(1);
        assertThat(service.sheet(PROJET).photos()).extracting(ResolutionDto.PhotoView::slot).containsExactly(3);
        assertThat(journal).containsExactly("photo+3");
    }

    @Test
    void verserSurUnEmplacementOccupeRemplaceLaPhoto() {
        service.uploadPhoto(PROJET, 1, "image/png", PNG, null);
        String ancienne = storage.objects.keySet().iterator().next();

        service.uploadPhoto(PROJET, 1, "image/png", PNG, "Nouvelle");

        assertThat(storage.objects).hasSize(1).doesNotContainKey(ancienne);
        assertThat(photos.rows).hasSize(1);
        assertThat(journal).containsExactly("photo+1", "photo-1", "photo+1");
    }

    @Test
    void lesGardesDuBinaireSAppliquent() {
        assertThatThrownBy(() -> service.uploadPhoto(PROJET, 1, "image/png", new byte[0], null))
                .isInstanceOf(ResolutionValidationException.class);
        assertThatThrownBy(() -> service.uploadPhoto(PROJET, 1, "image/png", null, null))
                .isInstanceOf(ResolutionValidationException.class);
        assertThatThrownBy(() -> service.uploadPhoto(PROJET, 1, "image/png",
                new byte[(int) SheetPhoto.MAX_SIZE_BYTES + 1], null)).isInstanceOf(ResolutionValidationException.class);
        assertThatThrownBy(() -> service.uploadPhoto(PROJET, 1, "application/pdf", PNG, null))
                .isInstanceOf(ResolutionValidationException.class).hasMessageContaining("JPEG");
        assertThatThrownBy(() -> service.uploadPhoto(PROJET, 1, "image/jpeg", PNG, null))
                .isInstanceOf(ResolutionValidationException.class).hasMessageContaining("does not match");
        assertThatThrownBy(() -> service.uploadPhoto(PROJET, 9, "image/png", PNG, null))
                .isInstanceOf(ResolutionValidationException.class);
        assertThat(storage.objects).isEmpty();
        assertThat(photos.rows).isEmpty();
    }

    @Test
    void laRemarqueSeCorrigeEtLaPhotoSeRetire() {
        service.uploadPhoto(PROJET, 2, "image/png", PNG, null);

        assertThat(service.updateRemark(PROJET, 2, " Bavure ").remark()).isEqualTo("Bavure");
        service.deletePhoto(PROJET, 2);

        assertThat(photos.rows).isEmpty();
        assertThat(storage.objects).isEmpty();
        assertThatThrownBy(() -> service.deletePhoto(PROJET, 2)).isInstanceOf(ResolutionNotFoundException.class);
        assertThatThrownBy(() -> service.updateRemark(PROJET, 2, "x")).isInstanceOf(ResolutionNotFoundException.class);
    }

    @Test
    void stockageEteintLaFicheResteLisibleEtLaPhotoEstRefusee() {
        service.uploadPhoto(PROJET, 1, "image/png", PNG, null);
        storage.on = false;

        assertThat(service.sheet(PROJET).photos()).isEmpty();
        assertThatThrownBy(() -> service.uploadPhoto(PROJET, 2, "image/png", PNG, null))
                .isInstanceOf(StorageDisabledException.class);
    }

    // ---------- rapport client ----------

    @Test
    void leRapportClientReprendLaFicheEtLeProjet() {
        Map<String, String> answers = new HashMap<>();
        answers.put("DEF-01", "Fissure à la base");
        answers.put("DEF-04", "YES");
        answers.put("DEF-12", "UNKNOWN");
        answers.put("MES-01", "NO");
        answers.put("CON-04", "Confiner le stock");
        answers.put("CTL-05", "Audit mensuel du taux de rebut");
        service.save(PROJET, contenu(answers));

        byte[] pdf = service.claimReport(PROJET);

        assertThat(pdf).startsWith('%', 'P', 'D', 'F');
        ClaimReport r = rendu[0];
        assertThat(r.clientName()).isEqualTo("Acme");
        assertThat(r.projectName()).isEqualTo("Fissures carter");
        assertThat(r.ownerName()).isEqualTo("Alice");
        assertThat(r.severity()).isEqualTo("1 - Sévère");
        assertThat(r.failureType()).startsWith("V10 - Defect - Linear");
        // Les trois questions clés de « Définir » du modèle client, avec leur libellé.
        assertThat(r.define()).extracting(ClaimReport.Answer::question).hasSize(3);
        assertThat(r.define()).extracting(ClaimReport.Answer::answer).containsExactly("Fissure à la base", null, null);
        assertThat(r.containmentMeasures()).isEqualTo("Tri");
        assertThat(r.containmentPlan()).isEqualTo("Confiner le stock");
        assertThat(r.bestSolutions()).isNull();
        assertThat(r.surveillance()).isEqualTo("Audit mensuel du taux de rebut");
        assertThat(r.actions()).extracting(ClaimReport.Action::action, ClaimReport.Action::status)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Changer", "Ouverte"));
        assertThat(r.ishikawaCauses()).isEmpty();
    }

    @Test
    void leRapportDUneFicheViergeSortQuandMeme() {
        service.claimReport(PROJET);

        assertThat(rendu[0].clientName()).isNull();
        assertThat(rendu[0].severity()).isNull();
        assertThat(rendu[0].failureType()).isNull();
        assertThat(rendu[0].ownerName()).isNull();
        assertThat(rendu[0].define()).allSatisfy(a -> assertThat(a.answer()).isNull());
        assertThat(rendu[0].whys()).isEmpty();
    }

    @Test
    void unCodeRetireDuCatalogueResteLisibleDansLeRapport() {
        service.save(PROJET, contenu(Map.of()));
        service.replaceSettings(List.of(new SheetQuestion("A", SheetSection.MEASURE, null, "?", AnswerType.TEXT)),
                List.of(), List.of(new SeverityLevel(2, "Moyenne")));

        service.claimReport(PROJET);

        assertThat(rendu[0].severity()).isEqualTo("1");
        assertThat(rendu[0].failureType()).isEqualTo("V10");
    }

    @Test
    void lesCodesDeListeSeLisentEnClair() {
        assertThat(ResolutionService.readable(AnswerType.TEXT, "YES")).isEqualTo("YES");
        assertThat(ResolutionService.readable(AnswerType.YES_NO, "NO")).isEqualTo("Non");
        assertThat(ResolutionService.readable(AnswerType.YES_NO_NA, "NA")).isEqualTo("Sans objet");
        assertThat(ResolutionService.readable(AnswerType.YES_NO, "AUTRE")).isEqualTo("AUTRE");
    }

    // ---------- ports en mémoire ----------

    static final class FakeSheets implements ResolutionPorts.Sheets {
        final Map<UUID, ResolutionSheet> store = new HashMap<>();

        @Override
        public Optional<ResolutionSheet> findByProject(UUID tenantId, UUID projectId) {
            return store.values().stream()
                    .filter(s -> s.getTenantId().equals(tenantId) && s.getProjectId().equals(projectId)).findFirst();
        }

        @Override
        public ResolutionSheet save(ResolutionSheet s) {
            UUID id = s.getId() == null ? UUID.randomUUID() : s.getId();
            ResolutionSheet saved = new ResolutionSheet(id, s.getTenantId(), s.getProjectId(), s.getContent(),
                    s.getCreatedBy(), s.getCreatedAt(), s.getUpdatedAt());
            store.put(id, saved);
            return saved;
        }
    }

    static final class FakeSettings implements ResolutionPorts.Settings {
        final Map<UUID, ResolutionSettings> store = new HashMap<>();
        UUID actor;

        @Override
        public Optional<ResolutionSettings> find(UUID tenantId) {
            return Optional.ofNullable(store.get(tenantId));
        }

        @Override
        public void save(UUID tenantId, ResolutionSettings s, UUID a, Instant now) {
            store.put(tenantId, s);
            actor = a;
        }

        @Override
        public void reset(UUID tenantId) {
            store.remove(tenantId);
        }
    }

    static final class FakePhotos implements ResolutionPorts.Photos {
        final Map<UUID, SheetPhoto> rows = new HashMap<>();

        @Override
        public List<SheetPhoto> list(UUID tenantId, UUID projectId) {
            return rows.values().stream().filter(p -> p.tenantId().equals(tenantId) && p.projectId().equals(projectId))
                    .toList();
        }

        @Override
        public Optional<SheetPhoto> find(UUID tenantId, UUID projectId, int slot) {
            return list(tenantId, projectId).stream().filter(p -> p.slot() == slot).findFirst();
        }

        @Override
        public SheetPhoto save(SheetPhoto p) {
            UUID id = p.id() == null ? UUID.randomUUID() : p.id();
            SheetPhoto saved = new SheetPhoto(id, p.tenantId(), p.projectId(), p.slot(), p.objectKey(),
                    p.contentType(), p.sizeBytes(), p.remark(), p.createdBy(), p.createdAt());
            rows.put(id, saved);
            return saved;
        }

        @Override
        public void delete(SheetPhoto p) {
            rows.remove(p.id());
        }
    }

    static final class FakeStorage implements ResolutionPorts.PhotoStorage {
        final Map<String, byte[]> objects = new HashMap<>();
        boolean on = true;

        @Override
        public boolean available() {
            return on;
        }

        @Override
        public void put(String key, String contentType, byte[] content) {
            if (!on) {
                throw new StorageDisabledException();
            }
            objects.put(key, content);
        }

        @Override
        public URL presignGet(String key) {
            try {
                return URI.create("https://storage.test/" + key).toURL();
            } catch (MalformedURLException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void delete(String key) {
            objects.remove(key);
        }
    }

    // ---------- complétude d'une phase ----------

    @Test
    void unePhaseIncompleteDitCeQuiManque() {
        List<String> vide = service.missing(PROJET, List.of(SheetSection.MEASURE));
        // Trois questions et la synthèse des mesures.
        assertThat(vide).hasSize(4).contains("Synthèse des mesures");

        Map<String, String> answers = new HashMap<>();
        answers.put("MES-01", "YES");
        answers.put("MES-02", "Zéro défaut");
        answers.put("MES-03", "2 %");
        service.save(PROJET, contenu(answers));

        // « Mesures » est rempli dans contenu(...) : la phase est complète.
        assertThat(service.missing(PROJET, List.of(SheetSection.MEASURE))).isEmpty();
        assertThat(service.missing(PROJET, List.of(SheetSection.DEFINE))).hasSize(14);
    }

    @Test
    void leConfinementSeTientParSaSyntheseOuParUneActionDeConfinement() {
        ResolutionContent c = contenu(Map.of());
        ActionLine amelioration = new ActionLine(null, "Changer", null, ActionStatus.OPEN, null, null, 0, null);
        ActionLine confinement = new ActionLine(null, "Trier le stock", new Person(ACTEUR, "Alice"),
                ActionStatus.OPEN, null, null, 0, null, null, true);
        ResolutionContent sansSynthese = new ResolutionContent(c.clientName(), c.claimNumber(), c.problemOwner(),
                c.team(), c.openedOn(), c.closedOn(), c.severity(), c.failureTypeCode(), c.customerSymptoms(),
                c.answers(), c.measureSummary(), null, c.rootCause(), c.retainedSolutions(), c.surveillancePlan(),
                c.whys(), List.of(amelioration), null);
        service.save(PROJET, sansSynthese);
        assertThat(service.missing(PROJET, List.of(SheetSection.CONTAIN))).contains("Action de confinement");

        ResolutionContent avecAction = new ResolutionContent(c.clientName(), c.claimNumber(), c.problemOwner(),
                c.team(), c.openedOn(), c.closedOn(), c.severity(), c.failureTypeCode(), c.customerSymptoms(),
                c.answers(), c.measureSummary(), null, c.rootCause(), c.retainedSolutions(), c.surveillancePlan(),
                c.whys(), List.of(amelioration, confinement), null);
        assertThat(service.save(PROJET, avecAction).content().actions())
                .extracting(ActionLine::containment).containsExactly(false, true);
        assertThat(service.missing(PROJET, List.of(SheetSection.CONTAIN))).doesNotContain("Action de confinement");

        // Le rapport range chaque action à sa place : le confinement d'un côté, le plan de l'autre.
        assertThat(ResolutionService.containment(avecAction)).isEqualTo("• Trier le stock — Alice");
        assertThat(ResolutionService.actions(avecAction)).extracting(ClaimReport.Action::action)
                .containsExactly("Changer");
    }

    @Test
    void lesCasesDesPhasesEtLeLienCapaSontGardes() {
        ResolutionContent c = contenu(Map.of());
        UUID capa = UUID.randomUUID();
        ResolutionContent coche = new ResolutionContent(c.clientName(), c.claimNumber(), c.problemOwner(), c.team(),
                c.openedOn(), c.closedOn(), c.severity(), c.failureTypeCode(), c.customerSymptoms(), c.answers(),
                c.measureSummary(), c.containmentMeasures(), c.rootCause(), c.retainedSolutions(),
                c.surveillancePlan(), c.whys(),
                List.of(new ActionLine(null, "Changer", null, ActionStatus.OPEN, null, null, 0, null, capa)),
                null, true, true, false);

        ResolutionContent vu = service.save(PROJET, coche).content();

        assertThat(vu.measuresEnabled()).isTrue();
        assertThat(vu.capabilityStudy()).isTrue();
        assertThat(vu.gageRrStudy()).isFalse();
        assertThat(vu.actions().get(0).capaId()).isEqualTo(capa);
    }

    @Test
    void unePieceJointeDePhaseAdmetLePdfEtPrendLeNomDuFichier() {
        byte[] pdfBytes = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31};

        ResolutionDto.PhotoView v = service.uploadPhoto(PROJET, 21, "application/pdf", pdfBytes, null,
                "C:\\docs\\plan de mesure.pdf");

        assertThat(v.slot()).isEqualTo(21);
        assertThat(v.remark()).isEqualTo("plan_de_mesure.pdf");
        // Un emplacement de photo refuse toujours le PDF, et 16 n'existe pas.
        assertThatThrownBy(() -> service.uploadPhoto(PROJET, 2, "application/pdf", pdfBytes, null))
                .isInstanceOf(ResolutionValidationException.class);
        assertThatThrownBy(() -> service.uploadPhoto(PROJET, 16, "application/pdf", pdfBytes, null))
                .isInstanceOf(ResolutionValidationException.class);
    }
}
