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
| **7** | Abonnement à la création, compteurs d'usage, écran de compteur côté client (§8) | 1 |
| **8** | Moteur de reprise de données du client : modèles Excel engendrés, contrôle à blanc, écriture transactionnelle (§9) | 6 pour l'écran, moteur parallélisable |

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

---

## 7. Ajout du 2026-11-05 — on-premise : la règle qui évite de casser plus tard

### 7.1 Le piège, rencontré au premier commit du lot 1

L'identité d'un client (logo, slogan, couleurs, mentions légales) a d'abord été
posée sous `/api/v1/tenants/{id}/identity`, parce que c'est le super
administrateur qui provisionne un client. Deux conséquences, toutes deux
silencieuses :

- `/api/v1/tenants/**` porte `@SaasOnly` (ADR 0082) : en on-premise, la route
  **répond 404**. Un client on-premise — celui qui a le plus de raisons de poser
  SA marque sur SON installation — n'aurait eu aucun chemin pour le faire ;
- la route est réservée au rôle de l'éditeur : en SaaS, un **administrateur de
  client** aurait dû demander à l'éditeur de changer son slogan, alors que tout
  paramétrage lui revient (`feedback_parametrage_admin_tenant`).

Le défaut n'aurait été vu qu'à la première installation on-premise, c'est-à-dire
après la vente.

### 7.2 La règle

> **Une surface d'API appartient à exactement une des deux familles, et le
> choix se fait à l'écriture, pas à la relecture.**

| Famille | Chemin | `@SaasOnly` | Qui | Existe en on-premise |
| --- | --- | --- | --- | --- |
| **Éditeur** | `/api/v1/platform/**`, `/api/v1/tenants/**` | oui | `SUPER_ADMIN` | non (404) |
| **Client** | tout le reste | non | rôles du client | oui |

Trois corollaires qui tranchent les cas réels :

1. **Toute capacité dont un client a besoin pour s'administrer lui-même vit dans
   la famille « client »**, même si l'éditeur s'en sert aussi pour dépanner. Le
   dépannage se fait alors avec le rôle `SUPER_ADMIN` sur la route du client, pas
   avec une seconde route. Deux routes pour une capacité, c'est deux contrôles
   d'accès à tenir en phase, donc un qui dérivera.
2. **La famille « éditeur » ne contient que ce qui n'a aucun sens hors SaaS** :
   créer un client, facturer, comparer des clients entre eux, gérer un cabinet.
   En on-premise, il n'y a qu'un client et personne à facturer.
3. **Un écran de la console de l'éditeur n'est jamais le seul chemin vers une
   donnée du client.** Sinon l'installation on-premise perd la donnée avec
   l'écran.

### 7.3 Ce que cela change pour les lots

- Lot 1 : l'identité et le logo vivent sous `/api/v1/branding` (famille client,
  hors `@SaasOnly`) ; `/api/v1/tenants/{id}/identity` reste pour le
  provisionnement par l'éditeur. **Fait.**
- Lot 3 : le paramétrage **central** (de la plateforme) est famille éditeur ; le
  paramétrage **du client** est famille client. Ce ne sont pas les mêmes écrans
  et ce ne sont pas les mêmes tables.
- Lot 4 : les statistiques inter-clients sont famille éditeur ; les statistiques
  d'un client sur lui-même existent déjà côté client et n'y montent pas.
- Lot 5 : le chiffrement par client doit fonctionner **sans Vault** en
  on-premise (coffre local scellé, même interface). Une installation qui ne peut
  pas chiffrer est une installation qui ne peut pas être vendue aux régulés, et
  ce sont eux qui installent chez eux.
- Lot 6 : les habilitations de l'équipe qualité sont famille client — en
  on-premise, c'est le seul endroit où elles existent.

### 7.4 Comment c'est tenu

Un test de garde, et non une relecture : `OnPremEditionRoutesTest` énumère les
contrôleurs portant `@SaasOnly` et échoue si l'un d'eux expose un chemin hors de
la liste des préfixes de la famille éditeur. Ajouter une route d'éditeur sous un
chemin de client casse le banc, pas la vente.

---

## 8. Ajout du 2026-11-05 — l'abonnement à la création du client

### 8.1 La demande

À la création d'un client, pouvoir l'associer à un **type d'abonnement** pour
les clients SaaS, la facturation se faisant **par nombre d'utilisateurs, par
site et par module**. Et faire mieux que les leaders, avec une particularité qui
marque la différenciation.

### 8.2 Ce qui existe déjà (ne pas réinventer)

