package com.openlab.qualitos.quality.dashboards;

import com.openlab.qualitos.quality.dashboards.annotations.application.ActorRoles;
import com.openlab.qualitos.quality.dashboards.annotations.application.DashboardAnnotationDto;
import com.openlab.qualitos.quality.dashboards.annotations.application.DashboardAnnotationService;
import com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotation;
import com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotationForbiddenException;
import com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotationNotFoundException;
import com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotationRepository;
import com.openlab.qualitos.quality.dashboards.application.TenantProvider;
import com.openlab.qualitos.quality.dashboards.timetravel.application.TimeTravelDto;
import com.openlab.qualitos.quality.dashboards.timetravel.application.TimeTravelService;
import com.openlab.qualitos.quality.dashboards.timetravel.domain.KpiAsOfRepository;
import com.openlab.qualitos.quality.dashboards.timetravel.domain.KpiAsOfSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Golden-master du parcours doré « Dashboards interactifs + time-travel » (ADR 0034 / 0035).
 *
 * <p>Verrouille de bout en bout, avec des dépôts en mémoire (fidèles au comportement
 * tenant-scoped des adapters JPA réels) :</p>
 * <ol>
 *   <li>time-travel as-of : valeur présente vs date trop ancienne (état vide) ;</li>
 *   <li>cycle de vie annotation : création → lecture → suppression par l'auteur ;</li>
 *   <li>isolation cross-tenant : une annotation d'un autre tenant est invisible (404) ;</li>
 *   <li>autorisation : un non-auteur non-admin ne peut pas supprimer (403), un admin oui.</li>
 * </ol>
 */
@DisplayName("Dashboards — Golden Path (cross-filter source / time-travel + annotations)")
class DashboardInteractivityGoldenPathTest {

