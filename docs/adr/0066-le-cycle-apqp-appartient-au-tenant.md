# ADR 0066 — Le cycle APQP appartient au tenant, amorcé par le manuel AIAG

- **Statut** : Accepté
- **Date** : 2026-09-08
- **Owners** : @Couldevlop
- **Portée** : Module APQP (phases, livrables, schéma en V)
- **S'inscrit dans** : ADR 0064 §1 (« un référentiel de méthode est une donnée de
  tenant, pas une constante du code »)

## Contexte

Les cinq phases APQP et leurs livrables vivaient dans `apqp.reference.ts`, une
constante du front, identique pour tous les tenants. L'écran les dessinait en V,
et la feuille de style figeait ce V à **cinq colonnes**.

Trois conséquences, dans l'ordre de gravité :

1. **C'est une logique sectorielle en dur**, ce qu'interdit l'invariant §22.11.
   Le manuel AIAG décrit l'industrie automobile. Un fabricant de dispositifs
   médicaux planifie autour de l'évaluation clinique et du dossier technique
   MDR ; un façonnier pharmaceutique autour de la validation IQ/OQ/PQ. Imposée,
   la trame automobile produit un écran que personne ne consulte, et la vraie
   planification repart sur un tableur à côté.
2. **Rien ne pouvait être adapté** : ni renommer une phase dans le vocabulaire
   de la maison, ni retirer une étape sans objet, ni ajouter le jalon que le
   client final exige.
3. Le nombre de colonnes étant écrit dans la feuille de style, **une sixième
   phase aurait écrasé le V en L** — le schéma aurait menti sur la méthode.

## Décisions

### 1. Le cycle est une donnée du tenant (V124), le manuel n'est plus qu'une amorce

Deux tables, `apqp_phases` et `apqp_deliverables`, portées par `tenant_id`. La
référence AIAG reste dans le code (`ApqpReference.java`) mais comme **valeur de
départ**, plus comme vérité.

### 2. L'amorçage a lieu à la première LECTURE, pas à la création du tenant

Amorcer à l'inscription aurait imposé une reprise sur tous les tenants existants
et laissé des données à ceux qui n'ouvrent jamais l'écran. La première lecture
écrit donc — c'est la seule raison pour laquelle `cycle()` n'est pas
`readOnly` — et les suivantes ne réamorcent pas.

Repartir d'un écran vide, à l'inverse, aurait obligé chaque organisation à
ressaisir un contenu normatif que la plateforme connaît déjà : l'adaptabilité
promise en §5 est un point de départ qu'on amende, pas une page blanche.

### 3. Le rang dans le V est calculé par le SERVEUR

`level = min(position, n + 1 - position)` : on descend jusqu'au milieu, on
remonte. Cinq phases donnent 1, 2, 3, 2, 1 ; six donnent 1, 2, 3, 3, 2, 1. La
forme tient quel que soit le nombre — c'était la condition pour qu'on puisse en
ajouter.

Calculé côté serveur et non dans l'écran : deux vues du même cycle doivent le
dessiner pareil, et la règle change dès qu'une phase entre ou sort. Le nombre de
colonnes est posé en style en ligne depuis le cycle reçu, parce qu'une feuille
de style ne peut pas connaître un nombre qui appartient au client.

### 4. Un rang libéré est aussitôt resserré

Supprimer la phase 3 renumérote les suivantes en 3, 4 plutôt que de laisser
1, 2, 4, 5. Sans cela le V se dessinerait avec un trou, et la contrainte
d'unicité `(tenant_id, position)` refuserait la prochaine insertion au rang
libéré. La contrainte est `DEFERRABLE`, une réorganisation passant par des états
intermédiaires où deux phases se croisent le temps d'une transaction.

### 5. Réorganiser porte sur le cycle ENTIER, jamais sur un déplacement isolé

`PUT /order` reçoit tous les identifiants dans leur nouvel ordre et refuse
(422) une liste partielle ou étrangère au cycle. Une liste partielle laisserait
des phases sans rang, donc hors du V ; plutôt que de deviner où ranger les
absentes, on refuse. 422 et non 400 : la requête est bien formée, c'est son
**contenu** qui ne décrit pas le cycle.

Sans cette opération, une phase ajoutée resterait à jamais en fin de cycle — le
serveur la pose à la suite — et le cycle n'appartiendrait au tenant qu'à moitié.

### 6. Lire est ouvert à tout authentifié, refondre ne l'est pas

Le cycle décrit ce que l'organisation attend à chaque phase : on ne peut pas
demander à quelqu'un de produire une AMDEC processus en lui cachant à quel
moment elle est due. Mais supprimer une phase emporte ses livrables, et
réorganiser change la méthode : ces gestes sont réservés au manager qualité, à
la direction qualité et à l'administration du tenant — exactement la liste des
quatorze autres référentiels de méthode du module.

## Conséquences

- ✅ Une organisation planifie sur SA trame, quel que soit son secteur, sans
  quitter la plateforme et sans qu'aucun code sectoriel soit écrit.
- ✅ Le schéma reste juste à cinq, six ou trois phases : c'est le serveur qui
  dit où chaque jalon se pose.
- ✅ Le schéma **est** l'interface : on ouvre une phase en cliquant dessus, et
  l'URL porte son rang, donc un lien vers une phase se partage.
- ⚠ Le segment d'URL est le rang, pas un mot du titre : un renommage ne casse
  pas les liens partagés, mais un **déplacement** change l'adresse de la phase.
  L'écran suit son propre déplacement ; un lien partagé avant une réorganisation
  ouvrira la phase qui occupe désormais ce rang.
- ⚠ Les libellés des phases et des livrables amorcés sont en français et ne sont
  **pas** traduits : ils deviennent des données du tenant dès la première
  lecture, et un livrable normatif traduit librement n'est plus le même livrable
  (« Control Plan » désigne un document précis). Seule la coquille de l'écran
  est traduite dans les six langues.

## Vérifications

- `ApqpServiceTest` — la forme du V à 1, 4, 5 et 6 phases ; amorçage à la
  première lecture et à elle seule ; resserrement des rangs après suppression
  d'une phase et d'un livrable ; refus d'un ordre partiel ou étranger ;
  isolation par tenant ; absence de contexte tenant.
- `ApqpControllerTest` — lecture ouverte à l'authentifié, fermée à l'anonyme ;
  écriture refusée à l'opérateur et à l'auditeur ; corps malformé sans le rôle
  rendu comme un refus et non comme une erreur de saisie (ADR 0065) ; 404 sur
  une phase ou un livrable inconnus, 422 sur un ordre partiel.
- `ApqpOverviewComponent` (front) — la ligne de grille suit le rang rendu par le
  serveur ; une colonne par phase quel qu'en soit le nombre ; l'ordre du DOM
  reste la séquence de lecture ; le cycle est rechargé après un ajout ou une
  suppression de phase, mais pas après un simple renommage ; l'ordre part
  entier ; aucune commande d'édition pour qui n'a pas le droit d'écrire.

## Références

- `CLAUDE.md` §5 (adaptabilité par domaine), §22.11 (aucune logique sectorielle
  en dur), §16 (modèle de rôles).
- ADR 0064 — le barème FMEA, même raisonnement appliqué à la cotation.
- ADR 0065 — l'autorisation décidée avant la lecture du corps.
- Migration `V124__create_apqp_phases.sql`.