Le socle de facturation est en place (PR #134) : `Subscription`, `ModulePrice`,
`BillingTier` (`FREE`, `STANDARD`, `PRO`, `ENTERPRISE`), `ModuleActivationPort`,
et `Tenant.Plan`. Ce qui manque n'est pas le moteur, c'est **la forme
commerciale** et son **branchement à la création**.

### 8.3 Ce que font les leaders, et pourquoi c'est critiquable

L'étude stratégique (`docs/QualitOS_Etude_Strategique_Approfondie_Nov2026.docx`)
situe les packs à 1 200 / 2 400 / 4 500 € par mois, avec « extension tarifaire
par site ou par utilisateur avancé ».

Le marché facture **au siège nommé**, souvent avec un plancher annuel :
MasterControl et ETQ vendent des licences par utilisateur nommé, engagement
pluriannuel, modules en supplément. L'effet pervers est connu et mesurable : le
client **rationne les comptes**. L'opérateur de terrain qui devrait déclarer une
non-conformité ne le fait pas parce qu'il n'a pas de licence ; le responsable
qualité saisit à sa place, deux jours plus tard, de mémoire. **Le modèle
tarifaire dégrade la donnée qualité qu'il est censé servir.**

### 8.4 La proposition QualitOS : trois axes, et un seul compté

La facturation reste **par utilisateur, par site et par module** comme demandé,
mais avec une distinction que personne ne fait :

> **Seuls les utilisateurs qui PILOTENT sont comptés. Ceux qui ALIMENTENT sont
> gratuits et illimités.**

| Rôle | Ce qu'il fait | Compté |
| --- | --- | --- |
| **Contributeur** | déclare une NC, remplit un audit 5S, propose une idée, suit une formation, consulte ses tâches | **non — illimité** |
| **Pilote** | crée et pilote des cycles, valide, signe, configure, exploite les tableaux de bord | **oui** |

Pourquoi c'est un avantage et pas un cadeau :

- **La valeur de la plateforme croît avec le nombre de contributeurs**, pas avec
  le nombre de licences. Un QMS nourri par 400 opérateurs vaut mieux qu'un QMS
  nourri par 12 ; c'est le volume de données qui fait fonctionner la détection
  d'anomalies, le regroupement de NC et les prévisions. Facturer le contributeur,
  c'est facturer notre propre carburant.
- **Le client cesse de rationner.** Argument de vente frontal face à
  MasterControl : « chez eux, chaque opérateur coûte ; chez nous, aucun ».
- **Le coût marginal d'un contributeur est réel mais faible** (stockage de
  photos, notifications), et il est couvert par les axes site et module.

Les deux autres axes portent la valeur :

- **par site** : un site, c'est un périmètre d'audit, une hiérarchie ISA-95, des
  équipements, une piste d'audit séparée — un coût de plateforme réel ;
- **par module** : le client paie ce qu'il a activé, et **la désactivation prend
  effet à la période suivante sans perte de données** (§4, lot 1).

### 8.5 La particularité qui marque la différenciation

**Le palier est atteint par l'usage, pas négocié à l'avance.** La plateforme
mesure en continu les pilotes actifs, les sites et les modules, et expose au
client **son propre compteur** dans son paramétrage — le même chiffre que celui
qui facture. Trois conséquences :

1. **Aucune surprise de facture** : le client voit venir le franchissement de
   palier avant qu'il n'arrive, avec la date et le montant.
2. **Aucun audit de licence** — la pratique que tout acheteur de logiciel
   d'entreprise redoute. Il n'y a rien à auditer : le compteur est le même des
   deux côtés, et il est dans la piste d'audit.
3. **Le dépassement n'interrompt pas le service.** Un pilote de plus un jour
   d'audit ne bloque pas l'audit : il est enregistré, facturé au prorata, et dit.
   Bloquer un travail qualité pour une question de licence, c'est transformer un
   problème commercial en incident qualité.

### 8.6 Conception

- À la création d'un client (écran Clients, lot 1) : choix du **palier**, du
  **nombre de sites**, et des **modules souscrits**. Le palier borne le nombre de
  pilotes ; les contributeurs ne sont pas bornés.
