# Console super-admin, cabinets et cloisonnement par client — spécification

> Statut : spécification arrêtée le 2026-11-05 (décisions prises avec l'architecte).
> Chaque lot reçoit son propre plan d'exécution dans `docs/superpowers/plans/`.
> Les décisions structurantes partent en ADR au moment de leur lot.

## 1. Le problème

Aujourd'hui, un `SUPER_ADMIN` ouvre la même application qu'un responsable qualité :
la navigation complète (une centaine d'écrans métier, onze groupes), à laquelle
s'ajoute `/admin/clients`. L'éditeur de la plateforme voit donc des écrans qui ne
lui appartiennent pas, et les routes métier lui restent atteignables à l'URL.

Trois manques s'y ajoutent :

- **Pas de cabinet.** L'étude stratégique (`docs/Partie_Prenantes/QualitOS_Etude_Strategique_Approfondie_Nov2026.docx`,
  §4.4 et §10) fait de la Console Cabinet un canal de distribution : un consultant
  QSE pilote plusieurs clients. Le modèle de données n'en connaît rien.
- **Pas d'identité de client.** `Tenant` porte `slug`, `name`, `plan`, `active`.
  Ni logo, ni slogan, ni mentions légales, ni couleurs, ni thème.
- **Pas de vue plateforme.** Ni statistiques par client / module / cabinet, ni
  piste d'audit inter-clients, ni catalogue des modules existants.

## 2. Périmètre de la console (et rien d'autre)

La console super-admin ne contient **que** de l'administration de plateforme :

| Écran | Contenu |
| --- | --- |
| **Clients** | Créer un client, son identité, son thème, ses modules souscrits, son administrateur ; suspendre, réactiver |
| **Cabinets** | Créer un cabinet, ses consultants, les clients de son portefeuille |
| **Catalogue des modules** | Tous les modules que la plateforme sait livrer : code, libellé, description, prix, nombre de clients qui l'ont activé |
| **Paramétrage plateforme** | Réglages centraux (édition, quotas, fournisseur d'IA par défaut, rétention, bornes) |
| **Piste d'audit** | Journal inter-clients, scellé, filtrable par client, acteur, type d'événement |
| **Statistiques** | Par client, par module, par cabinet |

Aucun écran qualité n'est accessible à ce rôle : ni dans le menu, ni à l'URL.

## 3. Décisions arrêtées

### D1 — Console séparée, coquille dédiée

Arborescence `/platform`, coquille propre (`PlatformShellComponent`), thème
éditeur. Le `SUPER_ADMIN` qui atteint une route métier est **refusé par une garde**,
pas seulement privé de son entrée de menu. Réciproquement, `/platform` est refusé à
tout rôle de client.

**Pourquoi** : la frontière devient structurelle. Un filtre de navigation est
déclaratif — il se contourne en tapant l'URL, et le prochain écran ajouté
l'oublie.

### D2 — Cabinet = portefeuille de clients (Console Partner)

Le cabinet est une entité propre :

- il a ses **consultants** (utilisateurs du cabinet, pas du client) ;
- il a un **portefeuille** : les clients qu'il accompagne ;
- l'accès d'un consultant dans un client est **borné et accordé par le client**
  (lecture, ou intervention), révocable, daté, journalisé ;
- l'éditeur voit la santé du portefeuille et les statistiques par cabinet.

Un consultant n'entre jamais chez un client sans accord enregistré de ce client.

### D3 — Chiffrement : clé par client **et** base dédiée pour les régulés

Deux niveaux, cumulables :

1. **Clé par client sur les colonnes sensibles.** Chaque client reçoit sa clé
   (Vault Transit, enveloppe). Les colonnes qui portent de la donnée sensible sont
   chiffrées avec SA clé. Conséquences : une copie de base ne livre rien sans les
   clés ; révoquer un client se fait en détruisant sa clé (*crypto-shredding*) ;
   les colonnes chiffrées ne sont plus requêtables en clair — le choix des colonnes
   est donc un acte d'architecture, pas un réglage.
2. **Base dédiée** pour les clients qui l'exigent (santé, défense, finance) :
   le niveau 2 de `CLAUDE.md` §10.3, rendu réellement opérationnel.

Le RLS (ADR 0086) reste la première barrière. Le chiffrement est la seconde : il
protège ce que le RLS ne protège pas — une sauvegarde volée, un accès direct à la
base, une erreur de politique.

### D4 — Identité du client : champs + thème au choix

À la création : logo, slogan, mentions légales (raison sociale, forme, SIREN/TVA,
adresse, contact), couleur principale, couleur secondaire, et **un thème parmi une
liste** de jeux cohérents (clair dense, clair aéré, sombre, contrasté). Pas
d'éditeur de jetons libre : le contraste reste vérifiable (WCAG 2.2 AA, `CLAUDE.md`
§15.1) et la surface testable.

Les couleurs choisies alimentent les jetons `--qos-*` existants
(`apps/web/src/styles/tokens`), appliqués au chargement du client.

### D5 — Admin du client : l'équipe qualité et ses habilitations

Le super-admin définit le **périmètre** (modules souscrits, options, quotas).
L'admin du client est souverain **dans** ce périmètre : il crée les membres de
l'équipe qualité et décide qui a droit à quoi. Il ne peut jamais s'accorder un
module hors souscription — la garde est au serveur, pas dans l'écran.

Cela étend l'existant (`/admin/team`, `/admin/roles`, matrice §16) plutôt que de le
remplacer.

## 4. Lots d'exécution

| Lot | Contenu | Dépend de |
| --- | --- | --- |
| **1** | Coquille `/platform`, gardes des deux côtés, écran Clients (identité + thème + modules souscrits), catalogue des modules | — |
| **2** | Cabinets : entité, consultants, portefeuille, accord d'accès du client, console du cabinet | 1 |
| **3** | Paramétrage central + piste d'audit inter-clients (lecture privilégiée, scellée, journalisée elle-même) | 1 |
| **4** | Statistiques par client / module / cabinet | 1, 2 |
| **5** | Chiffrement par client (clé Vault, colonnes sensibles) puis base dédiée pour les régulés | — (parallélisable) |
| **6** | Équipe qualité et habilitations de l'admin client, bornées au périmètre souscrit | 1 |

## 5. Invariants qui s'appliquent à tous les lots

- `tenant_id` jamais lu du corps d'une requête (`CLAUDE.md` §18.2.2) — y compris
  pour un super-admin : son client cible voyage dans le chemin, et l'appel est
  journalisé comme accès privilégié.
- Toute écriture de la console est un événement de piste d'audit, scellé
  (`AuditEventHasher`).
- Clean Architecture, OWASP, couverture ≥ 98 % sur le code neuf.
- Aucune logique sectorielle en dur ; aucun module en dur côté écran : le
  catalogue vient du serveur.
- Un accès de consultant chez un client sans accord enregistré est un défaut de
  sécurité, pas une commodité.

---

## 6. Ajout du 2026-11-05 — la plateforme 100 % paramétrable

**Exigence** : aucune donnée en dur dans le code ; toute liste déroulante est
paramétrable par le client.

### 6.1 Ce que l'état des lieux montre

Mesuré sur la branche du 2026-11-05 :

| Mesure | Nombre |
| --- | --- |
| Gabarits portant un `mat-select` | 143 |
| Listes de valeurs déclarées en dur côté écran (tableaux typés) | ~119 |
| Énumérations Java du moteur qualité | 220 |

Un précédent existe et fonctionne : les **natures d'écart** de la non-conformité
(V154, ADR 0103) — catalogue par client, valeurs par défaut fournies, écran
d'administration, filtre `subcategoryCode`. Le chantier généralise ce patron.

### 6.2 La distinction qui évite de casser le produit

Les 220 énumérations ne sont pas de même nature, et les traiter pareil
casserait le moteur :

- **Nomenclatures** (catégories, natures, origines, familles, sites, motifs,
  types de preuve, unités…) : ce sont des **listes de valeurs du client**. Elles
  deviennent paramétrables, valeur par valeur, avec libellé, code, ordre,
  activation, et un jeu par défaut livré par l'Industry Pack.
- **États de cycle de vie** (statut CAPA, phases DMAIC, statut d'une version de
  document, sévérités qui portent un calcul) : ils portent des **règles métier**
  — gardes de transition, calculs, migrations, clauses normatives. Les rendre
  librement éditables rendrait le moteur incohérent. Pour ceux-là, le client
  paramètre **le libellé** (« Écart majeur » plutôt que « MAJOR »), jamais le code
  ni les transitions.

Dire ceci explicitement, c'est tenir la promesse « rien en dur » sans promettre
qu'un client pourra supprimer la phase « Mesurer » d'un projet Six Sigma.

### 6.3 Conception

Un **registre de listes de valeurs** unique, par client :

```
value_set        (tenant_id, set_code, label, scope, system, ordered)
value_set_entry  (tenant_id, set_code, code, label_fr…label_zh, position,
                  active, system_default, color, meta jsonb)
```

- Lecture par un seul point d'entrée `GET /api/v1/value-sets/{setCode}` (cache
  Redis par client, invalidé à l'écriture).
- Écriture réservée à l'**admin du client** (`feedback_parametrage_admin_tenant`),
  journalisée, scellée.
- Un composant d'écran unique (`qos-value-select`) remplace les 143 `mat-select`
  nourris en dur, au rythme d'un module par lot.
- Les jeux par défaut sont livrés **en Industry Pack** (`CLAUDE.md` §18.2.9), pas
  en code : un nouveau secteur apporte ses nomenclatures sans toucher au cœur.
- Les entrées `system_default` ne se suppriment pas : elles se désactivent. Une
  valeur déjà employée par une fiche ne se supprime jamais — l'historique doit
  rester lisible.

### 6.4 Lot

| Lot | Contenu | Dépend de |
| --- | --- | --- |
| **7** | Registre de listes de valeurs : socle (table, API, cache, écran d'administration, `qos-value-select`), puis migration module par module des 143 écrans | 1 (écran d'administration dans la console client) |

Le socle est un lot court ; la migration des écrans est un travail au long cours,
mesurable (nombre de `mat-select` encore nourris en dur), à mener module par module
sans jamais laisser un écran à moitié migré.
