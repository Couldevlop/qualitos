package com.openlab.qualitos.quality.docs;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

/**
 * Les critères de la liste de la GED (ADR 0094), un par filtre de l'écran. Chacun
 * se combine aux autres par « et » ; {@code null} ne filtre rien.
 */
final class DocumentSpecifications {

    private DocumentSpecifications() {}

    static Specification<Document> tenant(UUID tenantId) {
        return (root, q, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    static Specification<Document> active() {
        return (root, q, cb) -> cb.equal(root.get("status"), DocumentStatus.ACTIVE);
    }

    static Specification<Document> archived() {
        return (root, q, cb) -> cb.equal(root.get("status"), DocumentStatus.ARCHIVED);
    }

    static Specification<Document> ownedBy(UUID ownerId) {
        return ownerId == null ? null : (root, q, cb) -> cb.equal(root.get("ownerId"), ownerId);
    }

    static Specification<Document> idIn(Collection<UUID> ids) {
        return (root, q, cb) -> ids.isEmpty() ? cb.disjunction() : root.get("id").in(ids);
    }

    static Specification<Document> inFolders(Collection<UUID> folderIds) {
        return folderIds == null ? null : (root, q, cb) -> root.get("folderId").in(folderIds);
    }

    static Specification<Document> ofType(DocumentType type) {
        return type == null ? null : (root, q, cb) -> cb.equal(root.get("type"), type);
    }

    static Specification<Document> ofProcess(UUID processId) {
        return processId == null ? null : (root, q, cb) -> cb.equal(root.get("processId"), processId);
    }

    /** Publié au moins une fois, et dont la revue tombe au plus tard le {@code limit}. */
    static Specification<Document> reviewDueBy(LocalDate limit) {
        return (root, q, cb) -> cb.and(
                cb.notEqual(root.get("lifecycle"), DocumentLifecycle.DRAFT),
                cb.isNotNull(root.get("nextReviewDate")),
                cb.lessThanOrEqualTo(root.get("nextReviewDate"), limit));
    }

    /** Le statut affiché, tel que {@link DocumentService#displayStatus} le calcule. */
    static Specification<Document> withDisplayStatus(DisplayStatus status, LocalDate today) {
        if (status == null) {
            return null;
        }
        return (root, q, cb) -> {
            Predicate isActive = cb.equal(root.get("status"), DocumentStatus.ACTIVE);
            Predicate overdue = cb.and(
                    cb.notEqual(root.get("lifecycle"), DocumentLifecycle.DRAFT),
                    cb.isNotNull(root.get("nextReviewDate")),
                    cb.lessThan(root.get("nextReviewDate"), today));
            return switch (status) {
                case ARCHIVED -> cb.equal(root.get("status"), DocumentStatus.ARCHIVED);
                case REVIEW_OVERDUE -> cb.and(isActive, overdue);
                case DRAFT -> cb.and(isActive, cb.equal(root.get("lifecycle"), DocumentLifecycle.DRAFT));
                case IN_FORCE -> cb.and(isActive, cb.not(overdue),
                        cb.equal(root.get("lifecycle"), DocumentLifecycle.IN_FORCE));
                case IN_REVISION -> cb.and(isActive, cb.not(overdue),
                        cb.equal(root.get("lifecycle"), DocumentLifecycle.IN_REVISION));
            };
        };
    }

    /** Référence, titre ou description contenant le texte, sans égard à la casse. */
    static Specification<Document> matching(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String pattern = "%" + escapeLike(text.strip().toLowerCase(Locale.ROOT)) + "%";
        return (root, q, cb) -> cb.or(
                cb.like(cb.lower(root.get("code")), pattern, '\\'),
                cb.like(cb.lower(root.get("title")), pattern, '\\'),
                cb.like(cb.lower(root.get("description")), pattern, '\\'));
    }

    /** Un « % » ou un « _ » tapé par l'utilisateur se cherche tel quel, il ne joue pas le joker. */
    static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
