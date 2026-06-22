# ADR 0034 — Interactivité premium des dashboards (cross-filtering, drill-down, annotations collaboratives)

- **Statut** : Accepté
- **Date** : 2026-06-20
- **Owners** : @Couldevlop
- **Phase** : N1 1.3 (différenciateurs commerciaux, CLAUDE.md §7.3)

## Contexte

CLAUDE.md §7.3 exige des dashboards « premium » : **cross-filtering** (cliquer un
point filtre tous les widgets), **drill-down infini** (du KPI agrégé vers le
détail) et **annotations collaboratives** (commentaires partageables sur un
graphique). Les concurrents (MasterControl/ETQ) offrent des dashboards statiques ;
c'est un levier de différenciation. Le dashboard exécutif existait déjà
(`apps/web/.../features/dashboard`) avec ECharts, mais sans interactivité ni
persistance.

Deux contraintes :

1. **Persistance des annotations** — elles doivent survivre à la session,
   être horodatées et attribuées à leur auteur. Cela impose un stockage serveur,
   donc une feature backend complète, multi-tenant.
2. **Sécurité (§18.2 #2 / OWASP A01)** — l'auteur d'une annotation et le tenant
   ne peuvent provenir du corps de requête (falsifiable). La suppression doit
   être réservée à l'auteur ou à un admin du tenant.

## Décision

**Front** — un état de cross-filtering **partagé à la page** (`CrossFilterService`,
fourni au niveau du `DashboardModule` lazy) : un clic sur une barre du Pareto pose
un filtre `(dimension, valeur)` ; tous les widgets s'y abonnent (atténuation des
catégories non sélectionnées), le drill-down niveau 2 affiche les sous-causes, et
l'annotation hérite de la catégorie comme ancre. Recliquer la même valeur agit en
toggle ; un bouton « Effacer » annule. Le clic est capté via un nouvel
`@Output() pointSelected` sur le composant partagé `qos-echart`.

**Backend** — sous-package hexagonal
`apps/api-quality-engine/.../quality/dashboards/annotations`
(domain pur / application / infrastructure JPA / web), endpoints
`/api/v1/dashboards/annotations` :

| Verbe | Chemin | Rôle |
|---|---|---|
| GET | `/api/v1/dashboards/annotations?chartKey=` | liste (tenant courant) |
| POST | `/api/v1/dashboards/annotations` | créer (auteur = sub JWT) |
| DELETE | `/api/v1/dashboards/annotations/{id}` | supprimer (auteur ou admin) |

Migration `V90__create_dashboard_annotations.sql`. `tenantId`/`authorId`
proviennent du JWT (réutilisation du `dashboardsTenantContextProvider` existant) ;
le corps est du texte brut, échappé à l'affichage par interpolation Angular
(jamais `innerHTML`). La capacité « admin tenant » est lue via un port `ActorRoles`
(adapter sur les authorities JWT `ROLE_ADMIN`/`ROLE_ADMIN_TENANT`/`ROLE_SUPER_ADMIN`).

## Justification

- **Cross-filter mono-valeur** : comportement Power BI le plus lisible, évite
  l'empilement d'états opaques. Annulable et testable (un `BehaviorSubject`).
- **Drill-down via données du référentiel** : sous-causes 6M servies par le
  service dashboard, branchées sur la catégorie filtrée — cohérence cross-filter ↔
  drill-down sans état dupliqué.
- **Annotations persistées** : seules des données serveur garantissent partage,
  horodatage et attribution fiables. Hexagonal pour rester testable à 98 %.
- **Auteur/tenant depuis le JWT** : invariant §18.2 #2 ; la suppression
  auteur-ou-admin évite à la fois le vandalisme et le verrouillage.

## Conséquences

- ✅ Dashboards interactifs réels (clic → filtre propagé → drill-down → annotation).
- ✅ `qos-echart` réutilisable émet désormais les clics — bénéficie à tous les
  graphiques (SPC, etc.).
- ⚠ Le carve-out `DELETE /api/v1/dashboards/annotations/**` = `authenticated()`
  dans `SecurityConfig` (l'autorisation fine auteur/admin est au use-case) — sinon
  la règle DELETE générique (Manager Qualité+) verrouillerait l'auteur « user ».
- ⚠ Cross-filter mono-valeur : le multi-sélection est hors périmètre (extension
  future possible sans changer le contrat).

## Tests d'invariant

- Golden-master backend `DashboardInteractivityGoldenPathTest` : création →
  lecture → suppression, isolation cross-tenant (404), autorisation (403 non-auteur,
  OK admin).
- Golden front `cross-filter.service.spec.ts` : clic → filtre propagé → re-clic
  toggle → clear.
- Service/contrôleur : isolation tenant, validation `chartKey`, 403/404 mappés.
- Couverture JaCoCo du package `dashboards/annotations` : 100 % lignes & branches.

## Références

CLAUDE.md §7.1/§7.3 (dashboards premium), §15 (design), §18.2 #2 (tenant via JWT),
§16 (rôles) ; ADR 0001 (multi-tenant JWT), 0002 (Angular NgModules), 0020 (matrice rôles).
