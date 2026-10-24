package com.openlab.qualitos.quality.dmaic.resolution.application;

import com.openlab.qualitos.quality.dmaic.resolution.domain.AnswerType;
import com.openlab.qualitos.quality.dmaic.resolution.domain.DefineTopic;
import com.openlab.qualitos.quality.dmaic.resolution.domain.FailureType;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionContent;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionNotFoundException;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionSettings;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionSheet;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionValidationException;
import com.openlab.qualitos.quality.dmaic.resolution.domain.SeverityLevel;
import com.openlab.qualitos.quality.dmaic.resolution.domain.SheetPhoto;
import com.openlab.qualitos.quality.dmaic.resolution.domain.SheetQuestion;
import com.openlab.qualitos.quality.dmaic.resolution.domain.SheetSection;
import com.openlab.qualitos.quality.nonconformity.storage.UploadedBinaryGuard;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * La fiche de résolution DMAIC (classeur « DMAIC Résolution de Problème ») et
 * son paramétrage par client.
 *
 * <p>Classe simple, sans Spring : le câblage est dans l'infrastructure, les
 * transactions sur le contrôleur.
 */
public class ResolutionService {

    private final ResolutionPorts.Context context;
    private final ResolutionPorts.Projects projects;
    private final ResolutionPorts.Sheets sheets;
    private final ResolutionPorts.Settings settingsStore;
    private final ResolutionPorts.Photos photos;
    private final ResolutionPorts.PhotoStorage storage;
    private final ResolutionPorts.Ishikawas ishikawas;
    private final ResolutionPorts.Audit audit;
    private final ResolutionPorts.ClaimReportRenderer renderer;
    private final Clock clock;

    @SuppressWarnings("java:S107") // un port par responsabilité : c'est le prix de l'isolation
    public ResolutionService(ResolutionPorts.Context context, ResolutionPorts.Projects projects,
                             ResolutionPorts.Sheets sheets, ResolutionPorts.Settings settingsStore,
                             ResolutionPorts.Photos photos, ResolutionPorts.PhotoStorage storage,
                             ResolutionPorts.Ishikawas ishikawas, ResolutionPorts.Audit audit,
                             ResolutionPorts.ClaimReportRenderer renderer, Clock clock) {
        this.context = context;
        this.projects = projects;
        this.sheets = sheets;
        this.settingsStore = settingsStore;
        this.photos = photos;
        this.storage = storage;
        this.ishikawas = ishikawas;
        this.audit = audit;
        this.renderer = renderer;
        this.clock = clock;
    }

    // ---------- paramétrage ----------

    public ResolutionDto.SettingsView settings() {
        return view(settingsOf(context.requireTenantId()));
    }

    public ResolutionDto.SettingsView replaceSettings(List<SheetQuestion> questions, List<FailureType> failureTypes,
                                                      List<SeverityLevel> severities) {
        UUID tenant = context.requireTenantId();
        UUID actor = context.requireActorId();
        ResolutionSettings settings = ResolutionSettings.configured(questions, failureTypes, severities);
        settingsStore.save(tenant, settings, actor, clock.instant());
        audit.settingsReplaced(tenant, settings, actor);
        return view(settings);
    }

    public ResolutionDto.SettingsView resetSettings() {
        UUID tenant = context.requireTenantId();
        UUID actor = context.requireActorId();
        settingsStore.reset(tenant);
        audit.settingsReset(tenant, actor);
        return view(ResolutionSettings.platformDefaults());
    }

    // ---------- fiche ----------

    public ResolutionDto.SheetView sheet(UUID projectId) {
        UUID tenant = context.requireTenantId();
        ResolutionPorts.ProjectHeader project = project(tenant, projectId);
        ResolutionSettings settings = settingsOf(tenant);
        return sheets.findByProject(tenant, projectId)
                .map(s -> view(project, s, true, settings, tenant))
                .orElseGet(() -> view(project, ResolutionSheet.blank(tenant, projectId, null, null), false,
                        settings, tenant));
    }

