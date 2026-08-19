package com.openlab.qualitos.quality.controlplan.application;

import com.openlab.qualitos.quality.controlplan.domain.ControlPlan;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanLine;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanNotFoundException;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanRepository;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanStateException;
import com.openlab.qualitos.quality.controlplan.domain.FmeaItemLookup;
import com.openlab.qualitos.quality.product.domain.Product;
import com.openlab.qualitos.quality.product.domain.ProductLookup;
import com.openlab.qualitos.quality.product.domain.ProductNotFoundException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Use cases — control plan.
 *
 * <p>Garantie clé (OWASP A01) : la route est imbriquée (produit → plan → ligne) et
 * la chaîne complète d'appartenance est revérifiée à chaque appel, jamais seulement
 * la feuille. Une ressource d'un autre tenant répond 404 et non 403 : un 403
 * confirmerait son existence.
 *
 * <p>Garantie métier : un plan approuvé est affiché au poste et montré à l'auditeur.
 * Toute écriture passe par {@link ControlPlan#requireDraft()} — le refus vient du
 * domaine, pas d'un contrôle que le service pourrait oublier.
 */
public class ControlPlanService {

    private final ControlPlanRepository repo;
    private final ProductLookup products;
    private final FmeaItemLookup fmeaItems;
    private final TenantProvider tenants;
    private final ActorProvider actors;
    private final Clock clock;

    public ControlPlanService(ControlPlanRepository repo, ProductLookup products,
                              FmeaItemLookup fmeaItems, TenantProvider tenants,
                              ActorProvider actors, Clock clock) {
        this.repo = repo;
        this.products = products;
        this.fmeaItems = fmeaItems;
        this.tenants = tenants;
        this.actors = actors;
        this.clock = clock;
    }

    public List<ControlPlanDto.View> listForProduct(UUID productId) {
        Product product = requireOwnedProduct(productId);
        return repo.findByProduct(product.getTenantId(), product.getId()).stream()
                .map(ControlPlanDto.View::of)
                .toList();
    }

    public ControlPlanDto.Detail get(UUID productId, UUID planId) {
        ControlPlan plan = requireOwnedPlan(productId, planId);
        List<ControlPlanDto.LineView> lines = repo.linesOf(plan.getId()).stream()
                .map(ControlPlanDto.LineView::of)
                .toList();
        return new ControlPlanDto.Detail(ControlPlanDto.View.of(plan), lines);
    }

    public ControlPlanDto.View createDraft(UUID productId, ControlPlanDto.CreateCommand cmd) {
        Product product = requireOwnedProduct(productId);
        repo.findDraft(product.getTenantId(), product.getId(), cmd.phase()).ifPresent(existing -> {
            throw new ControlPlanStateException(
                    "Un brouillon existe déjà pour la phase " + cmd.phase() + " : " + existing.getCode());
        });
        ControlPlan plan = ControlPlan.create(product.getTenantId(), product.getId(), cmd.phase(),
                cmd.code(), actors.currentUserId(), Instant.now(clock));
        if (cmd.ownerUserId() != null) plan.assignOwner(cmd.ownerUserId());
        return ControlPlanDto.View.of(repo.save(plan));
    }

    /**
     * Ouvre la révision suivante d'un plan en vigueur, en recopiant ses lignes.
     * Repartir d'un plan vide ferait perdre le travail : on corrigerait UNE ligne
     * et on perdrait les quarante autres.
     */
    public ControlPlanDto.View openRevision(UUID productId, UUID planId) {
        ControlPlan active = requireOwnedPlan(productId, planId);
        // nextRevision d'abord : c'est lui qui refuse un plan qui n'est pas en
        // vigueur, et l'annoncer avant de parler de brouillon évite un message
        // qui désignerait le plan lui-même comme la révision déjà ouverte.
        ControlPlan next = active.nextRevision(actors.currentUserId(), Instant.now(clock));
        repo.findDraft(active.getTenantId(), active.getProductId(), active.getPhase())
                .ifPresent(draft -> {
                    throw new ControlPlanStateException(
                            "Une révision est déjà ouverte pour cette phase : " + draft.getCode());
                });
        ControlPlan saved = repo.save(next);
        for (ControlPlanLine source : repo.linesOf(active.getId())) {
            repo.saveLine(copyOf(source, saved.getId()));
        }
        return ControlPlanDto.View.of(saved);
    }

    /**
     * Approuve le brouillon et archive le plan de la MÊME phase qu'il remplace.
     * Sans cet archivage, deux plans actifs coexisteraient, l'index partiel
     * rejetterait l'écriture et l'utilisateur recevrait une erreur de base de
     * données incompréhensible.
     */
    public ControlPlanDto.View approve(UUID productId, UUID planId) {
        ControlPlan draft = requireOwnedPlan(productId, planId);
        draft.requireDraft();
        Optional<ControlPlan> previous =
                repo.findActive(draft.getTenantId(), draft.getProductId(), draft.getPhase());
        previous.filter(p -> !p.getId().equals(draft.getId())).ifPresent(p -> {
            p.archive();
            repo.save(p);
        });
        draft.approve(actors.currentUserId(), Instant.now(clock));
        return ControlPlanDto.View.of(repo.save(draft));
    }

    public ControlPlanDto.LineView addLine(UUID productId, UUID planId, ControlPlanDto.LineCommand cmd) {
        ControlPlan plan = requireOwnedPlan(productId, planId);
        plan.requireDraft();
        ControlPlanLine line = ControlPlanLine.create(plan.getTenantId(), plan.getId(),
                cmd.sequenceNo(), cmd.characteristicLabel(), cmd.characteristicType());
        apply(cmd, line, plan.getProductId());
        return ControlPlanDto.LineView.of(repo.saveLine(line));
    }

    public ControlPlanDto.LineView updateLine(UUID productId, UUID planId, UUID lineId,
                                              ControlPlanDto.LineCommand cmd) {
        ControlPlan plan = requireOwnedPlan(productId, planId);
        plan.requireDraft();
        ControlPlanLine line = requireOwnedLine(plan, lineId);
        line.rename(cmd.characteristicLabel(), cmd.characteristicType(), cmd.sequenceNo());
        apply(cmd, line, plan.getProductId());
        return ControlPlanDto.LineView.of(repo.saveLine(line));
    }

    public void deleteLine(UUID productId, UUID planId, UUID lineId) {
        ControlPlan plan = requireOwnedPlan(productId, planId);
        plan.requireDraft();
        ControlPlanLine line = requireOwnedLine(plan, lineId);
        repo.deleteLine(line.getId());
    }

    // ---------- helpers ----------

    private void apply(ControlPlanDto.LineCommand cmd, ControlPlanLine line, UUID productId) {
        line.describe(cmd.operationId(), cmd.machine(), cmd.characteristicNo(), cmd.specialClass(),
                cmd.specification(), cmd.toleranceLower(), cmd.toleranceUpper(), cmd.unit(),
                cmd.measurementTechnique(), cmd.sampleSize(), cmd.sampleFrequency(),
                cmd.controlMethod(), cmd.reactionPlan());
        line.justifiedBy(requireFmeaItemOfProduct(cmd.fmeaItemId(), productId));
    }

    /**
     * Le lien PFMEA justifie le contrôle. Citer la ligne d'un autre produit
     * produirait une justification fausse — pire que pas de justification du tout.
     */
    private UUID requireFmeaItemOfProduct(UUID fmeaItemId, UUID productId) {
        if (fmeaItemId == null) return null;
        UUID covered = fmeaItems.productCoveredBy(fmeaItemId)
                .orElseThrow(() -> new ControlPlanStateException(
                        "Ligne de PFMEA inconnue ou sans produit : " + fmeaItemId));
        if (!covered.equals(productId)) {
            throw new ControlPlanStateException(
                    "La ligne de PFMEA " + fmeaItemId + " couvre un autre produit");
        }
        return fmeaItemId;
    }

    private ControlPlanLine copyOf(ControlPlanLine source, UUID targetPlanId) {
        ControlPlanLine copy = ControlPlanLine.create(source.getTenantId(), targetPlanId,
                source.getSequenceNo(), source.getCharacteristicLabel(), source.getCharacteristicType());
        copy.describe(source.getOperationId(), source.getMachine(), source.getCharacteristicNo(),
                source.getSpecialClass(), source.getSpecification(), source.getToleranceLower(),
                source.getToleranceUpper(), source.getUnit(), source.getMeasurementTechnique(),
                source.getSampleSize(), source.getSampleFrequency(), source.getControlMethod(),
                source.getReactionPlan());
        copy.justifiedBy(source.getFmeaItemId());
        return copy;
    }

    private Product requireOwnedProduct(UUID productId) {
        UUID tenant = tenants.requireTenantId();
        Product product = products.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        if (!product.getTenantId().equals(tenant)) {
            throw new ProductNotFoundException(productId);
        }
        return product;
    }

    private ControlPlan requireOwnedPlan(UUID productId, UUID planId) {
        Product product = requireOwnedProduct(productId);
        ControlPlan plan = repo.findById(planId)
                .orElseThrow(() -> new ControlPlanNotFoundException(planId));
        if (!plan.getProductId().equals(product.getId())
                || !plan.getTenantId().equals(product.getTenantId())) {
            throw new ControlPlanNotFoundException(planId);
        }
        return plan;
    }

    private ControlPlanLine requireOwnedLine(ControlPlan plan, UUID lineId) {
        ControlPlanLine line = repo.findLine(lineId)
                .orElseThrow(() -> new ControlPlanNotFoundException(lineId));
        if (!line.getPlanId().equals(plan.getId()) || !line.getTenantId().equals(plan.getTenantId())) {
            throw new ControlPlanNotFoundException(lineId);
        }
        return line;
    }
}
