# ADR 0019 — Industry Packs : schéma canonique unique et convergence du loader

- **Statut** : accepté
- **Date** : 2026-06-06
- **Contexte** : CLAUDE.md §5 (Domain Adapter Layer, 14 packs sectoriels)

## Contexte

Un audit du mécanisme Industry Packs (2026-06-06) révèle **trois implémentations
parallèles et déconnectées** :

| Chemin | Localisation | Schéma | Branché au runtime |
|---|---|---|---|
| A | `api-quality-engine …/industry` + `resources/industry-packs/*.yml` | plat (kpis = slugs) | **OUI** (loader → DB → REST → activation tenant) |
| B | `libs/industry-commons` (Clean Architecture) | riche v3 (`id`, `supported_norms`, `seed_causes`) | non (déclaré par iot-hub seulement) |
| C | `libs/industry-packs/packs/*.yaml` (contenu P4 : 6 packs) | riche v2 (`pack_id`, KPIs §6.6 complets, Ishikawa 6M, Poka-Yoke) | non (lu uniquement par un test) |

Conséquences : seuls 3 packs (manufacturing, healthcare-hospital, it-itsm) sont
activables ; les 6 packs P4 (banking, pharma, agro, aerospace, automotive,
public) sont du **contenu mort** malgré des tests verts ; 5 secteurs de §5.2
sont absents (BTP, Énergie, Éducation, Retail, Logistique) ; l'activation d'un
pack n'a **aucun effet de provisionnement** ; aucune validation référentielle
(slugs de normes/KPIs libres).

## Décision

1. **Le schéma riche éditorial (chemin C) devient le schéma canonique** des
   manifestes de packs — c'est le seul qui honore §5.1 (KPIs définis §6.6,
   templates Ishikawa 6M, bibliothèque Poka-Yoke, normes, glossaire,
   connecteurs).
2. **Le loader runtime (chemin A) converge vers ce schéma** : il apprend à
   lire le schéma riche, en conservant la rétro-compatibilité de lecture du
   schéma plat existant (les 3 packs actuels restent chargés tels quels tant
   qu'ils ne sont pas portés).
3. **Le contenu vit dans le classpath de l'engine**
   (`api-quality-engine/src/main/resources/industry-packs/*.yml`) — les 6 packs
   P4 y sont **portés** (et `libs/industry-packs` devient un répertoire
   éditorial historique, marqué déprécié dans son README).
4. **Validation référentielle au chargement** : les codes de `norms` sont
   vérifiés contre le catalogue Standards Hub (60 normes) — WARN explicite par
   référence inconnue, le pack reste chargé (tolérance contenu), métrique de
   santé exposée.
5. Le **provisionnement à l'activation** (`apply()` matérialisant KPIs,
   glossaire, templates chez le tenant) et l'**écran d'administration front**
   sont actés comme **Phase 2** (chantier code distinct) — hors périmètre de
   cette décision.
6. `libs/industry-commons` (chemin B) reste la lib de modèle utilisée par
   iot-hub ; son unification complète avec l'engine est différée à la Phase 2.

## Conséquences

- Ajouter un secteur = écrire UN fichier YAML riche au bon endroit — plus
  d'ambiguïté de schéma/répertoire/extension.
- Les 6 packs P4 cessent d'être morts : chargés, exposés par l'API, activables.
- Les 5 secteurs manquants de §5.2 peuvent être produits en contenu pur.
- Dette explicite : l'activation reste déclarative (sans effet) jusqu'à la
  Phase 2 — documenté dans la réponse de l'API (champ description du pack).
- Le test de contrat P4 (`P4IndustryPacksYamlTest`) migre vers les ressources
  de l'engine et s'étend aux nouveaux packs.

## Phase 2 — provisionnement (LIVRÉ pour les KPIs, 2026-06-06)

La décision §5 (« le provisionnement à l'activation matérialisant KPIs… »)
était actée comme chantier distinct. **Statut : LIVRÉ pour les KPIs.**

### Ce qui est matérialisé

À l'activation d'un pack (`POST /api/v1/industry-packs/{code}/activate`),
`IndustryPackProvisioningService.provision(...)` est appelé par
`IndustryPackService.activate(...)` **après** l'enregistrement de l'activation,
**dans la même transaction**. Pour chaque KPI riche (§6.6) du manifeste, une
`KpiDefinition` du tenant est créée (statut `DRAFT`) **si aucune n'existe déjà
avec ce code** (`code = kpi_id` du pack).

- **Idempotence / non-écrasement** : re-activation, ou second pack partageant un
  `kpi_id` → `skip`, jamais d'écrasement. Une collision de code pré-existant
  produit un `warning` et conserve le KPI du tenant en l'état.
- **Packs PLATS** (sans `richKpis`) : aucun provisionnement, comportement
  inchangé.
- **Résilience contenu** : l'échec de provisionnement d'**un** KPI (mapping,
  persistance) devient un `warning` dans le résultat ; **pas de rollback** de
  l'activation ni interruption des autres KPIs.
- **Désactivation** : **ne supprime rien**. Les KPIs provisionnés appartiennent
  désormais au tenant (catalogue éditable, mesures et CAPA attachées) ; les
  supprimer serait destructeur.

### Mapping tolérant pack → `KpiDefinition`

| Champ pack | `KpiDefinition` | Règle |
|---|---|---|
| `kpi_id` | `code` | clé d'idempotence |
| `name` / `category` / `unit` | idem | repli `name` ← `kpi_id` si vide |
| `formula` + `explainability` | `description` | `formula + ' — ' + explainability`, tronqué à 2000 |
| `target` / `threshold_warning` / `threshold_critical` | `targetValue` / `thresholdWarning` / `thresholdCritical` | **premier nombre décimal** de la chaîne (gère `>= 95 %`, `< 2.5`, `0,5` virgule FR, `80% budget`) → `BigDecimal` ; `null` si aucun nombre (jamais d'échec) |
| `target` (opérateur) | `direction` | `>=`/`>` → `HIGHER_IS_BETTER` ; `<=`/`<` → `LOWER_IS_BETTER` ; défaut `HIGHER_IS_BETTER` |
| `refresh_frequency` | `frequency` | mappé vers l'enum (`realtime`/`per-event` → `REALTIME`, `annual` → `YEARLY`…) ; défaut `MONTHLY` |
| (utilisateur qui active) | `ownerUserId` + `createdBy` | `activatedBy` du flux d'activation |

### Réponse d'activation enrichie (rétro-compatible)

`ActivationResponse` reçoit un champ **additif** `provisioning`
(`{ kpisCreated, kpisSkipped, warnings[] }`), `null` pour les réponses qui ne
provisionnent pas (activation idempotente déjà active, désactivation, listes).

### Hors périmètre (assumé)

Le **glossaire**, les **templates Ishikawa 6M/7M** et la **bibliothèque
Poka-Yoke** restent **exposés via le `manifest_json`** du pack **sans
matérialisation** chez le tenant. Leur provisionnement (création d'entités
dédiées) est différé : les modules cibles n'ont pas encore de table « seed »
multi-tenant équivalente au catalogue KPI. Dette explicite, à reprendre quand
ces modules exposeront un point d'entrée de provisionnement.
