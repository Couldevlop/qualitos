package com.openlab.qualitos.quality.fmeascale;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Le référentiel de cotation FMEA d'un tenant (§4.5).
 *
 * <p><b>Modèle par défaut, jamais par copie.</b> Un tenant qui n'a rien
 * redéfini n'a aucune ligne en base et reçoit le barème de référence. Semer les
 * trente lignes à la création de chaque tenant l'aurait figé au jour de
 * l'inscription et aurait rendu impossible de distinguer « jamais touché » de
 * « redéfini à l'identique » — la distinction même qu'un auditeur vient
 * chercher.
 *
 * <p><b>Remplacement d'un bloc, jamais ligne à ligne.</b> Une échelle est une
 * suite continue de 1 à 10 : autoriser la modification d'une seule ligne
 * permettrait de laisser un trou, et un score sans définition ferait coter au
 * jugé exactement là où le barème existe pour l'éviter.
 */
@Service
@Transactional
public class FmeaScaleService {

    private static final String RESOURCE_TYPE = "fmea_rating_scale";

    private final FmeaScaleRowRepository repository;
    private final AuditEventService auditEvents;

    public FmeaScaleService(FmeaScaleRowRepository repository, AuditEventService auditEvents) {
        this.repository = repository;
        this.auditEvents = auditEvents;
    }

    /** Les trois échelles du tenant courant, redéfinies ou de référence. */
    @Transactional(readOnly = true)
    public FmeaScaleDto.ReferenceView findAll() {
        UUID tenantId = requireTenantId();
        List<FmeaScaleDto.ScaleView> scales = new ArrayList<>();
        for (FmeaScaleKind kind : FmeaScaleKind.values()) {
            scales.add(view(tenantId, kind));
        }
        return new FmeaScaleDto.ReferenceView(List.copyOf(scales));
    }

    @Transactional(readOnly = true)
    public FmeaScaleDto.ScaleView find(FmeaScaleKind kind) {
        return view(requireTenantId(), kind);
    }

    /**
     * Remplace le barème d'une échelle.
     *
     * <p>Les dix lignes partent ensemble et écrasent ce qui existait. Un
     * remplacement partiel laisserait cohabiter deux barèmes dans la même
     * échelle, et personne ne saurait lequel a servi à coter quoi.
     */
    public FmeaScaleDto.ScaleView replace(FmeaScaleKind kind, FmeaScaleDto.ScaleRequest request) {
        UUID tenantId = requireTenantId();
        List<FmeaScaleRow> rows = validated(request);

        // L'acteur vient du jeton, jamais du corps : redéfinir un barème est une
        // décision de politique qualité, elle doit être attribuable (OWASP A01).
        UUID actor = CurrentUser.requireUserId();
        Instant now = Instant.now();

        repository.deleteByTenantIdAndKind(tenantId, kind.name());
        // Vidange forcée avant réécriture : sans elle, la contrainte d'unicité
        // (tenant, kind, score) refuserait les nouvelles lignes dans la même
        // transaction, l'ordre des instructions n'étant pas garanti.
        repository.flush();

        for (FmeaScaleRow row : rows) {
            FmeaScaleRowEntity entity = new FmeaScaleRowEntity();
            entity.setId(UUID.randomUUID());
            entity.setTenantId(tenantId);
            entity.setKind(kind.name());
            entity.setScore((short) row.score());
            entity.setLabel(row.label());
            entity.setDescription(row.description());
            entity.setTimePeriod(row.timePeriod());
            entity.setFailureRate(row.failureRate());
            entity.setUpdatedBy(actor);
            entity.setUpdatedAt(now);
            repository.save(entity);
        }
        trace(tenantId, actor, "fmea.rating_scale.redefined",
                "Barème " + kind.name() + " redéfini par le tenant",
                payload(kind, rows.size()));
        return view(tenantId, kind);
    }

    /**
     * Revient au barème de référence en supprimant les lignes du tenant.
     *
     * <p>Suppression et non recopie du référentiel : recopier laisserait croire
     * que l'organisation a délibérément adopté ces trente lignes, alors qu'elle
     * a seulement renoncé à les redéfinir.
     */
    public FmeaScaleDto.ScaleView revertToReference(FmeaScaleKind kind) {
        UUID tenantId = requireTenantId();
        UUID actor = CurrentUser.requireUserId();
        repository.deleteByTenantIdAndKind(tenantId, kind.name());
        repository.flush();
        trace(tenantId, actor, "fmea.rating_scale.reverted",
                "Barème " + kind.name() + " revenu au référentiel",
                payload(kind, 0));
        return view(tenantId, kind);
    }

