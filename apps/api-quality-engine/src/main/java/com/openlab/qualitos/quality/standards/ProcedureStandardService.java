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
    private final TenantStandardRepository adoptions;

    public ProcedureStandardService(StandardRepository standards,
                                    DocumentRepository documents,
                                    DocumentVersionRepository versions,
                                    TenantStandardRepository adoptions) {
        this.standards = standards;
        this.documents = documents;
        this.versions = versions;
        this.adoptions = adoptions;
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

    /**
     * Supprime un référentiel du tenant, avec toute son arborescence.
     *
     * <p>Refusé tant qu'une adoption existe : ce n'est pas une précaution de
     * confort. L'adoption porte les preuves rattachées aux exigences et le score
     * d'alignement du projet de certification ; les effacer en cascade ferait
     * disparaître un dossier d'audit sans que personne ne l'ait demandé.
     */
    public void deleteStandard(UUID standardId) {
        Standard s = editable(standardId);
        if (adoptions.existsByStandardId(standardId)) {
            throw new AdoptionConflictException(
                    "Ce référentiel est suivi par un projet de conformité : retirez l'adoption d'abord");
        }
        standards.delete(s);
    }

    // ---- Édition de l'arborescence : Sections → Clauses → Exigences ----

    public void addSection(UUID standardId, ProcedureStandardDto.SectionRequest req) {
        Standard s = editable(standardId);
        requireFreeCode(s.getSections(), StandardSection::getCode, req.code(), null, "section");

        StandardSection section = new StandardSection();
        section.setStandard(s);
        section.setCode(req.code());
        section.setTitle(req.title());
        section.setDescription(req.description());
        section.setOrderIndex(s.getSections().size());
        s.getSections().add(section);
        standards.save(s);
    }

    public void updateSection(UUID standardId, UUID sectionId, ProcedureStandardDto.SectionRequest req) {
        Standard s = editable(standardId);
        StandardSection section = section(s, sectionId);
        requireFreeCode(s.getSections(), StandardSection::getCode, req.code(), section, "section");

        section.setCode(req.code());
        section.setTitle(req.title());
        section.setDescription(req.description());
        standards.save(s);
    }

    /** La section emporte ses clauses, qui emportent leurs exigences ({@code orphanRemoval}). */
    public void deleteSection(UUID standardId, UUID sectionId) {
        Standard s = editable(standardId);
        s.getSections().remove(section(s, sectionId));
        standards.save(s);
    }

    public void addClause(UUID standardId, UUID sectionId, ProcedureStandardDto.ClauseRequest req) {
        Standard s = editable(standardId);
        StandardSection section = section(s, sectionId);
        requireFreeCode(section.getClauses(), StandardClause::getCode, req.code(), null, "clause");

        StandardClause clause = new StandardClause();
        clause.setSection(section);
        clause.setCode(req.code());
        clause.setTitle(req.title());
        clause.setDescription(req.description());
        clause.setOrderIndex(section.getClauses().size());
        section.getClauses().add(clause);
        standards.save(s);
    }

    public void updateClause(UUID standardId, UUID clauseId, ProcedureStandardDto.ClauseRequest req) {
        Standard s = editable(standardId);
        StandardClause clause = clause(s, clauseId);
        requireFreeCode(clause.getSection().getClauses(), StandardClause::getCode,
                req.code(), clause, "clause");

        clause.setCode(req.code());
        clause.setTitle(req.title());
        clause.setDescription(req.description());
        standards.save(s);
    }

    public void deleteClause(UUID standardId, UUID clauseId) {
        Standard s = editable(standardId);
        StandardClause clause = clause(s, clauseId);
        clause.getSection().getClauses().remove(clause);
        standards.save(s);
    }

    public void addRequirement(UUID standardId, UUID clauseId,
                               ProcedureStandardDto.RequirementRequest req) {
        Standard s = editable(standardId);
        StandardClause clause = clause(s, clauseId);
        requireFreeCode(clause.getRequirements(), StandardRequirement::getCode, req.code(), null, "exigence");

        StandardRequirement requirement = new StandardRequirement();
        requirement.setClause(clause);
        requirement.setOrderIndex(clause.getRequirements().size());
        apply(requirement, req);
        clause.getRequirements().add(requirement);
        standards.save(s);
    }

    public void updateRequirement(UUID standardId, UUID requirementId,
                                  ProcedureStandardDto.RequirementRequest req) {
        Standard s = editable(standardId);
        StandardRequirement requirement = requirement(s, requirementId);
        requireFreeCode(requirement.getClause().getRequirements(), StandardRequirement::getCode,
                req.code(), requirement, "exigence");

        apply(requirement, req);
        standards.save(s);
    }

    public void deleteRequirement(UUID standardId, UUID requirementId) {
        Standard s = editable(standardId);
        StandardRequirement requirement = requirement(s, requirementId);
        requirement.getClause().getRequirements().remove(requirement);
        standards.save(s);
    }

    private void apply(StandardRequirement requirement, ProcedureStandardDto.RequirementRequest req) {
        requirement.setCode(req.code());
        requirement.setText(req.text());
        requirement.setObligation(req.obligation());
        requirement.setEvidenceTypes(req.evidenceTypes());
        requirement.setMeasurableCriteria(req.measurableCriteria());
        requirement.setRiskIfMissing(req.riskIfMissing());
    }

    /**
     * Charge un référentiel MODIFIABLE par le tenant courant.
     *
     * <p>Trois issues, et la distinction compte : le référentiel du tenant (on
     * travaille), une norme de la plateforme (403 — elle existe, mais ne se
     * modifie pas ici), ou rien du tout (404 — y compris pour le référentiel d'un
     * autre tenant, dont on ne confirme pas l'existence).
     */
    private Standard editable(UUID standardId) {
        UUID tenantId = requireTenantId();
        return standards.findOwnedById(standardId, tenantId).orElseGet(() -> {
            if (standards.findVisibleById(standardId, tenantId).isPresent()) {
                throw new PlatformStandardWriteException();
            }
            throw new StandardNotFoundException(standardId);
        });
    }

    private StandardSection section(Standard s, UUID sectionId) {
        return s.getSections().stream()
                .filter(sec -> sectionId.equals(sec.getId()))
                .findFirst()
                .orElseThrow(() -> new SectionNotFoundException(sectionId));
    }

    private StandardClause clause(Standard s, UUID clauseId) {
        return s.getSections().stream()
                .flatMap(sec -> sec.getClauses().stream())
                .filter(cl -> clauseId.equals(cl.getId()))
                .findFirst()
                .orElseThrow(() -> new ClauseNotFoundException(clauseId));
    }

    private StandardRequirement requirement(Standard s, UUID requirementId) {
        return s.getSections().stream()
                .flatMap(sec -> sec.getClauses().stream())
                .flatMap(cl -> cl.getRequirements().stream())
                .filter(r -> requirementId.equals(r.getId()))
                .findFirst()
                .orElseThrow(() -> new RequirementNotFoundException(requirementId));
    }

    /**
     * Un code ne vaut que dans sa fratrie : deux sections peuvent numéroter leur
     * première clause de la même façon. {@code self} est l'élément en cours de
     * modification, qu'on ne compare pas à lui-même — sans quoi renommer une
     * section sans toucher à son code échouerait.
     */
    private <T> void requireFreeCode(java.util.List<T> siblings,
                                     java.util.function.Function<T, String> code,
                                     String wanted, T self, String level) {
        boolean taken = siblings.stream()
                .filter(sibling -> sibling != self)
                .anyMatch(sibling -> wanted.equals(code.apply(sibling)));
        if (taken) {
            throw new StandardCodeConflictException(
                    "Une " + level + " porte déjà le code " + wanted + " à ce niveau");
        }
    }

    UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }
}
