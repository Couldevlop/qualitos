# ADR 0127 — Le traitement d'un écart suit les huit disciplines, et sa clôture demande un second regard

- **Statut** : accepté
- **Date** : 2026-11-18
- **Portée** : module Non-Conformités (§4.3) — fiche de l'écart, modèle de
  données, autorisation de clôture

## Contexte

### La fiche ne disait pas où l'on en était

La fiche d'un écart présentait tout ce qu'il faut — l'équipe, le problème, les
photos, l'analyse de cause, les actions, les preuves — **en cartes, sans
ordre**. Chaque élément était juste ; l'ensemble ne répondait pas à la question
que se pose la personne qui ouvre la fiche : *qu'est-ce qui reste à faire ?*

Le produit connaissait pourtant déjà la réponse. Il génère un **rapport 8D**
(ADR 0115, ADR 0124), c'est-à-dire exactement la trame que les clients
industriels attendent : huit disciplines, dans l'ordre. Mais cette trame
n'existait qu'à la sortie, dans le PDF. On remplissait donc une fiche sans
structure pour produire un rapport qui en a une — et l'on découvrait en lisant
le rapport ce qui manquait.

### Le client et le lot n'étaient nulle part

Les deux informations que toute réclamation porte — *quel client*, *quel lot* —
s'écrivaient dans la description, en texte libre. Elles n'en ressortaient ni en
filtre, ni en indicateur, ni dans le rapport remis au client, qui les réclame en
tête.

### Un écart qui revient obligeait à en ouvrir un second

Une récidive se constate après la clôture. Un client relance. Un constat avait
été écarté à tort. Dans les trois cas, il fallait ouvrir un **second** écart —
qui perdait l'équipe, l'analyse de cause et les actions du premier, et comptait
deux fois le même problème dans les indicateurs.

### Personne ne contrôlait la clôture

Clore un écart, c'est déclarer « plus rien à faire ici » — donc **valider** que
la résolution tient. La même personne pouvait écrire la résolution puis la
clôturer dans la minute. Le registre déclarait l'écart traité **et** vérifié
sans qu'aucun contrôle indépendant n'ait eu lieu, et l'organisation n'avait rien
à montrer à un auditeur, alors que c'est précisément ce que l'ISO 9001 §10.2
attend de la vérification de l'efficacité.

## Décision

### 1. La fiche prend la trame du rapport

Un écran, huit sections, dans l'ordre des disciplines : équipe (D1), problème
(D2), confinement (D3), causes (D4), correctives (D5), preuves (D6), préventives
(D7), remerciements (D8). Un sommaire collant marque ce qui porte déjà quelque
chose et emmène sur la discipline qu'on choisit.

**Pas un second écran.** La fiche existante est restructurée. Son composant
portait déjà chaque transition, les photos, l'analyse visuelle, l'équipe, les
dossiers CAPA et les conversions, avec cinq fichiers de bancs qui les tiennent :
un écran neuf aurait dupliqué tout cela, et c'est le filet de régression qu'on
aurait perdu.

Les éléments ne sont pas déplacés au hasard. Les photos du constat **décrivent**
le problème : elles vont au D2, avec l'analyse visuelle qui lit l'une d'elles.
Les figures d'analyse de cause vont au D4, où la cause se discute. Le tableau des
actions CAPA paraît **trois fois**, filtré sur la nature de chaque discipline
(confinement, corrective, préventive) — le modèle d'action portait déjà cette
nature, rien n'a été inventé pour l'occasion.

### 2. Les huit disciplines sont toujours là, et rien n'y est obligatoire

Une discipline vide **se dit** plutôt que de disparaître : son absence ferait
croire à un écran incomplet. Mais aucune ne barre la suivante. Un écart mineur
n'a pas de confinement à décrire, et l'exiger ferait inventer une ligne pour
passer — ce qui vaut moins qu'une case vide honnête. Le sommaire marque ce qui
est **fait** ; il n'autorise ni n'interdit rien.

La conversion en cycle PDCA ou en projet DMAIC reste une **option** du D2, pas
une étape. Ce que l'écart révèle peut mériter un cycle, et c'est une décision
(§3.6).

### 3. Le client et le lot entrent au modèle

Deux colonnes (`customer_name`, `batch_number`), facultatives, saisies au D2.
Les ouvertures machine — constat d'audit, connecteur EHR — n'en mettent aucune :
un constat d'audit porte sur le système de management, pas sur une livraison, et
le nom d'un patient ne doit surtout pas entrer là (§11.3).

### 4. Un écart refermé se reprend

`POST /nc/{id}/reopen` ramène à `OPEN` un écart `RESOLVED`, `CLOSED`,
`CANCELLED` ou `REJECTED`. **Rien n'est effacé** : l'équipe, l'analyse, les
actions, les preuves et ce qui l'avait refermé restent attachés ; le bandeau de
la fiche montre le fait le plus récent.

Vers `OPEN` et non vers `ACTION_DEFINED` : ce qui a échoué, c'est justement la
cause ou l'action. Sous la permission `nc.close` : défaire un acte demande
l'autorité de le poser.

Le motif est **facultatif**, et c'est un choix qui a son prix — une fiche peut
montrer un retour en arrière que personne ne sait expliquer six mois plus tard.
Mais l'exiger bloquerait le geste même, et un écart qui revient doit pouvoir
être repris tout de suite.

### 5. La clôture demande un second regard

**Deux mécanismes, et ils sont distincts.**

