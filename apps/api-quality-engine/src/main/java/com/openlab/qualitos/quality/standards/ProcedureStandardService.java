package com.openlab.qualitos.quality.standards;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.docs.Document;
import com.openlab.qualitos.quality.docs.DocumentNotFoundException;
import com.openlab.qualitos.quality.docs.DocumentRepository;
import com.openlab.qualitos.quality.docs.DocumentType;
import com.openlab.qualitos.quality.docs.DocumentVersion;
import com.openlab.qualitos.quality.docs.DocumentVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Écriture des référentiels appartenant à un tenant (§8).
 *
 * <p>Séparé de {@link StandardsService}, qui ne fait que LIRE le catalogue livré
 * et piloter les adoptions : ces deux responsabilités n'ont ni les mêmes règles
 * d'accès, ni le même cycle de vie, et les mêler rendrait impossible de dire, à
 * la lecture, ce qui peut modifier le catalogue.
 */
@Service
@Transactional
public class ProcedureStandardService {

    static final String FAMILY = "INTERNAL_PROCEDURE";

    private final StandardRepository standards;
    private final DocumentRepository documents;
    private final DocumentVersionRepository versions;

    public ProcedureStandardService(StandardRepository standards,
                                    DocumentRepository documents,
                                    DocumentVersionRepository versions) {
        this.standards = standards;
        this.documents = documents;
        this.versions = versions;
    }

    /**
     * Crée un référentiel VIDE à partir d'une procédure approuvée.
     *
     * <p>Vide, et non pré-rempli : les clauses sont celles de la procédure du
     * tenant, que lui seul connaît. Les deviner produirait un référentiel
     * plausible et faux — le pire des deux mondes pour un audit.
     */
    public Standard createFromDocument(UUID documentId) {
        UUID tenantId = requireTenantId();
        Document doc = documents.findByIdAndTenantId(documentId, tenantId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        if (doc.getType() != DocumentType.PROCEDURE) {
            throw new ProcedureSourceException(
                    "Seule une procédure peut servir de référentiel d'audit");
        }
        // currentVersionId n'est renseigné que par la publication d'une version
        // APPROUVÉE : son absence signe un document resté à l'état de brouillon.
        if (doc.getCurrentVersionId() == null) {
            throw new ProcedureSourceException(
                    "Cette procédure doit être approuvée avant de servir de référentiel");
        }
        DocumentVersion published = versions
                .findByIdAndDocumentId(doc.getCurrentVersionId(), documentId)
                .orElseThrow(() -> new ProcedureSourceException(
                        "La version publiée de cette procédure est introuvable"));
        if (standards.existsBySourceDocument(documentId)) {
            throw new StandardCodeConflictException(
                    "Un référentiel existe déjà pour cette procédure");
        }

        Standard s = new Standard();
        s.setOwnerTenantId(tenantId);
        s.setSourceDocumentId(documentId);
        // Figée : la procédure continuera d'évoluer, l'audit doit rester
        // rattachable à la version contre laquelle il a été mené.
        s.setSourceDocumentVersion(published.getVersionNumber());
        s.setCode(doc.getCode());
        s.setFullName(doc.getTitle());
        s.setCurrentVersion("v" + published.getVersionNumber());
        s.setFamily(FAMILY);
        s.setCertificationBodyRequired(false);
        s.setStatus(StandardStatus.PUBLISHED);
        return standards.save(s);
    }

    UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }
}
