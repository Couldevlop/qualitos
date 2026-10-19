package com.openlab.qualitos.quality.docs;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID>, JpaSpecificationExecutor<Document> {

    Page<Document> findByTenantId(UUID tenantId, Pageable pageable);

    Page<Document> findByTenantIdAndStatus(UUID tenantId, DocumentStatus status, Pageable pageable);

    Optional<Document> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);

    boolean existsByTenantIdAndFolderId(UUID tenantId, UUID folderId);

    boolean existsByTenantIdAndProcessId(UUID tenantId, UUID processId);

    /**
     * Les documents dont une version en revue attend la décision de l'utilisateur
     * (vue « À valider par moi », ADR 0094). Deux cas, comme à l'approbation :
     * <ul>
     *   <li>sans circuit ouvert, l'approbation simple : il suffit d'en avoir le droit
     *       ({@code canApprove}) ;</li>
     *   <li>avec un circuit, l'étape en cours doit revenir à l'un de ses rôles, et il
     *       ne doit pas avoir déjà décidé dans ce passage (quatre yeux).</li>
     * </ul>
     * L'auteur d'une version ne la valide jamais. {@code roles} n'est jamais vide :
     * l'appelant y glisse une valeur neutre, une liste vide rendrait la requête invalide.
     * Les identifiants reviennent en texte : le pilote ne rend pas le même type
     * d'une base à l'autre, le texte, si.
     */
    @Query(value = """
            SELECT DISTINCT CAST(v.document_id AS VARCHAR(36))
            FROM document_versions v
            JOIN documents d ON d.id = v.document_id
            LEFT JOIN circuit_runs r ON r.tenant_id = d.tenant_id
                 AND r.subject = 'document-version' AND r.subject_id = v.id AND r.status = 'IN_PROGRESS'
            LEFT JOIN circuit_run_steps s ON s.run_id = r.id AND s.ordinal = r.current_step
            WHERE d.tenant_id = :tenantId
              AND d.status = 'ACTIVE'
              AND v.status = 'IN_REVIEW'
              AND v.author_id <> :userId
              AND ((r.id IS NULL AND :canApprove)
                   OR (r.id IS NOT NULL AND s.role_code IN (:roles)
                       AND NOT EXISTS (SELECT 1 FROM circuit_decisions c
                                       WHERE c.run_id = r.id AND c.actor_id = :userId)))
            """, nativeQuery = true)
    List<String> findIdsAwaitingDecisionOf(@Param("tenantId") UUID tenantId,
                                         @Param("userId") UUID userId,
                                         @Param("roles") Collection<String> roles,
                                         @Param("canApprove") boolean canApprove);
}