    public ResolutionDto.SheetView save(UUID projectId, ResolutionContent saisie) {
        UUID tenant = context.requireTenantId();
        UUID actor = context.requireActorId();
        ResolutionPorts.ProjectHeader project = project(tenant, projectId);
        ResolutionSettings settings = settingsOf(tenant);
        if (saisie.ishikawaId() != null && !ishikawas.exists(tenant, saisie.ishikawaId())) {
            throw new ResolutionValidationException("ishikawaId", "Unknown Ishikawa diagram");
        }
        Instant now = clock.instant();
        ResolutionSheet sheet = sheets.findByProject(tenant, projectId)
                .orElseGet(() -> ResolutionSheet.blank(tenant, projectId, actor, now));
        sheet.revise(saisie, settings, now);
        ResolutionSheet saved = sheets.save(sheet);
        audit.sheetSaved(saved, actor);
        return view(project, saved, true, settings, tenant);
    }

    // ---------- photos ----------

    /**
     * Verse une photo sur un emplacement ; une photo déjà présente y est
     * remplacée. La ligne est écrite avant le binaire : si le dépôt échoue, la
     * transaction annule la ligne et rien d'orphelin ne reste en base.
     */
    public ResolutionDto.PhotoView uploadPhoto(UUID projectId, int slot, String contentType, byte[] content,
                                               String remark) {
        UUID tenant = context.requireTenantId();
        UUID actor = context.requireActorId();
        project(tenant, projectId);
        SheetPhoto.checkSlot(slot);
        if (content == null || content.length == 0) {
            throw new ResolutionValidationException("file", "Empty photo");
        }
        if (content.length > SheetPhoto.MAX_SIZE_BYTES) {
            throw new ResolutionValidationException("file",
                    "A photo is at most " + (SheetPhoto.MAX_SIZE_BYTES / (1024 * 1024)) + " MB");
        }
        String type = UploadedBinaryGuard.normalizeType(contentType);
        String extension = UploadedBinaryGuard.extensionOf(SheetPhoto.ALLOWED_TYPES, type);
        if (extension == null) {
            throw new ResolutionValidationException("file", "Allowed photo types: JPEG, PNG, GIF, WEBP");
        }
        if (!UploadedBinaryGuard.magicBytesMatch(type, content)) {
            throw new ResolutionValidationException("file", "The file content does not match its declared type");
        }
        String cleanRemark = SheetPhoto.cleanRemark(remark);
        photos.find(tenant, projectId, slot).ifPresent(previous -> {
            photos.delete(previous);
            storage.delete(previous.objectKey());
            audit.photoRemoved(previous, actor);
        });
        String key = "tenants/" + tenant + "/dmaic/" + projectId + "/" + UUID.randomUUID() + "." + extension;
        SheetPhoto saved = photos.save(new SheetPhoto(null, tenant, projectId, slot, key, type, content.length,
                cleanRemark, actor, clock.instant()));
        storage.put(key, type, content);
        audit.photoAdded(saved, actor);
        return photoView(saved);
    }

    public ResolutionDto.PhotoView updateRemark(UUID projectId, int slot, String remark) {
        UUID tenant = context.requireTenantId();
        project(tenant, projectId);
        SheetPhoto.checkSlot(slot);
        SheetPhoto photo = photo(tenant, projectId, slot);
        return photoView(photos.save(photo.withRemark(remark)));
    }

    public void deletePhoto(UUID projectId, int slot) {
        UUID tenant = context.requireTenantId();
        UUID actor = context.requireActorId();
        project(tenant, projectId);
        SheetPhoto.checkSlot(slot);
        SheetPhoto photo = photo(tenant, projectId, slot);
        photos.delete(photo);
        storage.delete(photo.objectKey());
        audit.photoRemoved(photo, actor);
    }

    // ---------- rapport client ----------

    public byte[] claimReport(UUID projectId) {
        UUID tenant = context.requireTenantId();
        ResolutionPorts.ProjectHeader project = project(tenant, projectId);
        ResolutionSettings settings = settingsOf(tenant);
        ResolutionContent c = sheets.findByProject(tenant, projectId)
                .map(ResolutionSheet::getContent)
                .orElseGet(ResolutionContent::empty);
        return renderer.render(new ClaimReport(c.clientName(), c.claimNumber(), project.title(),
                c.problemOwner() == null ? null : c.problemOwner().name(), c.openedOn(), c.closedOn(),
                severityLabel(settings, c.severity()), failureTypeLabel(settings, c.failureTypeCode()),
                c.customerSymptoms(), definition(settings, c), project.problemStatement(), c.measureSummary(),
                c.containmentMeasures(), c.rootCause(), c.retainedSolutions(), c.surveillancePlan()));
    }

