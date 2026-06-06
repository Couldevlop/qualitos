# Internationalisation (i18n) — apps/web

> CLAUDE.md §15.1 : FR, EN, ES, AR (RTL), JA, ZH — **Angular i18n natif + ICU**
> (décision tranchée, pas de lib runtime).

## Architecture

- Locale source : **fr**. 5 locales cibles déclarées dans `angular.json`
  (`i18n.locales`) → `src/locale/messages.<lang>.xlf`.
- **IDs explicites** partout (`@@nav.*`, `@@shell.*`) : les traductions ne
  dépendent pas de l'ordre d'extraction, les diffs restent lisibles.
- Templates : attributs `i18n` / `i18n-placeholder="@@id"`.
  Chaînes TypeScript (ex. libellés de navigation) : tagged template
  `` $localize`:@@nav.audits:Audits` `` (polyfill `@angular/localize/init`).
- **RTL** : chaque locale étant un build dédié, `AppComponent` fixe
  `document.dir = 'rtl'` au bootstrap quand `LOCALE_ID` commence par `ar`.

## Fichiers de traduction — générés, pas édités

`apps/web/scripts/gen-i18n-xlf.py` détient la table {id → 6 langues} et génère
les 5 XLF (une seule source de vérité, ajout d'une langue = une colonne) :

```bash
cd apps/web && python scripts/gen-i18n-xlf.py
```

`ng extract-i18n` reste utile pour détecter les chaînes non couvertes.

## Builds

| Commande | Résultat |
|---|---|
| `ng build` | build FR seul (comportement historique inchangé) |
| `ng build -c production,i18n-all` | 6 builds (`dist/qualitos-web/browser/{fr,en,es,ar,ja,zh}`) |

Déploiement : servir chaque locale sous son préfixe (`/en/`, `/ar/`…) — routage
nginx/ingress par préfixe + négociation `Accept-Language` à ajouter au moment
du go-live multilingue.

## Placeholders d'interpolation

Pour une chaîne TS avec interpolation, nommer le placeholder côté code :
`` $localize`:@@spc.analyze.err-generic:Échec (HTTP ${err.status}:status:).` ``
et l'écrire `{$status}` dans la table du générateur — il devient `<x id="status"/>`
dans les XLF. **Jamais** d'interpolation anonyme dans une chaîne traduite.

## Vocabulaire commun

Les termes transverses (boutons, états, erreurs génériques) ont un ID `common.*`
unique réutilisé par tous les écrans (ex. `common.save`, `common.error-loading`,
`common.maxlength-255`). Règle Angular : même ID ⇒ même source FR au caractère
près. Consulter la section « vocabulaire commun » du générateur avant de créer
un nouvel ID.

## Périmètre couvert / restant

- ✅ Infrastructure complète + shell (navigation 49 libellés, topbar).
- ✅ **Les 6 modules méthodes** (PDCA, 5S, Ishikawa, DMAIC, SPC, Cercles) :
  27 templates + chaînes TS (snackbars, dialogs de confirmation) — 477 unités
  × 5 langues au total, vocabulaire `common.*` partagé.
- ✅ Page « File d'attente offline » (`offline.queue.*`).
- ⏳ Les ~34 features restantes (CAPA, Audits, Documents, GRC…) : à marquer
  progressivement avec des IDs `@@<feature>.*`. Règle : **toute nouvelle chaîne
  UI naît avec son attribut i18n et son ID.**
- ⏳ Chaînes signalées non marquées (ternaires dans interpolations, libellés
  d'axes ECharts, `aria-label` dynamiques concaténés) — nécessitent un léger
  refactor TS (getter `$localize`) ; listées dans les commentaires de revue.
