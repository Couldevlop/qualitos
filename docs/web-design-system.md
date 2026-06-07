# QualitOS — Design System (front)

> Refonte visuelle **« clair & aéré »** — direction Linear / Vercel / Notion (light) / Stripe Dashboard.
> Objectif business : placer QualitOS au-dessus de MasterControl / ETQ / Qualio (concurrents à l'UI vieillissante).

## Philosophie

Une interface **claire, aérée, quasi-flat**. Le contraste et la hiérarchie naissent de
**filets fins (1px) + ombres minimales + beaucoup de blanc**, jamais de fonds gris lourds
ni d'ombres dures. Accent **bleu profond et sobre** réservé aux actions et à l'état actif.

## Principes (non négociables)

1. **1px border + ombre minimale.** Une carte = `1px solid var(--qos-border-subtle)` + `var(--qos-shadow-xs)`. Pas de grosse ombre.
2. **Rayons 10–12px.** Cartes / dialogs / inputs / boutons : `--qos-radius-md` (10px) ou `--qos-radius-lg` (12px). Les pills restent `--qos-radius-pill`.
3. **Neutres froids.** App `#fcfcfd` (quasi-blanc), surfaces élevées blanc pur, bordures ultra-subtiles (`#eceef2` ≈ slate 8–10 %).
4. **Accent bleu profond.** Famille bleue sobre (`--qos-accent-bg: #2563d6`), ring focus **doux** (`--qos-ring-focus`, ~22 % d'opacité), jamais agressif.
5. **Air & densité confortable.** Padding cartes ~20–24px, espacements de page généreux, lignes de table aérées (52px).
6. **Toute nouvelle UI consomme les tokens.** Jamais de couleur, rayon ou ombre **en dur** : passer par les CSS custom properties `--qos-*`. Aucune exception sans ADR.

## Tokens clés (light → valeurs)

| Token                | Valeur               | Usage                                  |
| -------------------- | -------------------- | -------------------------------------- |
| `--qos-bg-app`       | `#fcfcfd`            | Fond application (quasi-blanc)         |
| `--qos-bg-elevated`  | `#ffffff`            | Surfaces élevées (cartes, dialogs)     |
| `--qos-bg-sunken`    | `#f9fafb`            | Headers de table, footers de panel     |
| `--qos-border-subtle`| `#eceef2`            | Filet 1px par défaut                   |
| `--qos-border-default`| `#e3e6eb`           | Bordure hover / inputs                 |
| `--qos-accent-bg`    | `#2563d6`            | Action primaire (bleu profond)         |
| `--qos-accent-bg-hover`| `#1d50b8`          | Hover action primaire                  |
| `--qos-shadow-xs`    | `0 1px 2px rgba(17,24,39,.035)` | Ombre carte (quasi nulle)   |
| `--qos-shadow-md`    | diffuse, douce       | Menus / overlays / hover carte         |
| `--qos-radius-md`    | `10px`               | Boutons, inputs, icon-buttons          |
| `--qos-radius-lg`    | `12px`               | Cartes, panels, dialogs                |
| `--qos-ring-focus`   | `0 0 0 3px rgba(37,99,214,.22)` | Anneau focus doux           |

La **sidebar** est désormais **claire** (`--qos-bg-sidebar: #f9fafb`), bordure droite fine,
item actif = fond bleu doux (`--qos-bg-sidebar-active`) + texte bleu (`--qos-fg-sidebar-active`)
+ barre d'accent gauche fine. Le bloc **dark** suit les mêmes principes sur surfaces ardoise.

## Où ça vit

- `apps/web/src/styles/_tokens.scss` — palette + tokens sémantiques light/dark, typo, espace, rayons, motion.
- `apps/web/src/styles.scss` — reset, surcharges Material 3 (cartes, boutons, form-fields, tables, dialogs, paginator, tabs, menus), `.qos-dialog-panel`.
- `apps/web/src/app/layout/main-shell/` — shell (sidebar claire + topbar blanche).
- `apps/web/src/app/shared/ui/**` — primitives (page-header, panel, kpi-card, status-pill, form/confirm-dialog).

## Scaffold de page cohérent

Toutes les pages partagent le même rythme **sans réécriture par feature** : les classes
récurrentes sont stylées **globalement** dans `styles.scss` via les tokens (light/dark).

| Classe          | Rôle                                                                       |
| --------------- | -------------------------------------------------------------------------- |
| `.qos-page`     | Conteneur de page : padding généreux, `max-width: 1440px`, pile `gap: 24px`.|
| `.page-header`  | Titre (`h1`, 24px/600) + sous-titre (`p`, secondaire) à gauche, **actions à droite**, filet bas. |
| `.filter-card`  | Barre de filtres : surface élevée, champs alignés et aérés.                |
| `.table-card`   | Carte de tableau sans padding (le `mat-table` remplit), coins arrondis nets.|
| `.clickable-row`| Ligne cliquable : hover subtil + `:focus-visible` (anneau interne accent). |

> Une page qui ne déclare que la **structure** hérite automatiquement des couleurs/espacements premium.
> Les SCSS de page ne doivent plus redéfinir couleurs/rayons en dur — seulement les teintes sémantiques par statut.

## États (loading / empty / error)

Primitives réutilisables, soignées, pilotées par tokens :

- **Loading** : `.state-row` — spinner accent centré + message secondaire, padding aéré.
- **Empty** : `.qos-empty` (ou `.empty` legacy) — `.qos-empty__icon` (pastille douce) + `.qos-empty__title` + `.qos-empty__hint` + `.qos-empty__cta`.
- **Error** : `.qos-error` — ton sémantique danger + icône, prévu pour un bouton de réessai.

Le `qos-panel` embarque déjà un **shimmer** de chargement ; `qos-kpi-card` des **skeletons**.

## Badges & sévérité (cohérence inter-modules)

- `.badge` — pastille statut neutre par défaut (fond sunken, filet subtil). Décliner avec des
  variantes locales qui ne changent **que** la teinte via `--qos-{success,warn,danger,accent}-bg-soft`/`-fg`.
- `.crit` — sévérité (LOW/MEDIUM/HIGH/CRITICAL), pill majuscule ; teintes douces sémantiques.
- Pour les nouveaux écrans : préférer la primitive `<qos-status-pill [tone]>` (`neutral|accent|success|warn|danger`).

## Règle boutons / icônes (FIX racine du « texte décalé »)

**Cause :** Material donne à `<mat-icon>` une boîte **24×24** alors que la police Material Symbols
rend le glyphe à **18px** dans les boutons → le glyphe se cale sur sa baseline pendant que le
libellé suit la sienne (décalage vertical optique), et MDC espace l'icône par des **marges
asymétriques** au lieu d'un gap homogène.

**Fix (global, `styles.scss`) :**

1. Le conteneur du bouton centre son contenu : `display:inline-flex; align-items:center; justify-content:center`.
2. `mat-icon` dans un bouton = boîte **1em** alignée sur le glyphe : `width/height:1em; font-size:18px; line-height:1; display:inline-flex; align-items/justify-content:center; vertical-align:middle; overflow:visible`.
3. Espacement icône↔libellé via un **gap cohérent** (`margin-right: var(--qos-space-2)`, `margin-left:0`) — les marges MDC héritées sont neutralisées.
4. `mat-icon-button` : icône seule **centrée** (glyphe 22px, marges nulles). Mêmes règles pour les icônes de `mat-menu`, chips, snackbar.

Couvre les 4 cas sans régression : flat+icône+texte, stroked+icône+texte, icon-button seul, bouton texte seul.

## Micro-interactions

- Transitions via tokens `--qos-motion-fast|base` (et `0ms` sous `prefers-reduced-motion`).
- Hover de carte/KPI : élévation douce (`shadow-xs → shadow-md`, `translateY(-1px)`).
- Lignes de table cliquables : feedback hover/active + `:focus-visible` (anneau interne accent).
- Material harmonisé : tooltip sobre sombre, snackbar à rayon + variantes `qos-snack-{success,warn,danger}`,
  paginator filet haut, spinner/progress/checkbox/radio/slide-toggle en **accent profond**.

## Accessibilité (WCAG 2.2 AA)

- Texte primaire `#111827` sur blanc ≈ 16:1 ; secondaire `#4b5563` ≈ 7:1 ; tertiaire `#6b7382` ≈ 4.8:1 — tous ≥ 4.5:1.
- Accent `#2563d6` sur blanc ≈ 5.0:1 (texte/lien OK) ; `--qos-fg-link`/`--qos-accent-fg-soft` = `#1d50b8` (plus contrasté).
- Focus toujours visible via `--qos-ring-focus` (anneau 3px) sur `:focus-visible`.
- Cibles tactiles ≥ 24px (icon-buttons 36px+, lignes de table 52px).
