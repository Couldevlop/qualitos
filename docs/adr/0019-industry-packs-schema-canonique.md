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
