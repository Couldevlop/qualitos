package com.openlab.qualitos.quality.derogation.infrastructure;

import com.openlab.qualitos.quality.derogation.domain.Derogation;
import com.openlab.qualitos.quality.derogation.domain.DerogationConflictException;
import com.openlab.qualitos.quality.derogation.domain.DerogationLine;
import com.openlab.qualitos.quality.derogation.domain.DerogationQuery;
import com.openlab.qualitos.quality.derogation.domain.DerogationRepository;
import com.openlab.qualitos.quality.derogation.domain.DerogationStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Réalise le port de persistance du domaine avec Spring Data et JPA.
 *
 * <p>Tout ce que le cadre apporte — {@code Specification}, {@code Pageable},
 * {@code Page}, le verrou optimiste — s'arrête à cette classe. Le domaine reçoit
 * ses propres types, et c'est ce qui permettrait de changer de base sans
 * toucher une règle.
 */
@Component
public class DerogationRepositoryAdapter implements DerogationRepository {

    private final DerogationJpaRepository dossiers;
    private final DerogationLineJpaRepository lignes;

    public DerogationRepositoryAdapter(DerogationJpaRepository dossiers,
                                       DerogationLineJpaRepository lignes) {
        this.dossiers = dossiers;
        this.lignes = lignes;
    }

    /**
     * Enregistre le dossier, puis réécrit ses lignes.
     *
     * <p><b>L'ordre des écritures de lignes est la subtilité de cette classe.</b>
     * Les lignes se renvoient en bloc et leur rang est refait : si la suppression
     * passait par le contexte de persistance, Hibernate ordonnerait les INSERT
     * avant les DELETE et le nouveau rang 1 heurterait l'ancien sur la contrainte
     * d'unicité. On supprime donc par requête, on vide le contexte, puis on
     * insère.
     */
    @Override
    public Derogation save(Derogation dossier) {
        DerogationJpaEntity entite = dossier.getId() == null
                ? new DerogationJpaEntity()
                : dossiers.findByIdAndTenantId(dossier.getId(), dossier.getTenantId())
                        .orElseGet(DerogationJpaEntity::new);
        DerogationMapper.copyToEntity(dossier, entite);

        DerogationJpaEntity ecrit;
        try {
            ecrit = dossiers.saveAndFlush(entite);
        } catch (OptimisticLockingFailureException e) {
            throw new DerogationConflictException(
                    "Ce dossier a changé entre-temps : rechargez-le avant de reprendre.", e);
        }

        UUID id = ecrit.getId();
        lignes.deleteLinesOf(dossier.getTenantId(), id);
        lignes.flush();
        List<DerogationLine> aEcrire = dossier.getLines();
        if (!aEcrire.isEmpty()) {
            lignes.saveAll(aEcrire.stream()
                    .map(l -> DerogationMapper.toEntity(dossier.getTenantId(), id, l))
                    .toList());
            lignes.flush();
        }

        dossier.assignId(id);
        dossier.assignVersion(ecrit.getVersion());
        return DerogationMapper.toDomain(ecrit, lignesDe(dossier.getTenantId(), id));
    }

    @Override
    public Optional<Derogation> findById(UUID tenantId, UUID id) {
        return dossiers.findByIdAndTenantId(id, tenantId)
                .map(e -> DerogationMapper.toDomain(e, lignesDe(tenantId, e.getId())));
    }

