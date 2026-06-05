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

## Périmètre couvert / restant

- ✅ Infrastructure complète + shell (navigation 49 libellés, topbar) traduits
  dans les 5 langues (51 unités).
- ⏳ Les 41 features (templates) : à marquer progressivement avec des IDs
  `@@<feature>.*` et à ajouter à la table du générateur. Règle : **toute
  nouvelle chaîne UI naît avec son attribut i18n et son ID.**
