# ADR 0010 — Drag-and-drop dashboards builder (P5)

Status: Accepted (P5 sprint-1, 2026-05-17)

## Context

CLAUDE.md §7.1 (3-tier dashboard hierarchy), §7.3 (Power-BI-style drag&drop
builder, cross-filtering, embed, NLQ integration). Concurrent products
(MasterControl, ETQ, Qualio) ship vieillissante UIs — a P5 differentiator.

## Decision

### Front-end (Angular)

A new lazy-loaded `DashboardBuilderModule` under
`apps/web/src/app/features/dashboard-builder/` follows **Clean Architecture**
inside the Angular feature:

```
dashboard-builder/
  domain/             — TS models, repository port, InjectionToken<T>
  application/        — DashboardBuilderService (use-case orchestrator)
  infrastructure/     — DashboardHttpRepository (HttpClient adapter)
  presentation/
    dashboard-list/   — NgModule component, separate HTML/SCSS
    dashboard-editor/
    widgets/          — WidgetHostComponent
```

Project memory rules respected: **NgModule + separate HTML/SCSS, NEVER
standalone, NO inline templates**. The presentation layer is *the only*
layer that imports Angular UI modules.

Widgets: KPI tile, line/bar/pie/gauge/table/heatmap (ECharts via
`ngx-echarts`), narrative text. Cross-filtering uses RxJS Subjects on
`DashboardBuilderService.emitFilter() / onFilter()`. `angular-gridster2`
is added to `package.json` for true drag-and-drop grid.

### Back-end (Java api-quality-engine)

`dashboard_layouts` Flyway table (V50) — id, tenant_id, user_id, name,
description, layout_json (jsonb), shared, signature_hash, version, timestamps.

`DashboardLayout` domain entity enforces invariants (name regex,
JSON object root, version bump on update, signature reset on update).
`DashboardLayoutService` enforces:
- write access = owner only (tenantId + userId both match JWT)
- read access = owner OR (sameTenant AND shared=true)
- cross-tenant access returns 404 (OWASP A01)
- name unique per (tenant, user)

REST: `POST/GET/PUT/DELETE /api/v1/dashboards/custom`. Tenant + user id from
JWT only (CLAUDE.md §18.2).

`signature_hash` is a placeholder for ML-DSA signature + Hyperledger Fabric
anchor (CLAUDE.md §11.4) — wired in a P5 sprint-2.

## Consequences

- Drag-and-drop UX matches Power BI / Tableau ergonomics — premium feel
  (project memory: "above MasterControl/ETQ/Qualio").
- A user owns their dashboards; sharing is opt-in per layout.
- Widgets degrade gracefully when ECharts is missing (placeholder pattern).

## Alternatives rejected

- Store layouts in MongoDB — would split the durable estate. PostgreSQL
  jsonb keeps it transactional and avoids a third datastore for a small
  payload.
- Standalone Angular components — banned by project memory.

## References

- CLAUDE.md §7.1, §7.3, §18.2.
- Files: `apps/web/src/app/features/dashboard-builder/`,
  `apps/api-quality-engine/.../dashboards/`,
  `apps/api-quality-engine/src/main/resources/db/migration/V50__create_dashboard_layouts.sql`.