    static final Instant NOW = Instant.parse("2026-06-20T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT_A = UUID.randomUUID();
    static final UUID TENANT_B = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID OTHER_USER = UUID.randomUUID();
    static final UUID KPI = UUID.randomUUID();
    static final String CHART = "exec.trend";

    @Test
    @DisplayName("time-travel as-of → présent puis vide ; annotation create→list→delete ; isolation tenant")
    void goldenPath() {
        // ---- contexte courant : tenant A, user USER (depuis le JWT, jamais le body) ----
        AtomicReference<UUID> tenant = new AtomicReference<>(TENANT_A);
        AtomicReference<UUID> user = new AtomicReference<>(USER);
        AtomicBoolean admin = new AtomicBoolean(false);

        TenantProvider tenantProvider = new TenantProvider() {
            @Override public UUID requireTenantId() { return tenant.get(); }
            @Override public UUID requireUserId() { return user.get(); }
        };
        ActorRoles actorRoles = admin::get;

        // ---- 1. TIME-TRAVEL : as-of avec donnée présente ----
        InMemoryKpiAsOf asOfRepo = new InMemoryKpiAsOf();
        // mesure tenant A au 1er mars (valeur 94.2) ; rien avant le 1er janv 2026.
        asOfRepo.put(TENANT_A, Instant.parse("2026-03-01T00:00:00Z"),
                KpiAsOfSnapshot.withValue(KPI, "fpy", "First Pass Yield", "%",
                        new BigDecimal("94.2"), Instant.parse("2026-03-01T00:00:00Z")));
        TimeTravelService timeTravel = new TimeTravelService(asOfRepo, tenantProvider, CLOCK);

        TimeTravelDto.DashboardSnapshotView present =
                timeTravel.snapshotAsOf(Instant.parse("2026-03-15T00:00:00Z"));
        assertThat(present.empty()).isFalse();
        assertThat(present.kpis()).hasSize(1);
        assertThat(present.kpis().get(0).value()).isEqualByComparingTo("94.2");

        // date trop ancienne → état vide soigné (pas d'exception)
        TimeTravelDto.DashboardSnapshotView ancient =
                timeTravel.snapshotAsOf(Instant.parse("2020-01-01T00:00:00Z"));
        assertThat(ancient.empty()).isTrue();
        assertThat(ancient.kpis().get(0).present()).isFalse();

        // ---- 2. ANNOTATIONS : create → list → delete (auteur) ----
        InMemoryAnnotations annoRepo = new InMemoryAnnotations();
        DashboardAnnotationService annotations =
                new DashboardAnnotationService(annoRepo, tenantProvider, actorRoles, CLOCK);

        DashboardAnnotationDto.View created = annotations.create(
                new DashboardAnnotationDto.CreateRequest(CHART, "Mai", "Dérive nette à surveiller"));
        assertThat(created.id()).isNotNull();
        assertThat(created.tenantId()).isEqualTo(TENANT_A);
        assertThat(created.authorId()).isEqualTo(USER);
        assertThat(created.createdAt()).isEqualTo(NOW);
        assertThat(created.deletable()).isTrue();

        List<DashboardAnnotationDto.View> listed = annotations.listByChart(CHART);
        assertThat(listed).hasSize(1);
        assertThat(listed.get(0).body()).isEqualTo("Dérive nette à surveiller");
        assertThat(listed.get(0).deletable()).isTrue();

        // ---- 3. ISOLATION CROSS-TENANT : tenant B ne voit rien et ne peut pas supprimer ----
        tenant.set(TENANT_B);
        user.set(OTHER_USER);
        assertThat(annotations.listByChart(CHART)).isEmpty();
        assertThatThrownBy(() -> annotations.delete(created.id()))
                .isInstanceOf(DashboardAnnotationNotFoundException.class);

        // ---- 4. AUTORISATION : non-auteur même tenant → 403 ; admin → OK ----
        tenant.set(TENANT_A);
        user.set(OTHER_USER);
        admin.set(false);
        assertThat(annotations.listByChart(CHART).get(0).deletable()).isFalse();
        assertThatThrownBy(() -> annotations.delete(created.id()))
                .isInstanceOf(DashboardAnnotationForbiddenException.class);

        admin.set(true);
        annotations.delete(created.id());

        // ---- 5. RÉSULTAT : l'annotation a disparu pour tout le tenant ----
        user.set(USER);
        admin.set(false);
        assertThat(annotations.listByChart(CHART)).isEmpty();
    }

    // ---- Fakes en mémoire, fidèles au tenant-scoping des adapters JPA ----

    private static final class InMemoryAnnotations implements DashboardAnnotationRepository {
        private final List<DashboardAnnotation> store = new ArrayList<>();

        @Override public DashboardAnnotation save(DashboardAnnotation a) {
            if (a.getId() == null) a.assignId(UUID.randomUUID());
            store.add(a);
            return a;
        }
        @Override public Optional<DashboardAnnotation> findByIdAndTenant(UUID id, UUID tenantId) {
            return store.stream()
                    .filter(a -> a.getId().equals(id) && a.getTenantId().equals(tenantId))
                    .findFirst();
        }
        @Override public List<DashboardAnnotation> findByTenantAndChartKey(UUID tenantId, String chartKey) {
            return store.stream()
                    .filter(a -> a.getTenantId().equals(tenantId) && a.getChartKey().equals(chartKey))
                    .toList();
        }
        @Override public void delete(UUID id) {
            store.removeIf(a -> a.getId().equals(id));
        }
    }

    private static final class InMemoryKpiAsOf implements KpiAsOfRepository {
        private final java.util.Map<UUID, java.util.TreeMap<Instant, KpiAsOfSnapshot>> data =
                new java.util.HashMap<>();

        void put(UUID tenant, Instant periodStart, KpiAsOfSnapshot snapshot) {
            data.computeIfAbsent(tenant, t -> new java.util.TreeMap<>()).put(periodStart, snapshot);
        }

        @Override public List<KpiAsOfSnapshot> snapshotAsOf(UUID tenantId, Instant asOf) {
            var byPeriod = data.get(tenantId);
            if (byPeriod == null || byPeriod.isEmpty()) {
                return List.of();
            }
            var floor = byPeriod.floorEntry(asOf);
            if (floor == null) {
                // KPI exists but no measurement at-or-before asOf → absent snapshot.
                KpiAsOfSnapshot any = byPeriod.firstEntry().getValue();
                return List.of(KpiAsOfSnapshot.absent(
                        any.getKpiId(), any.getCode(), any.getName(), any.getUnit()));
            }
            return List.of(floor.getValue());
        }
    }
}
