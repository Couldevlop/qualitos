package com.openlab.qualitos.quality.docs;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Le dépôt du fichier d'une version de document (ADR 0094).
 *
 * <p>Un préfixe à part, {@code /api/v1/document-files}, et non un sous-chemin de
 * {@code /api/v1/documents} : c'est la seule route qui reçoit des fichiers de
 * plusieurs dizaines de mégaoctets. Le proxy lui accorde une taille de corps que
 * le reste de l'API n'a pas, sur ce seul préfixe — un chemin à identifiants
 * variables ne se distinguerait pas sans expressions régulières, qui
 * s'appliqueraient alors à tout l'hôte.
 */
@RestController
@RequestMapping("/api/v1/document-files")
public class DocumentFileController {

    private final DocumentFileService files;

    public DocumentFileController(DocumentFileService files) {
        this.files = files;
    }

    @PostMapping(value = "/{documentId}/versions/{versionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiresPermission(Permission.DOCUMENT_EDIT)
    public DocumentDto.VersionResponse upload(@PathVariable UUID documentId, @PathVariable UUID versionId,
                                              @RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new DocumentFileRejectedException("Missing or empty 'file' part");
        }
        return files.upload(documentId, versionId, file.getContentType(), file.getOriginalFilename(),
                file.getBytes());
    }
}
