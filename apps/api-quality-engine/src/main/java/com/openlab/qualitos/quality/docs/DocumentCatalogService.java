package com.openlab.qualitos.quality.docs;

import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * L'arborescence, les processus et le paramétrage de la GED d'un client (ADR 0094).
 *
 * <p>Tout le monde lit l'arborescence et les processus : la liste en a besoin. Seul
 * l'administrateur du client les modifie, comme le paramétrage (droit
 * {@code authz.manage}, posé sur le contrôleur).
 */
@Service
@Transactional
public class DocumentCatalogService {

    private static final Logger log = LoggerFactory.getLogger(DocumentCatalogService.class);

    /** Au-delà, l'arborescence devient un labyrinthe que personne ne parcourt. */
    static final int MAX_DEPTH = 4;

    private final DocumentFolderRepository folders;
    private final DocumentProcessRepository processes;
    private final DocumentSettingsRowRepository settings;
    private final DocumentRepository documents;
    private final DocumentCatalogSeeder seeder;

    public DocumentCatalogService(DocumentFolderRepository folders,
                                  DocumentProcessRepository processes,
                                  DocumentSettingsRowRepository settings,
                                  DocumentRepository documents,
                                  DocumentCatalogSeeder seeder) {
        this.folders = folders;
        this.processes = processes;
        this.settings = settings;
        this.documents = documents;
        this.seeder = seeder;
    }

    // ---------- paramétrage ----------

    /** Le paramétrage du client ; celui de la plateforme tant qu'il n'a rien réglé. */
    @Transactional(readOnly = true)
    public DocumentSettingsRow settingsOf(UUID tenantId) {
        return settings.findById(tenantId).orElseGet(() -> DocumentSettingsRow.defaults(tenantId));
    }

    @Transactional(readOnly = true)
    public DocumentDto.SettingsView settings() {
        return toView(settingsOf(requireTenantId()));
    }

    public DocumentDto.SettingsView replaceSettings(DocumentDto.SettingsForm form) {
        UUID tenantId = requireTenantId();
        ensureSeeded(tenantId);
        DocumentSettingsRow row = settings.findById(tenantId)
                .orElseGet(() -> DocumentSettingsRow.defaults(tenantId));
        row.setReviewPeriodMonths(form.reviewPeriodMonths());
        row.setReviewWarningDays(form.reviewWarningDays());
        row.setMaxFileMb(form.maxFileMb());
        row.setQuotaMb(form.quotaMb());
        row.setObsoleteRetentionYears(form.obsoleteRetentionYears());
        row.setUpdatedBy(CurrentUser.requireUserId());
        return toView(settings.save(row));
    }

    @Transactional(readOnly = true)
    public DocumentDto.UploadPolicy uploadPolicy() {
        DocumentSettingsRow s = settingsOf(requireTenantId());
        return new DocumentDto.UploadPolicy(s.getMaxFileMb(),
                List.copyOf(DocumentFiles.ALLOWED_TYPES.keySet()),
                DocumentFiles.ALLOWED_TYPES.values().stream().distinct().toList());
    }

    // ---------- arborescence ----------

    public List<DocumentDto.FolderView> folders() {
        UUID tenantId = requireTenantId();
        ensureSeeded(tenantId);
        return folders.findByTenantIdOrderByPositionAscNameAsc(tenantId).stream().map(DocumentCatalogService::toView).toList();
    }

    public DocumentDto.FolderView createFolder(DocumentDto.FolderRequest req) {
        UUID tenantId = requireTenantId();
        ensureSeeded(tenantId);
        List<DocumentFolder> all = folders.findByTenantIdOrderByPositionAscNameAsc(tenantId);
        String name = req.name().strip();
        if (req.parentId() != null) {
            requireFolder(all, req.parentId());
            if (depthOf(all, req.parentId()) + 1 > MAX_DEPTH) {
                throw new DocumentStateException("A folder tree is at most " + MAX_DEPTH + " levels deep");
            }
        }
        requireUniqueName(all, req.parentId(), name, null);
        DocumentFolder f = new DocumentFolder();
        f.setTenantId(tenantId);
        f.setParentId(req.parentId());
        f.setName(name);
        f.setPosition(req.position() != null ? req.position() : nextPosition(all, req.parentId()));
        return toView(folders.save(f));
    }

