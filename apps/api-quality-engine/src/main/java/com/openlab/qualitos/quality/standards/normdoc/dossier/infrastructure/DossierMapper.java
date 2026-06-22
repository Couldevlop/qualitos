package com.openlab.qualitos.quality.standards.normdoc.dossier.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DocumentationDossier;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierDocStatus;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierDocument;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierDocument.SectionPlan;

import java.util.List;
import java.util.UUID;

/**
 * Conversion entité JPA ⇄ agrégat {@link DocumentationDossier}. Les pièces sont
 * (dé)sérialisées en JSON via Jackson (colonne TEXT). Mapper stateless.
 */
final class DossierMapper {

    private static final TypeReference<List<DocDto>> DOC_LIST = new TypeReference<>() { };

    private DossierMapper() {}

    /** Représentation JSON d'une pièce (découplée du value object du domaine). */
    record DocDto(String key, NormDocKind kind, String label, List<SectionDto> sections,
                  DossierDocStatus status, UUID normDocId, UUID reuseSuggestedNormDocId,
                  String failureReason) {
        static DocDto of(DossierDocument d) {
            return new DocDto(d.getKey(), d.getKind(), d.getLabel(),
                    d.getSections().stream().map(SectionDto::of).toList(),
                    d.getStatus(), d.getNormDocId(), d.getReuseSuggestedNormDocId(),
                    d.getFailureReason());
        }

        DossierDocument toDomain() {
            return new DossierDocument(key, kind, label,
                    sections.stream().map(SectionDto::toDomain).toList(),
                    status, normDocId, reuseSuggestedNormDocId, failureReason);
        }
    }

    record SectionDto(String key, String title, List<String> clauses, String guidance) {
        static SectionDto of(SectionPlan s) {
            return new SectionDto(s.key(), s.title(), s.clauses(), s.guidance());
        }

        SectionPlan toDomain() {
            return new SectionPlan(key, title, clauses, guidance);
        }
    }

    static DossierJpaEntity toEntity(DocumentationDossier d, DossierJpaEntity target,
                                     ObjectMapper json) {
        DossierJpaEntity e = target != null ? target : new DossierJpaEntity();
        if (d.getId() != null) {
            e.setId(d.getId());
        }
        e.setTenantId(d.getTenantId());
        e.setStandardId(d.getStandardId());
        e.setStandardCode(d.getStandardCode());
        e.setStandardName(d.getStandardName());
        e.setOrganizationName(d.getOrganizationName());
        e.setLanguage(d.getLanguage());
        e.setDocumentsJson(writeDocs(d.getDocuments(), json));
        e.setStatus(d.getStatus());
        e.setAiProvider(d.getAiProvider());
        e.setIntegritySha256(d.getIntegritySha256());
        e.setIntegritySignature(d.getIntegritySignature());
        e.setAnchorTxRef(d.getAnchorTxRef());
        e.setFinalizedAt(d.getFinalizedAt());
        e.setFinalizedByUserId(d.getFinalizedByUserId());
        e.setCreatedByUserId(d.getCreatedByUserId());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        return e;
    }

    static DocumentationDossier toDomain(DossierJpaEntity e, ObjectMapper json) {
        return new DocumentationDossier(
                e.getId(), e.getTenantId(), e.getStandardId(), e.getStandardCode(),
                e.getStandardName(), e.getOrganizationName(), e.getLanguage(),
                readDocs(e.getDocumentsJson(), json), e.getStatus(), e.getAiProvider(),
                e.getIntegritySha256(), e.getIntegritySignature(), e.getAnchorTxRef(),
                e.getFinalizedAt(), e.getFinalizedByUserId(),
                e.getCreatedByUserId(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private static String writeDocs(List<DossierDocument> docs, ObjectMapper json) {
        try {
            return json.writeValueAsString(docs.stream().map(DocDto::of).toList());
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot serialize dossier documents", ex);
        }
    }

    private static List<DossierDocument> readDocs(String value, ObjectMapper json) {
        try {
            return json.readValue(value, DOC_LIST).stream().map(DocDto::toDomain).toList();
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot deserialize dossier documents", ex);
        }
    }
}
