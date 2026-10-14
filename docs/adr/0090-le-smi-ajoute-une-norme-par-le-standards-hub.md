# ADR 0090 — Le SMI ajoute une norme par le Standards Hub, et seul l'administrateur du client le fait

- **Statut** : Accepté
- **Date** : 2026-10-14
- **Owners** : @Couldevlop
- **Portée** : module `smi` (api-quality-engine), écrans `/smi` et `/smi/exigences` (web)
- **S'inscrit dans** : ADR 0077 (le SMI relit les modules), ADR 0065 (autorisation
  avant lecture du corps), ADR 0020 (alias de rôles), CLAUDE.md §8.9 (systèmes
  intégrés), §16 (rôles)

## Contexte

En préproduction, le tableau de bord SMI n'affichait que l'ISO 9001. Constat après
lecture du code et des migrations :

1. **Rien ne bloquait techniquement une seconde norme.** L'ISO 14001 est au
   catalogue (V14, complétée par V59 : sections « 4 » à « 10 », toutes avec des
   exigences) ; son adoption passe par `POST /api/v1/standards/adoptions` sans
   contrainte particulière ; le tableau de bord, le filtre `?norme=`, la
   conformité par norme et la matrice des exigences savent déjà croiser plusieurs
   normes, par code de section (`HlsChapter`). Un test du service le montre avec
   9001 + 14001 côte à côte.
2. **Mais le SMI n'offrait aucun chemin pour l'ajouter.** Le lien « Adopter une
   norme » vers `/standards` n'apparaissait que lorsque AUCUNE norme n'était
   adoptée. Une fois l'ISO 9001 adoptée, il fallait savoir qu'il faut aller dans
   le Standards Hub, y retrouver l'ISO 14001 parmi une soixantaine de
   référentiels (la liste du catalogue charge 50 entrées, sans tri), et l'adopter
   — un parcours que rien n'indique depuis le SMI.
3. Deux défauts de lecture aggravaient la situation à deux normes : les normes
   étaient triées comme du texte (`iso-14001` avant `iso-9001`), et le titre
   complet du catalogue (« ISO 14001:2015 — Systèmes de management
   environnemental — Exigences… ») servait d'étiquette de filtre et d'en-tête de
   colonne.

Règle produit : tout paramétrage (hors gestion des utilisateurs) revient à
l'administrateur du client, et à lui seul. Choisir les normes du SMI en est un.

## Décisions

### 1. Pas de second mécanisme d'adoption

`POST /api/v1/smi/standards` délègue à `StandardsService.adopt` : même statut
initial (`PLANNING`), même feuille de route de certification (19 étapes), même
contrôle d'unicité. Le SMI ne fait que **filtrer** ce qu'il propose.

### 2. Le SMI ne propose que des normes de système de management certifiables

`GET /api/v1/smi/standards/candidates` rend les normes publiées par la plateforme
de la famille `HLS` exigeant un organisme certificateur, non encore adoptées
(quel que soit l'état d'une adoption existante : une adoption retirée se reprend
dans le Standards Hub, qui refuserait d'en créer une seconde). Les lignes
directrices de la même famille (ISO 19011, 31000, 10002, 26000) sont écartées.

Une norme est **suggérée** quand le catalogue la rattache à une norme pilotée, dans
un sens ou dans l'autre (`related_norm_codes` : l'ISO 14001 cite l'ISO 9001). Les
suggestions passent en tête, les plus rattachées d'abord, puis dans l'ordre
naturel des codes — l'ISO 14001 vient donc en tête pour un client ISO 9001, sans
qu'aucun code de norme soit écrit en dur.

### 3. Réservé à l'administrateur du client

`@PreAuthorize("hasAnyRole('ADMIN_TENANT','ADMIN','TENANT_ADMIN')")` sur le
contrôleur : décidé sur le jeton seul, donc avant la lecture du corps (ADR 0065).
Manager et directeur qualité, auditeurs, utilisateurs **et super administrateur
de l'éditeur** reçoivent 403. On n'a pas pris une action réglable par client
(ADR 0078) : elle serait délégable, ce que la règle produit exclut.

L'écran n'affiche « Ajouter une norme » qu'aux mêmes rôles du jeton ; les autres
lisent « L'administrateur de votre organisation ajoute les normes. »

### 4. Ordre naturel et noms courts

Les normes se rangent par l'ordre naturel de leur code (`StandardOrder`) : ISO
9001, puis 14001, puis 45001. L'écran affiche le nom court (« ISO 14001:2015 »,
avant le tiret long) et garde le titre complet en infobulle et en nom
accessible.

## Conséquences

- ✅ Depuis `/smi` ou `/smi/exigences`, l'administrateur ajoute l'ISO 14001 en deux
  clics ; le tableau se recharge sur « Tous », les deux normes côte à côte, et la
  matrice gagne sa colonne.
- ✅ Aucune donnée de référence à corriger : pas de migration (V141 non utilisée).
- ⚠ **L'API globale d'adoption reste ouverte à tout authentifié** :
  `POST /api/v1/standards/adoptions` n'est protégée ni par `SecurityConfig` ni par
  `@PreAuthorize` — un simple utilisateur peut adopter une norme depuis le
  Standards Hub. Hors du périmètre de ce changement ; à aligner sur la règle
  produit dans un lot dédié (avec les autres écritures du Standards Hub).
- ⚠ **La mutualisation des preuves n'existe pas encore.** `computeAlignment` ne
  compte que les preuves rattachées à l'adoption elle-même ; la table
  `norm_clause_mapping` (V53) et le module `ims` ne servent qu'à la matrice de
  co-couverture des clauses. Une preuve posée sur l'ISO 9001 §5.2 ne couvre donc
  pas l'ISO 14001 §5.2, alors que l'écran SMI l'affirme (« Une même preuve…
  est comptée pour tous les référentiels concernés »). À traiter par une
  décision propre : propager à l'alignement les preuves des clauses
  `EQUIVALENT`, et le dire dans le Standards Hub comme dans le SMI.

## Vérifications

- `SmiDashboardServiceTest#neufMilleEtQuatorzeMilleSePilotentCoteACote` — tableau
  de bord, filtre 14001 (conformité, risques, audit) et matrice chapitre par
  chapitre, avec les sections telles que livrées par V10/V57 et V14/V59.
- `SmiStandardsServiceTest` — propositions, ordre, rattachements dans les deux
  sens, refus (hors système de management, déjà adoptée).
- `SmiStandardsControllerTest` — 201/200 pour l'administrateur et ses alias ;
  403 pour les sept autres rôles, corps valide ou malformé ; 400, 404, 409.
- `SmiAdaptersTest` — filtre du catalogue, adoption déléguée au Standards Hub.
- Specs Karma : fenêtre d'ajout, ouvreur, tableau de bord et matrice à deux normes.