    /** Rubrique par rubrique, les questions de « Définir » qui ont une réponse. */
    static List<ClaimReport.Topic> definition(ResolutionSettings settings, ResolutionContent c) {
        List<ClaimReport.Topic> topics = new ArrayList<>();
        for (DefineTopic topic : DefineTopic.values()) {
            List<ClaimReport.Answer> answers = new ArrayList<>();
            for (SheetQuestion q : settings.questions()) {
                String value = c.answers().get(q.code());
                if (q.section() == SheetSection.DEFINE && q.topic() == topic && value != null) {
                    answers.add(new ClaimReport.Answer(q.label(), readable(q.answerType(), value)));
                }
            }
            topics.add(new ClaimReport.Topic(topic, answers));
        }
        return topics;
    }

    /** Le code d'une liste fermée, en clair pour le client. */
    static String readable(AnswerType type, String value) {
        if (type == AnswerType.TEXT) {
            return value;
        }
        return switch (value) {
            case "YES" -> "Oui";
            case "NO" -> "Non";
            case "NA" -> "Sans objet";
            case "UNKNOWN" -> "Inconnu";
            default -> value;
        };
    }

    private static String severityLabel(ResolutionSettings settings, Integer level) {
        if (level == null) {
            return null;
        }
        return settings.severities().stream().filter(s -> s.level() == level)
                .map(s -> s.level() + " - " + s.label()).findFirst().orElse(String.valueOf(level));
    }

    private static String failureTypeLabel(ResolutionSettings settings, String code) {
        if (code == null) {
            return null;
        }
        return settings.failureTypes().stream().filter(f -> f.code().equals(code))
                .map(f -> f.code() + " - " + f.label()).findFirst().orElse(code);
    }

    // ---------- outils ----------

    private ResolutionSettings settingsOf(UUID tenant) {
        return settingsStore.find(tenant).orElseGet(ResolutionSettings::platformDefaults);
    }

    private ResolutionPorts.ProjectHeader project(UUID tenant, UUID projectId) {
        return projects.find(tenant, projectId)
                .orElseThrow(() -> new ResolutionNotFoundException("DMAIC project not found: " + projectId));
    }

    private SheetPhoto photo(UUID tenant, UUID projectId, int slot) {
        return photos.find(tenant, projectId, slot)
                .orElseThrow(() -> new ResolutionNotFoundException("No photo in slot " + slot));
    }

    private ResolutionDto.SheetView view(ResolutionPorts.ProjectHeader project, ResolutionSheet sheet, boolean saved,
                                         ResolutionSettings settings, UUID tenant) {
        ResolutionContent c = sheet.getContent();
        Map<String, String> posees = new HashMap<>();
        c.answers().forEach((code, value) -> {
            if (settings.question(code).isPresent()) {
                posees.put(code, value);
            }
        });
        ResolutionContent visible = new ResolutionContent(c.clientName(), c.claimNumber(), c.problemOwner(),
                c.team(), c.openedOn(), c.closedOn(), c.severity(), c.failureTypeCode(), c.customerSymptoms(),
                posees, c.measureSummary(), c.containmentMeasures(), c.rootCause(), c.retainedSolutions(),
                c.surveillancePlan(), c.whys(), c.actions(), c.ishikawaId());
        // Stockage éteint : la fiche reste lisible, sans ses photos, plutôt que de
        // répondre 503 pour un complément.
        List<ResolutionDto.PhotoView> photoViews = storage.available()
                ? photos.list(tenant, project.id()).stream().map(this::photoView).toList()
                : List.of();
        return new ResolutionDto.SheetView(project.id(), project.title(), project.problemStatement(), saved,
                visible, view(settings), photoViews, sheet.getUpdatedAt());
    }

    private ResolutionDto.PhotoView photoView(SheetPhoto p) {
        return new ResolutionDto.PhotoView(p.slot(), p.contentType(), p.sizeBytes(), p.remark(),
                storage.presignGet(p.objectKey()).toString());
    }

    private static ResolutionDto.SettingsView view(ResolutionSettings s) {
        return new ResolutionDto.SettingsView(s.questions(), s.failureTypes(), s.severities(), s.defaults());
    }
}
