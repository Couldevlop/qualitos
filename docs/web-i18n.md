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

Les tables {id → 6 langues} vivent dans `apps/web/scripts/i18n/*.py`, **un
fichier par domaine** (core, methods, offline_queue, quality_ops, quality_docs,
pilotage, standards_privacy, grc_data1, grc_data2, ai_act, ai_cyber) : chaque
chantier ajoute son fichier sans toucher aux autres — pas de conflit de merge.
Un même id défini différemment dans deux fichiers = erreur fatale du générateur.

```bash
cd apps/web && python scripts/gen-i18n-xlf.py          # génère les 5 XLF
cd apps/web && python scripts/gen-i18n-xlf.py --check  # + vérif croisée code <-> tables
```

`--check` échoue (exit 1) si un `@@id` du code manque dans les tables — à
brancher en CI. `ng extract-i18n` reste utile en complément.

## Builds

| Commande | Résultat |
|---|---|
| `ng build` | build FR seul (comportement historique inchangé) |
| `ng build -c production,i18n-all` | 6 builds (`dist/qualitos-web/browser/{fr,en,es,ar,ja,zh}`) |

### Déploiement (routage livré)

L'image Docker `apps/web` construit les 6 locales (`npm run build:i18n`) et les
sert sous leur préfixe via `nginx.conf` :

- **Négociation** : `GET /` répond `302 /<locale>/` selon `Accept-Language`
  (`map $http_accept_language $qos_locale`). Match « commence par » sur la
  première langue (les variantes régionales `fr-CA`, `zh-Hans`… sont captées par
  le préfixe 2 lettres), filets de secours non ancrés pour les en-têtes où la
  langue cible n'est pas en tête. **Défaut : `fr`** (locale source).
- **Fallback SPA par locale** : `location /<locale>/` ⇒
  `try_files $uri $uri/ /<locale>/index.html` — on ne renvoie jamais l'index
  d'une autre locale (chaque build a son `<base href="/<locale>/">`).
- **Routes héritées sans préfixe** (favoris `/dashboard`…) : `302 /fr$request_uri`
  en dernier recours (302, pas 301 — la préférence peut changer).
- **Cache** : assets hashés `immutable` 1 an ; `index.html` / `ngsw.json` /
  `ngsw-worker.js` forcés en `no-cache` (sinon les déploiements ne se propagent
  pas). Headers de sécurité OWASP ré-émis sur toutes les réponses, redirections
  comprises.
- **Ingress K8s** : aucune réécriture de chemin n'est requise côté ingress ;
  laisser passer l'en-tête `Accept-Language` tel quel jusqu'au pod web pour que
  la négociation nginx fonctionne.

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

## Placeholders de balises imbriquées

Un élément i18n contenant `<strong>`/`<b>`/interpolation utilise les
placeholders canoniques Angular dans la table : `{$INTERPOLATION}` (puis
`{$INTERPOLATION_1}`…), `{$START_TAG_STRONG}`/`{$CLOSE_TAG_STRONG}` pour
`<strong>` (⚠ `START_BOLD_TEXT` = `<b>` uniquement). En cas de doute,
préférer le découpage en segments (`<strong i18n="@@x-title">` +
`<span i18n="@@x-text">`).

## Périmètre couvert / restant

- ✅ Infrastructure + shell (navigation, topbar, a11y) + composants partagés
  (form-dialog, confirm-dialog, `common.ok`/`common.confirm`).
- ✅ **TOUTES les features** (41) : méthodes qualité, qualité opérationnelle,
  pilotage, Standards Hub, GRC RGPD, AI Act, NIS 2, offline — **3 105 unités
  × 5 langues**, vocabulaire `common.*` partagé.
- ⏳ Hors périmètre assumé : libellés d'axes/légendes ECharts (config de
  graphique), valeurs d'enum backend affichées brutes (DRAFT, CRITICAL…),
  données mock des services, fragments non isolables mêlés à des ternaires
  dans `{{ }}`.
- Règle : **toute nouvelle chaîne UI naît avec son attribut i18n, son ID et
  son entrée de table** (`--check` la rattrape sinon).
