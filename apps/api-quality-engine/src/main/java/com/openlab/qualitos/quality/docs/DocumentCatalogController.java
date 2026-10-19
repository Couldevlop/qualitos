package com.openlab.qualitos.quality.docs;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * L'arborescence, les processus et le paramétrage de la GED (ADR 0094).
 *
 * <p>Lire l'arborescence, les processus et ce que le dépôt admet est ouvert à tout
 * membre du client : la liste et les formulaires en ont besoin. Les modifier, comme
 * lire le paramétrage et l'occupation du stockage, est réservé à l'administrateur
 * du client ({@code authz.manage}).
 */
@RestController
@RequestMapping("/api/v1/documents")
@PreAuthorize("isAuthenticated()")
public class DocumentCatalogController {

    private final DocumentCatalogService catalog;
    private final DocumentFileService files;

    public DocumentCatalogController(DocumentCatalogService catalog, DocumentFileService files) {
        this.catalog = catalog;
        this.files = files;
    }

    // ---------- arborescence ----------

    @GetMapping("/folders")
    public List<DocumentDto.FolderView> folders() {
        return catalog.folders();
    }

    @PostMapping("/folders")
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDto.FolderView createFolder(@Valid @RequestBody DocumentDto.FolderRequest req) {
        return catalog.createFolder(req);
    }

    @PutMapping("/folders/{id}")
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    public DocumentDto.FolderView updateFolder(@PathVariable UUID id, @Valid @RequestBody DocumentDto.FolderRequest req) {
        return catalog.updateFolder(id, req);
    }

    @DeleteMapping("/folders/{id}")
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFolder(@PathVariable UUID id) {
        catalog.deleteFolder(id);
    }

    // ---------- processus ----------

    @GetMapping("/processes")
    public List<DocumentDto.ProcessView> processes() {
        return catalog.processes();
    }

    @PostMapping("/processes")
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDto.ProcessView createProcess(@Valid @RequestBody DocumentDto.ProcessRequest req) {
        return catalog.createProcess(req);
    }

    @PutMapping("/processes/{id}")
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    public DocumentDto.ProcessView updateProcess(@PathVariable UUID id, @Valid @RequestBody DocumentDto.ProcessRequest req) {
        return catalog.updateProcess(id, req);
    }

    @DeleteMapping("/processes/{id}")
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProcess(@PathVariable UUID id) {
        catalog.deleteProcess(id);
    }

    // ---------- paramétrage ----------

    @GetMapping("/upload-policy")
    public DocumentDto.UploadPolicy uploadPolicy() {
        return catalog.uploadPolicy();
    }

    @GetMapping("/settings")
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    public DocumentDto.SettingsView settings() {
        return catalog.settings();
    }

    @PutMapping("/settings")
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    public DocumentDto.SettingsView replaceSettings(@Valid @RequestBody DocumentDto.SettingsForm form) {
        return catalog.replaceSettings(form);
    }

    @GetMapping("/storage")
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    public DocumentDto.StorageUsage storage() {
        return files.usage();
    }
}