    public DocumentDto.FolderView updateFolder(UUID id, DocumentDto.FolderRequest req) {
        UUID tenantId = requireTenantId();
        List<DocumentFolder> all = folders.findByTenantIdOrderByPositionAscNameAsc(tenantId);
        DocumentFolder f = requireFolder(all, id);
        String name = req.name().strip();
        if (!Objects.equals(f.getParentId(), req.parentId())) {
            if (req.parentId() != null) {
                requireFolder(all, req.parentId());
                if (id.equals(req.parentId()) || descendantsOf(all, id).contains(req.parentId())) {
                    throw new DocumentStateException("A folder cannot be moved under itself");
                }
                if (depthOf(all, req.parentId()) + heightOf(all, id) > MAX_DEPTH) {
                    throw new DocumentStateException("A folder tree is at most " + MAX_DEPTH + " levels deep");
                }
            }
            f.setParentId(req.parentId());
        }
        requireUniqueName(all, req.parentId(), name, id);
        f.setName(name);
        if (req.position() != null) {
            f.setPosition(req.position());
        }
        return toView(folders.save(f));
    }

    /** Un dossier ne se supprime que vide : ni sous-dossier, ni document. */
    public void deleteFolder(UUID id) {
        UUID tenantId = requireTenantId();
        DocumentFolder f = folders.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new DocumentCatalogNotFoundException("Folder", id));
        if (folders.existsByTenantIdAndParentId(tenantId, id)) {
            throw new DocumentStateException("The folder still has sub-folders: empty it first");
        }
        if (documents.existsByTenantIdAndFolderId(tenantId, id)) {
            throw new DocumentStateException("The folder still holds documents: move them first");
        }
        folders.delete(f);
    }

    /**
     * Le dossier et tous ses descendants : cliquer « Procédures » montre aussi les
     * procédures rangées dans ses sous-dossiers.
     */
    @Transactional(readOnly = true)
    public Set<UUID> folderAndDescendants(UUID tenantId, UUID folderId) {
        List<DocumentFolder> all = folders.findByTenantIdOrderByPositionAscNameAsc(tenantId);
        requireFolder(all, folderId);
        Set<UUID> ids = descendantsOf(all, folderId);
        ids.add(folderId);
        return ids;
    }

    @Transactional(readOnly = true)
    public void requireFolderOf(UUID tenantId, UUID folderId) {
        folders.findByIdAndTenantId(folderId, tenantId)
                .orElseThrow(() -> new DocumentCatalogNotFoundException("Folder", folderId));
    }

    // ---------- processus ----------

    public List<DocumentDto.ProcessView> processes() {
        UUID tenantId = requireTenantId();
        ensureSeeded(tenantId);
        return processes.findByTenantIdOrderByPositionAscNameAsc(tenantId).stream()
                .map(DocumentCatalogService::toView).toList();
    }

    public DocumentDto.ProcessView createProcess(DocumentDto.ProcessRequest req) {
        UUID tenantId = requireTenantId();
        ensureSeeded(tenantId);
        String name = req.name().strip();
        if (processes.existsByTenantIdAndNameIgnoreCase(tenantId, name)) {
            throw new DocumentStateException("A process with this name already exists");
        }
        List<DocumentProcess> all = processes.findByTenantIdOrderByPositionAscNameAsc(tenantId);
        DocumentProcess p = new DocumentProcess();
        p.setTenantId(tenantId);
        p.setName(name);
        p.setPosition(req.position() != null ? req.position()
                : all.stream().mapToInt(DocumentProcess::getPosition).max().orElse(-1) + 1);
        return toView(processes.save(p));
    }

    public DocumentDto.ProcessView updateProcess(UUID id, DocumentDto.ProcessRequest req) {
        UUID tenantId = requireTenantId();
        DocumentProcess p = processes.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new DocumentCatalogNotFoundException("Process", id));
        String name = req.name().strip();
        if (!p.getName().equalsIgnoreCase(name) && processes.existsByTenantIdAndNameIgnoreCase(tenantId, name)) {
            throw new DocumentStateException("A process with this name already exists");
        }
        p.setName(name);
        if (req.position() != null) {
            p.setPosition(req.position());
        }
        return toView(processes.save(p));
    }

    /** Un processus auquel des documents se rattachent ne se supprime pas : il se renomme. */
    public void deleteProcess(UUID id) {
        UUID tenantId = requireTenantId();
        DocumentProcess p = processes.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new DocumentCatalogNotFoundException("Process", id));
        if (documents.existsByTenantIdAndProcessId(tenantId, id)) {
            throw new DocumentStateException("Documents are attached to this process: reassign them first");
        }
        processes.delete(p);
    }

    @Transactional(readOnly = true)
    public void requireProcessOf(UUID tenantId, UUID processId) {
        processes.findByIdAndTenantId(processId, tenantId)
                .orElseThrow(() -> new DocumentCatalogNotFoundException("Process", processId));
    }

    // ---------- outils ----------

    /**
     * Sème l'arborescence et les processus de départ au premier usage. Un échec de
     * concurrence veut dire qu'une autre requête vient de le faire : on continue.
     */
    void ensureSeeded(UUID tenantId) {
        if (settings.existsById(tenantId)) {
            return;
        }
        try {
            seeder.seed(tenantId);
        } catch (DataIntegrityViolationException e) {
            log.debug("[documents] arborescence déjà semée par une requête concurrente");
        }
    }

    private static DocumentFolder requireFolder(List<DocumentFolder> all, UUID id) {
        return all.stream().filter(f -> f.getId().equals(id)).findFirst()
                .orElseThrow(() -> new DocumentCatalogNotFoundException("Folder", id));
    }

    private static void requireUniqueName(List<DocumentFolder> all, UUID parentId, String name, UUID self) {
        boolean taken = all.stream().anyMatch(f -> Objects.equals(f.getParentId(), parentId)
                && f.getName().equalsIgnoreCase(name) && !f.getId().equals(self));
        if (taken) {
            throw new DocumentStateException("A folder with this name already exists here");
        }
    }

    private static int nextPosition(List<DocumentFolder> all, UUID parentId) {
        return all.stream().filter(f -> Objects.equals(f.getParentId(), parentId))
                .mapToInt(DocumentFolder::getPosition).max().orElse(-1) + 1;
    }

    /** Le niveau d'un dossier : 1 à la racine. */
    static int depthOf(List<DocumentFolder> all, UUID id) {
        Map<UUID, UUID> parents = new HashMap<>();
        all.forEach(f -> parents.put(f.getId(), f.getParentId()));
        int depth = 0;
        Set<UUID> seen = new HashSet<>();
        for (UUID cur = id; cur != null && seen.add(cur); cur = parents.get(cur)) {
            depth++;
        }
        return depth;
    }

    /** Le nombre de niveaux qu'occupe un dossier avec sa descendance : 1 s'il est une feuille. */
    static int heightOf(List<DocumentFolder> all, UUID id) {
        int best = 1;
        for (DocumentFolder child : all) {
            if (id.equals(child.getParentId())) {
                best = Math.max(best, 1 + heightOf(all, child.getId()));
            }
        }
        return best;
    }

    static Set<UUID> descendantsOf(List<DocumentFolder> all, UUID id) {
        Set<UUID> out = new HashSet<>();
        Deque<UUID> todo = new ArrayDeque<>();
        todo.push(id);
        while (!todo.isEmpty()) {
            UUID cur = todo.pop();
            for (DocumentFolder f : all) {
                if (cur.equals(f.getParentId()) && out.add(f.getId())) {
                    todo.push(f.getId());
                }
            }
        }
        return out;
    }

    private static UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    private static DocumentDto.SettingsView toView(DocumentSettingsRow s) {
        return new DocumentDto.SettingsView(s.getReviewPeriodMonths(), s.getReviewWarningDays(), s.getMaxFileMb(),
                DocumentSettingsRow.PLATFORM_MAX_FILE_MB, s.getQuotaMb(), s.getObsoleteRetentionYears());
    }

    private static DocumentDto.FolderView toView(DocumentFolder f) {
        return new DocumentDto.FolderView(f.getId(), f.getParentId(), f.getName(), f.getPosition());
    }

    private static DocumentDto.ProcessView toView(DocumentProcess p) {
        return new DocumentDto.ProcessView(p.getId(), p.getName(), p.getPosition());
    }
}