- `Subscription` gagne `siteCount` et `pilotSeats` ; `BillingTier` conserve ses
  quatre valeurs et porte désormais un plafond de pilotes **par défaut**,
  surchargeable par client (un contrat se négocie, le code ne doit pas l'empêcher).
- Un compteur `UsageSnapshot` quotidien (pilotes actifs, sites, modules actifs)
  alimente à la fois la facture et l'écran du client. **Même source, même
  chiffre** — c'est tout l'intérêt.
- En on-premise : pas d'abonnement, une **licence** (`MemberLimit` existant).
  L'écran de compteur reste, pour que le client sache où il en est de sa licence.

### 8.7 Lot

**Lot 7** — abonnement à la création, compteurs d'usage, écran de compteur côté
client. Dépend du lot 1 (écran Clients) et réutilise le socle de facturation.

---

## 9. Ajout du 2026-11-05 — reprise de données par le client

### 9.1 La demande

Le client doit pouvoir faire **lui-même** sa reprise de données depuis son
onglet paramétrage : **télécharger des modèles Excel**, les remplir, les
**téléverser**. Les fichiers doivent être **contrôlés par un moteur** pour que
n'importe quoi ne puisse pas y être glissé et compromettre la sécurité ou
l'intégrité des données.

### 9.2 Pourquoi c'est un module et non un écran

Une reprise de données est l'opération la plus dangereuse qu'un client puisse
faire sur sa propre plateforme, et pour trois raisons distinctes :

- **sécurité** — un classeur est un format exécutable : macros, liens externes,
  objets OLE, formules qui appellent le système (`=cmd|…!A1` dans DDE), et le
  tableur du poste qui l'ouvre fait le reste. Le fichier téléversé est aussi
  **relu par un humain** ensuite, souvent hors de la plateforme ;
- **intégrité** — une ligne mal typée, un code en double, un responsable
  inexistant, une date impossible : acceptée, elle contamine un référentiel que
  des mois de travail prendront pour vrai. Une NC importée avec le mauvais site
  faussera les indicateurs de deux sites ;
- **irréversibilité perçue** — un import de 4 000 lignes qui échoue à la
  ligne 3 999 après avoir écrit 3 998 lignes laisse un référentiel dans un état
  que personne ne sait décrire.

### 9.3 Décision D6 — le moteur de reprise

> **Un import est une transaction, précédée d'un contrôle à blanc dont le client
> voit le résultat avant d'autoriser l'écriture.** Rien n'est écrit tant qu'il
> n'a pas vu ce qui va l'être.

Quatre étapes, dans cet ordre, et aucune n'est optionnelle :

| # | Étape | Ce qui est refusé |
| --- | --- | --- |
| 1 | **Recevabilité du fichier** | type hors allow-list, poids, contenu démenti par les premiers octets, **macros** (`.xlsm`, `vbaProject.bin`), liens externes, objets incorporés, bombe de décompression (ratio, nombre d'entrées, taille déclarée) |
| 2 | **Conformité au modèle** | colonnes manquantes ou en trop, modèle d'une autre version, feuille renommée, en-têtes déplacés |
| 3 | **Contrôle ligne à ligne, à blanc** | type, format, obligation, longueur, valeur hors nomenclature du client, référence inexistante (site, utilisateur, module), doublon dans le fichier, doublon avec l'existant, cohérence entre champs (date de clôture avant ouverture) |
| 4 | **Écriture** | tout ou rien, dans une transaction, après autorisation explicite du client sur le rapport de l'étape 3 |

Ce qui fait le moteur, et non une validation de formulaire :

- **Les modèles sont engendrés depuis le serveur**, pas stockés comme fichiers.
  Un modèle porte les **nomenclatures du client** (ses catégories de NC, ses
  sites, ses natures de problème — §6) en listes de validation, et son **numéro
  de version**. Un modèle téléchargé le mois dernier, dont les nomenclatures ont
  changé depuis, est **reconnu comme périmé** à l'étape 2 plutôt que d'importer
  des valeurs qui n'existent plus.
- **Le rapport de contrôle est un classeur lui-même** : le fichier du client,
  rendu avec une colonne de verdict et le motif par ligne. C'est ce qui rend
  l'erreur corrigeable sans nous écrire.
- **Aucune formule n'est évaluée.** Les cellules sont lues en **valeur brute**,
  jamais en valeur calculée : une cellule dont la valeur dépend d'une formule est
  refusée, pas devinée. C'est la contrepartie du refus des macros : accepter la
  formule et refuser la macro serait une demi-mesure.
- **Le texte importé est neutralisé à la sortie, pas à l'entrée** : une cellule
  qui commence par `=`, `+`, `-` ou `@` est stockée telle quelle (c'est peut-être
  un code d'article) mais **préfixée à l'export** pour qu'un tableur ne
  l'interprète pas. L'injection de formule dans un CSV est une faille dont la
  victime est le poste du collègue qui ouvre le fichier.
- **Chaque import est un événement de piste d'audit scellé** : qui, quand, quel
  fichier (empreinte), quel modèle, combien de lignes, et le rapport conservé.
  Une reprise de données est précisément ce qu'un auditeur demandera à retracer.

### 9.4 Périmètre d'entrée (le reste suit la même mécanique)

Les six référentiels dont la reprise est demandée en premier, parce que tout le
reste en dépend : **sites**, **utilisateurs de l'équipe qualité**,
**nomenclatures** (catégories et natures), **documents de la GED** (métadonnées),
**non-conformités historiques**, **équipements et plans de calibration**.

Un septième entrant ne demande aucun développement de moteur : un descripteur
de modèle (colonnes, types, références, règles de cohérence) et son adaptateur
d'écriture.

### 9.5 Ce qui n'est PAS fait

- Pas de reprise **incrémentale** ni de synchronisation continue : c'est un
  connecteur, pas un import (§13 de `CLAUDE.md`).
- Pas de **retour arrière** d'un import validé : l'étape 3 existe pour que la
  question ne se pose pas. Un import refusé n'a rien écrit ; un import accepté
  l'a été sur un rapport lu.
- Pas d'import de **pièces jointes en masse** dans ce lot.

### 9.6 Lot

**Lot 8** — moteur de reprise (recevabilité, conformité au modèle, contrôle à
blanc, écriture transactionnelle), génération des modèles depuis les
nomenclatures du client, écran dans le paramétrage du client, six descripteurs
d'entrée. Famille **client** (§7) : la reprise existe en on-premise, où elle est
même le premier geste après l'installation. Dépend du lot 6 (habilitations : qui
a le droit d'importer) pour l'écran, mais le moteur est parallélisable.