    // ---------- helpers ----------

    /**
     * Inscrit le changement de barème au journal chaîné du tenant.
     *
     * <p>La ligne en base porte bien {@code updated_by} et {@code updated_at},
     * mais elle est ÉCRASÉE au remplacement suivant : elle dit qui a posé le
     * barème actuel, jamais qu'il en a existé un autre avant. Or c'est
     * exactement la question de l'auditeur devant deux RPN de 120 cotés à six
     * mois d'écart — le second n'a pas la même signification si l'échelle a
     * bougé entre-temps. Le journal, lui, est en ajout seul et ancré
     * périodiquement (§11.5).
     */
    private void trace(UUID tenantId, UUID actor, String action, String summary, String payload) {
        auditEvents.recordForTenant(tenantId, new AuditEventDto.RecordEventRequest(
                null,
                actor == null ? ActorType.SYSTEM : ActorType.USER,
                actor,
                action,
                RESOURCE_TYPE,
                null,
                summary,
                payload,
                null,
                null));
    }

    /**
     * Deux champs écrits à la main, sans sérialiseur : l'échelle touchée et le
     * nombre de lignes posées. Le TEXTE du barème n'y entre pas — il est déjà en
     * base, l'y recopier ferait grossir un journal qu'on relit intégralement à
     * chaque vérification de chaîne.
     */
    private static String payload(FmeaScaleKind kind, int rowCount) {
        return "{\"kind\":\"" + kind.name() + "\",\"rows\":" + rowCount + "}";
    }

    private FmeaScaleDto.ScaleView view(UUID tenantId, FmeaScaleKind kind) {
        List<FmeaScaleRowEntity> stored =
                repository.findByTenantIdAndKindOrderByScoreDesc(tenantId, kind.name());
        if (stored.isEmpty()) {
            return new FmeaScaleDto.ScaleView(kind, false,
                    FmeaReferenceScales.of(kind).stream().map(FmeaScaleDto.RowView::of).toList(),
                    null, null);
        }
        List<FmeaScaleDto.RowView> rows = stored.stream()
                .map(e -> new FmeaScaleDto.RowView(e.getScore(), e.getLabel(),
                        e.getDescription(), e.getTimePeriod(), e.getFailureRate()))
                .toList();
        FmeaScaleRowEntity last = stored.stream()
                .max(Comparator.comparing(FmeaScaleRowEntity::getUpdatedAt))
                .orElseThrow();
        return new FmeaScaleDto.ScaleView(kind, true, rows, last.getUpdatedBy(), last.getUpdatedAt());
    }

    /**
     * Une échelle complète : dix lignes, une par score de 1 à 10.
     *
     * <p>Vérifié ici et pas seulement à la frontière HTTP : un barème incomplet
     * ne se voit pas à l'écran — il se découvre le jour où quelqu'un cote un 7
     * qui n'a aucune définition.
     */
    private List<FmeaScaleRow> validated(FmeaScaleDto.ScaleRequest request) {
        if (request == null || request.rows() == null) {
            throw new FmeaScaleValidationException("Aucune ligne de barème fournie");
        }
        Set<Integer> seen = new LinkedHashSet<>();
        List<FmeaScaleRow> rows = new ArrayList<>();
        for (FmeaScaleDto.RowRequest row : request.rows()) {
            if (!seen.add(row.score())) {
                throw new FmeaScaleValidationException(
                        "Le score " + row.score() + " est défini deux fois");
            }
            try {
                rows.add(new FmeaScaleRow(row.score(), row.label(), row.description(),
                        row.timePeriod(), row.failureRate()));
            } catch (IllegalArgumentException invalid) {
                throw new FmeaScaleValidationException(invalid.getMessage());
            }
        }
        for (int score = FmeaScaleRow.MIN_SCORE; score <= FmeaScaleRow.MAX_SCORE; score++) {
            if (!seen.contains(score)) {
                throw new FmeaScaleValidationException(
                        "Le score " + score + " n'a aucune définition : un barème va de 1 à 10");
            }
        }
        rows.sort(Comparator.comparingInt(FmeaScaleRow::score).reversed());
        return rows;
    }

    private static UUID requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(tenantId);
    }
}
