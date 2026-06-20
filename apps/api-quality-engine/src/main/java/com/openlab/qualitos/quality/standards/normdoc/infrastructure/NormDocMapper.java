package com.openlab.qualitos.quality.standards.normdoc.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocSection;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormativeDocument;

import java.util.List;

/**
 * Conversion entité JPA ⇄ agrégat de domaine. Les sections sont (dé)sérialisées
 * en JSON via Jackson. Mapper stateless ; l'{@link ObjectMapper} est injecté.
 */
final class NormDocMapper {

    private static final TypeReference<List<SectionDto>> SECTION_LIST =
            new TypeReference<>() { };

    private NormDocMapper() {}

    /** Représentation JSON d'une section (découplée du value object du domaine). */
    record SectionDto(String key, String title, List<String> clauses, String bodyMarkdown) {
        static SectionDto of(NormDocSection s) {
            return new SectionDto(s.getKey(), s.getTitle(), s.getClauses(), s.getBodyMarkdown());
        }

        NormDocSection toDomain() {
            return new NormDocSection(key, title, clauses, bodyMarkdown);
        }
    }

    static NormDocJpaEntity toEntity(NormativeDocument d, NormDocJpaEntity target, ObjectMapper json) {
        NormDocJpaEntity e = target != null ? target : new NormDocJpaEntity();
        if (d.getId() != null) {
            e.setId(d.getId());
        }
        e.setTenantId(d.getTenantId());
        e.setStandardId(d.getStandardId());
        e.setStandardCode(d.getStandardCode());
        e.setKind(d.getKind());
        e.setTitle(d.getTitle());
        e.setSectionsJson(writeSections(d.getSections(), json));
        e.setStatus(d.getStatus());
        e.setAiProvider(d.getAiProvider());
        e.setSubmittedAt(d.getSubmittedAt());
        e.setSubmittedByUserId(d.getSubmittedByUserId());
        e.setApprovedAt(d.getApprovedAt());
        e.setApprovedByUserId(d.getApprovedByUserId());
        e.setApprovalNotes(d.getApprovalNotes());
        e.setHumanSignature(d.getHumanSignature());
        e.setRejectionReason(d.getRejectionReason());
        e.setCreatedByUserId(d.getCreatedByUserId());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        return e;
    }

    static NormativeDocument toDomain(NormDocJpaEntity e, ObjectMapper json) {
        return new NormativeDocument(
                e.getId(), e.getTenantId(), e.getStandardId(), e.getStandardCode(),
                e.getKind(), e.getTitle(), readSections(e.getSectionsJson(), json),
                e.getStatus(), e.getAiProvider(),
                e.getSubmittedAt(), e.getSubmittedByUserId(),
                e.getApprovedAt(), e.getApprovedByUserId(), e.getApprovalNotes(),
                e.getHumanSignature(), e.getRejectionReason(),
                e.getCreatedByUserId(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private static String writeSections(List<NormDocSection> sections, ObjectMapper json) {
        try {
            return json.writeValueAsString(sections.stream().map(SectionDto::of).toList());
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot serialize normative document sections", ex);
        }
    }

    private static List<NormDocSection> readSections(String value, ObjectMapper json) {
        try {
            List<SectionDto> dtos = json.readValue(value, SECTION_LIST);
            return dtos.stream().map(SectionDto::toDomain).toList();
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot deserialize normative document sections", ex);
        }
    }
}