**Le droit.** Clore appartient aux rôles de pilotage **et** à une personne
*désignée* sur cet écart (`validator_id`, choisie dans l'annuaire), même sans
rôle qualité. Sans cette seconde voie, une organisation qui ne compte qu'un
responsable qualité ne pourrait plus rien clore dès lors qu'on exige deux
personnes : c'est lui qui résout, et lui seul qui a le droit de clore. La
désignation ouvre ce regard à un chef d'atelier, un pilote de processus, un
confrère d'un autre service — **sans lui donner le moindre droit sur les autres
écarts**.

**La séparation.** La personne qui a écrit la résolution ne peut pas la
clôturer, quel que soit son rôle — la désignation n'en exempte pas, sinon elle
offrirait le moyen exact de contourner la règle qu'elle sert.

Deux refus différents, parce que les deux situations n'ont rien de commun :

| Situation | Réponse | Pourquoi |
|---|---|---|
| Ni rôle ni désignation | **403**, muet | On n'explique pas le modèle de droits à qui n'en a pas, et le refus ne dit rien de l'état de l'écart |
| Auteur de la résolution | **409**, explicite | La personne sait qu'elle a résolu l'écart ; ce qu'elle doit savoir, c'est que quelqu'un d'autre doit prendre le relais |

La règle se **lit avant le clic** : elle s'ajoute aux motifs de blocage de la
fiche (`SECOND_LOOK_REQUIRED`), qui éteignent le bouton avec leur explication.
C'est le **seul motif qui dépende de qui lit la fiche** — l'écart, lui, est
parfaitement clôturable, par quelqu'un d'autre.

Trois paires de colonnes portent tout cela : qui a résolu, qui a clos, qui
valide. Les deux premières sont la **preuve** du contrôle ; sans elles la règle
serait appliquée sans être démontrable, et c'est la démonstration qu'un auditeur
vient chercher.

#### La permission quitte le point d'entrée

`POST /nc/{id}/close` est le seul point d'entrée du module sans
`@RequiresPermission`, et c'est volontaire : l'intercepteur tranche **avant**
d'avoir lu quel écart est visé, il ne peut donc pas connaître la désignation
portée par cet écart-là. Le contrôle n'est pas levé, il est **déplacé** dans le
service, qui refuse par un 403 avant toute autre vérification. Un banc de
contrôleur tient cet invariant : un appelant sans droit doit obtenir 403, et s'il
obtenait 200 le contrôle aurait disparu des deux côtés à la fois.

### 6. Les écarts d'avant la règle ne sont pas punis

Ils n'ont pas de `resolved_by_id` et se closent sans obstacle. Les bloquer tous
faute de trace reviendrait à reprocher à l'organisation une donnée que le
logiciel ne collectait pas encore.

## Conséquences

- La fiche répond à « qu'est-ce qui reste à faire ? », et elle le fait avec la
  trame que le client recevra.
- Le rapport 8D n'invente plus rien : il lit ce que la fiche a recueilli,
  discipline par discipline.
- Le client et le lot deviennent filtrables, comptables, et paraissent en tête du
  rapport.
- Une récidive se traite sur l'écart qui l'a connue, et les indicateurs ne
  comptent plus deux fois le même problème.
- Une clôture porte deux signatures distinctes, lisibles sur la fiche.
- ⚠ **Une organisation d'une seule personne ne peut plus clore ses écarts** sans
  désigner un valideur. C'est l'effet voulu : le second regard n'a pas de sens à
  une personne, et la désignation est précisément la sortie — elle n'exige aucun
  rôle qualité.
- ⚠ Le motif de reprise étant facultatif, une fiche peut porter un retour en
  arrière inexpliqué. Arbitrage assumé : mieux vaut une reprise immédiate sans
  phrase qu'un écart qu'on n'ose pas reprendre.
- ⚠ La clôture n'étant plus gardée par le point d'entrée, toute évolution de
  `NcService.close` doit préserver le refus en tête de méthode.

## Alternatives écartées

- **Un second écran pour le 8D.** Aurait dupliqué tout le comportement de la
  fiche, et perdu les bancs qui le tiennent.
- **Rendre les disciplines obligatoires l'une après l'autre.** Aurait fait
  inventer des lignes pour passer à l'étape suivante. Une case vide honnête vaut
  mieux qu'un confinement imaginaire.
- **Élargir `nc.close` au rôle auditeur.** L'auditeur a une lecture étendue et
  une position d'indépendance, mais il deviendrait acteur du dossier qu'il
  audite.
- **Exiger deux personnes parmi les seuls rôles de pilotage.** Plus strict, et
  bloquant pour qui n'en compte qu'un.
- **Vérifier la clôture dans l'intercepteur.** Impossible : il décide sans
  connaître l'écart visé, donc sans connaître son valideur désigné.
- **Un 403 pour le refus de second regard.** Sémantiquement défendable, mais le
  handler le rend muet — et la personne a besoin de savoir qu'il faut faire
  porter la clôture par quelqu'un d'autre, non qu'elle manque de droits.

## Références

- `CLAUDE.md` §4.3 (non-conformités), §4.2 (CAPA), §3.6 (référentiel
  transverse), §11.3 (RGPD), §16 (rôles)
- ISO 9001:2015 §10.2 — non-conformité et action corrective
- ADR 0078 — permissions réglables par client
- ADR 0081 — restriction de portée des enregistrements
- ADR 0108 — verdict d'efficacité d'un dossier CAPA
- ADR 0115 — les figures d'analyse de cause sur la fiche et dans le rapport 8D
- ADR 0124 — le PDF du 8D se présente comme l'écran
- Migrations `V171__nc_client_lot_et_reouverture.sql`,
  `V172__nc_second_regard.sql`