    @Override
    public Slice<Derogation> search(DerogationQuery query) {
        Pageable tranche = PageRequest.of(query.page(), query.size(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<DerogationJpaEntity> page = dossiers.findAll(specification(query), tranche);
        return new Slice<>(assembler(query.tenantId(), page.getContent()),
                page.getTotalElements(), query.page(), query.size());
    }

    @Override
    public Slice<Derogation> findPendingApprovals(UUID tenantId, UUID managerAccountId,
                                                  int page, int size) {
        int rang = Math.max(page, 0);
        int taille = size <= 0 ? 20 : Math.min(size, DerogationQuery.TAILLE_MAX);
        Pageable tranche = PageRequest.of(rang, taille,
                Sort.by(Sort.Direction.ASC, "updatedAt"));
        Page<DerogationJpaEntity> trouves = dossiers.findByTenantIdAndManagerAccountIdAndStatus(
                tenantId, managerAccountId, DerogationStatus.PENDING_INTERNAL.name(), tranche);
        return new Slice<>(assembler(tenantId, trouves.getContent()),
                trouves.getTotalElements(), rang, taille);
    }

    @Override
    public long countPendingApprovals(UUID tenantId, UUID managerAccountId) {
        return dossiers.countByTenantIdAndManagerAccountIdAndStatus(
                tenantId, managerAccountId, DerogationStatus.PENDING_INTERNAL.name());
    }

    @Override
    public long countByReferencePrefix(UUID tenantId, String prefix) {
        return dossiers.countByTenantIdAndReferenceStartingWith(tenantId, prefix);
    }

    @Override
    public boolean existsByReference(UUID tenantId, String reference) {
        return dossiers.existsByTenantIdAndReference(tenantId, reference);
    }

    /** Les critères du registre, traduits en prédicats. */
    private static Specification<DerogationJpaEntity> specification(DerogationQuery q) {
        return (root, requete, cb) -> {
            List<Predicate> clauses = new ArrayList<>();
            clauses.add(cb.equal(root.get("tenantId"), q.tenantId()));
            if (q.owner() != null) {
                clauses.add(cb.equal(root.get("createdByUserId"), q.owner()));
            }
            if (q.status() != null) {
                clauses.add(cb.equal(root.get("status"), q.status().name()));
            }
            if (q.partReference() != null) {
                clauses.add(cb.like(cb.lower(root.get("partReference")),
                        motif(q.partReference()), '\\'));
            }
            if (q.customerName() != null) {
                clauses.add(cb.like(cb.lower(root.get("customerName")),
                        motif(q.customerName()), '\\'));
            }
            if (q.visibleTo() != null) {
                // Sans le droit de tout voir : ses propres dossiers, et ceux
                // qu'on doit signer (ADR 0081).
                clauses.add(cb.or(
                        cb.equal(root.get("createdByUserId"), q.visibleTo()),
                        cb.equal(root.get("managerAccountId"), q.visibleTo())));
            }
            return cb.and(clauses.toArray(new Predicate[0]));
        };
    }

    /** Un {@code %} ou un {@code _} tapé par un utilisateur est un CARACTÈRE, pas un joker. */
    private static String motif(String saisi) {
        String echappe = saisi.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + echappe + "%";
    }

    /**
     * Les dossiers d'une tranche, lignes comprises, en <b>deux</b> requêtes.
     *
     * <p>Une par dossier en ferait cinquante sur un écran qui en affiche
     * cinquante, et la colonne « défaut » du registre les demande toutes.
     */
    private List<Derogation> assembler(UUID tenantId, List<DerogationJpaEntity> entites) {
        if (entites.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = entites.stream().map(DerogationJpaEntity::getId).toList();
        Map<UUID, List<DerogationLineJpaEntity>> parDossier = new HashMap<>();
        for (DerogationLineJpaEntity l
                : lignes.findByTenantIdAndDerogationIdInOrderByPositionAsc(tenantId, ids)) {
            parDossier.computeIfAbsent(l.getDerogationId(), k -> new ArrayList<>()).add(l);
        }
        return entites.stream()
                .map(e -> DerogationMapper.toDomain(e,
                        parDossier.getOrDefault(e.getId(), List.of())))
                .toList();
    }

    private List<DerogationLineJpaEntity> lignesDe(UUID tenantId, UUID id) {
        return lignes.findByTenantIdAndDerogationIdOrderByPositionAsc(tenantId, id);
    }
}
